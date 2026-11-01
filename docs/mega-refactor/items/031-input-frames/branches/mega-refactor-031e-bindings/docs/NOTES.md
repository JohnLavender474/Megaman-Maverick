# Notes: mega-refactor-031e-bindings

## Decisions

## Overlap with other branches

Scan on `master` at `5d4a37eac`. `git worktree list` at the end of the scan: the main checkout and
`mega-refactor-051b-camera-behaviors` (at `844bd8e13`).

- **`mega-refactor-051b-camera-behaviors` (active).** Its goal is one camera controller in place of
  `CameraManagerForRooms`. Its scope changes `MegamanMaverickGame.kt` (a `cameraController` field, the rotation end at
  `:316`) and `MegaLevelScreen.kt` (the camera set-up, the 5 callbacks, the frame block, 4 event cases, the level start
  and end). So far its code changes `CameraManagerForRooms.kt`, `LevelDefinition.kt` and new engine camera files.
  - `031e` changes `MegamanMaverickGame.kt` in other lines (`create` `:303-311`, `postCreate` `:453-454`, `resume`, new
    functions near `setCurrentScreen`) and `MegaLevelScreen.kt` in one line (`:926`). No shared lines are planned.
  - `031e` does not change `MegaLevelScreen.reset()`. Back to pause depends on `reset()` not running. It does not depend
    on what `reset()` holds, so the `051b` changes there do not affect it.
  - Back to pause saves and restores the UI camera. `051b` works on the game camera, so there is no shared state.
  - Whichever branch merges second takes `master` first and runs its checks again.
- **`mega-refactor-084a-player-state` (merged).** It merged into `master` during this scan (`c6cb6b3b2` to `5d4a37eac`), so
  it has no worktree now. It changes no input code: `git diff --name-only` shows no file in `input/` or `controllers/`, and
  not `MegamanMaverickGame`, `MegaLevelScreen`, `MegaPlayers`, `AirDashBehavior`, `ControllerComponent`, the bindings
  screen or the launcher. Its only change in a file that `031e` touches is 4 `ConstKeys` lines in `Constants.kt`, away from
  `PreferenceFiles`. The line numbers in `docs/SCOPE.md` are good at `5d4a37eac`.
- **`mega-refactor-031f-menu-input` (blocked by this branch).** It owns the menu input of the bindings screen. It adds `Esc`
  as another input for `backAction`, so it needs the back path of this branch and no new path.
- **Planned items that build on this branch:**
  - `013` (pause as an overlay stack) replaces the 2 functions that hold the level screen.
  - `034` (settings file) moves `PlayerSettingsStore` to `settings.json`. Its proposed scope still says "apply the saved
    bindings at boot"; see `docs/SCOPE.md` question 9.
  - `047` (co-op) raises `MegaPlayers.capacity`. One `PlayerSettings` and one store exist for each slot by then, and the
    adapters already take a `PlayerSettings`, so the design does not block the join flow or a claim of a device.

Scan again at plan step 2 (`3e4433744`). `git worktree list`: the main checkout (`393d9be81`, one docs commit after the branch
start), `mega-refactor-051b-camera-behaviors` (`b7fb77bb8`) and `mega-refactor-084d-damage-reaction` (`2aff79ae5`).

- **`051b`:** its code is now in. In `MegamanMaverickGame.kt` it changes the imports, `gameCamera`, `cameraController` and
  `services`. It does not change `create()` at the input lines, `postCreate()` at the screens, `resume()` or
  `setCurrentScreen()`. In `MLS`, its diff has no line at `isPauseButtonRequested`, `resize` or the body of `reset`. It also
  changes root `CLAUDE.md`. No shared lines.
- **`084d`:** at this scan it is docs only (one overlap note, which names this branch). Its freeze rule reads input through
  `MegamanValues.BUTTONS_TO_UNFREEZE`. This branch does not change `MegaControllerButton` or the input frame, so that rule
  stays the same.

Scan again at plan step 16 (`e29094367`). `git worktree list`: the main checkout (`6b0097aa1`), this branch and
`mega-refactor-084d-damage-reaction` (`5d7d87d9b`).

- **`051b` merged into `master` during this work** (`7fe00b8b8`, `6b0097aa1`). Its worktree and branch are gone. Its files in
  the list of plan step 18.3: `MegamanMaverickGame.kt`, `MegaLevelScreen.kt`, `TestServices.kt`. So step 18 needs the forced
  merge of `master` into this branch, and all automated checks again. This branch did not take `master` in; the brief says
  no merge.
- **Overlap result.** `git merge-tree --write-tree` does not exist in the local git (`2.34.1`, the option needs `2.38`), so
  the check used the old form, `git merge-tree <merge-base> HEAD <branch>`.
  - Against `master`: 0 conflict markers. 2 files are changed on both sides (`MegamanMaverickGame.kt`, `MegaLevelScreen.kt`),
    and each merges with no conflict.
  - Against `084d`: 0 conflict markers, the same 2 files. `084d` has `master` in it. Its diff against `master` at this scan has
    docs, `RefactorTest084.tmx`, `LevelDefinition.kt`, `LevelCompletionScreensTest.kt` and 2 temporary tests: no file in the list
    of plan step 18.3.

Forced merge of plan step 18.3, on 2026-11-01 (`8ed39c993`, `master` at `dad4b0e0f`).

- **Conflicts:** 2, both in the import lists (`AirDashBehavior.kt`, `ControllerComponent.kt`). Both sides stay.
- **Checks after the merge:** clean build exit 0. Engine 600, core 528, 0 failures. 76 unique warning lines, no new line
  against `build-baseline-warnings.txt`. A5 to A16 pass; A7 still lists the 14 paths.
- **Manual checks to run again:** none. `master`'s changes to `MegamanMaverickGame.kt` and `MegaLevelScreen.kt` are camera
  and player changes, with no line on pause, resume, viewports, music or input. Its change in the 2 merged entity files is
  one momentum line in `AirDashBehavior.kt`. It changes no file in the controller and input folders of the list.
