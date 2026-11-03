# 123: X6 flat step-down off a slope

- Status: idea
- Type: feature
- Objectives: `O4`, `O8`
- Depends on: `017`, `068`
- Related: `068`
- Branches: none yet

## Goal

A body that walks off a slope onto a flat floor up to 8 X6 px (1/4 tile) lower keeps its ground, as in X6: no fall
pose and no land sound. Today only the one-step drop at a slope's low end is handled (`GroundSlope.hitByFeet`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A ground probe after `resolveCollisions`, for a body whose last floor was a slope. It needs a world-step hook, so it
  goes after `017`.

## Owner's input

- 2026-11-03: deferred from `068a` (the branch adds no engine step).

## Open questions

## Sources

- `NOTES.md` in this folder.
- `items/068-slopes/NOTES.md`, "X6 slopes" (X6 `resolveFloor`).
- `items/068-slopes/branches/mega-refactor-068a-slopes/docs/TODO.md`.
