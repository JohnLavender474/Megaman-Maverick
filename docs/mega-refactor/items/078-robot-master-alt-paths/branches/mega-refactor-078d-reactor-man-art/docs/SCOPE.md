# Scope: mega-refactor-078d-reactor-man-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file. If the scope changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-19

## Goal

The Reactor Man stage (`REACTOR_MAN`, `assets/tiled_maps/tmx/ReactorMan_v3.tmx`) keeps its tile shapes, but gets
motion and refined art: one palette for all walls and tiles, one palette for all water, one moving motif per area,
decor entities where tiles cannot show the motion, and a background per area where it needs one (owner, 2026-11-19,
after the round-1 review).

## Item

`078` (stage redesigns), section "All stages: tileset art and backgrounds". The owner decisions of this branch are in
`docs/NOTES.md` ("Decisions").

## Current state

Scan of `master` `fdefeadd1`, 2026-11-19.

- Map: 500 x 200 tiles, 19 rooms. Tile layers `tiles2` (under) and `tiles1`. No tile animations.
- Tilesets: `Reactor_Man_No_Black` (embedded and also as `.tsx`), `React_Man_2`, `Reactor_Man_1`,
  `Reactor_Man_3`, `Reactor_Man_4`, `Tileset1` (used in 9 rooms). `Reactor_Man_3` is also linked by
  `WilyStage1_v2.tmx` and `Test1.tmx`, and `Tileset1` by 11 live levels: this branch does not edit them.
- One background object `CityBKG`.
- Moving parts today are entities only: `ToxicWater` (8), `ToxicWaterfall` (13), `TubeBeamerV2` (21), `Beamer`
  (14), `AcidGoopSupplier` (11), `DrippingToxicGoop` (10).
- Look: green-grey steel and magenta pipes and domes in every room, so the areas read the same.
- Before-renders: `temp/078-reactor-art/_before/` (main checkout).

Areas (proposal; the art round confirms):

| Area | Rooms | Accent (proposal) | Motif (proposal) |
|---|---|---|---|
| Entrance catwalks | `room1` to `room3` | steel blue, night | blinking catwalk lamps, city background |
| Coolant hall | `room4` to `room7`, `alt_path_1_room_1`, `alt_path_1_mini_boss_room_1`, `mini_boss_room_1` | green coolant | coolant flow in pipes, dome pulse |
| Upper works | `room8` to `room10` | amber | laser tubes, sparking panels |
| Waterfall shaft | `room11` to `room13`, `mini_boss_room_2` | cyan | waterfalls, water shimmer |
| Core | `room14`, `boss_room` | magenta and white glow | reactor core pulse, idle beamers |

## In scope

The final art is round 3 in `temp/078-reactor-art/round3/final/` (main checkout), rebuilt by `make_reactor_r3.py`;
`STYLE.md` "Round 3 (final)" lists each file and its target.

1. One merged tileset (new `.tsx` and `.png` in `assets/tiled_maps/`): the 5 Reactor Man tilesets in the refined
   `classic` palette, the 12 used `Tileset1` tiles, the animation frames as Tiled animations
   (`final/tiles_index.tsv`). A remap script in `temp/` repaints `tiles1` and `tiles2` by a gid mapping and the
   clear-black rule, and adds a `decor` tile layer. The map then links only the new tileset.
   Round 4 (R1): a new layout of the non-solid cells of the indoor areas replaces the master cells there.
2. Water: new `ToxicWater/waves`, `ToxicWater/water` (`Specials1`), `ToxicWaterfall` (`Decorations1`) and
   `Water/ToxicSplash` (`Environs1`) in the `acid` palette. The region sizes change; the code splits each region
   2 x 2 already, so no code change.
3. Backgrounds: 5 regions (`ReactorCityBKG`, `ReactorHallBKG`, `ReactorWorksBKG`, `ReactorShaftBKG`,
   `ReactorCoreBKG`) in the new atlas `Backgrounds11` (D1). 5 background objects of 512 x 448 (the `CityBKG` object changes per
   `city/NOTES.md`; 4 new objects per `final/backgrounds.tsv`) with show/hide triggers where 2 areas meet.
4. Decor entities in `entities/decorations/`: `DecorLaserTube` (height from the map object, a phase per object) and
   `SteamVent` (8 frames, a wait between puffs, brief D3); regions in `Decorations1`; objects in the `decorations`
   layer (placed by the script, D2); `entity-tags.txt` updated. First caller: the map.
5. Credits in `README.md` (`docs/ways-of-working/art.md`): the tileset update by Claude Fable 5.2 on the original
   Reactor Man tiles (original credit as the README has it); the water, backgrounds, city and decor made by Claude
   Fable 5.2, directed by the developer. `BGNeedle` (Capcom, Mega Man 3) needs no credit (owner). The old water art is the
   developer's own work and needs no credit (owner, 2026-11-19).

## Out of scope

- Gameplay: block geometry, spawns, rooms, camera, hazards.
- The `TubeBeamerV2` beam art and any redraw of its emitter tiles (`077d`). The 15 beamer tube tiles only take the
  palette map, which leaves them equal to today (round 3 check). The water art
  (`ToxicWater`, `ToxicWaterfall`) is in scope: only this map uses them among the live levels.
- `Tileset1`, `Reactor_Man_3` and the other shared tileset files, and other maps.

## Affected areas and risks

- `ReactorMan_v3.tmx`, a new `.tsx` and `.png`, possibly a new background atlas and decor entity classes,
  `TextureAsset.kt`, `README.md`, `core/src/smokeTest/resources/entity-tags.txt` for a new entity.
- A decor laser tube can look like the `TubeBeamerV2` hazard: the art must make decor read as background.
- A recolor can make a wall look open or air look solid: the script checks it, as in `078b`.

## Overlap with other branches

- `077d` (planned, Reactor Man fight): redraws the `TubeBeamerV2` beam and the ceiling emitter tiles
  (`Reactor_Man_3` tiles 210 and 211). This branch copies those tiles unchanged into the merged tileset, so `077d`
  then edits them in the merged tileset for this map (and in `Reactor_Man_3` for Wily Stage 1).
- `078c` (active, Wily Stage 3 enemy skins): no shared files. A new `TextureAsset` entry can give a simple list
  conflict.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O4` deeper levels | helps | each area gets its own identity |
| `O9` player experience | helps | motion and distinct areas |
| `O2-R2` random calls through the engine API | no effect | a decor entity that needs random timing uses the engine API |
| A branch keeps all behavior that its contract does not name | no effect | art and decor only |

## Open questions

None. Brief answers (2026-11-19, owner accepted the recommendations):

- D1: the 5 backgrounds go in a new atlas `Backgrounds11` (new `TextureAsset.BACKGROUNDS_11`); `Backgrounds4` stays.
- D2: the map script places `DecorLaserTube` and `SteamVent` by the `STYLE.md` list; the owner adjusts in Tiled.
- D3: `SteamVent` waits a random time between puffs, from `services.random`.

Play test 1, round 4 (2026-11-19, owner accepted the recommendations):

- R1: the redesign may repaint the non-solid cells of `tiles2` and `tiles1` (a new layout of pipes, girders and
  beams; each run starts and ends at a mount). New tile pieces (elbows, tees, caps, brackets) are allowed. The
  block objects and the look of the solid cells stay.
- R2: railing lights chase by x position: the lamp in column c is lit at step (c mod 4), 100 ms per step.
- R3: pipe flow per run, from its source end to its sink end; elbows and tees get frames for that direction.
- R4: reactor cores: a framed window glows; the dome body stays dark.

Play test 1, background pop (2026-11-20, owner accepted the recommendation):

- R5: the area backgrounds switch on room entry (`spawn_type=spawn_room`). The city to hall switch is in `room3`
  (city on `room1`, hall from `room3`; `room2` keeps the last one), because `room2` and `room3` have no clear pixel,
  so no pan shows the swap there. The other area borders keep a pop on a backtrack; the fix (backgrounds drawn per
  room) is a backlog candidate in `docs/TODO.md`, not this branch.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] Map script checks: every block object and every non-tile object other than the background objects, their
  triggers and the new decor objects is the same as on `master`.
- [ ] Atlas script checks: every old region of each repacked atlas keeps its name and pixels; every region that
  the map and the code name exists.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

From `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-078d-reactor-man-art`:

- `./run-game-alpha.sh --level REACTOR_MAN --room room1`: the night city with blinking beacons behind the
  catwalks; railing lights run.
- `./run-game-alpha.sh --level REACTOR_MAN --room room4`: pipe flow, dome pulse, acid water, steam vents, hall
  background.
- `./run-game-alpha.sh --level REACTOR_MAN --room room5`: acid waterfalls and splash.
- `./run-game-alpha.sh --level REACTOR_MAN --room room10`: decor laser tubes, sparks, works background; the hazard
  beams still read as hazards.
- `./run-game-alpha.sh --level REACTOR_MAN --room room12`: shaft background with far falls, girder trickle.
- `./run-game-alpha.sh --level REACTOR_MAN --room room13`: the tall shaft; no wall looks open, no air looks solid.
- `./run-game-alpha.sh --level REACTOR_MAN --room room14 --heartTanks all --healthTanks all`: core background
  pulse, then the Reactor Man fight with the beamer ceiling.

## Behaviors to check

- Hazards, water and beamers act as before.

## Intended changes

- Reactor Man stage art: tiles, tile animations, decor entities, backgrounds. No gameplay change.
