---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-047b-player-rules code review (2026-11-02)
related-items: [047]
---

# With `--players 2`, a dead slot 1 still counts at the `players.any` sites

## Where

- `engine/src/main/kotlin/com/maverick/game/engine/players/IPlayers.kt`: `IPlayers.any` tests no life or `inPlay`.
- 74 `players.any` lines in `core/src/main/kotlin/com/megaman/maverick/game/entities/`, for example `enemies/Cactus.kt:364` and `enemies/FireDispensenator.kt:240,248`.
- `core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt`: the `TODO` at `anyLive`.

## What happens

A dead slot 1 stays in the player list, and its body stays at its last position. So it can trip a scanner. This gives
a false trigger, not a stuck state.

## What should happen

A dead player does not count while another player is live. `MegaPlayers.anyLive` does this, and with no live player it
visits all players, so a game with 1 player does not change. The 8 sites of `047b` use it. Each other site needs a
review, because some may act on purpose for a dead player.

## How to reproduce

With `--players 2`, kill player 2 inside the scanner of a `Cactus`, with player 1 away from it. The `Cactus` reacts.
Found by reading the code; not run.
