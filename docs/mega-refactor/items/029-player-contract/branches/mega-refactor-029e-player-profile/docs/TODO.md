# TODO: mega-refactor-029e-player-profile

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Max health read through from the profile. Today the player's max health is a stored `Points.max` that the heart-tank listener sets from `GameState`. A read-through max (for example a max supplier on the engine `Points`) would remove the stored copy, so a player who joins late or a profile change never leaves a stale max. Owner request (2026-10-23). `TODO` at the max-health set in `Megaman`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
