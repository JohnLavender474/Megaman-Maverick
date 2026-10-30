# Notes: mega-refactor-031d-devices

## Decisions

- 2026-10-30 (owner, at the plan review, verbatim: "approved"): the trigger codes are `1004` and `1005`; a bound
  trigger shows its raw code on the bindings screen (a readable label is a backlog candidate for `031e`, in
  `docs/TODO.md`); the removal of the index-0 `isControllerKeyPressed` overload stays in step 5.
- 2026-10-30 (owner, verbatim): the test subclass of `ControllerMapping` "is fine, as long as there is a comment in
  the code explaining why the subclass is needed".

## Overlap with other branches

Checked on 2026-10-30, on `master` at `05fc5ed12`: no other branch is active, and no worktree exists
(`git worktree list` shows only the main checkout). So there is no overlap now.

Checked again on 2026-10-30, on `master` at `1dc4d898a`: `mega-refactor-051a-camera-read-interface` started (its own
worktree; its `docs/SCOPE.md` on `master` is still the template). Its item is the camera read model (`051`). This
branch does not change the camera, `MegamanMaverickGame` or the engine, so no overlap is known. Check again before the
branch starts and before the merge.

Planned branches that read or change the same code later:

- `031e` (bindings): replaces `ControllerButtons` and changes `ControllerSettingsScreen`. This branch changes
  `GamepadInput`, `KeyboardInput`, `getPreferredControllerCode` and (with scope question 3) one capture listener in
  `ControllerSettingsScreen`. `031e` starts after `031d` merges.
- `031f` (menu input): blocked by `031d` and `031e`. It reads the frames that this branch's adapters build. `Esc` needs
  no code in `031d`.
- `047` (co-op): splits the pads by claim later. This branch adds no API for it.
