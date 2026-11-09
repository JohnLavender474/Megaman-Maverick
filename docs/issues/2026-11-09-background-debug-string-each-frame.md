---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor-134a-draw-interpolation (2026-11-09)
related-items: [044, 046]
---

# `Background.update` builds a debug string each frame, also when its tag is filtered out

## Where

`core/src/main/kotlin/com/megaman/maverick/game/drawables/backgrounds/Background.kt`, `update(delta)`.

## What happens

`GameLogger.debug(TAG, "Updating background: ${(backgroundSprites[0, 0] as GameSprite).boundingRectangle}")` builds
the string, and reads `boundingRectangle`, on each frame for each background. `GameLogger.filterByTag` drops the
message when the tag is not in `TAGS_TO_LOG`, but the string is already built. This is a per-frame allocation in a
common path (`O5-R1`).

## What should happen

No string is built unless the message will print, or the line is removed.

## How to reproduce

Read the code. Any level with a background runs it on each frame.

## Notes

Item `044` (lazy debug and log strings) gives the general fix.
