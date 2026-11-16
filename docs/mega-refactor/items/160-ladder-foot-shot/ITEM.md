# 160: Ladder shot height at a ladder foot

- Status: idea
- Type: feature
- Objectives: `O9`
- Depends on: none
- Related: `060`
- Branches: none yet

## Goal

On a ladder the sprite has the air drop (`AIR_SPRITE_OFFSET_Y`), and the ladder shot offset counts it. At a ladder
foot with the feet over the floor the drop is off, so the shot leaves 0.25 tile under the muzzle. This is true for
Proto Man (`ProtomanBuster.kt`, `climbOffset`, `TODO`) and for Megaman. The shot must leave from the muzzle also there.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- The ladder shot offset reads the same sprite drop that the climb sprite uses, for Proto Man and Megaman.

## Owner's input

None yet. Found in `060d` step 7 (backlog candidate, 2026-11-14).

## Open questions

## Sources

- `060d` `docs/NOTES.md`, the shot offsets; `060d` `docs/TODO.md`.
