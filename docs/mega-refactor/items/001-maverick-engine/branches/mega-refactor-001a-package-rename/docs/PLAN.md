# Plan: mega-refactor-001a-package-rename

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-22

All steps run in the worktree `.worktrees/mega-refactor-001a-package-rename/`. The main
checkout stays on `master`.

The rename commit (steps 1 and 2) holds only the package rename, so that
`.git-blame-ignore-revs` can skip it without hiding any other change. The other changes go in
separate commits.

## Steps

### 1. Move the package folders

- Changes: `git mv engine/src/main/kotlin/com/mega/game/engine engine/src/main/kotlin/com/maverick/game/engine`,
  and the same for `engine/src/test/kotlin/`. Remove the empty `com/mega/` folders.
- Check: the `com/mega/` folders no longer exist; `git status` shows only renames.
- Done: yes (2026-10-22)

### 2. Replace the package name in all sources

- Changes: replace `com.mega.game.engine` with `com.maverick.game.engine` in every `.kt` and
  `.java` file under `engine/`, `core/` and `lwjgl3/` (package lines, imports, KDoc links and
  fully qualified names). This is a repeated pattern in about 760 files, so a script does it:
  `git grep -l 'com\.mega\.game\.engine' -- '*.kt' '*.java' | xargs sed -i 's/com\.mega\.game\.engine/com.maverick.game.engine/g'`.
- Check: `git grep -n 'com\.mega\.game\.engine' -- '*.kt' '*.java' '*.gradle'` finds nothing. The
  diff filter from the contract shows no changed line other than `package`, `import`, the 3 KDoc
  links and the 2 fully qualified names.
- Commit: step 1 is its own pure-move commit (`2f8ba1cd3`), and step 2 is the rename commit
  (`97f2bc7ca`). Changed from one commit on 2026-10-22; see the process log.
- Done: yes (2026-10-22)

### 3. Fix the Eclipse project name and the docs path

- Changes: `eclipse.project.name = appName + '-engine'` in `engine/build.gradle:2`. The engine
  path in the root `CLAUDE.md` module table becomes `engine/src/main/kotlin/com/maverick/game/engine/`.
- Check: `git grep -n 'com/mega/game/engine' -- CLAUDE.md engine core lwjgl3` finds nothing.
- Commit: "Fix the engine Eclipse project name and docs path".
- Done: yes (2026-10-22)

### 4. Run the automated checks

- Changes: none.
- Check: run the command in `CONTRACT.md` "Automated checks" with Corretto JDK 17. Compare with
  the baseline in `NOTES.md`: 427 engine tests and 49 core tests pass, the JAR builds, 133 or
  fewer `w:` lines. Do the "Source checks" in `CONTRACT.md`, including `git show --stat -M` on
  the rename commit. Write the results in the process log.
- Done: yes (2026-10-22)

### 5. Add `.git-blame-ignore-revs`

- Changes: create `.git-blame-ignore-revs` at the repo root with a comment line and the full SHA
  of the rename commit.
- Check: `git blame --ignore-revs-file .git-blame-ignore-revs` on a renamed engine file shows
  the older commits on its import lines, not the rename commit. GitHub reads this file without
  setup. Locally, `git config blame.ignoreRevsFile .git-blame-ignore-revs` turns it on (an owner
  choice, not committed).
- Commit: "Ignore the engine package rename in git blame".
- Done: yes (2026-10-22)

### 6. Play test (owner)

- Changes: none.
- Check: the owner does the "Manual playthrough" and "Behaviors to check" in `CONTRACT.md`. The
  owner may need to reload the Gradle project in IntelliJ first. Write the result in the process
  log.
- Done: yes (2026-10-22)

### 7. Merge

- Changes: follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`: merge into `master`; on
  `master`, add the `process-log.md` entry, set the `001a` row to `merged` with the finish date,
  move the bug candidate from `TODO.md` to a file in `docs/issues/`, and update the engine paths
  in the existing issue files.
- Check: `master` builds with the contract command.
- Done: yes (2026-10-22)
