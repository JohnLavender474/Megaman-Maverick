# Process log: mega-refactor-134a-draw-interpolation

## Status

- State: active
- Goal: draw bodies and the camera between the last 2 physics steps, behind `--interpolation` (default off)
- Done: branch start, scan, `SCOPE.md` and `CONTRACT.md` approved, `PLAN.md`
- Next: plan step 8 (owner play test, Diffity, diagnostics runs)
- Blocked:
- Brief: https://claude.ai/artifact/H5UC5JGUAuBnyMnrXjtxLe (approved 2026-11-09)

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-07

- Item start decisions (in `ITEM.md`): render interpolation, merge directly into `master`, no vsync change.
- Branch start: folder from the templates, `ITEMS.md` rows, worktree from `master` at `cba150aec`.
- Scan of the draw path on `cba150aec`: `NOTES.md` of the item is still current. New: `PlayersFocus.getFocusPosition()`
  has side effects (D2); `alpha = accumulator / fixedStep` holds with any `--fixedStepScalar`. 2 bug candidates in
  `docs/TODO.md`.
- Brief published; D1 (guard), D2 (co-op view), D3 (A/B switch) asked.

## 2026-11-09

- Owner answers at the brief: D1 guard of 1 tile per step; D2 average offset of the grouped players in co-op; D3
  `--interpolation` plus a live dev key. `SCOPE.md` and `CONTRACT.md` approved.
- Correction: the brief said the dev key works "as the `--viewEdges` keys", but those keys do not exist. The key is
  `I`, armed only when the launch gives `--interpolation`, like the screenshot key. Brief republished with the note.
- `PLAN.md` written (8 steps).
- Step 1: a subagent built `RefactorTest134.tmx` (rooms `open`, `shaft`, `edge`, `scroll`, `transition`,
  `transition_b`) with the `level-builder` skill, and added `REFACTOR_TEST_134`. The `level-analyzer` check passes. Note:
  the scroll section covers the whole `scroll` room, so the scroll starts at the spawn.
- Step 2: `IBody`/`Body` step state (`beginStep`, `endStep`, `getStepMove`, `stepSerial`, cleared in `reset`);
  `WorldSystem` `stepSerial`, `alpha` (also on frames with no step; 1 at the cap) and `IDrawOffsets.getDrawOffset`
  (guard of `ppm` per step, D1). Design change from the scope: the offsets are read through the new engine interface
  `IDrawOffsets`, which `WorldSystem` implements, so that entities do not need the world. New
  `WorldDrawOffsetsTest` (11 cases, with the evenness and jitter tests). The first run failed 3 cases because the
  default self friction of `Body` slows the test body; the test now turns off friction and gravity.
  `engine:test` and `core:test` pass.
- Step 3: `GameSprite` `drawOffsetX`/`drawOffsetY`. The draw copies the 20 vertices to a shared scratch array and
  adds the offset there, so the sprite state never changes (a translate and a translate back can drift a sprite
  that is placed once). `SpritesSystem` takes an optional `IDrawOffsets` and sets the offset of the entity's body on
  each sprite. `DarknessV2` overrides `draw` and skips the offset; this is correct, its grid is fixed in the world.
  New `SpriteDrawOffsetTest` (6 cases).
- Step 4: `CameraFollow` records the focus value per axis in its own `update()` only (not in `moveToFocus`, which
  the reset path of `CameraRoomBounds` also calls), and not in `smooth`. `IFocusable.getFocusDrawOffset` (default 0);
  `FocusComponent` takes an optional `drawBody`; `Megaman` and `MegamanX` pass their body. `PlayersFocus` gives the
  average of the grouped players, 0 while the ease runs or a keeper moved the value (D2). The new effect
  `CameraDrawOffset` takes the offset on an axis only when the settled position equals the clamp of the followed
  value, so any mover after the follow (a scroll) or a hold before it gives 0; it then clears the record.
  `MegaLevelScreen.draw` adds the offset to the game camera and sets the saved position back after the draw;
  `Background.draw(batch, offset)` does the same with `offset * parallax`. New `CameraDrawOffsetTest` (8 cases);
  a break of the settled check made its case fail, as expected.
- Step 5: `--interpolation` (`arity = 1`, nullable, default `false`); `params.interpolation` and
  `params.interpolationToggle` (`I`, armed only when the flag is given). The game polls the toggle next to the
  screenshot trigger. `createGameEngine()` builds `worldSystem` as a local value and passes it to `SpritesSystem`.
- Step 6: `engine:test` (656 tests), `core:test` (1182 tests), `core:smokeTest` and `lwjgl3:jar` pass.
- Finding (not in scope): `CameraLimitStage` and `CameraScroll` also call `getFocusPosition()` of the focus in each
  frame, so `PlayersFocus` runs its ease and keeper code more than once per frame today. The draw path adds no call.
- Step 7, play check review (1 subagent, read-only): all commands valid; the map loads. Findings: (1) room `scroll`
  was 1 view tall, so "the other axis follows" could not show; (2) no hazard for the death and respawn check;
  (3) the `X6_INTRO` room placeholder; (4) a wrong TODO text in `DesktopLauncher` (it named `I` as the screenshot
  key). Fixes: the map subagent made `scroll` and its section 2 views tall, with 3 climb steps, moved the floor bump
  up off the floor path, and added a `Spike` in `open` (properties as `RefactorTest051.tmx` id 170, checked);
  the contract now names `--room room1` for `X6_INTRO` and the spike for the respawn check; the TODO text is fixed.
- Step 7, diff review (1 subagent, read-only). With the flag off: no change found; `O2-R7` and `O5-R1` clean.
  Findings and actions: (1) bug: `alpha` was wrong with `--fixedStepScalar` other than 1 (the leftover is not in
  `[0, fixedStep)`; the scan of 2026-11-07 was wrong on this point). Fixed: alpha is measured over the interval
  `fixedStep / scalar`; 2 new tests (scalar 0.5 and 2) fail with the old formula and pass with the new one.
  (2) and (3): with the flag on, the shape renderer (`--debugShapes`, the `WhiteArrowPool` outline) and sprite
  entities with no body get no offset: named in the contract as known `134b` items, and in `docs/TODO.md`.
  (4) nit: the `I` key during a pause: noted in `docs/TODO.md`, no fix (dev key only). (5) nit: one frame after a
  tier change: accepted. (6) tests: added a body skipped by `doUpdate` (`WorldDrawOffsetsTest`) and the D2 rule
  (`SharedViewTest`: the mean of the group, 0 while it eases). Not added: a test with a real `CameraScroll` and an
  `X_ONLY` test; the stand-in mover tests cover the settled rule that both use.
- Automated checks after the fixes: `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar` pass.
- Diffity round 1, during the play test: the owner asked to keep `Background.draw` as the one entry point
  (no overload). Fixed: `Background.cameraDrawOffset` field, set by the level screen. The rule went into the root
  `CLAUDE.md` on `master` (`71ead449f`).
- Scope change (owner, 2026-11-09): the draw offset leaves `IFocusable`, and the room subject is split from it
  (`IRoomSubject`). See `docs/SCOPE.md` "Scope change". Brief republished with a marked note. Plan step 7b.
- Scope change done: `IRoomSubject` (engine); `IFocusable` keeps only `getFocusPosition()`;
  `CameraRoomTransition.start(..., subjectCenter)` without its `follow` parameter; `CameraRoomBounds.subject`;
  `IDrawOffsetSource`, `PlayerDrawOffset` and `CameraSubject` (core). The owner asked whether the 1-player draw
  lambda overloaded `MegaLevelScreen` (1063 lines; item `130` plans to move the camera stack out); the owner chose a
  `CameraSubject` class. New `CameraSubjectTest` (2 cases; a break of the co-op source made one fail, as expected).
  `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar` pass. Not committed: the Diffity round is open.
- Rename (owner, 2026-11-09): `IRoomSubject.getRoomBounds()` is now `getSubjectBounds()`, because the old name
  read like the bounds of a room next to `room.rectangle`. 13 files (code, tests, `SCOPE.md`, the brief).
- Owner's play test of `open` (640 x 480, with and without the flag): no clear visual difference. Reason (from the
  numbers): at scale 1 the run judder is about ±0.64 screen px. The owner chose to keep the change, with the flag on
  by default (scope change 2). `TODO`s at the 4 known `134b` items. Real-level checks being found by a subagent.
- Scope change 3 (owner, Diffity thread `7d2ecee3`): `CameraRoomBounds`, `CameraLimitStage`, `CameraDrawOffset` and
  `IDrawOffsetSource` moved to `engine`; `CameraSubject` is now the engine mechanism (`set`), and the new `core`
  `PlayersCameraSubject` holds the policy. New tests: `engine` `CameraSubjectTest`, `core` `PlayersCameraSubjectTest`.
  All automated checks pass. New files marked with `git add -N`, so that Diffity shows them.
- Interpolation on by default: a subagent found real-level rooms for the `134b` cases (`CONTRACT.md`). Not live:
  `SpiderWeb`, `RotationAnchor`; `ElecDevilBodyPieces` and `RotationAnchor` move their children inside the steps.
- Rename (owner, 2026-11-09): `FocusComponent` is now `CameraSubjectComponent` (it gives the follow target and the
  room subject of one entity, the per-entity inputs of `CameraSubject`); `defineFocusComponent` is now
  `defineCameraSubjectComponent`. 12 files, 3 of them moved with `git mv`. All automated checks pass.
- Play test `WILY_STAGE_2 --room room9`: the `GutsTank` fist hitches while the tank moves (a known `134b` case: the
  attached fist is placed with `setCenter` outside the steps). Owner's decision: keep the default on and accept the
  hitches; `TODO`s at `GutsTankFist` and `GutsTank`. The `134b` list in `docs/TODO.md` names the known cases.
- `O5-R2` diagnostics (owner's 2 `--diagnostics` runs, `REFACTOR_TEST_134 --room open`, level frames only; files in
  `temp/134a-diagnostics/`): frame time off / on: mean 1.57 / 1.58 ms, p50 1.44 / 1.45 ms, p95 2.22 / 2.34 ms,
  p99 3.57 / 3.47 ms. `SpritesSystem` 0.04 / 0.04 ms, `screen.draw` 0.08 / 0.08 ms, `WorldSystem` 0.94 / 0.93 ms.
  The difference is in the noise. The spikes in both runs are the level start (frames 1-2 of the level) and
  single `screen.render` frames, the same with the flag off.
- Owner: ready to merge (2026-11-09). `master` had new code that touches the same files (`127a`, `081e`), so
  `master` was merged into the branch. One simple conflict, resolved: `SharedViewTest.kt` (the `master` line
  `setBodySense(FEET_ON_GROUND)` kept, with the branch name `CameraSubjectComponent`). `engine:test`, `core:test`,
  `core:smokeTest` and `lwjgl3:jar` pass after the merge.
