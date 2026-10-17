# Tiled extensions

## generate-blocks.js

Generates block objects from tile layers, so tiles can double as collision.

**Usage** (Map menu):

- **Generate Blocks from Collision Tile Layers** — uses every tile layer whose name starts with `collision_tiles` (e.g. `collision_tiles_1`), including layers inside groups.
- **Generate Blocks from Selected Tile Layers** — uses the currently selected tile layer(s).

For each source layer, the solid (non-empty) cells are greedy-merged into as few rectangles as possible and written to a **new** object layer named `blocks_<yyyyMMdd_HHmmss>_<source layer>`. The new layer is inserted right above its source and has `source_layer` and `generated_at` properties. Each run is one undo step.

The script never edits or deletes existing layers. Re-running it creates another layer, so delete or hide stale generated layers yourself. Hidden layers are not loaded by the game, which makes hiding a quick way to disable one.

In-game, any layer named `blocks` or prefixed with `blocks_` is loaded like the regular `blocks` layer (see `MegaMapLayerBuilders`). The generated objects are unnamed rectangles, so they spawn as a plain `Block`. You can edit them freely afterwards, e.g. add `cull_room` or resize them.

**Install** — either:

- copy or symlink `generate-blocks.js` into your Tiled extensions folder (Linux: `~/.config/tiled/extensions/`, macOS: `~/Library/Preferences/Tiled/extensions/`, Windows: `%LOCALAPPDATA%/Tiled/extensions/`), or
- open the maps through a Tiled project (`.tiled-project`) whose **Extensions Directory** points at this folder.
