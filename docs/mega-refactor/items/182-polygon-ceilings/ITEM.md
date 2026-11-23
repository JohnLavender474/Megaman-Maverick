# 182: Polygon ceilings: blocks that push bodies out by their polygon

- Status: active
- Type: feature
- Objectives: `O4`, `O5`
- Depends on: none
- Related: `068` (floor slopes), `123`, `078e` (Precious Woman art: the V-point ceilings), `O5-R2` (the TODO at
  `MegaCollisionHandler.kt:50`)
- Branches: [`mega-refactor-182a-polygon-ceilings`](branches/mega-refactor-182a-polygon-ceilings/process-log.md)

## Goal

A level can have ceilings whose underside is not flat (for example the 45-degree V points that end the crystal
columns of the Precious Woman stage). The world pushes a body out of such a ceiling by the polygon itself, not by its
bounding rectangle, so a body that rises into a V stops on the face, and `DrillHead` drills into the art.

Today all body push-out is by bounding rectangles (`WorldSystem.kt:128-136`, `StandardCollisionHandler.kt:28-43`), so
the block under a V covers the empty triangles beside its tip, and a body stops at the tip line, up to 30 px below the
rock.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A new block `CeilingPolygonBlock : Block` from a Tiled polygon object in the `blocks` layer (`ConstKeys.POLYGON`,
  `Extensions.kt:83`). Body = the polygon's bounding rectangle; `BLOCK` fixture = the polygon; a new
  `BodyLabel.CEILING`. One convex polygon per block; a saw-tooth is several objects. Fail fast in `onSpawn` on a
  polygon that is not convex, has fewer than 3 vertices, is rotated, or has an edge angle that is not allowed.
- Engine: a pure function `lowestCeiling(lowerChain, count, x0, x1): Float` (the lowest point of the polygon's lower
  chain over the span `[x0, x1]`: the span ends and every vertex inside it), with no allocation, and engine tests.
- `MegaCollisionHandler`: a `CEILING` branch before the `SLOPE` branch: when the body overlaps, `body.maxY =
  lowestCeiling(body.x, body.maxX)`, an upward `vy` becomes 0; the branch always handles the pair, so the bounding
  rectangle never pushes.
- The contract (owner, 2026-11-23): polygons are reliable **only as ceilings**, and the player **never wall-slides**
  on a sloped edge. `BlockSideRule`, `FeetBlockRule` and `GroundProbe` skip `CEILING` (no side sense, so no wall
  slide or wall kick for Megaman and X; never a floor; never a `WALL` for the ground probe).
- `LaserRules.kt:38,58`: intersect the laser line with a polygon block (also fixes the `GroundSlope` crash below).
- The Precious Woman map: 26 polygon objects for the V points; the 18 rectangle blocks over the V rows cut back to the
  row above each V. A test map `RefactorTest182.tmx` (`REFACTOR_TEST_182`) with a room per check.

## Bugs folded in

- `LaserRules.kt:38` casts the block shape to `GameRectangle`, so a laser that hits a `GroundSlope` (a polygon
  `BLOCK` fixture) throws `ClassCastException`. No live map has a laser near a slope today. Found 2026-11-23 by the
  polygon-ceiling research (`NOTES.md`, "Bug found"); this item rewrites the laser-block intersection anyway.

## Owner's input

- 2026-11-23: "In the old tileset, the ceilings were flat so that when the `DrillHead` enemy rises up and hits the
  ceiling, it looks like his drill is hitting into the ceiling. However, with the 'spiked' ceiling tiles, his drill
  appears to be beneath the ceiling while he's 'drilling in'. [...] I kind of like the 'spiked look'."
- 2026-11-23: reusing `GroundSlope` upside down is "probably not ideal since it has built-in logic for the player's
  feet [...] might cause wedging or other edge cases". "The 2nd one here is ideal: the ability to push out entities
  using polygons. The contract here would be that polygons are considered reliable only as ceilings, and
  furthermore the player cannot wall slide against a sloped edge no matter what."
- 2026-11-23: the owner rejected a sprite-only lift of `DrillHead` (the body and its damager would stay below the
  sprite). A lift of body and sprite, drawn behind the tiles (E+ in `NOTES.md`), stays a possible stopgap.

## Decisions

All on 2026-11-23, owner accepted the recommendation:

- Merge target: directly into `master`, one branch, `182a`.
- Q1 push direction: down only (as X6: a slanted ceiling stops the rise).
- Q2 head contact: (b) the push-out reports the head contact (sets `HEAD_TOUCHING_BLOCK`, calls `hitByHead` and
  `HIT_BY_BLOCK`; a typed per-body step field clears it), so the 38 reads in 32 files work unchanged.
- Q3 grounded body under a low face: (a) an x push with no side sense, plus (c) a level rule: enough clearance under
  each polygon.
- Q4 gravity other than `UP`: no response, plus a rule: no polygon ceilings in levels where gravity turns.
- Q5 edge angles: 0 and 45 degrees only, fail fast on others.
- The Precious Woman map (`PreciousWoman_v2.tmx`) is not changed in `182a`: branch `078e` has uncommitted work in it.
  Its 26 V points become polygon ceilings in a later step, after `078e` and `182a` merge.

## Open questions

None. The 5 questions below were answered on 2026-11-23 (see "Decisions"); they stay for the record.

### Answered

Each with the research recommendation in bold.

1. Push direction: **down only (as X6: a slanted ceiling stops the rise)**, or along the face normal (a jump slides
   sideways along the face)?
2. Head contact on a face (the body's top corner meets the face, not its center; on the real shape only, the head
   fixture touches within about 5.6 px of a V tip, so 12 of 15 `DrillHead` hover spots near a V never sense the
   ceiling): (a) the real shape only; **(b) the push-out reports the head contact (sets `HEAD_TOUCHING_BLOCK`, calls
   `hitByHead` and `HIT_BY_BLOCK`; a typed per-body step field clears it), so the 38 reads in 32 files work
   unchanged**; (c) only `DrillHead` ends `FLY` when blocked.
3. A grounded body that walks into a low face, or stands up from a slide under one (down-only pushes it into the
   floor, a fragile order, `O1-R3`; `PlayerGroundSlide.kt:51` reads the head sense): **(a) an x push with no side
   sense, plus (c) a level rule: enough clearance under each polygon**; (b) always push down.
4. Bodies with gravity direction other than `UP`: **no response, plus a rule: no polygon ceilings in levels where
   gravity turns**; or push world-down anyway.
5. Edge angles: **0 and 45 degrees only, fail fast on others (all 26 V points are 45 degrees)**; or any convex shape.

## Sources

- `NOTES.md` in this folder.
