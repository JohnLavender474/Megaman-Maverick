# TODO: mega-refactor-019c-merge-spawn-queues

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Spatial or room index for spawners (moved out of item `019`). First step: measure the spawner pass
  (`spawnsMan.update`) with `--diagnostics` at `--performance low` or on a weak machine, in a level with many
  spawners. Build the index only if the measure shows a cost. Objective `O5`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
