# TODO: mega-refactor-064a-instance-identity

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `GameSystem.reset()` clears `entities` and `entitiesToAdd` but not `entitiesToRemove`. A remove deferred during a
  system's `update`, followed by `engine.reset()` and a new spawn of the same entity, would drop the live entity from
  that system at its next `update`. Not reached today (no code adds or removes a member during the system's own
  `update`).
- `MutableOrderedSet.retainAll` iterates its argument, not itself: already filed as
  `docs/issues/2026-10-22-mutable-ordered-set-retain-all.md` (no new item).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `SpiderWeb.onDestroy` (`core/.../entities/projectiles/SpiderWeb.kt:118-132`) does not call `super.onDestroy()`. So a
  destroyed `SpiderWeb` stays in `MegaGameEntities` (tag, type and id lookups still return it), its
  `runnablesOnDestroy` never run (for a pooled entity this includes the pool return; not checked how `SpiderWeb` is
  fetched), its
  negative `id` is never freed, and `AbstractProjectile.onDestroy` does not clear `owner`. Found in the `064a` scan
  (2026-10-27, `ce10fc4b1`). Owner (2026-10-27): a `fix/` branch after `064a` adds the call, with a permanent test
  that every `onDestroy()` override calls `super.onDestroy()`.
