---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-047b-player-rules enemy review (2026-11-01)
related-items: [047, 112]
---

# A Koopa shell takes its roll direction from the target, not from the player who kicked it

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/GreenKoopaShell.kt:150,187`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/RedKoopaShell.kt:151,188`

## What happens

The roll direction reads `targetPlayer`. With `--players 2`, the nearest player can be a different player from the
one who kicked the shell. So the shell can roll toward the kicker.

## What should happen

The shell rolls away from the player who kicked it. The stomp path knows that player.

## How to reproduce

Found by reading the code. Not run. With `--players 2`, stand both players next to a shell, with the other player
nearer to the shell, and kick it.
