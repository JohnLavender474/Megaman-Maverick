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

## Slopes

Slopes (extra levels only) need no extension. Each slope is a plain rectangle object named `GroundSlope`, in an object layer named `blocks_slopes` or `blocks_slopes_<n>`, which the game loads like the `blocks` layer. The rectangle is the bounding box of one straight run. Its string property `side` is the high side, `left` or `right`, and its height / width is 1/4 (shallow) or 1/2 (steep). The low end touches a floor at the rectangle's bottom; the high end touches a flat block at the rectangle's top, or a wall. A slope draws nothing in play (the debug view outlines its triangle), so paint slope tiles in its box.

To see only the slopes, toggle the visibility of the slope layers and the other layers. Turn every slope layer back on before you save: hidden layers are not loaded by the game, so a hidden slope layer means no slopes in play. An object with no name in a `blocks_*` layer spawns as a plain `Block`, a full solid box. The `level-builder` skill has the full rules, and `assets/tiled_maps/tmx/RefactorTest068.tmx` has examples.
