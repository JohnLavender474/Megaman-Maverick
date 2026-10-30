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
