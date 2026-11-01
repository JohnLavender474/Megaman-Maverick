# 091: Check that a test room loads with no visible game window

- Status: planned
- Type: tooling
- Objectives: `O6`
- Depends on: none
- Related: `003`, `006`, `008`, `090`
- Branches: none yet

## Goal

Claude must be able to check that a test room loads with one command that shows no game window, plays no sound, and ends
by itself with a clear pass or fail. Today Claude starts the real game, and `timeout` kills it. Each launch opens a window
on the owner's desktop, also while the owner plays another worktree.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. `NOTES.md` compares the options. The recommendation is
option A.

- A new launcher option that opens the real game with a hidden window (`setInitialVisible(false)`) and no sound.
- In this mode, the game exits by itself. Exit code 0 when `PLAYER_READY` comes for the room. A non-zero code for an
  exception, or when `PLAYER_READY` does not come before a time limit.
- A command that gives the exit code to the caller, for example `./check-rooms.sh --level REFACTOR_TEST_051 --room a,b`.
  The `lwjgl3:run` task cannot do this, because it ignores the exit value.
- Change the rule in "Test maps" in `docs/mega-refactor/CLAUDE.md`, and the `level-builder` skill if needed, so that Claude
  and subagents use this command in place of a launch.

Gameplay does not change.

## Owner's input

2026-10-31: "Claude has a habit right now of launching the game window while it builds "test rooms" in order to ensure
they compile, which becomes annoying when I'm testing over worktrees at the same time. Let's file another fix/issue to
make it so that it's easier for Claude to validate that test rooms compile without having to launch and kill a game
window repeatedly."

## Open questions

- Does the game loop render and update when the window is hidden? Check this first; option A needs it (`NOTES.md`).
- Does a `GameLogger.error` line fail the check? A wrong entity name only logs an error, so "no exception" does not find
  it. But `WILY_STAGE_1` logged an `ERROR` line in a smoke launch, so this rule can fail a real level. `008` also lists
  `--strictErrors`.
- One JVM for all the rooms of a run, or one launch per room? `--room` is used one time per process.
- Does a call with no `--room` check every room that has a player spawn?

## Sources

- `NOTES.md` in this folder.
