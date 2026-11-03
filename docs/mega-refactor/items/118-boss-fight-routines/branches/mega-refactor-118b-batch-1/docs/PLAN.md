# Plan: mega-refactor-118b-batch-1

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: draft
- Approved: no

## Design notes

### Frame match at the end of a mid-level fight

D is the frame of `BOSS_DEFEATED`, and E is the frame of `BOSS_DEAD`. The intro and the defeat are the same as in `118a` (`118a` `PLAN.md`, "Frame match").

| Old moment | Old action | New moment | New action |
|---|---|---|---|
| `run` of E, `BOSS_DEAD` | Slot 0 test, submits `INTERMEDIATE_BOSS_DEAD` | Tick of E+1 | `AwaitBossDefeatEnd` ends. `ExitMidBossFight` does the same slot 0 test and submit |
| `run` of E+1, `INTERMEDIATE_BOSS_DEAD` | `Gate` opens. The screen submits `TURN_CONTROLLER_ON`, unlocks, plays the level music | `run` of E+1 | `Gate` opens, the same frame. The screen branch skips the boss, because the fight is still open |
| (same) | (same) | Tick of E+1 | `ResumeAfterMidBoss` unlocks, plays the level music, and submits `TURN_CONTROLLER_ON`. This is the intended 1-frame shift |
| `run` of E+2, `TURN_CONTROLLER_ON` | Controller on | `run` of E+1 | Controller on, 1 frame earlier |
| (none) | (none) | Tick of E+2 | `AwaitBossGone` ends (the destroy ran in `engine.update` of E+1), and the fight closes |

### Steps and plans

```kotlin
object ExitMidBossFight : Step<Fight>     // begin: if slot 0 alive, submit INTERMEDIATE_BOSS_DEAD with the boss
                                         // isFinished: leadPlayerAlive (as LevelVictory)
object ResumeAfterMidBoss : Step<Fight>   // begin: TURN_CONTROLLER_ON, unlock BOSS_DEFEATED for each player,
                                         //        audio.playMusic(levelMusic(), true)
object AwaitBossGone : Step<Fight>        // isFinished: !boss.existenceState.liveNow
```

- `standardBossPlan(music: MusicAsset?)` and `midBossPlan(music: MusicAsset?)` share the intro and the first 4 outro steps through private builders in `FightPlans`.
- `LevelVictory` submits `VICTORY_EVENT`, not `boss.getEventOnDefeated()`.
- `FightPlans.find(bossName: String, props: Properties)` reads `ConstKeys.END` with the default `true`, as `AbstractBoss` and the screen do.
- `FightDirector.start(boss, plan, services, bar, levelMusic: () -> MusicAsset?)`. `Fight` keeps it as `levelMusic`. The screen passes `{ music }`, because a room can change `music` (`MegaLevelScreen.kt:563`) after the fight starts.

### New APIs and their first callers

| API | First production caller |
|---|---|
| `midBossPlan`, `ExitMidBossFight`, `ResumeAfterMidBoss`, `AwaitBossGone` | `FightPlans.find`, for the 3 mid-level fights (step 4) |
| The `props` parameter on `FightPlans.find` | `MegaLevelScreen`, `ENTER_BOSS_ROOM` (step 4) |
| `levelMusic` on `FightDirector.start` and `Fight` | `MegaLevelScreen`, `ENTER_BOSS_ROOM`; `ResumeAfterMidBoss` (steps 3 and 4) |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118b-batch-1 .worktrees/mega-refactor-118b-batch-1 master`. Recheck the overlap with the other active branches, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Music and names

- Changes: `FightPlan.music` is `MusicAsset?`. `resolve` writes `MUSIC` only when it is set. `stageBossPlan` is `standardBossPlan`. `LevelVictory` submits `VICTORY_EVENT`. Tests first.
- Check: `core:test` passes. New tests: `resolve` writes no `MUSIC` for a plan with no music; `LevelVictory` submits `VICTORY_EVENT` with the boss. The existing `BigAssMaverickRobot` tests pass with no change other than the name.
- Done: no

### 3. Mid-level steps and plan

- Changes: `levelMusic` on `FightDirector.start` and `Fight`. `ExitMidBossFight`, `ResumeAfterMidBoss`, `AwaitBossGone`, `midBossPlan`. `find` takes the props and chooses the plan from `end`. The screen passes the props and `{ music }` at the existing call sites, and the boss list stays `BigAssMaverickRobot` only. Tests first.
- Check: `core:test` passes. New tests in `FightTest` and `FightStepsTest`:
  - `midBossPlan`, slot 0 alive: `INTERMEDIATE_BOSS_DEAD` once and no `VICTORY_EVENT`; `TURN_CONTROLLER_ON` once, the `BOSS_DEFEATED` lock gone from each player, the level music played once; the fight open until `liveNow` is false, then closed.
  - `midBossPlan`, slot 0 dead at the defeat: no event, no resume, the fight open until a cancel.
  - `standardBossPlan`: `VICTORY_EVENT`, no resume.
  - `find`: `midBossPlan` for `end=false`, `standardBossPlan` for `end=true` and for no `end`.
  - Mutation checks: break each new step and the `end` read, one at a time. Confirm with `git diff` that the mutation is in the file, and see the matching test fail. Log the result.
- Done: no

### 4. Move the 3 bosses, and the screen guard

- Changes: add `TimberWoman`, `MoonMan` and `ReactorMan` to `find`, with no music. The `fights.owns(boss)` guard at the top of the screen's `INTERMEDIATE_BOSS_DEAD` branch. These go in one commit, so no commit has a moved boss without the guard.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. New tests: `find` gives a plan for the 4 moved bosses and `null` for each other class in `entities/bosses/`; in `MegaLevelScreenEventsTest`, the `INTERMEDIATE_BOSS_DEAD` branch skips a boss that a fight owns and acts for a boss with no fight. Mutation check of the guard, as in step 3.
- Done: no

### 5. README

- Changes: update `core/.../fights/README.md` if its generic text no longer fits, for example a plan whose outro hands control back, and the open fight after the last event.
- Check: the text names no current step list or count, and its Mermaid blocks render.
- Done: no

### 6. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `TIMBER_WOMAN room14`, `MOON_MAN room14`, `REACTOR_MAN room14`, `WILY_STAGE_1 room6`, `WILY_STAGE_3 room6`. A clean exit is a pass.
- Done: no

### 7. Owner play test

- Changes: none, unless a check fails. A fix means that the control checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 8. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
