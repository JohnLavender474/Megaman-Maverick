# Notes: 151 Rename the level progress vocabulary from "defeated" to "completed"

Research for this item. Scan of `master` at `8b8989468`, 2026-11-11, by a review subagent, checked by grep.

## The family

| Current | New |
|---|---|
| `GameState.levelsDefeated` | `levelsCompleted` |
| `GameState.isLevelDefeated` | `isLevelCompleted` |
| `GameState.addLevelDefeated` | `addLevelCompleted` |
| `GameState.removeLevelDefeated` | `removeLevelCompleted` |
| `GameState.allRobotMasterLevelsDefeated` | `allRobotMasterLevelsCompleted` |
| `GameState.allLevelsDefeated` | deleted (0 callers) |
| `IGameStateListener.onAddLevelDefeated` | `onAddLevelCompleted` |
| `IGameStateListener.onRemoveLevelDefeated` | `onRemoveLevelCompleted` |
| `PlayerProgressComponent(onLevelDefeated = ...)` | `onLevelCompleted` |
| `MegaPlayer.onLevelDefeated` and the `Megaman` override | `onLevelCompleted` |

## Counts

18 Kotlin files, 69 identifier occurrences: 13 under `core/src/main` (`GameState` 21, `LevelSelectScreen` 6,
`LevelSelectScreenV2` 6, `MegaPlayer` 3, `PlayerProgressComponent` 3, `IGameStateListener` 2, `LevelUtils` 2,
`InfernoDeath` 2, `MegaLevelScreen` 2, `Megaman` 1, `SaveGameScreen` 1, `MainMenuScreen` 1, `LevelPauseScreen` 1) and
5 under `core/src/test` (`GameStateSaveStringTest`, `GameStateWeaponsTest`, `PlayerProgressComponentTest`,
`MegamanProgressTest`, `MegamanWeaponsHandlerOwnershipTest`). No reference in `engine/`, `core/src/smokeTest`,
`core/src/test/resources`, `utils/` or `lwjgl3/`.

## Why the save file is unaffected

`MegamanMaverickGame.saveState()` writes `gameState.toString()` under the prefs key `ConstKeys.STATE` (`"state"`) in
the file `PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE`. `GameState.toString()` writes positional `;`-separated fields of
enum names, and `fromString()` reads field 0 by position with `LevelDefinition.valueOf`. No key, file name or field
holds the word. `GameStateSaveStringTest` pins the string.

## What must stay

The boss defeat vocabulary and every matched value: `AbstractBoss.defeated` and `onDefeated()` with about 20 boss
overrides, `IBossListener.onBossDefeated`, `EventType.BOSS_DEFEATED`, `MegaLockReason.BOSS_DEFEATED`,
`FightRule.AllDefeated`, `ConstKeys.DEFEATED` (`"defeated"`, an animation and atlas key, and part of the Tiled prop
`destroy_on_boss_defeated` in `Spike`), the `"defeated"` animation keys in boss files, the `*/defeated*` atlas regions
in 5 sprite sheets, `EndingScreen` text. So the branch renames symbols, never the word.

## Edge cases found

- `WILY_STAGE_3` is never recorded: the `GAME_COMPLETE` path never calls the add. `allLevelsDefeated()` stays false
  after the game is beaten. Decision: delete it (0 callers).
- `MegaPlayer.onLevelDefeated` forwards only the add side. `onRemoveLevelDefeated` has no production override; the
  rename keeps the shape.
- No active branch (`060c`, `081f`, `compactor-level`) adds or removes a family reference. `060c` later adds Proto Man
  code under `MegaPlayer`, so it must use the new name after this merges.
- `007` NOTES proposes a persisted JSON key `levelsDefeated`. The branch changes the note to `levelsCompleted`.
- `GameState.kt:311`, `:322`, `:334`: exception messages say "Levels defeated" for heart tanks, health tanks and
  enhancements (a copy-paste error). The branch fixes the noun.
- Nearby "completion" names fit, not clash: `screenOnCompletion`, `RobotMasterLevelOnCompletion`,
  `LevelCompletionScreensTest`. `GAME_COMPLETE` is the game's end, a different concept.
