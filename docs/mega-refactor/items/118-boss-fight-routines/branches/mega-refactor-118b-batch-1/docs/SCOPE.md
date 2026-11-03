# Scope: mega-refactor-118b-batch-1

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Move the first batch of bosses to the `118` fight flow: `TimberWoman`, `MoonMan` and `ReactorMan`. Each has 2 live fights: an end-of-level fight in its own stage, and a mid-level fight in a Wily stage. The player sees no change.

## Item

`118`, "Boss fights as routines, rules and leases", branch 2 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118b`: `TimberWoman`, `MoonMan`, `ReactorMan` (6 fights). Adds `midBoss`." The scan below shows that the batch needs no separate `midBoss` plan.

## Current state

Scan of `master` at `16a708322`.

### The fights

| Boss | Level | Room | Map props | Room before, with its spawn |
|---|---|---|---|---|
| `TimberWoman` | `TIMBER_WOMAN` | `boss_room` | none | `room14`, spawn `6` |
| `TimberWoman` | `WILY_STAGE_3` | `timber_woman_room` | `end=false`, `boss_key=4` | `precious_woman_room`, no spawn. The nearest spawn is `room6`, which needs the `MoonMan` and `PreciousWoman` fights first |
| `MoonMan` | `MOON_MAN` | `boss_room` | none | `room14`, spawn `9` |
| `MoonMan` | `WILY_STAGE_3` | `moon_man_room` | `end=false`, `boss_key=2` | `room6`, spawn `6` |
| `ReactorMan` | `REACTOR_MAN` | `boss_room` | none | `room14`, spawn `8` |
| `ReactorMan` | `WILY_STAGE_1` | `reactor_man_room` | `end=false`, `boss_key=1` | `room6`, spawn `5` |

### The 3 bosses

- No forced spawn props. They use the default boss music, `MMX6_BOSS_FIGHT_MUSIC` (`AbstractBoss.kt:86`), and no map object sets `music`.
- `isReady` is a timer in each. Only `MoonMan` overrides `onReady`, and it only logs and calls `super` (`MoonMan.kt:259`).
- Each reads `betweenReadyAndEndBossSpawnEvent` to hold its AI (`TimberWoman.kt:591`, `MoonMan.kt:289`, `ReactorMan.kt:293`). `118` keeps that flag until `118i`.
- `MoonMan` changes world gravity in the fight (`MoonMan.kt:526`). It is not part of the fight flow.

### The mid-level fight in the old flow

A mid-level fight is a stage fight with `end=false`. The two flows differ only at the end:

1. `AbstractBoss.getEventOnDefeated()` gives `INTERMEDIATE_BOSS_DEAD` in place of `VICTORY_EVENT` (`AbstractBoss.kt:236`).
2. The screen's `INTERMEDIATE_BOSS_DEAD` branch (`MegaLevelScreen.kt:814`) turns the controller on, removes the `BOSS_DEFEATED` damage lock from each player, and plays the level music again.
3. `Gate` reads the same event and opens the gate whose `boss_key` matches (`Gate.kt:196-209`).

The intro, the bar, the freeze, the defeat lock, the listener calls and the bar removal are the same as in a stage fight.

### What `118a` gives

`stageBossPlan` already sends `boss.getEventOnDefeated()` at the end (`LevelVictory`). So for a mid-level fight, it sends `INTERMEDIATE_BOSS_DEAD` with no change. The screen's `INTERMEDIATE_BOSS_DEAD` branch has no guard, so it acts for a boss that a fight owns.

`FightPlan.music` is not nullable, and `FightPlans.resolve` always writes it. These 3 bosses have no music of their own, so a plan for them must not write the prop.

## In scope

1. Add `TimberWoman`, `MoonMan` and `ReactorMan` to `FightPlans.find`, with the stage plan.
2. Make `FightPlan.music` nullable. `resolve` writes it only when it is set. The 3 bosses have none, so the map value or the boss default is used, as today.
3. Leave the screen's `INTERMEDIATE_BOSS_DEAD` branch shared by both flows (decision 1 below).
4. Rename the stage plan and the exit step, so that their names fit a mid-level fight too (decision 2 below).
5. Tests: `FightPlans.find` gives a plan for the 4 moved bosses and `null` for the others; a mid-level fight sends `INTERMEDIATE_BOSS_DEAD` and no `VICTORY_EVENT`; the `INTERMEDIATE_BOSS_DEAD` branch acts for a boss that a fight owns.

### New APIs

None. `FightPlan.music` changes from `MusicAsset` to `MusicAsset?`. Its caller is `FightPlans.find`.

## Out of scope

- A separate `midBoss` plan. No step differs from the stage plan.
- The `GlacierMan` map music and the forced music of other bosses. They come with their batches.
- Moving the `INTERMEDIATE_BOSS_DEAD` resume into a step. This goes in `118g` (decision 1).
- All boss AI, `AbstractBoss`, `Gate` and the map files.
- A temporary contract suite. This batch adds no step and no new screen path. The `118a` suite proved the stage plan, and the mid-level end reuses the old screen branch. `CONTRACT.md` can add a suite if the owner wants one.

## Affected areas and risks

Files: `FightPlans.kt`, `FightPlan.kt`, `FightSteps.kt` (a rename only), and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The mid-level end | The gate stays shut, or the controller and the music do not come back | The shared screen branch is the same code as today, and a test checks that it acts. Play checks in each mid-level room |
| Music | A wrong track, or no track | `resolve` writes nothing for these 3, so `AbstractBoss` reads the same prop as today. A test checks that |
| `MoonMan` gravity | A gravity flip stays after the fight | Not part of the flow. A control check covers it |
| The `WILY_STAGE_3` `TimberWoman` check | It needs 2 old-flow fights first | The `MoonMan` fight on the way is on the new flow, and the `PreciousWoman` fight is a control |

## Overlap with other branches

- `compactor-level` and `fix/pushable-block-ledge-sliver` do not touch `fights/`, `MegaLevelScreen`, `AbstractBoss` or these 3 bosses (checked in `118a`; recheck at branch start).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 6 more fights run on one routine. The resume after a mid-level fight stays an event until `118g` |
| `O1-R5` | no effect | No new API. A nullable field gets its caller in this branch |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change |
| `O2-R9` | no effect | `TURN_CONTROLLER_*` stays with no slot |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic. The real rooms are the checks |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Named checks for each of the 6 fights |
| `O7-R3` | no effect | The route reads the boss name |

## Open questions

1. **Does the screen's `INTERMEDIATE_BOSS_DEAD` branch stay shared in this batch?** Today it runs in `events.run()` one frame after the plan sends the event. A `ResumeLevel` step would run in the tick before that `run`. It would remove the damage lock and play the music 1 frame earlier, unless the plan waits 1 tick. Recommended: keep the branch shared in `118b`, and move it into a step in `118g`, where the old flow goes and one decision covers all mid-level fights.
2. **New names?** `stageBossPlan` now also runs mid-level fights, and `LevelVictory` sends `INTERMEDIATE_BOSS_DEAD` for them. Recommended: `standardBossPlan` (a fight with a bar and a freeze, as opposed to the mini-boss plan in `118e`) and `SubmitBossDefeatEvent`.
