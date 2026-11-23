# 184: Keep button taps that are shorter than one frame

- Status: planned
- Type: feature
- Objectives: `O9`, `O5`
- Depends on: none
- Related: `010`, `031`, `047`, `070`, `178`
- Branches: none yet

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

## Open questions

- Merge target (directly into `master` or into a parent branch): ask at item start.

## Sources

- `NOTES.md` in this folder.
