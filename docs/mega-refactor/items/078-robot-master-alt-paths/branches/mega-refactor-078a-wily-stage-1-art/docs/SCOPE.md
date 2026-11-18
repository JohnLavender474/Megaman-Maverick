# Scope: mega-refactor-078a-wily-stage-1-art

- Status: approved
- Approved: 2026-11-18

## Goal

Wily Stage 1 (`WILY_STAGE_1`, `assets/tiled_maps/tmx/WilyStage1_v2.tmx`) gets its new art: one tileset, the
repainted map, new background objects, and a generator entity in `boss_room` with light events and an off state.

## Item

`078` (stage redesigns), section "Wily Stage 1" of `ITEM.md`. All owner decisions of the art phase are in
`temp/078-wily1-art/STYLE.md` (top) and in that section.

## Current state

- The art drafts and the merge are done in `temp/078-wily1-art/` (git-ignored):
  - `merged/wily_stage_1_tileset.png/.tsx`: 290 tiles, 2 tile animations (cable cuts, sand pours).
  - `merged/WilyStage1_v2_new.tmx`: `tiles2` and `tiles1` repainted, a new layer `decor` (481 cells) after
    `tiles1`. Block layers and objects are as on `master`. `merged/checks.txt`: 45 checks pass.
  - `merged/map_objects_proposal.md`: background objects, atlas `Backgrounds8`, trigger changes.
  - `5-cave/generator/`: the generator strips (on, sparks, off, power down) and `lights.tsv`.
- The level renderer (`TiledMapLevelRenderer`) draws every tile layer in map order and plays Tiled tile
  animations. A new tile layer needs no code.
- Backgrounds (`BackgroundLayerBuilder.kt`, `Background.kt`): the object name is the show/hide key; all animated
  custom backgrounds share the key `AnimatedBackground`.
- Light: `LightSourceUtils.sendLightSourceEvent` (`utils/misc/LightSourceUtils.kt`) sends `ADD_LIGHT_SOURCE` to
  `DarknessV2`. `Lantern` (`entities/decorations/`, a `LightSourceEntity`) is the model of a lit decoration.
- `boss_room` has `ElecDevil` (object 448) and is under `DarknessV2` (object 349).

## In scope

1. Tileset and map: copy the merged tileset into `assets/tiled_maps/`, and the new map over `WilyStage1_v2.tmx`
   (same file name, so `LevelDefinition` does not change). Remove the old tilesets from the map when no cell uses
   them.
2. Backgrounds: the objects and triggers of `map_objects_proposal.md`, option C: the corridor wall and the cave
   wall are animated (4x2 sheets) and world-locked, each with its own show/hide key. Code change in
   `BackgroundLayerBuilder.kt`: a custom object is animated when it has the `animation_rows` property (today only
   when its name is `AnimatedBackground`), so its name stays its key. Maps that use `AnimatedBackground` keep
   their behavior. The frames need 2 atlases: `Backgrounds8` and `Backgrounds9` (frames, `.tps`, packed) and
   their `TextureAsset` entries.
3. Generator entity `WilyGenerator` (`entities/decorations/`): 4 sprite states (on, sparks over on, power down,
   off), light events per `lights.tsv`, spark bursts at random gaps (through the game's random API, rule `O2-R2`),
   and a public `powerDown()` for `077c`. Placed in `boss_room` as a map object. Its regions go in an atlas with
   free space, or in a new page.
4. `boss_room` background `bg_boss_room.png` (no generator in it).
5. Credits in `README.md` (`docs/ways-of-working/art.md`).
6. Map clean-up: delete the 928 transparent `clear` cells.
7. Added 2026-11-18 (owner, play test): a `wily` sprite skin for each enemy of the stage (`GreenUziJoe`, `DuoBallCanon`,
   `SpikeCopter`, `DragonFly`, `ShieldAttacker`, `BulbBlaster`, `WanaanLauncher`, `LaserBeamer`), chosen with the map property
   `color` through the shared enum `SpriteSkin` (`entities/utils/`). No property keeps today's look. All 60 placements
   in `WilyStage1_v2.tmx` set `color=wily`. Palettes: Claude's recommendations, accepted by the owner.

## Out of scope

- The `ElecDevil` fight and the phase-2 call to `powerDown()`: `077c`.
- Gameplay: block geometry, spawns, rooms, camera. The map keeps every block object.
- Other stages.

## Affected areas and risks

- `WilyStage1_v2.tmx`, a new tileset, `Backgrounds8`, `Backgrounds9`, `BackgroundLayerBuilder.kt`, `TextureAsset`, one new entity class,
  `core/src/smokeTest/resources/entity-tags.txt`, `README.md`.
- World-locked background objects (parallax 1) are new in this game: the formula is read from the code only.
- The moment an `EventTrigger` spawns in a room pan can show or hide the dunes while 2 rooms are in view.
- A wrong tile in an air cell can look solid: checked in `merged/checks.txt`, and at the play test.

## Overlap with other branches

- `077c` (`ElecDevil`, active): phase 2 needs the generator off. `078a` gives `WilyGenerator.powerDown()`;
  whichever branch merges second wires the call (both `ITEM.md` files and the `077c` `TODO.md` say so).
- No other active branch touches `WilyStage1_v2.tmx`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O2-R2` random calls through the engine API | no effect | the spark gaps use the game random API |
| `O4` deeper levels | helps | stage identity and readability |
| `O9` player experience | helps | the corridor floor reads against the wall |
| A branch keeps all behavior that its contract does not name | no effect | art only, plus one decoration entity |

## Owner answers (2026-11-18, brief)

- D1: the corridor and cave back walls are animated (not the recommendation; option C).
- D2: the 114 cave cable cells over the ceiling shadow stay as drawn.
- D3: no crest at gate 266; the other gates keep theirs.

## Open questions

None.
