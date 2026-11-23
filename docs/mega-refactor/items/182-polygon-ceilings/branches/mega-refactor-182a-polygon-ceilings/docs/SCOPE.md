# Scope: mega-refactor-182a-polygon-ceilings

- Status: approved
- Approved: 2026-11-23 (owner accepted the recommendation of each brief decision, D1 to D4)

## Goal

A level can have ceilings whose underside is not flat. A new block `CeilingPolygonBlock` (a convex Tiled polygon in a
`blocks` layer) pushes a body down by its polygon, not by its bounding rectangle. A body that rises into a 45-degree V
stops on the face, and `DrillHead` drills into the face. The polygon is reliable only as a ceiling, and a player never
wall-slides on it.

## Item

Item `182` (`items/182-polygon-ceilings/ITEM.md`). This branch takes all of "Proposed scope" except the Precious Woman
map, and the folded bug. The owner's answers (2026-11-23, owner accepted the recommendation):

- Q1: push down only (as X6).
- Q2: (b) the push-out reports the head contact (`HEAD_TOUCHING_BLOCK`, `hitByHead`, `HIT_BY_BLOCK`); a typed per-body
  step record clears it.
- Q3: (a) a grounded body gets an x push with no side sense, plus (c) a level rule: enough clearance under each
  polygon.
- Q4: no response for a body whose gravity direction is not `UP`, plus a level rule: no polygon ceilings in levels
  where gravity turns.
- Q5: edge angles 0 and 45 degrees only; fail fast on others.
- Bug folded in: `LaserRules.kt:38` casts a polygon block shape to `GameRectangle` (`ClassCastException` on a
  `GroundSlope`).
- Not in `182a`: `PreciousWoman_v2.tmx` (branch `078e` has uncommitted work in it). See `TODO.md`.

## Current state

- Body push-out is AABB only. `WorldSystem.kt:128-136` (`resolveCandidate`) checks the body bounds (strict
  `Rectangle.overlaps`) and calls the handler; `StandardCollisionHandler.kt:28-43` pushes on the smaller axis.
- `MegaCollisionHandler.kt:23-125` (`trySpecialCollission`) handles labeled static bodies first. `:50-60` is the
  `SLOPE` branch, with the accepted `O5-R2` TODO at `:50`.
- `BodyLabel.kt:7-12`: `COLLIDE_DOWN_ONLY`, `COLLIDE_UP_ONLY`, `PRESS_UP_FALL_THRU`, `SLOPE`.
- `Block.kt:274-289`: a `STATIC` body with one `BLOCK` fixture, a `GameRectangle` set to the body each `preProcess`.
  `Block.onSpawn` sets the body from `BOUNDS`, or from `WIDTH`/`HEIGHT`/`POSITION`; a polygon object gives only
  `ConstKeys.POLYGON` (`Extensions.kt:79-86`).
- `GroundSlope.kt:50-56,91-101` shows how a block uses a `GamePolygon` fixture: local vertices from `(0, 0)`, because
  `Fixture.getShape()` (`Fixture.kt:51-64`) centres the copy on the body and `GamePolygon.setCenter` (`:202-206`)
  places the polygon by its bounding box.
- `SpawnersLayerBuilder.kt:75-93`: the object `name` is the class; `SpawnerShapeFactory` uses the map shape, so a
  polygon object spawns when its polygon enters the camera bounds.
- Head contact: `HeadBlockRule.kt:17-54` sets `HEAD_TOUCHING_BLOCK`, sets `vy = 0` at begin, calls `hitByHead` and the
  `HIT_BY_BLOCK` receiver (the receivers ignore `delta`: `PlayerBody.kt:155`, `ReactorMonkeyBall.kt:145`,
  `BouncingPebble.kt:151`). `DrillHead.kt:253-256` ends `FLY` on the sense; its head fixture is 0.25 x 0.1 tiles,
  centred on the body top (`:284-286`), so it reaches 0.05 tiles above the body.
- Side contact: `BlockSideRule.kt:20,38,68` skips `SLOPE`; wall slide and wall kick come only from
  `SIDE_TOUCHING_BLOCK_*`.
- Feet contact: `FeetBlockRule.kt:17-74` has no label skip.
- `GroundProbe.kt:30` (`PASSABLE_LABELS`), `:131-140` (`blocks`, `isGround`).
- Laser: `LaserBlockRule` (`LaserRules.kt:20-63`): `begin` casts the block shape to `GameRectangle`; `continueContact`
  uses its bounding rectangle. The only `LASER` fixtures are in `Laser` (spawned by `LaserBeamer`) and `TubeBeamerV2`.
  `ShapeUtils.intersectRectangleAndLine` (`ShapeUtils.kt:31-52`) is the rectangle version.
- The world step: `WorldSystem.kt:247-292`: `beginStep`+`preProcess` for every body, `process`, contacts, contact
  rules, `resolveCollisions`, then `postProcess`+`endStep`. `IBody.stepSerial` is the serial of the current step.
- Blocks update always unless the map gives `update_distance` (`GameEntityUtils.kt:98-112`).
- `BlockCells` (pathfinding) uses body bounds (`BlockCells.kt:28-46`): conservative for a polygon.

## In scope

1. Engine `common/shapes/CeilingProfile.kt`: two pure functions on a lower chain (interleaved `x, y`, sorted by `x`):
   - `lowestCeiling(lowerChain, count, x0, x1): Float`: the lowest point of the chain over `[x0, x1]` (the span ends,
     clamped to the chain, and every vertex inside). First caller: `CeilingPolygonBlock`.
   - `spanBelow(lowerChain, count, y, out: Vector2): Boolean`: the `x` interval where the chain is below `y`. First
     caller: the grounded x push in `CeilingPolygonBlock`.
   No allocation. Engine tests.
2. Engine `ShapeUtils.intersectPolygonAndLine(polygon, line, out): Int`. First caller: `LaserBlockRule`.
3. `BodyLabel.CEILING`.
4. `entities/blocks/CeilingPolygonBlock`:
   - Body = the polygon's bounding rectangle; `BLOCK` fixture = the polygon (local vertices from `(0, 0)`); label
     `CEILING`.
   - Fail fast in `onSpawn` (`InvalidCeilingPolygonException`): no `POLYGON` prop, fewer than 3 vertices, rotation not
     0, not convex, an edge that is not 0 or 45 degrees.
   - `collide(body)`, called by the handler: down push (`maxY = lowestCeiling`, an upward `vy` becomes 0) for a body
     whose bottom is below the ceiling and whose top was at or under the ceiling at the start of the step (it came
     from below; `IBody.getStepStart`); for a body with `FEET_ON_GROUND`, or one that came from the side or from
     above, an x push out of `spanBelow(maxY)` in place of the down push. A body above the ceiling over its span gets
     no response. (Play test round 1, 2026-11-23: a wall slide down a column past the V top snapped Mega Man below the
     V; owner chose this rule.)
   - The `BLOCK` fixture has `NO_SIDE_TOUCHIE`, so `BlockSideRule` begin and continue skip it by the label.
   - Head contact (Q2): a typed map on the block from each touched `HEAD` fixture to the step serial of its last touch
     (D1). A head touches when its fixture top reaches the ceiling over the body span and the body came from below. The handler reports `BEGIN` or
     `CONTINUE` (sense on, `hitByHead`, `HIT_BY_BLOCK`; `BEGIN` also sets `vy = 0`). The block's `postProcess`
     checks each head not touched in this step again: still touching gives `CONTINUE`, else `END` (sense off). A dead
     entity's entry goes away with no call; `onDestroy` ends all entries.
5. `MegaCollisionHandler`: a `CEILING` branch before `SLOPE`. It casts to `CeilingPolygonBlock`
   (`InvalidCeilingLabelException` on a wrong entity), calls `collide` only for a body with direction `UP` (Q4), and
   always returns `true`. The `O5-R2` TODO names the new branch.
6. Rule skips: `BlockSideRule` (begin and continue by `NO_SIDE_TOUCHIE`; end by `CEILING`), `FeetBlockRule` (begin, continue), `HeadBlockRule` (begin,
   continue, end), `GroundProbe` (`PASSABLE_LABELS`, and never ground).
7. `LaserBlockRule`: polygon blocks use `intersectPolygonAndLine` at begin and continue (fixes the folded bug).
8. `core/src/smokeTest/resources/entity-tags.txt`: the new class.
9. Test map `RefactorTest182.tmx` (`REFACTOR_TEST_182`, `TEST_LEVEL`) with a room per check, and its row in
   `LevelCompletionScreensTest`.

## Out of scope

- `PreciousWoman_v2.tmx` (26 V points and 18 rectangle blocks): after `078e` and `182a` merge (`TODO.md`).
- Polygon push-out for floors, walls or other shapes; concave polygons; gravity other than `UP`.
- `Laser` and `TubeBeamerV2` map obstacles (rectangles from the map), `CameraEdgeBlock`, crush surfaces.
- A level validator for the 2 level rules (item `003`).

## Affected areas and risks

- `MegaCollisionHandler`: one more label read on every static-dynamic pair in every level (`O5-R2`, D2).
- Head-sense readers: the reported contact must match the rectangle-block timing (begin `vy = 0`, end when the head
  leaves). Tests cover begin, continue, end and the flush case.
- Wedging of a grounded body under a low face (`O1-R3`): the x push and the clearance rule.
- Rule skips: a `CEILING` body never gives a side, feet or head contact through the contact rules.

## Overlap with other branches

- `compactor-level` adds `LevelDefinition` entries: a list append, simple conflict.
- `078e` changes `PreciousWoman_v2.tmx` and `BreakableBlock.kt` (uncommitted). No file overlap with `182a`.
- `fix/auto-perf-freeze`, `fix/projectiles-ignore-death`, `060k`, `078f`: no overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | The head check runs in the block's `postProcess`, after `resolveCollisions`; the handler reports first. |
| `O1-R4` Typed state | helps | The head record is a typed map on the block, not a property key (D1). |
| `O1-R5` Code with a caller | no effect | Each new API has its caller in this branch (see "In scope"). |
| `O2-R4` Stable order | no effect | The head map is an `ObjectMap`; `END` calls go to different bodies, no order effect on gameplay. |
| `O2-R5` One player | no effect | No per-player path. |
| `O4-R1` Mechanics from Tiled | helps | A polygon object in a `blocks` layer. |
| `O4-R2` Test room per mechanic | helps | `REFACTOR_TEST_182`, a room per check. |
| `O5-R1` No per-frame allocation | no effect | Owned arrays and fields; pool-free math. |
| `O5-R2` Measure hot paths | helps | The owner runs one `--diagnostics` pair, master and branch (D2). |
| `O6-R1` CI passes | no effect | |
| `O6-R2` Named checks | helps | Engine and core tests for each new rule; manual checks below. |

## Open questions

None.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes (new `CeilingProfileTest`, `ShapeUtilsTest` cases).
- [ ] `./gradlew core:test` passes (new `CeilingPolygonBlockTest`, `CeilingPolygonCollisionTest`, rule-skip cases,
  `LaserBlockRuleTest`, `LevelCompletionScreensTest` row).
- [ ] `./gradlew core:smokeTest` passes (`entity-tags.txt` row).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

From `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-182a-polygon-ceilings`. Experimental
checks first, then controls.

| # | Level and room | What to check |
|---|---|---|
| 1 | `REFACTOR_TEST_182` `v_jump` (`--debugShapes`) | Jump into the V ceilings of each section (one V, a row of 4, a row under a higher ceiling, 2 Vs at the end of a flat ceiling): the jump ends at once, as under a flat ceiling. With a face over his head (a tip beside him), Megaman stops on the face, above the tip line; with a tip over his head, he stops at the tip. |
| 2 | `REFACTOR_TEST_182` `no_wall_slide` (`--debugShapes`) | Jump up into the V faces under each of the 3 hanging columns (tips at 3 heights) while holding toward the column: no wall slide or wall kick on a V face; Megaman slides off the face. A wall slide on the flat side of a column is the normal rectangle behavior. |
| 3 | `REFACTOR_TEST_182` `low_face` (`--debugShapes`) | Walk and slide into the low V faces of each section (a row of 3 and one V with tips 1 tile up, a row of 2 with tips 2 tiles up): Megaman stops at the face (no wedge, no fall through the floor, no wall slide). |
| 4 | `REFACTOR_TEST_182` `drill_head` (`--debugShapes`) | `DrillHead` rises into the V ceilings and drills with its drill in the face. Walk to the left wall and back to respawn it. |
| 5 | `REFACTOR_TEST_182` `laser` (`--debugShapes`) | The `LaserBeamer` sweeps over the V ceilings and a `GroundSlope`: no crash; the beam ends at the faces. |
| 6 | `REFACTOR_TEST_182` `rect_ceiling` | Control: jump into a flat ceiling (bonk) and wall-slide on the wall: as before. |
| 7 | `REFACTOR_TEST_068` `shallow` | Control: walk up and down the slopes: as before. |
| 8 | `PRECIOUS_WOMAN` (a `DrillHead` room) | Control: the `DrillHead`s act as before (rectangle ceilings). |
| 9 | `--diagnostics` pair (D2) | The same room on `master` and on the branch: `resolveCollisions` mean in the two `*-diagnostics.txt` files. |

The final commands are in `temp/play/` of the worktree at the handover.

## Behaviors to check

- No change for maps with no `CeilingPolygonBlock` (all live levels).

## Intended changes

- New: `CeilingPolygonBlock` push-out, head contact, no side or feet contact (item `182`, Q1 to Q5).
- Bug fix: a laser that hits a polygon `BLOCK` fixture (`GroundSlope` or `CeilingPolygonBlock`) no longer throws
  `ClassCastException`, and its hit points are on the polygon (item `182`, "Bugs folded in").
