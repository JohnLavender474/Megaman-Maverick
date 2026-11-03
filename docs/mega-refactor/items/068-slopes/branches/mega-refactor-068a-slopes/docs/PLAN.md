# Plan: mega-refactor-068a-slopes

- Status: approved
- Approved: 2026-11-03

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`.

## New APIs and their first callers

| API | First production caller |
|-----|-------------------------|
| `BodyLabel.SLOPE` | `GroundSlope.onSpawn`; read by `MegaCollisionHandler`, `BlockSideRule`, `HeadBlockRule` |
| `GroundSlope` (`M/entities/blocks/GroundSlope.kt`) | `SpawnersLayerBuilder`, by name from a `GroundSlope` map object |
| `GroundSlope.surfaceAt(x: Float): Float` | `MegaCollisionHandler` |
| `GroundSlope.highOnRight` (read-only) | `MegaCollisionHandler` (the high-end skip, step 4) |
| `InvalidSlopeLabelException` (`M/world/collisions/`) | `MegaCollisionHandler`, when a body with `BodyLabel.SLOPE` is not a `GroundSlope` |
| `GroundSlope.hitByFeet` (override of the existing `Block` hook) | `FeetBlockRule`, on each feet contact (step 6b) |
| `LevelDefinition.REFACTOR_TEST_068` | `--level REFACTOR_TEST_068` |

No other new type, function or field. No new `ConstKeys` value: the high side is the existing `ConstKeys.SIDE`
(`side`) with `left` or `right`.

## The surface (X6 facts)

Source: `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, "Slope profiles"; `mmx-x6`
`core/src/main/java/mmx/x6/physics/TileRules.java`, `slopeSurface`. Facts only, no code copied.

- A run of width `W` and height `H` has the ratio `r = H / W`: 1/4 (shallow) or 1/2 (steep). Any other ratio
  (with a small tolerance) logs an error, and the slope uses the nearer of the two.
- The step width `s` is 1/4 tile (shallow) or 1/8 tile (steep). Each step rises `s * r`: 1/16 tile for both.
- The step index `k` counts from the low end: `k = floor(d / s)`, where `d` is the distance in x from the low end.
  The surface is `y + min(H, (k + 1) * s * r)`. So the first step is one step above the low floor, and the last step
  is level with the top, as in X6.

## Steps

### 1. Test map and level entry

- Changes:
  - A subagent builds `assets/tiled_maps/tmx/RefactorTest068.tmx` with the `level-builder` skill. One room for each
    contract check: `shallow`, `steep`, `chain`, `high_end`, `jump_onto`, `slide`, `walls`, `shoot`, `enemy`. Each
    room has a player spawn, a `tiles` layer with a tile on each block cell, and `GroundSlope` rectangle objects in a
    `blocks_slopes` layer with `side` set. The low end of each run is at the height of the floor next to it, and the high
    end touches a flat block at the height of the top (except where the room needs a wall). The `enemy` room has two
    `Spiky`.
  - `LevelDefinition.REFACTOR_TEST_068` (`TEST_LEVEL`) after `REFACTOR_TEST_047`, and its line in
    `T/levels/LevelCompletionScreensTest.kt` after the `REFACTOR_TEST_047` line.
- Check: `./gradlew core:test` passes. A `timeout 12` launch of each room on the baseline code loads with no
  exception. The `GroundSlope` objects log "unknown entity" and are skipped (no class yet). The slope tiles show.
- Done: yes (2026-11-03)

### 2. Baseline measurement (dropped)

- Dropped by the owner on 2026-11-03: no baseline in a stage with no slopes. Step 6 has a sanity run in the slope test
  map instead (`NOTES.md`, "Decisions").
- Done: dropped

### 3. `BodyLabel.SLOPE` and the `GroundSlope` block

- Changes:
  - `BodyLabel.SLOPE` in `M/world/body/BodyLabel.kt`.
  - `GroundSlope : Block` with `TAG = "GroundSlope"`:
    - `defineBodyComponent()` calls `super`, then sets a triangle `GamePolygon` as the shape of `blockFixture`, and
      replaces the `block_fixture` pre-process entry of `Block` with one that only sets the debug color (the
      rectangle sync is not needed).
    - `onSpawn` calls `super`, then reads `side` and the ratio from the bounds, sets the triangle vertices from the
      bounds, and adds `BodyLabel.SLOPE` (after `super`, because `Block.onSpawn` clears the labels).
    - `surfaceAt(x)` as in "The surface". It clamps `x` to the run.
  - `T/entities/blocks/GroundSlopeSurfaceTest.kt`: both ratios and both sides, each step edge, the run ends, `x` outside
    the run.
- Check: `./gradlew core:test core:smokeTest` passes. A launch of `REFACTOR_TEST_068 --room shallow` with
  `--debugShapes` shows the triangles where the tiles are.
- Done: yes (2026-11-03)

### 3b. Authoring guidance

- Changes: a "Slopes" section in the `level-builder` skill (`.claude/skills/level-builder/SKILL.md`) and in
  `utils/tiled_extensions/README.md`: the `blocks_slopes*` layer, the `GroundSlope` name and the `side` property,
  the two ratios, the low end at the height of the next floor, the high end against a flat block at the top height,
  and "keep slope layers visible when you save".
- Check: the test map of step 1 follows the guidance.
- Done: yes (2026-11-03)

### 4. Slope case in `MegaCollisionHandler`

- Changes, in `trySpecialCollission`, after the block filters (so filters still apply to slopes):
  - Against a `SLOPE` body, if the dynamic body has `Direction.UP` (the same `IDirectional` read as the one-way
    case): with `cx` the bottom-centre x of the dynamic body, return `true` if `cx` is outside the run or
    `velocity.y > 0`. Else, with `surface = slope.surfaceAt(cx)`: if the bottom is below `surface`, or the body's
    feet blocks hold this slope, set the bottom to `surface` and add the slope's x friction, as the one-way case does.
    Return `true`. With another direction, return `false` (the rectangle push-out).
  - The high-end skip, just before the final `return false` (so the one-way branches do not change): if the dynamic
    body has `Direction.UP` and a feet-blocks property (read with `hasProperty` first, so no set is made for a body
    with no feet), and a `GroundSlope` in its feet blocks holds `cx` in its run, and the static body's top equals that
    slope's top (within a small tolerance) and the static body touches the slope's high-side edge: return `true`.
  - No allocation: one reused `Vector2` or plain float math; the feet-blocks set is iterated with no new iterator.
  - `T/world/collisions/GroundSlopeCollisionTest.kt` with real `Body` objects and `GroundSlope` instances from
    `testServices()`: the cases in the contract.
- Check: `./gradlew core:test` passes, also the existing `MegaCollisionHandlerTest`. Rooms `shallow`, `steep`,
  `chain`, `high_end`, `jump_onto` and `slide` play as the contract says (short launches by me, then the owner's play
  test in step 7).
- Done: yes (2026-11-03)

### 5. Side and head rules skip slopes

- Changes: `BlockSideRule` and `HeadBlockRule` return early in begin, continue and end when the block body has
  `BodyLabel.SLOPE`. The skip in `end` matters: the current `end` clears the side or head sense with no test, so an
  end from a slope would clear the sense that a real wall set.
- Check: `./gradlew core:test` passes; `ContactRuleSelectionTest` passes with `contact-rule-selection.txt`
  unchanged. Rooms `walls` and `enemy` play as the contract says.
- Done: yes (2026-11-03)

### 6. Automated checks and diagnostics sanity run

- Changes: none, unless a check fails.
- Check: all automated checks of `CONTRACT.md`, including `git diff master --stat -- engine/` empty. One
  `--diagnostics` run in `REFACTOR_TEST_068` `shallow` (no baseline): the `resolveCollisions` and `processContacts`
  entries show no outlier. The result is in the process log.
- Done: yes (2026-11-03)

### 6b. Play-test fixes from the diagnostics run

- Found by the owner in the `shallow` run: a jump pose and a land sound when a slide goes on or off a slope, and shots
  that hit the slope inside the drawn band.
- Causes (reproduced headless in `GroundSlopeWalkTest`): at the low end the first step is one step above the floor,
  so the body hangs there and the feet-gravity fixture loses the ground; a slide has no gravity, so from the plateau
  the body rests exactly on the top of the slope bounds, and the world calls the collision handler only on overlap.
  The shots hit the true triangle; the tiles fill the whole box.
- Changes:
  - `GroundSlope.hitByFeet`: the `C8` stick and the low-end step-down. It only moves a body down, with no friction,
    as a flat floor gives no friction to a body that rests on it.
  - `MegaCollisionHandler`: the slope case keeps the push-up (with friction), the pass-through and the high-end skip;
    its feet stick goes (one home for the stick).
  - `GroundSlope` draws a filled triangle (`C9`); `RefactorTest068.tmx` loses its slope tiles; the `level-builder`
    skill no longer says to paint slope cells.
  - Tests: `GroundSlopeFeetStickTest`, `GroundSlopeWalkTest`; the handler stick test moves to the new test.
- Check: `./gradlew core:test` passes; each new test fails with the stick turned off. Short launches of `shallow` and
  `slide`.
- Done: yes (2026-11-03)

### 7. Owner play test

- Changes: fixes from the play test. A fix after an experimental check means the control checks run again.
- Check: all manual checks of `CONTRACT.md` pass. The result is in the process log.
- Done: yes (2026-11-03)
