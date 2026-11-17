# Scope: mega-refactor-076a-desert-man-tileset

- Status: approved
- Approved: 2026-11-17 (owner: "Go with your recommendations for now for all open questions/decisions")

## Goal

Desert Man gets a fully new, original tileset, made from the art drafts of 2026-11-17 (`temp/076-desert-tiles/`
`phase1/` to `phase5/` in the main checkout), and the map `DesertMan_16x14_v4.tmx` is repainted with it. The block
objects, the sensors and every other object stay as they are, so the level plays the same.

## Item

`076`: Better tileset art for every level. From its `ITEM.md`:

- Goal: give each level tileset art of the same high quality, so that the game has one consistent look. The item
  changes how the levels look. It must not change the block geometry or the gameplay unless the scope says so.
- Proposed scope: for each tileset to replace, choose or make new art, and paint the level tiles with it. Keep the
  block objects as they are. One branch per level or per tileset.
- Decisions of 2026-11-17: Desert Man is the first level; its tileset is new and original art, drawn in phases;
  keep the gap-0 vertical rock joins; merge target `master`.

## Current state

- Load and draw: `MegamanMaverickGame.kt:938` sets `TmxMapLoader`; the TMX names its tilesets by `.tsx` path, and the
  loader reads the `.png` from the `.tsx`. `TiledMapLevelRenderer` (engine, an `OrthogonalTiledMapRenderer`) draws
  every tile layer in file order. `MegaLevelScreen.kt:908-928` draws the backgrounds (the sunset objects
  `DesertSunSky`, `SunriseHills`, `SunsetHills`) and the background sprites first, then the tile layers, then the
  playground and foreground sprites. No code reads tile gids. No tile has properties in either `.tsx`, and no object
  in the map has a `gid`.
- The map: 750x100 cells, 5 tile layers in CSV: `tiles3` (pure black backdrop), `wall_layer` (opacity 0.5),
  `tiles2` (back wall), `tiles1` (terrain, props), `tiles0` (front decoration). Tilesets: `Tileset1` (firstgid 1,
  69 used gids) and `tomahawk_man_stage_tileset` (firstgid 1025, 4 used gids). Both `.tsx` files are shared with
  other levels.
- The old back wall (tomahawk gids 11044 and 10788, 5,340 cells) is semi-transparent. 86 of these cells have no
  `tiles3` black under them, so the sunset shows faintly through.
- The drafts: `phase5/make_phase5.py` builds every new tile (rock, steel, props, sand, oasis groups, 126 tiles),
  the remap functions (`rock_tile`, `rope_tile`, `soil_tile`, `fence_tile`: a new tile is chosen by the old gid and
  the real neighbors; the rock and soil fill use a 2x2 block chosen by the cell's x/y parity) and before/after
  previews from the real TMX. After phase 5 the map paints 2 old-art gids: 11372 (5 cells, room 5, `tiles2`) and
  11462 (3 cells, room 14, `tiles1`, under a `Gate`), both gate-column art.

## In scope

1. One new tileset: `assets/tiled_maps/png/desert_man_tileset.png` (32 px cells, no spacing, no margin) and
   `assets/tiled_maps/tsx/desert_man_tileset.tsx`. It holds only the tiles that the map paints, plus the 2 unused
   soil singles if they are free to keep.
2. New art for the 2 gate-column tiles (gids 11372 and 11462), so that the map paints no old art. (Decision D1.)
3. A calmer bloom head, so that it does not read as a pickup. (Decision D2.)
4. The back wall stays opaque. The 86 cells where the sunset showed faintly through the old wall show the new wall.
   (Decision D4.)
5. The map: every cell of the 5 tile layers is rewritten by the remap functions. The map's tilesets become only
   `desert_man_tileset` (firstgid 1); `Tileset1` and `tomahawk_man_stage_tileset` leave this map. (Decision D5.)
6. The generator and remap script and its README are temporary work files, not branch content. They live in
   `temp/076-desert-tiles/tools/` in the main checkout (git-ignored). (Decision D3, changed 2026-11-17.)
7. The spikes under the water (room 15): checked at the play test; changed in this branch if they read weakly.
   (Decision D6.)

## Out of scope

- Block objects, sensors, spawns, rooms, backgrounds and every other object layer. No geometry change.
- `Tileset1.tsx`, `tomahawk_man_stage_tileset.tsx` and their images: unchanged; other levels keep using them.
- Other levels. Code changes (none are needed).
- `067` (PPM 16): the new art is 2x pixel art, as the old art.

## Affected areas and risks

- Files: 1 new `.png`, 1 new `.tsx`, `DesertMan_16x14_v4.tmx` (tile layers and the tileset list). `assets/assets.txt` is
  git-ignored; the build lists the new files in it.
- Risk: a cell maps to the wrong tile (a missing edge, a wrong grid-position tile). Check: the script counts every
  old gid and fails on a gid with no mapping; the previews of all rooms; the play test.
- Risk: the TMX also has a Tiled-only state (for example the `nextobjectid`); the script rewrites only the
  `<tileset>` elements and the CSV of the 5 layers, and leaves all other bytes as they are.
- Risk: lines between tiles (texture bleeding) at some camera positions. The old tilesets have no padding either, so
  the risk is not new; the play test looks for it.

## Overlap with other branches

Checked on 2026-11-17: no active branch changes `DesertMan_16x14_v4.tmx` or the tileset files. `compactor-level`
changes `LevelDefinition.kt`; this branch does not.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` Code only with a production caller | no effect | No game code. The script is a temporary file in `temp/`. |
| `O2-R7` Gameplay does not depend on rendering | no effect | Tiles are drawn only; blocks are objects. |
| `O4-R1` Mechanics from Tiled | no effect | No new mechanic. |
| `O5-R1` No per-frame allocation | no effect | Same renderer, fewer tilesets. |
| `O6-R1` CI passes | no effect | Checked before the merge. |
| `O6-R2` Named checks | helps | The contract names a room check for each tile group. |
| `O7-R3` No level-identity checks | no effect | No code. |
| Other always-on rules | no effect | No code. |

## Decisions (owner, 2026-11-17: the recommendations)

- D1: draw the 2 gate-column tiles in this branch.
- D2: make the bloom head calmer.
- D3: the script and its README are temporary files in `temp/076-desert-tiles/tools/` (owner, 2026-11-17, at the
  diff review; first choice: the branch folder). Tiled automapping is a backlog candidate.
- D4: the new back wall is opaque.
- D5: the map uses only the new tileset.
- D6: check the spikes under water at the play test; fix in this branch if needed.

## Open questions

None.
