# Notes: 026 Per-level render layers and an allocation-free draw queue

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEMS 25 + 45: Render order rework (named render layers, draw queue with no per-sprite allocation)".

````text
1. Current state

- The pipeline is in `MegaLevelScreen.draw(Batch)` (`core/.../screens/levels/MegaLevelScreen.kt:971-1031`). The order is fixed:
  a. All `Background` objects, each with its own viewport (`:976`). They are sorted only among themselves (`:966`).
  b. The `DrawingSection.BACKGROUND` queue (`:981-985`).
  c. `tiledMapLevelRenderer.render(gameCamera)` (`:987`). `TiledMapLevelRenderer` (`engine/.../screens/levels/tiledmap/TiledMapLevelRenderer.kt`) calls `OrthogonalTiledMapRenderer.render()`, which draws every visible tile and image layer of the map, in TMX order, in one call.
  d. The `PLAYGROUND` queue, then the `FOREGROUND` queue (`:989-999`).
  e. The UI viewport and HUD (`:1001-1029`).
- Result: every tile layer is always above all BACKGROUND sprites and below all PLAYGROUND and FOREGROUND sprites. A level cannot put one tile layer in front of an entity and another tile layer behind it. A background cannot be in front of tiles (for example fog): `BackgroundLayerBuilder` reads a `section` property, but `draw()` draws all backgrounds first anyway, so that property does nothing. No live map uses it.
- `DrawingSection` has 3 values. `DrawingPriority(section, value)` is a mutable data class (`engine/.../drawables/sorting/DrawingPriority.kt`). I count 258 `DrawingPriority(` calls in `core`. The values in use are -10 to 20 (most common: PLAYGROUND 10 (38), PLAYGROUND 5 (32), PLAYGROUND 1 (26)). About 20 sites change `priority.section` or `priority.value` at run time.
- The draw queue: `SpritesSystem` (`engine/.../drawables/sprites/SpritesSystem.kt`) calls `game.addDrawable` for each sprite. `addDrawable` (`MegamanMaverickGame.kt:920-925`) puts the sprite in an `InsertionOrderPriorityQueue` for each section (`:811-812`). Each `add` allocates a `GamePair` (`InsertionOrderPriorityQueue.kt:22`, `pairTo`). That is one allocation for each sprite, every frame, plus O(log n) heap operations. The `size` bug is at `:20` (`override val size = queue.size` reads the size once, at construction).
- Some levels have many tile layers. `WilyStage3_v4.tmx` has 11 (`fade_tiles_1..9` with opacity 0.1-0.9, `tiles2`, `tiles1`), all drawn in step (c).
- X6 facts (`engine.md`, "The render loop" and "The standard main-object update" step 0): the PS1 uses an ordering table of 32 fixed buckets. Each object has a draw priority and a BG layer. Nightmare Pressure is drawn on BG layer 1 ("A boss on a BG layer"). So X6 uses bucket insertion with no sort, and objects can be attached to background layers. The direction of the X6 priority numbers is still an open question in `engine.md`, item 5.

2. Options

A. Tiled layer order is the source of truth. Put empty object layers (Tiled "class" = `RenderLayer`, or a name prefix such as `render:playground`) between the tile layers in the Tiled layer panel. At load, walk `map.layers` in order and make one render-layer entry per tile layer and per marker.
   Pro: what the designer sees in Tiled is what the game draws. Tiled draws layers in the same order, so the editor preview matches the game.
   Con: object layers that hold spawn data (`enemies`, `blocks`) are not render layers, so this needs a clear convention. Designers can reorder layers by mistake (the validator, item 2, can catch this).
B. A map property `render_layers = "bkg_far, bg_sprites, tiles1, tiles2, playground, tiles_front, foreground"`.
   Pro: explicit and easy to diff.
   Con: a second list to keep in sync with the layer panel. Typos are possible (validator needed). There is no visual feedback in Tiled.
C. A numeric `z` for everything (tile layers get a `z` property, sprites keep ints).
   Pro: smallest change from today.
   Con: this is the "magic number" problem the owner wants to remove. Numbers do not say what they mean, and inserting between 5 and 6 is awkward.
D. Named layers in code only. No per-level control.
   Con: it does not meet the owner's need.

3. Recommendation

Use named layers, with option A as the per-level source and a default list in code.
- `RenderPlan`: an ordered list of entries. The entry kinds are `BackgroundLayer` (parallax, see item 46), `TileLayer` (one TMX tile/image layer), `EntityLayer(name)` and the UI (always last, not configurable).
- Default plan, used when a map has no markers. It reproduces today's order exactly: `[backgrounds..., bg_sprites, <all tile layers in TMX order>, playground, foreground]`.
- Entity layer names are a small enum or registry in `core` (`BG_SPRITES`, `PLAYGROUND`, `FOREGROUND`, plus new ones such as `BEHIND_TILES`, `FRONT_TILES`). The engine knows only "named layer". Each sprite has `layer` and `order: Int` (the old `value`).
- The default layer comes from the entity schema (item 21). It can be overridden per spawn with a TMX property `render_layer`. Runtime changes stay possible: set `sprite.layer`.
- Tile layers are drawn one at a time with `BatchTiledMapRenderer.renderTileLayer(layer)` after one `setView(camera)` and one `AnimatedTiledMapTile.updateAnimationBaseTime()` per frame.
- Draw queue (item 45): one reusable `Array<IComparableDrawable>` bucket per entity layer, cleared each frame. Sort with a stable sort by `order` only. A stable sort keeps insertion order for equal values, so the per-sprite `GamePair` index is no longer needed. LibGDX `Array.sort(Comparator)` uses `Sort`/TimSort, which is stable and reuses its work array. Use one static comparator. Then delete `InsertionOrderPriorityQueue` (it has 2 known bugs).
- Unknown layer names fail at load (or in the validator, item 2), with the map name and the layer name in the message.
- Debug aids: a debug overlay (dev flag, item 7) that lists the plan in order, with the sprite count per layer. A key toggles each layer on and off. Log the resolved plan at level load.
- Migration with today's exact order:
  a. Add the new API. A mechanical codemod changes `DrawingPriority(DrawingSection.X, n)` to `RenderOrder(Layer.X, n)` for the 258 sites. Keep `n`.
  b. Change backgrounds to `BackgroundLayer` entries at the front of the plan.
  c. Check with screenshot comparison per room, or the record-and-replay trace (item 5): same images on `master` and on the branch.
  d. Only then add new layers and markers to individual maps.

4. Open questions for the owner

- Do you prefer marker layers in the Tiled panel (A) or a property list (B)?
- Must an entity have sprites in two layers at once (for example a body behind tiles and an arm in front)? The design supports this, but it affects how the schema declares the default.
- Must a background be able to appear in front of tiles or entities (fog, rain, foreground parallax)?
- Must the layer of a whole tile layer change at run time (for example a wall that moves in front of Mega after a trigger), or only its visibility?

5. Proposed backlog wording

Title: Per-level render layers and an allocation-free draw queue (merges 25 and 45)
- Replace `DrawingSection`/`DrawingPriority` literals with named render layers plus a small `order` int. Use a codemod for the 258 sites.
- Each level defines an ordered render plan: backgrounds, tile layers drawn one at a time, and entity layers. The source is the Tiled layer order (marker layers). Without markers, a default plan reproduces today's order.
- The entity schema gives the default layer. A spawn property `render_layer` overrides it.
- One reusable bucket per layer with a stable sort. No per-sprite allocation. Delete `InsertionOrderPriorityQueue`.
- Debug overlay that lists layers with counts and toggles. The validator rejects unknown layer names.

6. Dependencies

21 (schema default layer), 2 (validator), 5 (pixel or trace compare), 46 (backgrounds become plan entries), 8 (measure allocations before and after), 7 (debug flag).
````
