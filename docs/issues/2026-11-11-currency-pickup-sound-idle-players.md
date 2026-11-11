---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-060b-protoman-move review before the play test (2026-11-11)
related-items: [060, 081]
---

# A currency pickup queues its sound in the players of the roster that are not in the engine

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt` (`postCreate`: each roster player's
  `PlayerProgressComponent` becomes a `gameState` listener)
- `core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayer.kt` (`onCurrencyAdded`)

## What happens

On a currency pickup, `onCurrencyAdded` queues `CURRENCY_PICKUP_SOUND` in the `AudioComponent` of each roster player,
also of a player that is not in the engine (for example X or Proto Man while Megaman plays). That queue plays or
clears only at that player's next spawn or destroy, so the queued sounds can play later (for example: collect bolts
as Megaman, then start a level as Proto Man). X did this before `060b`; Proto Man adds a second idle player.

## What should happen

Only the player that is in play (in a slot and in the engine) plays the pickup sound.

## How to reproduce

Not checked in the game. Collect several bolts as Megaman, then start a level whose `characters` puts another player
class in slot 0, and listen for pickup sounds at its spawn.

## Notes

Not confirmed: whether `AudioSystem` skips the queue of an entity that is not in the engine. Found in the `060b`
review (`docs/mega-refactor/items/060-proto-man/branches/mega-refactor-060b-protoman-move/docs/TODO.md`).
