# Contract: mega-refactor-118i-boss-phase

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-05

## Intended changes

1. Mini plan: the boss enters `FIGHT` in the tick after it became ready, 1 frame before the old `END_BOSS_SPAWN` delivery had effect. No live mini boss reads the phase, so a player sees no change.

## Behavior that must not change

- Each boss runs its entrance (for example the fall of `BigAssMaverickRobot` and the jump of `PreciousWoman`) as today, until it is ready.
- From ready until the bar is full, the boss AI is held, as today. In a stage, mid-level or Wily fight, the first AI update after the fill is in the same frame as today.
- `PreciousWoman` shows her intro sprite (`wink`, or `jump` in the air) until the fight starts.
- The hands of `BigAssMaverickRobot` start to turn, and it starts to shoot orbs, only when the fight starts.
- A boss can hurt a player from the moment it is ready, as today, and not before.
- The boss music, `END_BOSS_SPAWN`, the `WilyFinalBoss` phase handler, the bar fill, the defeat and every exit stay as today.
- A boss that respawns after a player death runs its full entrance and intro again.
- No map file, `Gate` or plan exit changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] A new `BossComponent` has the phase `ENTRANCE`, and `reset()` sets it back to `ENTRANCE`.
- [ ] A boss that has not spawned yet has its component, with the phase `ENTRANCE`.
- [ ] `AwaitBossReady` ends when the phase is not `ENTRANCE`.
- [ ] `StartBossFight` sets `FIGHT`.
- [ ] In a stage intro with a fill, the phase becomes `FIGHT` in the tick after the tick in which the fill ends, not in the same tick.
- [ ] In the mini intro, `StartBossFight` runs after `EndBossSpawn`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118i-boss-phase`

The intended change has no visible effect, so it has no play check. Control checks:

- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the gate and beat `BigAssMaverickRobot`. It falls, the room shakes, and the hands go to their places. The hands start to turn and the orbs start only after the bar is full.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: go through the gate and beat `PreciousWoman`. She jumps in and winks until the bar is full, then fights as today.
- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`. His entrance plays as today, he waits while the bar fills, then fights as today. Die once in the fight: after the respawn, go through the gate again, and a full new intro plays.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss. No bar, no freeze, and it attacks as today.

## Behaviors to check

- The other 6 bosses with the "window only" read (`TimberWoman`, `MoonMan`, `ReactorMan`, `DesertMan`, `InfernoMan`, `RodentMan`) use the same mapping as `GlacierMan`. No separate play check.
- `SigmaRat` has no live map. Its claw sprite reads "phase is `ENTRANCE`". No play check.
