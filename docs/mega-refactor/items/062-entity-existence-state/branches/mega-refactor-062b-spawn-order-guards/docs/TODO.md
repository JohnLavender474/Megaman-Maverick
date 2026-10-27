# TODO: mega-refactor-062b-spawn-order-guards

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `DESERT_MAN` (`DesertMan_16x14_v4.tmx`) logs `SpawnersLayerBuilder` errors at load: four `Cactus` objects on the
  `blocks` layer resolve to `entities.blocks.Cactus`, and one `Bat` on the `specials` layer to `entities.special.Bat`;
  neither class exists, so those spawners are skipped. Seen in the `062b` launch of `--room room3`; not caused by
  this branch (no map or builder change). To check: whether the objects belong on another layer.
