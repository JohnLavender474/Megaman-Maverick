# 145: View edge warp: the landing after the cap tests only solids

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `127a`
- Branches: none yet

## Goal

After the delay plus the cap, `127a` lands the player on the partner's spot when no solid blocks it there (owner decision, 2026-11-09). The landing does not test `DEATH`, `TELEPORTER` or hurtful `DAMAGER` fixtures at that spot, so the player can land in spikes, an enemy or a teleporter sensor at the partner's spot.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Run the full spot test also for the landing after the cap, and wait on while it fails.
- Or accept it: the partner stands at that spot, so such a fixture there is rare.

## Owner's input

Found by the review before the play test of the `127a` edge cases (2026-11-09), logged as a backlog candidate.

## Open questions

- Which of the proposed fixes, or accept it.

## Sources

- `127a` branch `docs/TODO.md` and `process-log.md`.
