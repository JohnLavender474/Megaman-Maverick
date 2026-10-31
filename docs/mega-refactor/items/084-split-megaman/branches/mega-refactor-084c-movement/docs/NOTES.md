# Notes: mega-refactor-084c-movement

## Decisions

- (2026-10-30, owner) `docs/SCOPE.md` approved with both recommendations:
  - `AButtonTask` has no component, until `060` shows that Proto Man needs one.
  - The `!!` at the end of a room transition stays, with a `TODO` at the code and a bug candidate. The code dates from
    `755989525` and `1bbc4868b` (2026-06-09), not from the refactor.

- (2026-10-30, step 7) `GameEngine.destroyNow` resets components in insertion order, and behaviors that end in that pass write momentum after the
  momentum component reset. Tracing `AirDashBehavior.end`: `preserveMomentum()` and `requestMomentumKill()`. With the `onSpawn` lines gone, a death during
  an air dash would carry a running timer and a pending kill into the next life. Options: add the component after `BehaviorsComponent` (hidden order rule),
  or call `reset()` in `onSpawn` (chosen: explicit, and exact to the old code). A general reset order for components is a possible later item.

- (2026-10-30, owner, through the request from `051c`) The room-carry crash is fixed on this branch. This replaces the earlier decision to keep the `!!`.
  `051c` reproduced the crash: a transition ends with a `megaman_direction` change, so `END_ROOM_TRANS` arrives on the other axis. See `docs/SCOPE.md`,
  "In scope", item 7. `reset()` still keeps the carried values, because the start of each transition now clears them.

## Overlap with other branches

Checked at branch start (2026-10-30). Design input: the 084 `NOTES.md`, "Design research".

- `mega-refactor-031e-bindings` (active): see `docs/SCOPE.md`, "Overlap with other branches". Check it again before the merge.

Checked again before the merge (2026-10-30, `master` at `6645bc694`):

- `master` has only docs changes since this branch took it, so no merge of `master` into this branch.
- `mega-refactor-031e-bindings` (step 17 next): 2 shared files, `AirDashBehavior.kt` and `ControllerComponent.kt`. It changes an import near the top and the `selectButtonAction`
  lines (`AirDashBehavior.kt:33,38`, `ControllerComponent.kt:99`). This branch changes other imports, and `AirDashBehavior.kt:140`. The hunks do not touch.
- `mega-refactor-051c-camera-zones`: no shared code file. It must merge after this branch.
