# TODO: mega-refactor-185a-lost-memories-launcher

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- At the merge, on `master`: replace `run-game-*.sh` with the new Maverick names in the live
  docs that only `master` changes: `docs/mega-refactor/CLAUDE.md`,
  `_templates/branch/docs/BRIEF.src.html`, and the open issue files in `docs/issues/` (about 40
  files). Branch folders and process logs are history and keep the old names.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Per-game platform builds: `build-game.sh` and the `build-game-*.sh` wrappers take the game
  (module, main class, name, version); a `construo` block per launcher module. Builds are
  Maverick only today (owner: "Game builds can be a TODO item for now"). Item `185` step 1
  remainder; fixes the folded `jdk.zip` bug.
- Lost Memories window icon and own assets folder (item open question 2).
- Graal native image setup (`nativeimage.gradle`) stays in `lwjgl3` only.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
- `generateAssetList` (root `build.gradle`) runs its body at configuration time in every
  subproject (now 6, was 3): guard it to one project or make it a real task.
- `--screen` values other than `MAIN_MENU_SCREEN` open Maverick menus in Lost Memories (no
  redirect); dev-only.
