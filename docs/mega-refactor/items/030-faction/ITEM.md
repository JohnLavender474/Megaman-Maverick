# 030: Faction and root owner instead of `owner == megaman`

- Status: planned
- Type: refactor
- Depends on: `029`, `005`
- Related: `014`, `023`, `045`
- Branches: none yet

## Goal

Decide who can hurt whom by faction, copied at spawn, not by `owner == megaman` checks.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `Faction` plus `ownerPlayerIndex`, copied at spawn from the root owner. Owner changes (reflect) update both.
- Replace 70 `megaman` owner checks and 37 `is Megaman` with faction or capability checks.
- One explosion-owner convention. Golden test first, because today's two conventions give different results.
- Enhancements and score read from the owning player's profile.

## Owner's input

Answer to review item 29 (2026-10-22, verbatim):

> yes

Item `029` research (2026-10-23, verbatim):

> This might be outside the scope of the current item, but it seems to me a mistake that entities like projectiles can have only one `owner`.

## Open questions

- Must Mega's own projectile explosions damage enemies? Today the default `explodeAndDie` path says no. (Decision E.)
- In co-op, can player shots hurt or push other players?
- Do reflected shots switch faction, as `ReactorManProjectile` does today?
- One `owner` per entity is probably not enough (owner's input, 2026-10-23). Keep an instigator (the player or entity responsible) and a causer (the entity that hit), or an owner chain? Examples: a reflected shot, an explosion spawned by a projectile, a shot from a turret that a player placed. See `029` `NOTES.md`, "Co-op gaps and ideas".

## Sources

- `NOTES.md` in this folder.
