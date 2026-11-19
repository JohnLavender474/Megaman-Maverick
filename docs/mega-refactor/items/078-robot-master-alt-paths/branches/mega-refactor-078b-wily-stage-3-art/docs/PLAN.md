# Plan: mega-refactor-078b-wily-stage-3-art

Sources: `temp/078-wily3-art/` in the main checkout (round 2: `make_wily3_r2.py`, `mapping2.tsv`,
`*/round2/`, `backgrounds/round2/placement.tsv`, `conveyor/`). Decisions: `docs/NOTES.md`.

1. **Final art set.** Apply Q1 in the generator (both plates in `room7`, none in `precious_woman_room`) and build
   one tileset sheet (shared, area tiles, trace animation frames) with its `.tsx` (Tiled tile animations).
   Check: the round-2 checks pass on the new output.
2. **Tileset and map.** Copy the tileset into `assets/tiled_maps/`; repaint `tiles1`, `tiles2` and the 9 fade
   layers; add the `decor` layer after `tiles1`; remove `Tileset1` and `mm9endless` from the map when no cell uses
   them. Check: script: every block object and non-tile object is as on `master`; no tile in a cell that is empty
   today; every spike cell keeps a spike tile.
3. **Background atlas.** `assets/sprites/frames/Backgrounds10/` (3 images), `.tps` copied from `Backgrounds9.tps`
   with `Basic`, packed; `TextureAsset.BACKGROUNDS_10`. Check: the 3 regions exist with the frame pixels.
4. **Background objects and triggers.** Replace the `BKG18` object with the lab wall, furnace glow and core field
   objects (`parallax_y` 1 in `room3` and `room5` per `placement.tsv`); show/hide triggers where 2 backgrounds meet;
   the map `hidden_backgrounds` property. Check: script recomputes each object position against the round-2
   placement renders.
5. **Lab belt.** 6 frames in `assets/sprites/frames/Platforms1/ConveyorBelt/`, `Platforms1` repacked;
   `ConveyorBelt` `lab` type; `type=lab` on the 15 belts. Check: script: every old `Platforms1` region keeps its
   name and pixels; `core:smokeTest`.
6. **Credits.** `README.md` per `docs/ways-of-working/art.md` (Claude Fable 5.2, directed by the developer).
7. **Checks.** The automated checks of the contract, then the review before the play test.
