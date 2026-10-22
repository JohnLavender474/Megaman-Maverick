# Mega refactor

Read this file before you do any work on the mega refactor.

## Goal

Refactor `engine/` and `core/` so that the game is easier to change, configure and extend.
The current gameplay is the contract. A branch merges into `master` only if the game still
plays the same. Each branch writes its own contract in `branches/<branch>/docs/CONTRACT.md`.

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
- When a branch uses an idea from X6, name the source (file and section) in the branch
  `docs/NOTES.md`.

## Files

| File | Purpose | Who changes it |
|------|---------|----------------|
| `CLAUDE.md` | This file. Rules for the effort. | `master` only |
| `ROADMAP.md` | Each branch, its goal and its coarse status. | `master` only |
| `BACKLOG.md` | Ideas that are not yet assigned to a branch. | `master` only |
| `process-log.md` | One entry for each branch that merged into `master`. | `master` only |
| `branches/_template/` | The folder to copy when a branch starts. | `master` only |
| `branches/<branch>/` | The docs of one branch. | That branch only |

## Branch names

- Each branch uses the prefix `mega-refactor-`, for example `mega-refactor-player-registry`.
- The branch folder has the same name as the branch: `branches/mega-refactor-player-registry/`.

## Rules

### Branch start

Do these steps in a docs-only commit directly on `master`:

1. Add a row to `ROADMAP.md` with the status `active`.
2. Copy `branches/_template/` to `branches/<branch>/`.
3. If the branch takes items from `BACKLOG.md`, move them out of `BACKLOG.md` and into the
   branch `docs/TODO.md`.

Then create the branch from `master`.

### During the branch

- Write `docs/CONTRACT.md` during planning, before you change code. It lists the gameplay that
  the branch must not change and how to check it, for example the levels to play through and
  what to look for. Only include what the branch can affect.
- Write docs only inside `branches/<branch>/`. Do not edit the top-level files on the branch.
  Then two active branches cannot conflict in the docs.
- Keep the status block at the top of `branches/<branch>/process-log.md` up to date.
- Add a dated entry to `branches/<branch>/process-log.md` for each work session.
- Put other docs (design notes, decisions, TODO lists) in `branches/<branch>/docs/`.
- If you find a new idea that is out of scope for the branch, write it in the branch
  `docs/TODO.md` under "Backlog candidates". Move it to `BACKLOG.md` when the branch merges.

### Branch merge

1. Do the checks in the branch `docs/CONTRACT.md`. Write the result in the branch process log.
2. Merge the branch into `master`.
3. On `master`, add an entry to `docs/mega-refactor/process-log.md`. Use the entry format in
   that file.
4. On `master`, set the row in `ROADMAP.md` to `merged` and fill in the merge date.
5. Move the "Backlog candidates" from the branch `docs/TODO.md` to `BACKLOG.md`.

Do not delete the branch folder after the merge. It stays on `master` as history.

### Dropped branch

If a branch is abandoned, set its row in `ROADMAP.md` to `dropped`, give the reason, and add a
short entry to `process-log.md`. Move its unfinished items back to `BACKLOG.md`.

## Parallel branches

- Use worktrees in `.worktrees/<branch>/`. The `.worktrees/` folder is git-ignored.
- Before a branch starts and before it merges, read the folders of the other active branches
  (through their worktrees). Look for overlap, for example two branches that change the same
  classes or that plan the same feature. Write each overlap in your branch `docs/NOTES.md`.

## Statuses

| Status | Meaning |
|--------|---------|
| `planned` | Agreed, but no branch exists yet. |
| `active` | The branch exists and work is in progress. |
| `merged` | The branch merged into `master`. |
| `dropped` | The branch was abandoned. |
