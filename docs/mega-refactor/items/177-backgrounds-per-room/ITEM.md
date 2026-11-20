# 177: Backgrounds drawn per room

- Status: idea
- Type: feature
- Objectives: `O9`, `O4`
- Depends on: none
- Related: `046`, `078`
- Branches: none yet

## Goal

Each background object names its rooms, and the level draws it only over the part of the view that is inside those
rooms. A room pan between 2 areas then shows each room with its own background, a backtrack shows no pop, and no
show or hide trigger is needed.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. In phases (owner, 2026-11-20):

1. The clip (a scissor or a clip per background) in the level draw, with the room list as a background property.
   Today `MegaLevelScreen.draw` shows or hides each background for the whole view (the `backgroundsToHide` set,
   changed by `SHOW_BACKGROUNDS` and `HIDE_BACKGROUNDS`).
2. Move the Reactor Man triggers to it. The borders `room7`/`room8`, `room10`/`room11` and `room13`/`room14` still pop
   on a backtrack (the `room3`/`room4` border is solved in `078d` by a switch in a fully covered room).
3. The other levels that use show and hide triggers (Desert Man uses a gate today).

## Owner's input

- 2026-11-20 (branch `078d`): "apply A now, and document option C as the preferred long-term solution. [...]
  Probably item so we can apply in phases rather than all at once".

## Open questions

- Order with `046` (backgrounds as pure functions of the camera), which changes the same draw code.

## Sources

- Branch `078d` `docs/TODO.md` (backlog candidate), `SCOPE.md` decision R5 and `process-log.md`.
