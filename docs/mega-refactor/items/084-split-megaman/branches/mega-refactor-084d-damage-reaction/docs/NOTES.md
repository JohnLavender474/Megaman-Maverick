# Notes: mega-refactor-084d-damage-reaction

## Decisions

- (2026-10-30, owner) `docs/SCOPE.md` approved with all 3 recommendations:
  - `invincible` reads a cached `damageRecovery` field in `Megaman`, so a read keeps today's 2 map lookups.
  - "Frozen Mega can be hit, frozen enemies cannot" stays, and `docs/CONTRACT.md` states it.
  - The `SPAWN` damage lock is a bug candidate, filed at the merge, with no change in this branch.

## Overlap with other branches

Checked at branch start (2026-10-30). Design input: the 084 `NOTES.md`, "Design research" and "Invincibility research".

- `mega-refactor-051b-camera-behaviors` (active, its automated checks pass): it changes `Megaman.kt` (the
  `focusSnappedAway` writes at `:210,343,501` and the `IFocusable` import) and `ClimbBehavior`. This branch does not
  change those lines, so a small conflict is possible only in the imports. Merge order: whichever is ready first; the
  other merges `master` and runs its checks again.
- `mega-refactor-031e-bindings` (active): its scope is the bindings. No code changed yet at this check. Its scope lists
  `ControllerComponent.kt:99`; this branch changes only that file's imports and reads at `:57,73,80`. Check it again
  before the merge, also because the freeze rule reads input through `MegamanValues.BUTTONS_TO_UNFREEZE`.

Checked again before the merge (2026-10-30, `master` at `6b0097aa1`):

- `051b` is merged, and `master` is in this branch.
- `mega-refactor-031e-bindings` (active) now changes `Constants.kt`, `AirDashBehavior.kt` and `ControllerComponent.kt`. This branch
  changes only the imports of `ControllerComponent.kt`, in a different hunk.
