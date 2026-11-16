---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: `mega-refactor-011a-game-random` (2026-11-14)
related-items: [011, 024]
---

# Moon Scythes of a new shot sort as the oldest and fade first

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/weapons/MegamanWeaponStates.kt`:
  `MoonScythesState.fadeExtraScythes`, the sort on `spawnOrder` (a `TODO` marks the line)

Before `024a`, the same code was in the old `MegamanWeaponsHandler.kt` `postShoot`. Before `011a`, the sort key was
the wall-clock `timeSpawned`.

## What happens

`fadeExtraScythes` runs right after the new shot asks to spawn its scythes. A spawn is queued until the next
`engine.update()`, so the new scythes have the `spawnOrder` of their last pooled life, or 0. They sort as the oldest,
and the loop fades them first, not the oldest live scythes. `master` before `011a` had the same fault with
`timeSpawned`.

## What should happen

With more than 4 scythes, the oldest scythes fade and the new shot stays.

## How to reproduce

`./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons MOON_SCYTHES`: fire Moon Scythes quickly so that more
than 4 are alive. Look at which scythes fade.

## Notes

Found by the review before the play test of `011a`. `011a` kept the old behavior (contract check 7). A fix can sort
after the spawn (for example on the next frame), or give a queued spawn its order number at the request. The other
Moon Scythes fault (`2026-11-14-moon-scythes-stale-array.md`) is in the same function.
