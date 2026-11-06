# TODO: mega-refactor-038a-respawn-routine

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `DesertMan_16x14_v4.tmx`: `Bat` 616 is in the `specials` layer. `entities.special.Bat` does not exist, so the spawner logs an error and is skipped. Exists on `master`.
- `docs/issues/2026-10-22-respawn-stale-room-music.md` says no live room has `music`; every `SMB3_1` room has it. This branch removes the block; at the merge, close the issue with that correction.
