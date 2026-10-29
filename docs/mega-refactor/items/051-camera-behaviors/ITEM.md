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

2026-10-29 (verbatim, about the `016c` camera state): "Do these changes play nicely with the item that exists for
support X-style "camera zones"? Ideally, the game camera's behavior is built in a composable and easy-to-modify
manner, i.e. a raw camera with components attached to it that affect is behavior and rules." Then "yes" to recording it
here.

## Decisions

- 2026-10-29 (owner): the camera is a raw camera with behavior components attached (room bounds, X-style bounds from
  stage objects, auto-scroll, rotation, focus), each with its own rules. `016c` adds `GameCameraState` (the flags
  `rotating` and `focusSnappedAway`; game and holder field `camera`) as the read side that entities use (39 reads).
  Here, those flags become values that the rotation and focus behaviors expose; the entity reads stay; the writers
  (today in `MegaLevelScreen` and the game reset) move into the behaviors.

## Open questions

- Reuse the focus logic, make each behavior first-class, or express each behavior as bounds changes (the X6 way)?
- One camera class or several small parts?
- Which room-transition side effects belong to the camera, and which to a room or level system?

## Sources

- `NOTES.md` in this folder.
