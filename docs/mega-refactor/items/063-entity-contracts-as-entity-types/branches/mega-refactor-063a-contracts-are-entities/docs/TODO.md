# TODO: mega-refactor-063a-contracts-are-entities

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `IChildEntity`/`IParentEntity` extend `IGameEntity` (7 and 17 implementers, all entities). No cast depends on it
  today, so no caller in `063a`. Small enough to fold into `063b` in place of a new item.
- `id` and `group` on `IMegaGameEntity`: 4 `(x as MegaGameEntity).id` casts (`Rain.kt:200`, `TubeBeamerV2.kt:208`,
  `Laser.kt:677`, `SmallIceCube.kt:195`) and the `GameEntityUtils.kt` casts would go.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
