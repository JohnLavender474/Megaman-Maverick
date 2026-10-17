# Tiled extensions

## generate-blocks.js

Generates block objects from tile layers, so tiles can double as collision.

**Usage** (Map menu):

- **Generate Blocks from Collision Tile Layers** — uses every tile layer whose name starts with `collision_tiles` (e.g. `collision_tiles_1`), including layers inside groups.
- **Generate Blocks from Selected Tile Layers** — uses the currently selected tile layer(s).

For each source layer, the solid (non-empty) cells are greedy-merged into as few rectangles as possible and written to a **new** object layer named `blocks_<yyyyMMdd_HHmmss>_<source layer>`. The new layer is inserted right above its source and has `source_layer` and `generated_at` properties. Each run is one undo step.

The script never edits or deletes existing layers. Re-running it creates another layer, so delete or hide stale generated layers yourself. Hidden layers are not loaded by the game, which makes hiding a quick way to disable one.

In-game, any layer named `blocks` or prefixed with `blocks_` is loaded like the regular `blocks` layer (see `MegaMapLayerBuilders`). The generated objects are unnamed rectangles, so they spawn as a plain `Block`. You can edit them freely afterwards, e.g. add `cull_room` or resize them.

**Install**: open `megaman-maverick.tiled-project` (at the repo root) in Tiled with **File → Open File or Project**. The project loads the scripts in `extensions/`, and its **Project** view lists `assets/tiled_maps`. Open maps while the project is open, or the script is not available. Tiled reopens the last project at start.

If you do not use the project, copy or symlink `extensions/generate-blocks.js` into your Tiled extensions folder instead:

- Linux: `~/.config/tiled/extensions/`
- Linux (Flatpak): `~/.var/app/org.mapeditor.Tiled/config/tiled/extensions/`
- macOS: `~/Library/Preferences/Tiled/extensions/`
- Windows: `%LOCALAPPDATA%/Tiled/extensions/`

**Tests**: run `./run_tests.sh` in this folder. It needs Node 18 or later and no packages. The tests are outside `extensions/` because Tiled runs every script in that folder.
