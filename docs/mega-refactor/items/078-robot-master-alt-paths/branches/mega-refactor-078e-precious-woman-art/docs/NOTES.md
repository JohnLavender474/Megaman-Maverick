# Notes: mega-refactor-078e-precious-woman-art

## Decisions

- 2026-11-20 (owner): background concept "crystal abyss": a deep chasm with crystal-lined cliffs in 2 to 3 parallax
  layers that fall away into dark, glowing veins, and a far lake that mirrors the gems.
- 2026-11-20 (owner accepted the recommendation): the stage reads as a jewel mine dug into the abyss. The girder
  platforms stay, redrawn as old mine scaffolds (lanterns, cart rails). The `tiles2` back wall goes; its steel
  pillars become a few scaffold towers in a decor layer, in front of the background.
- 2026-11-20 (owner): Claude Fable 5.2 draws the art (the confirmation of `docs/ways-of-working/art.md`).
- 2026-11-20 (owner approved the short design): the `078a`/`078b` method. One new original tileset built by a script
  in `temp/078e-precious-art/`; the map repainted by a gid mapping plus position rules; a new `decor` tile layer;
  Tiled tile animations (crystal glints, lamp flicker); new parallax background objects in place of `CrystalBKG`.
  Crystal decor as tiles, not entities. No Mega Man 5 shapes in the new crystals. Credits: the original by mrgranade
  and the update by Claude Fable 5.2. Merge target: `master`.

## Sources of the art

- Current tilesets of the map: `precious_woman_foreground_tileset`, `precious_woman_background_tileset`,
  `precious_woman_platforms_tileset` (credited to mrgranade in `README.md`, "Precious Woman"), `CrystalManTileset`
  (Mega Man 5 rip), `mm9jewel` (Mega Man 9 rip), `Tileset1` (1 tile).
- Current background: `CrystalBKG` (`BACKGROUNDS_6`), Megaman Maker "Crystal Cave Background".

## Overlap with other branches

- `078d` (active, Reactor Man art): no shared files except a possible new `TextureAsset` entry and `README.md` credit
  lines (simple list conflicts).
