---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: mega-refactor-047b-player-rules enemy review (2026-11-01)
related-items: [047, 112]
---

# Question: enemies copy their direction from the target player

## Where

All under `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/`:

- `JetMet.kt:124,171`
- `Starkner.kt:300`
- `TellySaucer.kt:149`
- `MoonEyeStone.kt:218`

## What happens

These sites run `direction = targetPlayer.direction`. If 2 players ever have different directions, the body of the
enemy can flip when the nearest player changes.

## What should happen

The owner decides. Gravity is world-wide today (`WorldGravity.set` writes all players), so the 2 directions are
unlikely to differ. The `047` open question "Is gravity one value for the world, or per player?" decides if this is a
bug.

## How to reproduce

Found by reading the code. Not proved. No known path gives 2 players different directions.
