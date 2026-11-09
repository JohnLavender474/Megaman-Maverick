# Plan: mega-refactor-134a-draw-interpolation

How to do the work in `SCOPE.md` (approved 2026-11-09). No game launch in any step (also for subagents).

## Steps

### 1. Test map `RefactorTest134`

- Changes: a subagent builds `assets/tiled_maps/tmx/RefactorTest134.tmx` with the `level-builder` skill: rooms
  `open` (3 views wide, flat floor, a few platforms, 2 player spawns for co-op), `shaft` (1 view wide, 4 views tall,
  spawn at the top), `edge` (exactly 1 view, closed walls), `scroll` (a `type=scroll` section to the right),
  `transition` (2 rooms side by side, spawn in the left one). Painted `tiles` in every room. Add
  `LevelDefinition.REFACTOR_TEST_134` (`TEST_LEVEL`) and its row in `LevelCompletionScreensTest`.
- Check: `./gradlew core:test`; the `level-analyzer` skill lists the 5 rooms, each with a player spawn.
- Done: yes (2026-11-09)

### 2. Body step state and world alpha (engine)

- Changes: `Body`: `beginStep(serial)`, `endStep()`, `getDrawOffset(alpha, serial, out)` with the 1-tile guard
  (`ppm` from the world), state cleared in `reset()`. `WorldSystem`: `stepSerial`, `alpha` (`accumulator /
  fixedStep`, clamped to 0..1, 1 at the cap), the 2 calls inside the existing `preProcess` and `postProcess` loops.
- Check: new `BodyTest` and `WorldSystemTest` cases, and the evenness test (1/60 s frames and ±1 ms jitter);
  `./gradlew engine:test`.
- Done: yes (2026-11-09)

### 3. Sprite draw offset (engine)

- Changes: `GameSprite`: 2 draw offset floats, applied around `super.draw`. `SpritesSystem`: an optional world read
  and an `on` flag; after `component.preProcess`, set each sprite's offset from the entity's `BodyComponent`, or 0.
- Check: `GameSprite` tests (applied only in the draw, no drift over 100 frames); `SpritesSystem` test (0 with the
  flag off and for an entity without a body); `./gradlew engine:test`.
- Done: yes (2026-11-09)

### 4. Draw camera and backgrounds (engine and core)

- Changes: `IFocusable.getFocusDrawOffset(out)` (default 0); `FocusComponent` (body offset); `PlayersFocus`
  (average of the grouped players, 0 while the ease or a keeper acts, D2). `CameraFollow` records per axis the value
  it wrote before the clamp, and if it followed this frame (not in `smooth`). A new core camera part computes the
  per-axis offset with the stage limits and the blended view (0 when held, on a scroll axis, or not followed).
  `MegaLevelScreen.draw` adds the offset before `apply()` and takes it off after; each background adds
  `offset * parallax` to its camera for its draw.
- Check: core camera tests (open room, room edge, transition, scroll axis, smooth follow); `./gradlew core:test`.
- Done: yes (2026-11-09)

### 5. Flag and dev key

- Changes: `--interpolation` in `DesktopAppArgs` (default `false`), a param field, the `I` trigger armed only when
  the flag is given (D3); `createGameEngine()` wires the world read into `SpritesSystem`; the level screen reads the
  current value each frame.
- Check: `./gradlew lwjgl3:jar`; a launcher args test if one exists for the flags.
- Done: yes (2026-11-09)

### 6. Full automated checks and the `--diagnostics` baseline

- Changes: none in code. Run all automated checks of the contract.
- Check: `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar` pass.
- Done: yes (2026-11-09)

### 7. Review before the play test

- Changes: code-review subagents for the branch diff (with the `O2-R7` and `O5-R1` checks) and for the play checks
  (commands, the test map rooms, the `X6_INTRO` room). Fix the findings.
- Check: the result in the process log; the automated checks pass again.
- Done: yes (2026-11-09)

### 7b. Scope change: camera inputs split (2026-11-09)

- Changes: `IFocusable` keeps only `getFocusPosition()`; new `IRoomSubject` (engine) and `IDrawOffsetSource` (core);
  `CameraRoomBounds.subject`; `CameraRoomTransition.start(..., subjectCenter)`; `FocusComponent` without `drawBody`;
  `PlayersFocus` implements `IDrawOffsetSource` and drops `getFocusBounds`; `PlayerDrawOffset` and `CameraSubject`
  in `screens/levels/camera/`; the test fakes; `CameraSubjectTest`.
- Check: all automated checks; the control checks `transition` and a co-op room change at the play test.
- Done: yes (2026-11-09)

### 7c. Scope change 3: camera parts to `engine`, `CameraSubject` split (2026-11-09)

- Changes: see `SCOPE.md` "Scope change 3".
- Check: all automated checks pass (2026-11-09).
- Done: yes (2026-11-09)

### 8. Play test and diff review

- Changes: none. Give the owner the play checks; open Diffity. The owner runs the 2 `--diagnostics` runs; Claude
  reads them with the `diagnostics-analyzer` skill and writes the numbers in the process log (`O5-R2`).
- Check: the owner's report.
- Done: no
