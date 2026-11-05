# Contract: mega-refactor-118g-remove-old-flow

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-05

## Intended changes

None that a player can see. A boss room whose boss has no plan in `FightPlans.find` now throws `MissingFightPlanException` at room entry. No map that a `LevelDefinition` names has such a room.

## Behavior that must not change

- All 25 live fights play as after `118f`: the stage fights (bar, freeze, defeat lock, level end), the mid-level fights (the resume, the gate, the level music), the mini fights (no bar, no freeze, the gate), and the Wily fight (the phases and the end scene).
- In a defeated level, a stage boss room still sends `VICTORY_EVENT` and spawns no boss. A mid-level, a mini and the Wily room still spawn their boss.
- A death in a fight, a respawn, and a game over behave as today: `PLAYER_SPAWN_INIT` and `reset()` cancel the fight.
- `Spike` and `PreciousGem` still go on `BOSS_DEFEATED`. `Gate` still opens on `INTERMEDIATE_BOSS_DEAD`. `AbstractBoss` still clears `betweenReadyAndEndBossSpawnEvent` and plays its music on `END_BOSS_SPAWN`.
- No step, plan, boss AI, `Gate` or map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find` throws `MissingFightPlanException` for each boss with no plan (`MechaDragon_OLD`, `ReactorMan_OLD`, `SphinxMiniBoss`, and a name that is no boss), and the message names the boss.
- [ ] `MegaLevelScreen.eventKeyMask` has none of `END_BOSS_SPAWN`, `BOSS_DEFEATED` and `INTERMEDIATE_BOSS_DEAD`, and still has `ENTER_BOSS_ROOM` and `VICTORY_EVENT`. (`BOSS_READY` and `BOSS_DEAD` no longer exist, so the build checks them.)
- [ ] The tests that stay pass: the `reset()` test, the `GAME_COMPLETE` tests, `FightTest` and `FightStepsTest`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118g-remove-old-flow`

There are no experimental checks, because nothing changes on purpose. Control checks, one for each kind of plan that the old branches could have touched:

- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`. Intro, bar, freeze, fight, defeat and level end as today, with the same boss music.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9`: go through the gate and beat `DesertMan`, a mid-level fight. Bar, freeze and defeat lock; then the gate opens, Megaman can move, and the level music plays.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss. No bar, no freeze, the level music goes on, and the next gate opens.

## Behaviors to check

- The Wily fight: the only old branch that it reached was `BOSS_DEAD`, which did nothing for it, and the guard skipped it. So it has no separate play check.
- Death and respawn: the same code as before, through `PLAYER_SPAWN_INIT`. No separate play check.
- The defeated-level check: `endsLevel` has tests from `118f`, and the screen line reads it with no fallback.
