---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: `mega-refactor-077g-glacier-man` (2026-11-20)
related-items: [077]
---

# Glacier Man can take a player who stands on his floor as "above"

## Where

`GlacierMan.isMegamanAboveOffsetY` (`entities/bosses/GlacierMan.kt`): `>=` on the player's top against his feet +
`MEGAMAN_ABOVE_OFFSET_Y` (1.5 tiles).

## What happens

Megaman and X are 1.5 tiles tall, so a player on the same floor is exactly at the limit. When the float values are
equal, the player counts as above: no duck, every throw is `aim_up`, and snowballs go up. The same rule is on
`master` (before `077g`).

## What should happen

Question for the owner: only a player clearly over him counts as above (for example `>`, or a larger offset).

## How to reproduce

`GlacierManRig` places the player's top at 1.25 tiles by default to avoid the limit; a top at exactly 1.5 tiles
gives the "above" weights (`GlacierManMoveChoiceTest`, the 1.5 / 1.49 boundary cases). In the game the result
depends on the float positions of the bodies.

## Notes
