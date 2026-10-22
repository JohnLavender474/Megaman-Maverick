# 018: Event-driven system membership; component IDs when needed

- Status: idea
- Type: refactor
- Depends on: none
- Related: `009`, `017`, `048`
- Branches: none yet

## Goal

Stop re-checking every entity in every system each frame, and prepare the ECS for many more small components.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Remove the per-frame `removeAll { !qualifies }` and the per-frame `ImmutableCollection` allocation.
- Refresh membership when a component is added or removed on a spawned entity.
- A diagnostics entry for the cost of system bookkeeping.
- Later, if profiling or `048` needs it: integer component IDs, `Bits` masks, array lookup.
- Expected benefit: a small CPU and GC win today; mostly cleaner rules and room for many more components.

## Owner's input

Answer to review item 17 (2026-10-22, verbatim):

> let's discuss this further, particularly what benefits we'll see

## Open questions

- Will entities add or remove components in the middle of their life (for example a "frozen" component)?

## Sources

- `NOTES.md` in this folder.
