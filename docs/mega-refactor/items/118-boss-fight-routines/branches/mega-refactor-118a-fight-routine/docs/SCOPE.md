# Scope: mega-refactor-118a-fight-routine

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-02

## Goal

Build the core of the `118` fight design, and move exactly one boss onto it: `BigAssMaverickRobot`, the intro stage boss. All other 24 live fights stay on the old flow and play as they do today. The player sees no change.

## Item

`118`, "Boss fights as routines, rules and leases". This branch takes these parts of `ITEM.md`:

- One `Fight` object owns the life of a fight, and one `Routine` owns its order.
- A step changes shared state only through a lease, and `Fight` releases the leases at close.
- A rule ends the wait. A step never polls the win itself.
- No loop and no branch in the step type.
- `FightPlans.resolve` is the one source for fight facts, and it runs before the spawn.
- `118a` adds `Step`, and `082a` reuses it.

The owner's phased approach (2026-11-02) replaces "move all 25 fights in `118a`". See "Later branches".

## Current state

Scan of `master` at `093049dc0`.

### The old flow

| Event | Sent by | Handled at | What it does |
|---|---|---|---|
| `ENTER_BOSS_ROOM` | `LevelRoomHandler.kt:128` | `MegaLevelScreen.kt:691-714` | Reads `mini` and `end` from the map, skips to `VICTORY_EVENT` if the level is defeated, and calls `BossSpawnEventHandler.init` |
| `BOSS_READY` | `AbstractBoss.kt:250` | `MegaLevelScreen.kt:716-748` | No bar for a mini boss. Else starts the bar fill, pauses the music, and turns off all systems but sprites and animations |
| `END_BOSS_SPAWN` | `MegaLevelScreen.kt:720,737` | `MegaLevelScreen.kt:750-754`, `AbstractBoss.kt:173` | Controller on, all systems on. The boss clears `betweenReadyAndEndBossSpawnEvent` and starts its music |
| `BOSS_DEFEATED` | `AbstractBoss.kt:278` | `MegaLevelScreen.kt:756-783`, `Spike.kt:204`, `PreciousGem.kt:216` | Input lock, damage lock, music off, `IBossListener` loop, bar off. Does nothing if slot 0 is dead |
| `BOSS_DEAD` | `AbstractBoss.kt:216` | `MegaLevelScreen.kt:785-796` | Submits `boss.getEventOnDefeated()`. Does nothing if slot 0 is dead |
| `VICTORY_EVENT` | `MegaLevelScreen.kt:795` | `MegaLevelScreen.kt:806-810` | `endLevelEventHandler.init()` |

`BEGIN_BOSS_SPAWN` has no listener. `PLAYER_SPAWN` destroys the boss (`AbstractBoss.kt:174`), and `PLAYER_SPAWN_INIT` removes the bar and turns on all systems (`MegaLevelScreen.kt:537-539`).

### Facts that shape this branch

- `spawn` only queues, so `boss.mini` is not set while `ENTER_BOSS_ROOM` runs. Facts must resolve before the spawn.
- `events.run()` runs after `screen.render`, and a submit during `run` waits one more frame (`EventsManager.kt:30`).
- The bar handler runs first in the `when` at `MegaLevelScreen.kt:920-925`, inside `if (!game.clock.paused)`. The freeze also turns off `UpdatablesSystem`, so the fight tick must run in the screen, not in an entity.
- `bossHealthHandler` is a private field of the screen (`MegaLevelScreen.kt:195,284`).
- `Spike` and `PreciousGem` listen to `BOSS_DEFEATED`, `Gate` listens to `INTERMEDIATE_BOSS_DEAD`, and `AbstractBoss` listens to `END_BOSS_SPAWN`. These events must stay.

### `BigAssMaverickRobot`

- 1 live fight: `IntroStage_v3`, `boss_room`, map object `335`. It is a stage fight: `mini` false, `end` true by default, and no `boss_key`.
- `room12` holds player spawn `6` and the `Gate` to `boss_room`.
- It forces `ConstKeys.MUSIC` at `BigAssMaverickRobot.kt:211`, so the music has 2 sources. It is one of the 12 forced prop lines.
- `isReady` waits for the fall target and for both hands to reach `ROTATE` (`:397`). `preReady` runs the shake (`:285`). `onReady` only logs and calls `super` (`:400`).
- `betweenReadyAndEndBossSpawnEvent` gates its attacks at `:474`, and `BigAssMaverickRobotHand.kt:306` also reads it.
- Defeat: `explodeOnDefeat` in its updatable, and overrides of `spawnExplosionOrbs` and `spawnDefeatExplosion`.
- The boss does not read `mini`. No live `IBossListener` entity is known in this room; this is not checked.

### Live fights by boss

| Boss | Live fights | Preset |
|---|---|---|
| `BigAssMaverickRobot` | 1 | stage |
| `TimberWoman`, `MoonMan`, `RodentMan`, `DesertMan`, `InfernoMan`, `ReactorMan`, `GlacierMan`, `PreciousWoman` | 2 each: 1 in its own stage, 1 in a Wily stage | stage, mid |
| `ElecDevil`, `GutsTank` | 1 each, in a Wily stage | stage |
| `MoonHead`, `MechaDragon` | 1 each | mini |
| `ReactorMonkey` | 3 | mini |
| `WilyFinalBoss` | 1 | final |
| `Bospider`, `SigmaRat`, `PenguinMiniBoss` | 0 | none live |

Total: 25 fights.

## In scope

1. `Step<C>` and `Routine<C>` in `engine`, next to `state`, with tests in `engine`.
2. `Fight`, `FightPlan`, `FightRule.AllDefeated`, `Lease` and `FightDirector` in `core`.
3. `FightPlans.find(bossName)`, which gives a plan for `BigAssMaverickRobot` and `null` for every other boss, and `FightPlans.resolve`, which writes the resolved music into the spawn props.
4. The `stageBoss` preset and its steps: spawn, `AwaitReady`, `HoldWorld`, `FillBar`, `ReleaseWorld`, `ControllerOn`, `DefeatLock`, `NotifyListeners`, `HideBar`, `AwaitDefeatEnd`, `LevelVictory`.
5. A route at `ENTER_BOSS_ROOM`: a boss with a plan starts a `Fight`, and a boss with no plan takes the old flow.
6. A guard in the old screen branches, so that they do not also act on a boss that a `Fight` owns.
7. One director tick in the screen, before the `when` at `MegaLevelScreen.kt:920`, and one cancel at `PLAYER_SPAWN_INIT`.
8. Remove `BigAssMaverickRobot.kt:211`, the forced music line.
9. A temporary contract suite with a frozen copy of the 6 screen branches, and a permanent suite for `Routine`, leases and `resolve`.

### New APIs

| API | First production caller in this branch |
|---|---|
| `Step<C>`, `Routine<C>` | `Fight` |
| `Fight`, `FightPlan` | `FightDirector` |
| `FightDirector` | `MegaLevelScreen`, at `ENTER_BOSS_ROOM` and in `render` |
| `FightPlans.find`, `FightPlans.resolve`, `stageBoss` | `MegaLevelScreen`, for the `BigAssMaverickRobot` fight |
| `FightRule.AllDefeated` | `stageBoss` |
| `Lease` | `HoldWorld`, `FillBar` |
| Each step in "In scope", item 4 | `stageBoss` |

### Smaller than the `ITEM.md` sketch

These parts of the sketch have no caller in this branch, so they wait:

| Part | Waits for |
|---|---|
| `FightPlan.lose` | The first fight with a lose rule |
| `Fight.key` | `FightMemory` |
| `AllDefeated.group` | A fight with a group |
| `midBoss`, `miniBoss`, `finalBoss` presets | Their batches |
| `FightDirector` on `MegaServices` | The first entity that starts a fight. In this branch, the screen owns it |

## Out of scope

- Every boss other than `BigAssMaverickRobot`, and their forced prop lines.
- Removal of `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN`. The old flow still uses the first 2, and `AbstractBoss` still submits them.
- The 14 hooks on `AbstractBoss`, `isReady`, `ready`, `defeated`, `betweenReadyAndEndBossSpawnEvent`, and `mini`.
- All boss AI, including the brain split of `122`.
- `BossHealthHandler` and `HealthFillType`: `FillBar` calls the handler as it is.
- `Gate`, `IBossListener`, `DeathRule` and the boss death path.
- The `wily_room` check. It moves to the `WilyFinalBoss` batch.
- The `082b` talk, `say`, `parallel` and `Routine.skip`.
- A new test map. `room12` is 1 room from the fight, and it has a player spawn. `CONTRACT.md` can change this if a check needs a room that the real stage cannot give.

## Later branches

Each branch moves a batch, ships alone, and has its own short review and play checks. The old flow stays until the last batch.

| Branch | Moves | Live fights | New in the branch | Why this batch |
|---|---|---|---|---|
| `118b` | `TimberWoman`, `MoonMan`, `ReactorMan` | 6 | `midBoss`, gate unlock through the existing event | Single-body fighters with 2 fights each, and no gravity step at ready |
| `118c` | `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan`, `PreciousWoman` | 10 | none | All turn gravity on at ready, and the presets are proven by `118b` |
| `118d` | `ElecDevil`, `GutsTank`, `SigmaRat` | 2 | none | Multi-entity bosses and custom ready checks |
| `118e` | `MoonHead`, `MechaDragon`, `ReactorMonkey`, `Bospider`, `PenguinMiniBoss` | 5 | `miniBoss` | The no-bar and no-freeze path |
| `118f` | `WilyFinalBoss` | 1 | `finalBoss`, removal of the `wily_room` check | The largest boss, with a `null` exit and an end scene |
| `118g` | No boss | 0 | Remove the old screen branches, the route, the guards, `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN` | The flow has one path |
| `118h` | No boss | 0 | `HudBars`, `BarSpec`, `BarSource` | The bar leaves the flow |
| `118i` | No boss | 0 | `BossComponent`, shared with `048` step 6 | Hooks leave the base class |
| `118j` | No boss | 0 | Key-based gate unlock | Shared with `GauntletMaster` |

`Bospider`, `SigmaRat` and `PenguinMiniBoss` have no live fight, so they have only the automated checks.

## Affected areas and risks

Files: 1 screen, 1 boss, new files in `engine` and `core`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| Frame match. A step in the tick of frame F+1 must equal a handler in `events.run` of frame F | The controller returns at another frame, or the freeze starts late | Temporary contract suite with a trace per frame |
| A guard misses a branch | Old and new flow both act: a double bar, a double victory | Contract suite, and a test that each branch skips a fight-owned boss |
| Pause | An event in the frame before a pause now waits for resume, because the tick does not run in a pause. A pause is blocked in most of this flow (inference) | Named as an intended change in `CONTRACT.md`, with a play check |
| Death in the fight | A lease or a step stays on after a slot 0 death, and the respawn runs with no controller | Cancel at `PLAYER_SPAWN_INIT`, test on each step, and a play check |
| Slot 0 dead at defeat | Old branches do nothing in this case. The steps must match | Matrix row in the contract suite |
| Tick place | The tick runs in the screen until `010` | `O2-R3` stays waiting, and `010` must move it |
| Two flows on `master` | A boss change in another branch must know which flow it is on until `118g` | Route table in `FightPlans` is the one list |
| `Step` owner | `082a` could add a second type | `082` decision 16 already says that `118a` adds it |

## Overlap with other branches

- `compactor-level` and `fix/pushable-block-ledge-sliver` do not touch `MegaLevelScreen`, `AbstractBoss`, `BossHealthHandler`, `Gate`, `LevelRoomHandler` or `BigAssMaverickRobot`.
- `082` and `012` have no branch yet.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | no effect | The screen builds the director. No new static |
| `O1-R2` | helps | No new `abstract class` under `entities/`. Flow leaves the screen and the base class |
| `O1-R3` | helps | One routine owns the order for this fight. The old chain stays for 24 fights until `118g` |
| `O1-R4` | helps | The plan is typed. No new `ConstKeys` key |
| `O1-R5` | helps | Each API has a caller in the branch. See "Smaller than the `ITEM.md` sketch" |
| `O2-R1` | no effect | No input read |
| `O2-R2` | no effect | No random value |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R4` | no effect | One fight at a time. `players.forEach` keeps slot order |
| `O2-R5` | no effect | The slot 0 tests at `MegaLevelScreen.kt:759,788` move to a step with no change. `047e` owns them |
| `O2-R7` | no effect | The bar fill remains a UI timer that ends the freeze, as today |
| `O2-R9` | no effect | `TURN_CONTROLLER_*` stays with no slot, as today (`047`, `038`) |
| `O2-R10` | waiting | System writes move to one step. `012` owns the fix |
| `O4-R1` | no effect | No new mechanic from Tiled in this branch |
| `O4-R2` | no effect | No new mechanic. The real room is the check |
| `O4-R3` | waiting | Starts after `015` |
| `O5-R1` | no effect | The plan builds once per fight. A tick must not allocate |
| `O5-R2` | no effect | Not a hot path: 1 empty check per frame when no fight runs |
| `O6-R1` | no effect | CI must pass |
| `O6-R2` | helps | Named checks and a frame trace, where no test exists today |
| `O7-R3` | no effect | The route reads the boss name, not the level |
| `O8-R2` | no effect | No character check |

## Open questions

None. The owner chose (2026-11-02) that `118g` stays a separate branch.
