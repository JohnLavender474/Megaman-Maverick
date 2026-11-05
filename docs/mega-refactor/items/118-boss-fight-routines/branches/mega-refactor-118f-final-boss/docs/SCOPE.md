# Scope: mega-refactor-118f-final-boss

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-05

## Goal

Move the last live fight, `WilyFinalBoss` in `WILY_STAGE_3`, to the `118` fight flow with a new `finalBossPlan`, and remove the "level already defeated" check for its room (`wily_room`). After this branch, all 25 live fights run on a `Fight`.

## Item

`118`, "Boss fights as routines, rules and leases", branch 6 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118f`: `WilyFinalBoss` (1 fight). Adds `finalBoss`, and removes the `wily_room` check." The `118a` `SCOPE.md`, "Later branches", gives the reason: "The largest boss, with a `null` exit and an end scene".

`ITEM.md` decision (2026-11-02, owner): "The `WilyFinalBoss` batch removes the `isLevelDefeated` check for `wily_room`, as a named intended change. The research infers that normal play cannot reach it: the write at `MegaLevelScreen.kt:829` runs on `END_LEVEL`, and `Wily` ends with `GAME_COMPLETE`."

`ITEM.md` decision (2026-11-03): a branch that adds a new kind of plan checks the rule "a fight starts only when no fight is open" in its scope. See "Open fights".

## Current state

Scan of `master` at `c19e75ad9`.

### The fight

- `WILY_STAGE_3` (`WilyStage3_v4.tmx`), boss room `wily_room` (room `636`, `fade_out_music=true`), boss object `641`. The object sets no `mini`, `end`, `music` or `orb`, so the screen reads `mini=false` and `end=true`.
- There is no entry gate. The player drops from `room8` (spawn `7`) into `room9`, which has no spawn, and then through a 4-tile hole into `wily_room`. Block `650` (`spawn_room=wily_room`, `events=end_room_trans`) spawns over the hole when the room transition ends, so the room is shut for the fight.
- The 4 earlier fights in this level (`InfernoMan`, `MoonMan`, `PreciousWoman`, `TimberWoman`, all `end=false`) are on the new flow since `118b` and `118c`.

### What the boss does

- `onSpawn` forces `MUSIC = OUR_BLOOD_BOILS_8_BIT`, `ORB = false` and `END = false` (`WilyFinalBoss.kt:237-239`). The screen reads the map `end` (`true`) before the spawn, and the boss reads the forced `false` (item `NOTES.md`, F2).
- `isReady` is `!initSequence` (`:380`). The init swoop waits until all players are on the ground (`:448`).
- `playBossMusic()` is "no music plays now" (`:333`). The room fades out the level music, so the boss track plays at `END_BOSS_SPAWN`.
- `onEndBossSpawnEvent` also ends the phase transition (`:327-331`).
- 3 phases. In phases 1 and 2, `triggerDefeat` starts a phase transition and does not call `super`, so `defeated` stays `false` and no `BOSS_DEFEATED` is sent (`:344-357`). A phase change refills the health with `setHealthToMax()` (`:3354`), with no bar fill. Only phase 3 calls `super.triggerDefeat()`.
- `getEventOnDefeated()` is `null` (`:325`), so `BOSS_DEAD` sends nothing.
- The end scene is in `onDestroy` (`:289-311`): if the target player is alive, the health is depleted and the phase is 3, it spawns `DrWily`, locks player damage (`WILY_FINAL_BOSS`) and sends `TURN_CONTROLLER_OFF`. `DrWily` later sends `GAME_COMPLETE` (`DrWily.kt:151`), and the screen opens the ending (`MegaLevelScreen.kt:852`).

### The old flow for this fight

| Event | What the screen does |
|---|---|
| `ENTER_BOSS_ROOM` | `!mini && end` is `true`, so if `WILY_STAGE_3` is defeated it sends `VICTORY_EVENT` and spawns no boss (`MegaLevelScreen.kt:707-713`). Else it spawns the boss |
| `BOSS_READY` | The bar fill (`BIT_BY_BIT`), the music pause, and the freeze of all systems but sprites and animations |
| `END_BOSS_SPAWN` | Controller on, all systems on |
| `BOSS_DEFEATED` (phase 3 only) | Controller off, damage lock, music stop, the listener loop, the bar removal |
| `BOSS_DEAD` | Nothing, because the event is `null` |

So the flow is the stage flow, with no exit. `standardBossPlan` without `LevelVictory` fits it.

### Open fights

The rule "a fight starts only when no fight is open" holds. Block `650` shuts the room, the level has no later fight, and the game ends after this one. The fight closes when the boss is gone. The `fights` README says "a gate shuts each fight room", which is not true for `MechaDragon` (`118e`) and this fight: there a spawned block shuts the room.

## In scope

1. `finalBossPlan`, chosen by name in `FightPlans.find` (`mini` and `end` are not read for it):
   - intro: the stage intro (`AwaitBossReady`, `HoldWorld`, `FillBossHealthBar`, `ReleaseWorld`, `ControllerOn`);
   - win: `AllDefeated`;
   - outro: `BossDefeatLock`, `NotifyListenersOfBossDefeat`, `HideBossHealthBar`, `AwaitBossDefeatEnd`, with no exit step;
   - music: `OUR_BLOOD_BOILS_8_BIT`.
2. Remove the forced `MUSIC` and `END` lines from `WilyFinalBoss.onSpawn`. `resolve` writes the music, as for the other bosses. `end` has no reader for this boss, because `getEventOnDefeated` is overridden. `ORB = false` stays: it is boss data, as in `MoonHead`.
3. The "level already defeated" check at `ENTER_BOSS_ROOM` applies only to a plan that ends the level: `FightPlan.endsLevel` (its outro has `LevelVictory`). `find` moves before the check. For each stage, mid-level and mini plan this gives the same result as `!mini && end`. For `finalBossPlan` the check goes (owner, 2026-11-02). A boss with no plan keeps `!mini && end` until `118g`.
4. The end scene stays in `WilyFinalBoss.onDestroy` (owner, 2026-11-05).
5. The `fights` README: the open-fight rule says that a gate or a spawned block shuts each fight room.
6. Tests: `find` gives `finalBossPlan` for `WilyFinalBoss` with any `mini` and `end`, with its exact steps and music; `resolve` writes the music; the "moved" and "no plan" lists; `endsLevel` is `true` only for `standardBossPlan`.

### New APIs

| API | First production caller |
|---|---|
| `finalBossPlan()` (private) | `FightPlans.find` |
| `FightPlan.endsLevel` | `MegaLevelScreen` at `ENTER_BOSS_ROOM` |

### Intended changes

- In a defeated `WILY_STAGE_3`, `wily_room` spawns the boss, not `VICTORY_EVENT` (owner, 2026-11-02). Normal play cannot reach this (inference, item `NOTES.md`, fact 14).
- If slot 0 is dead when the Wily defeat animation ends, the bar goes at that moment, because the fight closes. Today it stays until the respawn (owner, 2026-11-05).

## Out of scope

- The end scene as steps, and the controller that it leaves off on a death path (item `NOTES.md`, risk 9). `082` owns scene steps.
- The 3 phases, the phase transition and the health refill. They stay in the boss.
- The "all players on the ground" ready check (`047e`), and the `mini` reads in `AbstractBoss` (`118i`).
- The removal of the old screen branches, the route and the guards (`118g`).
- All boss AI, `DrWily`, `Gate` and the map files.

## Affected areas and risks

Files: `FightPlans.kt`, `FightPlan.kt`, `MegaLevelScreen.kt`, `WilyFinalBoss.kt`, `fights/README.md`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The wrong music, or a second track | A wrong or a silent boss track | The plan gives the same value as the removed line, and `playBossMusic()` does not change. A test checks the `resolve` value. Play check |
| A phase change ends the fight | The fight closes in phase 1 or 2 | `AllDefeated` reads `defeated`, which only phase 3 sets. Play check of all 3 phases |
| The fight closes before `DrWily` | No end scene | `onDestroy` is unchanged and runs at the start of the next `engine.update`, whatever the fight does. Play check to the ending |
| The level check moves | A stage boss in a defeated level spawns again | `endsLevel` gives the old result for each plan; a test checks each plan |
| `find` before the check | A plan for a skipped fight | `find` only reads props; the check returns before the spawn and the `start` |

## Overlap with other branches

Active worktrees at `c19e75ad9`: `compactor-level` (no change in `fights/`, the bosses or `MegaLevelScreen`), `fix/spawner-rearm-margin`, which merged into `master` at `48a6ff917` with 1 line in `MegaLevelScreen` at `PLAYER_SPAWN_INIT`, not near the boss branches, and `x6-intro-extra-level`, which adds `D1000` to the `when` in `FightPlans.find` (the same text conflict as after `118e`).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | The last live fight runs on one routine. All 25 live fights are on the new flow, and the old screen branches have no live caller until `118g` |
| `O1-R5` | no effect | 2 new APIs, each with a production caller in this branch |
| `O1-R4` | no effect | The music goes through the existing `MUSIC` spawn prop. No new key |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change. The "all players landed" check stays for `047e` |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic |
| `O5-R1` | no effect | No per-frame allocation. `endsLevel` runs once per boss room entry |
| `O6-R2` | helps | A named check for the last fight |
| `O7-R3` | no effect | The check still reads whether the current level is defeated, as today. No new level-identity check; the plan decides if the check applies |

## Open questions

None. The owner chose (2026-11-05) to keep the end scene in the boss, and accepted the earlier bar removal when slot 0 is dead.
