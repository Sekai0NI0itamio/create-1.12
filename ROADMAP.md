# Create112 Roadmap — every Create feature on 1.12.2

Target: full Create parity (kinetics, contraptions, processing, logistics,
fluids, trains, schematics, ponder) on MC 1.12.2 / Forge 2859, cloud-built,
official textures.

## Phase 0 — Foundation (DONE)

- Imported Siepert123/create-legacy `rewrite` (MIT) with history into
  Sekai0NI0itamio/create-1.12 (private), upstream remote kept for sync.
- CI: Java 8 + Gradle wrapper (4.9) + FG3 + Forge 2859 (userdev3; 2847 lacks
  it, FG3 cannot use plain userdev). First green build + `latest` release.

## Phase 1 — Stabilize what exists

Verify in-game (survival world, cheats): hand crank → shaft → gearbox →
press makes sheets; water wheel powers belt line; depot/funnel/chute move
items; basin + mixer/deployer/millstone/drill/saw run recipes; bearing +
piston contraptions assemble and move; JEI shows categories; ponder opens.
Fix logic bugs found, keep upstream syncable (small diffs, no rewrites).

## Phase 2 — Missing features (dependency order)

1. Crushing wheels + crushing recipes (needs: basin, JEI category).
2. Encased fan + haunting/washing/smoking/bulk recipes.
3. Spouts, hose pulley, item drain, portable fluid interface.
4. Steam boiler (heat providers, boiler heaters) + steam engine.
5. Mechanical crafter + sequenced assembly recipes.
6. Rotation speed controller, sequenced gearshift, adjustable chain drive.
7. Displays, redstone links, pulse/toggle latches, stock ticker basics.
8. Packager + logistics (stock ticker requests, package routing, factory gauges).
9. Elevators, gantry carriages, rope/pulley contraptions.
10. Schematics + schematicannon + clipboard/blueprints.
11. Trains: tracks, bogeys, carriages, stations, signals, schedules.
12. Worldgen audit (zinc/asurine/etc.), full recipe parity, advancements.

Each item: implement → CI green → in-game verify checklist → release.

## Phase 3 — Texture + polish

- Sync pass vs official Create assets (mc1.18 branch): 29/42 sampled paths
  byte-identical, rest near-identical redraws; replace diverged ones with
  official PNGs resized to 1.12 format where the block exists here.
- Missing-block textures come from official assets when the block lands.
- Credit: Create by simibubi/Creators-of-Create (MIT on ported branches);
  this repo keeps upstream LICENSE + attribution. Unofficial backport,
  not affiliated.
