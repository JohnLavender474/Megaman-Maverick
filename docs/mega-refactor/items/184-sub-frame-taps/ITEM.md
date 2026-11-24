# 184: Keep button taps that are shorter than one frame

- Status: dropped (2026-11-23)
- Type: feature
- Objectives: `O9`, `O5`
- Depends on: none
- Related: `010`, `031`, `047`, `070`, `178`
- Branches: `mega-refactor-184a-sub-frame-taps` (keyboard and gamepad, by D3)

## Dropped (2026-11-23)

The owner abandoned the item on 2026-11-23, at the end of the planning of `184a`. Facts from the planning (branch
`docs/NOTES.md`):

- SDL2 2.0.20 and Jamepad are not safe from a second thread next to the gdx-controllers monitor (use-after-free at
  hot-plug), so the 500 Hz pad sampler of D1 needs one thread that owns every Jamepad call.
- input4j and sdl2gdx compare the pad state once per poll and lose sub-frame taps as Jamepad does. Only LWJGL 3.4
  `lwjgl-sdl` (SDL 3) gives timestamped gamepad button events.
- The owner then cut `184a` to the keyboard latch only (`isKeyJustPressed` into `InputFrameBuilder.build(held, downs)`),
  with the pad at one read per frame and a target of at least 60 FPS. That scope was not implemented.

Unfinished work, for a later item (not filed as items):

- The keyboard latch and the builder `downs` mask (`184a` `docs/SCOPE.md`, "In scope" and "Steps").
- Pad taps through SDL 3 gamepad events with LWJGL 3.4 `lwjgl-sdl`, behind a custom gdx `ControllerManager`.
- A 60 FPS policy for auto-perf.
- `gradle.properties` says `jamepadVersion=2.0.14.2`, but `2.0.20.0` resolves.
- Not checked: Windows DirectInput-only pads may not open in Jamepad (no `SDL_HelperWindow`).
- The idea of a 60 Hz loop with a 30 Hz update is a note in `010` `NOTES.md`.

## Goal

The game reads each button once per render frame. A tap that starts and ends between two reads is lost, and a short
release between two presses is lost (two presses become one). The read interval is the frame time, so the loss gets
worse when the FPS goes down: at 30 FPS (`VERY_LOW`) a 15 ms tap is lost 55 % of the time; at 60 FPS, 10 % of the
time. Make each tap and each short release count, at every performance tier.

```
render frame:  |------- 33 ms -------|------- 33 ms -------|
read:          R                     R                     R
pad button:          [down 15 ms]
InputFrame:    RELEASED              RELEASED              (tap lost)
```

## Cause (found 2026-11-23, `master` `2f15a4b85`)

- One read per render frame: `MegamanMaverickGame.kt:627-632` builds the frames from `keyboardInput.held()` and
  `gamepadInput.held()`, once in each `render()`. `InputFrameBuilder.build` (`engine/.../input/InputFrameBuilder.kt:31-50`)
  makes the edge (`JUST_PRESSED`, `JUST_RELEASED`) from two held masks only.
- Gamepad: `GamepadInput.kt:81-86` calls `pad.getButton(code)`. In gdx-controllers 2.2.3 desktop (Jamepad
  2.0.20.0), that call runs `SDL_GameControllerUpdate()` and returns `SDL_GameControllerGetButton()`, the current
  state only. Jamepad drains and discards the SDL button events (`SDL_PollEvent` loop in
  `nativeControllerConnectedOrDisconnected`), so no transition history exists. The `ControllerListener` events of
  gdx-controllers come from a state compare once per loop (`JamepadControllerMonitor.run`, posted with
  `Gdx.app.postRunnable`), so they lose the same taps.
- Keyboard: `KeyboardInput.kt:134` reads `Gdx.input.isKeyPressed`. GLFW key callbacks queue every press, and the
  LWJGL3 backend sets `justPressedKeys` in the callback (`DefaultLwjgl3Input.keyCallback`), so
  `Gdx.input.isKeyJustPressed` keeps a sub-frame tap. The game does not read it, so keyboard taps are lost the same way.
- The game logic is not the FPS-dependent part. Each tier has 2.5 physics steps per frame (`Performance.kt`:
  150/60, 75/30, 112.5/45, 225/90), and the controller and behaviors systems read the frame once per
  `engine.update`. So "just pressed" is not seen 0 or 2 times per frame by tier. The one reader inside a physics step
  (`PlayerBody.kt:67`, `FALL_THROUGH_PRESS_UP`) sees the same step ratio at every tier.

The owner's guess is correct: the cause is the poll once per render frame. But a fixed input rate on the render thread
does not fix it by itself: a fixed tick (`010` step 3) that still reads the devices once per render frame loses the
same taps. The fix must latch what happens between two reads.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. See `NOTES.md` for the options and the sources.

- `InputFrameBuilder.build` takes a second mask with the buttons that went down at any time since the last build
  (and, by decision D2, the buttons that went up). A tap that is not held at the build gives `JUST_PRESSED` in that
  frame and `JUST_RELEASED` in the next frame.
- Keyboard: fill the mask from `Gdx.input.isKeyJustPressed` for the bound keys and the alias keys.
- Gamepad: fill the mask from a sampler that reads the pads more often than once per frame (decision D1).
- Unit tests for the builder (tap, short release, control lock) and for the sampler latch.

## Owner's input

2026-11-23 (from the main session): controller input is less reliable when the game runs below 60 FPS. The owner
thinks that the cause is the poll once per render frame.

## Decisions

Owner accepted the recommendations, 2026-11-23:

- D1: the gamepad uses a sampler thread at 500 Hz that latches each "went down" and "went up" for `render()` to read
  and clear. First check that SDL2 joystick calls are safe from a second thread on Linux, Windows and macOS; if not,
  move all Jamepad calls to the sampler thread. If that check fails, fall back to extra reads on the main thread at
  more points of the frame. Details in `NOTES.md`, "Options".
- D2: a short release between two presses gives `JUST_RELEASED` in this frame and `JUST_PRESSED` in the next frame,
  so each mashed press counts. The control lock ignores the latch.
- D3: one branch for keyboard and gamepad.
- Merge target: directly into `master` (owner, 2026-11-23).

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
