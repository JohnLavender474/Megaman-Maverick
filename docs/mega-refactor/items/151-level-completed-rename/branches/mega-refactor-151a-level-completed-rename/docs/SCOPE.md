# Scope: mega-refactor-151a-level-completed-rename

- Status: approved
- Approved: 2026-11-11 (the owner answered the 3 decisions of the review with "Go with your recommendations")

## Goal

Rename the level progress family of `GameState`, its listener, the player progress component and the `MegaPlayer`
hook from "defeated" to "completed". No behavior change.

## Item

`151`. The whole item: the symbol rename, the deletion of `allLevelsDefeated`, the 3 message fixes, and the live docs.

## Current state

The scan is in the item `NOTES.md` (the family table, the counts per file, the save path, what must stay, the edge
cases). Call sites on `master` at `8b8989468`:

- `state/GameState.kt:29,53,55,64,73,82` declare the family; `:57,:66` log with the old name; `:295-301` and
  `:379` use the set.
- `state/IGameStateListener.kt:10,12`.
- `entities/state/PlayerProgressComponent.kt:9,14`.
- `players/MegaPlayer.kt:114,256`; `entities/megaman/Megaman.kt:261`.
- Readers: `levels/LevelUtils.kt:7-8`, `entities/sensors/InfernoDeath.kt:24-25`,
  `screens/levels/MegaLevelScreen.kt:741,777`, `screens/menus/SaveGameScreen.kt:88`,
  `screens/menus/MainMenuScreen.kt:152`, `screens/menus/level/LevelPauseScreen.kt:328`,
  `screens/menus/level/LevelSelectScreen.kt:153,230,251,305,346,367`,
  `screens/menus/level/LevelSelectScreenV2.kt:62,148,161,198,233,259`.
- Tests: `GameStateSaveStringTest`, `GameStateWeaponsTest`, `PlayerProgressComponentTest`, `MegamanProgressTest`,
  `MegamanWeaponsHandlerOwnershipTest`.

## In scope

- The symbol rename of the family (see the item `NOTES.md` table), in main and test sources.
- Delete `GameState.allLevelsDefeated` (0 callers).
- Fix the exception messages at `GameState.kt:311,322,334` to name heart tanks, health tanks and enhancements.
- Reword the 2 log and exception strings that say "defeated" about levels (`GameState.kt:301,380`).
- Update `core/.../players/README.md` and the root `CLAUDE.md` on the branch; at the merge, on `master`: `docs/issues/2026-11-02-boss-props-have-two-sources.md`,
  `items/007-json-save-slots/NOTES.md` (JSON key `levelsCompleted`) and `items/036-checkpoints-as-data/NOTES.md`.

- Added 2026-11-11 as a small QoL change (owner's request at the play checks): the dev flag `--saveFile <name>`, which
  chooses the preferences file of the save (`LaunchOptions`, `ResolvedLaunch.saveFile`, the 3 `getPreferences` calls in
  `MegamanMaverickGame`, the launcher parameter, `LaunchOptionsResolverTest`).

## Out of scope

- Every "defeated" of a boss: `AbstractBoss`, `IBossListener`, `EventType.BOSS_DEFEATED`,
  `MegaLockReason.BOSS_DEFEATED`, `FightRule.AllDefeated`, `ConstKeys.DEFEATED`, animation keys, atlas regions,
  `EndingScreen` text, the `MegaLevelScreen.kt:740` comment ("boss is already defeated" describes the boss).
- The save string format.
- Recording `WILY_STAGE_3` on `GAME_COMPLETE` (a behavior change; `036` and the open boss-props issue discuss it).
- Done-item docs that name the old identifiers (history).

## Affected areas and risks

- 18 Kotlin files in `core`. A text replace would hit the boss vocabulary, so the rename is by symbol, checked with a
  grep for the old identifiers afterwards.
- `MegaPlayer.onLevelDefeated` is `protected open`; a subclass on another branch that overrides it would break at
  merge. None does today.

## Overlap with other branches

- `060c` (Proto Man buster): docs only so far; it adds Proto Man code under `MegaPlayer` later and must use
  `onLevelCompleted` after this merges. No family file in its diff.
- `081f` (X6 intro fixes) and `compactor-level`: no family file in their diffs.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | no effect | no session access changes |
| `O1-R2` | no effect | no new class |
| `O1-R3` | no effect | no new listener or event |
| `O1-R4` | no effect | no new key |
| `O1-R5` | helps | no new API; 1 dead function removed |
| `O2-R1`, `R2`, `R3`, `R4`, `R5`, `R6`, `R7`, `R8` | no effect | |
| `O2-R9` | no effect | the add listener keeps its shape; progress is still per game state, as before |
| `O3-R1`, `O5-R1`, `O5-R2`, `O7-R3`, `O8-R2`, `O9-R1` | no effect | |
| `O6-R1` | helps | the build and the tests run in the contract |
| `O6-R2` | helps | the contract names the kept behavior; `GameStateSaveStringTest` pins the save string |
| `O9-R2` | no effect | the save string does not change (waiting on `007` anyway) |

## Open questions

None.
