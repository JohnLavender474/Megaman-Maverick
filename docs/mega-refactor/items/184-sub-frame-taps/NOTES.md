# Notes: 184 Keep button taps that are shorter than one frame

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Scan (2026-11-23, `master` `2f15a4b85`)

### The read path, one desktop loop

```mermaid
sequenceDiagram
    participant Loop as Lwjgl3Application.loop
    participant Game as MegamanMaverickGame.render
    participant GLFW
    participant SDL as Jamepad / SDL2
    Loop->>Game: window.update() -> render()
    Game->>SDL: GamepadInput.held(): pad.getButton(code)<br/>(SDL_GameControllerUpdate + GetButton, current state)
    Game->>Game: KeyboardInput.held(): Gdx.input.isKeyPressed
    Game->>Game: InputFrameBuilder.build(held) -> InputFrame
    Game->>Game: screen.render(delta) -> engine.update (1x)
    Loop->>Loop: input.prepareNext() clears justPressedKeys
    Loop->>GLFW: glfwPollEvents() (key callbacks queue each press)
    Loop->>SDL: app runnables: JamepadControllerMonitor.run()<br/>(hot-plug, state compare for listener events)
    Loop->>Loop: sync.sync(foregroundFPS) sleeps
```

- `MegamanMaverickGame.kt:627-632`: one frame per slot per `render()`, from `keyboardInput.held()`,
  `gamepadInput.held()`, `slot1GamepadInput.held()` and `followBrain.held(...)`.
- `engine/.../input/InputFrameBuilder.kt:31-50`: the edge comes only from the last frame and the new held mask.
  A button that is down and up again between two builds stays `RELEASED`. A button that is up and down again
  between two builds stays `PRESSED`.
- `GamepadInput.kt:76-96`: reads every bound code with `pad.getButton(code)` (or a trigger axis), and the stick axes.
- `KeyboardInput.kt:130-140`: reads `Gdx.input.isKeyPressed` for the bound keys and the alias keys.
- Library facts (sources extracted to `temp/184-input-sampling/src/`, not committed):
  - `Lwjgl3Application.loop()`: `window.update()` (render), then `GLFW.glfwPollEvents()`, then the app runnables,
    then `sync.sync(targetFramerate)`. One loop per render frame.
  - `JamepadControllerMonitor.run()` calls `controllerManager.update()`, then compares the state of each pad, and
    posts itself again with `Gdx.app.postRunnable`. So the `ControllerListener` events are also one compare per loop.
  - `DefaultLwjgl3Input.keyCallback` sets `pressedKeys[key]` and `justPressedKeys[key]` on `GLFW_PRESS`;
    `prepareNext()` clears `justPressedKeys` after the render. So `isKeyJustPressed` keeps a sub-frame tap.
- Jamepad 2.0.20.0 (source: `ControllerManager.java` and `ControllerIndex.java` in
  https://github.com/libgdx/Jamepad, JNI code in comments):
  - `nativeControllerConnectedOrDisconnected` runs `SDL_JoystickUpdate()` and a `SDL_PollEvent` loop. It returns
    `true` only for `SDL_JOYDEVICEADDED` and `SDL_JOYDEVICEREMOVED`, and it discards every other event, also
    `SDL_CONTROLLERBUTTONDOWN` and `SDL_CONTROLLERBUTTONUP`.
  - `nativeCheckButton` runs `SDL_GameControllerUpdate()` and returns `SDL_GameControllerGetButton()`.
  - So each `getButton` call is a fresh read of the current state, and no transition history is kept.

### Things ruled out

- Physics steps per frame by tier: `Performance.kt` gives 2.5 steps per frame on every tier (150/60, 75/30,
  112.5/45, 225/90). `ControllerSystem` and the behaviors read the frame once per `engine.update`, with the frame
  delta. So a `JUST_PRESSED` is seen once per frame on every tier.
- Readers inside a physics step: 1 found, `PlayerBody.kt:67` (`FALL_THROUGH_PRESS_UP`, read in the contact path).
  It sees the same `JUST_PRESSED` in each step of the frame, but the step ratio is the same on every tier.
- Readers of `isJustPressed` / `isJustReleased` in `core`: 35 call sites in 20 files. All read the frame, so a latch
  in the builder reaches all of them with no change at the call sites.

### Loss rate

For a tap of length `d` and a read interval `T` (the frame time), with a random phase, the tap is lost when no read
falls inside it: `P(lost) = max(0, 1 - d / T)`. The same formula gives the loss of a short release between two
presses (the presses merge into one).

| Tap length | 90 FPS (11.1 ms) | 60 FPS (16.7 ms) | 45 FPS (22.2 ms) | 30 FPS (33.3 ms) |
|---|---|---|---|---|
| 10 ms | 10 % | 40 % | 55 % | 70 % |
| 15 ms | 0 % | 10 % | 32 % | 55 % |
| 25 ms | 0 % | 0 % | 0 % | 25 % |
| 30 ms | 0 % | 0 % | 0 % | 10 % |

A hitch frame (for example 100 ms) loses every tap inside it, on any tier. The tap lengths of real players were not
measured. A `--diagnostics` field or a debug log of tap lengths could measure them (not in scope yet).

Side fact: the gamepad read happens at the start of `render()`. The keyboard events are polled after the render and
before the sleep, so a key event during the sleep shows in the frame after the next one. This is latency, not loss.
It is not in scope.

## Other games and engines (web research, 2026-11-23)

- SDL2: `SDL_GameControllerGetButton` gives "the current state of a button"
  (https://wiki.libsdl.org/SDL2/SDL_GameControllerGetButton). Button events carry a timestamp and a state
  (https://wiki.libsdl.org/SDL2/SDL_ControllerButtonEvent) and go through the event queue
  (https://wiki.libsdl.org/SDL2/SDL_PollEvent). An event reader sees both edges of a short tap (inference from the
  queue design; the wiki does not say it in those words).
- GLFW: gamepad and joystick state is polled only; the only joystick callback is connect and disconnect. Keys have
  callbacks, and sticky keys keep a press until it is polled (https://www.glfw.org/docs/latest/input_guide.html).
- Unity Input System: with one update mode and polling in another, discrete presses can be missed or doubled
  (https://docs.unity3d.com/Packages/com.unity.inputsystem@1.12/manual/timing-missed-duplicate-events.html). The fix
  that the docs give: set a flag in an event handler or in `Update`, and clear it after `FixedUpdate` reads it, or
  process the events in the fixed update
  (https://docs.unity3d.com/Packages/com.unity.inputsystem@1.12/manual/timing-mixed-scenarios.html).
- Godot: `Input.is_action_just_pressed` stays `true` for a press and release that are fast, "so as not to miss input"
  (https://docs.godotengine.org/en/stable/classes/class_input.html). Input is buffered as it arrives and flushed at
  points of the loop; "agile event flushing" flushes more than once per frame
  (https://godotengine.org/article/agile-input-processing-is-here-for-smoother-gameplay).
- Apple Game Controller: reading the state once per frame can miss events of a fast device; use the change handlers
  (https://developer.apple.com/documentation/gamecontroller/handling-input-events.md).
- Unreal (forum advice, not an official doc): a separate input thread accumulates the input, and the game thread
  reads it each tick (https://forums.unrealengine.com/t/realtime-input-frame-rate-independent/407044).
- A devlog with the same bug: taps released before the next frame read were lost; the fix kept the press and release
  times (https://gamedev.net/blogs/entry/2262482-kreep-missed-tap).
- "Fix Your Timestep" (https://gafferongames.com/post/fix_your_timestep/) says nothing on input. No source was found
  for the read rate of fighting games (GGPO) or for RetroArch polling modes.

The common pattern: keep the input as events or as a latch ("pressed since the last read"), and let the tick consume
the latch once. A fixed tick alone does not do it.

## Options

### D1: how the gamepad sees a tap between two frames

| Option | What | Keeps a tap of | Risk |
|---|---|---|---|
| (a) sampler thread | A daemon thread reads the bound pad codes at a fixed rate and ORs "went down" and "went up" bits into an atomic latch; `render()` takes and clears it | ≥ 1 read interval (2 ms at 500 Hz) | SDL and Jamepad from 2 threads: the main thread still runs `JamepadControllerMonitor`; Jamepad's Java state (`heldDownButtons`) is not synchronized; the platform backends differ |
| (b) more reads on the main thread | Read the pads at 2 or 3 points of each frame (start, after `engine.update`, after the draw) into the latch | taps that cover one of the points | uneven intervals; the sleep part of the frame is not covered |
| (c) wait for `010` | Read per fixed tick | no change: the ticks of one frame run back to back, with one device read | does not fix the loss |

Recommendation: (a), at 500 Hz (inference: a 2 ms read interval keeps any human tap; the cost is 8 button reads and
2 axis reads per pad per read). First check, with sources, that SDL2 joystick calls are safe from a second thread
(SDL2 has `SDL_LockJoysticks`) on Linux, Windows and macOS. If not safe, move all Jamepad calls, also the hot-plug
monitor, to the sampler thread. Fallback if the check fails: (b).

### D2: short release between two presses

- (a) The builder sees "went up" and "went down" since the last build and the button is held: give `JUST_RELEASED`
  now and `JUST_PRESSED` in the next frame. Each mashed press makes a shot. Cost: the second press is one frame late.
- (b) Merge them, as today.

Recommendation: (a). The control lock keeps its rule: a locked builder ignores the latch.

### Design sketch (not approved)

```kotlin
val frame = frameBuilders[0].build(held = keyboard.held() or pads.held(), downs = keyboard.downs() or pads.takeDowns())
```

- `InputFrameBuilder.build` gets a `downs` mask (and an `ups` mask by D2). A button in `downs` that is not held and
  was `RELEASED` gives `JUST_PRESSED`, and the builder keeps a "release next" bit, so the next frame gives
  `JUST_RELEASED`.
- `KeyboardInput.downs()` reads `Gdx.input.isKeyJustPressed` for the bound and alias keys.
- `FollowBrain` gives `downs = 0` (it copies frames, not devices).
- Callers: `MegamanMaverickGame.render()` (3 build calls). Files touched (count from the scan): `InputFrameBuilder.kt`,
  `GamepadInput.kt`, `KeyboardInput.kt`, `MegamanMaverickGame.kt`, a new sampler class in `core/.../input/`, and the
  tests (`engine/src/test/.../input/`, `core/src/test/.../input/`).

## Overlap

- `010` step 3 moves the frame build into the tick. The latch must then be taken once per tick: the first tick of a
  frame takes it, the next ticks see none. `010/NOTES.md` already says "a press that comes and goes between two ticks
  must be latched (`031`)"; `031` did not build it, so this item does.
- `047f` (device claim) changes `GamepadInput` (the `TODO` at `GamepadInput.kt:39`). The sampler must follow the
  claimed pad of each slot.
- `178` bot input copies P1's frames; a latched tap reaches the bot as a normal one-frame press.
