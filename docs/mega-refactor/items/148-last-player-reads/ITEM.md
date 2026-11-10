# 148: The last player reads take `IPlayer`

- Status: idea
- Type: refactor
- Objectives: `O1`, `O8`
- Depends on: none
- Related: `146`, `060`
- Branches: none yet

## Goal

`146a` gave a player the type `IPlayer`, with a `slot` that is never null. Two places still read the slot with
`playerSlot!!` on a type that is not the player type, so the compiler does not know that they need a player.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `core/.../players/PlayerAirDash.kt` `airDashButton()` (receiver `AbstractHealthEntity`): bind it to `IPlayer` and read
  `slot`. The change spreads to the public `airDashInputPressed` and `airDashInputJustPressed`.
- `core/.../entities/megaman/components/ControllerComponent.kt`: the same, after `060a` merges (it changes this file).

## Owner's input

None yet. From the `146a` backlog candidates (2026-11-10).

## Open questions

None.

## Sources

- `146a` `docs/TODO.md` and `docs/NOTES.md`.
