<img width="756" height="260" alt="header" src="https://github.com/Siepert123/create-legacy/blob/rewrite/images/header.png" />

# Create 1.12 — ongoing work to fully port Create to 1.12.2 Forge

> **Status: active work in progress.** This is an ongoing effort to bring
> **every Create feature** to Minecraft 1.12.2 (Forge): kinetics,
> contraptions, crushing, fans, fluids, steam, speed control, displays,
> logistics, elevators, schematics, trains, worldgen, advancements.
> Finished systems are playable; the rest lands feature by feature.
> See [`ROADMAP.md`](ROADMAP.md) for exactly what's done and what's next.
>
> Unofficial backport — not affiliated with the Create team. Please report
> bugs [here](https://github.com/Sekai0NI0itamio/create-1.12/issues), never
> on official Create channels.

**Download (no building needed):** every green `main` push publishes
to the [latest release](https://github.com/Sekai0NI0itamio/create-1.12/releases/latest).

- `create-112-mods.zip` — **easiest**: unzip into `.minecraft/`, everything lands in `mods/`
- `create-112-prism.mrpack` — Prism Launcher: Add Instance → Import → select the file
- `create-1.12.2.jar` — mod alone (add MixinBooter + MelonLib + CTM yourself, JEI optional)

Requires Minecraft 1.12.2 + Forge 14.23.5.2859. Bundle pins: MixinBooter
11.17, MelonLib 1.12.2-1.11.3, CTM 1.0.2.31, JEI 4.16.5.1030 (see
`create-112-manifest.json` for URLs + sha1).

## Credits (full details + license compliance in [`CREDITS.md`](CREDITS.md))

- **[Create](https://github.com/Creators-of-Create/Create) by simibubi and contributors (MIT)** — the original mod: all designs, mechanics, and reference textures. Not affiliated; bugs go here, not upstream.
- **[Create Legacy](https://github.com/Siepert123/create-legacy) by Siepert et al. (MIT)** — the 1.12.2 foundation (imported with history, `upstream` kept).
- **This fork** — crushing wheels, fan, fluids, steam, speed control, displays, logistics, elevators, schematics, trains, advancements, install bundles.
- **Runtime deps** (own licenses, not shipped in our jar): [MixinBooter](https://github.com/CleanroomMC/MixinBooter) by Rongmario/CleanroomMC (LGPL-2.1) · [MelonLib](https://modrinth.com/mod/melonlib) by Siepert123 (MIT) · [CTM](https://www.curseforge.com/minecraft/mc-mods/ctm) by tterrag1098 (GPL-3.0, fetched at pack time, never vendored) · [JEI](https://modrinth.com/mod/jei) by mezz (MIT, optional).
- **MinecraftForge** (LGPL-2.1) loads everything; **Mojang** owns all game assets — we ship code + backport art only.

---

Create Legacy aims to port a version of Create to 1.12.2, keeping it as accurate as the original in terms of gameplay, changing the textures to fit in with 1.12.2 and adjusting some aspects to have it fit in with the 1.12.2 environment.

It can be downloaded from both Modrinth and CurseForge:
- [Modrinth download](https://modrinth.com/mod/create-legacy)
- [CurseForge download](https://www.curseforge.com/minecraft/mc-mods/create-legacy)

The official Create mod for modern Minecraft is also available on those platforms:
- [Modrinth download](https://modrinth.com/mod/create)
- [CurseForge download](https://www.curseforge.com/minecraft/mc-mods/create)

Installing Create legacy is not all that complex. You can either use some sort of dedicated mod manager, like the CurseForge/Modrinth App, or MultiMC, etc. or drag-and-drop the JAR into the mods folder.
Do note that apart from this mod JAR, you will also need to install both MelonLib and ConnectedTexturesMod (CTM).
MelonLib is available on both Modrinth and CurseForge, and also has a GitHub repo:
- [Modrinth download](https://modrinth.com/mod/melonlib)
- [CurseForge download](https://www.curseforge.com/minecraft/mc-mods/melonlib)
- [Github repository](https://github.com/Siepert123/MelonLib)

CTM can only be downloaded from CurseForge, but also has a GitHub repo:
- [CurseForge download](https://www.curseforge.com/minecraft/mc-mods/ctm)
- [GitHub repository](https://github.com/Chisel-Team/ConnectedTexturesMod)

Note that for the latest version of Create Legacy, you may need to enable "Show Alpha Versions" in the CurseForge versions list.
Be aware that snapshots are prone to bugs and crashes!!

And most importantly: have fun!
