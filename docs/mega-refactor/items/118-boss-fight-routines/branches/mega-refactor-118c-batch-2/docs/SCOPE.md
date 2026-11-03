# Scope: mega-refactor-118c-batch-2

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Move the second batch of bosses to the `118` fight flow: `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman`. Each has 2 live fights: an end-of-level fight in its own stage, and a mid-level fight in a Wily stage. The plans from `118b` fit with no change.

## Item

`118`, "Boss fights as routines, rules and leases", branch 3 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118c`: `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan`, `PreciousWoman` (10 fights)." The `118a` `SCOPE.md`, "Later branches", gives the reason: "All turn gravity on at ready, and the presets are proven by `118b`".

## Current state

Scan of `master` at `e27c08751`.

### The fights

| Boss | Level | Map props | Room before the boss room, with its spawn |
|---|---|---|---|
| `GlacierMan` | `GLACIER_MAN` | `music=MMX6_BOSS_FIGHT_MUSIC` | `room12`, spawn `6` |
| `GlacierMan` | `WILY_STAGE_2` | `end=false`, `boss_key=2` | `room8`, spawn `4` |
| `DesertMan` | `DESERT_MAN` | none for the flow | `room16`, spawn `10` |
| `DesertMan` | `WILY_STAGE_1` | `end=false`, `boss_key=2`, `darkness=true`, `light_source_keys=1` | `room9`, spawn `8` |
| `InfernoMan` | `INFERNO_MAN` | none for the flow | `room11`, spawn `6` |
| `InfernoMan` | `WILY_STAGE_3` | `end=false`, `boss_key=1` | `room4`, spawn `4` |
| `RodentMan` | `RODENT_MAN` | none for the flow | `room16`, spawn `5` |
| `RodentMan` | `WILY_STAGE_2` | `end=false`, `boss_key=1` | `room4`, spawn `1` |
| `PreciousWoman` | `PRECIOUS_WOMAN` | none for the flow | `room7`, spawn `5` |
| `PreciousWoman` | `WILY_STAGE_3` | `end=false`, `boss_key=3` | `room7`, no spawn. The nearest spawn is `room6` (spawn `6`), before the `MoonMan` fight |

No map object sets `mini`, `health_fill_type` or `orb`. The other props (walls, children, darkness) are boss data, not fight facts.

### The 5 bosses

- None writes spawn props or overrides `getEventOnDefeated`, `playBossMusic`, `getHealthFillType` or `preReady`. So they use the default music, the `BIT_BY_BIT` fill, and the exit from `end` (`AbstractBoss.kt:86`, `:235-240`). This is the same as the `118b` bosses.
- `isReady` is a timer in each, and each reads `betweenReadyAndEndBossSpawnEvent` to hold its AI. `118` keeps that flag until `118i`.
- "Gravity on at ready" is a body flag (`body.physics.gravityOn = true`), set in `onSpawn` and again in `onReady` after `super.onReady()` (for example `PreciousWoman.kt:262`, `:340-344`). It is not world gravity, and it is not part of the fight flow.
- At the defeat, `RodentMan` destroys its `RatRobot` children and `PreciousWoman` destroys its shield gems in `triggerDefeat`. Both run in the boss, on both flows.

### Listeners in the fight rooms

- `IBossListener`: `BulbBlaster` (Wily 1 `DesertMan` room), `PipePuff` (Wily 2 `RodentMan` room), and the `FloatingCan` children of `FloatingCanHole` (Wily 2 `GlacierMan` room). `NotifyListenersOfBossDefeat` calls them in the same tick as the old branch.
- `BOSS_DEFEATED`: `Spike` (Wily 3 `PreciousWoman` room) and `PreciousGem`. `AbstractBoss` still sends this event on both flows.
- `Gate` opens on `INTERMEDIATE_BOSS_DEAD` with the matching `boss_key`. `ExitMidBossFight` sends it.

### Two corrections to earlier docs

- The screen field `music` is written only at level start (`MegamanMaverickGame.kt:1061`). A room `music` property plays a track (`MegaLevelScreen.kt:562-571`) but does not write the field. So the `118b` `SCOPE.md` line "a room can change" the field is not exact. The supplier is still correct, because it reads the field when the step runs, as the old branch did. No `game_rooms` object in the 8 levels has a `music` property.
- The `fights` README says "a value that the screen can change during the fight, such as the level music". It must say a value that the screen owns.

### Two fights on the new flow in a row

In `WILY_STAGE_3`, gate `578` leads from the `PreciousWoman` room to the `TimberWoman` room. After `118c`, both fights are on the new flow. `FightDirector.start` replaces the current fight with no cancel (`FightDirector.kt:17-25`). The first fight closes 1 tick after the boss is gone (`AwaitBossGone`), and a room transition takes many frames, so an overlap is not expected (inference). If one did happen, the leases of the first fight would not be released.

## In scope

1. Add `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` to `FightPlans.find`, with no plan music. The plan comes from `end`, as in `118b`.
2. `FightDirector.start` cancels an open fight before it starts the new one. On the expected path there is no open fight, so nothing changes. If there is one, its leases are released.
3. Change the README line about the supplier.
4. Tests:
   - `find`: a plan with no music for the 5 bosses, for `end=false` and `end=true`. The "no plan" list loses them.
   - `start` with an open fight: the open fight's leases are released once, and the new fight is the active one.

### New APIs

None. `FightDirector.start` keeps its signature.

### Intended changes

None that a player can see. The 5 mid-level fights get the `118b` intended change: the unlock, the level music and the first free input frame come 1 frame earlier.

## Out of scope

- The `GlacierMan` map `music` prop. Its value is the default, and a map change was out of scope in `118b`. The prop stays, and `resolve` writes nothing over it.
- The `DesertMan` darkness and light setup. It is boss and room data, not the flow. A play check covers it.
- All boss AI, `AbstractBoss`, `Gate`, `Spike`, `PreciousGem` and the map files.
- A temporary contract suite. The plans and steps are the ones that `118a` and `118b` proved.

## Affected areas and risks

Files: `FightPlans.kt`, `FightDirector.kt`, `README.md` in `fights/`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| A boss missed in `find` | It stays on the old flow, with no visible change | A test lists the 9 moved bosses |
| The wrong plan | A mid-level fight ends the level | `find` reads `end`, as in `118b`, and a test covers both values for each new boss |
| `GlacierMan` music | A wrong track | `resolve` writes nothing, so the map prop and the default apply as today |
| Two fights in a row in `WILY_STAGE_3` | A frozen world or a stuck bar | The cancel in `start`, a test, and a play check of `MoonMan` → `PreciousWoman` → `TimberWoman` |
| Defeat listeners | A hazard stays after the fight | The listener step and `BOSS_DEFEATED` do not change. Play checks in the Wily rooms with listeners |

## Overlap with other branches

- Active worktrees at `e27c08751`: `compactor-level`, `mega-refactor-019c-merge-spawn-queues`, `mega-refactor-068a-slopes`, `x6-intro-extra-level`. Recheck at branch start for `fights/`, `MegaLevelScreen`, `AbstractBoss` and the 5 bosses.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 10 more fights run on one routine. After this branch, 17 of 25 live fights are on the new flow |
| `O1-R5` | no effect | No new API |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Named checks for each of the 10 fights |
| `O7-R3` | no effect | The route reads the boss name and the map's `end` value |

## Open questions

1. The cancel in `FightDirector.start` (in scope, item 2). I recommend it: no API change, nothing changes on the expected path, and it closes the one way that a lease could stay held. The other choice is a play check only.
