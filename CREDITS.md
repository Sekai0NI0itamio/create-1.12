# Credits & Licenses — Create 1.12

This is an **ongoing unofficial backport** bringing the Create mod to
Minecraft 1.12.2 (Forge). It is not affiliated with, endorsed by, or
connected to the official Create team. Please report bugs **here**, never
on the official Create issue tracker or Discord.

## This project

- **Create 1.12** — continued backport work in this repository
  (crushing wheels, encased fan, fluids, steam, speed control, displays,
  logistics, elevators, schematics, trains, advancements, install bundles).
  Same terms as the codebase it continues: **MIT** (see `LICENSE`).
  Status: work in progress — expect rough edges; see `ROADMAP.md`.

## Code we build on

- **[Create Legacy](https://github.com/Siepert123/create-legacy) by Siepert
  and contributors** — the 1.12.2 foundation (imported with history;
  `upstream` remote kept for syncing). **MIT**, Copyright (c) 2026 Siepert.
  All Java here is from-scratch backport code; no official Create sources
  are vendored.
- **[Create](https://github.com/Creators-of-Create/Create) by simibubi and
  contributors** — the original mod this ports. **Fully MIT** (code +
  assets — verified in their `LICENSE.md`, which covers "this software
  and associated documentation files"). So both mechanics AND official
  art (textures, models, sounds) may be ported into this repo with the
  MIT copyright + permission notice kept (reproduced below and in
  `LICENSE`).

## Runtime dependencies (not shipped in our jar; fetched by `pack/pack.py`
## or installed by hand — their own licenses apply)

| Mod | Author | License | Role |
|---|---|---|---|
| [MixinBooter](https://github.com/CleanroomMC/MixinBooter) 11.17 (zone.rong) | Rongmario / CleanroomMC | **LGPL-2.1-only** | Mixin host (required) |
| [MelonLib](https://modrinth.com/mod/melonlib) 1.12.2-1.11.3 | Siepert123 / MelonStudios | **MIT** | Library (required) |
| [CTM](https://www.curseforge.com/minecraft/mc-mods/ctm) 1.0.2.31 | tterrag1098 (Chisel Team) | **GPL-3.0** | Connected textures (required for full visuals) |
| [JEI](https://modrinth.com/mod/jei) 4.16.5.1030 | mezz | **MIT** | Recipe viewer (optional) |

Notes for the license-conscious:

- **MIT** (this repo, Create Legacy, Create, MelonLib, JEI): keep the
  copyright + permission notices (done — `LICENSE` + this file).
- **LGPL-2.1** (MixinBooter, and MinecraftForge itself, which loads
  everything): we link, never modify or embed — separate jars, dynamic
  linking only. Compliant as a "work that uses the Library".
- **GPL-3.0** (CTM): we do **not** distribute it — the bundler downloads it
  from CurseForge at pack time and the manifest links its page. Users
  receive it under its own GPL-3.0 terms from its author.
- **Mojang/Minecraft**: game assets belong to Mojang; this project ships
  code and backport art only — no game files, no decompiled sources.

## Compliance checklist (for reviewers)

- [x] Upstream `LICENSE` (MIT, Siepert) kept at repo root, Create Team
      attribution added (both MIT).
- [x] Official Create code + art ported under their MIT terms (notices kept).
- [x] No GPL code vendored (CTM fetched at pack time, never committed).
- [x] LGPL deps linked as separate jars, unmodified.
- [x] Attribution in README + in-game `mcmod.info` credits + this file.
- [x] Bug reports directed here, not upstream (README + this file).

## Third-party license texts

### Create (code + assets) — MIT, Copyright (c) The Create Team / The Creators of Create

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
