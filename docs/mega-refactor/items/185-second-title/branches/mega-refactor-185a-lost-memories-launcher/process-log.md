# Process log: mega-refactor-185a-lost-memories-launcher

## Status

- State: merged
- Goal: second playable title "Mega Man X: Lost Memories" (splash → X6 intro) on the shared code; run scripts take the game
- Done: all steps, play test, merge
- Next: none (next branch `185b`, builds per game)
- Blocked: none

## Entries

## 2026-11-23

- Branch started. The owner asked for a full first pass with `/fan-out-agents` and
  `/go-with-recs`, and Sonnet agents for the smaller tasks. No HTML brief: the owner gave the go
  and accepted the recommendations; the decisions are in `docs/NOTES.md`.
- Wave 1 (2 agents, parallel): title hook in `core` (Opus), `desktop-common` (Sonnet). Build and
  `core:test` passed. Commit `b5b115f6d` took `desktop-common/build/` by mistake (no ignore
  line); `57e490d3d` adds the ignore lines and untracks it.
- Wave 2 (2 Sonnet agents, parallel): Lost Memories modules; run scripts, CI and root
  `CLAUDE.md`. The first build failed: `lost-memories-core` did not see `engine` types (`core`
  has `engine` as `implementation`); fixed with `api project(':engine')`. Then all automated
  checks passed: `engine:test core:test core:smokeTest lwjgl3:jar lost-memories-desktop:jar
  detektMain core:verifyRandomRule`.
- Small QoL in the branch: root `CLAUDE.md` "Modules" and "Build, run, test" updated for the
  new modules and the game argument of the run scripts.
- Review before the play test: 1 Sonnet agent. 4 findings, no blocker. Fixed: music of the
  level or the game over screen played on under the splash (the splash now stops the music in
  `show()`). Backlog: `generateAssetList` repeats, `--screen` redirect gap. Note for the owner:
  Lost Memories has its own settings files, so `--loadKeyboardPrefs` loads default bindings on
  its first run (decision D8). All 5 manual checks trace as pass.
- Play scripts in `temp/play/` of the worktree.
- Owner request at the handover (D9): one run script set per game, `run-<game>-<mode>.sh`.
  The 4 Maverick scripts renamed with `git mv` and restored to their `master` bodies;
  `run-game-common.sh` removed; 2 Lost Memories scripts (`debug`, `alpha`). Owner request: root
  `README.md` and `CLAUDE.md` updated in the branch. The master-only docs that name the old
  scripts are a merge task (`docs/TODO.md`).
- Play test: the owner reported that the play tests pass (2026-11-23). Diffity: 0 threads.
  Merge approved by that report. `master` had no new commits since the branch start.
