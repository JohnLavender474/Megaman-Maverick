# Process log: mega-refactor-078b-wily-stage-3-art

## Status

- State: merged
- Goal: Wily Stage 3 art make-over: one new tileset in the mood of the X6 Gate stages, the repainted map, and new backgrounds
- Done: planning (brief https://claude.ai/artifact/BtRkqv7FQtegZ726ki3RCD, approved 2026-11-19), PLAN.md; PLAN steps 1-6 (tileset, map repaint, backgrounds, lab belt, credits)
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch start. Owner direction: the stage keeps the X6 Gate stage music, so the art goes for a similar mood. The owner asked for a first draft from Claude's best guess (owner accepted the recommendation). Drafts: `temp/078-wily3-art/` in the main checkout.
- Scan: `WilyStage3_v4.tmx` has 16 rooms; `tiles1` paints 11 distinct tiles and `tiles2` 4 (`Tileset1`, 129 cells of `mm9endless`), one background `BKG18`. The 9 `fade_tiles_*` layers are opacity 0.1 to 0.9 copies of one tile: a fade from black in `intro_room` (drawn by the normal tile renderer). Room renders of today: `temp/078-wily3-art/_before/`.
- Draft 1 of the style guide: `temp/078-wily3-art/STYLE.md` (concept "the last lab": a cyan circuit trace in teal lab panels, scorched in the furnace rooms, violet at the core). Next: Fable confirmation, then art round 1.
- Owner confirmed Fable for this branch. Art round 1 (1 Claude Fable 5.2 art lead): shared set (101 tiles), 4 areas (129 area tiles), 3 backgrounds, previews of all 16 rooms; 30 of 30 checks pass (`temp/078-wily3-art/checks.txt`). Review page: https://claude.ai/artifact/XcDwaErABYx2Q47KjzrToB (8 open choices; 10 smaller choices stay as drawn unless the owner objects).
- Art round 2 (a new Claude Fable 5.2 art lead): the 8 round-1 decisions applied, lab conveyor belt frames (6 files, same format as `-Green`: 32x32 px of 2x2 frames of 16 px), emblem plates in the rooms before the fights with boss-sampled colors; 49 of 49 checks pass (`temp/078-wily3-art/checks2.txt`, `review2.md`). Review page version 2: 4 open choices (Timber Woman plate place, background parallax in `room3`/`room5`, belt lights, belt ends).
- Owner accepted the recommendations Q1 to Q4 of the round-2 review (`docs/NOTES.md`). Scope and contract drafted (`docs/SCOPE.md`); brief published: https://claude.ai/artifact/BtRkqv7FQtegZ726ki3RCD (1 decision: enemy skins).
- Owner D1: no enemy skins in `078b`; decide after the play test. Scope approved; `PLAN.md` written (7 steps). End of the planning phase.

## 2026-11-19 (implementation: PLAN steps 1-2)

- Q1 applied in a copy of the round-2 generator (`temp/mega-refactor-078b-wily-stage-3-art/build/gen/`): gem and leaf plates in `room7`, none in `precious_woman_room`.
- New tileset `wily_stage_3_tileset` (`.png` + `.tsx`): 187 tiles, 16 columns; 9 trace pulse animations (4 frames, 120 ms each).
- `WilyStage3_v4.tmx`: `tiles1`, `tiles2` and the 9 fade layers repainted (also the 1858 painted cells outside the rooms); new `decor` layer after `tiles1` (628 cells: trace, emblem plates, vents, lamps); flip bits kept on spikes and platforms.
- `Tileset1` and `mm9endless` removed from the map: no cell uses them.
- Checks (`build/checks.txt`): all PASS. Object layers byte-identical to `master`; no tile in an empty cell; spikes kept; all gids resolve; round-2 checks 49 PASS; every painted cell and all 16 room renders pixel-equal to round 2 (with Q1).

## 2026-11-19 (implementation: PLAN steps 3-6)

- Step 3: new atlas `Backgrounds10` (`WilyStage3/LabWall`, `WilyStage3/FurnaceGlow`, `WilyStage3/CoreField`, 512x448 each, frames = round-2 images), `.tps` copied from `Backgrounds9.tps` (`Basic`), `TextureAsset.BACKGROUNDS_10`. Check (`build2/check_atlas.py`, `build2/checks_step3.txt`): the 3 regions equal their frames.
- Step 4: the `BKG18` object is replaced by 7 world-locked background objects (`parallax_x` and `parallax_y` 1, `Backgrounds10`): `FurnaceGlowLow` (room1, room2, room3 bottom), `LabWallRoom3`, `LabWallEntrance`, `LabWallUpper` (room4, `inferno_man_room`, room5 above the furnace), `FurnaceGlowRoom5`, `LabWallHall` (room6 to room8 and the 4 refight rooms), `CoreField` (room9, `wily_room`); priorities 0 to 2 settle the overlaps. Decision: no show/hide triggers and no `hidden_backgrounds`: every object draws always and is fixed in the world, so a `--room` start cannot skip a trigger (the 078a pitfall). Decision: `parallax_x` 1 also (the old `BKG18` had 0.25), as the 078a interior walls, so each room matches the round-2 preview. Check (`build2/check_backgrounds.py`, `checks_step4.txt`, method in `build2/METHOD.txt`): 50 PASS; 13 rooms pixel-equal to the round-2 renders, `room1`, `intro_room` and `wily_room` show the same image with another copy phase (seamless joins to the next room); room3 and room5 furnace on the bottom 14 rows. Play-test watch: the copy rows jump 96 px at the `room5`/`room6` edge in that pan.
- Step 5: the 6 lab belt frames in `assets/sprites/frames/Platforms1/ConveyorBelt/` (`*-Lab.png`), `Platforms1` repacked (`Basic`); `ConveyorBelt` type `lab` (regions `*-Lab`, `AnimationDef(2, 2, 0.075f)`, middle parts per direction as `green`); `type=lab` on the 15 belts of `WilyStage3_v4.tmx` (no other map change). Atlas check (`build2/checks_step5.txt`): the 6 lab regions equal their frames (visible pixels); 63 of 65 old regions pixel-equal; `Platforms1` is `png8` and its frames already had 267 colors, so the quantizer palette moved: `BreakableIce/SmallShards` 11 px (max 11 RGB) and `Platform2` 116 px (max 19 RGB), alpha unchanged (accepted as the 078a `Enemies2` shifts). `core:smokeTest` passes.
- Step 6: `README.md` credit "Wily Stage 3 (tileset, backgrounds and the lab conveyor belt)", made by Claude Fable 5.2, directed by the developer. No "Inspired by" line: the art copies no pixels; it keeps only the teal color of `Tileset1` (no README entry; 078a owner rule: credit only known sources), redraws the `wily_room` circuit wall in its own style (the `mm9endless` tiles are not followed), takes only ideas from the X6 Gate stage notes, and the belt shares only the frame format of the green belt. Sources: `temp/078-wily3-art/STYLE.md`.
- Steps 3 to 6 accepted as done, with 3 changes from the plan (logged above by the step agent): no show/hide triggers (7 world-locked objects, 93 sprite copies against 10000 for `BKG18`), `parallax_x` 1 as well, and the `Platforms1` `png8` repack shift of 2 old regions.
- Automated checks: `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass (`temp/mega-refactor-078b-wily-stage-3-art/gradle-checks.txt`).
- Review before the play test (1 Sonnet agent): no bug; the 7 manual checks pass. Risk checked and dismissed: `Background.reset()` runs only in the level screen `reset()` (level exit), and the level start zeroes the prior camera position, so a respawn does not offset the backgrounds. Play-test watch: the 96 px jump of the lab wall pattern in the pan from `room5` to `room6`.
- Owner play test passed ("Stage looks good"); Diffity: no threads. Squash-merged into `master` as `e346c558d`.
