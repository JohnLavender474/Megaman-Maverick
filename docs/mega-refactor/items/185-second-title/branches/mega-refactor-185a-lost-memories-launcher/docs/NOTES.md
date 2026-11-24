# Notes: mega-refactor-185a-lost-memories-launcher

## Decisions

All 2026-11-23, owner accepted the recommendation (`/go-with-recs`):

- D1 Merge target: directly into `master`. The branch is safe to ship alone.
- D2 Item open question 1 (start before or after `081`): start now (owner asked for it).
- D3 Item open question 2 (mixed atlases): the first pass runs from the root `assets/` and ships
  no assets of its own. Re-packing waits for step 4 of the item.
- D4 Item open question 3 (package name): `core` keeps `com.megaman.maverick.game`. The new game
  uses `com.megaman.lostmemories`.
- D5 The game keeps `MegamanMaverickGame` as the host class for both titles; an `IGameTitle`
  supplies what differs. The host contract `001b` replaces this later.
- D6 Run scripts keep their names (item decision of 2026-11-23) and take the game as an optional
  first argument: `maverick` (default) or `lost-memories`. Replaced by D9.
- D9 (owner, 2026-11-23, at the play-test handover): one script set per game, named
  `run-<game>-<mode>.sh`, each with its own flags, because the flags of the two games can drift
  apart. Scripts: `run-mega-man-maverick-{debug,alpha,test,test-coop}.sh` (the old `run-game-*`
  scripts, renamed with `git mv`) and `run-mega-man-x-lost-memories-{debug,alpha}.sh`. No game
  argument and no shared helper. The owner's example was `run-mega-man-maverick-game-debug.sh`;
  the names leave out `game`, as the pattern `run-<game>-<mode>` gives.
- D7 Gradle project names `desktop-common`, `lost-memories-core`, `lost-memories-desktop`; the
  folders are `desktop-common/` and `games/mega-man-x-lost-memories/{core,desktop}/`. Unique
  project names avoid a second project named `core`.
- D8 Lost Memories uses its own save file and player settings file names.

## Overlap with other branches

Active worktrees on 2026-11-23: `060k` (Protoman frames), `078f` (Timber Woman art),
`compactor-level`, two `fix/` branches. Only `fix/auto-perf-freeze` touches `MegamanMaverickGame.kt` (the auto-performance code, other hunks). None touch the launcher, the Gradle files or the start code (to check again before the merge).
