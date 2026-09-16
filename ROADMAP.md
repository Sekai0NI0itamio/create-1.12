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

1. Crushing wheels + crushing recipes (needs: basin, JEI category). DONE (build 34965877956; verify checklist above).
2. Encased fan + haunting/washing/smoking/bulk recipes. DONE (build 34970315794): push/pull air current (official RPM range table), washing table, haunting, smoking/blasting via furnace/smoker fallback, soul-fire via soul-sand-under-fire, blaze-burner heat levels, 2 SU stress, official fan textures, spinning propeller TESR.
3. Spouts, hose pulley, item drain, portable fluid interface. DONE (build 35038944980): fluid tank block (multiblock height stacking, official textures), spout (fills containers below, right-click refill, 4 SU), hose pulley (extends to 32, drains/infinite water, places back, 4 SU), portable fluid interface (contraption bridging), item drain already existed upstream. Smoking/blasting recipe types registered (furnace/smoker fallback at runtime).
4. Steam boiler (heat providers, boiler heaters) + steam engine. DONE (build 35039791458): boiler level = min(heat, size/4, water/10) sampled on tank bottoms, steam engine 16 RPM at 16 SU/RPM * heat/engines efficiency, official engine model+texture.
5. Mechanical crafter + sequenced assembly recipes. ALREADY UPSTREAM (verified): 3x3 groups, covers, deployer/press/saw steps, precision mechanism chain, 178 recipe registrations.
6. Rotation speed controller, sequenced gearshift, adjustable chain drive.
7. Displays, redstone links, pulse/toggle latches, stock ticker basics.
8. Packager + logistics (stock ticker requests, package routing, factory gauges).
9. Elevators, gantry carriages, rope/pulley contraptions.
10. Schematics + schematicannon + clipboard/blueprints.
11. Trains: tracks, bogeys, carriages, stations, signals, schedules.
12. Worldgen audit (zinc/asurine/etc.), full recipe parity, advancements.

Each item: implement → CI green → in-game verify checklist → release.

## Verify: crushing wheels (do this in game now)

1. Creative: find Crushing Wheel in the Create tab (en_us: "Crushing Wheel").
2. Place two wheels side by side on the same axis with 1 gap? No — adjacent
   (official: vertically one apart means controller gap; here wheels sit
   adjacent along their axis, controller logic internal).
3. Power both with opposite directions (gearbox + shafts, or two hand cranks
   turned opposite). Only the lower-coordinate wheel runs the logic.
4. Drop cobblestone above the gap → gravel pops out below. Drop iron ore →
   crushed iron ore (+bonus rolls). Drop wheat → flour.
5. JEI: "Crushing" category lists all 30 recipes, wheel as catalyst.
6. Goggles show 8.0 SU stress per wheel; overstress stops both.
7. Push a pig between running wheels → damage (official: mobs crushed).
8. Wheel spins in-world (TESR, official plates/insert textures), item model
   in hand/inventory correct, no missing-texture purple.

## Phase 3 — Texture + polish

- Sync pass vs official Create assets (mc1.18 branch): 29/42 sampled paths
  byte-identical, rest near-identical redraws; replace diverged ones with
  official PNGs resized to 1.12 format where the block exists here.
- Missing-block textures come from official assets when the block lands.
- Credit: Create by simibubi/Creators-of-Create (MIT on ported branches);
  this repo keeps upstream LICENSE + attribution. Unofficial backport,
  not affiliated.
