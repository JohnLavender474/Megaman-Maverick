# 065: Every `init()` calls `super.init()`

- Status: planned
- Type: refactor
- Depends on: none
- Related: `048`, `064`
- Branches: none yet

## Goal

Every `init()` override calls `super.init()`, so base classes can rely on their `init()` running. Today 91 of the 469
`init()` overrides in `core/src/main` skip it (count on `master` at `df49f9c39`). A base `init()` adds components (for
example `AbstractEnemy.init()` adds Audio, Cullables, Body and Sprites), so an override can skip it on purpose (to avoid
a second component or to build its own) or by an old mistake.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Read each of the 91 overrides and sort them: safe to add `super.init()`; needs a change to the base or the override
  first; skips on purpose.
- The contract proves that each entity has the same components and the same system memberships after `init()` as
  before (for example a test that builds each entity and compares its component classes with a frozen list).
- A permanent test that every `init()` override calls `super.init()`, if the sorting allows it.

## Owner's input

2026-10-27 (verbatim): "I see your point. Ideally, we'll be getting rid of `AbstractEnemy` so that enemies are
identified by components/markers instead of OOP interfaces." and "Sure, a new item to review these."

## Open questions

1. Do this before or as part of `048` (components instead of inheritance), which removes base classes such as
   `AbstractEnemy`?

## Sources

- `NOTES.md` in this folder.
- `064a` scan (`branches/mega-refactor-064a-instance-identity/docs/SCAN.md`).
