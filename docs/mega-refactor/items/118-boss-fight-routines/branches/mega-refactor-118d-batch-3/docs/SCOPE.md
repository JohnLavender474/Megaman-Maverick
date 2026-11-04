# Scope: mega-refactor-118d-batch-3

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-04

## Goal

Move the third batch of bosses to the `118` fight flow: `ElecDevil`, `GutsTank` and `SigmaRat`. `ElecDevil` and `GutsTank` each have 1 live fight, at the end of a Wily stage. `SigmaRat` has no live fight. The `standardBossPlan` from `118b` fits all 3 with no change.

## Item

`118`, "Boss fights as routines, rules and leases", branch 4 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118d`: `ElecDevil`, `GutsTank`, `SigmaRat` (2 fights)." The `118a` `SCOPE.md`, "Later branches", gives the reason: "Multi-entity bosses and custom ready checks".

## Current state

Scan of `master` at `5fd3ed157`. An agent did the TMX and boss reads; I checked the music lines, the `SigmaRat` maps and the `ITEM.md` gate line.

### The fights

| Boss | Level | Map props for the flow | Room before the boss room, with its spawn |
|---|---|---|---|
| `ElecDevil` | `WILY_STAGE_1` | none (`end` defaults to `true`) | `room10`, spawn `9` |
| `GutsTank` | `WILY_STAGE_2` | none (`end` defaults to `true`) | `room9`, spawn `5` |
| `SigmaRat` | none | `Test7.tmx` and `WilyStage2.tmx` only; no `LevelDefinition` uses them | none |

No map object sets `end`, `mini`, `health_fill_type`, `orb` or `music` for these fights. Both live boss rooms have `fade_out_music=true` and an entry gate with no `boss_key` (`432` in Wily 1, `492` in Wily 2).

### "Multi-entity" and "custom ready"

- Each boss is 1 `AbstractBoss`, and that entity has the health, the bar and the defeat. The other entities are children: `ElecDevilBody` and `ElecDevilBodyPieces`; the `GutsTank` tank `Block`, `GutsTankFist` (its own health, which does not end the fight), Mets and HeliMets; the 2 `SigmaRatClaw`s and the electric balls. So `Fight.boss` is the right entity, and `Fight.cast` still has no caller.
- The custom ready checks are inside `isReady`:
  - `ElecDevil`: a timer that runs only while all players are on the ground (`ElecDevil.kt:239`, `:339`).
  - `GutsTank`: all players on the ground and a timer (`GutsTank.kt:279-280`).
  - `SigmaRat`: `true // TODO` (`SigmaRat.kt:199`).
  `AwaitBossReady` reads `boss.ready`, which `AbstractBoss` sets from `isReady` (`AbstractBoss.kt:195-200`). So no step changes.

### Hooks and side effects

- `ElecDevil` forces `MUSIC = JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC` in `onSpawn` (`ElecDevil.kt:139`), before `super.onSpawn`.
- `GutsTank` forces `MUSIC = MMX6_SIGMA_2_BATTLE_INTRO_MUSIC` in `onSpawn` (`GutsTank.kt:204`). It is an intro track that plays its loop track when it ends, as with `BigAssMaverickRobot`.
- `SigmaRat` writes no spawn props.
- None of the 3 overrides `getEventOnDefeated`, `playBossMusic`, `getHealthFillType` or `preReady`. None sends or listens to fight events, freezes systems, or reads `betweenReadyAndEndBossSpawnEvent` or `mini`. `ElecDevil.onReady` calls `super.onReady()` first.
- `GutsTank` and `SigmaRat` destroy their children in `triggerDefeat`, inside the boss, on both flows.

### Defeat listeners

- `ElecDevilBodyPieces` (an `IHazard`), the launched pieces, the `GutsTank` projectiles (including `BigAssMaverickRobotOrb`, which overrides `onBossDefeated`) and the `SigmaRat` electric balls are `IBossListener`s. `NotifyListenersOfBossDefeat` calls them in the same tick position as the old branch.
- The 2 live boss rooms and the rooms before them have no `Spike`, `PreciousGem` or other `BOSS_DEFEATED` listener.

### Two fights in a row

The mid-level fight before each boss room (`DesertMan` in Wily 1, `GlacierMan` in Wily 2) is on the new flow since `118c`. Its fight closes 1 tick after its exit event, and the player then walks through the next room and its gate. So the rule "a fight starts only when no fight is open" holds (`118c` `NOTES.md`).

## In scope

1. Add `ElecDevil` and `GutsTank` to `FightPlans.find`, each with its music as the plan music. Remove the forced music line from each boss's `onSpawn` (`fights` README, "Move a boss").
2. Add `SigmaRat` to `FightPlans.find` with no plan music (owner, 2026-11-04). Tests only, because it has no live fight.
3. Tests: `find` gives a plan for the 12 moved bosses and `null` for the others. `ElecDevil` and `GutsTank` get their music in the plan, and `resolve` writes it to the spawn props. `SigmaRat` has no plan music. The plan comes from `end`.

### New APIs

None.

### Intended changes

None that a player can see.

## Out of scope

- The key-based gate unlock. It stays in `118j` (owner, 2026-11-04). The `ITEM.md` decision line that says `118d` is corrected on `master`.
- The `SigmaRat` stub `isReady` and the music value `CHRONO_TRIGGER_WORLD_REVOLUTION` in `Test7.tmx`, which is not in `MusicAsset`. A branch that puts `SigmaRat` back in a live map checks them.
- The "all players on the ground" ready checks, while the flow tests only slot 0. This is the same on both flows. `047e` owns it, and the bosses have `TODO`s for item `047`.
- A double destroy of `ElecDevilBodyPieces` and the `SigmaRat` balls after the defeat (listener step, then `onDestroy`). It is the same on both flows.
- All boss AI, `AbstractBoss`, `Gate` and the map files.

## Affected areas and risks

Files: `FightPlans.kt`, `ElecDevil.kt`, `GutsTank.kt`, and tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The wrong music | A wrong or a silent boss track | The plan gives the same track as the removed line. A test checks the `resolve` value for each boss. Play checks |
| The `GutsTank` intro track | The loop track does not follow the intro | The same `MusicAsset` value as today; `BigAssMaverickRobot` uses the same pattern on the new flow. Play check |
| A boss missed in `find` | It stays on the old flow, with no visible change | A test lists the 12 moved bosses |
| The custom ready checks | The bar fills too early or never | `AwaitBossReady` reads `boss.ready`, as the old `BOSS_READY` branch did. Play checks |
| `SigmaRat` with no live fight | Not seen in play | Tests only. No player can reach it |

## Overlap with other branches

Active worktrees at `5fd3ed157`: `compactor-level` and `x6-intro-extra-level`. Recheck at branch start for `fights/`, `MegaLevelScreen`, `AbstractBoss` and the 3 bosses.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 2 more live fights run on one routine. After this branch, 19 of 25 live fights are on the new flow |
| `O1-R5` | no effect | No new API. The `SigmaRat` entry is a value in an existing `when`, chosen by the owner |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | The slot 0 test does not change. The "all players landed" ready checks stay for `047e` |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | helps | Named checks for each live fight |
| `O1-R4` | no effect | The plan music goes through the existing `MUSIC` spawn prop. No new key |
| `O7-R3` | no effect | The route reads the boss name and the map's `end` value, not the level |

## Open questions

None. The owner chose (2026-11-04) to move `SigmaRat`, and to keep the gate unlock in `118j`.
