# Scope: mega-refactor-118b-batch-1

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Move the first batch of bosses to the `118` fight flow: `TimberWoman`, `MoonMan` and `ReactorMan`. Each has 2 live fights: an end-of-level fight in its own stage, and a mid-level fight in a Wily stage. Add a plan for mid-level fights, and move the resume after a mid-level fight out of the screen and into that plan.

## Item

`118`, "Boss fights as routines, rules and leases", branch 2 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118b`: `TimberWoman`, `MoonMan`, `ReactorMan` (6 fights). Adds `midBoss`."

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
2. The screen's `INTERMEDIATE_BOSS_DEAD` branch (`MegaLevelScreen.kt:814-820`) submits `TURN_CONTROLLER_ON`, removes the `BOSS_DEFEATED` damage lock from each player, and plays the level music again (`music`, a screen field that a room can change, `:189`, `:563`).
3. `Gate` reads the same event and opens the gate whose `boss_key` matches (`Gate.kt:196-209`).

The intro, the bar, the freeze, the defeat lock, the listener calls and the bar removal are the same as in an end-of-level fight.

### What `118a` gives

- `stageBossPlan` ends with `LevelVictory`, which sends `boss.getEventOnDefeated()`. So the name is wrong for a mid-level fight: the level goes on.
- The fight closes in the same tick as its last step, before the `events.run()` that delivers the exit event. So a `fights.owns(boss)` guard on a screen branch for that event would not see the fight (`NOTES.md`, research report).
- `FightPlan.music` is not nullable, and `FightPlans.resolve` always writes it. These 3 bosses have no music of their own.

## In scope

1. **2 plans.** `FightPlans.find` takes the map props and chooses the plan from `end`:
   - `standardBossPlan` (the `118a` `stageBossPlan`, renamed) for an end-of-level fight. Its last step, `LevelVictory`, always sends `VICTORY_EVENT`.
   - `midBossPlan` for a fight with `end=false`. Its outro ends with:
     - `ExitMidBossFight`: sends `INTERMEDIATE_BOSS_DEAD` with the boss, while slot 0 is alive. `Gate` opens on it as today.
     - `ResumeAfterMidBoss`: submits `TURN_CONTROLLER_ON`, removes the `BOSS_DEFEATED` damage lock from each player, and plays the level music.
     - `AwaitBossGone`: ends when `boss.existenceState.liveNow` is false, 1 tick after the boss is destroyed. It keeps the fight open through the `events.run()` that delivers the exit event.
   - The other steps are shared: the intro, `BossDefeatLock`, `NotifyListenersOfBossDefeat`, `HideBossHealthBar` and `AwaitBossDefeatEnd`.
2. Add `TimberWoman`, `MoonMan` and `ReactorMan` to `FightPlans.find`.
3. Make `FightPlan.music` nullable. `resolve` writes it only when it is set. The 3 bosses have none, so the map value or the boss default is used, as today.
4. A guard in the screen's `INTERMEDIATE_BOSS_DEAD` branch: it skips a boss that a fight owns.
5. `FightDirector.start` takes a supplier of the screen's level music, as it takes the bar.
6. Update `core/.../fights/README.md` if its generic text no longer fits (for example the example plan's last step).
7. Tests:
   - `FightPlans.find`: a plan for the 4 moved bosses and `null` for the others; `midBossPlan` for `end=false` and `standardBossPlan` otherwise.
   - `midBossPlan`: sends `INTERMEDIATE_BOSS_DEAD` and no `VICTORY_EVENT`, resumes once, and stays open until the boss is gone. With slot 0 dead, it sends nothing and does not resume.
   - `standardBossPlan`: sends `VICTORY_EVENT` and does not resume.
   - The screen's `INTERMEDIATE_BOSS_DEAD` branch skips a boss that a fight owns, and acts for one with no fight.

### Intended change

After a mid-level fight on the new flow, the damage lock goes and the level music starts 1 frame earlier (before `engine.update`, not after it), and the first free input frame comes 1 frame earlier. The research found no adverse effect in the 8 mid-level rooms, with 1 or 2 players: no damager can reach a player in that frame, the gate is already `triggerable` before the first free input frame, and no music fade is in progress. See `NOTES.md`.

### New APIs

| API | First production caller |
|---|---|
| `midBossPlan`, `ExitMidBossFight`, `ResumeAfterMidBoss`, `AwaitBossGone` | `FightPlans.find`, for the 3 mid-level fights in this batch |
| A map props parameter on `FightPlans.find` | `MegaLevelScreen` at `ENTER_BOSS_ROOM` |
| The level music supplier on `FightDirector.start`, and `Fight.levelMusic` | `MegaLevelScreen` at `ENTER_BOSS_ROOM`; `ResumeAfterMidBoss` |

`FightPlan.music` changes from `MusicAsset` to `MusicAsset?`. Its caller is `FightPlans.find`. `stageBossPlan` is renamed `standardBossPlan`.

## Out of scope

- The `GlacierMan` map music and the forced music of other bosses. They come with their batches.
- The mini-boss rules: no bar, no freeze, and no level music at the resume. `118e` brings the first mini boss and its plan.
- `AbstractBoss.getEventOnDefeated()`: it stays for the old flow until `118g`. For a moved boss, the plan chooses the exit event, and both read the same `end` map value.
- All boss AI, `AbstractBoss`, `Gate` and the map files.
- A temporary contract suite. The `118a` suite proved the shared steps, and the resume changes its timing on purpose. Permanent tests cover it.

## Affected areas and risks

Files: `FightPlans.kt`, `FightPlan.kt`, `FightSteps.kt`, `Fight.kt`, `FightDirector.kt`, `MegaLevelScreen.kt` (the route, 1 guard and 1 supplier), `README.md` in `fights/`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The wrong plan | A mid-level fight ends the level, or an end-of-level fight resumes | `find` reads `end` with the same default as `AbstractBoss` (`true`), and a test covers both values |
| The mid-level end | The gate stays shut, or the controller and the music do not come back | Tests of the exit event and the resume, and play checks in each mid-level room |
| A double resume | The screen branch and the step both act, so the music restarts | `AwaitBossGone` keeps the fight open, and a test checks that the guard skips |
| Music | A wrong track at the fight start | `resolve` writes nothing for these 3, so `AbstractBoss` reads the same prop as today |
| `MoonMan` gravity | A gravity flip stays after the fight | Not part of the flow. A play check covers it |
| The `WILY_STAGE_3` `TimberWoman` check | It needs 2 earlier fights | The `MoonMan` fight on the way is on the new flow, and the `PreciousWoman` fight is a control |

## Overlap with other branches

- `compactor-level` and `fix/pushable-block-ledge-sliver` did not touch `fights/`, `MegaLevelScreen`, `AbstractBoss` or these 3 bosses at the `118a` merge. Recheck at branch start.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 6 more fights run on one routine, including the resume after a mid-level fight |
| `O1-R5` | helps | Each new API has a caller in this branch |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change |
| `O2-R9` | no effect | `TURN_CONTROLLER_*` stays with no slot |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic. The real rooms are the checks |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Named checks for each of the 6 fights |
| `O7-R3` | no effect | The route reads the boss name and the map's `end` value, not the level |

## Open questions

None. The owner chose (2026-11-02) the 2-plan design, the resume as a step, and the names `standardBossPlan` and `midBossPlan`.
