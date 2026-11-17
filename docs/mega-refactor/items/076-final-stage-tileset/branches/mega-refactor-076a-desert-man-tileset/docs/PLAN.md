# Plan: mega-refactor-076a-desert-man-tileset

## Steps

### 1. Script into the branch

- Changes: copy `temp/076-desert-tiles/phase5/make_phase5.py` (main checkout) to `tools/make_desert_man_tileset.py` in this branch folder (moved to `temp/076-desert-tiles/tools/` on 2026-11-17, owner). Read the TMX of the worktree. Make the script fail on any old gid with no mapping.
- Check: it runs and makes the same tiles and previews as phase 5.
- Done: yes (2026-11-17)

### 2. The last art (D1, D2)

- Changes: new tiles for the gate columns (gids 11372 and 11462); a calmer bloom head.
- Check: previews of room 5, room 14 and room 15; no old-art gid is left.
- Done: yes (2026-11-17)

### 3. Tileset files

- Changes: `assets/tiled_maps/png/desert_man_tileset.png` (only the tiles that the map paints, 32 px cells, no spacing) and `assets/tiled_maps/tsx/desert_man_tileset.tsx`.
- Check: the `.tsx` image size and tile count match the PNG.
- Done: yes (2026-11-17)

### 4. Map repaint

- Changes: the script writes `DesertMan_16x14_v4.tmx`: the tileset list (only the new one, firstgid 1) and the CSV of the 5 tile layers. All other lines stay as they are.
- Check: the TMX checks of the contract (all other lines byte-identical; the same number of non-empty cells per layer); a full render of every room from the new TMX and the new PNG, compared with the phase 5 previews.
- Done: yes (2026-11-17)

### 5. README for hand edits (D3)

- Changes (moved to `temp/076-desert-tiles/tools/` on 2026-11-17, owner): `tools/README.md`: how to rebuild the sheet and repaint the map, and the parity rule for the rock and soil fill.
- Check: the commands in it run.
- Done: yes (2026-11-17)

### 6. Checks and review

- Changes: none.
- Check: the automated checks of the contract; the review before the play test by a subagent; then the play test and the Diffity review.
- Done: yes (2026-11-17)
