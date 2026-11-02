# Process log: mega-refactor-004a-entity-smoke-test

## Status

- State: active
- Goal: Headless `smokeTest` suite: build and `init()` every entity class with real atlas data
- Done: planning, all plan steps, automated checks, fault checks, code review, CI
- Next: the owner approves the merge
- Blocked: no

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-01

- Branch start. The owner chose a direct merge into `master`, with no parent branch.
- The branch-start commit was made in `.worktrees/issue-stored-scratch`, because `master` was checked out there and the main checkout was on `compactor-level`.
- Scan done. A throwaway spike (a `smokeTest` suite and one test file, not committed, removed from the worktree) measured the classes that fail and the real cost of PNG decode. Result: 432 classes, 14 throw, 4 more have silent region misses.
- `docs/SCOPE.md` drafted, with three open questions for the owner. The review notes in the item `NOTES.md` have three wrong facts (`MockGL20`, the PNG size, the FreeType need); `SCOPE.md` lists them.
- Bug candidates are in `docs/TODO.md`.
- The owner approved `docs/SCOPE.md` with the recommendation on each open question. `docs/CONTRACT.md` drafted.
- The owner approved `docs/CONTRACT.md` and one push of the branch for the CI check. `docs/PLAN.md` drafted.
- The owner approved `docs/PLAN.md`. Steps 1 to 5 are done.
- The suite has 438 tests: 432 classes, 2 discovery checks and 4 recorder tests. It passes with no `DISPLAY` and no `WAYLAND_DISPLAY`.
- Plan change: `/usr/lib/jvm/java-17-openjdk-amd64` has no compiler, so `lwjgl3:jar` failed on it. The plan now names `~/.jdks/corretto-17.0.13`.
- Plan change: the silent-miss fault uses `GravityBlock`, because `Coin` uses `region!!` and throws. The contract text changed to match, as the plan allows.
- Change from the plan: `core/build.gradle` declares `assets/sprites/sprite_sheets` as an input of the suite. Without it, a changed atlas or PNG leaves the task up to date, which gives a stale pass on a local machine. The missing-PNG fault ran with no `--rerun` to prove it.
- Fault check results, after the review fix. Each fault failed the suite and was reverted:
  - Bad region in `Met`: `enemies.Met: init() threw, missing regions: [Enemies1.txt:Met/RunX]`.
  - Silent miss in `GravityBlock`: `blocks.GravityBlock: missing regions: [Platforms1.txt:nope]`.
  - Bad atlas name: `File not found: sprites/sprite_sheets/ColorsX.txt`.
  - Missing PNG: `Couldn't load file: sprites/sprite_sheets/Colors.png`.
  - Bad constructor in `Coin`: `items.Coin: init() threw`, with the cause in the report.
  - `JetMet` removed from the list: `enemies.JetMet: init() threw, missing regions: [Enemies2.txt:frozen]`.
  - `Met` added to the list: `enemies.Met now passes. Remove it from /known-entity-failures.txt.`
- A fresh reviewer read the branch and found 0 Critical, 0 Important and 5 Minor. I raised one to Important and fixed it: a failure from a throw lost its cause and the region names. The fix was checked with the same two faults before and after. One finding was the process log, written here. The other three are backlog candidates in `docs/TODO.md`.
- The contract line on classpaths is now exact: `gdx-backend-headless:1.6.3` was already on the `core` and `lwjgl3` classpaths through `gdx-tools` in `engine`.
- Automated checks with `--rerun-tasks` and no display: `engine:test` 631, `core:test` 645, `core:smokeTest` 438, 0 failures. `lwjgl3:jar` builds. The `git diff` on the three `src/main` folders is empty.
- The push failed: the session shell has no GitHub credentials (`could not read Username`). A push of this branch sends 108 commits to `origin`: 5 of this branch and 103 of local `master` that `origin` does not have.
- The owner pushed the branch. CI run `41368452607` on `40509d023` passed: `gradle`, `utils-node`, `utils-pytest`. The `gradle` step "Test and build" ran `engine:test core:test core:smokeTest lwjgl3:jar` and passed. I read the step result through the API, not the step log.
