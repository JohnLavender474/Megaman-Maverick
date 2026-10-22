# 051: X-style camera bounds, auto-scroll and other camera behaviors

- Status: planned
- Type: feature
- Depends on: none
- Related: `019`, `036`, `038`, `041`, `047`
- Branches: none yet

## Goal

Support camera behaviors beyond room bounds (X-style bounds set by stage objects, auto-scroll), and decouple `MegaLevelScreen` from `CameraManagerForRooms`.

## Item text

See `NOTES.md` for the full item text: the behaviors, the X6 facts, the problems now, the goal, the research questions and the contract note.

## Owner's input

From the camera request (summary): X6 manages the camera through objects that control bounds; `CameraManagerForRooms` supports only rooms, and `MegaLevelScreen` is tightly coupled to it. Research the focusable versus first-class design, and one class versus several.

## Open questions

- Reuse the focus logic, make each behavior first-class, or express each behavior as bounds changes (the X6 way)?
- One camera class or several small parts?
- Which room-transition side effects belong to the camera, and which to a room or level system?

## Sources

- `NOTES.md` in this folder.
