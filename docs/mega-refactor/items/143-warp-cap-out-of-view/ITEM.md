# 143: View edge warp: the cap does not run while no partner is in view

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `127a`
- Branches: none yet

## Goal

A held player waits for a free partner in view. While no partner is in view (for example the partner is above a `NONE` top edge), `findWarpSpot` returns no spot, and the cap timer of `127a` does not end the wait. The held player waits until the partner comes back into view. A `TODO` in `CameraViewEdges.updateHolds` marks it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Let the cap run also while no partner is in view, and then warp to the partner's last spot in view, or treat the held player as dead.
- Or accept it: the partner comes back into view.

## Owner's input

Found by the diff review before the `127a` play test (2026-11-07), logged as a backlog candidate.

## Open questions

- Which of the proposed fixes, or accept it.

## Sources

- `127a` branch `docs/TODO.md` and `process-log.md`.
