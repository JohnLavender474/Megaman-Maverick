# 144: View edge warp beside a partner on a ladder

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `127a`
- Branches: none yet

## Goal

Since `127a`, a partner on a ladder counts as standing, but a side spot needs ground under the feet. Beside a ladder there is often no ground, so the warp uses the partner's own spot. The player lands in the ladder without a climb, can fall below the view again and warps again, with the warp damage each time. A `TODO` in `CameraViewEdges.stands` marks it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Land the player on the ladder in the climb state (needs a capability for the climb, `O8-R2`).
- Or wait while the partner climbs, as while the partner is in the air.

## Owner's input

Found by the review before the play test of the `127a` edge cases (2026-11-09), logged as a backlog candidate.

## Open questions

- Which of the proposed fixes, or accept it.

## Sources

- `127a` branch `docs/TODO.md` and `process-log.md`.
