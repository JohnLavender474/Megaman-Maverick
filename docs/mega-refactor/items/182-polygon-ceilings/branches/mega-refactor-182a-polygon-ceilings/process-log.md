# Process log: mega-refactor-182a-polygon-ceilings

## Status

- State: active
- Goal: polygon ceilings (`CeilingPolygonBlock`) push bodies down by their polygon; no wall slide on them; laser fix
- Brief: https://claude.ai/artifact/VkrPo8iocZyxfbfuGEyZ3x (approved 2026-11-23)
- Done: branch start, planning, implementation, automated checks, review before the play test
- Next: play test (checks in `temp/play/`) and Diffity review
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-23

- Branch start on `master`. Owner accepted the recommendation of each of the 5 open questions of `ITEM.md` and the
  merge target (directly into `master`, one branch).
- Scan, `SCOPE.md`, brief and `PLAN.md`. The owner's prompt said to take each brief recommendation (D1 to D4); no
  decision was new without a recommendation, so none was asked.
- Implementation, `PLAN.md` steps 1 to 8. The test map was built by one subagent (level-builder skill). It changed
  the `no_wall_slide` geometry: the brief asked for a 12-tile ceiling, a 3-tile-deep V and a tip 5 tiles above the
  floor, which do not fit together; a 6x4-tile rectangle hangs from the ceiling and the V hangs under it (top edge 8
  tiles above the floor).
- Scope detail: the "fewer than 3 points" fail-fast check was dropped: libGDX `Polygon` throws on fewer than 3
  points before the block sees the object, so the check had no reachable case.
- First `core:test` run: `GlacierManIcicleRainTest` failed because `CeilingPolygonBlockTest` left a spawned block
  in `MegaGameEntities`; the test now removes it in `afterEach`.
- Automated checks: `engine:test`, `core:test` (2905 tests, 0 failures), `core:smokeTest`, `lwjgl3:jar`,
  `detektMain core:verifyRandomRule`: pass.
- Mutations, one batched run (`engine:test core:test`), each caught:
  - handler calls `collide` for every direction: "gives no response to a body whose gravity is not up".
  - no grounded x push: "pushes a grounded body sideways out of the face".
  - `postProcess` never re-checks a head: "continues while the body stays flush under the tip".
  - laser uses the bounding box for a polygon: `LaserBlockRuleTest` polygon case.
  - `FeetBlockRule` skip removed: "never makes a ceiling polygon a floor".
  - `BlockSideRule` begin skip removed: "does not set the side sense for a ceiling polygon".
  - `lowestCeiling` ignores inner vertices: "gives the apex", "finds the lowest tooth of two".
  - Not caught: the duplicate-hit check in `intersectPolygonAndLine` (second run, `engine:test`). libGDX
    `intersectSegments` reports a shared vertex for only one of its 2 edges in the test case, so the check never
    fires there. It stays as a guard.
- Review before the play test: 1 Sonnet agent, diff and all 9 manual checks. Checks 1 to 8: OK; check 9 needs a run.
  Findings and actions:
  - Boxing in `onSpawn` (`getTransformedVertices(Array<Float>)`): once per spawn, not per frame; no constructor in
    the spawn body. Not changed.
  - Head sense stays on if the block stops updating (`update_distance`): accepted edge case, `TODO` at
    `checkHeadContacts` and in `TODO.md`.
  - Head touch by the body span, not the head x range: as the scope says. Not changed.
  - Grounded walkers get no side sense from a face: as Q3 says. Not changed.
  - Laser `getBoundingRectangle()` with no out: a pool scratch, not an allocation. Dismissed.
  - `GameLine.overlaps(GamePolygon)` can start a contact with 0 hits: no effect. Not changed.
  - Check 1 wording: Megaman (1 tile wide) stops at the tip when the tip is over his head. Contract text fixed.
- Play test round 1, Diffity comment: `CeilingPolygonBlock` now throws `CeilingPolygonGravityException` when it spawns
  while the world gravity is not `UP` (new test in `CeilingPolygonBlockTest`). A gravity change after the spawn is not
  checked.
- Play test round 1, check 1: the owner saw flat ceilings: the map painted the polygons with the flat tile 175.
  New rule on `master` (`2ed068162`, `docs/ways-of-working/levels.md`): the tiles agree with the goal. Owner choices:
  each room gets several sections that use only the 2x1 V (no new shapes or angles); the V art comes from the `078e`
  mine tileset (tiles 388/389, 392/393, 408/409, 412/413 of its working tree), copied into a test-only tileset
  `refactor_test_182_tiles` so that the `078e` files are not touched. Rooms `v_jump` (44 wide, 4 sections),
  `no_wall_slide` (36 wide, 3 hanging columns, tips 3, 5 and 7 tiles up; the 6x3 V is gone) and `low_face` (36 wide,
  3 sections) are rebuilt; all V cells use the V tiles and the columns above them. Contract checks 1 to 3 reworded.
- The test tileset is art of the Precious Woman tileset. Its credit is the Precious Woman entry that `078e` adds to the
  `README.md`; if `182a` merges first, add a test-tileset line there.
- 2026-11-23, owner accepted the recommendation: the `GroundSlope` of room `laser` keeps plain square tiles (no tile
  in the repo has its 1:2 angle; check 5 looks only at where the beam ends). An exception to the tiles rule, for this
  control room only.
- `core:test` after the round 1 fixes: 2906 tests, 0 failures. Mutation: the gravity guard removed, "fails fast when
  the gravity is not up" failed (and the test leaked its block into `MegaGameEntities`; the test now removes it in a
  `finally`).
- Play test round 1, owner screenshots: checks 1 to 5 started in room `rect_ceiling`, not in their own rooms. Cause: the
  `rect_ceiling` spawn overlaps the camera at its start position near the world origin, and the checkpoint manager
  makes it current. Filed on `master` as `docs/issues/2026-11-23-launch-room-spawn-taken-by-camera-origin.md`
  (`1daf6dbfa`). Workaround: the map has 16 empty rows at its bottom (`gen.py`), so the lowest spawn is at world
  y = 736. Checks 1 to 5 need a new run.
- Play test round 1, check 2: a wall slide down a hanging column snapped Mega Man below the V when his body reached the
  V top. Cause: his span touched the V box at its corner (zero width), where the lowest ceiling is the V top, and the
  airborne rule pushed him down. Owner chose (2026-11-23): push down only a body that came from below (its top at or
  under the ceiling at the step start, new `IBody.getStepStart`); else the x push. The head contact needs the same.
  Also by owner choice: the V `BLOCK` fixture has `NO_SIDE_TOUCHIE`, and the `CEILING` skip leaves `BlockSideRule`
  begin and continue. The `CEILING` skip in `onEnd` stays: without it, the end of a V contact clears the wall sense
  when the touched list prunes to empty (`CeilingContactRulesTest`).
- Tests after the snap fix: `engine:test` 776 and `core:test` 2909, 0 failures. Mutations, each caught: down push
  without the from-below test ("does not push down a body that slides down beside the face", "pushes a body that
  comes down into the face sideways"); head touch without it (same first test, `vy` 0, separate run); no
  `NO_SIDE_TOUCHIE` on the fixture ("gives the block fixture no side contact"). Check 2 runs again, then 1, 3 and 4.
- Play test: all checks pass (owner, 2026-11-23). Check 9 (`O5-R2` diagnostics pair) dropped by the owner; no
  measurement was made, and the `O5-R2` TODO at `MegaCollisionHandler` stays.
- Diff review round 1: 0 Diffity threads; 1 page comment (the gravity guard), fixed. Round closed; fixes committed in
  `f81980db0`.
