#!/usr/bin/env python3
"""Bundle the Create 1.12.2 mod + pinned dependencies into installable packs.

Outputs (in dist/):
  create-112-mods.zip        -> unzip straight into .minecraft/, mods land in mods/
  create-112-prism.mrpack    -> Prism Launcher: Import -> follow prompts
  create-112-manifest.json   -> pinned file list with URLs + sha1 (audit trail)

Dependency pins (Forge 1.12.2):
  MixinBooter  !mixinbooter-11.17.jar   (Modrinth, required bootstrapping)
  MelonLib     melonlib-1.12.2-1.11.3.jar (Modrinth, required library)
  CTM          CTM-MC1.12.2-1.0.2.31.jar  (CurseForge file 2915363, textures)
  JEI          jei_1.12.2-4.16.5.1030.jar (Modrinth, optional recipe viewer)

Usage: pack.py <create-jar> [--out dist] [--mc 1.12.2] [--forge 14.23.5.2859]
Env: MODRINTH_TOKEN optional (raises rate limits for dependency check).
"""
import argparse
import hashlib
import json
import os
import shutil
import sys
import urllib.request
import zipfile

UA = {"User-Agent": "create-112-pack/1.0 (+github.com/Sekai0NI0itamio/create-1.12)"}

DEPS = [
    {
        "name": "MixinBooter",
        "file": "!mixinbooter-11.17.jar",
        "url": "https://cdn.modrinth.com/data/G1ckZuWK/versions/6jJK1B2d/%21mixinbooter-11.17.jar",
        "side": "both",
        "required": True,
    },
    {
        "name": "MelonLib",
        "file": "melonlib-1.12.2-1.11.3.jar",
        "url": "https://cdn.modrinth.com/data/UUXMxc2X/versions/z24Kifmj/melonlib-1.12.2-1.11.3.jar",
        "side": "both",
        "required": True,
    },
    {
        "name": "CTM",
        "file": "CTM-MC1.12.2-1.0.2.31.jar",
        "url": "https://edge.forgecdn.net/files/2915/363/CTM-MC1.12.2-1.0.2.31.jar",
        "side": "client",
        "required": True,
    },
    {
        "name": "JEI",
        "file": "jei_1.12.2-4.16.5.1030.jar",
        "url": "https://cdn.modrinth.com/data/u6dRKJwZ/versions/ys10FvNX/jei_1.12.2-4.16.5.1030.jar",
        "side": "both",
        "required": False,
    },
]


def sha1_of(path):
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch(url, dest):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=120) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("create_jar")
    ap.add_argument("--out", default="dist")
    ap.add_argument("--mc", default="1.12.2")
    ap.add_argument("--forge", default="14.23.5.2859")
    ap.add_argument("--version", default="26w19b")
    args = ap.parse_args()

    if not os.path.isfile(args.create_jar):
        print(f"missing create jar: {args.create_jar}", file=sys.stderr)
        return 1

    os.makedirs(args.out, exist_ok=True)
    dl = os.path.join(args.out, "deps")
    os.makedirs(dl, exist_ok=True)

    manifest = {
        "pack": "create-1.12",
        "version": args.version,
        "minecraft": args.mc,
        "forge": args.forge,
        "files": [],
    }

    create_dest = os.path.join(dl, "create-1.12.2.jar")
    shutil.copyfile(args.create_jar, create_dest)
    manifest["files"].append({
        "name": "Create 1.12 (this repo)",
        "file": "create-1.12.2.jar",
        "sha1": sha1_of(create_dest),
        "side": "both",
        "required": True,
    })

    for dep in DEPS:
        dest = os.path.join(dl, dep["file"])
        if not os.path.isfile(dest) or os.path.getsize(dest) < 50000:
            print(f"downloading {dep['name']} ...")
            fetch(dep["url"], dest)
        size = os.path.getsize(dest)
        if size < 50000:
            print(f"ERROR: {dep['file']} too small ({size}b), download failed", file=sys.stderr)
            return 1
        if not dest.endswith(".jar") or not zipfile.is_zipfile(dest):
            print(f"ERROR: {dep['file']} is not a valid jar", file=sys.stderr)
            return 1
        manifest["files"].append({
            "name": dep["name"],
            "file": dep["file"],
            "url": dep["url"],
            "sha1": sha1_of(dest),
            "side": dep["side"],
            "required": dep["required"],
        })
        print(f"ok {dep['file']} ({size}b)")

    # 1. Plain mods zip: unzip into .minecraft/
    mods_zip = os.path.join(args.out, "create-112-mods.zip")
    with zipfile.ZipFile(mods_zip, "w", zipfile.ZIP_DEFLATED) as z:
        for entry in sorted(os.listdir(dl)):
            z.write(os.path.join(dl, entry), f"mods/{entry}")
        z.writestr("README.txt",
                   "Create 1.12.2 bundle\nUnzip into .minecraft/ (mods/ land automatically).\n"
                   f"Minecraft {args.mc} + Forge {args.forge} required.\n"
                   "JEI optional; MixinBooter+MelonLib+CTM required.\n")

    # 2. Prism Launcher .mrpack
    mrpack = os.path.join(args.out, "create-112-prism.mrpack")
    overrides = []
    for entry in sorted(os.listdir(dl)):
        overrides.append(f"overrides/mods/{entry}")
    modrinth_index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": f"create-112-{args.version}",
        "name": f"Create 1.12 ({args.version})",
        "dependencies": {"minecraft": args.mc, "forge": args.forge},
        "files": [],
    }
    with zipfile.ZipFile(mrpack, "w", zipfile.ZIP_DEFLATED) as z:
        for entry in sorted(os.listdir(dl)):
            src = os.path.join(dl, entry)
            z.write(src, f"overrides/mods/{entry}")
            meta = next((m for m in manifest["files"] if m["file"] == entry), {})
            zinfo_env = {"client": "optional", "server": "optional"}
            modrinth_index["files"].append({
                "path": f"overrides/mods/{entry}",
                "hashes": {"sha1": sha1_of(src)},
                "env": zinfo_env,
                "downloads": [meta.get("url", "")] if meta.get("url") else [],
                "fileSize": os.path.getsize(src),
            })
        z.writestr("modrinth.index.json", json.dumps(modrinth_index, indent=2))

    man_path = os.path.join(args.out, "create-112-manifest.json")
    with open(man_path, "w") as f:
        json.dump(manifest, f, indent=2)

    print(f"wrote {mods_zip}, {mrpack}, {man_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
