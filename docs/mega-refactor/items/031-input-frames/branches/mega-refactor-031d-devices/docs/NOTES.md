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

Checked again on 2026-10-30, at step 8, on the branch at `f1e597f03` (merge base `2307c5414`):

- `mega-refactor-051a-camera-read-interface` (at `d3c8ea133`, owner play test passed): its `docs/SCOPE.md` is the
  camera read interface (`ICameraView` in `engine`, `services.gameCamera`, about 63 files). `git diff --stat
  master...HEAD` in its worktree lists 74 files. No file is in both diffs (`comm -12` of the two name lists gives no
  output). It changes `MegamanMaverickGame.kt` and `engine/` (camera only); this branch changes neither. No overlap.
- `mega-refactor-084a-player-state` started on `master` after this branch (`c6dd8b109`). Its diff is docs only (6
  files in its branch folder). No overlap now.
- `master` moved after the branch start: `1eaef3415` (the `--loadKeyboardPrefs` launch flag) changes 2 files that
  this branch also changes (`ControllerUtilsExtensions.kt`: a new `loadKeyboardSettingsFromPrefs`;
  `ControllerSettingsScreen.kt`: "LOAD SAVED SETTINGS" for the keyboard calls it), plus `MegamanMaverickGame.kt`,
  `DesktopLauncher.kt`, the 3 run scripts and the root `CLAUDE.md`. `git merge-tree` of the branch and `master`
  gives no conflict marker; the hunks do not touch the same lines. Because the files are the same, step 10.3 applies
  at the merge: merge `master` into the branch in its worktree and run the step 8 automated checks again.

Planned branches that read or change the same code later:

- `031e` (bindings): replaces `ControllerButtons` and changes `ControllerSettingsScreen`. This branch changes
  `GamepadInput`, `KeyboardInput`, `getPreferredControllerCode` and (with scope question 3) one capture listener in
  `ControllerSettingsScreen`. `031e` starts after `031d` merges.
- `031f` (menu input): blocked by `031d` and `031e`. It reads the frames that this branch's adapters build. `Esc` needs
  no code in `031d`.
- `047` (co-op): splits the pads by claim later. This branch adds no API for it.

Checked at the merge, on 2026-10-30, on `master` at `1645b155d`: `051a` merged and changed `MegamanMaverickGame.kt`, so
`master` was merged into this branch (plan step 10.3). The active branches `051b` and `084a` share no file with this
branch.
