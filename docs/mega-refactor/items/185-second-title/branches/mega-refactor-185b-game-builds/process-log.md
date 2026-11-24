# Process log: mega-refactor-185b-game-builds

## Status

- State: merged
- Goal: platform builds for each game into `game-builds/<game>/`; `game-builds/` git-ignored
- Done: steps 1-4, play test, merge
- Next: none
- Blocked: none

## Entries

## 2026-11-23

- Branch started on the owner's request after the `185a` merge. Small branch: steps in
  `docs/SCOPE.md`.
- Steps 1-3 in `5ea1feb8a`. `bash -n` passes on the 7 build scripts.
- Step 4, builds from this worktree with `GAME_BUILDS_DIR` set to the main checkout's
  `game-builds/` (it holds the JREs): `./build-mega-man-x-lost-memories-linux.sh` →
  `game-builds/mega-man-x-lost-memories/Mega-Man-X-Lost-Memories-0.1.0-linux64/` (409M) and
  `.zip` (323M); `./build-mega-man-maverick-linux.sh` →
  `game-builds/mega-man-maverick/Megaman-Maverick-1.0.1-linux64/` and `.zip` (338M). The packr
  configs name the right main classes. `git status` in the worktree is clean. The old builds at
  the top of `game-builds/` are left in place.
- No review agent: the branch changes only shell scripts, `.gitignore` and docs; the builds ran.
- Play test: the owner reported that the play tests of both bundles pass (2026-11-23). Diffity:
  0 threads. Merge approved by that report. `master` had only a docs commit since the branch start.
