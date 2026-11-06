# Contract: mega-refactor-038a-respawn-routine

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-06

## Intended changes

- A death in a `SMB3_1` room with `music`, followed by a respawn in a room with no `music`, plays the level music. Today the death room's music keeps playing.
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
- [ ] `grep` finds no `PLAYER_SPAWN_INIT`, `PlayerSpawnEventHandler` or `PlayerDeathEventHandler` in live code.

Permanent tests:

- [ ] The routine runs its steps in the order of the brief, and submits each event in the frame of today.
- [ ] With no lives left, the routine ends at game over, and no respawn step begins.
- [ ] `cancel` (screen reset) ends the routine with no event.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-038a-respawn-routine`

- `./run-game-alpha.sh --level TIMBER_WOMAN`: at level start, the fade in, `READY` and the beam-in look as today. Die to an enemy: the fade out, the respawn and the beam-in look as today, and the enemies of the room come back.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: die in the `MoonHead` fight. The boss bar goes, the gate locks again, and the fight starts again when you enter.
- `./run-game-alpha.sh --level INFERNO_MAN`: die while the rising lava moves. The lava is dormant after the respawn, and starts again after `READY`.
- `./run-game-alpha.sh --level SMB3_1`: die in a room with its own music, after a checkpoint in a room with none. The level music plays after the respawn (intended change).
- `./run-game-alpha.sh --level TIMBER_WOMAN --players 2`: slot 0 dies. Slot 1 goes and spawns again with slot 0.
- Lose all lives in any level: the game over screen opens.

## Behaviors to check

- Pause, then die (for example to a fall while the pause opens): the game resumes.
