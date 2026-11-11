# Plan: mega-refactor-060b-protoman-move

## New APIs

| API | First production caller |
|---|---|
| `entities/protoman/Protoman` | `MegamanMaverickGame.postCreate` (roster, slot 0 on `REFACTOR_TEST_060`) |
| `entities/protoman/ProtomanValues` | `Protoman` run, jump, body and gravity params |
| `PlayerCharacter.PROTOMAN` | `LevelDefinition.REFACTOR_TEST_060.characters`, `postCreate` |
| `TextureAsset.PROTOMAN` | `Protoman.defineAnimationsComponent` |
| `LevelDefinition.REFACTOR_TEST_060` | `--level REFACTOR_TEST_060` |

## Steps

1. **Test map.** `assets/tiled_maps/tmx/RefactorTest060.tmx` with the rooms `walk` (flat floor, a step up, a wall),
   `jump` (ledges of 1 to 4 tiles, a pit with a `PlayerDeath` sensor), `water`, and `transition` with a next room. No
   enemies. Painted `tiles`. Built by a subagent with the `level-builder` skill. Check: the rooms parse, each has a
   player spawn.
2. **Atlas.** The prep script (crop, key colors, mirror; D2, D3), the prepared sheet and the YAML in
   `utils/sprite_slicer/sheets/`; the frames and `assets/sprites/sprite_sheets/Protoman.txt` and `.png`; a credit line
   in `assets/credits.txt`. Regions: `stand` (2), the step, `run` (4: w1 w2 w3 w2), `jump`. Check: the
   `utils/sprite_slicer` tests; the preview matches the draft.
3. **Protoman.** `Protoman` and `ProtomanValues` (D4), `PlayerCharacter.PROTOMAN`, the `postCreate` block,
   `TextureAsset.PROTOMAN`, `LevelDefinition.REFACTOR_TEST_060`, `LevelCompletionScreensTest`, `entity-tags.txt`.
   The step as Megaman (D1). `TODO`s: beam color and damage art (`060e`), slide on `DOWN` + `A` (`060d`), the enemy
   `Megaman::class` masks where they miss him. Check: `core:test`, `core:smokeTest`.
4. **Tests.** His run and jump through `InputFrame` (model: `MegamanXTest`). Check: `core:test`; break the code once to
   see each new test fail.
5. **Docs.** `players/README.md` (3 player classes).
6. **Checks.** `engine:test core:test core:smokeTest lwjgl3:jar`; review before the play test (diff; play checks and
   the new map); play checks and Diffity.
