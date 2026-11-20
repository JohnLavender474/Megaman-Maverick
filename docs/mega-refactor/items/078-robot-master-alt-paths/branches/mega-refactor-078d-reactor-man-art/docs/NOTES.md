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

- 2026-11-19 (owner accepted the recommendations, round-1 review https://claude.ai/artifact/MS6y4SoXyaVvKL4KDLrDkd):
  - Q1: the coolant hall pipes and tanks are green; magenta means only "toxic or core".
  - Q2: areas 2 to 5 get a dim background each (hall tank farm, works furnace wall, shaft sluice wall with falling
    water, core containment ring with a pulse); the merge script makes the black of background-art tiles clear.
    The entrance keeps `CityBKG`.
  - Q3: the decor laser tube beam is dim amber.
  - Q4: 2 decor entities, `DecorLaserTube` and `SteamVent`, plus the spark overlay tile.
  - Q5: the steel gets a light tint per area.
  - Q6: the dome pulse stays as drawn.
  - The round-1 choices of the art agent (listed in `temp/078-reactor-art/STYLE.md`) stand.
- 2026-11-19 (owner, after the round-1 review; replaces the per-area accents of Q1, Q3 and Q5): "A unified palette
  would be better instead of having multiple very different colors scattered around. Let's settle on a single
  palette for the walls/tiles and another for the water. Dynamic effects like pulsating cores, wires, etc. can stay.
  Do another pass of redrawing/refinement." So: one palette for all walls and tiles, one palette for all water
  (toxic water, waterfalls); the motion (flow, pulse, sparks, lights, decor) stays; round 2 redraws and refines.

- 2026-11-19 (owner, round-2 review, version 2 of the page):
  - Q1: tile palette `classic` (today's grey, green and magenta, refined). Not the recommendation.
  - Q2 (owner accepted the recommendation): water palette `acid` (acid green, lime foam); the toxic splash follows;
    `DrippingToxicGoop` keeps its magenta pixels for now.
  - Q3 (owner accepted the recommendation): the refine pass is on (bevel, riveted plates, pipe highlight).
  - Q4 (owner accepted the recommendation): the 15 beamer tube tiles take the palette map, shapes unchanged; `077d`
    redraws on this base.
  - Known gap from Q1 + Q2: the `classic` green steel shadow is near the deep water green (distance 52, check limit
    60). Claude's call: the next art round moves one of the 2 greens far enough apart to pass the check, and keeps
    both looks.
- 2026-11-19 (owner): an art agent redraws the outdoor city background of the entrance (`CityBKG` object, region
  `BGNeedle` in `Backgrounds4`, seen in `room1`). Only the live Reactor Man map uses `BGNeedle`.

- 2026-11-19 (owner accepted the recommendations, city section of the review page, version 3):
  - C1: the layered night city with the reactor plant (2 cooling towers with steam, a stack, a dome).
  - C2: animated, 4 frames of 0.5 s (beacons, stars, windows, steam).
  - C3: a new region `ReactorCityBKG` in `Backgrounds4`; `BGNeedle` stays for the old map files. The `CityBKG`
    object becomes 512 x 448 with `offset_y=0`, `animation_rows=2`, `animation_columns=2`, `duration=0.5`, so
    the art draws at true 2x.
- 2026-11-19 (owner): `BGNeedle` is Capcom art from Mega Man 3. The new city background is an original work by Claude
  Fable 5.2 and needs no credit to `BGNeedle` (no "Inspired by" line).

## Overlap with other branches

- `077d` (planned): owns the `TubeBeamerV2` laser and the ceiling emitter tile art (`Reactor_Man_3` tiles 210, 211).
  This branch copies those tiles unchanged into the merged tileset. `077d` then edits the copies in the merged
  tileset for the Reactor Man stage.
- `078c` (active): no shared files.
