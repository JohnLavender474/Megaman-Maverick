# 154: Spawners test the camera after it moves

- Status: dropped (2026-11-13): the owner saw no fault in play; a 1-frame delay is not visible
- Type: refactor
- Objectives: `O9`, `O1`
- Depends on: none
- Related: `138`
- Branches: none yet

## Dropped

2026-11-13 (owner): "I never noticed any issue in gameplay. A single-frame delay is barely noticeable if at all."

The scope research (2026-11-13) found:

- The delay is 1 frame. A queued spawn takes effect in the same frame's `engine.update`; the only lag is that the
  spawner tests the camera of the frame before (`MegaLevelScreen.render`: the spawner update, then `engine.update`,
  then `cameraController.update`).
- Moving the spawner update after the camera update does not remove the delay: the spawn takes effect only at the
  next `engine.update`, where the sprites are collected.
- A view margin on all camera spawners (4736 objects on 31 live maps) needs the same margin on the cull test
  (`getGameCameraCullingLogic`, 107 call lines in 51 files). Otherwise an entity spawned in the margin culls at once
  and spawns again. It also starts enemy AI and timers earlier, and changes which object a spawn cap (`Bat`,
  `SpikeCopter` and others) lets through.
- About 1100 objects (every enemy, all items) already have a sprite larger than their spawn rect; about 400 objects
  (28 classes, mostly blocks, spikes and water) are the pure 1-frame case. Only 6 objects have a spawn rect that leads
  the sprite.
- The lowest-risk fix, if this comes back: a spawn lead for `MapSprite` only, in `SpawnerShapeFactory.getSpawnShape`.

The `TODO` at `MapSprite` stays as the record of the accepted edge case.

## Goal

`MegaLevelScreen` updates the spawners with the camera bounds of the frame before, then moves the camera, then draws.
An entity whose spawn rect is its sprite rect (for example a `MapSprite`) can be missing for 1 frame at the place
where it enters the view. Test the spawners after the camera moves, or with a small margin around the view, so that
an entity is in place on the first frame that shows it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Measure which entities show this today (a spawn rect equal to or inside the sprite rect).
- Choose: move the spawner update after the camera update, or keep the order and add a view margin.
- Keep the spawn and cull order of every other entity the same.

## Owner's input

Found by the `138a` review before the play test (2026-11-13): wreck B in the X6 intro lost the 4 px spawn lead of its
old 32 px box. A backlog candidate in the `138a` branch `docs/TODO.md`.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `138a` branch `docs/TODO.md` and `process-log.md`; the `TODO` at `MapSprite`.
