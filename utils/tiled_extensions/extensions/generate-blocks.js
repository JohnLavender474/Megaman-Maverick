/// <reference types="@mapeditor/tiled-api" />

/*
 * generate-blocks.js
 *
 * Tiled extension for Megaman Maverick. Generates block objects (collision rectangles) from tile layers.
 *
 * For each source tile layer, every non-empty tile is treated as solid, and the solid cells are merged into as few
 * rectangles as possible with a greedy mesh (extend right first, then down). The rectangles are written as unnamed
 * rectangle objects into a NEW object layer named:
 *
 *     blocks_<yyyyMMdd_HHmmss>_<source layer name>
 *
 * which the game loads like the regular "blocks" layer (any layer named "blocks" or prefixed with "blocks_" is
 * loaded by MegaMapLayerBuilders). Unnamed objects spawn as a plain Block.
 *
 * The script is self-contained and never edits or deletes existing layers: every run creates new layers. Cleaning up
 * stale generated layers and hand-tweaking the generated blocks is up to the developer. Tip: hiding a generated layer
 * disables it in-game too, since invisible layers are not loaded.
 *
 * Actions (Map menu):
 *  - "Generate Blocks from Collision Tile Layers": source layers = every tile layer named "collision_tiles" or
 *    prefixed with "collision_tiles" (including layers nested in groups).
 *  - "Generate Blocks from Selected Tile Layers": source layers = the currently selected tile layers.
 *
 * Each run is a single undo step.
 */

var COLLISION_TILES_PREFIX = "collision_tiles";
var BLOCKS_LAYER_PREFIX = "blocks";

/**
 * Greedy-meshes a grid of solid cells into rectangles. Pure function (no Tiled API), so it can be tested outside
 * Tiled.
 *
 * @param {number} width grid width in cells
 * @param {number} height grid height in cells
 * @param {(x: number, y: number) => boolean} isSolid whether the cell at (x, y) is solid; (0, 0) is the top-left
 * @returns {{x: number, y: number, width: number, height: number}[]} rectangles in cell units; they exactly cover the
 * solid cells and never overlap
 */
function greedyMesh(width, height, isSolid) {
    var solid = new Uint8Array(width * height);
    for (var y = 0; y < height; y++) {
        for (var x = 0; x < width; x++) {
            if (isSolid(x, y)) solid[y * width + x] = 1;
        }
    }

    // A cell is "open" if it is solid and not yet covered by a rectangle.
    function open(x, y) {
        return solid[y * width + x] === 1;
    }

    var rects = [];
    for (var y = 0; y < height; y++) {
        var x = 0;
        while (x < width) {
            if (!open(x, y)) {
                x++;
                continue;
            }

            var w = 1;
            while (x + w < width && open(x + w, y)) w++;

            var h = 1;
            outer:
            while (y + h < height) {
                for (var i = 0; i < w; i++) {
                    if (!open(x + i, y + h)) break outer;
                }
                h++;
            }

            for (var j = 0; j < h; j++) {
                for (var i = 0; i < w; i++) solid[(y + j) * width + (x + i)] = 2;
            }

            rects.push({ x: x, y: y, width: w, height: h });
            x += w;
        }
    }

    return rects;
}

function timestamp() {
    function pad(n) {
        return n < 10 ? "0" + n : "" + n;
    }

    var d = new Date();
    return "" + d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate()) +
        "_" + pad(d.getHours()) + pad(d.getMinutes()) + pad(d.getSeconds());
}

function isCollisionTilesLayer(layer) {
    return layer.isTileLayer && layer.name.indexOf(COLLISION_TILES_PREFIX) === 0;
}

/** Collects every layer in the map, including layers nested inside group layers. */
function allLayers(container) {
    var result = [];
    for (var i = 0; i < container.layerCount; i++) {
        var layer = container.layerAt(i);
        result.push(layer);
        if (layer.isGroupLayer) result = result.concat(allLayers(layer));
    }
    return result;
}

/** Returns the top-level layer that contains the given layer (or the layer itself if it is top-level). */
function topLevelAncestor(layer) {
    var current = layer;
    while (current.parentLayer) current = current.parentLayer;
    return current;
}

/** Sum of the pixel offsets of the layer and all of its parent group layers. */
function totalOffset(layer) {
    var x = 0;
    var y = 0;
    var current = layer;
    while (current) {
        if (current.offset) {
            x += current.offset.x;
            y += current.offset.y;
        }
        current = current.parentLayer;
    }
    return { x: x, y: y };
}

/** The cell bounds to scan; handles infinite maps, where a tile layer can extend into negative coordinates. */
function layerBounds(map, layer) {
    if (map.infinite && typeof layer.region === "function") {
        var rect = layer.region().boundingRect;
        return { x: rect.x, y: rect.y, width: rect.width, height: rect.height };
    }
    return { x: 0, y: 0, width: layer.width, height: layer.height };
}

function indexOfTopLevelLayer(map, layer) {
    for (var i = 0; i < map.layerCount; i++) {
        if (map.layerAt(i) === layer) return i;
    }
    return map.layerCount - 1;
}

/**
 * Generates one new blocks layer for the given source tile layer and inserts it at the top level of the map, directly
 * above the source layer (or above the source's top-level group). Returns the number of blocks created.
 */
function generateBlocksForLayer(map, source, stamp) {
    var bounds = layerBounds(map, source);
    var rects = greedyMesh(bounds.width, bounds.height, function (x, y) {
        return source.tileAt(bounds.x + x, bounds.y + y) !== null;
    });

    if (rects.length === 0) {
        tiled.log("Generate Blocks: \"" + source.name + "\" has no tiles; no layer created.");
        return 0;
    }

    var tileWidth = map.tileWidth;
    var tileHeight = map.tileHeight;
    var offset = totalOffset(source);

    var layer = new ObjectGroup(BLOCKS_LAYER_PREFIX + "_" + stamp + "_" + source.name);
    layer.setProperty("source_layer", source.name);
    layer.setProperty("generated_at", stamp);

    rects.forEach(function (r) {
        var object = new MapObject();
        object.shape = MapObject.Rectangle;
        object.x = (bounds.x + r.x) * tileWidth + offset.x;
        object.y = (bounds.y + r.y) * tileHeight + offset.y;
        object.width = r.width * tileWidth;
        object.height = r.height * tileHeight;
        layer.addObject(object);
    });

    var index = indexOfTopLevelLayer(map, topLevelAncestor(source));
    map.insertLayerAt(index + 1, layer);

    tiled.log("Generate Blocks: \"" + source.name + "\" -> \"" + layer.name + "\" (" + rects.length + " blocks)");
    return rects.length;
}

function generateBlocks(sources, description) {
    var map = tiled.activeAsset;
    if (!map || !map.isTileMap) {
        tiled.alert("Open a map first.");
        return;
    }

    if (sources.length === 0) {
        tiled.alert("No source tile layers found (" + description + ").");
        return;
    }

    var stamp = timestamp();
    var total = 0;
    var layersCreated = 0;

    map.macro("Generate Blocks", function () {
        sources.forEach(function (source) {
            var count = generateBlocksForLayer(map, source, stamp);
            total += count;
            if (count > 0) layersCreated++;
        });
    });

    tiled.log("Generate Blocks: created " + layersCreated + " layer(s) with " + total + " block(s) total.");
}

if (typeof tiled !== "undefined") {
    var fromCollisionLayers = tiled.registerAction("MaverickGenerateBlocksFromCollisionTiles", function () {
        var map = tiled.activeAsset;
        if (!map || !map.isTileMap) {
            tiled.alert("Open a map first.");
            return;
        }
        generateBlocks(
            allLayers(map).filter(isCollisionTilesLayer),
            "tile layers named \"" + COLLISION_TILES_PREFIX + "*\""
        );
    });
    fromCollisionLayers.text = "Generate Blocks from Collision Tile Layers";

    var fromSelectedLayers = tiled.registerAction("MaverickGenerateBlocksFromSelectedTileLayers", function () {
        var map = tiled.activeAsset;
        if (!map || !map.isTileMap) {
            tiled.alert("Open a map first.");
            return;
        }
        generateBlocks(
            map.selectedLayers.filter(function (layer) { return layer.isTileLayer; }),
            "select one or more tile layers"
        );
    });
    fromSelectedLayers.text = "Generate Blocks from Selected Tile Layers";

    tiled.extendMenu("Map", [
        { separator: true },
        { action: "MaverickGenerateBlocksFromCollisionTiles" },
        { action: "MaverickGenerateBlocksFromSelectedTileLayers" }
    ]);
}

if (typeof module !== "undefined") {
    module.exports = {
        greedyMesh: greedyMesh,
        timestamp: timestamp,
        isCollisionTilesLayer: isCollisionTilesLayer,
        allLayers: allLayers,
        topLevelAncestor: topLevelAncestor,
        totalOffset: totalOffset,
        layerBounds: layerBounds,
        generateBlocksForLayer: generateBlocksForLayer,
        generateBlocks: generateBlocks
    };
}
