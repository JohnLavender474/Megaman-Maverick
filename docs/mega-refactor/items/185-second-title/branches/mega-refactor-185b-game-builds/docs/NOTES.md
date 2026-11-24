# Notes: mega-refactor-185b-game-builds

## Decisions

- 2026-11-23: build wrappers are named like the run scripts (`build-<game>-<platform>.sh`),
  following the owner's run-script decision of `185a`.
- 2026-11-23: `packr-all-4.0.0.jar` stays tracked; the rest of `game-builds/` is ignored.

## Overlap with other branches

None of the active worktrees change the build scripts or `.gitignore` (checked 2026-11-23).
