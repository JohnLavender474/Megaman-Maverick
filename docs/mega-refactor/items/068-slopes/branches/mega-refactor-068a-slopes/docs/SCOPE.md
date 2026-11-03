# Scope: mega-refactor-068a-slopes

- Status: approved
- Approved: 2026-11-03; scope change (name, slope layers) approved 2026-11-03

## Goal

Add X6-style floor slopes. A `GroundSlope` object in a `blocks_slopes` layer of a map makes a straight, stepped floor run. Every
body that the world resolves against blocks stands, walks and runs on it, Megaman included. Maps with no `GroundSlope`
object play exactly as now.

## Item

Item `068` (`Objectives: O8, O4`). The branch takes these parts of `ITEM.md`:

- Goal: "Support X6-style floor slopes for all entities, not only X, so that "extra" levels built from X6 stages can
  use them. [...] slopes must not block Megaman. The main levels must not get slopes [...] and they must play exactly
  as now. Slopes are a feature that a level turns on."
- Decisions: the surface is stepped, as in X6 (one step per 4 X6 px of x on the shallow slope, per 2 X6 px on the
  steep slope). All slope code is in core. No level flag. X (`081`) is not a prerequisite; the branch is play-tested
  with Megaman. The branch squash-merges into `master`. The branch adds `RefactorTest068.tmx` (`REFACTOR_TEST_068`),
  one room per check. The intro-stage section is not part of `068a`.
- Proposed scope: a `Slope` entity in `entities/blocks/` that extends `Block`; the surface function and the snap as
  a special case in `MegaCollisionHandler`, keyed on a new body label; Tiled authoring as a rectangle object named
  `Slope`; slopes for every body with a `FEET` fixture; `Direction.UP` gravity only; no ice, belt or moving slopes.
- Proposed scope, changed by this scan (see "Open questions" 1 and 2): "one generic step after `resolveCollisions`
  in `WorldSystem`" and "the X6 flat step-down (up to 8 px) [...] when a body leaves a slope".

## Current state

`master` at `574cdbbf7`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/`. The facts in the item `NOTES.md` (2026-10-28) are still true. New
findings:

### S1: the collision handler already runs for a body above the slope surface

- `WorldSystem.resolveCollisions` (`E/world/WorldSystem.kt:255-275`) calls the handler for each static body whose
  bounds overlap the dynamic body's bounds.
- A slope's bounds are the bounding box of the run. A body whose bottom is above the surface but below the top of the
  box overlaps the box. So the handler runs for it, and a snap down (the "stick") can happen in the handler.
- On a flat floor, Megaman sinks into the floor by a very small distance in each sub-step (`GROUND_GRAVITY = -0.15`,
  `MegamanValues.kt:41`; the small-velocity cut at `M/entities/megaman/components/BodyComponent.kt:285`), and the
  push-out moves him back. So a body that walks from the flat top onto the slope overlaps the slope box in the first
  sub-step, also at full speed.
- The handler does not run only when the body leaves the slope box: at its low end (the floor there is at the same
  height, so nothing is needed) or over an edge with a drop below it. Only the X6 flat step-down needs a probe there.

### S2: the flat block at the high end fights the slope

- At the high end of a run, the flat floor block touches the slope. When the body's bottom-centre is over the slope,
  part of the body is still over the flat block and below its top.
- `StandardCollisionHandler` (`E/world/collisions/StandardCollisionHandler.kt:30-43`) pushes the body up to the flat
  top while the slope snaps it down to its surface. The result depends on the order of the candidates in the grid.
- Megaman is 1 tile wide (`BodyComponent.kt:44`). At a distance `d` of the bottom-centre into the slope, the overlap
  with the flat block is `0.5 tile - d` wide and `d * rise` high. When `d` is more than 0.4 tile (shallow) or 0.33
  tile (steep), the overlap is higher than wide, so the push is horizontal: the flat block pushes Megaman back.
- X6 has no such fight: the floor is only the cell under the bottom-centre point, and that point decides
  (`NOTES.md`, "X6 slopes", `floorAt`).
- The feet blocks of the body (`M/world/body/IBodyExtensions.kt:99-107`) are set in `processContacts`, which runs
  before `resolveCollisions` in the same sub-step (`WorldSystem.kt:195-201`). So the handler can know, with no new
  state, that a body's feet touch a slope under its bottom-centre.

### S3: the block fixture is a rectangle, so every fixture rule sees the empty triangle

- `Block.defineBodyComponent` (`M/entities/blocks/Block.kt:267-282`) makes one `BLOCK` rectangle fixture the size of
  the body. It is `protected open`, so a subclass can make a different shape.
- With a rectangle, all `BLOCK` contact rules fire in the empty part above the surface: feet (`FEET_ON_GROUND` in
  mid air), feet gravity (`bodyOverGround`, `BodyComponent.kt:145-169`), head, side, body, projectiles
  (`MegaContactRules.kt:63-65`), lasers (`:73`) and explosions (`:35`).
- `GamePolygon` overlaps rectangles, circles, lines and convex polygons (`E/common/shapes/GamePolygon.kt:154-175`),
  with no allocation after the first call. A triangle `BLOCK` fixture makes all these rules see the real floor, up to
  one step of difference from the stepped surface.

### S4: side and head rules

- `BlockSideRule` (`M/world/contacts/rules/BlockSideRule.kt`) sets `SIDE_TOUCHING_BLOCK_LEFT` or `_RIGHT` for any
  `BLOCK` contact. Megaman's side fixtures go down to near his feet (`BodyComponent.kt:190-200`), so on a slope they
  touch the triangle: wall slide, and walking enemies that turn at walls turn on the slope.
- `HeadBlockRule` (`M/world/contacts/rules/HeadBlockRule.kt`) stops the y velocity for any `BLOCK` contact, except a
  `COLLIDE_DOWN_ONLY` body. X6 never uses a slope as a wall or a ceiling.

### S5: other facts

- The x speed: the snap changes only y, so the run and slide speed on a slope is the same as on a flat floor (item
  open question 5).
- 56 enemy files and 94 entity files in total define a `FEET` fixture.
- `LevelDefinition.kt:157-168`: `REFACTOR_TEST_051` and `REFACTOR_TEST_047` are the last test entries.
  `LevelCompletionScreensTest.kt:36-37` lists them.

## In scope

- `GroundSlope` (`M/entities/blocks/GroundSlope.kt`), a subclass of `Block`:
  - The name: "Ground" says that it is a floor only. A later `CeilingSlope` can sit next to it.
  - Tiled: a plain rectangle object named `GroundSlope` in a layer named `blocks_slopes` or `blocks_slopes_<n>`. These
    names already go to the blocks builder (`MegaMapLayerBuilders.kt:29-32`), so no loader code changes. A dedicated
    layer lets the author see the slopes apart from the other blocks by a toggle of layer visibility. The bounds are
    one straight run. The `side` property gives the high side (`left` or `right`). The ratio of height to width gives the steepness, 1/4 (shallow) or 1/2
    (steep). Any other ratio logs an error.
  - A triangle `GamePolygon` `BLOCK` fixture (S3).
  - The stepped surface height at an x, in world units: the shallow step is 1/4 tile wide, the steep step 1/8 tile
    (item open question 1, proposed answer).
  - The new body label `SLOPE` on its body.
- `MegaCollisionHandler`, for `Direction.UP` bodies:
  - Against a `SLOPE` body: if the bottom-centre x is over the run and the body does not rise (`velocity.y <= 0`), and
    the bottom is below the surface, or above it while the feet touch the slope, set the bottom to the surface.
    Return `true` in all cases, so the rectangle push-out never runs against a slope.
  - Against another block: skip the push-out when the body's feet touch a slope whose run holds the body's
    bottom-centre x and the block top is not above the slope top (S2).
- Authoring guidance in the `level-builder` skill and in `utils/tiled_extensions/README.md`: slopes go in
  `blocks_slopes*` layers, each object is named `GroundSlope` (an object with no name spawns as a plain `Block`, a
  full box), and every slope layer is visible when the map is saved (the game builds only visible layers,
  `MegaMapLayerBuilders.kt:59`).
- `BlockSideRule` and `HeadBlockRule` skip a `SLOPE` body (item open question 2, proposed answer).
- A body with other gravity directions: the slope acts as a normal rectangle block (the standard push-out).
- Unit tests of the surface function and of the handler cases (core tests, with real `Body` objects).
- The test map `RefactorTest068.tmx` and `LevelDefinition.REFACTOR_TEST_068`, after `REFACTOR_TEST_047`, so that
  the entry does not conflict with `X6_INTRO` in `x6-intro-extra-level`. Rooms: both steepnesses, both directions, a
  run down at full speed, a jump onto a slope from above, a ground slide on a slope, a slope next to a flat floor,
  one walking enemy on a slope.
- One `--diagnostics` sanity run in the slope test map after the implementation, with no baseline (owner decision,
  2026-11-03).

## Out of scope

- The engine step after `resolveCollisions` and the X6 flat step-down (open questions 1 and 2).
- Ice and belt slopes, moving slopes, slopes for other gravity directions.
- A slope as a wall or a ceiling.
- The intro-stage section and the X6 converter.
- A full pass over the enemies on slopes (item open question 4): the branch checks Megaman and one walking enemy.
- Any change to the main maps.
- Tiled tooling for slopes: a template, a tile object or editor guides. A tile object would need a new
  `TiledMapTileMapObject` case in `convertToProps` and a tileset in each map; the owner chose plain rectangles
  (2026-11-03, `NOTES.md`).
- A dedicated `slopes` layer builder. A validator rule for the objects in `blocks_slopes*` layers is a backlog
  candidate for item `003`.

## Affected areas and risks

- `MegaCollisionHandler` runs for every dynamic-static overlap in every sub-step. The new checks add one label test
  per pair, and one walk over the feet blocks of a body when it has a slope. Risk: a change of flat-floor behavior if
  the S2 skip is too wide. The control checks in the contract cover it.
- `BlockSideRule` and `HeadBlockRule`: one label test more. No change for bodies with no `SLOPE` label.
- `Block`: no change in behavior; `GroundSlope` overrides `defineBodyComponent`.
- `LevelDefinition` and `LevelCompletionScreensTest`: one new entry each.
- `contact-rule-selection.txt` does not change: no new fixture-type pair.

## Overlap with other branches

- `mega-refactor-019c-merge-spawn-queues`, `mega-refactor-118b-batch-1`: no shared files.
- `x6-intro-extra-level` (not a refactor branch): it adds `X6_INTRO` at the end of `LevelDefinition` and of
  `LevelCompletionScreensTest`. The new entry goes after `REFACTOR_TEST_047` to avoid a conflict. Its map has 13
  `TODO` markers "replace steps with slope", the first real user of `GroundSlope`.
- `compactor-level`: it also adds an entry at the end of `LevelDefinition`. Same placement rule.
- Item `017` (planned) plans world-step hooks in `WorldSystem`. With open question 1 answered "no engine step",
  `068a` does not touch that area.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | waiting | `GroundSlope` reads only its spawn props. |
| `O1-R2` Components, not new inheritance | no effect | `GroundSlope` is a leaf class like the other `Block` subclasses, not a new shared layer. `FeetBlockRule` casts to `Block`. |
| `O1-R3` No new fragile order | helps | The S2 skip removes a result that depends on grid order. It uses the engine order "contacts before resolve", which exists now. |
| `O1-R4` Typed state | no effect | The label comes from code, through the existing label set. The feet blocks are existing state. The Tiled properties are map data. |
| `O1-R5` Code only with a production caller | helps | Open question 1 drops the engine step, which has no need in `068a`. |
| `O2-R4` Stable iteration order | no effect | No new collection is iterated in an order that matters. |
| `O2-R5` No logic assumes one player | no effect | The code is per body. |
| `O5-R1` No per-frame allocation | no effect | Pooled or field temporaries only. |
| `O5-R2` Measure hot paths | conflict (accepted gap) | The handler checks run for every block collision in every level, but the owner chose no before-and-after measurement (2026-11-03). Only a sanity run in the slope test map. At the merge, a row in the `O5` current state on `master`. |
| `O6-R1` CI passes | no effect | |
| `O6-R2` Named checks | helps | One test room per check. |
| `O7-R3` No level-identity checks | no effect | No level check. A map turns slopes on by its objects. |
| `O8-R2` Capabilities, not character checks | no effect | No `Megaman` check in the slope code. |
| `O8-R3` Name the modes | no effect | Extra levels only, as in `ITEM.md`. |
| `O4-R1` Mechanics from Tiled | helps | A `GroundSlope` object with properties. |
| `O4-R2` A test room for each mechanic | helps | `RefactorTest068.tmx`. |

## Open questions

None. The owner approved the proposed answers to questions 1 to 4 on 2026-11-03 (see `NOTES.md`, "Decisions"):
no engine step, no flat step-down, a triangle `BLOCK` fixture, and no change to the feet fixtures.
