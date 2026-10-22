# Notes: 028 Game-owned texture and animation cache

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEM 27: Game-owned texture cache".

````text
1. Current state

- 203 files keep `companion object` region maps or single `region` fields. They fill them in `init()` on first use. Example: `ReactorMan.kt:216-227` checks `if (regions.isEmpty)`. About 124 files have `var region: TextureRegion? = null` with `if (region == null) region = ...`. There are 561 `findRegion` calls. No file ever calls `regions.clear()` (0 matches).
- `AssetManager.getTextureRegion` (`engine/.../common/extensions/AssetManagerExtensions.kt:10`) does `findRegion(region)!!`. A missing region gives an NPE with no atlas or region name. `AnimationUtils.loadRegions` (`core/.../utils/AnimationUtils.kt:17-20`) says only `"Region is null: $key"`, with no atlas and no tag. `ReactorMan` collects all failed keys, which is the better pattern.
- LibGDX `TextureAtlas.findRegion` is a linear scan of all regions in the atlas. The statics hide this cost, but the cost moves to the first spawn (a hitch).
- Hazards:
  (a) On Android, the JVM process can live longer than the Activity and the `Game`. Statics then keep `TextureRegion`s that point to `Texture`s of a disposed `AssetManager`: black or garbage sprites after the app is reopened.
  (b) Per-level unload (item 32) disposes textures that the statics still hold.
  (c) GL context loss alone is not the main risk: LibGDX reloads managed textures in place, so the regions stay valid while the same `AssetManager` lives.

2. Options

A. Keep companion maps but clear them from one place (a registry of caches). Small change, but the statics stay.
B. Game-owned `TextureCache` service keyed by (atlas, region). It indexes each atlas once into a hash map when the atlas loads.
C. B plus animation definitions as data (atlas + region + `AnimationDef`). Entities ask for animations by key, not for regions.

3. Recommendation

Use C, built in two steps.
- `TextureCache` lives in the game/level context (item 15), not in a companion. API: `region(atlas: TextureAsset, name: String): TextureRegion` and `animation(defKey): Animation`.
- When an atlas finishes loading, build `ObjectMap<String, AtlasRegion>` from `atlas.regions`. This gives O(1) lookups and fixes the linear scan.
- Lifecycle: drop an atlas's entries when the atlas is unloaded (item 32). Drop all entries in `Game.dispose()`. Nothing is static, so Android Activity re-creation is safe.
- Missing regions throw `MissingRegionException("atlas=Enemies1.txt region=ReactorMan/run requested by ReactorMan")`. In dev mode, also list near matches with the same prefix. Collect all misses of one entity before the throw, as `ReactorMan` does.
- Animation defs as data: `AnimationDef` gains `atlas` and `region` fields. The entity schema (item 21) declares its defs. Shared frame arrays are built once in the cache, and each entity gets an `Animation.copy()`, because `Animation` has per-instance state (`currentIndex`, `elapsedTime` in `engine/.../animations/Animation.kt:26-34`).
- The smoke test (item 3) then inits every entity and fails on any miss. With defs as data, a build-time check (item 39) can verify regions against `.txt` atlases with no game run.

4. Open questions for the owner

- Do you want animation defs in Kotlin data objects first, or straight in JSON or YAML?
- Do some entities choose atlases at run time (for example by level variant)? Then the key must include the variant.

5. Proposed backlog wording

Title: Game-owned texture and animation cache
- Replace about 200 companion region maps with one `TextureCache` keyed by atlas + region. Index each atlas once on load.
- The cache is cleared on atlas unload and on game dispose. No statics hold GPU resources (Android-safe).
- Missing-region errors name the atlas, the region and the requesting entity, and list all misses at once.
- `AnimationDef` holds atlas + region. Entities request animations by key. Shared frames, per-entity copies.

6. Dependencies

Prerequisite for 32. Works with 21 (schema declares atlases and animations), 39 (build-time region check), 3 (smoke test), 15 (service injection), 20 (registry).
````
