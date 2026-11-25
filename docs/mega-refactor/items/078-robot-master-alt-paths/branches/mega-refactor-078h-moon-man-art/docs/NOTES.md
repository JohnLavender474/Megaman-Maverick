# Notes: mega-refactor-078h-moon-man-art

## Decisions

- 2026-11-25 (owner): the art goes to a Claude Fable 5.2 art agent (`art.md`); the owner named the model.
- Art decisions of the first pass (Fable agent, 2026-11-25; details and drafts in `temp/moon-man-redraw/NOTES.md` of
  the worktree, which is not committed): the rock is 100% new pixels in the slots 25-28, 33-36, 41-44, 49-52 with
  shared edge stones so that any rock tile joins any other; metal, tech, ladder and hazard tiles keep their shapes
  and get the 2 px grid and NES colors; the transparency mask is kept, except 11 half-cut blocks in tile 55; the
  background layers are drawn at NES resolution (1 art px = 2 game px) like the old `Space.png`.
- Palettes (NES 2C02, backdrop `000000`): P1 rock/metal `004058 7C7C7C BCBCBC`; P2 tech `0058F8 3CBCFC F8F8F8`;
  P3 warm surface/moon `7C7C7C BCBCBC F0D0B0`; P4 sky greys `7C7C7C BCBCBC F8F8F8`; PX hazard tile 13
  `F8B800 A81000 F8F8F8` (not in the live map).
- Proposed layers and parallax (`temp/moon-man-redraw/out/backgrounds/WIRING.md`): `Space` stars 0.02/0.02,
  `MoonNebula` 0.04/0.04, `MoonSky` 0.05/0, `MoonComets` 0.12/0.08, `MoonRidge` 0.25/0.

## Source record for credits

- Tileset: builds on the existing `MoonManTiles.png`, which SamuelX4 made (owner, 2026-11-25). Claude Fable 5.2
  redrew the rock tiles and recolored the rest to the NES palette.
- Backgrounds: the original `Space` background is by Capcom. All pixels of the new layers were made by a generator
  script in this branch, so the new background is original and needs no Capcom credit (owner, 2026-11-25). The star
  field follows the old one only in idea (sparse, 2-frame twinkling stars on black); no pixels copied.
- No outside art, pack or game sheet was followed.
- Proposed `CREDITS` lines (`#### Tilesets`, a new group for SamuelX4 with the link that the README already uses for
  him; check that no group for him exists yet), to add in this branch before the merge:

  ```markdown
  - [SamuelX4](https://www.deviantart.com/samuelx4)
    - Moon Man
      - Original by [SamuelX4](https://www.deviantart.com/samuelx4)
      - Updated by Claude Fable 5.2, an AI model by [Anthropic](https://www.anthropic.com/claude), directed by the developer: the rock tiles and the NES palette
  - Moon Man Backgrounds (`Space`, `MoonNebula`, `MoonSky`, `MoonComets`, `MoonRidge`)
    - Made by Claude Fable 5.2, an AI model by [Anthropic](https://www.anthropic.com/claude), directed by the developer
  ```

## Overlap with other branches

See `SCOPE.md`.
