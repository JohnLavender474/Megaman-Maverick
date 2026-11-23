# TODO: mega-refactor-182a-polygon-ceilings

## Tasks

- After `078e` and `182a` merge: convert the 26 V points of `PreciousWoman_v2.tmx` to `CeilingPolygonBlock` objects,
  and cut the 18 rectangle blocks over the V rows back to the row above each V (item `182`, "Proposed scope"). Do not
  edit the map before: `078e` has uncommitted work in it.
- At the merge on `master`: add the 2 level rules to `docs/ways-of-working/levels.md` (D3).

- `CeilingPolygonBlock` with `update_distance`: a head sense can stay on (TODO at `checkHeadContacts`); decide at the
  Precious Woman conversion whether to fail fast on the property.

## Backlog candidates

## Bug candidates

- `--room` start spawn taken by a later spawn near the camera origin: filed on `master` as
  `docs/issues/2026-11-23-launch-room-spawn-taken-by-camera-origin.md`; map workaround in `RefactorTest182.tmx`.
