# 156: `WhiteBurst` has an `EXPLOSION` fixture that projectiles react to

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `060` (branch `060c` added `ConstKeys.COLLIDE` to `WhiteBurst`)
- Branches: none yet

## Goal

`WhiteBurst` (`entities/decorations/WhiteBurst.kt`) is a decoration, but its body has an active `EXPLOSION` fixture.
At a `Laser` end, a projectile that reacts to an explosion (for example `BunbyRedRocket`, `IceBomb`, `Snowball`) or a
`SmallIceCube` can react to the burst. Make the fixture serve only the contacts that need it, so that a decoration
does not act as an explosion by default.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Find each receiver of the burst's `EXPLOSION` fixture, and decide which ones the `Laser` burst must keep.
- Facts from the `060c` merge (2026-11-14, `master` `3247c7fbe`, by reading):
  - `PinkBlock` needs it: its `HIT_BY_EXPLOSION` receiver explodes the block when the burst's owner is a `Laser`
    (for a heart-tank block, only a player's laser).
  - Since `060c`, `WhiteBurst` reads `ConstKeys.COLLIDE` (default `true`). Proto Man's deflect spark passes `false`.
    `Laser` passes nothing, so its burst keeps the fixture on.
  - Other files with an explosion reaction: `FallingIcicle`, `BunbyRedRocket`, `IceBomb`, `Snowball`,
    `CrumblingBlock`, `SmallIceCube`, and the `hitExplosion` default of `IProjectileEntity`.
- Options to compare: a narrower fixture type for the laser end, a filter on the owner, or the default
  `COLLIDE = false` with the `Laser` as the one caller that passes `true`.
- A change of what reacts to a laser end is a gameplay change: list it in the contract, with a check in a level
  that has a `Laser`.

## Owner's input

A backlog candidate of `060c` (2026-11-13), from its review before the play test (finding M1: the deflect spark made
some projectiles explode and a `SmallIceCube` shatter).

## Open questions

- Which of the explosion reactions at a laser end are on purpose (for example `PinkBlock`), and which are side
  effects?

## Sources

- `NOTES.md` in this folder.
- `060c` branch `docs/TODO.md`, "Backlog candidates"; the branch process log, 2026-11-13 review (M1).
