# Plan: mega-refactor-019a-remove-second-spawn-pass

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-22

All steps run in the worktree `.worktrees/mega-refactor-019a-remove-second-spawn-pass/`.

## Steps

### 1. Baseline

- Changes: none.
- Check: run the contract command with Corretto JDK 17. Write the test counts in `NOTES.md`.
- Done: yes (2026-10-22)

### 2. One spawn pass

- Changes: in `MegaLevelScreen.render()`, remove the `spawnsMan.update(delta / 2f)` call after
  `engine.update(delta)`, and change the call before it to `spawnsMan.update(delta)`. Add one
  `TODO` comment at the remaining call that names the two accepted rare cases and points to
  `019b`.
- Check: the "Source checks" in `CONTRACT.md` (`git diff` shows only these lines).
- Commit: "Run one spawn pass per frame".
- Done: yes (2026-10-22)

### 3. Automated checks

- Changes: none.
- Check: the "Automated checks" in `CONTRACT.md`, compared with the baseline. Write the results
  in the process log.
- Done: yes (2026-10-22)

### 4. Play test (owner)

- Changes: none.
- Check: the "Manual playthrough" and "Behaviors to check" in `CONTRACT.md`.
- Done: yes (2026-10-22)

### 5. Merge

- Changes: follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`, including the owner's
  approval and, if `master` changed, a merge of `master` into the branch and the checks again.
  On `master`, add a branch row for `019b` (the destroy notification) as `planned`.
- Check: `master` builds with the contract command.
- Done: yes (2026-10-22)
