---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [026]
---

# The TMX background `section` property has no effect

## Where

`BackgroundLayerBuilder`, `MegaLevelScreen.kt:976`

## What happens

The property is parsed, but `draw()` draws all backgrounds first anyway. No live map uses it.

## What should happen

Remove it, or make it work in the render plan (`026`).

## How to reproduce

Read the code.
