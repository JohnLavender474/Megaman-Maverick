# Plan: mega-refactor-078a-wily-stage-1-art

Sources: `temp/078-wily1-art/merged/` (tileset, map, `map_objects_proposal.md`), `temp/078-wily1-art/5-cave/generator/`.

1. **Tileset and map.** Copy `wily_stage_1_tileset.png/.tsx` into `assets/tiled_maps/`, write the new map over
   `WilyStage1_v2.tmx` with the asset paths, drop the crest at gate 266, delete the 928 `clear` cells, remove
   tilesets that no cell uses. Check: every block object and non-tile object is the same as on `master`; the
   room renders match `merged/rooms/`.
2. **Animated custom backgrounds with their own key.** `BackgroundLayerBuilder.kt`: animated when the object has
   `animation_rows`. Check: `core:test`; `IntroStage_v3.tmx` builds the same objects (unit test of the builder
   choice, or a read of the map).
3. **Background atlases.** Frames in `assets/sprites/frames/Backgrounds8/` and `Backgrounds9/`, `.tps` copies of
   `Backgrounds7.tps`, packed with `Basic`; `TextureAsset.BACKGROUNDS_8`, `BACKGROUNDS_9`. Check: every region
   that the map names is in its atlas.
4. **Background objects and triggers.** The objects of the proposal (cliff/pit, fortress, `WilyWall`, `SandDunes`,
   `CaveWall`, boss-room wall), the trigger key changes, the new hide trigger, `hidden_backgrounds`. Check: a
   script recomputes each `initPos` against the round-2 renders.
5. **`WilyGenerator` entity.** Regions in an atlas with free space (or a new page), the class in
   `entities/decorations/`, light events per `lights.tsv`, spark bursts through the game random API,
   `powerDown()`, the map object in `boss_room`, `entity-tags.txt`. Check: `core:smokeTest`,
   `detektMain core:verifyRandomRule`.
6. **Credits.** `README.md` per `docs/ways-of-working/art.md`.
7. **Checks.** The automated checks of `CONTRACT.md`, then the review before the play test.
