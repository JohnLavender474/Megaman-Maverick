# Notes: mega-refactor-060a-shared-run-body

## Decisions

## Overlap with other branches

## Overlap: `146` (marker `IPlayer`)

2026-11-10: the owner starts `146` in a separate session. `146` changes the player type at the call sites
(`asPlayer`, functions that take `MegaGameEntity`), and it lists `Megaman` and `MegamanX` as implementers. The new
`players/` functions of this branch (`playerRun`, `playerRunButton`, `playerBody`) take a generic receiver bounded by
`AbstractHealthEntity`, `IBodyEntity`, `IFaceable`, `IDirectional` (and `IScalableGravityEntity`), not
`MegaGameEntity`, so they are not among the call sites that `146` lists. If `146` merges first: merge `master` into this
branch, run the automated checks, and change the bounds to its player type where it fits.

## `PlayerGravityComponent` has no `require` reader yet

The review after the `146` merge (2026-11-10, finding L3): `Megaman` adds `PlayerGravityComponent` with
`addComponent`, but every reader uses the `playerGravity` field or `PlayerBodyParams.gravity`. No code calls
`require(PlayerGravityComponent::class)`. The owner keeps it as a component (decision D3). `060b` is the second
caller: when `Protoman` adds it, decide there whether the readers take it with `require` and the param goes away.
