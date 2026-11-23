# Notes: 182 polygon ceilings

Research of 2026-11-23, by 2 read-only agents in the `078e` worktree (branch `mega-refactor-078e-precious-woman-art`,
based on `master` at about `e3f5caf04`). Their scripts and crops are in
`.worktrees/mega-refactor-078e-precious-woman-art/temp/078e-angled-ceilings/` and `temp/078e-polygon-ceilings/`
(git-ignored; they go with the worktree).

## The case in the Precious Woman map

- Each crystal column ends in a 45-degree V, 2 cells wide and 1 cell deep: tile pairs `402/403` and `412/413` in
  `tiles1` of `PreciousWoman_v2.tmx`, 26 V points (52 tiles). 18 rectangle blocks cover the full rectangle of each V,
  so the block bottom is at the tip line.
- `DrillHead` stops when `HEAD_TOUCHING_BLOCK` turns on (`DrillHead.kt:253-256`); its sprite is set at the body's top
  centre (`DrillHead.kt:315`). 6 of the 8 `DrillHead`s sit under a V. The jagged `decor` fringe (tiles `515-559`) is
  over 2 of them.

## The world step today

- Order in one step (`WorldSystem.kt:246-290`): `process`, `collectContacts`, `processContacts`, `resolveCollisions`.
  The contact rules see an overlap before the push-out.
- `resolveCollisions` (`:419`) skips static bodies; a dynamic body queries the grid cells of its bounds;
  `resolveCandidate` (`:128-136`) checks the AABBs before it calls the handler.
- Body push-out is AABB only: `StandardCollisionHandler.kt:28-43` pushes out on the smaller axis.
- Fixture contacts use the real shapes (`WorldSystem.kt:116-126`); `GamePolygon.overlaps` (`GamePolygon.kt:154-175`)
  uses `Intersector.overlapConvexPolygons`, so it is right for convex shapes only.
- A block is a `STATIC` body with one `BLOCK` fixture, a `GameRectangle` (`Block.kt:277-283`).
- Tiled polygon objects already become `GamePolygon` props (`Extensions.kt:57,65,83`); no block reads them.

## Floor slopes (`068`) and why they are not reused

- `GroundSlope.kt` is a floor only: a triangle `GamePolygon` (`:50,91-101`), ratios 1/4 and 1/2 (`:31-32,76-89`),
  X6-style steps in `surfaceAt` (`:128-134`), `Direction.UP` only (`:111`).
- `MegaCollisionHandler.kt:52-61,144-155` snaps the body bottom centre to the surface when `vy <= 0`;
  `:157-176` lets a body on a slope pass a flat block next to it (`standsOnSlopeBeside`).
- `HeadBlockRule.kt:19,33,45`, `BlockSideRule.kt:21,39,68` skip `SLOPE`; `GroundProbe.kt:30` treats it as passable.
- Tests: `GroundSlopeCollisionTest` (165 lines), `GroundSlopeWalkTest` (223), `GroundSlopeContactRulesTest` (103),
  `GroundSlopeFeetStickTest` (128), `GroundSlopeSurfaceTest` (127).
- Owner: not reused (feet logic, wedging).

## Wall slide and head contact

- Wall slide comes only from `SIDE_TOUCHING_BLOCK_*`, set only by `BlockSideRule`. Readers: `PlayerWallSlide.kt:25-26`,
  `PlayerJump.kt:41-42`, `PlayerAirDash.kt:112`, `PlayerBody.kt:96,239-243`, X `MegamanXMoves.kt:127-128,150-152`.
- Head contact: 38 reads of `HEAD_TOUCHING_BLOCK` in 32 files; 44 entity files define a `HEAD` fixture.

## Algorithm options

| Option | Corners | Tunnelling | Cost per AABB pair | Touches |
|---|---|---|---|---|
| A. Lowest ceiling over the top span, down only (recommended) | exact: span ends and every vertex in the span | max step 17.1 px (`CLAMP_Y` 40 tiles/s at `VERY_LOW`, 1/75 s) < 32 px V depth | 3-vertex loop, no allocation | 1 engine file, 1 handler branch, 1 block class |
| B. SAT minimum push limited to downward normals | exact; slides along the face | same | rectangle-polygon SAT, 7 axes; custom (libGDX MTV is not limited) | engine file + SAT helper, handler, block |
| C. Stair-step rectangles in Tiled with `no_side_touchie` | step error; side pushes at step corners (snags) | same | more bodies in the grid | map only; not "ceiling only" |
| D. One concave polygon | push right | same | as A | contacts wrong (convex-only `overlaps`); needs parts |

Rejected for now: polygon push-out in the engine for all shapes (every body and level changes; highest cost on weak
hardware) and per-entity fixes (only `DrillHead`).

## Files and call sites (design A)

- Engine: new `common/shapes/CeilingProfile.kt` (`lowestCeiling`) and its test.
- `BodyLabel.kt`: `CEILING`.
- New `entities/blocks/CeilingPolygonBlock.kt` (fail-fast checks in `onSpawn`); `core/src/smokeTest/resources/entity-tags.txt`.
- `MegaCollisionHandler.kt` about `:52` (the `CEILING` branch); a per-pair label read joins the `O5-R2` TODO at `:50`.
- `BlockSideRule.kt:21,39,68`, `FeetBlockRule.kt` (begin and continue), `HeadBlockRule.kt:19,33,45` (per open
  question 2), `GroundProbe.kt:30,137` (`PASSABLE_LABELS`; not `isGround`; else its bounding box reads as `WALL`; 2 users).
- `LaserRules.kt:38,58`: polygon intersection.
- `contact-rule-selection.txt`: no change (no new fixture pair).
- No change: `BlockCells` pathfinding (bounds, `BlockCells.kt:41`, conservative), `CameraEdgeBlock` and crush
  surfaces (rectangles; a polygon never crushes), co-op (no per-player path).

## Bug found

`LaserRules.kt:38` casts the block shape to `GameRectangle`: a laser that hits a `GroundSlope` throws
`ClassCastException` (checked 2026-11-23). It must be fixed for polygon blocks anyway; file it under `docs/issues/`
if this item does not start soon.

## Tests today and needed

- Today: `StandardCollisionHandlerTest` (68 lines), `WorldSystemTest` (143), `GamePolygonTest` (125),
  `MegaCollisionHandlerTest` (71), `BlockSideRuleTest` (115), `GroundProbeTest` (175), and the 5 slope files above.
- Needed: `lowestCeiling` (span across the apex, on one face, past the ends, two teeth); push-out (down, `vy` to 0;
  no response for a body above; the grounded case of question 3); no side sense from `CEILING`; head contact per
  question 2; `GroundProbe` not `WALL`; laser against a polygon; each fail-fast case.

## Risks

- Performance: one more label read on every static-dynamic pair in every level (`O5-R1`, `O5-R2`); read the label set
  once per pair; measure before and after with `--diagnostics`.
- Head-sense readers change behavior unless question 2 is (b) (`O6-R2`).
- Wedging of a grounded body under a low face (`O1-R3`).
- Cutting back the 18 rectangle blocks can open gaps beside the V points.

## Stopgap considered (`078e`)

E+: while drilling, `DrillHead` lifts its body and sprite 16 px with collision off and draws in
`DrawingSection.BACKGROUND`, so the tiles cover the drill tip (tiles draw between `BACKGROUND` and `PLAYGROUND`,
`MegaLevelScreen.kt:951-963`). Mockup: `.worktrees/mega-refactor-078e-precious-woman-art/temp/078e-drill-mock/drill_mock.png`.
A sprite-only lift (E) was rejected: the body and its damager would stay 16 px below the sprite.
