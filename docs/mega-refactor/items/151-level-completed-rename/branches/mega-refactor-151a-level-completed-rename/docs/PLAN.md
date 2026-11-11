# Plan: mega-refactor-151a-level-completed-rename

## Steps

### 1. Rename the symbols

- Changes: `sed` on the 18 files for the exact identifiers (`levelsDefeated`, `LevelDefeated`, `LevelsDefeated` as
  identifier parts), then a grep for the old identifiers and a grep that the boss vocabulary count is unchanged.
- Check: the grep gives no old identifier; `git diff --stat` lists only the 18 files.
- Done: no

### 2. Delete `allLevelsDefeated` and fix the messages

- Changes: remove the function; fix the 3 "Levels defeated" messages; reword the 2 level strings.
- Check: `./gradlew lwjgl3:jar` and `core:test`.
- Done: no

### 3. Docs

- Changes: `players/README.md` and the root `CLAUDE.md` line on the branch. The open issue and the `007` and `036` notes are top-level docs, so they change on `master` in the merge docs commit (branch rule: docs only inside the branch folder).
- Check: grep of the 4 docs for the old names.
- Done: no

### 4. Automated checks and review

- Changes: none.
- Check: the contract's automated checks; a code-review subagent over the diff.
- Done: no
