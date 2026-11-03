# Notes: mega-refactor-118a-fight-routine

## Decisions

## Overlap with other branches

## Decisions

Changes from `PLAN.md` during the work, all to keep to `O1-R5`:

- `Step` has no `update` and no `delta`. No step needs them: `FillBar` waits on the bar, which the `when` still updates. They come with the first step that needs them.
- `Fight` holds `boss`, not a `cast` array, because no fight has a second participant.
- `FightDirector` holds 1 fight, and its call is `cancel`, not `cancelAll`.
- `FightDirector` takes no constructor argument. `start` takes the bar. A supplier was not needed, and this lets the guard tests start a fight with no screen `init()`.
- `FillBar` uses `BIT_BY_BIT` as a constant, because `ALL_AT_ONCE` has no caller, so `getHealthFillType` stays unread on this path.
- Leases have keys (`FightLease`), because `ReleaseWorld` and `HideBar` must release one lease before the fight closes.
- `LevelVictory` does not finish while slot 0 is dead. The fight stays open until `PLAYER_SPAWN_INIT` or `reset()` cancels it, so the bar goes at the same moment as in the old flow.
- `fights` is `internal` on the screen, for the guard tests.

## Contract suite limits

- The trace is taken just before `engine.update`, not after `events.run()`. That is the state that the systems see. A step in the tick of frame F+1 replaces a handler in `run` of frame F, so a sample after `run` differs by design.
- The harness follows 2 real rules. A player cannot die while the world is held, because no system runs. A level reset waits for the bar, because the `when` runs only the first unfinished handler. Without these rules, the suite reports 1 difference: a reset in the same `run` as `END_BOSS_SPAWN` gives no `TURN_CONTROLLER_ON`. The game cannot reach that case: the death handler starts only after the bar is finished. This comes from reading the code.
- A level reset with slot 0 alive is not in the suite, because `PLAYER_SPAWN_INIT` follows a death.
- The game-over row is a guard test: `reset()` cancels a fight. It is not a trace row, because the old flow leaves the bar object set when the screen leaves.

## Owner's manual edits (2026-11-02)

The owner renamed these by hand after `db8fe34dd`. The approved `SCOPE.md`, `CONTRACT.md` and `PLAN.md`, and the earlier process log entries, use the old names.

| Old name | New name |
|---|---|
| `AwaitReady` | `AwaitBossReady` |
| `FillBar` | `FillBossHealthBar` |
| `DefeatLock` | `BossDefeatLock` |
| `NotifyListeners` | `NotifyListenersOfBossDefeat` |
| `HideBar` | `HideBossHealthBar` |
| `AwaitDefeatEnd` | `AwaitBossDefeatEnd` |
| `stageBoss` (preset) | `stageBossPlan` |
| `Fight.leadLive` | `Fight.leadPlayerAlive` |

`HoldWorld`, `ReleaseWorld`, `ControllerOn` and `LevelVictory` keep their names.

Other owner edits:

- `FightPlans` passes the steps to `gdxArrayOf` with no explicit `<Step<Fight>>`. Kotlin infers the type from the `FightPlan` parameters.
- A `TODO` in `FightRule.AllDefeated`: a fight holds 1 boss. It points to the `118` `ITEM.md` row for `Fight.cast`.

Names chosen after review (owner, 2026-11-02):

- `leadPlayerAlive`, not `playerAlive`, because it reads slot 0 only and a call site must not read as "any player is alive". The owner first renamed it `playerAlive`.
- `stageBossPlan`, not `defaultStageBossSetup`, because the plan also holds the win rule and the outro, and the later presets are `midBoss`, `miniBoss` and `finalBoss`. The owner first renamed it `defaultStageBossSetup`.
