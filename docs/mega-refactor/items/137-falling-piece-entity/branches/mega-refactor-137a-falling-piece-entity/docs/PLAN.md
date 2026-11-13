# Plan: mega-refactor-137a-falling-piece-entity

How to do the work in `SCOPE.md`. Write this file after the owner approves the brief. It needs
no approval. When the plan changes, write the reason in the process log.

Temporary side-by-side suite (deleted in step 7, before the report and the merge):

- `core/src/test/.../entities/decorations/FallingPieceLegacyContractTest.kt` (the suite).
- `core/src/test/.../entities/decorations/LegacyDebrisPiece.kt`, `LegacyBlockPiece.kt`,
  `LegacyWoodCratePiece.kt` (frozen copies of the 3 classes from `master` at `add9d66d2`).

## Steps

### 1. Test map `RefactorTest137.tmx`, room `mixed`

- Changes: new `assets/tiled_maps/tmx/RefactorTest137.tmx` (with the `level-builder` CLI and a small
  script in `temp/137a/` for the named objects and the `tiles` layer): one 16 × 14 room `mixed`, floor,
  ceiling, 2 walls, 2 `WoodCrate`s, 2 brick `BreakableBlock`s (`type=Brick`), 2 `SMB3_GoldBreakableBlock`s
  3 tiles over the floor, player spawn `0`. `LevelDefinition.REFACTOR_TEST_137` (`TEST_LEVEL`), and its line
  in `LevelCompletionScreensTest`.
- Check: the TMX parses as XML; `LevelCompletionScreensTest` passes.
- Done: yes

### 2. Temporary side-by-side suite, green on the old code

- Changes: the frozen copies and the suite listed above. The suite drives a real `GameEngine`
  (`WorldSystem`, `UpdatablesSystem`) with a camera that has a viewport, and runs the sprite pre-process
  after each update. For each kind it spawns the frozen copy and the piece under test with the same inputs
  (a fixed seed on `UtilMethods.random`), and compares at each step of 1/60 s over 2.1 s: body center and
  velocity, sprite size, rotation, alpha, hidden, and the step on which each piece goes. For the X6 throw it
  compares the sets of start velocities and the jitter range over many seeds. In this step the piece under
  test is the old class.
- Check: `core:test` with `-Pkotest_filter_specs='*FallingPiece*'` passes.
- Done: yes

### 3. `FallingPiece`, `FallingPieceDef`, `BlockPieceColor`

- Changes: `entities/decorations/FallingPiece.kt`, `FallingPieceDef.kt` (the enum, `FallingPieceSize`,
  `FallingPieceSpin`), `BlockPieceColor.kt`. Spawn properties override the def per value (D1); new keys
  `ConstKeys.JITTER` and `ConstKeys.BLINK`.
- Check: `core:compileKotlin` passes.
- Done: yes

### 4. Move the callers, remove the 3 classes

- Changes: the 8 spawn sites and 5 load sites; `PinkBlock`, `BigAssMaverickRobot`, `Picket` imports;
  remove `DebrisPiece`, `BlockPiece`, `WoodCratePiece`, the `THUMP` prop and `ConstKeys.THUMP`;
  `D1000Rig`, `D1000DefeatTest`; `entity-tags.txt`.
- Check: no `DebrisPiece`, `BlockPiece` class, `WoodCratePiece` class or `ConstKeys.THUMP` in `core/src`;
  `core:compileTestKotlin` passes.
- Done: yes

### 5. Side-by-side suite against `FallingPiece`

- Changes: the suite's piece under test becomes `FallingPiece` (through `FallingPiece.spawn` and
  `FallingPiece.throwDebris`). The wood crate rotation is compared from a fresh instance only (D3).
- Check: the suite passes; `D1000DefeatTest` passes.
- Done: yes

### 6. Permanent `FallingPieceTest`

- Changes: `core/src/test/.../entities/decorations/FallingPieceTest.kt`: each def's body and sprite
  values, life of 2 s, spin from 0°, debris blink, one reused instance as block, wood crate, debris, the
  D1 override cases (def + one override; no def + all props; no def + a missing prop throws), and region
  failures (not loaded; missing from the atlas).
- Check: passes; then break the code under each group of tests, confirm it fails, and restore (results in
  the process log).
- Done: yes

### 7. Delete the temporary suite, full run

- Changes: delete the suite and the 3 frozen copies (record their last result in the process log).
- Check: `engine:test core:test core:smokeTest lwjgl3:jar` pass.
- Done: yes
