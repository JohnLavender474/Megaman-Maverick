# Contract: mega-refactor-038b-level-reset

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-06

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `grep -rn "PLAYER_SPAWN\b" core/src/main` gives 0 lines (D1).
- [ ] `RespawnRoutineTest`: `LEVEL_RESET` is submitted once, in the frame of the level reset, at a menu start, a launch start and a respawn; not on game over and not on cancel.
- [ ] Mutation checks: move the submit to the next step, drop it, and send it twice. Each fails at least 1 test.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-038b-level-reset`. Experimental first, then control. (Revised 2026-11-06 after the review: wording of 1 to 3; the `INFERNO_MAN` `FireballBar` check went, because neither of its claims could fail; `TIMBER_WOMAN` is a control check, because the screen destroys the enemies before `LEVEL_RESET`.)

- `./run-game-alpha.sh --level REFACTOR_TEST_038 --room death_pit`: hands off. 2 deaths, each with the fade out, `READY` and the beam-in. The 3rd death fades out to the game over screen.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go right through the gate and die in the `MoonHead` fight. After the respawn in `room11`, the boss is gone. Walk right into the gate: it opens again, and the fight starts again. The exit gate stays locked.
- `./run-game-alpha.sh --level RODENT_MAN --room room8`: go left into `room9`. Push the `PushableBlock` off its start spot, then stay in `room9` and take hits from the `SniperJoe` until you die. (Do not change rooms first: a room change also removes the block.) After the respawn in `room8`, go left again: the block is at its start spot.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: control check. Kill the first `Bat`, then die in the pit to the right of the start. After the respawn the `Bat` is back.

## Behaviors to check

- Death and respawn: the same frames as `038a` for every event; `LEVEL_RESET` arrives in the run where `PLAYER_SPAWN` arrived.
- Level start from a menu and from `--level`: the entities spawned at the start are not culled one frame late or early.
- Game over: no `LEVEL_RESET`.

## Intended changes

None that a player can see. `PLAYER_SPAWN` goes (D1, 2026-11-06); `LEVEL_RESET` takes its job.
