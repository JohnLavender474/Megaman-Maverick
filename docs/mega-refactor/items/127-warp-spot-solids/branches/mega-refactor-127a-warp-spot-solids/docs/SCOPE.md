# Scope: mega-refactor-127a-warp-spot-solids

- Status: approved
- Approved: 2026-11-07

## Goal

The view edge warp (`WARP` rule, `047g`; the co-op bottom default since `047j`) lands the held player only on a safe
spot. It tries the side that the partner faces, then the other side, then the partner's own spot. While no spot is safe
or the partner is in the air, the warp waits, up to a cap. After the cap, the player lands on the partner's spot.

## Item

`127` (View edge warp: test the warp spot for solids). Decisions of 2026-11-06, copied from `ITEM.md`:

- Order of tries: the side the partner faces, then the other side, then the partner's own spot (two players do not
  collide). Each spot must be fully inside the view.
- Safe: no solid that blocks the player (the collision handler's block filter test; one-way blocks and slopes do not
  block; `CameraEdgeBlock` does), no `DEATH` fixture, no enemy `DAMAGER` that may hurt the player.
- Wait: while no spot is safe, the warp waits, up to a cap; after the cap, the player lands on the partner's spot.
- A spot in the air: the warp happens only while the partner stands (on the ground, on a ladder or in water).
- The soft lock "slot 0 held, slot 1 dies" was fixed in `047j`.

Open questions of the item, asked in the brief: the cap length, when facing is read, the guide color while the warp
waits, what counts as an enemy. Also the merge target (first branch of the item).

## Current state

`master` at `122aa53bb`.

- `CameraViewEdges.kt:231-260` `updateHolds`: a held player is frozen at its hold position; each frame
  `findWarpSpot` runs; after `rules.delay` and when a spot exists, the player is released, set to the spot center and
  takes the view edge damage.
- `CameraViewEdges.kt:269-280` `findWarpSpot`: the first live, free partner in view; spot = partner center ± 1 tile on
  x, on the side of the view center. No solid test (the `TODO` at `:267`).
- `CameraViewEdges.kt:112-122` `drawWarpGuides`: a white line and a circle at the spot.
- `CameraViewEdges.kt:198-211` `hold`: locks movement, behaviors and damage; gravity and collision off.
- Query: `levelState.worldContainer` (`LevelState.kt:17`), `IWorldContainer.forEachFixture` and `forEachBody` take grid
  cells (1 cell = `PPM`, `SimpleGridWorldContainer.kt`). Rebuilt after each world step.
- Block test: `MegaCollisionHandler.kt:40-47` (block filters of both bodies). `COLLIDE_DOWN_ONLY` and `SLOPE` labels
  (`BodyLabel.kt:8,11`) do not block a body that lands in them.
- Hazard: `FixtureType.DEATH`. Enemy: a `DAMAGER` fixture whose entity is an `IDamagerEntity` and passes
  `playerAllowsDamageFrom` (`PlayerRules.kt:34`). `canBeDamagedBy` cannot be used: the hold locks damage.
- Stand: `BodySense.FEET_ON_GROUND`, `BodySense.IN_WATER`, `BehaviorType.CLIMBING` (all generic, no character test).
- Facing: `IGameEntity.facing` (`EntityQueries.kt:21`).
- Wiring: `MegaLevelScreen.kt:342-350` builds `CameraViewEdges`.
- Tests: `SharedViewTest.kt` (`EdgesRig`, `TallRig`).

## In scope

- A new class `WarpSpotTest` (camera package): `isSafe(player, centerX, centerY): Boolean`, with the world container
  supplier. First caller: `CameraViewEdges.findWarpSpot`. Its query lambdas are fields (`O5-R1`).
- `CameraViewEdges`: the order of tries, the stand test, the cap and its timer, the guide color while it waits.
- `ViewEdgeRules`: one new field for the cap, with a dev flag key in `--viewEdges` (as `delay`).
- `MegaLevelScreen`: pass the world container supplier.
- Unit tests in `SharedViewTest.kt` (or a new `WarpSpotTestTest.kt`) with a real `SimpleGridWorldContainer`.
- A test map `RefactorTest127.tmx` and `LevelDefinition.REFACTOR_TEST_127` with rooms for the manual checks.

## Out of scope

- Side edges `WARP` (still experimental).
- The float-back sprite and bubbles (`TODO` at `CameraViewEdges.kt:195`).
- `135` (held player left behind at a side exit), `136` (spike skip in tall rooms), `130` (camera consolidation),
  `047i` (per-player transition).
- A margin around enemies (unless the owner asks for it in the brief).

## Affected areas and risks

- `CameraViewEdges.kt`, `ViewEdgeRules` (`state/SessionRules.kt`), the `--viewEdges` parser
  (`LaunchOptionsResolver`), `MegaLevelScreen.kt`, `LevelDefinition.kt`, a new TMX.
- Risk: a spot that is safe now but a moving block arrives next frame. Accepted: the test runs on the frame of the warp.
- Risk: a long wait while the partner jumps a lot; the cap bounds it.
- Risk: a blocked check that misses an odd block (a block whose filter passes only some states). The test uses the
  same filters as the collision handler.

## Overlap with other branches

- `081d` (X dash and sabre): X files only. No overlap.
- `compactor-level`: not a refactor branch, level content. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | the test reads the container that the world step rebuilt, in the camera effects, as today |
| `O1-R4` Typed state | no effect | the cap is a typed field of `ViewEdgeRules` |
| `O1-R5` Code with a caller | no effect | `WarpSpotTest` has its caller in `CameraViewEdges` |
| `O2-R4` Stable order | no effect | players in slot order; the container cells are ordered sets |
| `O2-R5` No single-player logic | helps | the partner is any free slot |
| `O2-R6` No single-view logic | waiting | the spot must be in the one view, as today (after `094`) |
| `O2-R7` Simulation apart from presentation | no effect | the guide color is drawn only; the wait is simulation state |
| `O2-R10` Freezes are simulation state | no effect | the wait timer runs on the update delta |
| `O5-R1` No per-frame allocation | no effect | fields for lambdas and rectangles |
| `O5-R2` Measure hot paths | no effect | the query runs only while a player is held, for a few cells |
| `O6-R2` Named checks | helps | unit tests and manual checks for each case |
| `O8-R2` Capabilities, not character checks | no effect | stand reads senses and behaviors |

## Open questions

See the brief.

## Decisions (owner, 2026-11-07, at the brief)

- D1 cap: 3 s after the delay (`ViewEdgeRules.warpCap`, dev key `cap` in `--viewEdges`).
- D2 guide: gray line and circle at the best try while the warp waits; white when ready.
- D3 enemies: a hurtful `DAMAGER` overlap only, no margin.
- D4 merge target: directly into `master`.
- Facing is read every frame (no objection at the brief).

## Decisions (owner, 2026-11-09, after the first play test)

- Pit: a side spot needs ground under the feet: a solid that blocks the player (one-way tops count) less than 1 tile
  below, or water at the spot. The partner's own spot needs no ground test.
- Crevice: the cap lands on the partner's spot only if no solid blocks it there; else the warp waits on, with the gray
  guide, until the partner leaves (for example a ground slide out of a 1-tile crevice).
- Teleporter: a `TELEPORTER` fixture makes a spot unsafe, as a `DEATH` fixture does.
