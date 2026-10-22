---
status: open
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [040]
---

# 12 TexturePacker projects point to missing source folders, and 3 atlases have no project

## Where

`assets/sprites/texture_packer/`

## What happens

Some `.tps` files use wrong-case paths (`../Frames/`, `../SpriteSheets/`), `../frames/Bosses`, or another repo (`RocketPartnersGame`). Examples: `FreezerMan.tps`, `MegamanMaverick.tps`, `AstroPlayer.tps`. `FridgeMan`, `MegamanChargedShot` and `MegamanMaverick_BUSTER` have no `.tps`.

## What should happen

Every atlas has a project whose sources exist.

## How to reproduce

Open the projects in TexturePacker.
