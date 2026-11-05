# Plan: mega-refactor-118e-batch-4

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-05

## Design notes

- In `FightPlans.find`, after the `when` gives the boss music:
  ```kotlin
  if (props.getOrDefault(ConstKeys.MINI, false, Boolean::class)) return miniBossPlan()
  ```
  Then the `end` read, as today. The `mini` read comes first, because a mini fight ignores `end`, as `getEventOnDefeated` does today (`AbstractBoss.kt:235`). The plan music of the `when` is not used for a mini plan, because a mini boss plays no boss music (`playBossMusic()` is `!mini`).
- `miniBossPlan()` builds a `FightPlan` directly. It does not use `stageBossPlan`, because its intro is different:
  - intro: `AwaitBossReady`, `EndBossSpawn`, `ControllerOn`;
  - win: `FightRule.AllDefeated`;
  - outro: `NotifyListenersOfBossDefeat`, `AwaitBossDefeatEnd`, `ExitMidBossFight`, `ControllerOn`, `AwaitBossGone`;
  - music: `null`.
- `EndBossSpawn` is a new step in `FightSteps.kt`. `begin` submits `Event(EventType.END_BOSS_SPAWN)`, and `isFinished` is `true`. `FillBossHealthBar` keeps its own submit in its fill callback, because there the event must wait for the end of the fill.
- The new `when` entries: `MoonHead.TAG`, `MechaDragon.TAG`, `ReactorMonkey.TAG` and `PenguinMiniBoss.TAG` go in the branch that gives `null` music.

### Frame match

Ready in frame R (`engine.update`):

| | Old (screen) | New (fight tick) |
|---|---|---|
| `BOSS_READY` handled, `END_BOSS_SPAWN` submitted | `events.run` R | tick R+1 (`AwaitBossReady`, `EndBossSpawn`) |
| `END_BOSS_SPAWN` reaches `AbstractBoss` | `events.run` R+1 | `events.run` R+1 |
| `TURN_CONTROLLER_ON` reaches Megaman | `events.run` R+2 | `events.run` R+1 (intended change 1) |

Defeat animation ends in frame D (the boss `destroy()` and `BOSS_DEAD`):

| | Old (screen) | New (fight tick) |
|---|---|---|
| `INTERMEDIATE_BOSS_DEAD` submitted | `events.run` D | tick D+1 (`AwaitBossDefeatEnd`, `ExitMidBossFight`) |
| The gate gets `INTERMEDIATE_BOSS_DEAD` | `events.run` D+1 | `events.run` D+1 |
| `TURN_CONTROLLER_ON` reaches Megaman | `events.run` D+2 | `events.run` D+1 (intended change 1) |

In tick D+1 the boss is still in the engine (`liveNow`), because the engine removes it at the start of `engine.update` D+1. So `AwaitBossGone` keeps the fight open through `events.run` D+1, and the `INTERMEDIATE_BOSS_DEAD` guard sees the fight, as in `midBossPlan`.

### New APIs and their first callers

| API | First production caller |
|---|---|
| `EndBossSpawn` (`FightSteps.kt`) | `miniBossPlan` |
| `miniBossPlan()` (private, `FightPlans.kt`) | `FightPlans.find` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118e-batch-4 .worktrees/mega-refactor-118e-batch-4 master`. Recheck the overlap with the other active branches for `fights/`, `MegaLevelScreen`, `AbstractBoss` and the 5 bosses, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Tests first

- Changes: in `FightTest`:
  - `MoonHead`, `MechaDragon`, `ReactorMonkey` and `PenguinMiniBoss` move from the "no plan" list to the moved-boss list; `Bospider` leaves the "no plan" list;
  - each of the 4 gives a plan with the exact `miniBossPlan` intro and outro and no music, for `mini=true` with no `end`, `end=true` and `end=false`;
  - each of the 4 with no `mini` and with `mini=false` gives the stage or mid plan from `end`;
  - a fight with the intro `EndBossSpawn` delivers `END_BOSS_SPAWN` to a listener after `events.run()`.
- Check: `core:test` fails only on the new or changed tests. The `EndBossSpawn` test cannot compile without the step, so add the step object with an empty `begin` first, and confirm that its test fails.
- Done: no

### 3. The mini plan and the 4 bosses

- Changes:
  - `EndBossSpawn` submits `END_BOSS_SPAWN`;
  - `miniBossPlan()`, the `mini` read and the 4 bosses in `FightPlans.find`;
  - remove the forced `mini` line from `MoonHead.onSpawn`, `MechaDragon.onSpawn` and `PenguinMiniBoss.onSpawn`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. Mutation checks, one at a time:
  - remove each of the 4 bosses from `find` (4 runs);
  - make the `mini` read always `false`;
  - put the `mini` read after the `end` read, so that `end=false` gives `midBossPlan`;
  - remove `ControllerOn` from the mini intro;
  - remove `ExitMidBossFight` from the mini outro;
  - an empty `EndBossSpawn.begin`.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result.
- Done: no

### 4. Comment out `Bospider`

- Changes: comment out `Bospider.kt` with a `TODO` at the top, in the form of `SphinxMiniBoss.kt` (no live level uses it; commented out, not deleted, in `118e`; check it against the current entity code before use). Remove the `BOSPIDER` constant and its pool line from `BossesFactory`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. `grep` finds no live reference to `Bospider` in `core/src/main` outside its own file.
- Done: no

### 5. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `MOON_MAN room11`, `INFERNO_MAN room8`, `REACTOR_MAN room12`, `REACTOR_MAN room5`, `WILY_STAGE_1 room9`. A clean exit is a pass.
- Done: no

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the control check runs again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
