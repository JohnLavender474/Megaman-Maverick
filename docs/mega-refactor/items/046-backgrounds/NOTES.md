# Notes: 046 Backgrounds as pure functions of the camera, with visible-cell drawing

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEM 46: Backgrounds (the 10,000-quad Wily 3 background)".

````text
1. Current state

- Each `Background` has its own viewport and camera (`Background.kt:65-79`). Each frame, `MegaLevelScreen.render` computes the game camera delta and calls `background.move(dx, dy)` (`MegaLevelScreen.kt:951-954`). `move` adds `delta × parallax` to the background camera (`Background.kt:103-107`). `gameCameraPriorPosition` is set to zero in `show()` (`:473`). `reset()` (`Background.kt:137`) is called only on level exit (`MegaLevelScreen.kt:1080`), not on respawn. On respawn only `immediateRotation(UP)` runs (`:551`).
- The math: the sum of the deltas gives `bgCam = initPos + parallax × gameCam`. In theory this is already path-independent. In practice: (1) the float error grows over a long run; (2) the `doMove` hook would make it path-dependent (no current caller changes it); (3) the respawn does not restore background visibility (`backgroundsToHide`, changed by `SHOW_BACKGROUNDS`/`HIDE_BACKGROUNDS` at `:847-857`, item 36). Point (3) is the most probable cause of "the background does not match after respawn".
- The real reason for 100×100 is coverage. `SpriteMatrix` (`engine/.../drawables/sprites/SpriteMatrix.kt:32-41`) places a finite grid from the rectangle corner, going right and up only. There is no wrap. `draw` draws every cell with no culling (`:65`). The designer must compute by hand how far the background camera can move.
- `WilyStage3_v4.tmx`: the map is 500×250 tiles (16000×8000 px). The background object is 512×448 with rows = columns = 100, offset 20/20 tiles, `parallax_y` 0.1, and the default `parallax_x` 0.25 (`Constants.kt:35`). The background camera can move only about 0.25 × 16000 = 4000 px in x (about 8 cells) and 0.1 × 8000 = 800 px in y (about 2 cells). So about 10 × 4 = 40 cells cover the whole level, and only about 4 cells are on screen at a time. Today all 10,000 quads are sent every frame, which is at least 10 batch flushes with the default `SpriteBatch()` of 1000 sprites (`MegamanMaverickGame.kt:259`).
- Other live maps use 1 row × 25-50 columns (preset backgrounds in `BackgroundLayerBuilder.kt`).
- `Background.update` builds a debug log string with `boundingRectangle` every frame (`Background.kt:126`), even when the log tag is filtered (item 43).
- X6 facts (`engine.md` "Camera", "BG layer modes", and "Checkpoints"):
  - Each BG layer position is computed from the layer-0 camera every frame: mode 1 = x/2 + offset, mode 3 = x/4 + offset, mode 2 = full, mode 9 = follow x only, mode 5 = set by another object.
  - The offsets (`+0x40`, `+0x42`) come from the checkpoint respawn record, which also stores the layer 1 and 2 camera positions.
  - A move cap (`LayerFollowCap`) smooths large jumps, but at respawn the old position is set to the target, so there is no cap.
  - Stage 7 changes a layer's offset and mode at checkpoint ≥ 1 (`SheldonBgSwitch`).
  - So in X6 the position is a pure function of the camera, plus per-checkpoint state.

2. Options

A. Keep the delta accumulation but compute the needed rows and columns automatically from the level size and parallax. Small change, but it still draws all cells and still drifts.
B. Pure function plus drawing only the visible cells: `bgX = anchorX + offsetX + parallaxX × camX` (the same formula as today with origin 0, so the migration is exact). Draw only the cells from `floor((viewLeft − bgX) / cellW)` to the right edge. On an axis with wrap, the count has no limit. On an axis with no wrap, draw one strip.
C. Pure function plus GL texture repeat (one quad with UV scroll). This does not work with atlas regions, because repeat wraps the whole page. It also does not work for pages that are not powers of two on WebGL1, and most pages here are not (for example `Backgrounds3` is 2000×2172). It is possible only with separate POT background textures.

3. Recommendation

Use B.
- `BackgroundLayer(region, cellW, cellH, parallaxX, parallaxY, anchor, offset, wrapX, wrapY, rotatable)`. Its position is recomputed each frame from the game camera. Nothing is accumulated, so respawn and teleport need no special handling. Floor to whole pixels for pixel-perfect mode.
- Draw 2-4 quads per layer instead of 10,000. The TMX `rows`/`columns` properties are no longer needed. Replace them with `wrap_x`/`wrap_y` (default: wrap in x, no wrap in y).
- Optional "modes" as presets, following X6 (`half` 0.5, `quarter` 0.25, `follow_x` (1, 0), `fixed`, `scripted`).
- Per-checkpoint background state (items 35 and 36): visibility set and optional offset or mode overrides, restored by the respawn routine (item 37). This replaces the `EventTrigger` workarounds for visibility.
- Backgrounds become entries in the render plan (item 25), so they can also be placed in front of tiles.
- Contract check: with the same `initPos` and parallax, positions match today's to within float error. Compare screenshots in Wily 3 and in one preset-background level.

4. Open questions for the owner

- Did you see the respawn mismatch as a wrong position or as a wrong visible/hidden background? The code suggests visibility (item 36).
- Do any backgrounds need non-linear motion (for example X6-style capped catch-up after a teleport), or is linear parallax enough?
- Must a background change by checkpoint (as in X6 stage 7), or only by triggers?

5. Proposed backlog wording

Title: Backgrounds as pure functions of the camera, with visible-cell drawing
- Compute the background position each frame from the game camera: `anchor + offset + parallax × cam`. No accumulated deltas.
- Draw only the visible cells, with wrap in x and/or y. Remove the `rows`/`columns` grid (Wily 3: from 10,000 quads to about 4).
- Optional X6-style modes (half, quarter, follow-x, fixed, scripted) as parallax presets.
- Restore background visibility and offsets per checkpoint on respawn.
- Remove the per-frame debug string in `Background.update`.

6. Dependencies

25 (backgrounds as render-plan entries), 35, 36 and 37 (per-checkpoint state and the respawn routine), 43 (debug string), 8 (draw-call baseline).
````
