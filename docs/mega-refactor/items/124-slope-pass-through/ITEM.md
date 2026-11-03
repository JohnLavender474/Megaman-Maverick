# 124: Slope pass-through from below

- Status: idea
- Type: feature
- Objectives: `O4`
- Depends on: `068`
- Related: `068`
- Branches: none yet

## Goal

A body that jumps or air dashes up into a `GroundSlope` from below does not snap up onto the surface. Today, at the top
of the jump (or at once in a dash, with `vy = 0`), its bottom is inside the triangle, and `MegaCollisionHandler` pushes
it up. A one-way platform (`COLLIDE_DOWN_ONLY`) lets such a body fall back down instead.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Push a body up onto a slope only if its bottom was on or above the surface in the cycle before. This needs state for
  each body (for example the last surface contact).

## Owner's input

- 2026-11-03 (`068a` play test, `jump_onto`): the snap looks janky, but no level is planned that needs Megaman to pass
  up through a slope. A `TODO` at `GroundSlope` names this item's case.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `items/068-slopes/branches/mega-refactor-068a-slopes/docs/TODO.md`.
