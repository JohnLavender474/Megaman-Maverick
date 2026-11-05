# Contract: mega-refactor-118h-bar-fill

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: draft
- Approved: no

## Intended changes

1. If slot 0 died in the boss intro and the boss then becomes ready, the death sequence runs while the bar fills. Today it waits until the bar is full. The respawn cancels the fight as before.

## Behavior that must not change

- The bar fill of each fight with a bar (stage, mid-level and Wily): the music pauses at the first bit, 1 bit per `DUR_PER_BIT` with the fill sound, the bar shows the count until it is full, `END_BOSS_SPAWN` and the music resume at the end, and the world release and the controller in the next tick.
- The bar position, look and real-health display after the fill.
- The bar goes at the defeat, at a respawn and at a screen reset, as today.
- No fill and no bar in a mini fight.
- In a pause, the fill stops and goes on after the pause.
- No step, plan, boss AI, `Gate` or map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `Fight.update(delta)` updates the bar with that `delta` after the steps of the tick, and does not update a bar that is finished.
- [ ] In a stage intro with a fill, `ReleaseWorld` and `ControllerOn` run in the tick after the tick in which the fill ends, not in the same tick.
- [ ] The existing `FightTest` and `FightStepsTest` cases pass with the `delta` parameter.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118h-bar-fill`

The intended change needs a death in the boss intro, which is hard to cause on purpose, so it has no play check. Control checks:

- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`. The bar fills bit by bit with its sound while the world is held and the music is paused; then the music, the world and the control come back as today. Pause during the fill once: the fill stops, and goes on after the pause.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9`: go through the gate and beat `DesertMan`, a mid-level fight. The same fill; after the defeat, the bar goes, the gate opens, and the level music plays.

## Behaviors to check

- The Wily fight uses the same fill step and code as the stage fight. No separate play check.
- A mini fight has no bar. No separate play check.
