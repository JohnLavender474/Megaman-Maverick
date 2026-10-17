const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");

const gb = require(path.join(__dirname, "extensions", "generate-blocks.js"));

class FakeObjectGroup {
    constructor(name) {
        this.name = name;
        this.properties = {};
        this.objects = [];
    }

    setProperty(key, value) {
        this.properties[key] = value;
    }

    addObject(object) {
        this.objects.push(object);
    }
}

class FakeMapObject {
    constructor() {
        this.shape = undefined;
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
    }
}
FakeMapObject.Rectangle = "rectangle";

function installTiled(activeAsset) {
    const calls = { log: [], alert: [] };
    globalThis.tiled = {
        activeAsset: activeAsset,
        log: (message) => calls.log.push(message),
        alert: (message) => calls.alert.push(message)
    };
    globalThis.ObjectGroup = FakeObjectGroup;
    globalThis.MapObject = FakeMapObject;
    return calls;
}

test.afterEach(() => {
    delete globalThis.tiled;
    delete globalThis.ObjectGroup;
    delete globalThis.MapObject;
});

function tileLayer(name, rows, options = {}) {
    const originX = options.originX || 0;
    const originY = options.originY || 0;
    const width = rows.length > 0 ? rows[0].length : 0;
    const height = rows.length;
    return {
        name: name,
        isTileLayer: true,
        isGroupLayer: false,
        parentLayer: options.parentLayer || null,
        offset: options.offset || { x: 0, y: 0 },
        width: width,
        height: height,
        tileAt: (x, y) => {
            const row = rows[y - originY];
            if (row === undefined) return null;
            return row[x - originX] === "#" ? {} : null;
        },
        region: () => ({ boundingRect: { x: originX, y: originY, width: width, height: height } })
    };
}

function objectLayer(name) {
    return { name: name, isTileLayer: false, isGroupLayer: false, parentLayer: null };
}

function groupLayer(name, children, options = {}) {
    const group = {
        name: name,
        isTileLayer: false,
        isGroupLayer: true,
        parentLayer: null,
        offset: options.offset || { x: 0, y: 0 },
        layerCount: children.length,
        layerAt: (i) => children[i]
    };
    children.forEach((child) => { child.parentLayer = group; });
    return group;
}

function fakeMap(layers, options = {}) {
    const map = {
        isTileMap: true,
        infinite: options.infinite || false,
        tileWidth: options.tileWidth || 32,
        tileHeight: options.tileHeight || 32,
        selectedLayers: options.selectedLayers || [],
        layers: layers.slice(),
        macroCalls: [],
        inserted: [],
        get layerCount() {
            return this.layers.length;
        },
        layerAt(i) {
            return this.layers[i];
        },
        insertLayerAt(index, layer) {
            this.inserted.push({ index: index, layer: layer });
            this.layers.splice(index, 0, layer);
        },
        macro(name, fn) {
            this.macroCalls.push(name);
            fn();
        }
    };
    return map;
}

function grid(rows) {
    const height = rows.length;
    const width = height > 0 ? rows[0].length : 0;
    return { width: width, height: height, isSolid: (x, y) => rows[y][x] === "#" };
}

function mesh(rows) {
    const g = grid(rows);
    return gb.greedyMesh(g.width, g.height, g.isSolid);
}

function assertExactCover(width, height, isSolid, rects) {
    const covered = new Uint8Array(width * height);
    rects.forEach((r) => {
        assert.ok(r.width > 0 && r.height > 0, "empty rectangle " + JSON.stringify(r));
        assert.ok(r.x >= 0 && r.y >= 0 && r.x + r.width <= width && r.y + r.height <= height,
            "rectangle outside the grid " + JSON.stringify(r));
        for (let y = r.y; y < r.y + r.height; y++) {
            for (let x = r.x; x < r.x + r.width; x++) {
                assert.ok(isSolid(x, y), "empty cell covered at " + x + "," + y);
                assert.equal(covered[y * width + x], 0, "cell covered twice at " + x + "," + y);
                covered[y * width + x] = 1;
            }
        }
    });
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            if (isSolid(x, y)) assert.equal(covered[y * width + x], 1, "solid cell not covered at " + x + "," + y);
        }
    }
}

function seededRandom(seed) {
    let state = seed >>> 0;
    return () => {
        state = (state + 0x6d2b79f5) >>> 0;
        let t = state;
        t = Math.imul(t ^ (t >>> 15), t | 1);
        t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

test.describe("greedyMesh", () => {
    test("an empty grid gives no rectangles", () => {
        assert.deepEqual(mesh(["...", "..."]), []);
    });

    test("a grid with zero width or height gives no rectangles", () => {
        assert.deepEqual(gb.greedyMesh(0, 5, () => true), []);
        assert.deepEqual(gb.greedyMesh(5, 0, () => true), []);
    });

    test("a full grid gives one rectangle", () => {
        assert.deepEqual(mesh(["####", "####", "####"]), [{ x: 0, y: 0, width: 4, height: 3 }]);
    });

    test("one solid cell gives one 1x1 rectangle at its position", () => {
        assert.deepEqual(mesh(["...", "..#", "..."]), [{ x: 2, y: 1, width: 1, height: 1 }]);
    });

    test("an L shape merges the top row first, then the rest", () => {
        assert.deepEqual(mesh(["###", "#..", "#.."]), [
            { x: 0, y: 0, width: 3, height: 1 },
            { x: 0, y: 1, width: 1, height: 2 }
        ]);
    });

    test("a strip extends down while every cell below it is solid", () => {
        assert.deepEqual(mesh(["##.", "##.", "###", "#.."]), [
            { x: 0, y: 0, width: 2, height: 3 },
            { x: 2, y: 2, width: 1, height: 1 },
            { x: 0, y: 3, width: 1, height: 1 }
        ]);
    });

    test("a checkerboard gives one rectangle per solid cell", () => {
        const rows = ["#.#.", ".#.#", "#.#."];
        const rects = mesh(rows);
        assert.equal(rects.length, 6);
        rects.forEach((r) => assert.deepEqual([r.width, r.height], [1, 1]));
        const g = grid(rows);
        assertExactCover(g.width, g.height, g.isSolid, rects);
    });

    test("a grid that is not square keeps x and y apart", () => {
        assert.deepEqual(mesh([".......", "......#", "......#"]), [{ x: 6, y: 1, width: 1, height: 2 }]);
    });

    test("random grids are covered exactly, with no overlap and nothing outside the grid", () => {
        const random = seededRandom(474);
        for (let run = 0; run < 200; run++) {
            const width = 1 + Math.floor(random() * 20);
            const height = 1 + Math.floor(random() * 20);
            const density = random();
            const cells = [];
            for (let i = 0; i < width * height; i++) cells.push(random() < density);
            const isSolid = (x, y) => cells[y * width + x];
            assertExactCover(width, height, isSolid, gb.greedyMesh(width, height, isSolid));
        }
    });
});

test.describe("layer selection", () => {
    test("isCollisionTilesLayer accepts tile layers whose name starts with collision_tiles", () => {
        assert.equal(gb.isCollisionTilesLayer(tileLayer("collision_tiles", [])), true);
        assert.equal(gb.isCollisionTilesLayer(tileLayer("collision_tiles_1", [])), true);
    });

    test("isCollisionTilesLayer rejects other names and object layers", () => {
        assert.equal(gb.isCollisionTilesLayer(tileLayer("my_collision_tiles", [])), false);
        assert.equal(gb.isCollisionTilesLayer(tileLayer("blocks", [])), false);
        assert.equal(gb.isCollisionTilesLayer(objectLayer("collision_tiles")), false);
    });

    test("allLayers finds layers in nested groups, in map order", () => {
        const a = tileLayer("a", []);
        const b = tileLayer("b", []);
        const c = tileLayer("c", []);
        const inner = groupLayer("inner", [c]);
        const outer = groupLayer("outer", [b, inner]);
        const d = objectLayer("d");
        const names = gb.allLayers(fakeMap([a, outer, d])).map((layer) => layer.name);
        assert.deepEqual(names, ["a", "outer", "b", "inner", "c", "d"]);
    });

    test("topLevelAncestor returns a top-level layer itself", () => {
        const a = tileLayer("a", []);
        assert.equal(gb.topLevelAncestor(a), a);
    });

    test("topLevelAncestor returns the outer group of a nested layer", () => {
        const c = tileLayer("c", []);
        const inner = groupLayer("inner", [c]);
        const outer = groupLayer("outer", [inner]);
        assert.equal(gb.topLevelAncestor(c), outer);
    });
});

test.describe("coordinate math", () => {
    test("totalOffset adds the offsets of the layer and all its parent groups", () => {
        const c = tileLayer("c", [], { offset: { x: 1, y: 2 } });
        const inner = groupLayer("inner", [c], { offset: { x: 10, y: 20 } });
        groupLayer("outer", [inner], { offset: { x: 100, y: 200 } });
        assert.deepEqual(gb.totalOffset(c), { x: 111, y: 222 });
    });

    test("layerBounds of a fixed-size map is the layer size from the origin", () => {
        const layer = tileLayer("t", ["###", "###"]);
        assert.deepEqual(gb.layerBounds(fakeMap([layer]), layer), { x: 0, y: 0, width: 3, height: 2 });
    });

    test("layerBounds of an infinite map is the region bounds, including negative coordinates", () => {
        const layer = tileLayer("t", ["##", "##"], { originX: -4, originY: -2 });
        assert.deepEqual(gb.layerBounds(fakeMap([layer], { infinite: true }), layer),
            { x: -4, y: -2, width: 2, height: 2 });
    });

    test("generateBlocksForLayer converts cells to pixels with the tile size", () => {
        installTiled();
        const source = tileLayer("collision_tiles", ["...", ".##"]);
        const map = fakeMap([source], { tileWidth: 16, tileHeight: 8 });
        gb.generateBlocksForLayer(map, source, "STAMP");
        const object = map.inserted[0].layer.objects[0];
        assert.deepEqual([object.x, object.y, object.width, object.height], [16, 8, 32, 8]);
    });

    test("generateBlocksForLayer adds the layer and group offsets to the positions", () => {
        installTiled();
        const source = tileLayer("collision_tiles", ["#"], { offset: { x: 3, y: 4 } });
        const group = groupLayer("group", [source], { offset: { x: 30, y: 40 } });
        const map = fakeMap([group]);
        gb.generateBlocksForLayer(map, source, "STAMP");
        const object = map.inserted[0].layer.objects[0];
        assert.deepEqual([object.x, object.y], [33, 44]);
    });

    test("generateBlocksForLayer adds the region origin on an infinite map", () => {
        installTiled();
        const source = tileLayer("collision_tiles", ["#"], { originX: -2, originY: -1 });
        const map = fakeMap([source], { infinite: true });
        gb.generateBlocksForLayer(map, source, "STAMP");
        const object = map.inserted[0].layer.objects[0];
        assert.deepEqual([object.x, object.y], [-64, -32]);
    });
});

test.describe("generated layer", () => {
    test("the layer has the stamped name and the source properties", () => {
        installTiled();
        const source = tileLayer("collision_tiles_1", ["#"]);
        const map = fakeMap([source]);
        gb.generateBlocksForLayer(map, source, "20260107_090503");
        const layer = map.inserted[0].layer;
        assert.equal(layer.name, "blocks_20260107_090503_collision_tiles_1");
        assert.deepEqual(layer.properties, { source_layer: "collision_tiles_1", generated_at: "20260107_090503" });
    });

    test("the layer is inserted directly after the source layer", () => {
        installTiled();
        const below = objectLayer("below");
        const source = tileLayer("collision_tiles", ["#"]);
        const above = objectLayer("above");
        const map = fakeMap([below, source, above]);
        gb.generateBlocksForLayer(map, source, "STAMP");
        assert.equal(map.inserted[0].index, 2);
    });

    test("the layer is inserted directly after the top-level group of a nested source", () => {
        installTiled();
        const source = tileLayer("collision_tiles", ["#"]);
        const group = groupLayer("group", [source]);
        const map = fakeMap([objectLayer("first"), group, objectLayer("last")]);
        gb.generateBlocksForLayer(map, source, "STAMP");
        assert.equal(map.inserted[0].index, 2);
    });

    test("a source with no tiles inserts no layer, returns 0 and logs", () => {
        const calls = installTiled();
        const source = tileLayer("collision_tiles", ["...", "..."]);
        const map = fakeMap([source]);
        assert.equal(gb.generateBlocksForLayer(map, source, "STAMP"), 0);
        assert.equal(map.inserted.length, 0);
        assert.equal(calls.log.length, 1);
    });

    test("the objects are unnamed rectangles and the count is returned", () => {
        installTiled();
        const source = tileLayer("collision_tiles", ["#.#"]);
        const map = fakeMap([source]);
        assert.equal(gb.generateBlocksForLayer(map, source, "STAMP"), 2);
        map.inserted[0].layer.objects.forEach((object) => {
            assert.equal(object.shape, FakeMapObject.Rectangle);
            assert.equal(object.name, undefined);
        });
    });
});

test.describe("generateBlocks", () => {
    test("with no active asset it alerts and changes nothing", () => {
        const calls = installTiled(null);
        gb.generateBlocks([tileLayer("collision_tiles", ["#"])], "description");
        assert.equal(calls.alert.length, 1);
    });

    test("with an active asset that is not a map it alerts and changes nothing", () => {
        const tileset = { isTileMap: false, macro: () => assert.fail("macro called") };
        const calls = installTiled(tileset);
        gb.generateBlocks([tileLayer("collision_tiles", ["#"])], "description");
        assert.equal(calls.alert.length, 1);
    });

    test("with no source layers it alerts and changes nothing", () => {
        const map = fakeMap([]);
        const calls = installTiled(map);
        gb.generateBlocks([], "description");
        assert.equal(calls.alert.length, 1);
        assert.equal(map.macroCalls.length, 0);
    });

    test("several sources run in one macro and share one stamp", () => {
        const first = tileLayer("collision_tiles_1", ["#"]);
        const second = tileLayer("collision_tiles_2", ["##"]);
        const map = fakeMap([first, second]);
        installTiled(map);
        gb.generateBlocks([first, second], "description");
        assert.equal(map.macroCalls.length, 1);
        assert.equal(map.inserted.length, 2);
        const stamps = map.inserted.map((entry) => entry.layer.properties.generated_at);
        assert.equal(stamps[0], stamps[1]);
    });
});

test.describe("timestamp", () => {
    test("the format is yyyyMMdd_HHmmss with zero padding", () => {
        const RealDate = Date;
        globalThis.Date = class extends RealDate {
            constructor(...args) {
                if (args.length === 0) super(2026, 0, 7, 9, 5, 3);
                else super(...args);
            }
        };
        try {
            assert.equal(gb.timestamp(), "20260107_090503");
        } finally {
            globalThis.Date = RealDate;
        }
    });
});
