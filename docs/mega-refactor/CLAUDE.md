# Mega refactor

Read this file before you do any work on the mega refactor.

## Goal

Refactor `engine/` and `core/` so that the game is easier to change, configure and extend.
The current gameplay is the contract. A branch merges into `master` only if the game still
plays the same. Each branch writes its own contract in its `docs/CONTRACT.md`.

## Inspiration: the X6 decomp

Some ideas for the refactor come from the Mega Man X6 (PS1) decomp effort:

- `~/IdeaProjects/mmx-demake-tools/`: the tools and docs. `docs/engine.md` describes how the X6
  engine works (camera, spawning, damage, game state). `docs/enemies/` and `docs/stages/` hold
  per-object and per-stage notes.
- `~/mmx-data/x6/`: the data that the tools and their Claude sessions write (dumps, extracted
  files, reverse-engineering notes in `re/`).

Rules:

- Use these sources for ideas and facts only. Do not copy code from
  `~/IdeaProjects/mmx-reference/`, and do not commit any game data from `~/mmx-data/` to this
  repo.
- When a branch uses an idea from X6, name the source (file and section) in the item `NOTES.md`
  or the branch `docs/NOTES.md`.

## Files

| Path | Purpose | Who changes it |
|------|---------|----------------|
| `CLAUDE.md` | This file. Rules for the effort. | `master` only |
| `ITEMS.md` | The index: one table of items and one table of branches, with status and dates. Nothing else. | `master` only |
| `process-log.md` | One entry for each merged or dropped branch, and for each docs change to the setup. | `master` only |
| `_templates/item/` | The files to copy for a new item. | `master` only |
| `_templates/branch/` | The files to copy for a new branch folder. | `master` only |
| `items/<id>-<slug>/ITEM.md` | The item: goal, proposed scope, owner's input, open questions. | `master` only |
| `items/<id>-<slug>/NOTES.md` | Research for the item. | `master` only |
| `items/<id>-<slug>/branches/<branch>/` | The docs of one branch. | `master` before the branch exists, then that branch only |

## Items and IDs

- An item is one feature or change. It can need more than one branch.
- Each item has a 3-digit ID (`001`, `002`, ...) and a short kebab-case slug. The folder is
  `items/<id>-<slug>/`, for example `items/016-narrow-services/`.
- Give a new item the next free ID. Never reuse an ID. A dropped item keeps its ID and folder.
- `000` is reserved for the bug list (see "Bugs").
- When an item is too big or too vague, split it into new items and set the old item to
  `dropped` with a pointer to the new IDs.
- In docs and commit messages, refer to an item by its ID, for example "after `016`".

## Branch names

- Each branch belongs to one item. Its ID is the item ID plus a letter: `001a`, `001b`, ...
- The branch name is `mega-refactor-<id><letter>-<slug>`, for example
  `mega-refactor-001a-package-rename`.
- The branch folder has the same name as the branch:
  `items/001-maverick-engine/branches/mega-refactor-001a-package-rename/`.

## Statuses

Item statuses:

| Status | Meaning |
|--------|---------|
| `idea` | Under discussion. Not agreed yet. |
| `planned` | Agreed, but no branch exists yet. |
| `active` | At least one branch of the item exists. |
| `done` | All branches that the item needs merged. |
| `dropped` | Abandoned. `ITEM.md` gives the reason. |

Branch statuses:

| Status | Meaning |
|--------|---------|
| `planned` | Agreed, but the branch does not exist yet. |
| `blocked` | It waits for other items or branches. `process-log.md` names them. |
| `active` | The branch exists and work is in progress. |
| `merged` | The branch merged into `master`. |
| `dropped` | The branch was abandoned. |

## `ITEMS.md` columns

- **Depends on:** the items that must be done first.
- **Started:** the date the first branch of the item (or the branch) was created.
- **Finished:** the date the item was done or dropped (or the branch merged or dropped).

## Decisions and open questions

Do not put decisions in `ITEMS.md`. An owner decision goes in the "Decisions" section of the
`ITEM.md` it affects, with the date. A decision that affects several items goes in each of them.
An open question goes in "Open questions" of the `ITEM.md`. Once a branch exists, decisions about
that branch's work go in its `docs/NOTES.md` under "Decisions".

## Rules

### New item

Do these steps in a docs-only commit directly on `master`:

1. Copy `_templates/item/` to `items/<id>-<slug>/` and fill in `ITEM.md`.
2. Add a row to the items table in `ITEMS.md`.

### Prepare a branch folder ahead of time

It is good to create a branch folder on `master` before the branch exists, for example to hold
research, a draft `SCOPE.md` or the split of an item into branches. Do it in a docs-only commit
on `master`:

1. Copy `_templates/branch/` to `items/<id>-<slug>/branches/<branch>/`. Replace `<branch>` in the
   files.
2. Set the status block in the branch `process-log.md` to `planned` or `blocked`, and add an
   entry that says no branch exists yet.
3. Add a row to the branches table in `ITEMS.md` with the status `planned` or `blocked`.

A draft `SCOPE.md` written ahead of time still needs the user's approval (see "Planning").

### Branch start

Do these steps in a docs-only commit directly on `master`:

1. If the branch folder does not exist yet, create it as in "Prepare a branch folder ahead of
   time".
2. In `ITEMS.md`, set the branch row to `active` and fill in its start date. If this is the
   item's first branch, set the item to `active` and fill in its start date.
3. In `ITEM.md`, list the branch under "Branches".

Then create the branch from `master` in its own worktree (see "Worktrees").

### Planning

Do these steps in this order, before you change code. Do not start a step until the user
approves the step before it.

1. **Scan.** Read the code that the branch will touch. Find the classes, call sites, events and
   data involved, how they connect, and their problems. Read the item `NOTES.md`. Read the
   folders of the other active branches for overlap.
2. **`docs/SCOPE.md`.** Write what the scan found and what the branch will and will not change.
   The user approves it.
3. **`docs/CONTRACT.md`.** Write the gameplay that the branch must not change and how to check
   it, for example the levels to play through and what to look for. Only include what the scope
   can affect. The user approves it.
4. **`docs/PLAN.md`.** Write the ordered steps to do the work. Give each step its own check. The
   user approves it.

Specs and plans for a `mega-refactor-` branch go in the branch folder, not in
`docs/superpowers/`. This also applies when a skill (for example `superpowers:writing-plans`)
gives a different default path.

If the scope must change during the work, update `docs/SCOPE.md` and get the user's approval
again. Then update `docs/CONTRACT.md` and `docs/PLAN.md` to match. A change to `docs/PLAN.md`
alone does not need a new approval, but write the reason in the process log.

### During the branch

- On the branch, write docs only inside the branch folder. Do not edit `ITEMS.md`, `ITEM.md`,
  the item `NOTES.md` or other top-level files on the branch. Then two active branches cannot
  conflict in the docs.
- Keep the status block at the top of the branch `process-log.md` up to date.
- Add a dated entry to the branch `process-log.md` for each work session.
- Put other docs (design notes, decisions, TODO lists) in the branch `docs/`.
- If you find a new idea that is out of scope, write it in the branch `docs/TODO.md` under
  "Backlog candidates". If you find a bug, write it under "Bug candidates".
- A very small quality-of-life change that is out of scope (for example a run script option or a
  docs line) may go into the current branch, with no new scope approval. It must not change
  gameplay. Write each one in the branch process log. Anything larger, or anything that changes
  gameplay, is a backlog candidate or a bug candidate as above.

### Branch merge

1. Do the checks in the branch `docs/CONTRACT.md`, including the user's play test. Write the
   result in the branch process log.
2. Get the user's explicit approval to merge this branch. Approval for one branch never covers
   another branch, also when branches run in parallel. Merge only one branch at a time. If
   `master` changed since the branch last took it in, compare the two
   (`git diff --stat <branch>...master`):
   - If `master`'s new changes touch the same files or systems as the branch (for example the
     same classes, the build files, or tests that the branch's changes affect), first merge
     `master` into the branch in its worktree, run the contract's automated checks again, and
     tell the user the result.
   - If they do not (for example docs only, or unrelated code), skip that merge. The build check
     on `master` after the squash (step 9) catches problems.
3. Squash-merge the branch into `master`. Do not carry the branch commits over. From the main
   checkout: `git merge --squash <branch>`, then one commit named after the branch goal. List
   the main changes in the commit message.
4. If the branch has a mechanical commit that `.git-blame-ignore-revs` lists (for example a
   rename), replace its branch SHA in that file with the SHA of the squash commit, in a
   separate commit on `master`. Branch SHAs do not exist on `master` after a squash.
5. On `master`, add an entry to `process-log.md`. Use the entry format in that file.
6. On `master`, in `ITEMS.md`, set the branch row to `merged` and fill in its finish date. If
   the item needs no more branches, set the item to `done` and fill in its finish date.
7. Make each "Backlog candidate" from the branch `docs/TODO.md` a new item (see "New item").
   File each "Bug candidate" (see "Bugs").
8. Remove the worktree (`git worktree remove .worktrees/<branch>`). Then delete the branch with
   `git branch -D <branch>`: after a squash, git does not see the branch as merged, so `-d`
   refuses.
9. On `master`, run the contract's automated checks (at least the build and the tests). If they
   fail, fix `master` at once, before any other merge.

Do not delete the branch folder after the merge. It stays on `master` as history.

### Dropped branch or item

- Branch: in `ITEMS.md`, set its row to `dropped` and fill in the finish date. Give the reason in
  the branch `process-log.md`. Add a short entry to `process-log.md`. Move its unfinished work
  back into the item `ITEM.md`.
- Item: set it to `dropped` in `ITEMS.md`, fill in the finish date, and give the reason in
  `ITEM.md`.

## Bugs

Bugs are not refactors. A bug fix changes gameplay on purpose, so it does not belong in a branch
whose contract is "plays the same as `master`".

- Bugs go in `docs/issues/`, one Markdown file per bug (decision A in `items/000-bugs/ITEM.md`). The rules and
  the index are in `docs/issues/README.md`.
- If a refactor branch must change buggy behavior, its `CONTRACT.md` names the bug under
  "Intended changes".

## Worktrees

- The main checkout of the repo always stays on `master`. Docs-only commits to `master` (branch
  start, merge steps, new items, issue files) happen there.
- Each `mega-refactor-` branch is checked out in its own worktree in `.worktrees/<branch>/`. The
  `.worktrees/` folder is git-ignored. Git creates it with the first worktree.
- Create the worktree at branch start, from the main checkout:
  - new branch: `git worktree add -b <branch> .worktrees/<branch> master`
  - existing branch: `git worktree add .worktrees/<branch> <branch>`
- Do all branch work (code, branch docs, builds, tests, play tests) inside the worktree. Each
  worktree has its own `build/` folders, so its first build is a full build. To play-test from
  IntelliJ, open the worktree folder as its own project, or run the run scripts from inside it.
- List the worktrees with `git worktree list`.
- After the merge, remove the worktree and the branch as in "Branch merge".
- Parallel branches are validated and approved one by one. See "Branch merge", step 2.

## Parallel branches

- Before a branch starts and before it merges, read the folders of the other active branches
  (through their worktrees). Look for overlap, for example two branches that change the same
  classes or that plan the same feature. Write each overlap in your branch `docs/NOTES.md`.
