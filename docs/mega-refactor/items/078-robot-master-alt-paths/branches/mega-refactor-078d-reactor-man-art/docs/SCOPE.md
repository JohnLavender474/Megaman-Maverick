# Scope: mega-refactor-078d-reactor-man-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file. If the scope changes later, update this file and the brief, and ask the owner again.

- Status: draft (short design approved by the owner on 2026-11-19; the brief follows the round-1 art review)
- Approved: no

## Goal

The Reactor Man stage (`REACTOR_MAN`, `assets/tiled_maps/tmx/ReactorMan_v3.tmx`) keeps its tile shapes, but gets
motion and a clear identity for each area: an accent palette and one moving motif per area, decor entities where
tiles cannot show the motion, and a background per area where it needs one.

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

1. One merged tileset for the map, built by a script in `temp/078-reactor-art/`: the base tiles copied from the 6
   current tilesets, an accent recolor per area, and Tiled animation frames. The script repaints `tiles1` and
   `tiles2` by a gid mapping and adds a new `decor` tile layer. The map links only the new tileset afterwards.
2. Tile animations: the motif of each area.
3. Decor entities (no gameplay effect) only for motion that tiles cannot show, for example a laser tube with a
   moving beam or a steam vent. Each new class has its first caller in the map.
4. Backgrounds: one per area where the area needs it.
5. Credits in `README.md` (`docs/ways-of-working/art.md`): the original Reactor Man tileset and the update by Claude
   Fable 5.2, directed by the developer.

## Out of scope

- Gameplay: block geometry, spawns, rooms, camera, hazards.
- The art of `TubeBeamerV2`, its emitter tiles and `ToxicWater` (`077d` owns the laser and emitter art).
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

- The final area split, accents and motifs: round-1 art review.
- The decor entity list: round-1 art review.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] Map script checks: every block object and every non-tile object other than the background objects, their
  triggers and the new decor objects is the same as on `master`.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Filled in after the brief.

## Behaviors to check

- Hazards, water and beamers act as before.

## Intended changes

- Reactor Man stage art: tiles, tile animations, decor entities, backgrounds. No gameplay change.
