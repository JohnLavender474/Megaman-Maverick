# 141: The sabre damage by slash kind and by target

- Status: idea
- Type: feature
- Objectives: `O8`
- Depends on: `081`
- Related: `023`, `081`
- Branches: none yet

## Goal

Since `081e`, each target gives X's sabre slash (`MegamanXSabreSlash`) one static damage for every slash kind (floor,
air, wall), for example `D1000.SABRE_DAMAGE = 2` and `dmgNeg(9)` in `SabreBlock` and `MetaWheelF`. A more dynamic rule
can give a different damage for each slash kind and for each target, as X6 does.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Find the X6 sabre damage for each slash kind and each target in the decomp notes.
- A rule that gives the damage from the slash kind and the target. Check if `023` (damage profiles) gives the place
  for it.
- Each target keeps its current damage until the owner chooses the new values.

## Owner's input

The owner decided at the `081e` diff review (2026-11-09) that the sabre does one static damage for every slash kind
for now. A backlog candidate in the `081e` branch `docs/TODO.md`.

## Open questions

- Does this wait for `023`?

## Sources

- `081e` branch `docs/TODO.md`, `docs/NOTES.md` and `process-log.md`.
- The `TODO` at `MegamanXSabreSlash.kt:40`.
