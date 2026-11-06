# Contract: mega-refactor-038a-respawn-routine

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-06

## Intended changes

- None that a player can see or hear. The room-music block goes; in `SMB3_1` the respawn room's music plays after the respawn, as today.
- `PLAYER_SPAWN_INIT` goes. No entity listens to it.
- `TURN_CONTROLLER_ON` is sent once at ready, not twice.

## Behavior that must not change

- The death: the defeat sound, 4 s, a 0.25 s fade out. The music and loops stop at the death. One life goes.
- Game over when the lives are gone, with no respawn.
- The respawn: a 0.25 s fade in, `READY` blinks, the beam falls, the beam lands, the beam sound plays, and control comes back.
- The other players are destroyed when slot 0 dies, and spawn again with slot 0. A slot 1 death changes nothing until that reset.
- `PLAYER_JUST_DIED`, `PLAYER_DONE_DYIN`, `PLAYER_SPAWN` and `PLAYER_READY` go out with the same slot, in the same frame relative to each other, as today.
- At the respawn: the camera, backgrounds, fight, boss bar, systems and shake reset; the enemies go and the spawners rearm, in the same frame as today.
- The level start has the same fade in and beam-in as today.
- A death while paused resumes the game.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.
- [ ] Death during the beam-in waits for `READY`; a level start from a menu resets in the first events run.
- [ ] `grep` finds no `PLAYER_SPAWN_INIT`, `PlayerSpawnEventHandler` or `PlayerDeathEventHandler` in live code.

Permanent tests:

- [ ] The routine runs its steps in the order of the brief, and submits each event in the frame of today.
- [ ] With no lives left, the routine ends at game over, and no respawn step begins.
- [ ] `cancel` (screen reset) ends the routine with no event.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-038a-respawn-routine`

The branch has no intended change that a player can see, so all checks are control checks. The first one runs the whole flow with no input.

- `./run-game-alpha.sh --level REFACTOR_TEST_038 --room death_pit`: do not touch the controls. Megaman beams in, falls into the pit and dies 3 times. Each death: the defeat sound, a pause, the fade out, then the fade in, `READY` and the beam-in. After the third death the game over screen opens.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: the level start looks as today. Kill an enemy, then die. After the respawn the enemy is back.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and die in the `MoonHead` fight. The boss bar goes, the exit gate of the boss room is locked again, and the fight starts again when you enter.
- `./run-game-alpha.sh --level DESERT_MAN --room room6`: in the dark room, die. During the beam-in the dark area is lit at the beam, as today, and after `READY` the darkness acts as today.
- `./run-game-alpha.sh --level SMB3_1 --room room2`: go left into `room1`, take the pipe at the far left down to the underground room, and let the `GreenKoopa` hit you until you die (do not kill it). After the respawn in `room2`, the overworld music plays, as today.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1 --players 2`: let slot 0 die. Slot 1 goes at once. Only slot 0 beams in; slot 1 appears 1 tile to the right at `READY`, as today.

## Behaviors to check

- Pause, then die (for example to a fall while the pause opens): the game resumes.
