# Plan: mega-refactor-016e-service-names

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "plan approved")

The rename scripts from the scan (`tok.py`, `rename.py`, `check.py`, `norm.py`, `members.py`) are in the session
scratchpad `/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/016e/`.
They are not committed (as in `063b`). The file lists that they read are made again from the branch code in step 3, not
taken from the dry run.

New API and first caller: none (`SCOPE.md`).

## Steps

### 1. Baseline

- Changes: none. On the branch at `c83a447ee` (code equal to the parent `ffb8741a0`): run the clean build, save the
  build log, the unique warning set and the `javap -c -p` output of every `core` main class.
- Check: build passes; engine 575, core 272, 0 failures; 81 unique warnings.
- Done: yes

### 2. Pre-step commit: the `queueAssets()` local

- Changes: in `MegamanMaverickGame.queueAssets()`, the local `assets` → `queuedAssets` (6 lines). One commit, not
  mechanical.
- Check: `git show --stat` is 1 file, 6 lines changed; `core:compileKotlin` passes; the `javap -c -p` of
  `MegamanMaverickGame` equals the baseline.
- Done: yes

### 3. Mechanical commit: the rename

- Changes: make the file lists again from the branch code (services files, host files) and compare them with the scan
  lists; then run `rename.py`: `game` → `services` in the services files; `eventsMan` → `events`, `assMan` →
  `assets`, `audioMan` → `audio`, `state` → `gameState` on `MegaServices` and on the host, and on every read on a
  `services` or host `game` receiver; the `testServices(...)` parameters and the 3 named arguments. Code tokens only.
  Nothing else goes in this commit.
- Check:
  - `check.py`: each changed line is the old line with only the listed token renames; the file and line counts match
    the dry run (532 files, 2,038 lines) or the difference is written in the process log.
  - The greps of the contract (no old member read, no `game` left in a services file, the `game` tokens that stay are
    all host).
  - Clean build: passes; engine 575, core 272, 0 failures.
  - Warnings: the same set as step 1 after the name map.
  - `javap -c -p` of every `core` main class: equal to step 1 after `norm.py`.
  - `contact-rule-selection.txt`: no diff.
- Done: yes

### 4. `.git-blame-ignore-revs`

- Changes: one commit that adds the SHA of the step 3 commit, with a comment line in the format of the existing
  entries.
- Check: `git blame --ignore-revs-file .git-blame-ignore-revs` on one renamed entity line shows a commit before step 3.
- Done: yes

### 5. Root `CLAUDE.md`

- Changes: one docs commit. The lines that name the old members or say "Inside an entity it is still named `game`"
  (`CLAUDE.md:49`, `:56`, `:61`, `:86`, `:99-101`, `:119`, and any other line that the step 3 greps show) use the new
  names.
- Check: `grep -nE "assMan|eventsMan|audioMan|still named" CLAUDE.md` finds only lines that are about the host's
  launcher text or none.
- Done: yes

### 6. Contract checks and owner sanity pass

- Changes: none in code. Run every automated check of `CONTRACT.md` on the branch head, including the 3 smoke
  launches; write the results in `CONTRACT.md` and the process log. Then ask the owner for the short sanity pass.
- Check: all automated boxes ticked; the owner's result in the process log.
- Done: yes (owner: "approved")
