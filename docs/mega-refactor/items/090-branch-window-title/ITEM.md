# 090: Branch name in the game window title

- Status: planned
- Type: tooling
- Depends on: none
- Related: `008`, `091`
- Branches: none yet

## Goal

When a run script starts the game, the window title is `Megaman Maverick - <branch name>`. Then the owner can see which
worktree each open game window comes from.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A new launcher option, proposed name `--branch <name>`, in `DesktopLauncher.DesktopAppArgs`. With the option, the title
  is `Megaman Maverick - <name>`. Without it, the title stays `Megaman Maverick`, so a packaged build does not change.
- `run-game-alpha.sh` and `run-game-debug.sh` read the current branch from git and pass the option.
- `run-game-test.sh` does the same. The owner did not name this script, but it has the same problem.
- Add the option to the launcher flag list in the root `CLAUDE.md`.

Gameplay does not change.

## Owner's input

2026-10-31: "when running the game via `./run-game-alpha` or `./run-game-debug`, then the game window should have the
title "Megaman Maverick - <branch name>". This makes it so that when I'm working on multiple worktrees at the same time,
then I can distinguish which branch the game session pertains to."

## Open questions

- A detached `HEAD` has no branch name. Proposed: the script leaves out the option, and the title stays
  `Megaman Maverick`.

## Sources

- `NOTES.md` in this folder.
