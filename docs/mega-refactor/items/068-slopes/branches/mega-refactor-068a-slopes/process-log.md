# Process log: mega-refactor-068a-slopes

## Status

- State: merged (2026-11-03)
- Goal: X6-style floor slopes (`GroundSlope` block) for every body with a `FEET` fixture; main levels unchanged
- Done: branch start, scan, `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` (all approved 2026-11-03), plan steps 1, 3, 3b, 4, 5, 6, 6b and 7
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-03

- Branch start on `master`. Merge target: `master` directly (item `068` decision, 2026-10-29). The dependency on the
  `016` parent merge is met (`016` done 2026-10-30).
- Scan done. Findings in `docs/SCOPE.md` (S1 to S5) and overlaps in `docs/NOTES.md`. Two findings change the
  proposed scope: the stick fits in the collision handler with no engine step (S1), and the flat block at the high end
  of a run fights the slope (S2).
- Draft `docs/SCOPE.md` written. Not approved.
- Owner approved `docs/SCOPE.md` with the proposed answers to the 4 open questions. Decisions in `docs/NOTES.md`; the
  flat step-down is a backlog candidate in `docs/TODO.md`.
- Draft `docs/CONTRACT.md` written: 9 test rooms in `RefactorTest068.tmx`, 5 control checks in real and test levels.
- Owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written: 7 steps. The X6 surface facts and their sources are in
  the plan.
- Owner question on rectangle versus polygon objects; rectangles kept. Owner scope change, approved: the class is
  `GroundSlope`, and slopes go in `blocks_slopes*` layers. Tile-object tooling and a dedicated layer builder were
  rejected (`docs/NOTES.md`). `SCOPE.md`, `CONTRACT.md` and `PLAN.md` updated (new plan step 3b: authoring guidance).
- Owner approved `docs/PLAN.md`. Step 1 started: `REFACTOR_TEST_068` added to `LevelDefinition` after
  `REFACTOR_TEST_047`, with its `LevelCompletionScreensTest` line. A subagent builds `RefactorTest068.tmx` with the
  `level-builder` skill.
- Step 1 done. `RefactorTest068.tmx` (subagent, `level-builder` skill): 9 rooms stacked in one column, 22
  `GroundSlope` objects in `blocks_slopes`, block tile 175 and slope tile 307 (local 306) in `tiles`. The CLI cannot
  write named objects with properties in a custom layer or paint tiles, so a script did those parts.
  - Read-back script: all ratios 1/4 or 1/2, every low end on a floor at the slope bottom, every high end against a
    block at the slope top (except the floating slope in `jump_onto`), spawns on plain blocks.
  - `LevelCompletionScreensTest` passes (3 tests). Kotest needs the full class name for `--tests`; a `*` pattern finds
    no tests.
  - `lwjgl3:jar` builds. One 12 s launch per room on the baseline code: all 9 exit 124, each with 22 caught
    `ClassNotFound` errors for `GroundSlope` (expected: no class yet) and no other error.
  - Each launch prints the LibGDX `TiledMapLoader` warning "There is at least one property of type class...". The map
    has no `class` or `type` attribute, and neither do `Tileset1.tsx`, `RefactorTest051.tmx` or `IntroStage_v3.tmx`.
    Not caused by this map; not investigated further.
- Owner dropped plan step 2 (baseline in `INTRO_STAGE`): only a `--diagnostics` sanity run in the slope test map in
  step 6. Accepted gap of `O5-R2`, recorded in `docs/NOTES.md` and the `SCOPE.md` objective check. `CONTRACT.md` and
  `PLAN.md` updated; `K6` removed.
- Step 3 done. `BodyLabel.SLOPE`; `GroundSlope` (`entities/blocks/GroundSlope.kt`): a triangle `GamePolygon` as the
  `BLOCK` fixture shape (local vertices from (0, 0), so the fixture centring puts it on the body bounds), `side` and
  the ratio read in `onSpawn`, `surfaceAt(x)`. `highOnRight` added to the plan API table (step 4 reads it).
  - `GroundSlopeSurfaceTest`: 11 tests pass. With `(step + 1)` changed to `step`, 8 tests fail, so the tests can fail.
  - `core:test` and `core:smokeTest` pass. `lwjgl3:jar` builds.
  - 12 s launch of `REFACTOR_TEST_068 --room shallow --debugShapes`: exit 124, no error (the 22 class errors are gone).
    The triangles in the window are for the owner to see; I cannot see the window.
- Step 3b done. "Slopes" section in the `level-builder` skill (under Iteration 2) and in
  `utils/tiled_extensions/README.md`. `RefactorTest068.tmx` follows it: `blocks_slopes` layer, `GroundSlope` name,
  `side`, ratios 1/4 and 1/2, the end heights (step 1 read-back).
- Step 4 done. `MegaCollisionHandler`: the slope case after the block filters (snap, stick with the feet on the slope,
  no snap while rising or outside the run, never the rectangle push-out; other gravity directions get the push-out),
  and the high-end skip before the final `return false`. `GroundSlopeCollisionTest`: 11 tests pass.
  - Can-fail checks: with the stick, the rising check and the high-end skip broken, exactly those 3 tests fail. With a
    safe `as?` cast, the label test fails.
  - Owner design review (see `docs/NOTES.md`, "Decisions"): the `GroundSlope` subclass stays; fail fast with the new
    `InvalidSlopeLabelException`; both entities read once at the top of `trySpecialCollission`.
  - The first run of the exception test failed in the test setup: a relaxed MockK `Block` returned a mock component for
    `getComponent(MapObjectComponent::class)`, so `mapObjectId` threw while the message was built. Fixed by a stub in
    the test.
  - `core:test` and `core:smokeTest` pass. 12 s launches with a fresh jar: `shallow`, `steep`, `chain`, `high_end`,
    `jump_onto`, `slide`, then `INTRO_STAGE room1` and `shallow` again after the early entity reads: all exit 124, no
    error.
- On `master`: the root `CLAUDE.md` now has the fail-fast rule under "Conventions" (`b176f72b5`, owner request). It names
  `InvalidSlopeLabelException`, which reaches `master` with this branch.
- Small QoL docs line (no gameplay change): `core/src/test/CLAUDE.md` notes that a relaxed MockK entity returns a mock
  from `getComponent`, so `mapObjectId` throws on it; stub the call.
- Step 5 done. `BlockSideRule` and `HeadBlockRule` return early for a block body with `BodyLabel.SLOPE`, in begin,
  continue and end. `BlockSideRule.onEnd` now passes the block fixture too (it passed only the side fixture).
  - `GroundSlopeContactRulesTest`: 6 tests pass (slope skip, wall and ceiling controls, an end from a slope keeps the
    sense that a wall or ceiling set). With the slope checks turned off, exactly the 4 slope tests fail.
  - The test entities must be `IBodyEntity` mocks that return the body: `IFixture.getBody()` reads the body through the
    entity.
  - `core:test`, `core:smokeTest` and `lwjgl3:jar` pass. 12 s launches of `walls` and `enemy`: exit 124, no error.
  - `contact-rule-selection.txt` is unchanged; `ContactRuleSelectionTest` passes.
- Step 6 done. `engine:test`, `core:test`, `core:smokeTest` and `lwjgl3:jar` pass; `git diff master --stat -- engine/`
  is empty; `contact-rule-selection.txt` is unchanged.
  - Diagnostics sanity run by the owner (`./run-game-debug.sh --level REFACTOR_TEST_068 --room shallow`, capture
    `1793724263962-diagnostics.txt`, 3,349 frames): frame time p50 1.08 ms, p95 2.34 ms; the p99 and the maximum are
    the load (`postCreate`, `assMan.update`). `resolveCollisions` mean 0.02 ms, p99 0.06 ms; `processContacts` mean
    0.05 ms, p99 0.17 ms. No outlier.
- Owner findings in the diagnostics run: a jump pose and a land sound when Megaman slides on or off a slope; shots hit
  the slope inside the drawn band; the slope tiles are boxes, not slopes. Plan step 6b added (owner approval of the
  gates and of triangles in all rooms, 2026-11-03).
- Step 6b done.
  - Reproduced headless first (`GroundSlopeWalkTest`, a real `WorldSystem` with a Megaman-sized body): all 4 walk and
    slide runs failed. Causes: at the low end the body hangs one step (1/16 tile) above the floor; on a slide (no
    gravity) from the plateau the body rests exactly on the top of the slope bounds, so the handler never runs. The
    shots hit the true triangle; the tiles filled the box.
  - `GroundSlope.hitByFeet` holds the stick and the low-end step-down (down only, no friction). The handler keeps the
    push-up with friction and loses its feet stick. `GroundSlope` draws a filled triangle; the 236 slope tile cells
    are gone from `RefactorTest068.tmx`; the `level-builder` skill says not to paint slope cells.
  - New tests: `GroundSlopeFeetStickTest` (9), `GroundSlopeWalkTest` (7). `core:test`: 838 tests pass. Break checks:
    stick off fails the 4 walk and slide runs and 2 stick tests; no gravity and `vy` gates fail the knockback and
    hover runs and 2 gate tests; no `CONTINUE` and direction gates fail the 2 matching gate tests.
  - `core:smokeTest` and `lwjgl3:jar` pass; engine diff empty. 12 s launches of `shallow` and `slide`: exit 124, no
    error.
- Play test (step 7), `jump_onto`: a jump or an air dash up through the floating slope snaps Megaman onto the surface
  at the top of the jump (at once in a dash). Owner: accepted, no level will need it; a `TODO` at `GroundSlope` and a
  backlog candidate in `docs/TODO.md`. No code change.
- Step 7 done. Owner play test: all experimental checks (`shallow`, `steep`, `chain`, `high_end`, `jump_onto`,
  `slide`, `walls`, `shoot`, `enemy` with a knockback on a slope), all control checks (`INTRO_STAGE` `room1` and
  `room3`, `TEST_1` `room9` and `room12`, `WILY_STAGE_1` `room2`), death and respawn, and pause and resume on a slope
  pass. The one finding (the snap after a jump or dash up through a slope) is accepted, see the entry above.
