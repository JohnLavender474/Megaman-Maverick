# Scope: mega-refactor-184a-sub-frame-taps

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file.

- Status: approved
- Approved: 2026-11-23 (scope change of the same day: keyboard only, see "Decisions" in `NOTES.md`)

This is a small branch (3 production files, no open decision): it has a "Steps" part and no `PLAN.md`.

## Goal

Each keyboard tap and each short keyboard release between two presses counts, also when it is shorter than one render
frame, at every performance tier. The frame builder gets the "went down" mask that a later pad latch can also use.

## Item

`184` (`ITEMS.md`). From `ITEM.md`:

- Proposed scope: `InputFrameBuilder.build` takes a second mask with the buttons that went down since the last build.
  Keyboard: fill the mask from `Gdx.input.isKeyJustPressed`. Gamepad: a sampler that reads the pads more often than once
  per frame.
- D1: the gamepad uses a sampler thread at 500 Hz, if SDL2 is safe from a second thread; else all Jamepad calls move to
  the sampler thread.
- D2: a short release between two presses gives `JUST_RELEASED` in this frame and `JUST_PRESSED` in the next frame.
  The control lock ignores the latch.
- D3: one branch for keyboard and gamepad.
- Merge target: directly into `master`.

Changed on 2026-11-23 by the owner, after the research in `NOTES.md`: the gamepad part is out (see "Out of scope").

## Current state

Scan of 2026-11-23 on `master` `d30e27565`. The item `NOTES.md` has the read path and the loss rates.

- `MegamanMaverickGame.kt:600-604`: one build per slot per `render()`, from `keyboardInput.held()`,
  `gamepadInput.held()`, `slot1GamepadInput.held()` and `followBrain.held(...)`, and the system frame from
  `keyboardInput.systemHeld()`. The asset-load branch (`MegamanMaverickGame.kt:560-585`) builds no frames.
- `InputFrameBuilder.kt:14-35`: edges come from the last frame and the new held mask only. The lock maps
  `PRESSED`/`JUST_PRESSED` to `JUST_RELEASED` and the rest to `RELEASED`.
- `KeyboardInput.kt:30-43`: `Gdx.input.isKeyPressed` for the bound keys and 6 alias keys.
- `DefaultLwjgl3Input.keyCallback` sets `justPressedKeys[key]` only on `GLFW_PRESS` (not on `GLFW_REPEAT`), and
  `prepareNext()` clears it after the render. So at the read, `isKeyJustPressed` means "a new press since the last
  read". `pressedKeys` and `justPressedKeys` change in the same callback on the main thread, so `held` and `downs`
  agree.
- Readers of `isJustPressed` / `isJustReleased` in `core`: 35 call sites in 20 files, all through the frame. None
  changes.
- `FollowBrain` records `isPressed` bits of the leader frame; a latched tap reaches the bot as a one-frame press.

## In scope

1. `InputFrameBuilder.build(held, downs)`. For a button in `downs`:
   - it was released at the last build and is not held now: `JUST_PRESSED` now, and a "release next" bit gives
     `JUST_RELEASED` in the next build (a tap);
   - it was pressed at the last build and is held now: `JUST_RELEASED` now, and a "press next" bit gives
     `JUST_PRESSED` in the next build (a short release, D2).
   - A pending bit wins over the sample in its build. Then the sample of that build sets the next pending bit if it
     differs from the forced status.
   - The lock ignores `downs` and clears the pending bits.
   - No `ups` mask: a "went down" while the button was pressed at the last build means that a release came before it.
     2 taps inside one frame count as 1.
2. `KeyboardInput.downs()`: a mask from `Gdx.input.isKeyJustPressed` for the bound keys and the alias keys.
3. `MegamanMaverickGame.render()`: slot 0 builds from `keyboardInput.held() or gamepadInput.held()` and
   `keyboardInput.downs()`. Slot 1 (pad or bot) and the system frame pass `downs = 0`.
4. Tests: builder (tap, short release, pending bits, lock, no change when `downs = 0`) in `engine`; `downs()` in
   `KeyboardInputTest`.

### New API and first caller

| API | First caller |
|---|---|
| `InputFrameBuilder.build(held, downs)` | `MegamanMaverickGame.render()` |
| `KeyboardInput.downs()` | `MegamanMaverickGame.render()` |

## Out of scope

- The gamepad latch. Research (`NOTES.md`): SDL2 and Jamepad are not safe from a second thread, and no drop-in Java
  library (input4j, sdl2gdx) keeps the press and release events. Only LWJGL 3.4 `lwjgl-sdl` (SDL 3) does: backlog
  candidate. The pad stays one read per frame; the game targets at least 60 FPS.
- A 60 FPS policy for auto-perf: backlog candidate.
- The fixed tick of `010`. A `TODO` at the build says that the latch is taken once per tick after `010`.
- `ESCAPE` (system frame): no latch.
- The keyboard latency (events polled after the render). Not loss.

## Affected areas and risks

- All readers of the slot-0 frame see the latched keyboard edges. A sub-frame key tap is now one frame of
  `JUST_PRESSED`, then one of `JUST_RELEASED`.
- A key that is pressed in a frame where the pad holds the same button: `downs` from the keyboard and `held` from the
  pad are ORed. Pad held + new key press while the button was pressed at the last build gives a short release. This
  needs the pad and the key on the same button at once; accepted, `TODO` at the build.
- The rebind screen swaps `Gdx.input.inputProcessor`; `justPressedKeys` does not depend on it.

## Overlap with other branches

Read 2026-11-23: `060k`, `078e`, `078f` (art), `183b` (charged shot draw), `compactor-level`,
`fix/projectiles-ignore-death`: no input files. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | The latch is read at the same point as `held`. |
| `O1-R5` Code only with a production caller | no effect | Each new API has a caller (table above). |
| `O2-R1` Input only through `InputFrame` | helps | The edges stay in the frame; no reader changes. |
| `O2-R3` One fixed tick | waiting | After `010`, the first tick of a frame takes the latch (`TODO`). |
| `O2-R5` No logic assumes one player | no effect | Slot 1 has no keyboard. |
| `O2-R7` Gameplay apart from rendering | helps | A key tap no longer depends on the frame time. |
| `O5-R1` No per-frame allocation | no effect | Masks are `Long`. |
| `O5-R2` Measure hot paths | no effect | The `inputFrames` entry adds 14 key reads; compare it in `--diagnostics` at the play test. |
| `O9-R1` NES-style pad | no effect | Same buttons. |

## Open questions

None.

## Steps

1. `InputFrameBuilder.build(held, downs)` with the pending bits, and the builder tests. Check: `./gradlew engine:test`.
2. `KeyboardInput.downs()` and its test. Check: `./gradlew core:test --tests '*KeyboardInputTest*'`.
3. `MegamanMaverickGame.render()` passes the masks; the `TODO` for `010`. Check: `./gradlew lwjgl3:jar`.
4. All automated checks, the review before the play test, play scripts in `temp/play/`.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes, with the new builder tests.
- [ ] `./gradlew core:test` passes, with the new keyboard test.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

From `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-184a-sub-frame-taps`.

Experimental:

1. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_flat --performance low`: keyboard at 30 FPS. Tap jump
   and shoot as short as possible; mash shoot. Each tap gives a hop or a shot.
2. Same command: hold shoot to charge, release for a very short moment and press again. The charge shot fires, and a
   new charge starts.

Control:

3. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: keyboard and pad at 60 FPS. Walk, jump, slide,
   charge. No double presses, no lost holds.
4. Same command with a pad only: taps and holds as today.
5. Menus: on the logo screen and the main menu, `ENTER` and the arrow keys move and select once per press.

## Behaviors to check

- The control lock: during a room transition, a key tap does nothing and gives no press after the lock ends.

## Intended changes

- A keyboard tap shorter than one frame gives `JUST_PRESSED` in one frame and `JUST_RELEASED` in the next.
- A keyboard release shorter than one frame between two presses gives `JUST_RELEASED`, then `JUST_PRESSED`.
