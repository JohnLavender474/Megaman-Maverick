# Scope: mega-refactor-118e-batch-4

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-05

## Goal

Move the mini bosses to the `118` fight flow with a new `miniBossPlan`: no bar, no freeze, no boss music, and an exit that opens the way on. `MoonHead`, `MechaDragon` and `ReactorMonkey` have 5 live fights. `PenguinMiniBoss` has no live fight and moves with tests only. `Bospider` has no live fight and is commented out (owner, 2026-11-05).

## Item

`118`, "Boss fights as routines, rules and leases", branch 5 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118e`: `MoonHead`, `MechaDragon`, `ReactorMonkey`, `Bospider`, `PenguinMiniBoss` (5 fights). Adds `miniBoss`." The `118a` `SCOPE.md`, "Later branches", gives the reason: "The no-bar and no-freeze path". The item `NOTES.md` sketch: "miniBoss: intro = SpawnCast, AwaitReady, ControllerOn; outro = NotifyListeners, AwaitDefeatEnd, gate unlock".

`ITEM.md` decision (2026-11-03): a branch that adds a new kind of plan checks the rule "a fight starts only when no fight is open" in its scope. See "Open fights".

## Current state

Scan of `master` at `ca1510f0e`. An agent did the TMX reads; I checked the `Gate` key logic, the `MechaDragon` spawn block and the old screen branches.

### The fights

All 5 boss objects are in the `children` layer, which has no builder. So each one spawns only through its boss room (`ENTER_BOSS_ROOM`). None is a plain spawner.

| Boss | Level | Boss room | Map props for the flow | Entry | Exit gate | `--room` |
|---|---|---|---|---|---|---|
| `MoonHead` | `MOON_MAN` | `mini_boss_room` | `mini=true`, `boss_key=1` | gate `380` | `381` (`boss_key=1`) | `room11` (spawn `6`) |
| `MechaDragon` | `INFERNO_MAN` | `mini_boss_room` | `mini=true`, `boss_key=1` | a 4-tile hole in the floor of `room8`; no gate | `368` (`boss_key=1`) | `room8` (spawn `4`) |
| `ReactorMonkey` | `REACTOR_MAN` | `mini_boss_room_1` | `mini=true`, `boss_key=1` | gate `194` | `196` (`boss_key=1`) | `room5` (spawn `3`); `room6` has no spawn |
| `ReactorMonkey` | `REACTOR_MAN` | `alt_path_1_mini_boss_room_1` | `mini=true`, `boss_key=2`, `heart_tank=e` | gate `198` | `197` (`boss_key=2`) | `room5` (spawn `3`); `alt_path_1_room_1` has no spawn |
| `ReactorMonkey` | `REACTOR_MAN` | `mini_boss_room_2` | `mini=true`, `boss_key=3` | gate `558` | `661` (`boss_key=3`) | `room12` (spawn `6`) |

- No map object sets `end`, `music`, `orb` or `health_fill_type` for these fights. Each level has a stage boss fight later, on the new flow since `118b`.
- The 2 `ReactorMonkey` fights before `room7` are alternative paths. A player does one of them in a pass.
- `Gate.kt:196-226`: an exit gate with a `boss_key` opens on `INTERMEDIATE_BOSS_DEAD` when the key equals `boss.bossKey`. The gate property `mini_boss=true` is in the maps, but no code reads it.

### The old mini path in `MegaLevelScreen.onEvent`

| Event | What the screen does for a mini boss |
|---|---|
| `ENTER_BOSS_ROOM` (`:694`) | It skips the "level already defeated" check, because the map `mini` is `true` (`:707`). Then it spawns the boss |
| `BOSS_READY` (`:728-729`) | It submits `END_BOSS_SPAWN`. No bar and no freeze |
| `END_BOSS_SPAWN` (`:759`) | It submits `TURN_CONTROLLER_ON`. `AbstractBoss` clears `betweenReadyAndEndBossSpawnEvent`, and plays no music, because `playBossMusic()` is `!mini` (`AbstractBoss.kt:238`) |
| `BOSS_DEFEATED` (`:767-795`) | No controller off, no damage lock and no music stop (`:778`). The `IBossListener` loop runs. The bar unset does nothing |
| `BOSS_DEAD` (`:797`) | It submits `INTERMEDIATE_BOSS_DEAD`, because `getEventOnDefeated()` gives it for a mini boss (`AbstractBoss.kt:235`). None of the 5 overrides it |
| `INTERMEDIATE_BOSS_DEAD` (`:812`) | It submits `TURN_CONTROLLER_ON`, unlocks the damage lock that was never set, and does not play the level music (`:819`). The exit gate opens |

`BOSS_DEFEATED` and `BOSS_DEAD` do nothing if slot 0 is dead, as on the new flow (`leadPlayerAlive`).

### Two sources for `mini`

`MoonHead.kt:133`, `MechaDragon.kt:206` and `PenguinMiniBoss.kt:84` force `mini=true` in `onSpawn`. The screen reads only the map (item `NOTES.md`, F2). All live maps set `mini=true` for these fights, so the forced lines change nothing in a live level. `ReactorMonkey` does not force it.

`AbstractBoss` reads `mini` for the orb default, `playBossMusic` and `getEventOnDefeated` (`AbstractBoss.kt:131,142,236,238`). These reads stay until `118i`.

### Frames

A step in the tick of frame F+1 has the same effect as a screen handler in `events.run` of frame F (item `NOTES.md`, fact 15). So:

- `END_BOSS_SPAWN` reaches its listeners in the same frame as today.
- `INTERMEDIATE_BOSS_DEAD` reaches the gate in the same frame as today.
- `TURN_CONTROLLER_ON`, after the ready and after the defeat, comes 1 frame earlier. Today it comes from a screen handler of an event that a screen handler sent. The `118b` mid-level resume has the same 1-frame change.

### Open fights

The rule "a fight starts only when no fight is open" holds for the 5 fights:

- Fights 1, 3, 4 and 5: the entry gate is `CLOSED` behind the player until a respawn (`Gate.kt:273-293`). The exit gate waits for `INTERMEDIATE_BOSS_DEAD` with the key of this boss.
- Fight 2 (`MechaDragon`): no gate. `MechaDragon.onSpawn` spawns a `Block` over the hole (`MechaDragon.kt:273-275`), and the player falls 14 tiles into the room. The exit gate waits for the key.
- The plan ends with `AwaitBossGone` after the exit event, as `midBossPlan` does. So the fight closes before the player can reach the next boss room.
- `miniBossPlan` takes no lease. So if a second fight did replace it, no world hold and no bar would stay behind.

### `Bospider`

No live map spawns it (old maps only: `TimberWoman_16x14` to `_v3`, `Test4`, `WilyStage1`). When it is not mini, its `onReady` forces `MM7_FINAL_BOSS_LOOP_MUSIC` (`Bospider.kt:202`), and `AbstractBoss` then plays the default boss music. The owner chose to comment out the class (2026-11-05), as `029c` did with `SphinxMiniBoss`. The deprecated `BossesFactory` has the only other reference (`BossesFactory.kt:17,34`).

### Defeat listeners

The boss projectiles (`Asteroid`, `SpitFireball`, `ReactorMonkeyBall`) are `AbstractProjectile`s, so they are `IBossListener`s. `FloatingCan` in `mini_boss_room_2` is an `IBossListener` that the `ReactorMonkey` defeat must destroy (`FloatingCan.kt:64`). `NotifyListenersOfBossDefeat` calls them in the same tick position as the old branch. The rooms have no `Spike` or `PreciousGem`.

## In scope

1. `FightPlans.find` reads the map `mini` (default `false`), as it reads `end`. With `mini=true` it gives `miniBossPlan`, whatever `end` says (owner, 2026-11-05):
   - intro: `AwaitBossReady`, `EndBossSpawn`, `ControllerOn`;
   - win: `AllDefeated`;
   - outro: `NotifyListenersOfBossDefeat`, `AwaitBossDefeatEnd`, `ExitMidBossFight`, `ControllerOn`, `AwaitBossGone`;
   - no music.
2. Add `MoonHead`, `MechaDragon`, `ReactorMonkey` and `PenguinMiniBoss` to the `when` in `find`, with no plan music.
3. Remove the forced `mini` lines from `MoonHead`, `MechaDragon` and `PenguinMiniBoss` (owner, 2026-11-05), so the map is the one source.
4. Comment out `Bospider.kt`, with a `TODO` as in `SphinxMiniBoss.kt`, and remove its 2 lines in `BossesFactory` (owner, 2026-11-05).
5. Tests:
   - `find` gives `miniBossPlan` for `mini=true`, with `end` absent, `true` and `false`;
   - its steps, in order, and no music;
   - the moved bosses with no `mini` still get the stage or mid plan;
   - the "moved" and "no plan" lists, with `Bospider` out of both.

### New APIs

| API | First production caller |
|---|---|
| `EndBossSpawn` step: submits `END_BOSS_SPAWN` | `miniBossPlan` |
| `miniBossPlan()` (private) | `FightPlans.find` |

### Intended changes

- `TURN_CONTROLLER_ON` comes 1 frame earlier after a mini boss is ready and after its defeat (owner, 2026-11-05).
- `PenguinMiniBoss` on a map with no `mini` is no longer mini. No live map has it.
- `Bospider` can no longer spawn. No live map has it; an old map logs a spawner error for it.

## Out of scope

- The unlock of a damage lock that a mini fight never sets. `miniBossPlan` leaves it out.
- The name of `ExitMidBossFight`, which the mini plan also uses.
- The `MechaDragon` spawn block, which is never destroyed and does not get the map `body_labels`. It is the same on both flows.
- The `mini` reads in `AbstractBoss` (`118i`), the key-based gate unlock (`118j`), and the `mini` map key (`022`).
- All boss AI, `Gate` and the map files.

## Affected areas and risks

Files: `FightPlans.kt`, `FightSteps.kt`, `MoonHead.kt`, `MechaDragon.kt`, `PenguinMiniBoss.kt`, `Bospider.kt`, `BossesFactory.kt`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| A bar or a freeze in a mini fight | The fight pauses, or a bar shows | The plan has no bar or freeze step. A test checks the exact steps. Play checks |
| Boss music in a mini fight | The level music stops | No plan music, and `playBossMusic()` stays `!mini`. Play checks |
| The exit gate does not open | Soft lock | `ExitMidBossFight` sends `INTERMEDIATE_BOSS_DEAD` with the boss, as for the mid-level fights. Play checks of all 3 bosses |
| `mini` from the map only | A boss is not mini | All 5 live fights set `mini=true`. A test checks the plan for `mini=true` |
| The `FloatingCan`s stay after `ReactorMonkey` | Cans in the next room | The listener step runs as the old branch did. Play check in `mini_boss_room_2` |
| The `MechaDragon` room has no gate | A second fight starts while this one is open | The spawn block shuts the hole. The plan holds no lease |
| `Bospider` commented out | A compile or a smoke test error | The full check. The factory lines go |

## Overlap with other branches

Active worktrees at `ca1510f0e`: `compactor-level` (no change in `fights/`, the bosses or the fight tests) and `x6-intro-extra-level`, which adds `D1000` to the `when` in `FightPlans.find`. Its text conflict there grows by the entries of this branch. Recheck at branch start.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 5 more live fights run on one routine. After this branch, 24 of 25 live fights are on the new flow; only `WilyFinalBoss` is left. The screen's `mini` branches are then dead code until `118g` |
| `O1-R5` | no effect | 2 new APIs, each with a production caller in this branch |
| `O1-R4` | no effect | `mini` is an existing key. No new key |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic. The 5 fights are in real levels |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Named checks for each live fight |
| `O7-R3` | no effect | The route reads the boss name and the map values, not the level |

## Open questions

None. The owner chose (2026-11-05) the map `mini` as the one source, accepted the 1-frame earlier controller, and chose to comment out `Bospider`.
