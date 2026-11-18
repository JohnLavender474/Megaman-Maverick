# 170: `UP` and `DOWN` gates in co-op

- Status: idea
- Type: feature
- Objectives: `O2`, `O9`
- Depends on: `047`
- Related: `047` (branch `047k`)
- Branches: none yet

## Goal

Make vertical gates (`UP`, `DOWN`) play as the owner wants with 2 players. Until this item is done, levels for co-op
have no vertical gate (`docs/ways-of-working/levels.md`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- State at the end of `047k` (2026-11-18): in co-op, a `GateBlock` (`entities/blocks/GateBlock.kt`) on the top half of
  each `UP` gate holds a player who waits for the partner, and the `GATE` fixture of a vertical gate is 3 tiles wide
  (1.5 in single-player). The pan of a vertical gate moves every player to one shared point. `TODO`s at `Gate` (the
  fixture size) and at `GateBlock.canSpawn` (co-op only).
- The `GateBlock` objects are at `RefactorTest047.tmx` room `coop_gate_down`, Wily 1 gate 432 and Wily 2 gates 347
  and 492.
- `047k` backlog candidate: a `GateBlock` in single-player, if a later stage wants a player to stand on top of a closed
  `UP` gate.

## Owner's input

- 2026-11-18 (`047k` play test): "The UP/DOWN gates still aren't exactly how I'd like them to be in co-op, but other
  checks pass, so we'll concede this as a TODO for now. In the meantime, levels compatible with co-op should avoid
  UP/DOWN gates."
- 2026-11-18: a full-width `GATE` fixture in single-player made the gate look as if it opened too early while the
  player fell in.

## Open questions

- What does the owner want to change in co-op vertical gates? Not written yet.

## Sources

- `NOTES.md` in this folder.
- `047k` branch folder: `docs/SCOPE.md` (D8, D9), `process-log.md` (2026-11-18).
