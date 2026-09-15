# AGENTS.md — Create112

## Iron rule: NEVER build the mod locally

- Do NOT run `gradlew`, `gradle build`, `setupDecompWorkspace`, or any compile deobfuscation task on this machine. No exceptions.
- All compilation happens in GitHub Actions (`.github/workflows/build.yml`, Java 8 + Gradle 4.9). Push and read the CI result instead.
- Local `javac` syntax peeks are also off-limits — they produce misleading errors without the Forge MDK and violate this rule's spirit.
- What you MAY do locally: read files, edit sources, write docs, `git status`/`git diff`, `gh` read-only commands.

## Project facts

- Minecraft 1.12.2, Forge 14.23.5.2847 (compile; runs on 2859 clients), ForgeGradle 2.3-style via FG3 userdev, mappings snapshot_20171003, Java 8 (CI-only).
- Remote: https://github.com/Sekai0NI0itamio/create-1.12 (private during backport).
- Origin: fork-import of Siepert123/create-legacy `rewrite` branch (MIT, (c) 2026 Siepert). Upstream remote kept as `upstream` for syncing. All Java from scratch by upstream; no simibubi code vendored. Keep their LICENSE + credit.
- Deps: MixinBooter 10.7 (runtime mixin host), MelonLib + CTM at runtime, JEI/CraftTweaker compile-only.
- Backport target: every Create feature playable on 1.12.2 — kinetics/SU/speed, contraptions, belts/funnels, processing (press/crush/mix/drill/saw/deployer), logistics, fluids, trains/elevators, schematics + cannon, ponder, worldgen (zinc etc.), JEI integration.
