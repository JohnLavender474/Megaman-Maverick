# Plan: mega-refactor-182a-polygon-ceilings

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

## Steps

### 1. Test map `RefactorTest182.tmx`

- Changes: map with rooms `v_jump`, `no_wall_slide`, `low_face`, `drill_head`, `laser`, `rect_ceiling` (level-builder
  skill, subagent); `LevelDefinition.REFACTOR_TEST_182`; `LevelCompletionScreensTest` row.
- Check: each room has a player spawn and painted `tiles`; polygons are convex with 0/45-degree edges.
- Done: yes

### 2. Engine `CeilingProfile`

- Changes: `lowestCeiling`, `spanBelow`; `CeilingProfileTest` (span across the apex, on one face, past the ends, two
  teeth, flat bottom).
- Check: `engine:test`.
- Done: yes

### 3. Engine `ShapeUtils.intersectPolygonAndLine`

- Changes: function and `ShapeUtilsTest` cases.
- Check: `engine:test`.
- Done: yes

### 4. `BodyLabel.CEILING` and `CeilingPolygonBlock` shape

- Changes: the label; the block (body from the polygon bounds, polygon fixture, lower chain); fail-fast checks with
  `InvalidCeilingPolygonException`; `entity-tags.txt`.
- Check: `CeilingPolygonBlockTest` (each fail-fast case; fixture shape at the polygon).
- Done: yes

### 5. Push-out and handler branch

- Changes: `CeilingPolygonBlock.collide` (down push, x push for a grounded body); the `CEILING` branch in
  `MegaCollisionHandler` with `InvalidCeilingLabelException`; the `O5-R2` TODO.
- Check: `CeilingPolygonCollisionTest` (down, `vy` to 0, body above, beside the tip, grounded x push, gravity not `UP`,
  never the rectangle push).
- Done: yes

### 6. Head contact

- Changes: the head map, begin/continue in `collide`, the `postProcess` check, `onDestroy`.
- Check: tests for begin (sense, `vy` 0, receiver), continue (flush under the tip), end, dead entity.
- Done: yes

### 7. Rule skips

- Changes: `BlockSideRule`, `FeetBlockRule`, `HeadBlockRule`, `GroundProbe` skip `CEILING`.
- Check: rule tests with a `CEILING` block.
- Done: yes

### 8. Laser fix

- Changes: `LaserBlockRule` uses `intersectPolygonAndLine` for a polygon shape.
- Check: a laser test against a `GroundSlope`-like polygon fixture (no exception, hit points on the faces).
- Done: yes

### 9. Automated checks, review, handover

- Changes: none in code; review fixes.
- Check: the contract's automated checks; the Sonnet review; play scripts in `temp/play/`; Diffity.
- Done: no
