# 094: Split screen (`O2` phase 2)

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`, `010`
- Related: `047`, `051`, `016`
- Branches: none yet

## Goal

Two players play on one machine with one view each, and both stay in the same room. This is phase 2 of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Change the one `gameCamera` in `MegaServices` to a set of views.
- Make spawn and cull ask about all views. `overlapsGameCamera` and `getGameCameraCullingLogic` carry much of this; 115 direct lines in 87 entity files need a review.
- One `CameraController` per view. Give scroll deaths, edge blocks and crush surfaces a rule for two views of one room.
- HUD and fades per view.

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
