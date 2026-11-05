# Scope: mega-refactor-118g-remove-old-flow

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-05

## Goal

Remove the old boss flow. Since `118f`, all 25 live fights run on a `Fight`, so the old event branches in `MegaLevelScreen.onEvent`, the route and the 5 guards act on no live fight. Remove them, the events that only they used, and the boss parts that lose their only caller. The game must play the same.

## Item

`118`, "Boss fights as routines, rules and leases", branch 7 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118g`: remove the old screen branches, the route, the guards, and `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN`. Keep `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `INTERMEDIATE_BOSS_DEAD`, the 14 hooks, `isReady` and `betweenReadyAndEndBossSpawnEvent`."

Owner decision (2026-11-05): the hooks that lose their only caller in this branch go now, not in `118i`. See "In scope", item 4.

## Current state

Scan of `master` at `373923c8e`.

### Every boss room has a plan

The 28 `LevelDefinition` entries name 26 maps. These have 25 boss rooms, and `FightPlans.find` has a plan for the boss of each one. So no level that the game can start reaches an old branch.

### The old flow in `MegaLevelScreen`

| Part | Lines | After `118f` |
|---|---|---|
| `ENTER_BOSS_ROOM`: the `mini` and `end` reads, and `plan?.endsLevel ?: (!mini && end)` | `:694-724` | The fallback runs only for a boss with no plan |
| `ENTER_BOSS_ROOM`: `if (plan != null)` before `resolve` and `start` (the route) | `:720,723` | Always true |
| `BOSS_READY`, guard `fights.owns(boss)` | `:726-759` | Always skips |
| `END_BOSS_SPAWN`, guard `fights.active` | `:761-767` | Always skips: only steps send it, while their fight is open |
| `BOSS_DEFEATED`, guard `fights.owns(boss)` | `:769-797` | Always skips |
| `BOSS_DEAD`, guard `fights.owns(boss)` | `:799-812` | Always skips |
| `INTERMEDIATE_BOSS_DEAD`, guard `fights.owns(boss)` | `:814-822` | Always skips: only `ExitMidBossFight` sends it, and `AwaitBossGone` keeps the fight open for that `run` |

The 5 events are also in the screen's `eventKeyMask` (`:157-161`). `VICTORY_EVENT` stays: `LevelVictory` and `LevelRoomHandler.kt:129` send it.

### The events

| Event | Sent by | Other listeners | After this branch |
|---|---|---|---|
| `BOSS_READY` | `AbstractBoss.onReady` (`:249-255`) | none | goes |
| `BOSS_DEAD` | `AbstractBoss` at the end of the defeat timer (`:216`) | none | goes |
| `BEGIN_BOSS_SPAWN` | `BossSpawnEventHandler.init` (`:26`) | none | goes |
| `END_BOSS_SPAWN` | `FillBossHealthBar`, `EndBossSpawn` | `AbstractBoss` | stays |
| `BOSS_DEFEATED` | `AbstractBoss.triggerDefeat` | `Spike`, `PreciousGem` | stays |
| `INTERMEDIATE_BOSS_DEAD` | `ExitMidBossFight` | `Gate` | stays |

### Parts that lose their only caller

- `AbstractBoss.getEventOnDefeated()` and its `WilyFinalBoss` override (`WilyFinalBoss.kt:322`): the only caller is the `BOSS_DEAD` branch (`MegaLevelScreen.kt:810`).
- The private `AbstractBoss.end` (`:112,132`): its only reader is `getEventOnDefeated`.
- `AbstractBoss.getHealthFillType()` (`:240`): its only caller is the `BOSS_READY` event props (`:252`). No boss overrides it.
- `ConstKeys.HEALTH_FILL_TYPE` (`Constants.kt:449`): read only by the `BOSS_READY` branch, and written only by `onReady`.
- `FightDirector.owns` (`FightDirector.kt:15`): its only callers are the guards. `FightDirector.active` stays: the test of `reset()` reads it, as the way to see that a fight is gone.

`AbstractBoss.mini` stays: the orb default and `playBossMusic()` read it (`118i`). `HealthFillType` stays: `BossHealthHandler` and `FillBossHealthBar` use it (`118h`).

### Tests

- `MegaLevelScreenEventsTest`: 10 of its tests check the old branches and the guards. They go. The `reset()` test and the 2 `GAME_COMPLETE` tests stay.
- `FightStepsTest` and `MegaLevelScreenEventsTest` stub `getEventOnDefeated()`. The stubs go with the hook.
- `FightTest`: the "no plan" test expects `null` from `find` for `MechaDragon_OLD`, `ReactorMan_OLD` and `SphinxMiniBoss`.

### The `fights` README

- "Status" says that a boss that `find` does not find takes the old branches.
- "Events" says that guards stop the old branches, and that a plan ends with a wait step so that a guard sees the fight.

## In scope

1. `MegaLevelScreen`: remove the `BOSS_READY`, `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `BOSS_DEAD` and `INTERMEDIATE_BOSS_DEAD` branches and their `eventKeyMask` entries. At `ENTER_BOSS_ROOM`, remove the `mini` and `end` reads, the fallback and the `plan != null` checks; the level check reads `plan.endsLevel`. Remove the imports that become unused.
2. `FightPlans.find` returns a `FightPlan`, not `FightPlan?`. For a boss with no plan it throws `MissingFightPlanException` (new, in `fights/`), with the boss name and the fix in its message (root `CLAUDE.md`, "Fail fast"). The screen calls it before the spawn, as today.
3. `EventType`: remove `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN`. `AbstractBoss` stops sending `BOSS_READY` and `BOSS_DEAD`; `onReady` keeps its other work (the log and `betweenReadyAndEndBossSpawnEvent = true`). `BossSpawnEventHandler` stops sending `BEGIN_BOSS_SPAWN`.
4. Remove `getEventOnDefeated` (with the `WilyFinalBoss` override), `AbstractBoss.end`, `getHealthFillType` and `ConstKeys.HEALTH_FILL_TYPE` (owner, 2026-11-05).
5. Remove `FightDirector.owns`.
6. Tests: remove the old-branch tests and the `owns` test; `find` throws `MissingFightPlanException` for each boss with no plan; the screen listens to none of the 5 removed branch events and still listens to `VICTORY_EVENT`.
7. The `fights` README: "Status" says that every boss room needs a plan; "Events" drops the guards and says why `AwaitBossGone` stays: it keeps the fight open until the boss leaves the engine, so the fight is open while the exit event is delivered.

### New APIs

| API | First production caller |
|---|---|
| `MissingFightPlanException` | `FightPlans.find` |

### Intended changes

None that a player can see. A boss room whose boss has no plan now throws `MissingFightPlanException` at entry. Today such a room would take the old branches. No map that a `LevelDefinition` names has such a room.

## Out of scope

- The remaining hooks (`isReady`, `onReady`, `preReady`, `playBossMusic`, `onEndBossSpawnEvent` and the others), `ready`, `betweenReadyAndEndBossSpawnEvent` and `mini` (`118i`).
- `HudBars` and the bar (`118h`). The key-based gate unlock (`118j`).
- `AwaitBossGone` and the other steps and plans. No plan changes.
- `BossSpawnEventHandler` itself, which still spawns the boss.
- Old map files that name a boss with no plan. They are not live.

## Affected areas and risks

Files: `MegaLevelScreen.kt`, `FightPlans.kt`, new `MissingFightPlanException.kt`, `FightDirector.kt`, `AbstractBoss.kt`, `WilyFinalBoss.kt`, `BossSpawnEventHandler.kt`, `EventType.kt`, `Constants.kt`, `fights/README.md`, `FightTest.kt`, `FightStepsTest.kt`, `MegaLevelScreenEventsTest.kt`.

| Risk | Result if wrong | Control |
|---|---|---|
| An old branch still acted in some path | A lost controller-on, lock or music change | The scan shows each guard always skips; the 4 play checks cover a stage, a mid-level and a mini fight; death and respawn are the same code as before |
| A boss room with no plan in a live map | A throw at room entry | The scan of all 26 maps; a test lists the bosses with no plan |
| A removed event has a listener | A listener stops acting | The scan found none outside the screen; the build fails on a removed enum value |
| `onReady` loses more than the event | AI gating changes | Only the `submitEvent` call goes; `betweenReadyAndEndBossSpawnEvent` stays |

## Overlap with other branches

Active worktrees at `373923c8e`: `compactor-level` (no change in `fights/`, the bosses, `MegaLevelScreen` or `EventType`) and `x6-intro-extra-level`, which adds `D1000` to `FightPlans.find` and adds `D1000` (an `AbstractBoss`). At the scan, no file of either branch overrides `getEventOnDefeated` or `getHealthFillType`, sends a removed event, or reads `HEALTH_FILL_TYPE` or `owns`, so their merges still compile. Recheck at branch start.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | The old event chain for boss fights goes. A fight's order is one routine, with no screen branch |
| `O1-R5` | helps | Code with no caller goes. 1 new API, with a production caller |
| `O1-R4` | helps | 1 `ConstKeys` key goes |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 tests in the steps do not change; the old slot 0 tests in the screen branches go with them |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Fewer paths in the screen, and a fail-fast error for a missing plan |
| `O7-R3` | no effect | No level-identity check |

## Open questions

None. The owner chose (2026-11-05) to remove the hooks that lose their only caller in this branch.
