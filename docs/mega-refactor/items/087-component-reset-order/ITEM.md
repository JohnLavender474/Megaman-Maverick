# 087: Reset order for components

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `048`, `084`
- Branches: none yet

## Goal

Make the component reset pass safe when one component's `reset()` writes to another component. Today the result depends on
the order in which `addComponent` ran, and nothing shows that rule.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `GameEngine.destroyNow` runs `reset()` on the components in insertion order.
- `BehaviorsComponent.reset()` runs `end()` of each active behavior, and that code can write to components that have already
  reset.
- Options: a defined reset order for components, or a rule that `reset()` must not write to another component.
- When a fix is in, delete the `PostActionMomentumComponent.reset()` line in `Megaman.onSpawn`, and its `TODO`.

## Owner's input

None yet. This item comes from a backlog candidate of `084c`.

## Open questions

- A defined order, or a rule against writes between components?
- Do other entities have the same case? `084c` checked only the player: the `084d` parts have no writes from behaviors.

## Sources

- `NOTES.md` in this folder.
- `items/084-split-megaman/branches/mega-refactor-084c-movement/docs/NOTES.md`, "Decisions" (2026-10-30, step 7).
