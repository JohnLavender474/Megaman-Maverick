# Notes: mega-refactor-031f-menu-input

## Decisions

## Overlap with other branches

Copied from `docs/SCOPE.md`, "Overlap with other branches", at plan step 1 (base `bd7eca889`). The 2 `051` lines are history: `051e` and its
parent merged into `master` (`e12c880f5`).

Scan on `c6b8cecf7`. `git worktree list`: the main checkout, `mega-refactor-051-master` (`d28c60893`) and
`mega-refactor-051e-camera-rotation` (`b79c6de35`). The other folders in `.worktrees/` are not registered worktrees. In `ITEMS.md`,
only those 2 branches are active.

Update at the contract approval: `051e` and its parent merged into `master` (`e12c880f5`), so the 2 items below on `051` are history. That merge also
changes `LPS` at the `EXIT` row, and `EXIT` then works in a `TEST_LEVEL`; no check depends on it. `084g` is active in its own worktree. Read the
line numbers of this file again on the current `master`.

- **History: `mega-refactor-051e-camera-rotation` (merged).** It changes `G` at the imports, `:189-201`, and the
  `services` build at `:423-434`. In `MLS`, its hunks start at `:93`, `:214`, `:302`, `:314`, `:323`, `:370`, `:847`, `:884`, `:1035` and
  `:1076`. This branch changes `MLS:862-864` and `:876`. The `:847` hunk ends before `:862`, and the `:884` hunk starts 8 lines after
  `:876`. Inference: no conflict. Both branches also change `G`, in other lines. It changes no menu, no skip screen and no input
  file. Whichever merges second takes `master` first and runs its checks again. The line numbers here move after it merges.
- **History: `mega-refactor-051-master` (merged).** It has the same base. No code of its own beyond `master`.
- **`084g-movement-state` (at the scan: planned; now active, see below).** Its plan names `canMove`, `RunComponent`, the crouch timers and
  `entities/megaman/components/ControllerComponent.kt`. It reads the slot frame through `input`. This branch changes no entity file and no player frame.
  Inference from its plan text only: no shared file. `084h` is conditional and has no shared file.
- **`013` (planned).** It owns the pause flow and replaces `showOverPause` and `closeOverPause`. Its stack takes over the `Esc` pause
  entry and the `LPS` back. This branch adds 1 test at `MLS:876` and 1 `onBack`.
- **`012` (planned).** It replaces `TURN_CONTROLLER_*` and the lock. `game.inputLocked` becomes its query. `NOTES.md` says to run `012`
  and `031b`-like work apart: it edits `G.onEvent`. This branch does not.
- **`082` (planned).** Its `ITEM.md:74` says that "`031` must split 'gameplay input locked' from 'UI input' before this item". See
  question 6. The system frame already gives `082` the pattern: a builder that the lock does not touch.
- **`047` (planned).** "`Esc` pauses for both players" is true with 1 keyboard frame. `MegaPlayers.menuInput` is the one place that
  chooses the slot. Its `capacity` rises in `047`.
- **`055`, `010` (planned).** A replay must record the system frame too, or `Esc` is lost. It is 1 more `Long` per frame. A note for
  those items, no code here.
- **`034`, `069`, `070` (planned or idea).** `034` has no overlap. `069` adds a button to `MegaControllerButton`, and no menu reads it.
  `070` is the home of a pad back button, if the owner wants it later.
- **`016`.** Its note says "expose input as a service, not as more `game` fields". `game.systemInput` and `game.inputLocked` are 2 fields, but
  screens keep the game, and entities do not read these 2 values. No `MegaServices` change.

### Read again at plan step 1 (base `bd7eca889`)

`git worktree list`: the main checkout, this worktree, and `mega-refactor-084g-movement-state` at `7439ebaac`. The other folders in `.worktrees/` are not
registered worktrees.

- **`mega-refactor-084g-movement-state` (active; last commit: "084g: automated checks pass; play test next").** Its code diff against the base has 18
  files (670 insertions, 89 deletions). No file is in contract A6, A7 or A8. Its production files are in `entities/` and `screens/levels/LevelRoomHandler.kt`.
  `LevelRoomHandler.kt` is on the merge rule of plan step 14.2, so if `084g` merges first, this branch must merge `master`.

### Read again at plan step 12 (`230844f6e`)

- `git worktree list`: the main checkout at `1fa0be319` and this worktree. No other active worktree. `084g` merged into `master` during this work (`87d512886`, with
  `1fa0be319` for its docs), so 3 commits of `master` are not on this branch.
- `git diff --name-only bd7eca889..master` and `git diff --name-only master...HEAD` have no shared path. `git merge-tree <merge-base> HEAD master` (3-argument form,
  because the local git is `2.34.1`) gives 0 conflict markers.
- The merge rule of plan step 14.2 matches 1 path: `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/LevelRoomHandler.kt`. So the merge of `master` is
  necessary. `LevelRoomHandlerTest.kt` also changes on `master`, and `G`, `MLS` and the files of A6 to A8 do not.
- Plan step 12 does not merge. Rule of the task: no merge of `master` unless a step says so. The merge comes at step 14.2, or before the play test if the owner asks for it.

### Read again at plan step 14.2 (`master` at `ee72f5d0a`)

- `git worktree list`: the main checkout and this worktree. No other active worktree.
- `master` is 12 commits past the merge base `1fa0be319`. Its code change is `084h`, the left and right last-pressed fix and the `HeartTank` sand fix: 14 paths outside `docs/`, all in `entities/`, `CLAUDE.md`, `CameraEdgeBlocks.kt` or the tests of these entities.
- The merge rule of plan step 14.2 matches 0 paths. The two sides share 0 paths, in code and in docs. `master`'s new code adds no use of a removed name.
- `ControllerComponent.kt` changes on `master` and reads the slot frame through `input`. This branch does not change it or the slot frame.
- So the merge of `master` is skipped, as the rule says. The build on `master` after the squash covers the combined code.
