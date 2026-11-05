# Scope: mega-refactor-118i-boss-phase

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Put the boss intro state in a `BossComponent` with 1 field, `phase`. The boss still says when it is ready. The fight, not the `END_BOSS_SPAWN` event, says when the boss AI starts. The 2 flags `ready` and `betweenReadyAndEndBossSpawnEvent` go.

## Item

`118`, "Boss fights as routines, rules and leases", branch 9 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118i`: `BossComponent` replaces `ready`, `betweenReadyAndEndBossSpawnEvent` and `onReady`. Shared with `048` step 6. It changes AI gating by up to 1 or 2 frames in 10 files, so its contract keeps the same window or names the change."

Owner decisions (2026-11-05):

- Phase only. `isReady`, `preReady` and `onReady` stay as hooks on `AbstractBoss` until `048` step 6 removes the base class. The boss music stays in `onEndBossSpawnEvent`, so `END_BOSS_SPAWN` stays.
- `x6-intro-extra-level` adapts its 2 `D1000` lines when it next merges `master`.

`048` decision (2026-11-02): `BossComponent` holds only fight data, it is added at `init()`, and no code uses `hasComponent(BossComponent)` as a role test. This branch adds only the field that has callers (`O1-R5`).

## Current state

Scan of `master` at `6d04b08c5`.

### The 2 flags

- `AbstractBoss.ready` (`AbstractBoss.kt:99`) is public and writable. The boss updatable (`:190-197`) runs `preReady(delta)` while `!ready`; when `isReady(delta)` is `true`, it sets `ready = true` and calls `onReady()`. `onSpawn` (`:133`) and `onDestroy` (`:151`) set it to `false`. `BossSpawnEventHandler.kt:18` also sets it to `false` before `spawn`.
- `betweenReadyAndEndBossSpawnEvent` (`:108`) is `true` from the base `onReady()` (`:238`) until `onEndBossSpawnEvent()` (`:244`), which runs when the boss gets `END_BOSS_SPAWN`. `onSpawn` sets it to `false`.
- So the boss has 3 states: not ready (`!ready`), the intro window (`ready && between`), and the fight (`ready && !between`).

### Who sends `END_BOSS_SPAWN`

- Stage plan (`standardBossPlan`, `midBossPlan`, `finalBossPlan`): the fill end in `FillBossHealthBar` sends it, in the tick of frame G (after the steps, `118h`). `events.run` of frame G delivers it. `ReleaseWorld` and `ControllerOn` run in the tick of frame G+1.
- Mini plan: the `EndBossSpawn` step sends it in the tick of frame F+1, the tick after the boss became ready. `events.run` of frame F+1 delivers it.
- The boss listens in `AbstractBoss.onEvent` (`:169`). `onEndBossSpawnEvent` clears the window and plays the boss music (`playBossMusic()`, `ConstKeys.MUSIC`). `WilyFinalBoss.kt:321-325` overrides it to also turn off its phase transition handler.

### Readers

| Read | Files | Lines |
|---|---|---|
| `if (betweenReadyAndEndBossSpawnEvent) return` at the top of the AI updatable | `TimberWoman`, `MoonMan`, `ReactorMan`, `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan`, `PreciousWoman` | 8 |
| `ready && !betweenReadyAndEndBossSpawnEvent` (the fight) | `BigAssMaverickRobot.kt:472`, `BigAssMaverickRobotHand.kt:311` | 2 |
| `!ready \|\| betweenReadyAndEndBossSpawnEvent` (the sprite key) | `PreciousWoman.kt:677` | 1 |
| `!ready` (sprite hidden) | `SigmaRatClaw.kt:336` | 1 |
| `ready` | `AbstractBoss.canDamage` (`:174`), `AwaitBossReady` (`FightSteps.kt:23`), `FightStepsTest.kt:51` (mock) | 3 |
| write `ready = false` | `BossSpawnEventHandler.kt:18` | 1 |

The 8 bosses in the first row run their AI also before they are ready, because the check is only the window. So their check maps to "phase is `INTRO`", not to "phase is not `FIGHT`".

No live mini boss (`MoonHead`, `MechaDragon`, `ReactorMonkey`) and no `ElecDevil`, `GutsTank` or `WilyFinalBoss` code reads either flag.

### The frames today

- Frame F, `engine.update`: the boss updatable sets `ready` and calls `onReady()`, so the window opens. The AI updatable of the same boss runs after it in the same update and sees the window.
- Stage plan: the tick of frame F+1 holds the world, so no boss updatable runs during the fill. The window closes in `events.run` of frame G. The first AI update outside the window is in `engine.update` of frame G+1.
- Mini plan: the window closes in `events.run` of frame F+1. The first AI update outside the window is in `engine.update` of frame F+2.

### When the component must exist

`FightDirector.start` runs before the boss spawns: `spawn` only queues, and the engine calls `init()` on the first spawn, inside `engine.update`. `AwaitBossReady` reads the boss in the tick before that update. So the component must exist from the boss constructor, not only from `init()`. The engine calls `reset()` on each component at destroy (`GameEngine.kt:191`), before `onDestroy`.

## In scope

1. `BossComponent` (`entities/state/`), with `phase: BossPhase` and `reset()` back to `ENTRANCE`. `BossPhase` has 3 values: `ENTRANCE` (not ready), `INTRO` (ready, the fight not started), `FIGHT`. The boss makes it as a field and adds it in `init()`.
2. `AbstractBoss`: when `isReady` is `true`, the boss sets `INTRO` and calls `onReady()`, as today. `ready` and `betweenReadyAndEndBossSpawnEvent` go. `onSpawn` sets `ENTRANCE`. `onEndBossSpawnEvent` keeps the music and no longer writes a flag. `canDamage` reads "phase is not `ENTRANCE`".
3. A new fight step, `StartBossFight`, sets `FIGHT`.
   - In the stage plan, it comes right after `FillBossHealthBar`. It runs in the tick of frame G+1, which has the same effect as `events.run` of frame G (the frame rule).
   - In the mini plan, it comes right after `EndBossSpawn`.
4. The readers in the table read `phase`. `BossSpawnEventHandler` no longer writes the flag; the component reset at destroy covers it.
5. The `fights` README: the fight starts the boss AI with a step, and a boss reads its phase. The status line names the boss hooks that stay until `048`.
6. Tests: the phase values and the reset; the boss sets `INTRO` when ready; `StartBossFight` sets `FIGHT`; in the stage plan, `FIGHT` comes in the tick after the fill ends and not before.

### New APIs

| API | First caller |
|---|---|
| `BossComponent`, `BossPhase` | `AbstractBoss` (writes `ENTRANCE` and `INTRO`), `StartBossFight` (writes `FIGHT`), the 12 reader files |
| `StartBossFight` | `stageBossPlan` and `miniBossPlan` in `FightPlans` |

### Intended changes

- Mini plan: the boss enters `FIGHT` in the tick of frame F+1, not after `events.run` of frame F+1. So the first AI update outside the intro window is 1 frame earlier. No live mini boss reads the window today, so nothing that a player can see changes.

## Out of scope

- `isReady`, `preReady`, `onReady` and `onEndBossSpawnEvent` as hooks; the base class (`048` step 6).
- The boss music, `playBossMusic`, `END_BOSS_SPAWN` and the `WilyFinalBoss` handler (owner, 2026-11-05).
- `defeated`, `mini`, `bossKey` and `spawnDefeatOrbs`. `bossKey` is `118j`.
- `D1000` in `x6-intro-extra-level` (owner, 2026-11-05).
- The commented-out bosses (`Bospider`, `SphinxMiniBoss`, `ReactorMan_OLD`, `MechaDragon_OLD`).

## Affected areas and risks

Files: `AbstractBoss.kt`, `BossComponent.kt` (new), `FightSteps.kt`, `FightPlans.kt`, `BossSpawnEventHandler.kt`, the 12 reader files, `fights/README.md`, and the fight tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The AI starts 1 frame early or late in a stage fight | A boss moves or attacks at a different frame | `StartBossFight` runs in the tick after the fill ends; a test checks the tick. The control play checks |
| A reader maps to the wrong phase | A boss that runs its entrance (for example a fall or a jump) freezes before it is ready | The 8 "window only" readers map to `INTRO`, the others to `FIGHT` or `ENTRANCE`, as in the table |
| The component does not exist when the fight reads it | A crash at the boss room entry | The boss makes it as a field; a test reads the phase of a boss that has not spawned |
| A pooled boss keeps `FIGHT` | `AwaitBossReady` ends at once on the next spawn | `reset()` at destroy, and `onSpawn` sets `ENTRANCE` |
| The `x6-intro-extra-level` merge of `master` does not compile | That branch is blocked | The merge docs name the 2 `D1000` lines and the new reads |

## Overlap with other branches

Active worktrees at `6d04b08c5`:

- `compactor-level` and `x-sabre-sprites` do not change boss or fight code.
- `x6-intro-extra-level` adds `D1000`, which reads `ready` and `betweenReadyAndEndBossSpawnEvent` (`D1000.kt:556`, `:583`), and changes the `D1000` lines in `FightPlans.kt`. The owner chose that the x6 branch adapts.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | The boss AI starts from a fight step, not from the order of an event delivery |
| `O1-R4` | no effect | No key |
| `O1-R5` | helps | 1 field with callers; no hook moves only to rename it |
| `O1-R2` | no effect | No new abstract layer. The component is the first step out of `AbstractBoss` |
| `O2-R3` | waiting | The tick runs in the screen until `010` |
| `O2-R5` | no effect | No slot test changes |
| `O2-R10` | no effect | No system write |
| `O4-R2` | helps | A new kind of fight can start the boss AI at any point of its plan |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | no effect | No new check |
| `O7-R3` | no effect | No level-identity check |

## Open questions

None. The owner chose (2026-11-05) phase only, and the x6 branch adapts.
