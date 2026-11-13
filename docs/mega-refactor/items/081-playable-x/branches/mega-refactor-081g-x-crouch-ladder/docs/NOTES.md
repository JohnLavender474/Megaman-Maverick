# Notes: mega-refactor-081g-x-crouch-ladder

## Decisions

Owner, 2026-11-13, at the brief (all recommendations): D1 shared climb, D2 X6 ladder ends, D3 X6 crouch, D4 one-way
pass only while climbing. Details in `SCOPE.md`.

Made during the work (no owner decision needed):

- **X's crouch body is 1 tile** (standing 1.5). The art: the standing shot pose is 28 px tall, the low crouch frame
  22 px. A 1-tile body is a little lower than the art, so a low shot that grazes the art passes.
- **The ladder-end lock.** The climb-down pose (`climb_start`) holds X still on the ladder (`holdStill`), not with a
  lock: a `MOVEMENT` lock ends the climb, because the climb needs `playerCanMove`. The climb-over pose
  (`climb_finish`) starts after the climb ends, so it uses a `MOVEMENT` lock with the new reason
  `MegaLockReason.LADDER`.
- **The shot on the ladder** leaves at `SHOT_OFFSET_Y_CLIMB` (0.45 tile over the body center), measured from the
  `climb_shoot` arm (19 px over the feet). The crouch needs no new offset: the shot offset is from the body center,
  which goes down with the crouch body.
- **X climbs while he shoots** (X6 `updateClimb` moves after `tryShoot`). Megaman still stops while he shoots.
- **X has no sabre on the ladder** (no art).
- **Test room `crouch`:** the `SniperJoe` step is 20 px high (the agent's 16 px put the shot 1 px into a 1-tile
  crouch body).

## X6 sources (facts only, no code copied)

`~/IdeaProjects/mmx-x6/core/src/main/java/mmx/x6/game/player/`:

- `Player.java`, `toCrouch` and `updateCrouch`: crouch on `DOWN` on the ground; X stops, turns, shoots; jump and dash
  leave the crouch; on release a rise animation, then stand.
- `Player.java`, `tryLadderFromGround`, `tryLadderAir`, `updateLadder`, `updateClimb`: grab with `UP` at the head,
  `DOWN` on a ladder top, `UP` in the air (resets the air dash); the climb-down and climb-over animations at the top;
  `DOWN` on a floor at the bottom stands; jump lets go; turn and shoot on the ladder.
- `PlayerConst.java`: `LADDER` = 1.5 px per frame, so 5.625 tiles/s at 60 fps (`CLIMB_SPEED`).

## Overlap with other branches

- `060c` (Proto Man's buster and sabre, another session, 2026-11-13): it changes `MegamanX.kt`, `MegamanXBuster.kt`,
  `MegamanXController.kt`, `MegamanXSprites.kt`, `MegamanXSabre.kt` (moved to `MegamanXSabreMoves.kt` and
  `players/PlayerSabre.kt`) and `players/README.md`. This branch changes the same files in small hunks: the behavior
  list and the animations map in `MegamanX.kt`, the shot offset in the buster, the turn-only run, the animation keys,
  and `canStart` of the sabre. Expect text conflicts in whichever branch merges second; the hunks are independent.
  The X climb and crouch code is in its own file (`MegamanXClimbCrouch.kt`) to keep the overlap small.
