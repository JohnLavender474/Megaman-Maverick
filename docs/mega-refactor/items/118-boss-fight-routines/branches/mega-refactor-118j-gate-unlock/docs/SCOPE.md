# Scope: mega-refactor-118j-gate-unlock

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Make 1 keyed unlock for `Gate`. A boss fight and a `GauntletMaster` send the same event with a key, and a `Gate` with that key unlocks. The 2 events `INTERMEDIATE_BOSS_DEAD` and `GAUNTLET_CLEARED` go. The fight takes its key from the map, so `AbstractBoss.bossKey` goes and `Gate` no longer needs `AbstractBoss`.

## Item

`118`, "Boss fights as routines, rules and leases", branch 10 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118j`: key-based gate unlock for `GauntletMaster` and bosses (`Gate.kt:196-226`). `GauntletMaster` does not become a `Fight`."

Item decision (2026-11-02, owner, corrected 2026-11-04): "`GauntletMaster` does not become a `Fight`. It shares only the key-based gate unlock in `118j`."

Item finding `F10` (`NOTES.md`): "Room lock is emergent. It comes from gate state, and it has 2 unlock channels." Finding `F6` names `Gate.kt:198` as a site that needs `AbstractBoss` by type.

Owner decisions (2026-11-05):

- The fight plan reads `boss_key` from the boss map props, as it reads the music. `AbstractBoss.bossKey` goes.
- The boss keys and the gauntlet keys share 1 namespace in a level. A `Gate` with both properties throws at spawn.

## Current state

Scan of `master` at `e3d89f0e5`.

### The 2 channels

| Channel | Sender | Payload | `Gate` match |
|---|---|---|---|
| Boss | `ExitMidBossFight` (`FightSteps.kt:158-169`), in `midBossPlan` and `miniBossPlan`. It sends only when slot 0 is alive | `INTERMEDIATE_BOSS_DEAD`, `BOSS` = the boss | `boss_key` property equals `boss.bossKey` (`Gate.kt:196-210`) |
| Gauntlet | `GauntletMaster` when the last wave is dead (`GauntletMaster.kt:286-291`) | `GAUNTLET_CLEARED`, `KEY` = the master's `key` | `gauntlet_key` property equals `KEY` (`Gate.kt:212-226`) |

- On a match, `Gate` sets `triggerable = true`. A `Gate` with neither property is unlocked by default (`Gate.kt:239`). The `trigger` map property can override the start value (`:152`). `PLAYER_SPAWN` locks a resettable gate again (`:178-181`, `:230-237`).
- `AbstractBoss.bossKey` (`AbstractBoss.kt:107`) comes from the `boss_key` spawn prop, with the default `"NO_BOSS_KEY"` (`:128`). `Gate.kt:198-200` is its only reader.
- `Gate` is the only listener of both events. `MegaLevelScreen` does not listen to either; `MegaLevelScreenEventsTest.kt:54` checks that its mask has no `INTERMEDIATE_BOSS_DEAD`.
- The fight reads the boss map props at `ENTER_BOSS_ROOM` (`MegaLevelScreen.kt:682-686`) and gives them to `FightPlans.find`.

### Map data

Live maps that `LevelDefinition` names:

| Map | Keys |
|---|---|
| `Gauntlet1_v2.tmx`, `Gauntlet2.tmx` | 8 gates each, `gauntlet_key` `g0` to `g7` |
| `MoonMan_16x14_v5.tmx` | `MoonHead` (mini) and 1 gate, `boss_key` `1` |
| `InfernoMan_16x14_v3.tmx` | `MechaDragon` (mini) and 1 gate, `boss_key` `1` |
| `ReactorMan_v3.tmx` | 3 `ReactorMonkey` (mini) and 3 gates, `boss_key` `1` to `3`; 2 gates are in `room7` |
| `WilyStage1_v2.tmx`, `WilyStage2_v2.tmx` | 2 mid bosses and 2 gates each, `boss_key` `1`, `2` |
| `WilyStage3_v4.tmx` | 4 mid bosses and 4 gates, `boss_key` `1` to `4` |

No map uses both kinds of key. No gate has both properties. Each mid and mini boss of a live map has a `boss_key`. The map data does not change.

### Frames

Both senders submit in a frame, and `events.run` of the same frame delivers to the gate. The new event keeps this, so the gate unlocks in the same frame as today.

## In scope

1. A new `EventType.UNLOCK_GATE` with `ConstKeys.KEY`. It replaces `INTERMEDIATE_BOSS_DEAD` and `GAUNTLET_CLEARED`, which go.
2. `Gate`: 1 key, read from `boss_key` or `gauntlet_key`. It unlocks on `UNLOCK_GATE` with an equal key. A gate with both properties throws at spawn, with a message that says which gate and how to fix it. The `AbstractBoss` import goes.
3. `GauntletMaster` sends `UNLOCK_GATE` with its key. Its class doc names the new event.
4. `FightPlan` gets the gate key. `FightPlans.find` reads `boss_key` from the boss map props.
5. `ExitMidBossFight` becomes `UnlockGate`. It sends `UNLOCK_GATE` with the plan key, only when slot 0 is alive, as today. A plan with no key sends nothing. Its `isFinished` does not change.
6. `AbstractBoss.bossKey` and `"NO_BOSS_KEY"` go.
7. The `fights` README: the exit of a mid-level fight unlocks a gate by its key. Only if a line there names the old event.
8. Tests: `Gate` unlocks on its key from either property, stays locked for another key, locks again on `PLAYER_SPAWN`, and throws with both properties; `FightPlans.find` puts `boss_key` in the plan; `UnlockGate` sends the key, and sends nothing with no key or with slot 0 dead; the screen mask test names `UNLOCK_GATE`.

### New APIs

| API | First caller |
|---|---|
| `EventType.UNLOCK_GATE` | `UnlockGate`, `GauntletMaster` (send); `Gate` (listen) |
| The gate key on `FightPlan` | `FightPlans.find` (writes), `UnlockGate` (reads) |
| `UnlockGate` (rename of `ExitMidBossFight`) | `midBossPlan`, `miniBossPlan` |

### Intended changes

None that a player can see.

## Out of scope

- The map property names `boss_key` and `gauntlet_key`, and the map data.
- The `Gate` states, timers, `trigger`, `reset` and `PLAYER_SPAWN` lock.
- `GauntletMaster` waves and its reset on a player death (`docs/issues/2026-11-02-slot-1-death-acts-on-p1-objects.md`).
- A gate unlock from a stage-end fight (`LevelVictory`) or from the final boss.
- The `gauntlet-stages` skill: the map properties do not change, so its text stays true.
- `IBossListener` and the other `AbstractBoss` hooks (`048` step 6).

## Affected areas and risks

Files: `EventType.kt`, `Gate.kt`, `GauntletMaster.kt`, `FightPlan.kt`, `FightPlans.kt`, `FightSteps.kt`, `AbstractBoss.kt`, maybe `fights/README.md`, and the tests (`FightStepsTest`, `FightTest` if it names the step, `MegaLevelScreenEventsTest`, a new `GateTest`).

| Risk | Result if wrong | Control |
|---|---|---|
| A boss and a gauntlet in 1 level use the same key | Both gates open at the first unlock | No live map has both kinds. The `Gate` code and this branch's docs say that keys share 1 namespace |
| The plan reads the key from the wrong props | A mid-level gate never opens, and the player is stuck | A `FightPlans` test; the control play checks in a mid-boss and a mini-boss room |
| `ReactorMan` `room7` has 2 gates with keys `1` and `2` | One monkey opens the wrong gate | The keys are compared as today; a play check in that level |
| A gate with both properties in a map not in `LevelDefinition` | A throw at spawn | Only live maps are in scope; the scan found none with both |
| The `x6-intro-extra-level` merge of `master` | A text conflict in `FightPlans.kt` | That branch changes only `D1000` lines in the `when`; the new key read is outside it |

## Overlap with other branches

Active worktrees at `e3d89f0e5`:

- `compactor-level`: `Compactor.tmx` has no `boss_key` or `gauntlet_key`. No gate or fight code.
- `x6-intro-extra-level`: changes the `D1000` lines in `FightPlans.kt`. `X6Intro_16x14_v1.tmx` has no `boss_key` or `gauntlet_key`. It must still adapt its 2 `D1000` reads from `118i`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` | helps | `Gate` no longer needs `AbstractBoss` by type, and 1 boss field goes |
| `O1-R3` | helps | 2 unlock channels become 1. The delivery frame does not change |
| `O1-R4` | no effect | No new `ConstKeys` key; the event uses `KEY`. The keys come from map data |
| `O1-R5` | no effect | Each new API has a production caller (table above) |
| `O2-R4` | no effect | No iteration order changes |
| `O2-R9` | no effect | The event is not about a player |
| `O4-R1` | no effect | The mechanic already comes from Tiled, and the properties stay |
| `O4-R2` | no effect | No new mechanic. The checks use real levels |

## Open questions

None. The owner chose (2026-11-05) that the plan reads the key from the map, and 1 key namespace.
