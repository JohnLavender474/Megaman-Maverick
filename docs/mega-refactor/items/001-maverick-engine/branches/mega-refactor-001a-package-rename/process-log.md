# Process log: mega-refactor-001a-package-rename

## Status

- State: merged (2026-10-22)
- Goal: rename the engine root package to a neutral "Maverick Engine" identity.
- Done: branch start; scan; `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` (approved); baseline build; plan steps 1-7.
- Next: none. Branch `001b` continues item `001` later.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- A research agent studied `MegamanMaverickGame`, the engine, the naming and the rename cost.
- Wrote the draft scope and the findings on `master`. No branch was created and no other work
  was started. `docs/CONTRACT.md`, `docs/PLAN.md`, `docs/TODO.md` and `docs/NOTES.md` are still
  the template files.
- Later the same day: the docs moved from `branches/mega-refactor-maverick-engine/` to this
  folder, when the mega-refactor docs changed to one folder per item. The scope now covers only
  the package rename. The host contract moved to `mega-refactor-001b-host-contract`.
- Branch start: `master` commit `f595ec73a`, then created the branch.
- Planning step 1 (scan): 303 files declare the engine package and 11,376 lines import it. No
  runtime string, Gradle file, CI workflow, script or skill names it. Wrong
  `eclipse.project.name` in `engine/build.gradle:2`.
- Planning step 2: rewrote `docs/SCOPE.md` with the scan results. One open question: the exact
  root package name.
- Baseline build with Corretto JDK 17: 427 engine tests and 49 core tests pass; the JAR builds.
  Details in `docs/NOTES.md`. Two earlier runs failed because of the local JDKs (JDK 25, and a
  JDK 17 runtime without a compiler); the JDK 25 problem is a bug candidate in `docs/TODO.md`.
- The user approved `docs/SCOPE.md` and chose the root package `com.maverick.game.engine`.
- Planning step 3: wrote the draft `docs/CONTRACT.md`.
- The user approved `docs/CONTRACT.md`.
- Planning step 4: wrote the draft `docs/PLAN.md`.
- The user approved `docs/PLAN.md`. The branch moved into the worktree
  `.worktrees/mega-refactor-001a-package-rename/`; the main checkout is back on `master`.
- Plan change (steps 1 and 2): with one commit, git paired only 278 of the 304 moved files as
  renames. The other 26 are tiny, so the changed package line is a large share of each file, and
  `git log --follow` (default 50% threshold) would lose their history. So the move became its own
  commit (`2f8ba1cd3`, 304 renames at 100%), followed by the text change (`97f2bc7ca`). The move
  commit does not compile on its own. Both commits are in `.git-blame-ignore-revs`.
- Step 1-2 results: no `com.mega.game.engine` left in `.kt`, `.java` or `.gradle` files; the old
  `com/mega/` folders are gone; the rename commit changes only `package` and `import` lines, the
  3 KDoc links and the 2 fully qualified names in `CullableOnUncontainedTest.kt`.
- Step 3: `eclipse.project.name` is now `appName + '-engine'`; the root `CLAUDE.md` path is
  updated (`93cdd0a60`).
- Step 4, contract automated checks (Corretto JDK 17, clean build): `BUILD SUCCESSFUL`;
  `engine:test` 50 specs, 427 tests, 0 failures; `core:test` 4 specs, 49 tests, 0 failures; the
  JAR builds (35,895,548 bytes); 133 compiler warnings, the same list as the baseline after paths
  are removed. All source checks pass.
- Step 5: `.git-blame-ignore-revs` lists both commits (`071d03dfc`). `git blame
  --ignore-revs-file` credits the import lines of `GameEngine.kt` to `2cb62816c`, and
  `git log --follow` on the tiny `Axis.java` reaches `2cb62816c`.
- Step 6, play test (owner): ran `./run-game-debug.sh` and played part of the intro stage at a
  stable 60-61 FPS. Start, pause and resume work. Contract result: pass.
- Step 7: the owner asked to squash-merge the branch into `master`.
