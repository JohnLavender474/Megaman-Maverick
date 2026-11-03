# Notes: mega-refactor-068a-slopes

## Decisions

- 2026-11-03 (owner, scope review): no engine step after `resolveCollisions` in `068a`. The stick runs in
  `MegaCollisionHandler` (`SCOPE.md`, S1). This replaces the engine step of the item decision of 2026-10-28. If a
  later branch needs a post-resolve step, it goes with `017`.
- 2026-11-03 (owner): no X6 flat step-down in `068a`. The low end of a run is at the height of the next floor. The
  step-down is a backlog candidate in `TODO.md`.
- 2026-11-03 (owner): the `BLOCK` fixture of a `Slope` is a triangle `GamePolygon` (`SCOPE.md`, S3).
- 2026-11-03 (owner): no change to any feet fixture (item open question 3).
- 2026-11-03 (owner, scope change): the class is `GroundSlope`, not `Slope`: slopes are floors only for now. Slopes are
  plain rectangle objects with the `side` property, in `blocks_slopes` or `blocks_slopes_<n>` layers (the existing
  blocks builder). Rejected: a tile-object template with a slope picture (it ties the loader to
  `TiledMapTileMapObject` and needs a tileset in each map), generated editor guides (they go stale until a save), and
  a dedicated `slopes` layer builder (new code for an authoring convenience; the `003` validator is the better place).
- 2026-11-03 (owner): no baseline measurement in `INTRO_STAGE` (plan step 2 dropped). One `--diagnostics` sanity run
  in the slope test map after the implementation. This is an accepted gap of `O5-R2`: the new handler checks run in
  every level, and they are not measured before and after. At the merge, the gap goes in the `O5` current state.
- 2026-11-03 (owner, design review in step 4): keep `GroundSlope` as a `Block` subclass that sets `BodyLabel.SLOPE`
  on itself. Rejected: a `SlopeComponent` (any entity could take it, also a dynamic player, as a silent no-op), a
  `Block.slope` field (slope state on every block), and an unnamed rectangle with `body_labels=slope` (it breaks the
  pattern of a class name as the Tiled shorthand).
- 2026-11-03 (owner): fail fast. A body with `BodyLabel.SLOPE` whose entity is not a `GroundSlope` throws
  `InvalidSlopeLabelException`, with the `ClassCastException` as the cause and a message with the class, the map object
  id and the Tiled fix. No silent fallback to the rectangle push-out. The owner wants this rule in the whole project.
- 2026-11-03 (owner): `trySpecialCollission` reads `staticEntity` and `dynamicEntity` once, right after it sorts the
  bodies, so that a body with no entity throws early. All later reads in the function use the two values. Every file
  that makes a `Body` in `core` calls `BodyComponentCreator.create`, which sets the entity (323 of 323 files), so the
  main levels are not expected to throw.

- 2026-11-03 (owner, after the diagnostics run): the stick moves from the collision handler to
  `GroundSlope.hitByFeet`, the same idea as the sticky feet of moving blocks: the feet contact exists with no body
  overlap. Gates: `CONTINUE` only (no early snap on a landing), `Direction.UP`, `velocity.y <= 0` (a jump, a knockback
  and a stun all rise first), `physics.gravityOn` (a hovering body with a feet fixture is not pulled down), and the
  bottom-centre in the run or just past the low end. Not gated by `STICK_TO_BLOCK`: Megaman sets it false in water,
  and a slide in water would float off the slope. Accepted edge case: Megaman who climbs down a ladder that ends on a
  slope snaps onto the surface (no ladder ends on a slope today).
- 2026-11-03 (owner): each `GroundSlope` draws a filled triangle with the `ShapeRenderer`, in all rooms. The slope
  tiles go from the test map. The diagnostics run already done stays valid; no new run with the shapes.

## Overlap with other branches

Checked 2026-11-03, `master` at `574cdbbf7`.

- `mega-refactor-019c-merge-spawn-queues`: docs only so far. Its plan changes `MegaLevelScreen`, `SpawnsManager` and
  `PlayerSpawnsManager`, and nothing in `engine/`. No overlap.
- `mega-refactor-118b-batch-1`: `fights/`, 8 lines of `MegaLevelScreen` and fight tests. No overlap.
- `x6-intro-extra-level` (not a refactor branch): adds `X6_INTRO` at the end of `LevelDefinition` and of
  `LevelCompletionScreensTest`. `REFACTOR_TEST_068` goes after `REFACTOR_TEST_047` so that the two do not conflict.
  Its map `X6Intro_16x14_v1.tmx` has a `TODO` layer with 13 markers "replace steps with slope": the first real user
  of `Slope`, after this branch.
- `compactor-level` (not a refactor branch): adds `COMPACTOR` at the end of `LevelDefinition`. Same placement rule.
- Item `017` (planned): world-step hooks in `WorldSystem`. No overlap if `068a` adds no engine step (`SCOPE.md`,
  open question 1).
