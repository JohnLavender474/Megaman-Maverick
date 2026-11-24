# TODO: mega-refactor-184a-sub-frame-taps

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Pad taps shorter than one frame: SDL 3 gamepad button events through LWJGL 3.4 `lwjgl-sdl`, behind a custom gdx `ControllerManager` that one thread owns; latch into `InputFrameBuilder.build(held, downs)`. Research in `docs/NOTES.md`.
- A 60 FPS policy: keep the game at 60 FPS where the machine allows it (for example an auto-perf floor or a longer wait before it drops to 45 or 30), because pad taps are read once per frame.
- `gradle.properties` says `jamepadVersion=2.0.14.2`, but `2.0.20.0` resolves (through `gdx-controllers-desktop`). Align the property or remove it.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Windows: DirectInput-only pads may not open in Jamepad, because SDL makes `SDL_HelperWindow` only with video or haptic init (research side note, not checked; `NOTES.md`).
