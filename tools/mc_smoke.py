#!/usr/bin/env python3
"""Headless Minecraft 1.12.2 + Forge smoke launcher.

Boots the REAL Forge distribution (vanilla client + Forge universal, exactly what a
user installs) with the jars from --mods, under a virtual X display, and reports
whether the game reaches the title screen or crashes.

This deliberately does NOT use the ForgeGradle dev run: FG3's transformed 1.12.2
Forge artifacts carry a third `Side` constant (BUKKIT) that makes Forge's own
NetworkRegistry throw before any mod code runs, so they cannot verify the artifact
we actually ship.

Usage:
  mc_smoke.py --mods dist/deps --mc-dir .smokeclient --cache .smoke-cache \
              --log client-smoke.log [--timeout 900] [--dry-run] [--download-assets]

Exit codes: 0 title screen reached, 1 crash, 2 no marker before timeout, 3 setup failure.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

UA = {"User-Agent": "create-112-smoke/1.0 (+github.com/Sekai0NI0itamio/create-1.12)"}
VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FORGE_INSTALLER = (
    "https://maven.minecraftforge.net/net/minecraftforge/forge/{v}/forge-{v}-installer.jar"
)

MC_VERSION = "1.12.2"
FORGE_VERSION = "1.12.2-14.23.5.2859"

CRASH_MARKERS = [
    "Minecraft Crash Report",
    "A fatal error has been detected",
    "ExceptionInInitializerError",
    "LoaderExceptionModCrash",
    "Attempted to set registry name",
    "Failed to start game",
]
# FML finishing mod loading, then Minecraft finishing client init: together they mean
# the title screen was constructed with our mods present.
LOADED_MARKER = "Forge Mod Loader has successfully loaded"
TITLE_MARKERS = [
    "Narrator library",
    "Sound engine started",
    "Created: 1024x512",
]
MOD_IDS = ["create", "mixinbooter", "melonlib"]


def log(msg):
    print(f"[smoke] {msg}", flush=True)


def http_json(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)


def fetch(url, dest, min_size=1024, attempts=3, unzip_to=None, expect_jar=False, file_hint=None, extra_search_dirs=()):
    """Download url to dest with retries; optionally extract a jar/zip into unzip_to.

    Mojang replaced some retired binaries with 22-byte empty zips served as HTTP 200,
    so jar downloads are validated as non-empty zips. When the network fails, fall back
    to jars already on disk (the ForgeGradle cache in CI already holds LWJGL).
    """
    if usable(dest, min_size, expect_jar):
        return dest
    os.makedirs(os.path.dirname(dest) or ".", exist_ok=True)
    last = None
    for n in range(1, attempts + 1):
        try:
            req = urllib.request.Request(url, headers=UA)
            tmp = dest + ".part"
            with urllib.request.urlopen(req, timeout=180) as r, open(tmp, "wb") as f:
                shutil.copyfileobj(r, f)
            if not usable(tmp, min_size, expect_jar):
                raise RuntimeError(f"unusable download: {os.path.getsize(tmp)}b")
            os.replace(tmp, dest)
            if unzip_to:
                os.makedirs(unzip_to, exist_ok=True)
                with zipfile.ZipFile(dest) as z:
                    z.extractall(unzip_to)
            return dest
        except Exception as e:  # noqa: BLE001 - reported and retried
            last = e
            log(f"download attempt {n}/{attempts} failed for {url}: {e}")
            time.sleep(5 * n)
    salvaged = salvage(file_hint or os.path.basename(dest), min_size, expect_jar, extra_search_dirs)
    if salvaged:
        log(f"using on-disk copy {salvaged} for {dest}")
        shutil.copyfile(salvaged, dest)
        if unzip_to:
            os.makedirs(unzip_to, exist_ok=True)
            with zipfile.ZipFile(dest) as z:
                z.extractall(unzip_to)
        return dest
    raise RuntimeError(f"could not download {url}: {last}")


def usable(path, min_size, expect_jar):
    if not (os.path.isfile(path) and os.path.getsize(path) >= min_size):
        return False
    if expect_jar:
        try:
            with zipfile.ZipFile(path) as z:
                return len(z.namelist()) > 0
        except zipfile.BadZipFile:
            return False
    return True


def salvage(filename, min_size, expect_jar, extra_search_dirs):
    """Find a usable copy of filename under the given roots (ForgeGradle cache first)."""
    home = os.path.expanduser("~")
    log(f"salvage: looking for {filename} (home={home})")
    roots = [os.path.join(home, ".gradle")] + list(extra_search_dirs)
    walked = 0
    for root in roots:
        if not os.path.isdir(root):
            log(f"salvage: not a dir: {root}")
            continue
        for dirpath, _dirnames, filenames in os.walk(root):
            walked += len(filenames)
            if filename in filenames:
                cand = os.path.join(dirpath, filename)
                if usable(cand, min_size, expect_jar):
                    log(f"salvage: walked {walked} files, using {cand}")
                    return cand
    log(f"salvage: walked {walked} files, no usable {filename}")
    return None


def vanilla_version_json(cache):
    vpath = os.path.join(cache, f"vanilla-{MC_VERSION}.json")
    if not os.path.isfile(vpath):
        manifest = http_json(VERSION_MANIFEST)
        entry = next(v for v in manifest["versions"] if v["id"] == MC_VERSION)
        data = http_json(entry["url"])
        with open(vpath, "w") as f:
            json.dump(data, f)
    with open(vpath) as f:
        return json.load(f)


def vanilla_client_jar(cache, vjson):
    url = vjson["downloads"]["client"]["url"]
    dest = os.path.join(cache, f"client-{MC_VERSION}.jar")
    return fetch(url, dest, min_size=5_000_000, expect_jar=True)


def install_forge(mc_dir, cache):
    """Run the official Forge installer for the client; return the generated version json."""
    installer = os.path.join(cache, f"forge-{FORGE_VERSION}-installer.jar")
    fetch(FORGE_INSTALLER.format(v=FORGE_VERSION), installer, min_size=500_000, expect_jar=True)
    profiles = os.path.join(mc_dir, "launcher_profiles.json")
    if not os.path.isfile(profiles):
        os.makedirs(mc_dir, exist_ok=True)
        with open(profiles, "w") as f:
            json.dump({"profiles": {}, "selectedProfile": "", "clientToken": "smoke"}, f)
    log("running Forge installer (--installClient)")
    p = subprocess.run(
        [java_bin(), "-jar", installer, "--installClient", os.path.abspath(mc_dir)],
        capture_output=True,
        text=True,
    )
    tail = "\n".join((p.stdout + p.stderr).splitlines()[-15:])
    if p.returncode != 0:
        raise RuntimeError(f"forge installer exited {p.returncode}\n{tail}")
    versions_dir = os.path.join(mc_dir, "versions")
    cands = [
        os.path.join(versions_dir, d, d + ".json")
        for d in os.listdir(versions_dir)
        if d.startswith(f"{MC_VERSION}-forge")
    ]
    if not cands:
        raise RuntimeError(f"installer produced no version json in {versions_dir}")
    with open(cands[0]) as f:
        return json.load(f)


def merge_version(vjson, fjson):
    """Child (Forge) json wins; vanilla json fills the gaps."""
    merged = dict(vjson)
    merged.update({k: v for k, v in fjson.items() if k != "libraries"})
    libs = {}
    for lib in vjson.get("libraries", []) + fjson.get("libraries", []):
        libs[lib["name"].split("@")[0]] = lib
    merged["libraries"] = list(libs.values())
    return merged


def java_bin():
    home = os.environ.get("JAVA_HOME")
    if home:
        cand = os.path.join(home, "bin", "java")
        if os.path.isfile(cand):
            return cand
    return "java"


def rules_allow(entry):
    """Minimal launcher rule check: keep entries with no rules or a linux/windows allow."""
    rules = entry.get("rules")
    if not rules:
        return True
    allowed = False
    for rule in rules:
        action = rule.get("action") == "allow"
        os_name = rule.get("os", {}).get("name")
        if "features" in rule:
            continue  # no extra features enabled in CI
        if os_name is None or os_name == "linux":
            allowed = action
    return allowed


def maven_path(name):
    """net.minecraftforge:forge:1.12.2-14.23.5.2859 ->
    net/minecraftforge/forge/1.12.2-14.23.5.2859/forge-1.12.2-14.23.5.2859.jar"""
    base = name.split("@")[0]
    group, artifact, version = base.split(":")[:3]
    return "/".join(group.split(".") + [artifact, version, f"{artifact}-{version}.jar"])


def resolve_libraries(merged, mc_dir, cache, dry_run):
    """Download every artifact + natives; return (classpath entries, natives dir).

    Some entries (the Forge universal itself) ship with an empty download URL because
    the installer already placed the file under <mc_dir>/libraries - fall back to the
    installed file, then to the public maven URL.
    """
    libs_dir = os.path.join(cache, "libraries")
    installed_dir = os.path.join(mc_dir, "libraries")
    natives_dir = os.path.join(cache, "natives")
    os.makedirs(natives_dir, exist_ok=True)
    classpath, natives = [], []
    for lib in merged["libraries"]:
        if not rules_allow(lib):
            continue
        downloads = lib.get("downloads", {})
        artifact = downloads.get("artifact")
        if artifact:
            rel = artifact.get("path") or maven_path(lib["name"])
            url = artifact.get("url") or ""
            installed = os.path.join(installed_dir, rel)
            dest = os.path.join(libs_dir, rel)
            if not dry_run:
                if url:
                    fetch(url, dest, min_size=512, expect_jar=True, extra_search_dirs=[installed_dir])
                elif os.path.isfile(installed):
                    dest = installed
                else:
                    fallback = "https://maven.minecraftforge.net/" + maven_path(lib["name"])
                    log(f"no url for {lib['name']}, trying {fallback}")
                    fetch(fallback, dest, min_size=512, expect_jar=True, extra_search_dirs=[installed_dir])
            elif os.path.isfile(installed):
                dest = installed
            classpath.append(os.path.abspath(dest))
        native = downloads.get("classifiers", {}).get("natives-linux")
        if native and native.get("url"):
            rel = native.get("path") or maven_path(lib["name"])
            dest = os.path.join(libs_dir, rel)
            if not dry_run:
                fetch(native["url"], dest, min_size=512, unzip_to=natives_dir, expect_jar=True,
                      extra_search_dirs=[installed_dir])
            natives.append(dest)
    return classpath, natives_dir


def ensure_assets(mc_dir, cache, merged, download_objects, dry_run):
    """Stage the asset index, and (optionally) every object it references, under mc_dir/assets."""
    index = merged.get("assetIndex")
    if not index:
        raise RuntimeError("version json has no assetIndex")
    index_path = os.path.join(mc_dir, "assets", "indexes", index["id"] + ".json")
    if not dry_run:
        fetch(index["url"], index_path, min_size=1024)
    if not download_objects:
        log("skipping asset objects (index only; missing textures render as fallback)")
        return index["id"]
    if dry_run:
        log("asset objects requested but dry run - skipping")
        return index["id"]
    with open(index_path) as f:
        data = json.load(f)
    objects_dir = os.path.join(mc_dir, "assets", "objects")
    os.makedirs(objects_dir, exist_ok=True)
    total = len(data["objects"])
    log(f"downloading {total} asset objects (8 workers)")
    failures = []
    lock = threading.Lock()
    done = [0]

    def grab(item):
        name, obj = item
        dest = os.path.join(objects_dir, obj["hash"][:2], obj["hash"])
        if not os.path.isfile(dest):
            url = f"https://resources.download.minecraft.net/{obj['hash'][:2]}/{obj['hash']}"
            try:
                fetch(url, dest, min_size=1, attempts=2)
            except RuntimeError as e:
                with lock:
                    failures.append(f"{name}: {e}")
        with lock:
            done[0] += 1
            if done[0] % 1000 == 0:
                log(f"assets {done[0]}/{total}")

    with ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(grab, data["objects"].items()))
    log(f"assets {done[0]}/{total} done, {len(failures)} unavailable")
    return index["id"]


def build_command(merged, client_jar, classpath, natives_dir, mc_dir, asset_index, username):
    cp = os.pathsep.join(classpath + [client_jar])
    tokens = {
        "${auth_player_name}": username,
        "${version_name}": merged["id"],
        "${game_directory}": os.path.abspath(mc_dir),
        "${assets_root}": os.path.abspath(os.path.join(mc_dir, "assets")),
        "${assets_index_name}": asset_index,
        "${auth_uuid}": "00000000000000000000000000000000",
        "${auth_access_token}": "0",
        "${auth_session}": "0",
        "${user_type}": "legacy",
        "${user_properties}": "{}",
        "${version_type}": "release",
        "${resolution_width}": "1280",
        "${resolution_height}": "720",
        "${classpath}": cp,
        "${classpath_separator}": os.pathsep,
        "${library_directory}": os.path.abspath(os.path.join(mc_dir, "libraries")),
    }

    def sub(text):
        for k, v in tokens.items():
            text = text.replace(k, v)
        return text

    cmd = [
        java_bin(),
        "-Xmx3G",
        "-Djava.library.path=" + os.path.abspath(natives_dir),
        "-Dfml.ignoreInvalidMinecraftCertificates=true",
        "-Dfml.ignorePatchDiscrepancies=true",
        "-Dmixin.debug.verbose=false",
    ]
    args_obj = merged.get("arguments")
    if args_obj:
        for part in args_obj.get("jvm", []):
            if isinstance(part, str):
                cmd.append(sub(part))
            elif isinstance(part, dict) and rules_allow(part) and "value" in part:
                value = part["value"]
                cmd += [sub(v) for v in (value if isinstance(value, list) else [value])]
        cmd += ["-cp", cp]
        cmd.append(merged["mainClass"])
        for part in args_obj.get("game", []):
            if isinstance(part, str):
                cmd.append(sub(part))
            elif isinstance(part, dict) and rules_allow(part) and "value" in part:
                value = part["value"]
                cmd += [sub(v) for v in (value if isinstance(value, list) else [value])]
    else:
        cmd += ["-cp", cp, merged["mainClass"]]
        cmd += [sub(a) for a in merged.get("minecraftArguments", "").split()]
    return cmd


def launch(cmd, log_path, timeout, display):
    env = dict(os.environ)
    env["DISPLAY"] = display
    env["LIBGL_ALWAYS_SOFTWARE"] = "1"
    log(f"launching: {' '.join(cmd[:6])} ... (log -> {log_path})")
    with open(log_path, "wb") as out:
        proc = subprocess.Popen(cmd, stdout=out, stderr=subprocess.STDOUT, cwd=os.path.dirname(log_path) or ".", env=env)
        deadline = time.time() + timeout
        while time.time() < deadline:
            if proc.poll() is not None:
                return proc.returncode, read_log(log_path)
            time.sleep(2)
        log(f"timeout after {timeout}s - terminating")
        proc.terminate()
        try:
            proc.wait(timeout=30)
        except subprocess.TimeoutExpired:
            proc.kill()
        return 124, read_log(log_path)


def read_log(path):
    try:
        with open(path, errors="replace") as f:
            return f.read()
    except OSError:
        return ""


def assess(text, log_path):
    for marker in CRASH_MARKERS:
        if marker in text:
            idx = text.find(marker)
            log(f"CRASH: marker {marker!r} at offset {idx}")
            print(tail(text, 120))
            return 1
    if LOADED_MARKER in text:
        found = [m for m in MOD_IDS if m in text]
        missing = [m for m in MOD_IDS if m not in text]
        if missing:
            log(f"mod list check: found {found}, missing {missing} - failing")
            return 1
        for marker in TITLE_MARKERS:
            if marker in text:
                log(f"PASS: mod loading complete and title-screen marker {marker!r} seen")
                return 0
        log("mod loading completed but no title-screen marker appeared")
        print(tail(text, 60))
        return 2
    log("game did not reach FML mod-loading completion")
    print(tail(text, 60))
    return 2


def tail(text, lines):
    return "\n".join(text.splitlines()[-lines:])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mods", default="dist/deps", help="directory of jars to copy into mods/")
    ap.add_argument("--skip-mods", default="", help="comma-separated jars to leave out of mods/")
    ap.add_argument("--mc-dir", default=".smokeclient")
    ap.add_argument("--cache", default=".smoke-cache")
    ap.add_argument("--log", default="client-smoke.log")
    ap.add_argument("--timeout", type=int, default=900)
    ap.add_argument("--display", default=":99")
    ap.add_argument("--username", default="SmokeTest")
    ap.add_argument("--download-assets", action="store_true")
    ap.add_argument("--dry-run", action="store_true", help="resolve everything, print the command, launch nothing")
    args = ap.parse_args()

    os.makedirs(args.cache, exist_ok=True)
    os.makedirs(args.mc_dir, exist_ok=True)

    try:
        vjson = vanilla_version_json(args.cache)
        client_jar = vanilla_client_jar(args.cache, vjson)
        fjson = install_forge(args.mc_dir, args.cache)
        merged = merge_version(vjson, fjson)
        classpath, natives_dir = resolve_libraries(merged, args.mc_dir, args.cache, args.dry_run)
        asset_index = ensure_assets(args.mc_dir, args.cache, merged, args.download_assets, args.dry_run)

        mods_dir = os.path.join(args.mc_dir, "mods")
        os.makedirs(mods_dir, exist_ok=True)
        skip = {s.strip() for s in args.skip_mods.split(",") if s.strip()}
        for entry in sorted(os.listdir(args.mods)):
            if not entry.endswith(".jar") or entry in skip:
                continue
            dst = os.path.join(mods_dir, entry)
            if not os.path.isfile(dst) or os.path.getsize(dst) != os.path.getsize(os.path.join(args.mods, entry)):
                shutil.copyfile(os.path.join(args.mods, entry), dst)
            log(f"mod {entry}")
        # The dev-built jar is what we are validating: copy it in from build/libs if given.
        for extra in os.environ.get("SMOKE_EXTRA_JARS", "").split(","):
            if extra.strip() and os.path.isfile(extra.strip()):
                shutil.copyfile(extra.strip(), os.path.join(mods_dir, os.path.basename(extra.strip())))
                log(f"mod (local build) {os.path.basename(extra.strip())}")
    except Exception as e:  # noqa: BLE001 - surfaced as a setup failure
        log(f"SETUP FAILED: {e}")
        return 3

    cmd = build_command(merged, client_jar, classpath, natives_dir, args.mc_dir, asset_index, args.username)
    if args.dry_run:
        log("dry run - command:")
        print(" ".join(cmd))
        return 0

    code, text = launch(cmd, args.log, args.timeout, args.display)
    log(f"game process exited with {code}")
    return assess(text, args.log)


if __name__ == "__main__":
    sys.exit(main())