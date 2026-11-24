---
status: open
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor-078e-precious-woman-art (2026-11-23)
related-items: [078]
---

# Each repack of `Platforms1` moves the colors of old regions

## Where

`assets/sprites/texture_packer/Platforms1.tps` (page format `png8`), `assets/sprites/sprite_sheets/Platforms1.png`.

## What happens

The atlas page has a palette. Each repack that adds or changes a region builds a new palette, so regions that did not
change get other colors. In `078e`: `Platform2` (round 8: 175 px, RGB up to 19 against round 7; 236 px, RGB up to 31
against `master`) and `BreakableIce/SmallShards` (RGB up to 11 against `master`). Alpha does not change.

## What should happen

A repack keeps the pixels of each region whose frames did not change.

## How to reproduce

Repack `Platforms1` after a change to one frame, and compare the other regions with the frames in
`assets/sprites/frames/Platforms1/`.

## Notes

Options: a page format with no palette for `Platforms1`, or fewer colors in the atlas.
