---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: 048 research (2026-11-02)
related-items: [047, 119]
---

# With `--players 2`, the death of slot 1 destroys P1's gems and resets a gauntlet

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/PreciousGem.kt:215`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/PreciousGemCluster.kt:109`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/special/GauntletMaster.kt:168`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/SpiderWeb.kt:138`

## What happens

`PLAYER_JUST_DIED` carries a slot, but these 4 listeners do not read it.

- `PreciousGem` and `PreciousGemCluster` test only that their owner is a player. So the death of P2 destroys the `PRECIOUS_GUARD` of P1.
- `GauntletMaster` calls `resetGauntlet()` on the death of any slot. It sets `INACTIVE`, keeps the live enemies and loses its child list. Its comment at `:191` shows that it expects a level reset. A gauntlet `Gate` opens only on `GAUNTLET_CLEARED`, so the room appears to stay locked until slot 0 dies. This last result comes from reading the code. Not run.
- `SpiderWeb` is destroyed on any death. It has a `TODO` at `:136`. It has no production spawner today.

## What should happen

An owned object reacts only to the death of its owner. A gauntlet resets only when the level resets.

## How to reproduce

1. `./run-game-alpha.sh --level GAUNTLET_1 --players 2`.
2. Start a wave, and let slot 1 die while slot 0 stays alive.
3. Look for the room to stay locked with no way to finish it.

For the gems: give P1 `PRECIOUS_GUARD` with `--weapons all`, use it, and let slot 1 die.

## Notes

`119` fixes this in its filter step, with `LEVEL_FAILED` for the gauntlet.
