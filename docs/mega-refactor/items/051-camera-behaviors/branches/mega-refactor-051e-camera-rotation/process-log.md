# Process log: mega-refactor-051e-camera-rotation

## Status

- State: planned
- Goal: remove `RotatableCamera` and `GameCameraState`: plain cameras, a `CameraRotation` part, and `rotating` on `ICameraView`
- Done:
- Next: the scan, then `docs/SCOPE.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-01

- No branch exists yet. The folder holds the plan for the branch. The design is in the `051` `ITEM.md`, "Decisions"
  (2026-10-30 and 2026-11-01).
- Contract checks to carry into `docs/CONTRACT.md`: the value of `rotating` at level start; a small float tolerance on
  `up`; the direction change at the start of a turn; the place of the rotation step in the effect order; the
  `END_GAME_CAM_ROTATION` listeners; the smooth turn on a respawn in a turned room.
