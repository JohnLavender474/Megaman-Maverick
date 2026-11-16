# TODO: mega-refactor-011a-game-random

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Moon Scythe fade order (old, also on `master`): `MegamanWeaponsHandler` `postShoot` sorts the scythes right after
  the new shot queues its scythes. The new scythes are still spawn-queued, so their `spawnOrder` (before: `timeSpawned`)
  is from an earlier pooled life or 0, and they sort as the oldest and fade first. Found by the review before the play
  test, 2026-11-14. `TODO` at the sort.

## Backlog candidates (added)

- The guard test does not catch a call with no receiver (`shuffle()` or `random()` inside `with(array)` or an `Array`
  subclass) or a call with an argument (`list.shuffle(rng)`). A detekt `ForbiddenMethodCall` rule would see the types.
