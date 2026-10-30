# Notes: mega-refactor-031d-devices

## Decisions

## Overlap with other branches

Checked on 2026-10-30, on `master` at `05fc5ed12`: no other branch is active, and no worktree exists
(`git worktree list` shows only the main checkout). So there is no overlap now.

Planned branches that read or change the same code later:

- `031e` (bindings): replaces `ControllerButtons` and changes `ControllerSettingsScreen`. This branch changes
  `GamepadInput`, `KeyboardInput`, `getPreferredControllerCode` and (with scope question 3) one capture listener in
  `ControllerSettingsScreen`. `031e` starts after `031d` merges.
- `031f` (menu input): blocked by `031d` and `031e`. It reads the frames that this branch's adapters build. `Esc` needs
  no code in `031d`.
- `047` (co-op): splits the pads by claim later. This branch adds no API for it.
