# Scope: mega-refactor-118h-bar-fill

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Take the boss bar fill out of the level screen, and remove the fill type that has 1 used value. The fight that holds the bar also runs its fill. The screen only draws the bar. No `HudBars`, `BarSpec` or `BarSource`: they would have the boss bar as their only caller (owner, 2026-11-05).

## Item

`118`, "Boss fights as routines, rules and leases", branch 8 of the phased rollout (`ITEM.md`, "Proposed branches"): "`118h`: `HudBars`, `BarSpec` and `BarSource`. `FillBossHealthBar` replaces `BossHealthHandler`, and `HealthFillType` goes."

Owner decision (2026-11-05): a lean `118h`. Remove `HealthFillType` and its dead branch, and let the fight run the fill, so the bar leaves the screen's `when` chain. `HudBars`, `BarSpec` and `BarSource` wait until a second bar has a caller; the item records them under "Possible later".

## Current state

Scan of `master` at `e33f2e491`.

### The bar

- `BossHealthHandler` (`screens/levels/stats/BossHealthHandler.kt`) is a `BitsBar` view and a fill timer. `set(entity, type, runOnFirstUpdate, runOnFinished)` builds the bar; `unset()` clears it; `finished` is "no timer, or the timer ended"; `update(delta)` runs the timer.
- With `BIT_BY_BIT`, the timer counts 1 bit per `DUR_PER_BIT`, plays `ENERGY_FILL_SOUND` per bit, calls `runOnFirstUpdate` (the music pause) on its first update and `runOnFinished` (`END_BOSS_SPAWN` and the music resume) when it ends. Until the timer ends, the bar shows the count, not the real health.
- `ALL_AT_ONCE` calls both callbacks at once and sets no timer. Nothing uses it: `FillBossHealthBar` (`FightSteps.kt:47-57`) passes `BIT_BY_BIT`, and the only other path was the `BOSS_READY` branch, which `118g` removed.
- `HealthFillType` (`utils/misc/HealthFillType.kt`) has only these 2 callers: `BossHealthHandler` and `FillBossHealthBar`.

### Who updates and draws it

- The screen owns the handler (`MegaLevelScreen.kt:180,270`) and passes it to `fights.start` (`:700`). `Fight` holds it as `bar`; `FillBossHealthBar` sets it under the `HEALTH_BAR` lease, and its `isFinished` reads `bar.finished`.
- The screen updates the timer in its "one handler at a time" chain, after `fights.update()` and before `engine.update` (`:807-817`):
  ```kotlin
  fights.update()
  when {
      !bossHealthHandler.finished -> bossHealthHandler.update(delta)
      !endLevelEventHandler.finished -> endLevelEventHandler.update(delta)
      !playerSpawnEventHandler.isFinished() -> playerSpawnEventHandler.update(delta)
      !playerDeathEventHandler.finished -> playerDeathEventHandler.update(delta)
  }
  ```
  So while the bar fills, the other 3 handlers do not update.
- The screen draws the bar (`:892`) and unsets it at `PLAYER_SPAWN_INIT` (`:524`), next to `fights.cancel()`.
- `Fight.update()` and `FightDirector.update()` take no `delta`.

### The order in a frame

Today, in frame F: the fight tick checks `FillBossHealthBar.isFinished` (the timer has not ended), then the screen updates the timer. If the timer ends in that update, `runOnFinished` sends `END_BOSS_SPAWN`, and the tick of frame F+1 sees `finished` and runs `ReleaseWorld` and `ControllerOn`. If the fight ran the timer before its steps, the steps would see the end 1 frame earlier. So the fight runs the timer after its steps, in the same order as today.

### Which handler can be active during a fill

During a fill the world is held. `endLevelEventHandler` starts only at `VICTORY_EVENT`, after the outro. `playerSpawnEventHandler` runs after a respawn, and `PLAYER_SPAWN_INIT` cancels the fight and unsets the bar first. So only `playerDeathEventHandler` can be active during a fill: when slot 0 died in the intro and the boss then became ready (the intro does not check slot 0).

## In scope

1. `Fight.update(delta)` and `FightDirector.update(delta)`. After its phases, a fight updates its bar while the bar is not finished. The screen passes `delta`.
2. Remove the `bossHealthHandler` branch from the screen's `when` chain. The screen keeps the handler as the bar view (it builds it, passes it to the fight, draws it, and unsets it at `PLAYER_SPAWN_INIT`).
3. Remove `HealthFillType`, its parameter in `BossHealthHandler.set`, and the `ALL_AT_ONCE` branch. The `BIT_BY_BIT` fill stays as it is.
4. The `fights` README: the fight runs the bar fill after its steps; the screen only draws the bar. Remove the "later branches move the health bar" line.
5. Tests: a fight updates its bar after its steps, and only while the bar is not finished; the intro step order in a fill does not change (`ReleaseWorld` runs in the tick after the fill ends).
6. On `master` at the merge: the item `ITEM.md` "Possible later" table gets `HudBars`, `BarSpec` and `BarSource`, with no named caller.

### New APIs

None. `Fight.update` and `FightDirector.update` get a `delta` parameter; their callers are the screen and `FightDirector`.

### Intended changes

- If slot 0 died in the boss intro and the boss then becomes ready, the death sequence now runs while the bar fills. Today it waits until the bar is full. The respawn cancels the fight as before.

## Out of scope

- `HudBars`, `BarSpec`, `BarSource`, a second bar, and a bar position other than today's (owner, 2026-11-05).
- The bar showing the count, not the real health, during the fill (item `NOTES.md`, the `refill` note). It is the same on both sides.
- The other handlers in the `when` chain (`013`).
- `BossComponent` and the boss hooks (`118i`). The key-based gate unlock (`118j`).

## Affected areas and risks

Files: `Fight.kt`, `FightDirector.kt`, `FightSteps.kt`, `MegaLevelScreen.kt`, `BossHealthHandler.kt`, `HealthFillType.kt` (removed), `fights/README.md`, and the fight tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The fill ends 1 frame early or late | The controller and the world come back at a different frame | The fight updates the bar after its steps, as the screen did after `fights.update()`; a test checks that `ReleaseWorld` runs in the tick after the fill ends |
| The bar updates in a pause | The fill runs on in a pause | `fights.update` is inside the same `if (!game.clock.paused)` block as the chain |
| The bar stops at a cancel | A bar timer that runs on | `unset()` clears the timer at the lease release and at `PLAYER_SPAWN_INIT`, as today, so `finished` is `true` |
| A handler runs during a fill | A visible change | Only the death sequence can, in the case under "Intended changes" |

## Overlap with other branches

Active worktrees at `e33f2e491`: `compactor-level` and `x6-intro-extra-level`. Neither changes `fights/`, `MegaLevelScreen`, `BossHealthHandler` or `HealthFillType` (the x6 branch changes only the `D1000` lines in `FightPlans.kt`). Recheck at branch start.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` | helps | 1 handler leaves the screen's "order must not change" chain; the fight that owns the bar runs it |
| `O1-R5` | helps | The dead `ALL_AT_ONCE` branch and the 1-value enum go; no API with no caller is added |
| `O1-R4` | no effect | No key |
| `O2-R3` | waiting | The tick runs in the screen until `010`; the bar fill now runs in the same tick |
| `O2-R5` | no effect | No slot test changes |
| `O2-R10` | waiting | No new system write. `012` owns the fix |
| `O4-R2` | no effect | No new mechanic |
| `O5-R1` | no effect | No per-frame allocation |
| `O6-R2` | no effect | No new check |
| `O7-R3` | no effect | No level-identity check |

## Open questions

None. The owner chose (2026-11-05) the lean `118h`.
