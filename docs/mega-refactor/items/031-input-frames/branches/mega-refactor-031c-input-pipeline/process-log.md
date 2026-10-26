# Process log: mega-refactor-031c-input-pipeline

## Status

- State: merged
- Goal: a new input pipeline with the same behavior: device adapters, one held mask per device, an engine edge builder with the lock; the poller goes
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master`.
- Scan on `1df22a05c`: the engine `ControllerButton`, `ControllerButtons`, `ControllerUtils`, `ControllerPoller`,
  `IControllerPoller`; core `MegaControllerPoller`, `ControllerUtilsExtensions`, `ControllerMappingExtensions`,
  `ScreenController`, the frame builder and the lock in `MegamanMaverickGame`, the `controllerPoller` readers,
  `ControllerSettingsScreen`, `GameSettingsPanel`, `MegaControllerPollerTest`, `InputFrameTest`, the open input issues
  and item `031` `NOTES.md`. The owner chose the split and the five recommendations (item `ITEM.md`, decisions of
  2026-10-26). Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written. No new test rooms; the manual checks use
  `REFACTOR_TEST_029` `movement`, `INTRO_STAGE` `room1` and `room12`, the start screens, the settings screens and
  `--screen` for the skip screens.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (8 steps: branch start, baseline with the
  `--screen` launches, the temporary suite harness, the new pipeline with the lock and the facade, removals and moves,
  contract checks, the play test, the suite's removal and the merge).
- The owner approved `docs/PLAN.md`. Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree
  created.
- Step 2 (baseline, worktree on `a8b3ff84b`): clean build passes; 481 engine and 161 core tests, 0 failures; 114
  warnings. `--screen OPENING_SCREEN`, `CREDITS_SCREEN` and `GAME_OVER_SCREEN` each ran with no exception until the
  timeout; `--screen` calls `setCurrentScreen` directly (`MegamanMaverickGame.kt:445-450`), so each screen opens.
- Step 3: temporary suite harness in `core/src/test/.../input/frozen031c/` (each file has a `TODO` to delete the package
  before the merge). `FrozenInput031c.kt`: frozen copies of `ControllerButton` (the device read), `ControllerPoller`
  and `MegaControllerPoller` as on `master` at `a8b3ff84b`, without the per-frame debug log and without
  `alternateActuators` (only the dead `ScreenController` adds them). `FakeDevices031c.kt`: fake devices and a seeded
  script (keys, gamepad connect and disconnect, gamepad buttons, prefs present or not, `loadPrefsOnConnect`, the
  lock). Plan detail (no approval needed): no seams in the adapters. The fakes stand in for the real device APIs, as
  `MegaControllerPollerTest` does: `Gdx.input` is a `java.lang.reflect.Proxy`, `ControllerUtils` is a MockK object
  mock, and the mapping and prefs extensions are static mocks. So the frozen copies stay verbatim, and the adapters
  in step 4 read the real APIs. The first run hit `OutOfMemoryError` (MockK records every call); the proxy and a
  per-tick `clearMocks(recordedCalls = true)` fixed it. `InputPipelineContract031cTest`, harness case: on 5 seeds of
  1,500 ticks, the frozen copies give the live poller's statuses and gamepad codes on every tick; the scripts reach all
  four statuses and the codes `null`, `0`, the mapping codes and the prefs codes. Build passes; 481 engine and 162 core
  tests, 0 failures.
- Step 4: engine `input/InputFrameBuilder` (held mask with one bit per button index → frame; `locked`; the old rule
  without `enabled`; the last frame in `frame`; a loop over a button array, no allocation). Core `input/KeyboardInput`
  and `input/GamepadInput` (held masks; the gamepad adapter has `MegaControllerPoller`'s connect, disconnect and prefs
  logic in the same order, before its read; both read the codes from `ControllerButtons` through `orderedKeys()`).
  Plan detail (no approval needed): the per-frame debug log of `MegaControllerPoller.run()` is not carried over (it
  built a string each frame and only printed with its tag on). `input/FrameControllerPoller`: the facade over a source,
  with a `TODO` for the menu-input branch. `MegamanMaverickGame`: `keyboardInput`, `gamepadInput` (public, for the
  settings screen) and `slot0FrameBuilder`; `render` writes slot 0's frame from `keyboardInput.held() or
  gamepadInput.held()` at the old poll point (the diagnostics entry keeps its name `controllerPoller`); `onEvent` sets
  `slot0FrameBuilder.locked`; `controllerPoller` is `FrameControllerPoller { players.inputs[PlayerSlot(0)] }`;
  `defineControllerPoller` is gone. `ControllerSettingsScreen` sets `game.gamepadInput.loadPrefsOnConnect`. Tests: the
  temporary suite compares the new pipeline with the frozen copies on the 5 seeded scripts (frames and gamepad codes)
  and in 6 named cases (hold then lock, unlock while held, press and lock in one tick, keyboard A then gamepad A,
  gamepad connect while held, gamepad disconnect while held); `InputFrameBuilderTest` (engine, 8);
  `FrameControllerPollerTest` (core, 3). Build passes; 489 engine and 172 core tests, 0 failures. Launches of
  `REFACTOR_TEST_029 --room movement` and of the start screens ran with no exception until the timeout.
- Step 5: removed `ControllerPoller` (engine), `IControllerButton` (engine), `MegaControllerPoller` (core) and
  `input/InputFrames.kt` (the copy from the poller, no caller left). `ControllerButton` keeps only `keyboardCode` and
  `controllerCode`; `ControllerButtons` maps to `ControllerButton`, so a script removed the now-redundant
  `as ControllerButton` casts (14 places in `ControllerUtilsExtensions`, `ControllerSettingsScreen`, `ScreenController`,
  the adapters and tests). `ControllerUtils` moved from the engine to `core/.../controllers/` (`git mv`, package only);
  its imports changed in `MegamanMaverickGame`, `ControllerSettingsScreen`, `GameSettingsPanel`, the adapters and the
  tests. `ScreenController` no longer adds an alternate actuator; a `TODO` names the touch adapter for the mobile build.
  `MegaLevelScreen.controllerPoller` (no reader) removed. Tests: `MegaControllerPollerTest` became `GamepadInputTest`
  (the same cases on `GamepadInput.held()`, plus 2 for the held bits); new `KeyboardInputTest` (3); the core
  `InputFrameTest` (all poller comparisons) became `PlayerInputsTest` (1); `MegaPlayersTest` builds its frame with
  `with`; the temporary suite loses its harness case (the live poller is gone) and its coverage checks move into the
  seeded pipeline case. Greps: no `Gdx.input` and no `gdx.controllers` in `engine/src/main`; none of the removed names
  outside the frozen package. Build passes; 489 engine and 165 core tests, 0 failures. Launches of
  `REFACTOR_TEST_029 --room movement` and of the start screens ran with no exception until the timeout.
- Step 6, automated contract checks (clean build on `256803b47`): build passes; 489 engine and 165 core tests, 0
  failures (`master`: 481 and 161); 114 warnings (same as `master`); `lwjgl3:jar` builds; `contact-rule-selection.txt`
  unchanged; the temporary suite passes (7: the seeded scripts with the coverage checks, and the 6 named cases);
  `InputFrameBuilderTest` (8), `KeyboardInputTest` (3), `GamepadInputTest` (8), `FrameControllerPollerTest` (3) pass;
  greps: no `Gdx.input` and no `gdx.controllers` in `engine/src/main`, none of the removed names outside the frozen
  package; the slot-0 frame is written only at `MegamanMaverickGame.kt:540`, the old poll point. Extra check: `javap`
  of `InputFrameBuilder`, `KeyboardInput`, `GamepadInput` and `FrameControllerPoller` shows no `box-impl`; the only
  `new` is the unreachable `NoWhenBranchMatchedException`. Waiting for the owner's play test. The frozen copies and the
  temporary suite stay until step 8.
- Step 7, play test: the owner played the manual checks in `docs/CONTRACT.md` and reported "All tests pass".
- Step 8: the last commit before the merge deletes the frozen copies and the temporary suite
  (`core/src/test/.../input/frozen031c/`, 3 files). Clean build passes without them; 489 engine and 158 core tests, 0
  failures; 114 warnings. Waiting for the owner's merge approval.
- The owner approved the merge ("yes"). Merged into `master`.
