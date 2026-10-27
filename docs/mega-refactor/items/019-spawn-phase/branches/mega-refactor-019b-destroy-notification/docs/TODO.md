# TODO: mega-refactor-019b-destroy-notification

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Merge the four spawn queues (`spawns`, the engine queue, `playerSpawnsMan`, `SPAWN_NOW`): a later branch `019c` of item `019`, not a new item.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `WILY_STAGE_1` (`WilyStage1_v2.tmx`): a `DuoBallCanon` (object `322`, `x=11320, y=1680`) is on the `blocks` layer,
  so `SpawnersLayerBuilder` looks for `entities.blocks.DuoBallCanon`, logs an error at load and skips it (the class is
  in `entities.enemies`; seven other `DuoBallCanon` objects are on the `enemies` layer). The same kind of map problem
  as `docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`. Seen in the `019b` launch of `--room room8`; not
  caused by this branch.
