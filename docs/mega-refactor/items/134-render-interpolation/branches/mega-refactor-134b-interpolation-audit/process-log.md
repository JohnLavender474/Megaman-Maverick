# Process log: mega-refactor-134b-interpolation-audit

## Status

- State: merged
- Goal: fix the known render interpolation hitches (bodies placed outside the world steps, bodiless sprites, the shape
  renderer, snaps at teleports and respawns)
- Done: branch start, scan (3 subagents), draft `SCOPE.md` and `CONTRACT.md`, brief, `SCOPE.md` and `CONTRACT.md` approved, `PLAN.md`, plan steps 1 to 6
- Next: none
- Blocked:
- Brief: https://claude.ai/artifact/9E3c77ggVAQ2NnKDQU1jBo (approved 2026-11-09)

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-09

- Branch start: folder from the templates, `ITEMS.md` row, worktree from `master` at `0957c567e`. The case list
  is in the `134` `ITEM.md`, section "`134b`".
- Scan: 3 read-only subagents (bosses and `special/`; the other entities; teleports, direct draws, shapes and the
  pause). Inventories in `temp/mega-refactor-134b-interpolation-audit/`. Claude checked the `GutsTankFist`,
  `PreciousGemCluster` and `PipePortal` claims in the code. New finding: bodies that move once per frame outside the
  steps (for example `D1000`) are drawn at the frame time, up to 1 step ahead of the camera (`SCOPE.md`, table C).
- Draft `SCOPE.md` and `CONTRACT.md`; brief published. Decisions D1 (where the link lives), D2 (shapes), D3 (riders
  and bodies that move once per frame).
- Owner answers at the brief: D1 the link on the sprites component, not on the body (not the recommended option;
  reason: the body is physics, draw state goes on the draw side; rule added to `docs/mega-refactor/CLAUDE.md` on
  `master`, `c49d23d09`, merged into the branch). So the snap is named in physics terms: `restartStep()` in place of
  `snapDraw()` (marked in the brief). D2 the camera offset only. D3 a new item (`docs/TODO.md`).
  `BigAssMaverickRobotBody` is back in scope, because the sprites link covers an entity with no body.
- `PLAN.md` written (8 steps).
- Step 1: a subagent added rooms `attach` (`FireMet`, `PetitDevil`, `Electrocutie` under a ceiling block, a
  `RocketPlatform` with a child `Spike`) and `pipe` (a `PipePortal` pair; enter pipe A with DOWN) to
  `RefactorTest134.tmx` with `level_builder`; the analyzer passes. No live map puts a `Spike` on a moving parent, so
  that combination is new (the child link uses the `Test1.tmx` togglee pattern; spike properties from
  `Gauntlet1_v2.tmx` id 293). The review must check it.
- Step 2: `SpritesComponent.drawOffsetBody`; `SpritesSystem` reads it before the own body. 4 new tests in
  `SpriteDrawOffsetTest`. A break of the link read made `engine:test` fail, as expected. First caller `GutsTankFist`
  (the tank while `ATTACHED`); the `GutsTank` block `TODO` now names the riders item (D3).
- Step 3: `IBody.restartStep()`; `PipePortal.pullEntityIn` calls it after the snap (not the exit, which runs outside
  the steps with the sprite hidden). 3 new tests in `WorldDrawOffsetsTest`; an empty `restartStep` made all 3 fail.
- Step 4: links in `ReactorManProjectile` (inactive with a `ReactorMan` owner), `PreciousWoman` shield gems (while
  `SPIN`), `PreciousGemCluster` (its gems: the player while the cluster follows the player and no gem is released,
  else the cluster body; the gem clears the link in `onDestroy`), `BigAssMaverickRobotHand` (the hand block while it
  exists, the robot in `FOLLOW`), `BigAssMaverickRobotBody` (new `OWNER` spawn property from the robot),
  `FireMetFlame` (until `launch`), `PetitDevilChild` (its parent), `Electrocutie` (sets the links of its children and
  clears them at destroy; `Bolt` reads no parent from these props), `Spike` (its parent while gravity is off),
  `MegamanXSabreSlash` (X's body). Not needed: the `CeilingCrusher` block (a plain `Block` with no sprite).
- Step 5: `MegaLevelScreen.draw(renderer)` moves the camera by the draw offset and puts it back. Finding: the game
  calls `shapeRenderer.begin` before `screen.draw(renderer)`, and a new projection matrix takes effect only at the
  next `begin`, so the shapes drew with the matrix of the frame before (also before this branch). The draw now ends
  and begins the renderer again after it sets the matrix. The `I` key does nothing while `clock.paused`.
- Step 6: `engine:test` (667), `core:test` (1252), `core:smokeTest` (451), `lwjgl3:jar` pass.
- Step 7, review before the play test (2 read-only subagents: the diff; the play checks and the test map). Findings
  and fixes:
  - High (both reviews): `PreciousWoman` wrote the gem link with `getComponent(...)!!` right after `gem.spawn()`.
    `spawn()` only queues, and a new pooled gem has no components until `init()`, so the first shield crashed. The
    same cause made the `Electrocutie` links silent no-ops (`?.`), and its horizontal bolts are new `Bolt(services)`.
    Fix: each child holds its link in its own sprites component. `PreciousGem` has a field `drawOffsetBody` that its
    owner sets (`PreciousWoman`, `PreciousGemCluster`) and that `onDestroy` clears; `ElectrocutieChild` and `Bolt` link
    to their `Electrocutie` parent themselves. `Electrocutie.kt` is back to `master`. `Bolt` read its parent from
    `ConstKeys.ENTITY`, but both callers pass `PARENT`, so `parent` was always `null`; it now reads `PARENT` (no other
    reader of `Bolt.parent`; no gameplay change).
  - Medium: the `BigAssMaverickRobotHand` link to its block gave 0 (the block moves by the catch-up velocity, all in
    the first step). Now the hand links to the robot in `FOLLOW` and uses its own body otherwise.
  - Low: the `SpritesSystem` `TODO` is back, for the bodies that move once per frame (D3). `PetitDevilChild` and
    `ElectrocutieChild` cast their parent with `as` (fail fast); `Bolt` keeps `as?`, because `SigmaRatClaw` is also a
    parent. `CeilingCrusher`: no sprite, no link (recorded in `SCOPE.md`). Accepted: the `PreciousWoman` gems keep her
    link for 1 frame after the throw, until the cluster spawns.
  - Map: the child `Spike` in `attach` got `cull_out_of_bounds=false` (it was culled out of view and did not come
    back). The `INTRO_STAGE room12` check text now says that the robot falls in after the gate.
  - Checked OK: all other commands, rooms, spawns and properties; the rocket and spike chain; the pipes; `081f` touches
    `MegamanXSabreSlash.kt` only at a comment line, no conflict; `O2-R7` and `O5-R1` hold.
  - After the fixes: `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar` pass. The fixed parts were read again.
- Step 8: Diffity opened at base `c49d23d09`. During the play test the owner found that `attach` and `pipe` started
  over a pit: blocks spawn only when they overlap the camera spawn bounds, and both floors only touched the room's
  bottom edge. Fix: both rooms 64 px lower, so that each floor lies 64 px inside its room (the pattern of the `134a`
  rooms). The owner asked what `restartStep()` does and whether a deferred `PipePortal` pull-in would make it moot;
  answered (it would, at the cost of a teleport up to 2 steps later); the owner did not ask for the change, so `restartStep()` stays.
- During the play test the owner saw hitches and freezes; a read-only GC audit (`temp/gc-audit/AUDIT.md`) found the
  machine swapping (19 of 19 GiB, 18 JVMs) and allocation sources with no item; filed on `master` as
  `docs/issues/2026-11-09-per-frame-allocations-hot-paths.md` (`80ca1eb0a`).
- Owner: "All tests pass" (2026-11-09), the merge approval. Diffity: 0 threads. Not done: the `O5-R2` `--diagnostics`
  capture with the flag on (no capture was found); the branch adds one function call per drawn entity and no
  allocation (review: `O5-R1` holds).
- `master` (`047c` merge, docs) merged into the branch with no conflict; `engine:test` (667), `core:test` (1276),
  `core:smokeTest` (451) and `lwjgl3:jar` pass.
