# Process log: mega-refactor-138a-wreck-into-mock-sprite

## Status

- State: merged (2026-11-13)
- Goal: Merge `Wreck` into `MockSpriteEntity` (renamed `MapSprite`, D2)
- Done: branch start; scan; draft `SCOPE.md`, `CONTRACT.md`, `docs/NOTES.md`
- Brief: https://claude.ai/artifact/EzY8ri3vJAXQLnzUToF7Bi
- Done: scope and contract approved (2026-11-13)
- Done: `PLAN.md` steps 1 to 5 (code, map, `Wreck` removed, `MapSpriteTest`, automated checks pass); not committed
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-13

- Branch start (owner chose this item while `060c` runs in another session; merge target: directly into `master`).
- Scan and drafts: `SCOPE.md` and `CONTRACT.md` (status draft), overlap in `docs/NOTES.md`. 2 `Wreck` spawners (X6 intro objects `218`, `219`); `MockSpriteEntity` has no user. No code changed.
- Brief published. Owner answers: D1 the sprite fills the object rect; D2 rename to `MapSprite`; D3 drop the old mock
  behavior. Scope and contract approved.
- `PLAN.md` steps 1 to 5 (implementation subagent):
  - `MockSpriteEntity.kt` renamed (`git mv`) to `MapSprite.kt` and rewritten: reads `atlas` and `region`
    (`TEXTURE_ASSET_PREFIX` + file name, as `BackgroundLayerBuilder`), one `GameSprite` at `BACKGROUND` 1, region and
    bounds set once in `onSpawn` from the object bounds, no flip, no animation, no updatable, no debug box, no
    companion static. A missing `atlas` or `region`, an atlas that is not loaded, or a region not in the atlas throws
    an `IllegalArgumentException` that names the map object id, the atlas and the region. `init()` needs no spawn
    props.
  - `X6Intro_16x14_v1.tmx`: objects `218` and `219` only (`git diff`: 6 lines added, 4 removed, all inside the 2
    objects). `218`: `MapSprite` 0, 1081, 96 x 72, `Enemies2.txt` / `Wreck/wreck_a`. `219`: `MapSprite` 1868, 1014,
    36 x 28, `Enemies2.txt` / `Wreck/wreck_b`.
  - Rect proof: `temp/138a/check_rects.py` (output `temp/138a/check_rects.out`, in the main checkout) reads the old
    objects and `Wreck.FRAMES` from `master`, flips Tiled y to game y (map height 1536 px; `TmxMapLoader` default
    `flipY`, unit scale 1), runs the `TickFrames.place` formula (scale 2) and flips back. Both new objects equal the
    old drawn rect: PASS. Game y-up rects: `218` left 0, bottom 383, 96 x 72; `219` left 1868, bottom 494, 36 x 28.
  - `Wreck.kt` deleted; `entity-tags.txt`: `Wreck` and `MockSpriteEntity` lines removed, `MapSprite` added. Grep: no
    `Wreck` class or `MockSpriteEntity` reference left outside `docs/` (only the region names `Wreck/wreck_*`).
  - `MapSpriteTest` (4 tests): region, bounds, no flip, `BACKGROUND` 1; pooled respawn takes the new region and bounds;
    a region not in the atlas throws (message names region, atlas, `id=218`); no `region` throws. 4 of 4 pass.
    Mutations: priority to `PLAYGROUND` 1: 1 test fails (the priority check); `setBounds` replaced by `setPosition`:
    2 tests fail (width 0). Code restored after each (checked with `cmp`).
  - Automated checks, one run of `engine:test core:test core:smokeTest lwjgl3:jar`: pass. `engine:test` 675 tests,
    `core:test` 1710, `core:smokeTest` 456 (includes "MapSprite builds and runs init()"), 0 failures, 0 skipped;
    `Megaman-Maverick-1.0.1.jar` built. `known-entity-failures.txt` not changed.
  - Implementation choices in `docs/NOTES.md` ("Decisions").
- Review before the play test (1 agent): no blocking bug; the manual check loads and runs as written; the rects draw at
  the same pixels. Findings: (1) wreck B can miss 1 frame where it enters the view (the spawner tests the camera of
  the frame before; its old 32 px box had a 4 px lead): accepted, a `TODO` at `MapSprite` and a backlog candidate in
  `docs/TODO.md`; (2) no test for the no-atlas and atlas-not-loaded throws: 2 tests added, and each fails when its
  guard is removed. (3) The test file was untracked: committed by path.
- The test setup only passed inside the full suite: alone, `MegaEntityFactory.init` read regions that the atlas mock
  did not give (a `ClassCastException`, then `Region is null: default`). Fixed: the mock gives a 64 px region for any
  name and `null` only for the missing-region case, as `MegaEntityFactoryTest` does. Note: `-Pkotest_filter_specs`
  does nothing in this build; the environment variable `kotest_filter_specs` works.
- Checks: `engine` 675, `core` 1712, smoke 456, JAR built.
- Owner: all play checks pass; Diffity: no threads. Merged into `master` (owner chose to merge before `060c`).
