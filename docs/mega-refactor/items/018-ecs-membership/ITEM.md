# 018: Event-driven system membership; component IDs when needed

- Status: idea
- Type: refactor
- Objectives: `O5`, `O1`
- Depends on: none
- Related: `009`, `017`, `048`, `120`
- Branches: none yet

## Goal

Stop re-checking every entity in every system each frame, and prepare the ECS for many more small components.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

In this order:

- A diagnostics entry for the cost of system bookkeeping (`O5-R2`).
- Remove the per-frame sweep (`GameSystem.kt:88-95`) and the per-frame `ImmutableCollection` allocation. Rewrite `GameSystemHooksTest.kt:101-114`, the only test that depends on the sweep.
- Make `updateSystemMembershipsFor` private. It has 1 caller and no gate, and it crashes if called inside a system update.
- Add a `TODO` at `GameEntity.removeComponent` with the membership rule (see "Decisions").
- Later, if profiling or `048` needs it: integer component IDs, `Bits` masks, array lookup.
- Expected benefit: a small CPU and GC win today; mostly cleaner rules and room for many more components.

## Owner's input

Answer to review item 17 (2026-10-22, verbatim):

> let's discuss this further, particularly what benefits we'll see

## Decisions

- (2026-11-02, owner) The engine does not recompute membership on each component add or remove. No production write changes the membership of a live entity: `WilyFinalBoss.kt:2305,2941` replace a key that is already there. A direct recompute also crashes on nested LibGDX iterators (`GameEngine.kt:212` inside `:240`), and it can lose a member. Details are in `NOTES.md`.
- (2026-11-02, owner) The membership rule: an entity's set of masked component keys is fixed from the end of `onSpawn` to destroy. Replacing an instance of a present key is allowed. Runtime state is a flag in a component. This answers "Will entities add or remove components in the middle of their life?": no.
- (2026-11-02, owner) If a real caller comes, the form is a dirty queue that the engine drains before each system update, added in the branch of that caller (option b2 in `NOTES.md`).
- (2026-11-02, owner) This item deletes the sweep with no replacement, after the diagnostics entry. The gain is not measured yet.

## Open questions


## Sources

- `NOTES.md` in this folder.
