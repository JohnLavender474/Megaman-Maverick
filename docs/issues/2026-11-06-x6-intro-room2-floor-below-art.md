---
status: open
verified: yes
area: content
gameplay-change: yes
found-by: mega-refactor-081b-x-run-jump-shoot review before the play test (2026-11-06)
related-items: [081]
---

# `X6_INTRO` `room2`: the floor block is 1 tile below the painted floor, and the boss gate is above standing height

## Where

- `assets/tiled_maps/tmx/X6Intro_16x14_v1.tmx`: block 235 (`x` 5440, `y` 448, 800 x 160 px) in the `blocks` layer.
- Gate 14 (`x` 6224, `y` 320, 64 x 96 px), the gate to `boss_room`.

## What happens

- The painted floor of `room2` starts at row 13 (`y` 416 px), but the top of block 235 is at `y` 448. The player
  stands 1 tile inside the floor art.
- The trigger of gate 14 reaches about 1 tile above the player's head, so the player must jump into the gate to open
  it. This is the same for Megaman and X.

## What should happen

The player stands on the painted floor, and a walk into gate 14 opens it.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level X6_INTRO --room room2`: look at the feet against the floor art, then walk into the gate
on the right.

## Notes

- The map comes from a `nesmap` export (see `docs/x6-intro/`). A fix must also hold after a new export.
- The spawn `3` (bottom at `y` 416) and the gates (bottom at `y` 416) suggest that the floor belongs at `y` 416.
- `081b` added the missing floor of `boss_room` at the painted floor (`y` 512, object 287) and moved the `D1000`
  arena onto it.
