---
status: open
verified: yes
area: core
gameplay-change: no
found-by: fix/per-frame-allocations-hot-paths (2026-11-10)
related-items: []
---

# The lookups of `MegaGameEntities` give out mutable sets, and the empty set is guarded only against `add`

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/MegaGameEntities.kt`: `getOfTag`, `getOfType`, `getOfId`, and
the field `noEntities` (class `NoEntities`). A `TODO` at `noEntities` points to this file.

## What happens

The 3 lookups return `OrderedSet<MegaGameEntity>`, a mutable type.

- **A key with entities:** the caller gets the live set of the index. A caller that adds to it or removes from it
  changes the index.
- **A key with no entities:** before `fix/per-frame-allocations-hot-paths`, each miss allocated a new empty
  `OrderedSet`. The fix returns one shared set, `noEntities`, so that a miss allocates nothing. The shared set
  overrides only `add`, which throws. The other mutators of `OrderedSet` are not overridden.

No caller writes to a returned set today. But the type does not stop a caller from doing it, and an error shows only
at run time, and only through `add` on a miss.

## What should happen

A caller cannot change the index through a lookup, and the compiler catches an attempt. A miss still allocates
nothing.

## How to reproduce

Read the code. `MegaGameEntitiesTest` shows the shared set and the `add` guard.

## Notes

- About 45 call sites in `core/src/main` use the result. They read `size`, `isEmpty`, `first()`, `firstOrNull()`,
  `random()`, `forEach`, `any`, `none`, `filter` and `map`. A new return type must give these, or the call sites
  change.
- Returning `null` for a miss was considered and rejected: almost every call site treats a miss as an empty set, so
  each one would need its own default.
- Options to look at: a read-only interface or view type (with no allocation per call), or functions on
  `MegaGameEntities` that cover the common reads (count, any, for-each) so that callers do not hold the set.
