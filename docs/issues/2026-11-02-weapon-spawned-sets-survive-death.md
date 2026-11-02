---
status: open
verified: no
area: core
gameplay-change: yes
found-by: 047 pooling research (2026-11-02)
related-items: [047]
---

# A dead player's weapon `spawned` sets are not cleared

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/handlers/MegamanWeaponsHandler.kt`: `reset()` at `:156` has 0 production callers, and `cullAllDead` at `:99` tests only `liveAfterUpdate`.

## What happens

Nothing clears the `spawned` sets when a player dies. `cullAllDead` does not run for a dead player, so a shot that went back to its pool and was reused by another owner stays in the set.

Other owners can fetch the same class: `MoonMan.kt:671`, `PreciousWoman.kt:926`, `LumberJoe.kt:455`, and the other player. `spawnedUpdateFunc` (`:373-390`) also casts and moves a stale `PreciousGemCluster`.

With 2 players, this is the plain case: slot 1 is dead and slot 0 fires `MOON_SCYTHES`. The dead slot's count grows, and its `postShoot` can fade the scythes of the other player. Found by reading the code. Not run.

Cooldowns and inner timers also survive a death, but they end during the beam-in, so they have no effect at a level respawn.

## What should happen

Each spawn starts with empty `spawned` sets.

## How to reproduce

Run `--players 2 --weapons all`, let slot 1 die, and fire `MOON_SCYTHES` with slot 0.

## Notes

The fix calls `reset()` in `Megaman.onSpawn`, after `Megaman.kt:338`. If it runs earlier, `onChangeWeapon` no longer destroys the clusters. `047c` owns this.
