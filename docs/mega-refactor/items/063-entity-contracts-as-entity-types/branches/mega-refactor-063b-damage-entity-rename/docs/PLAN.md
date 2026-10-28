# Plan: mega-refactor-063b-damage-entity-rename

- Status: approved
- Approved: 2026-10-27

New API: none. The branch renames two interfaces and adds `IGameEntity` to the supertypes of two others. It adds no
member, function, type or overload.

Temporary test code: none (no runtime path changes; see `CONTRACT.md`). Scratchpad scripts (not committed):
`rename.sh` and `check_rename.py` in
`/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/063b/`.

JDK: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13` (the default JDK 25 does not run Gradle 8.10.1). Build
command for each check below: `./gradlew clean engine:test core:test lwjgl3:jar`, or `./gradlew engine:test
core:test` where a step says so.

## Steps

### 1. Baseline

- Changes: none. On the branch before any code change, run the clean build.
- Check: the build and the jar pass; write the engine and core test counts and the failures (expected 0) in the
  process log; save the unique warning lines (`^w:` lines, line and column removed with
  `sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u`) to `063b-warnings-baseline.txt` in the scratchpad. Also save a mapped
  copy (`063b-warnings-baseline-mapped.txt`) with the word replacements and the two file names applied, for step 5.
- Done: yes (baseline: engine 549, core 192, 0 failures, 88 unique warnings)

### 2. Mechanical rename commit

- Changes, all by `rename.sh`, nothing by hand:
  1. `git mv engine/.../damage/IDamageable.kt engine/.../damage/IDamageableEntity.kt` and the same for `IDamager.kt`
     to `IDamagerEntity.kt`.
  2. In every `.kt` file under `engine/`, `core/` and `lwjgl3/` (main and test) that has a word match, replace the
     word `IDamageable` with `IDamageableEntity` and the word `IDamager` with `IDamagerEntity`
     (`perl -pi -e 's/\bIDamageable\b/IDamageableEntity/g; s/\bIDamager\b/IDamagerEntity/g'`). The word boundary
     keeps `IDamageNegotiator` and other names unchanged.
- `check_rename.py` (input: the commit before and after): it lists the changed paths with `git diff --name-status -M`
  and accepts only `M` on `.kt` files and the two expected renames (or the same pair as `D` plus `A`). For each changed
  file it reads the old content (`git show <before>:<old path>`), applies the same two word replacements, and asserts
  that the result is byte-equal to the new content. It also asserts that the number of changed files is the scan count
  (170) and that the total number of replacements is 484. Any other difference makes it exit non-zero.
- Check: `check_rename.py` passes on the staged change and again on the commit; `engine:test core:test` pass on the
  commit; greps find no word `IDamageable` or `IDamager` in `engine/`, `core/` or `lwjgl3/`, and `E/damage/` holds
  only `IDamageableEntity.kt` and `IDamagerEntity.kt`.
- Commit (each commit message ends with the `Co-Authored-By` line): "Rename IDamageable/IDamager to IDamageableEntity/IDamagerEntity (mechanical)", with no other change.
- Done: yes (`f555536eb`)

### 3. `.git-blame-ignore-revs`

- Changes: add a comment line and the full SHA of the step 2 commit to `.git-blame-ignore-revs`, in the format of the
  existing `001a` entry.
- Check: `git blame --ignore-revs-file .git-blame-ignore-revs` on a renamed import line (for example in
  `DamageRule.kt`) shows a commit before step 2; without the flag it shows the step 2 commit.
- Commit: "Ignore the 063b damage contract rename in git blame".
- Done: yes (`e34950842`)

### 4. `IChildEntity` and `IParentEntity` extend `IGameEntity`

- Changes: `interface IChildEntity : IGameEntity` and `interface IParentEntity<T> : IGameEntity` in
  `E/entities/contracts/` (add the `IGameEntity` import to `IParentEntity.kt`; `IChildEntity.kt` already imports it).
  New permanent engine test `engine/src/test/kotlin/com/maverick/game/engine/entities/EngineEntityContractsTest.kt`
  (Kotest `DescribeSpec`, as `AnyGameEntityTest`): both are subtypes of `IGameEntity`
  (`IGameEntity::class.java.isAssignableFrom(...)`).
- Check: `engine:test core:test` pass; the engine count goes up by 2.
- Commit: "IChildEntity and IParentEntity extend IGameEntity".
- Done: yes (`1425c215e`)

### 5. Contract checks

- Changes: none in code. Re-read `064a` (`.worktrees/mega-refactor-064a-instance-identity`) for overlap and update
  `docs/NOTES.md`.
- Check: every automated check in `CONTRACT.md`: clean build and jar; test counts = step 1 + 2 engine, core the same,
  0 failures; the warning set equals `063b-warnings-baseline-mapped.txt` (each new warning listed in the process log);
  `check_rename.py` on the step 2 commit; the greps; the `git blame` check; `contact-rule-selection.txt` unchanged
  (`git diff master -- core/src/test/resources/contact-rule-selection.txt` is empty); the review items. Write the
  results in the process log. Then stop and ask for the owner's play test with the manual checks in `CONTRACT.md`.
- Done: yes

### 6. After the play test

- Changes: no temporary suite to delete. Write the play test result in the process log.
- Check: the automated checks of step 5 again on a clean build; the process log and its status block are up to date.
  Stop. No merge, no push, no change to `master`.
- Done: yes (play test passed; checks again pass)
