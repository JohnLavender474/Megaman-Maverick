# Notes: mega-refactor-084d-damage-reaction

## Decisions

## Overlap with other branches

Checked at branch start (2026-10-30). Design input: the 084 `NOTES.md`, "Design research" and "Invincibility research".

- `mega-refactor-051b-camera-behaviors` (active, its automated checks pass): it changes `Megaman.kt` (the
  `focusSnappedAway` writes at `:210,343,501` and the `IFocusable` import) and `ClimbBehavior`. This branch does not
  change those lines, so a small conflict is possible only in the imports. Merge order: whichever is ready first; the
  other merges `master` and runs its checks again.
- `mega-refactor-031e-bindings`: no worktree yet at this check, and its folder is about bindings, not the player.
