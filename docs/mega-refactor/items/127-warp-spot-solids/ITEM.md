# 127: View edge warp: test the warp spot for solids

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `047g`
- Branches: none yet

## Goal

The `WARP` view edge rule (`047g`) puts the out player 1 tile beside a partner in view, on the side of the view center.
It does not test that spot for solids, so the player can land inside a wall or a platform. A `TODO` in
`CameraViewEdges.kt` marks it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Test the spot for solids; if it is blocked, try the other side, then the partner's own spot.
- Or warp to the partner's last ground position.

## Owner's input

Found by the review before the play test of `047g` (2026-11-06), accepted for later.

## Open questions

- Which fallback: the other side, the partner's spot, or the last ground position?

## Sources

- `NOTES.md` in this folder.
- `047g` branch `docs/TODO.md` and `process-log.md`.
