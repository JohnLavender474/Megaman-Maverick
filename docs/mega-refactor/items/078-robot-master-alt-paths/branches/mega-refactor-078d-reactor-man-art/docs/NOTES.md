# Notes: mega-refactor-078d-reactor-man-art

## Decisions

- 2026-11-19 (owner accepted the recommendations):
  - Dynamic elements: Tiled tile animations, plus decor entities with no gameplay effect where tiles cannot show the
    motion. No new hazards; block geometry and hazards stay.
  - Distinct sections: keep the tile shapes; each area gets its own accent palette, one moving motif and its own
    background where needed.
  - Tileset: one merged `reactor_man` tileset built by a script (base tiles from the 6 current tilesets, area
    recolors, animation frames), and the map repainted by a gid mapping (the `078a`/`078b` method). Shared tileset
    files stay unchanged.
  - Process: as `078a`/`078b`: round-1 drafts by Claude Fable 5.2 in `temp/078-reactor-art/` from one `STYLE.md`,
    an owner review page, then the brief.
  - Merge target: `master`.

## Overlap with other branches

- `077d` (planned): owns the `TubeBeamerV2` laser and the ceiling emitter tile art (`Reactor_Man_3` tiles 210, 211).
  This branch copies those tiles unchanged into the merged tileset. `077d` then edits the copies in the merged
  tileset for the Reactor Man stage.
- `078c` (active): no shared files.
