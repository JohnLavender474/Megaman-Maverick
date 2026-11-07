# 129: View edge warp visuals, phase 2

- Status: idea
- Type: feature
- Objectives: `O9`, `O2`
- Depends on: `047`
- Related: `047g`
- Branches: none yet

## Goal

While a player is held at a `WARP` view edge (`047g`), phase 1 draws only a white guide line and circle to the warp
spot, and the held player is out of view. Phase 2 makes the warp readable in play: the held player's sprite floats
back into the view, and bubbles move to the warp spot before the warp.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A sprite of the held player at the view edge, which floats into the view.
- Bubbles along the line to the warp spot.
- Replace the debug line and circle in `CameraViewEdges.drawWarpGuides`.

## Owner's input

Play test of `047g` (2026-11-06): the float, the line and the bubbles, in phases; phase 1 is the debug line and circle.
"Not now" for the held player's visibility in `047g`.

## Open questions

- Art: new sprites for the bubbles, or reuse of an existing effect?

## Sources

- `NOTES.md` in this folder.
- `047g` branch `docs/TODO.md`.
