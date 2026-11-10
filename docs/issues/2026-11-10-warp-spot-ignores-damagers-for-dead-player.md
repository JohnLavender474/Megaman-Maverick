---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: mega-refactor-146a-player-marker (2026-11-10)
related-items: [047]
---

# A warp spot test may ignore damagers when it tests spots for a dead player

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/camera/WarpSpotTest.kt` (`hurts`)
- `core/src/main/kotlin/com/megaman/maverick/game/players/PlayerRules.kt` (`playerAllowsDamageFrom`)

## What happens

`WarpSpotTest.hurts` asks `playerAllowsDamageFrom`, which returns `false` for a player that is not live
(`existenceState.liveAfterUpdate`). If `SlotRespawn` tests spots for a dead player, a damager fixture at a spot never
counts as unsafe, so the player can respawn on a damager.

## What should happen

A spot with a damager that would hurt the player counts as unsafe, also while the player is dead.

## How to reproduce

Not confirmed. Check in which existence state `SlotRespawn` tests the spots, then play
`./run-game-test-coop.sh --room slot_0_death` with an enemy near the partner.
