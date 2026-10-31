# Notes: 090 Branch name in the game window title

Scan of 2026-10-31, based on `master` commit `dc6057a99`.

## Current code

- The title has one source: `TITLE` (`DesktopLauncher.kt:25`), set with `config.setTitle(TITLE)`
  (`DesktopLauncher.kt:122`). No other code in `core`, `engine` or `lwjgl3` sets a window title.
- The 3 run scripts have one line each: `./gradlew lwjgl3:run --args="... $*"`.

## Why the script reads the branch, not the game

- The game runs from `assets/` (`workingDir` of the `run` task), and a packaged build has no git. If the launcher called
  git, the shipped code would have a dev-only path. A flag keeps git out of the game.
- `git branch --show-current` is correct in a worktree, and it is empty for a detached `HEAD`.
- An empty value must not go into `--args`, because `--branch` would then take the next flag as its value. So the script
  adds the flag only when the value is not empty:

  ```bash
  BRANCH=$(git branch --show-current 2>/dev/null)
  ./gradlew lwjgl3:run --args="... ${BRANCH:+--branch $BRANCH} $*"
  ```

## Change size

1 Kotlin file (`DesktopLauncher.kt`), 3 scripts, 1 line in the root `CLAUDE.md`.

## Overlap

`mega-refactor-031e-bindings` adds 10 lines to `DesktopLauncher.kt` (`--resetBindings`). A textual conflict in the
argument list is possible.
