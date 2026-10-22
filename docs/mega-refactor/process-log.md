# Process log

Work that went into `master` for the mega refactor. Change this file only on `master`. Add one
entry for each merged or dropped branch, and for each docs-only change to the setup. Put the
newest entry at the bottom.

## Entry format

```
## YYYY-MM-DD — <branch or "master (docs)">

- Summary: what changed and why.
- Contract check: which checks in the branch `docs/CONTRACT.md` were done, and the result.
- Follow-up: new items added to `ITEMS.md`, bugs filed, or open questions.
```

## Entries

## 2026-10-22 — master (docs)

- Summary: set up the docs for the mega refactor in `docs/mega-refactor/`. Added the root
  `CLAUDE.md` with a pointer to this folder, and added `.worktrees/` to `.gitignore`.
- Contract check: not needed (docs only).
- Follow-up: fill in the levels and behaviors in `CONTRACT.md`. Pick the first branch from
  `BACKLOG.md`.

## 2026-10-22 — master (docs)

- Summary: removed the shared `CONTRACT.md`. Each branch now writes its own
  `docs/CONTRACT.md` during planning, before it changes code. Added `CONTRACT.md` to
  `branches/_template/docs/`.
- Contract check: not needed (docs only).
- Follow-up: pick the first branch from `BACKLOG.md`.

## 2026-10-22 — master (docs)

- Summary: added the X6 decomp (`mmx-demake-tools`, `~/mmx-data/x6`) as a source of ideas in
  `CLAUDE.md`. Added the X-style camera bounds item to `BACKLOG.md`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: expanded the camera item in `BACKLOG.md`. It now includes auto-scroll, the X6 facts
  for camera zones and `AutoScroll`, and the research questions to answer before the design.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: added a planning phase for each branch: scan, then `SCOPE.md`, `CONTRACT.md` and
  `PLAN.md`, each approved by the user before the next. Added `SCOPE.md` and `PLAN.md` to the
  branch template. Removed the "In scope" section from the template `TODO.md`. Branch specs and
  plans go in the branch folder, not in `docs/superpowers/`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: added `branches/mega-refactor-maverick-engine/` with the research findings and a
  draft scope for two parts: rename the engine package to a "Maverick Engine" identity, and add
  the engine host contract (`IMaverickGame` and `MaverickGame`). No branch was created, and no
  further work was started.
- Contract check: not needed (docs only).
- Follow-up: the user answers the open questions in the branch `docs/SCOPE.md` and approves the
  scope.

## 2026-10-22 — master (docs)

- Summary: changed the docs to one folder per item. `BACKLOG.md` and `ROADMAP.md` are replaced
  by `ITEMS.md`, an index of every item and branch with status, start and finish dates, and the
  open decisions. Items have 3-digit IDs; branches are `mega-refactor-<id><letter>-<slug>`.
  Created 56 item folders (`000`–`055`) from the old backlog and the review of 46 candidates.
  Each `ITEM.md` has the goal, the proposed scope, the owner's answers (verbatim) and the open
  questions; each `NOTES.md` has the review text. `000` lists the bugs until a bug tracker is
  chosen. The Maverick Engine docs moved to `items/001-maverick-engine/`, split into branch
  folders `001a` (package rename) and `001b` (host contract). Branch folders may now be created
  on `master` ahead of time. Templates moved to `_templates/item/` and `_templates/branch/`.
- Contract check: not needed (docs only).
- Follow-up: the user answers the open decisions in `ITEMS.md` (A–O), and the `idea` items are
  discussed.

## 2026-10-22 — master (docs)

- Summary: recorded the owner's answers to decisions A, B, E (claw), G, H, I, J, K, M and O in
  `ITEMS.md` and in the "Decisions" section of each affected `ITEM.md`. Added open decision P
  (one project or a separate engine). Added the decision K research to `019` and created the
  branch folder `mega-refactor-019a-remove-second-spawn-pass` ahead of time. `001a` is blocked by
  P. The root `CLAUDE.md` now describes the engine as an engine for Mega Man-like games.
- Contract check: not needed (docs only).
- Follow-up: decisions C, D, F, L, N and P; the open question in the `019a` draft scope.

## 2026-10-22 — master (docs)

- Summary: recorded decisions P (one repo, keep the module split, no copyrighted assets in the
  engine), L (no publishing) and N (keep `GameEngine`). Folded the layer-agnostic spawning steps
  into `021`, `022` and `048`. Moved the bug list to `docs/issues/` (25 files and a README with
  the rules and an index) and set `000` to `done`.
- Contract check: not needed (docs only).
- Follow-up: start branch `001a` as the first practice round of the branch process.

## 2026-10-22 — master (docs)

- Summary: added a "Worktrees" section to `CLAUDE.md`. The main checkout stays on `master`, and
  each `mega-refactor-` branch works in its own worktree in `.worktrees/<branch>/`, with the
  commands to create, list and remove one. Branch `001a` moved into a worktree.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: the merge rule now says to squash-merge `mega-refactor-` branches into `master`, to
  move any `.git-blame-ignore-revs` entry to the squash commit, and to delete the branch with
  `git branch -D` after the worktree is removed.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — mega-refactor-001a-package-rename

- Summary: renamed the engine root package from `com.mega.game.engine` to
  `com.maverick.game.engine` (304 files moved, about 11,400 import lines changed), fixed the
  engine Eclipse project name, and added `.git-blame-ignore-revs`. Squash commit `1a61734b5`.
  This was the first practice round of the branch process. During the work, the plan changed:
  the move became its own commit so that every file was a 100% rename. The squash merges them
  again, so for 26 tiny files `git log --follow -M20%` is needed to see their older history.
- Contract check: all automated checks matched the baseline (427 engine and 49 core tests pass,
  the JAR builds, the same 133 compiler warnings); all source checks passed; the owner's play
  test passed (intro stage at 60-61 FPS; start, pause and resume work).
- Follow-up: filed `docs/issues/2026-10-22-jdk25-test-jvm-target.md`. Item `001` continues
  with `001b` (blocked by `016`, `010`, `032`).

## 2026-10-22 — master (docs)

- Summary: removed the "Decisions" section from `ITEMS.md`, so it holds only the items table and
  the branches table. Every decision was already in the "Decisions" section of the `ITEM.md` it
  affects; the open parts of decisions C and F moved into "Open questions" of `003` and `026`.
  `CLAUDE.md` now explains the `ITEMS.md` columns and where decisions and open questions go.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: the merge rule now requires the user's explicit approval for each branch, one merge at
  a time; a branch merges `master` in and runs its checks again if `master` changed since it
  started. Started wave 1: `019a` (remove the second spawn pass), `002a` (CI) and `008a` (dev
  launch options), each in its own worktree. The owner accepted the `019a` rare cases.
- Contract check: not needed (docs only).
- Follow-up: plan each wave 1 branch.

## 2026-10-22 — mega-refactor-008a-dev-launch-options

- Summary: added the dev launch options `--screen`, `--level` (staged when the screen is not the
  level screen), `--room`, `--logTags` and `--autoPerf`, with checks that exit with code 2 and the
  usage. `LaunchOptions` is a plain data class; `resolveLaunchOptions` checks and resolves it (the
  owner asked for this split during the work). `--runType test` is a deprecated alias. Fixed the
  `--logLevels` whitespace parse. Squash commit `c731cf9ab`.
- Contract check: clean build; 427 engine and 77 core tests pass (28 new: 15 for the resolver, 13
  for `PlayerSpawnsManager`); 133 compiler warnings as before; the error runs checked by me; the
  owner's play test passed after one fix (`--room` lost its start spawn to spawn `0` on the first
  frame, because `startAt()` put spawn `0` back).
- Follow-up: the launcher part of `docs/issues/2026-10-22-literal-whitespace-regex.md` is fixed;
  the `TODO` in `PlayerSpawnsManager.startAt()` must be revisited in `036`. Later `008` branches
  are listed in `items/008-dev-launch-options/ITEM.md`.

## 2026-10-22 — mega-refactor-019a-remove-second-spawn-pass

- Summary: removed the second `spawnsMan.update(delta / 2f)` after `engine.update`; the remaining
  pass gets the full `delta`. Research (decision K) showed the second pass never gave a
  same-frame spawn and no live entity needed it. A `TODO` names the two accepted rare cases.
  Squash commit `21df34661`.
- Contract check: clean build; 427 engine and 77 core tests pass; 133 compiler warnings as
  before; the owner's play test passed (Desert Man `room13`/`room14`, `room10`; Wily Stage 1
  `room8`, `room3`/`room4`).
- Follow-up: `019b` (destroy notification) is a `planned` branch. The owner's play test showed the
  old misplaced-object errors, filed as `docs/issues/2026-10-22-misplaced-tmx-objects.md`.

