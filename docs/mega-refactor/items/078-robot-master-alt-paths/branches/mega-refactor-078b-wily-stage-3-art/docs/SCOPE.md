# Scope: mega-refactor-078b-wily-stage-3-art

- Status: approved
- Approved: 2026-11-19

## Goal

Wily Stage 3 (`WILY_STAGE_3`, `assets/tiled_maps/tmx/WilyStage3_v4.tmx`) gets its new art: one new tileset in the
mood of the X6 Gate stages ("the last lab"), the repainted map with a new `decor` layer, new background objects,
and a `lab` type of `ConveyorBelt` with its own frames.

## Item

`078` (stage redesigns), section "Wily Stage 3" of `ITEM.md`. All owner decisions of the art phase are in this
branch `docs/NOTES.md` ("Decisions") and in `temp/078-wily3-art/STYLE.md`.

## Current state

- Art drafts (git-ignored, main checkout): `temp/078-wily3-art/`. Round 2 is the base: `make_wily3_r2.py`
  rebuilds all output; `mapping2.tsv` maps the 14 old gids to new tiles by position rules; `checks2.txt` has 49
  PASS. Round 2 still shows the Timber Woman plate in `precious_woman_room`; owner decision Q1 moves it to `room7`
  (`3-refight/round2/option_both_plates_in_room7.png`).
- Map: 16 rooms; tile layers `tiles1` (11 distinct tiles), `tiles2` (4), and 9 `fade_tiles_*` layers (opacity 0.1
  to 0.9, one tile each: a fade from black in `intro_room`). Tilesets `Tileset1` (shared by 11 live levels) and
  `mm9endless`. One background object `Background` (`Backgrounds3`, region `BKG18`, `parallax_y` 0.1).
- The level renderer draws every tile layer in map order and plays Tiled tile animations (`078a`), so the new
  `decor` layer and the animated trace need no code.
- Background objects (`BackgroundLayerBuilder.kt`): the object name is the show/hide key; an object with
  `animation_rows` is animated (`078a`). The new backgrounds are static.
- `ConveyorBelt` (`entities/blocks/ConveyorBelt.kt`): the spawn property `type` picks the regions in `Platforms1`
  (`default`, `green`); each type has an `AnimationDef`. The 15 belts of the map use `type=green` (Glacier Man art).
- Atlases: `Backgrounds8` (1922 x 1346) and `Backgrounds9` (1024 x 1922) are near the page limit; `Platforms1`
  (482 x 1098) has room.

## In scope

1. Tileset and map: the round-2 tileset (shared, area tiles, trace animations) as one new tileset in
   `assets/tiled_maps/`; repaint `tiles1`, `tiles2` and the 9 fade layers by `mapping2.tsv`; a new `decor` tile layer
   after `tiles1` for the trace, the emblem plates and the vents; the plates per Q1 (`room4`, `room6`, and 2 in
   `room7`). Remove `Tileset1` and `mm9endless` from this map when no cell uses them. Same file name, so
   `LevelDefinition` does not change.
2. Backgrounds: 3 static images (`bg_lab_wall`, `bg_furnace_glow`, `bg_core_field`) in a new atlas `Backgrounds10`
   (frames folder, `.tps` with `Basic`, `TextureAsset.BACKGROUNDS_10`). Objects: lab wall for the entrance, the
   refight hall and the upper parts of `room3` and `room5`; furnace glow for `room1`, `room2` and the bottom 14 rows
   of `room3` and `room5` (`parallax_y` 1, rows per `backgrounds/round2/placement.tsv`); core field for `room9` and
   `wily_room`. Show/hide triggers where 2 backgrounds meet. The `BKG18` object goes.
3. Conveyor belt: a `lab` type in `ConveyorBelt` (regions `*-Lab`, same `AnimationDef` as `green`); the 6 frames
   in `assets/sprites/frames/Platforms1/ConveyorBelt/`; `Platforms1` repacked; `type=lab` on the 15 belts of the
   map. First caller of the new type: the map.
4. Credits in `README.md` (`docs/ways-of-working/art.md`): the tileset, the backgrounds and the belt, made by Claude
   Fable 5.2, directed by the developer.

## Out of scope

- Gameplay: block geometry, spawns, rooms, camera, hazards. The map keeps every block object.
- Enemy and hazard sprite skins for this stage (decision D1 of the brief).
- `Tileset1` itself (11 live levels use it) and other maps.

## Affected areas and risks

- `WilyStage3_v4.tmx`, a new `.tsx` and `.png`, `Backgrounds10` (new), `Platforms1` (repack), `TextureAsset.kt`,
  `ConveyorBelt.kt`, `README.md`.
- A tile in an air cell can look solid, or a wall tile can look open: `checks2.txt` checks it on the drafts; the
  branch reruns the checks on the final map.
- The show/hide moment of a background in a room pan can show 2 backgrounds or none for a short time (as in `078a`).
- The `Platforms1` repack must keep every old region with its name and pixels (script check, `assets.md`).

## Overlap with other branches

- `077c` (active, `ElecDevil`, Wily Stage 1): no shared files today. If it adds a `TextureAsset` entry, the merge
  second resolves a simple list conflict.
- `060k` (active, Protoman frames): can repack an atlas; no shared atlas today.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O4` deeper levels | helps | the final stage gets its own identity |
| `O9` player experience | helps | one consistent look; belts show their direction |
| `O2-R2` random calls through the engine API | no effect | no random calls |
| A branch keeps all behavior that its contract does not name | no effect | art only, plus one belt type |

## Open questions

None. D1 (owner, 2026-11-19): no enemy skins in `078b`; decide after the play test.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] Map script checks: every block object and every non-tile object other than the background objects and their
  triggers is the same as on `master`; the round-2 checks pass on the final map.
- [ ] Atlas script checks: every old `Platforms1` region keeps its name and pixels; every region that the map and
  `ConveyorBelt` name exists.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

From `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-078b-wily-stage-3-art`:

- `./run-game-alpha.sh --level WILY_STAGE_3 --room intro_room`: the shaft walls rise from black (fade layers), lamps,
  the steady trace.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room intro_room_2`: into `room1`: lab belts move and show their
  direction; the furnace background; no wall looks open, no air looks solid.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: the climb: furnace glow at the bottom, lab wall above,
  no gap or double background in the pans.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room4 --heartTanks all --healthTanks all`: the flame plate,
  then the Inferno Man refight room.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room6 --heartTanks all --healthTanks all`: the crescent plate;
  after the Moon Man fight, `room7` shows the gem and leaf plates.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room8 --heartTanks all --healthTanks all`: `room9` trace turns
  violet; the core background; `wily_room` steel and violet blocks.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: a green belt looks and moves as before (control).

## Behaviors to check

- Belt push speed and direction are the same as before (only the art changes).

## Intended changes

- Wily Stage 3 art: tiles, backgrounds, belts. No gameplay change.
