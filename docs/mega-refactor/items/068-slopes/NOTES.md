# Notes: 068 Floor slopes for extra levels

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-28: research (read-only agent, `master` at `d711798da`)

Paths: `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

### Nothing for slopes exists

- The only slope helper is `UtilMethods.getSlope(p1, p2)` (`E/common/UtilMethods.kt:76`), rise over run.
- Related helpers: `GameLine` (`E/common/shapes/GameLine.kt`), `ShapeUtils.intersectRectangleAndLine`
  (`ShapeUtils.kt:32`).
- No world-step hook, no map-level flag, no slope block type.

### Physics step (`E/world/WorldSystem.kt`)

- `process()` (L100-148) runs a fixed-step accumulator and calls `cycle()` once per sub-step.
- `cycle()` (L164-206), in order:
  1. `body.preProcess()`: entities set gravity, sizes and fixture offsets.
  2. `body.process(delta)` (`Body.kt` L103-118): friction, gravity, clamp, move.
  3. Rebuild the world container.
  4. `collectContacts` (L230-253): fixture shape overlap tests.
  5. `processContacts` (L208-228): begin, continue and end rules. This sets `FEET_ON_GROUND`.
  6. `resolveCollisions` (L255-275): for each dynamic body, `collisionHandler.handleCollision(body, candidate)` for
     each body whose AABB overlaps.
  7. `body.postProcess()`.
- Contacts are checked after the move and before the push-out.
- `StandardCollisionHandler` (`E/world/collisions/StandardCollisionHandler.kt` L24-57): dynamic against static only,
  rectangle overlap only, push-out along the axis with the smaller overlap, plus friction.
- Body bounds are always a `GameRectangle`, rotated by `direction.rotation`. Fixtures accept `GameRectangle`,
  `GameCircle`, `GameLine` and convex `GamePolygon`; overlap tests exist for all of them. Collision resolution looks
  only at body AABBs, never at fixture shapes.

### `MegaCollisionHandler` (`M/world/collisions/MegaCollisionHandler.kt`)

- `handleCollision` (L115-116) is `trySpecialCollission(...) || StandardCollisionHandler...`. A `true` from the
  special path skips the rectangle push-out.
- Special cases now: `collisionOn == false`, block filters, `PRESS_UP_FALL_THRU`, `COLLIDE_DOWN_ONLY` (one-way, per
  direction, snaps with `setY(static.maxY)`), `COLLIDE_UP_ONLY`.
- A slope fits here: a new `BodyLabel` (`M/world/body/BodyLabel.kt`), compute the surface height at the dynamic
  body's bottom-centre x, snap and return `true`.
- Limit: the handler runs only when the AABBs overlap. The down-slope stick (the body is above the surface after the
  move) needs its own probe after `resolveCollisions`.

### Grounded

- Megaman's `FEET` fixture is 0.5 × 0.25 PPM (16 × 8 px), centred on the body's bottom edge
  (`M/entities/megaman/components/BodyComponent.kt` L110-111, L352).
- A second "feet gravity" `CONSUMER` fixture (0.1 PPM high, filtered to `BLOCK`) gives `bodyOverGround`, which picks
  `groundGravity` (L164-186, L377). A slope must also satisfy it, or Megaman gets fall gravity on a slope.
- `FEET` with `BLOCK` goes to `FeetBlockRule` (`MegaContactRules.kt` L36,
  `M/world/contacts/rules/FeetBlockRule.kt`):
  - It casts the entity to `Block` (L21, L51). So a slope must be a `Block` subclass.
  - If the block's `collisionOn == false`, the block is removed from the feet blocks (L24-27). So a slope cannot use
    `collisionOn = false` to skip the push-out; use the label and the handler special case.
  - It adds the feet block, applies the block's position delta (stick to moving blocks), and sets
    `FEET_ON_GROUND`.
- `HEAD_BLOCK` and `BLOCK_SIDE` rules also fire against any `BLOCK` fixture.
- 56 of the 127 enemy files define a `FEET` fixture; 87 files in `core` read `FEET_ON_GROUND`.

### Blocks and Tiled

- `Block` (`M/entities/blocks/Block.kt`): a static body with one `BLOCK` rectangle fixture. `onSpawn` (L62-230)
  reads bounds, friction, gravity, labels, `ON`, filters and more. It ignores `POLYGON` and `LINES`.
- Layers `blocks` and `blocks_*` go to `SpawnersLayerBuilder` (`MegaMapLayerBuilders.kt` L28-50). An unnamed object
  becomes `Block`; a named object is resolved by reflection in `entities/blocks/`. A new `Slope` class is found by
  name.
- All Tiled object properties are copied to the spawn `Properties` (`M/utils/extensions/Extensions.kt` L105).
  Polygon and polyline objects give `POLYGON` and `LINES` keys, but no block reads them. In the TMX files, polygons
  are only in `children` and `decorations` layers.
- `GravityBlock` is a dynamic falling crate with its own `FEET` and an inner static `Block`, not a `Block` itself.
- Moving blocks (`Lift`, `DropperLift`, `PropellerPlatform`, `SwingingPlatform`) carry riders through
  `getPositionDelta` (`IBodyExtensions.kt` L67-71).

### Gravity direction

- `Direction` UP / LEFT / DOWN / RIGHT on `Body.direction`. Bounds and fixtures rotate around the centre.
- `WorldGravity` sets the direction on every player; `Megaman.direction` starts the camera rotation. Gravity changes
  come from `BodyGravityChangeRule`. Item `058` (separate camera rotation) is an idea.
- Code with per-direction branches: Megaman's gravity and clamp (`BodyComponent.kt` L384-438),
  `MegaCollisionHandler` L53-97, `GravityBlock` L187-198.

### Level config

- `LevelDefinition` carries `type`, `tmxMapSource`, `screenOnCompletion`, `music`, mugshot, `levelOverride`.
  `LevelOverride` has `loadout`, `difficultyMode`, `lives`, `canSpawn`. No physics flag.
- `LevelType.EXTRA_LEVEL` is used by `GAUNTLET_1` and `GAUNTLET_2` (`LevelDefinition.kt` L151-185). So
  `EXTRA_LEVEL` alone does not mean "X6-style level".
- Existing per-level checks: `type == MARIO_LEVEL` (`JumpBehavior.kt` L114, `Megaman.kt` L540, `BodyComponent.kt`
  L191); `== LevelDefinition.MOON_MAN`.
- TMX map-level properties read now: only `width`, `height`, `tilewidth`, `tileheight`
  (`E/tiledmap/TiledMapLoader.kt` L28-31).

### X6 slopes (`~/IdeaProjects/mmx-x6/src/main/java/mmx/x6/physics/`)

Facts only; no code is copied.

- Y points down, the grid is 16 px, the body bottom is the feet line.
- `TileRules`: codes 1-12 are slopes, 17-28 are the same shapes on ice and belts. The surface is stepped: one step
  per 4 px of x (codes 1-8, 4 px rise per 16 px) or per 2 px of x (codes 9-12, 8 px rise per 16 px). The shallow
  slope is a chain of 4 cells, the steep slope a chain of 2. Horizontal resolution ignores slopes: a slope is never
  a wall.
- `TerrainMover.move()`: X first (move, then wall push-out with 3 side probe points), then Y. The floor is resolved
  only when `dy >= 0`. While rising, only the ceiling is checked, so there is no snap during a jump up.
- `floorAt`: only the cell under the bottom-centre point. A fill cell (16) means "check the cell above". If grounded,
  or the feet are at or below the surface, feet = surface (snap up or down). If airborne and above, no floor.
- Flat floors: probes at left+1, centre, right-1. One-way cells count only if the previous bottom was at or above
  the cell top.
- `resolveFloor` stick: if no floor is found and the body was grounded, move 16 px down and run `floorAt` again.
  Accept a slope, or a flat drop of at most 8 px (`MAX_FLAT_STEP_DOWN`). Else restore Y; the body is airborne.
- `onFloor()`: a 1 px probe down for the grounded test. `embedded()` never counts slopes.
