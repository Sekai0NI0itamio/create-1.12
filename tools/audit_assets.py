#!/usr/bin/env python3
"""Static asset audit for the Create 1.12 port. Fails on any gap that would
log a model/texture error in-game.

Resolution rules (verified against real 1.12.2 Forge behavior):
- Blockstate variant model `create:P` loads `models/block/P.json`
  (Forge prepends `block/`; refs must NOT contain the prefix themselves).
- Model parent `create:P` (in block or item models) loads `models/P.json`
  exactly, as does `minecraft:P`.
- Texture `create:T` must exist as `textures/T.png`. Domain-less `block/*`
  paths resolve to our domain; domain-less `blocks/*`, `item(s)/*`,
  `entity/*`, `gui/*` fall back to vanilla and are not checked.
- Every registered BLOCK needs `blockstates/<name>.json`.
- Every block with a plain ItemBlock needs `models/item/<name>.json`.
  Variant items (ItemBlockVariants / explicit setItemModel mappings) use
  per-meta files under `models/item/<name>/` instead.
- Items registered in ItemInit need `models/item/<name>.json` or a
  `models/item/<name>/` directory with per-meta files.
- Every blockstate/model JSON file must parse.

Usage: audit_assets.py [--root src/main/resources/assets/create]
Exit: 0 clean, 1 gaps found (printed).
"""
import argparse
import glob
import json
import os
import re
import sys

VANILLA_TEXTURE_PREFIXES = ("blocks/", "item/", "items/", "entity/", "gui/",
                            "font/", "misc/", "paintings/", "particles/",
                            "mob_effect/", "map/", "colormap/")


def split_domain(ref):
    if ":" in ref:
        return ref.split(":", 1)
    return None, ref


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default="src/main/resources/assets/create")
    args = ap.parse_args()
    r = args.root
    java = os.path.join(os.path.dirname(os.path.abspath(r)),
                        "..", "..", "java", "nl", "melonstudios", "create")
    java = os.path.normpath(java)
    gaps = []

    def fail(msg):
        gaps.append(msg)

    # ---- collect registry names -------------------------------------------
    chained = {}   # class -> name via BlockInit/ItemInit chaining (approx by order)
    ctor_names = {}
    for f in glob.glob(os.path.join(java, "**", "*.java"), recursive=True):
        s = open(f, errors="replace").read()
        for m in re.findall(r"setRegistryName\(\"([^\"]+)\"\)", s):
            ctor_names.setdefault(m, f)

    block_names = set()
    src = open(os.path.join(java, "init", "BlockInit.java")).read()
    for m in re.finditer(r"registerBlock(?:WithItem)?\(\s*new (\w+)\(", src):
        cls = m.group(1)
        # balance parens from the call to find chained name / extra args
        i = src.index("(", m.start())
        depth, j = 0, i
        while j < len(src):
            if src[j] == "(":
                depth += 1
            elif src[j] == ")":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        call = src[m.start():j + 1]
        chained_m = re.search(r"\.setRegistryName\(\"([^\"]+)\"\)", call)
        name = chained_m.group(1) if chained_m else None
        if name is None:
            # constructor-set name: find class file
            for f in glob.glob(os.path.join(java, "**", f"{cls}.java"), recursive=True):
                s2 = open(f, errors="replace").read()
                cm = re.search(r"setRegistryName\(\"([^\"]+)\"\)", s2)
                if cm:
                    # BlockOrestone-style: name + suffix
                    raw = cm.group(1)
                    name = raw
                    break
        if name and "/" not in name:
            block_names.add(name)

    item_names = set()
    ipath = os.path.join(java, "init", "ItemInit.java")
    if os.path.isfile(ipath):
        s = open(ipath, errors="replace").read()
        for m in re.findall(r"setRegistryName\(\"([^\"]+)\"\)", s):
            if "/" not in m:
                item_names.add(m)
    # constructor-named items (ItemIngredient, ItemAssembly, ItemBlockFunnel, ...)
    for f in glob.glob(os.path.join(java, "item", "*.java")):
        s = open(f, errors="replace").read()
        for m in re.findall(r"setRegistryName\(\"([^\"]+)\"\)", s):
            if "/" not in m:
                item_names.add(m)

    # ---- helpers ------------------------------------------------------------
    seen_models = set()

    def check_texture(t, via):
        dom, p = split_domain(t)
        if dom == "minecraft" or t.startswith("#"):
            return
        if dom is None:
            if t.startswith("block/"):
                p = t
            else:
                return  # vanilla-domain path, game falls back to vanilla
        elif dom != "create":
            return
        if not os.path.isfile(os.path.join(r, "textures", p + ".png")):
            fail(f"texture {p} (via {via})")

    def check_parent(p, via):
        if not p or p.startswith("builtin/"):
            return
        dom, path = split_domain(p)
        if dom is not None and dom not in ("create", "minecraft"):
            return
        if dom == "minecraft":
            return  # vanilla parent, assumed present
        if dom is None:
            return  # domain-less parent resolves to vanilla in practice
        if not os.path.isfile(os.path.join(r, "models", path + ".json")):
            fail(f"parent model {path} (via {via})")

    def check_block_model(path, via):
        f = os.path.join(r, "models", "block", path + ".json")
        if not os.path.isfile(f):
            fail(f"model {path} (via {via})")
            return
        if f in seen_models:
            return
        seen_models.add(f)
        try:
            d = json.load(open(f))
        except Exception as e:  # noqa: BLE001
            fail(f"{path}: bad json ({e})")
            return
        if d.get("parent"):
            check_parent(d["parent"], path)
        for v in (d.get("textures") or {}).values():
            if isinstance(v, str) and not v.startswith("#"):
                check_texture(v, path)

    # ---- blockstates ---------------------------------------------------------
    for n in sorted(block_names):
        bs = os.path.join(r, "blockstates", n + ".json")
        if not os.path.isfile(bs):
            # Blocks without BlockState (pure TESR dummies) still need the file
            # if they are real blocks; report and let the author confirm.
            fail(f"blockstate {n}")
            continue
        try:
            d = json.load(open(bs))
        except Exception as e:  # noqa: BLE001
            fail(f"{n}: blockstate bad json ({e})")
            continue
        for vname, v in (d.get("variants") or {}).items():
            ms = []
            if isinstance(v, dict) and "model" in v:
                ms = [v["model"]]
            elif isinstance(v, list):
                ms = [e["model"] for e in v if isinstance(e, dict) and "model" in e]
            for mref in ms:
                if mref.startswith("minecraft:"):
                    continue
                dom, p = split_domain(mref)
                if dom not in (None, "create"):
                    continue
                if p.startswith("block/"):
                    # Forge prepends block/ itself; an explicit prefix doubles
                    # the path and can never resolve.
                    fail(f"{n}#{vname}: model ref {mref!r} contains redundant block/ prefix")
                    continue
                check_block_model(p, f"{n}#{vname}")
        for part in d.get("multipart") or []:
            apply = part.get("apply") or {}
            if isinstance(apply, dict) and "model" in apply:
                mref = apply["model"]
                if not mref.startswith("minecraft:"):
                    dom, p = split_domain(mref)
                    if p.startswith("block/"):
                        fail(f"{n} multipart: model ref {mref!r} contains redundant block/ prefix")
                    elif dom in (None, "create"):
                        check_block_model(p, f"{n} multipart")

    # ---- item models ----------------------------------------------------------
    for f in glob.glob(os.path.join(r, "models", "item", "**", "*.json"), recursive=True):
        try:
            d = json.load(open(f))
        except Exception as e:  # noqa: BLE001
            fail(f"{os.path.relpath(f, r)}: bad json ({e})")
            continue
        p = d.get("parent")
        if p:
            check_parent_model_exact(p, f, fail, r)

    # Blocks with a plain ItemBlock must have models/item/<name>.json.
    # Variant items (ItemBlockVariants/custom mesh) and item-less blocks do not.
    bsrc = open(os.path.join(java, "init", "BlockInit.java")).read()
    ctor_name = {}
    for f in glob.glob(os.path.join(java, "**", "*.java"), recursive=True):
        s = open(f, errors="replace").read()
        cls_m = re.search(r"public (?:final )?class (\w+)", s)
        nm = re.search(r"setRegistryName\(\"([^\"]+)\"\)", s)
        if cls_m and nm and "/" not in nm.group(1):
            ctor_name.setdefault(cls_m.group(1), nm.group(1))

    def outer_call(text, start):
        """Return (full_call_text, inner_text) for the call starting at start."""
        i = text.index("(", start)
        depth, j = 0, i
        while j < len(text):
            if text[j] == "(":
                depth += 1
            elif text[j] == ")":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        return text[start:j + 1], text[i + 1:j]

    def top_comma_split(inner):
        depth, in_str, parts, cur = 0, False, [], ""
        for ch in inner:
            if ch == '"':
                in_str = not in_str
            if not in_str:
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                elif ch == "," and depth == 0:
                    parts.append(cur)
                    cur = ""
                    continue
            cur += ch
        parts.append(cur)
        return parts

    for m in re.finditer(r"registerBlock(WithItem)?\(\s*new (\w+)\(", bsrc):
        with_item = m.group(1) is not None
        cls = m.group(2)
        _call, inner = outer_call(bsrc, m.start())
        args = top_comma_split(inner)
        second = args[1].strip() if len(args) > 1 else None
        chained = re.search(r"\.setRegistryName\(\"([^\"]+)\"\)", _call)
        name = chained.group(1) if chained else ctor_name.get(cls)
        if not name or "/" in name:
            continue
        if not with_item:
            continue  # no ItemBlock at all
        if second:
            continue  # variants flag or custom ItemBlock factory
        if not os.path.isfile(os.path.join(r, "models", "item", name + ".json")):
            fail(f"block {name} has a plain ItemBlock but no models/item/{name}.json")

    # Standalone items need a file or a per-meta directory. Per-meta layouts
    # may nest one level deeper (models/item/<group>/<form>/<meta>.json),
    # which counts when the directory components rejoin to the item name.
    for n in sorted(item_names):
        f = os.path.join(r, "models", "item", n + ".json")
        d = os.path.join(r, "models", "item", n)
        has_dir = os.path.isdir(d) and any(
            os.path.isfile(os.path.join(dp, fn))
            for dp, _, fns in os.walk(d) for fn in fns)
        nested = False
        if not os.path.isfile(f) and not has_dir:
            for dp, _, fns in os.walk(os.path.join(r, "models", "item")):
                if not fns:
                    continue
                rel = os.path.relpath(dp, os.path.join(r, "models", "item"))
                if "_".join(rel.split(os.sep)) == n:
                    nested = True
                    break
        if not os.path.isfile(f) and not has_dir and not nested:
            fail(f"item {n} has no model file or per-meta directory")

    print(f"blocks: {len(block_names)}, standalone items: {len(item_names)}")
    if gaps:
        print(f"GAPS: {len(set(gaps))}")
        for g in sorted(set(gaps)):
            print(" ", g)
        return 1
    print("OK: no asset gaps")
    return 0


def check_parent_model_exact(p, via, fail, r):
    if not p or p.startswith("builtin/"):
        return
    dom, path = split_domain(p)
    if dom == "minecraft" or dom is None:
        return
    if dom != "create":
        return
    if not os.path.isfile(os.path.join(r, "models", path + ".json")):
        fail(f"item-parent model {path} (via {os.path.basename(via)})")


if __name__ == "__main__":
    sys.exit(main())
