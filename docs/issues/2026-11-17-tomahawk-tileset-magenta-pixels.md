---
status: open
verified: yes
area: content
gameplay-change: no
found-by: `mega-refactor-076a-desert-man-tileset` (2026-11-17)
related-items: [076]
---

# Two gate-column tiles of `tomahawk_man_stage_tileset` have magenta pixels

## Where

`assets/tiled_maps/png/tomahawk_man_stage_tileset.png`, local tile ids 10347 (column 107, row 40) and 10437 (column
197, row 40). They are the same gate-track image.

## What happens

The tiles contain magenta (key-color) pixels, which draw as magenta in the game.

## What should happen

No magenta pixels: transparent pixels or tile colors.

## How to reproduce

Crop the two tiles from the PNG (32x32 cells, 256 columns) and look at them.

## Notes

- Desert Man no longer uses this tileset after `076a`. Other maps that use `tomahawk_man_stage_tileset` were not
  checked for these 2 tiles.
