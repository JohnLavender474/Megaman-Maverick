# Plan: mega-refactor-118a-fight-routine

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-02

## Design notes

### Frame match

`events.run()` runs after `screen.render`. `submitEvent` during `run` waits for the next `run` (`EventsManager.kt:29`), and a submit outside `run` reaches the `run` of the same frame. No system runs between `run` of frame F and the tick of frame F+1, because the tick runs before `engine.update`. So a handler in `run` of frame F and a step in the tick of frame F+1 have the same effect, and their submits reach the same `run`.

| Old moment | Old action | New moment | New action |
|---|---|---|---|
| `run` of F, `ENTER_BOSS_ROOM` | Skip check, then `init` queues the spawn | `run` of F, same branch | Skip check, `resolve`, `init` queues the spawn, and `fights.start`. No frame moves |
| `run` of F, `BOSS_READY` | Bar set, systems off | Tick of F+1 | `AwaitReady` ends, then `HoldWorld` and `FillBar` begin in the same tick. The bar's first update stays in the `when` of F+1 |
| Render of G, bar `runOnFinished` | Submits `END_BOSS_SPAWN`, resumes music | Same | `FillBar` passes the same callback |
| `run` of G, `END_BOSS_SPAWN` | Submits `TURN_CONTROLLER_ON`, systems on. `AbstractBoss` receives the event | Tick of G+1 | `ReleaseWorld` and `ControllerOn`. `AbstractBoss` still receives the event in `run` of G |
| `run` of D, `BOSS_DEFEATED` | Slot 0 test, input off, damage lock, music off, listeners, bar off | Tick of D+1 | The win rule ends the wait, and the outro steps run with the same slot 0 test |
| `run` of E, `BOSS_DEAD` | Slot 0 test, submits `VICTORY_EVENT` | Tick of E+1 | `AwaitDefeatEnd` ends, and `LevelVictory` does the same |

### Types

```kotlin
interface Step<C> {
    fun begin(c: C) {}
    fun update(c: C, delta: Float) {}
    fun isFinished(c: C): Boolean
    fun end(c: C, interrupted: Boolean) {}
}

class Routine<C>(private val steps: Array<Step<C>>) {
    fun update(c: C, delta: Float): Boolean   // true when all steps are done
    fun cancel(c: C)
}
```

`Routine.update` moves through each step that finishes in the same tick, so `AwaitReady`, `HoldWorld` and `FillBar` start in one tick.

`Fight` holds `cast`, `plan`, `services`, the bar handler and its leases. It runs `intro`, then the wait, then `outro`, then it closes. The win rule is read in each tick. If it is true during the intro, the fight cancels the intro and goes to the outro, so a boss killed before `ready` cannot leave the fight stuck.

`FightDirector(services) { bossHealthHandler }` is a `val` of the screen. It takes a supplier, because the handler is built in `init()` (`MegaLevelScreen.kt:284`). No new context type is needed.

### Guards

`BOSS_READY`, `BOSS_DEFEATED` and `BOSS_DEAD` carry the boss, so their guard is `fights.owns(boss)`. `END_BOSS_SPAWN` has no boss, so its guard is `fights.active`. Old and new flow cannot run at one time, because a level has one boss object per room and a room change cannot start a second fight during the first. The guards stay until `118g`.

### Changes from the approved `SCOPE.md`

1. No spawn step. The spawn stays in the `ENTER_BOSS_ROOM` branch, because `Fight` needs the boss at start and the frame then does not move. `BossSpawnEventHandler.init` returns the boss (1 caller).
2. A second cancel, in `reset()` before `engine.reset()` (`MegaLevelScreen.kt:1085`). A game over reaches this path with no `PLAYER_SPAWN_INIT`.

Both are in `SCOPE.md` now. They need the owner's approval with this plan.

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` and start dates, item `active`, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118a-fight-routine .worktrees/mega-refactor-118a-fight-routine master`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base. Use the Corretto 17 JDK.
- Done: no

### 2. `Step` and `Routine` in `engine`

- Changes: `engine/.../state/Step.kt`, `engine/.../state/Routine.kt`. Tests first.
- Check: `RoutineTest` passes: order, one step at a time, several steps in one tick, `end` once for each started step, `interrupted` on a cancel, a cancel with no started step.
- Done: no

### 3. Core types

- Changes: in `core/.../fights/`: `Lease`, `FightRule.AllDefeated`, `FightPlan`, `Fight`, `FightDirector`. Tests first. No screen change.
- Check: `FightTest` passes: leases release once, in reverse order, on an end and on a cancel; a win during the intro goes to the outro; `owns` and `active`; `cancelAll` with no fight does nothing.
- Done: no

### 4. Steps, preset and `FightPlans`

- Changes: `AwaitReady`, `HoldWorld`, `FillBar`, `ReleaseWorld`, `ControllerOn`, `DefeatLock`, `NotifyListeners`, `HideBar`, `AwaitDefeatEnd`, `LevelVictory`, `stageBoss`, `FightPlans.find`, `FightPlans.resolve`. Each step body is a copy of the old branch, including the slot 0 tests and a `TODO` for `047e`. No screen change, so these steps have a first caller only in step 6, in the same branch.
- Check: `FightPlansTest` passes: a plan for `BigAssMaverickRobot`, `null` for each other class in `entities/bosses/`, the music value in the props.
- Done: no

### 5. Temporary contract suite

- Changes: `FrozenBossFlow` and `BossFlowContractTest` in the test sources. The frozen copy is the 6 branches at `MegaLevelScreen.kt:691-804`, with fake systems, a relaxed `BossHealthHandler` mock with a scripted `finished`, a real `EventsManager`, and a mock `AbstractBoss` with scripted `ready`, `defeated` and existence state.
- Check: all rows in `CONTRACT.md` pass, with equal traces for each frame. To prove that the trace is sensitive, delay one step by 1 frame by hand and see the suite fail, then undo this. Log the result.
- Done: no

### 6. Wire the screen

- Changes in `MegaLevelScreen`: the `fights` field; the route in `ENTER_BOSS_ROOM` after the skip check; the 4 guards; `fights.update(delta)` inside `if (!game.clock.paused)`, before the `when`; `fights.cancelAll()` in `PLAYER_SPAWN_INIT` and in `reset()`. `BossSpawnEventHandler.init` returns the boss. Remove `BigAssMaverickRobot.kt:211`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. A guard test in `MegaLevelScreenEventsTest`, for each branch: it skips a boss that a fight owns, and it acts for a boss with no fight. If the mock `game` cannot reach a branch, test `fights.owns` for that branch, and log which one.
- Done: no

### 7. Short launch

- Changes: none.
- Check: build first, then `timeout 12` one launch of `--level INTRO_STAGE --room room12`. A clean exit is a pass.
- Done: no

### 8. Owner play test

- Changes: none, unless a check fails. A fix means that the control checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 9. Merge

- Changes: delete `FrozenBossFlow` and `BossFlowContractTest` in the last commit. Then the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes with the suite gone.
- Done: no
