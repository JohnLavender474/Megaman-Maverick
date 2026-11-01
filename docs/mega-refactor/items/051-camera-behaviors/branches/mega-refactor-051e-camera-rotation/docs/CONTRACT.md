# Contract: mega-refactor-051e-camera-rotation

The gameplay that this branch must not change, and how to check it. Do the checks before the branch merges, and write
the result in the branch process log.

The game must play the same as `master`. A camera turn looks and acts the same: its time, its direction, the backgrounds,
the movement lock, and every entity that waits for it.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The build has no more than the baseline of 96 unique warnings.
- [ ] `RotatableCamera`, `GameCameraState`, `CameraRotationStep` and `CameraRotator` have no reference in `core`, `engine`
      or `lwjgl3`, other than the temporary suite.
- [ ] `ICameraPart` and `CameraController` have no type parameter.

### Temporary contract test suite

`RotationEquivalenceTest` in `core/src/test/`, with a frozen copy of `RotatableCamera` named `FrozenRotatableCamera`. Both
are deleted in the last commit before the merge.

The suite runs the frozen camera and the new `CameraRotation`, `CameraView` and `CameraController` on the same seeded
sequences of `start`, `snap` and delta values, with a new turn during a turn in some sequences. After each step it
asserts:

- [ ] `direction` is equal.
- [ ] `rotating` is equal to `!rotFinished`, and the end callback runs on the same step.
- [ ] `viewWidth` and `viewHeight` are equal.
- [ ] `blendedViewWidth` and `blendedViewHeight` are equal within `0.001`.
- [ ] The angle is exactly equal, so a 180 degree turn goes the same way.
- [ ] `up` is within `0.00001` of the exact value of the angle.
- [ ] `up` is equal to the frozen camera within `0.003`, over at least 1000 turns. The frozen camera drifts up to `0.0018`
      from the exact value in 1000 turns (measured on 2026-11-01), and the absolute write removes that drift.

### Permanent tests

- [ ] `CameraRotationTest` (`engine`): the turn, a new turn during a turn, the snap, the end callback.
- [ ] `CameraViewTest` (`engine`): the view sizes, the blend, and a camera with no rotation part (`rotating` is `false`,
      `direction` is `UP`, and the sizes are the viewport sizes).
- [ ] `CameraOrientation`: the write of an angle, and no write when the angle did not change.
- [ ] `CameraController`: the write with a rotation part, no write without one, and `reset()` does not change the
      rotation.

## Manual playthrough

Run the commands from this worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051e-camera-rotation`

Experimental checks (the code that this branch rewrites):

1. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_switch_sides`: use the `left` switch, and then fall past the
   leading edge to die. At the respawn, the camera makes a smooth turn back to up. Megaman cannot move until the turn
   ends, and then he falls and moves as normal. The camera must not snap.
2. `./run-game-alpha.sh --level REFACTOR_TEST_016 --room gravity_change`: walk into the `down` area and then the `up` area. The
   camera turns each time with the same speed, Megaman holds still in the air during the turn, and he falls after it.
3. `./run-game-alpha.sh --level MOON_MAN --room room6`: the gravity switches in rooms 8 to 10 turn the camera and the
   backgrounds together, and the backgrounds stay in place after the turn. Room transitions work with a turned camera.
4. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: pause during a turn. The turn stops, and it continues after the
   resume. Then the camera eases back to Megaman after the turn.
5. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_flip_right`: touch the `down` switch, and pause at once (a turn
   takes 0.75 s). Use the exit button. On the save screen, use `CONTINUE` and start a level. The camera is up, Megaman can
   move, and no entity acts as if a turn runs.

Control checks (behavior that must not change):

6. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_rotated`: the spawn in a `left` room, the edge blocks and the
   view size in a turned camera.
7. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_gravity`: the scroll waits for the turn and continues in the
   same world direction with no jump.
8. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room to_gravity`: enter the next room. At the end of the transition, the
   gravity changes and the camera turns.
9. `./run-game-alpha.sh --level INFERNO_MAN --room room5`: go right into `room6`. The shake and the follow are as before.
10. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: one short pass of a real stage with no turn. Die in a pit, and the
    camera resets to the spawn.

## Behaviors to check

- During a turn, these wait as before: enemies that read `rotating` (for example `SniperJoe`, `Bat`, `TellySaucer`),
  projectiles, the non-player bodies, and Megaman's jetpack, air dash, crouch and slide. Checks 2 and 3 show these where
  they meet a turn.
- `END_GAME_CAM_ROTATION` unlocks Megaman, and a smooth follow stops at the end of a turn.

## Intended changes

No gameplay change.

One internal change: after `MegaLevelScreen.reset()` during a turn, `rotating` is `false` at once. Today it stays `true`
until the next level start. No level runs in that time, so it cannot be seen in play. Check 5 covers it.
