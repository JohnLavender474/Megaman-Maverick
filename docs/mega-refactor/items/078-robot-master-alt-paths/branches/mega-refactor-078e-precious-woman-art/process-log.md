# Process log: mega-refactor-078e-precious-woman-art

## Status

- State: merged
- Goal: Precious Woman art make-over: a jewel mine in a crystal abyss; one new original tileset, no back-wall
  tiles, a new parallax background
- Done: art rounds 1 to 10, implementation, rounds 8 (laser loop, pink block burst) and V ceilings, play test passed
  (2026-11-23), squash-merged into `master`.
- Next:
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-20

- Branch start. Owner: "Let's start a new branch of 'mega-refactor' item 078 to update and refine the tileset in
  Precious Woman's stage. The decorative crystal fixtures throughout the level are ripped from Mega Man 5. The purple
  rock tileset is overly simplistic and repetitive. The stage needs more of a "deep cave of jewels" look to it with an
  awe-inspiring background where the rocky wall tiles are not drawn."
- Owner answers (2026-11-20): background concept "crystal abyss"; stage identity "jewel mine"; Fable confirmed.
  Owner approved the short design (`docs/NOTES.md`, "Decisions").
- Before-renders of the 8 rooms: `temp/078e-precious-art/_before/` (main checkout), script
  `temp/078e-precious-art/render_before.py`.
- Art round 1 (1 Claude Fable 5.2 art lead): generator `make_precious.py`, a 541-cell tileset, 4 background layers
  (far depth, far lake, mid cliffs, near cliffs), previews of all 8 rooms with the entity sprites, 9 option renders;
  39 PASS, 0 FAIL, 12 INFO (`temp/078e-precious-art/checks.txt`); choices in `STYLE.md` ("Round 1 choices"),
  questions in `review.md`. Finding: the old crystal pillar tiles are solid cells, so the new columns stay in
  `tiles1`, not in `decor`.
- Review page (Q0 by Claude: the far background may be too dim for "awe-inspiring"; Q1 to Q8 by the art lead):
  https://claude.ai/artifact/7uufYEZeBb658d2gSffu5j
- Owner accepted all round-1 recommendations (Q0 B, Q1 to Q6 A, Q7 B, Q8 B; `docs/NOTES.md`). Q7 B widens the scope:
  the `PreciousBlock` frames are redrawn (`SCOPE.md`, in scope 5). Art round 2 (a new Claude Fable 5.2 art lead):
  a brighter, larger background, no floor crystals, the `PreciousBlock` frames.
- Art round 2 done: `make_precious_r2.py`, outputs in `*/round2/`; 43 PASS, 0 FAIL, 13 INFO (`checks2.txt`). One
  check limit lowered by the art lead (the lake copy equals the depth copy in its top 96 rows, not 216, because the
  lake light rises above the shore); accepted, reason in `STYLE.md`. Review page version 2 (same link): R1 boss-room
  far layer, R2 `PreciousBlock` animation, R3 (Claude) the hard mist bands at the spire feet.
- Owner: R1 B, R2 A, R3 A (accepted the recommendations); haze source "surface cracks" (`docs/NOTES.md`). Art round 3
  (a new Claude Fable 5.2 art lead).
- Art round 3 done: `make_precious_r3.py`, outputs in `*/round3/`; 63 PASS, 0 FAIL, 15 INFO (`checks3.txt`). The far
  layer is 7 textures by map height (lake, 5 height bands, boss); daylight shafts grow from `room3` to the top; the
  spire feet fade in a dither. The art lead replaced the lake-join check (its base texture is gone) with a join check
  of the stacked far textures, and added level-edge, shaft and boss-dim checks. Review page version 3 (same link):
  S1 to S3 (art lead), S4 (Claude, shaft strength).
- Owner accepted the recommendations of review page version 3 (S1 to S4 A; `docs/NOTES.md`). Art final.
- Scan for the brief: `BackgroundLayerBuilder` already builds custom backgrounds from map properties (`atlas`,
  `region`, `rows`, `columns`, `offset_x`/`offset_y`, `parallax_x`/`parallax_y`), so the only code change is a
  `BACKGROUNDS_11` entry. Correction: the README "Precious Woman" credit (mrgranade) is for the boss sprite; the old
  stage tiles have no credit line. `SCOPE.md` and `NOTES.md` updated.
- Brief published: https://claude.ai/artifact/TCpMwbgT1TvzAp6WMPbXT9 (D1 credits, D2 unused old tilesets).
- Owner D1: "The tileset is a mix of mrgranade and Capcom. Keep them credited along with Fable 5.2." D2: keep the
  old tileset files (accepted the recommendation). `SCOPE.md` approved; manual checks filled in; `PLAN.md` written
  (6 steps). End of the planning phase.

## 2026-11-20 (implementation)

- Step 1 done: `precious_woman_mine_tileset.png` (512 x 1056, 526 tiles) and `.tsx` (19 animated tiles, 80 frames)
  in `assets/tiled_maps/`. Script `temp/mega-refactor-078e-precious-woman-art/build_tileset.py` (draws with
  `precious_art.py`, a copy of `make_precious_r3.py`): the image is pixel-equal to `tiles/round3/`, the animations
  equal `tiles_index.tsv`, every frame id is in range. The copy reruns the round-3 checks in the worktree:
  `checks3.txt`, `mapping3.tsv` and `decor_cells3.tsv` are byte-equal to the main checkout (63 PASS, 0 FAIL).
- Step 2 done: `PreciousWoman_v2.tmx` links only `precious_woman_mine_tileset`; `tiles1` 2543 cells (master 2665
  less the 122 `mm9jewel` decor cells that round 1 dropped), `tiles2` with no cells, new `decor` layer (553 cells,
  equal to `decor_cells3.tsv`). Script `repaint_map.py` (reads a `git show master:` copy of the map): a render of
  each of the 8 rooms from the TMX and the new tileset image is pixel-equal to the round-3 render without
  backgrounds; the 10 object layers other than `backgrounds` are byte-equal to `master`; every gid is in range and
  no cell is flipped.
- Merged `master` into the branch (`078d` merged with `Backgrounds11`, `TextureAsset.kt` and `README.md`); no
  conflict. This branch's atlas is `Backgrounds12` (`PLAN.md` step 3 changed).
- Owner decision (step 3): the boss room gets one fixed picture, with a seam in the pan from `room7`
  (`docs/NOTES.md`).
- Step 3 done: 9 frames in `assets/sprites/frames/Backgrounds12/PreciousWoman/`, `Backgrounds12.tps` (a copy of
  `Backgrounds10.tps`: `Basic`, `png8`, no trim), atlas 1540 x 1348; `BACKGROUNDS_12` in `TextureAsset`; `CrystalBKG`
  replaced by 9 custom objects (6 far bands at their map rows with parallax 0.1 / 1, mid 0.25 / 0.25, near 0.5 / 0.5,
  `PreciousBossRoom` fixed, priorities 0 to 3; 66 sprite copies, `CrystalBKG` had 50). Math: the level start zeroes
  the prior camera position, so the background camera is `initPos + parallax * game camera` at all times, and a
  `--room` start equals a walk-in. Scripts `build_backgrounds.py`, `check_backgrounds.py` (`checks_step3.txt`): each
  atlas region equals its frame and the round-3 texture (clear pixels aside, which `png8` gives an RGB); the 22
  round-3 views rendered from the object data are pixel-equal to the round-3 background renders; the 6 far bands join
  edge to edge. `sweep_backgrounds.py`: 302 camera positions in the 8 rooms, no copy runs out.
- Step 4 done: `blocks/round2/PreciousBlock.png` replaces `assets/sprites/frames/Platforms1/PreciousBlock.png`;
  `Platforms1` repacked (`Basic`; layout file unchanged). Script `check_platforms.py`: the new region equals the frame
  strip; 69 of 71 regions pixel-equal; the `png8` quantizer moved 2 old regions, the same 2 as in `078b`:
  `BreakableIce/SmallShards` 5 px and `Platform2` 119 px, max RGB step 11, alpha unchanged (accepted as in `078b`).
  `core:smokeTest` runs in step 6.
- Step 5 done: the 4 credit lines of `SCOPE.md` (in scope 6) in `README.md` under "Tilesets", after Reactor Man, in
  the "Made by" / "Inspired by" pattern of `docs/ways-of-working/art.md`. The Megaman Maker "Crystal Cave
  Background" line stays: `CrystalBKG` stays in `Backgrounds6` and the builder.
- Step 6: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` passes
  (Corretto 17). Review before the play test: 1 Sonnet agent, the diff against `master` `b3d739da6` and the 6
  manual checks. No real finding; 12 points verified as not a problem (background properties and regions, the
  `--room` = walk-in math, coverage, the boss picture, the `decor` layer is drawn and skipped by the builders, no code
  reads the old tilesets, the `.tsx` format, the 10 object layers byte-equal to `master`, `PreciousBlock` regions).
  All 6 checks valid (each `--room` has a player spawn; `room5` has none). Play scripts in `temp/play/`.
- Play-test round 1, owner: "Make the jeweled platforms more refined and detailed like in the original spritesheet.
  Let's make the hazy background animated. Update the sparkling sprites to be new." Answers in `docs/NOTES.md`. Art
  round 4 (a Claude Fable 5.2 art lead) in `temp/078e-precious-art/` of the main checkout.
- Art round 4 done (art lead): `make_precious_r4.py`, 74 PASS, 0 FAIL, 16 INFO (`checks4.txt`); the 4 jewel pieces
  at 32 px per cell, 17 star glints (5 frames), the haze overlay (6 frames of 512 x 448 in a 2 x 3 grid, 0.3 s,
  parallax 0.15 / 1); tile sheet 560 cells (526), same 465 tile names, decor cells unchanged. Open choices T1 to T4
  in `review4.md`, all with recommendation A (as drawn); the play test decides.
- Round 4 in the branch (uncommitted while the Diffity round is open): `precious_art.py` is now a copy of
  `make_precious_r4.py` (the round-3 copy is `precious_art_r3.py`). Tileset rebuilt (512 x 1120, 560 tiles, 19
  animated, 114 frames; pixel-equal to `tiles/round4/`); map repainted (8 room renders equal to round 4; 10 object
  layers equal to `master`); `PreciousHazeMotes` added as an `AnimatedBackground` object (`animation_rows` 2,
  `animation_columns` 3, `duration` 0.3, priority 1; mid, near and the boss picture move to priorities 2 to 4). The
  haze grid (1536 x 896) does not fit in `Backgrounds12` with `Basic` (TexturePacker exit 10), so it is in a new
  atlas `Backgrounds13` with `BACKGROUNDS_13` (rule of `docs/ways-of-working/assets.md`). `check_backgrounds.py`:
  22 views equal to the round-4 renders (with the brightest haze frame); `sweep_backgrounds.py`: 302 positions, no
  gaps. `PreciousBlock` round 4 in `Platforms1`: region equals the strip; the same 2 old regions shift (`png8`):
  `SmallShards` 5 px max RGB 11, `Platform2` 45 px max RGB 20, alpha unchanged. `SCOPE.md` (in scope 8, contract
  checks 1 and 3) and the brief updated and published again (version 3); play scripts 1 and 3 updated.
- Round 4: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` passes. Play-test round 2 handed over (checks 1, 3 and 6 show the round-4 art).
- Play-test round 2, owner: redraw `DrillHead`, `PreciousTron`, `LaserBeamer` and `Laser` (`docs/NOTES.md`). Owner
  answers: new beam everywhere (Wily Stage 1 too), red. Art round 5 (art lead): `make_entities_r5.py`, 90 PASS,
  0 FAIL, 4 INFO (`checks5.txt`); no region grid, frame size or duration changed; new `Hazards1` regions
  `Laser/beam` (8 frames of 16 x 9 px), `Laser/origin` and `Laser/end` (4 frames of 16 x 16 px), 0.05 s. Open choices
  U1 to U6 in `review5.md`, all applied with recommendation A.
- Round 5 in the branch (uncommitted): frames copied; `Enemies1` (11 regions redrawn) and `Hazards1` (1 redrawn, 3
  new) repacked; `check_atlas.py`: every other region pixel-equal, no `png8` shift. `Laser.kt`: the 4 px squares
  replaced by rotated beam segments of 1 tile x 18 px, end to end, the last one cut at the end point; segment `i`
  shows beam frame `(tick - i) mod 8` (the pulse runs from the emitter); an origin glow at the first point (not on a
  reflected laser) and an end glow at the end point; body, damage, reflection, timers and light events unchanged.
  `WhiteBurst` gets a `hidden` spawn option (`ConstKeys.HIDDEN`), which `Laser` sets: the burst keeps its
  `EXPLOSION` fixture at the end point, only its sprite is hidden. Contract: checks 1, 2, 4, 5 updated, check 7 (Wily
  Stage 1, `room3`) added; play scripts rewritten (7).
- Review before the play test of rounds 4 and 5 (1 Sonnet agent, the uncommitted diff, 7 checks): no code bug in
  `Laser.kt` or `WhiteBurst.kt` (beam geometry, last-segment cut, frame split and index, pooled reuse, off/on,
  reflected lasers, `WhiteBurst` users, style); the haze object, atlases and regions verified. 1 real finding: check 2
  (`room2`) has no `PreciousBlock`, so it cannot show a reflection; fixed: the reflection moved to check 3 (`room3`:
  1 beamer, 12 blocks). Note: a `LaserBeamer` facing down, left or right starts its beam about 3 px from the ruby
  point (old code, same as with the old art).

## 2026-11-22

- Owner: the new art may have pixels smaller than NES pixels; scan, fix, and put NES pixel size and palettes in the
  art doc. Scan (`scan_nes_grid.py`, `scan_nes_palette.py` in `temp/mega-refactor-078e-precious-woman-art/`): 68 of
  560 tiles (30 column, 3 slab, `gem_block`, 35 glint tiles) and `PreciousBlock` (393 of 768 2x2 blocks) are finer
  than 1 NES pixel; backgrounds, haze and the round-5 entities are at NES size. No 078e color is an exact NES color.
  Owner: NES palette with NES counts, for all 078e art (`docs/NOTES.md`). Rule added to
  `docs/ways-of-working/art.md` on `master`. Art round 6 (a new Claude Fable 5.2 art lead). The Diffity server of
  round 1 stopped with the last session; it reopens at the next handover.
- Art round 6 done: `make_precious_r6.py` 76 PASS, 0 FAIL, 8 INFO; `make_entities_r6.py` 92 PASS, 0 FAIL, 5 INFO.
  4 background palettes (P0 rock, P1 pink, P2 mine, P3 abyss; backdrop `000000`), 2 sprite palettes per entity; the
  jewel tiles, glint stars and `PreciousBlock` redrawn on the NES grid, all else recolored; 6 pink gems on rock tops
  became white gems (`decor_cells6.tsv`). The agent reported that its session ran as Claude Opus 5.5 for part of the
  round; owner: keep it, credit both models (`README.md` line added). V1 to V5 (`review6.md`) applied as
  recommended; the owner decides at the play test.
- Round 6 in the branch (uncommitted): `precious_art.py` is now the r6 copy (r4 kept as `precious_art_r4.py`);
  tileset (pixel-equal to `tiles/round6/`), map (8 room renders equal to round 6, 10 object layers equal to
  `master`), `Backgrounds12` and `Backgrounds13` rebuilt (22 views equal, 302 camera positions with no gap);
  `PreciousBlock` in `Platforms1` (the same 2 old regions shift: `SmallShards` 5 px, `Platform2` 170 px, max RGB 12,
  alpha unchanged); entities and laser in `Enemies1` and `Hazards1` (no other region changed). Scans on the asset
  files: 0 blocks finer than 1 NES px; every color an exact NES color (tileset 7, backgrounds 4, entities 4 to 7).
- Round 6: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` passes (`core:smokeTest` ran on the new atlases). No code changed since the review of rounds 4 and 5. Play-test round 3 handed over.
- Owner: `DrillHead` and `PreciousTron` back to their `master` art. Frame strips and `Enemies1.png` restored from `HEAD` (equal to `master`); README credit, SCOPE and NOTES updated.
- Art round 7 (Claude Fable 5.2, tileset refinement): `checks7.txt` 78 PASS, 0 FAIL, 8 INFO. Integrated (uncommitted): `precious_art.py` is now the r7 copy (r6 kept as `precious_art_r6.py`); tileset 576 tiles, pixel-equal to `tiles/round7/`, 19 animated tiles; map renders of 8 rooms equal, 10 object layers equal to `master`, `tiles1` equal to `tiles1_cells7.tsv` (2543 cells); backgrounds rebuilt, 22 views PASS. NES scans: 0 blocks finer than 1 NES px in the full sheet; 7 colors, all exact NES; at most 4 colors per 8x8 area. README credit line added. No code change, so Gradle was not run again.
- Owner: lasers turn their pixels. Rule "No rotated pixels" added to `docs/ways-of-working/art.md` on `master`.
  Research (1 Opus agent): only `Laser.kt` turned pixels (`sprite.rotation = angle`); options A to F, recommendation
  D. Owner accepted the recommendations (`docs/NOTES.md`). Code (uncommitted): new `LaserBeamSprite` (one drawable
  that writes runs of 1-art-px beam columns as quads, the frame turned in 90-degree steps by the major axis);
  `Laser` builds the stepped cells on the 2 px grid, snaps the glows to the grid, and uses one beam clock for all
  lasers (`Gdx.graphics.frameId` guard). Body, damage, reflection and sweep unchanged. `core:test core:smokeTest detektMain` pass (play test
  in progress); play checks 2, 3 and 7 must run again.
- Owner: Mega Man stands 1 px below the top of the new `LaserBeamer` sprite (the new art is 1 art px taller than on
  `master`). Owner chose: the top of the 6 up-facing `ShieldBlock`s under the beamers in `PreciousWoman_v2.tmx` (ids
  119, 126, 250, 395, 524, 657) is 2 game px higher (`y - 2`, `height + 2`); no block touches their sides. Wily Stage 1
  keeps its old art and blocks. Play checks 2, 3 and 7 run again.
- Owner: the 2 px block raise did not fix it; Mega Man's feet cut into the black top outline of the beamer, so the
  beamer is off the NES pixel grid. The block raise is undone. Cause: the up-facing sprite was moved by
  `-0.1f * PPM` (3.2 game px, off the 2 px grid), and its art top was 4.8 to 6.8 game px above the block top. Fix:
  `UP_SPRITE_OFFSET_Y = -10f / PPM` in `LaserBeamer`, so the default art top row meets a block top 1.25 tiles above
  the beamer center; the Wily frame (one row lower in its frame) moved up 1 row in its frame strip and in
  `Hazards1.png` (234 px, only in its region; `temp/078e-beamer-grid/shift_wily_up.py`). Rule "One pixel grid for
  all art" added to `docs/ways-of-working/art.md` on `master`.
- Incident: the undo of the block raise used `git checkout -- PreciousWoman_v2.tmx`, which also dropped the
  uncommitted round-7 map. Rebuilt with the round scripts: `repaint_map.py` (PASS: 8 room renders equal, 10 object
  layers equal to `master`, 2543 tiles1 cells) and the TMX part of `build_backgrounds.py`
  (`temp/078e-beamer-grid/restore_bg_objects.py`; 10 background objects); `check_backgrounds.py` 22 views PASS.
- Owner accepted the recommendation: the 6 up-facing beamers whose block top is not 1.25 tiles above their center
  move so that it is (`temp/078e-beamer-grid/move_beamers.py`): Precious Woman 523 (+8 px down) and 658 (16 px up);
  Wily Stage 1 126 (8 down), 179, 211 and 381 (8 up). Each ignores its own block, so the beam start inside the
  block is not cut. The laser start point moves with the beamer.

## 2026-11-23

- Round 8 (scope 12, approved 2026-11-23; `PLAN.md` R8.1 to R8.5). Owner picks: synthesized sounds, a loop for all
  `LaserBeamer`s, approach A (the X6 junk-block recipe).
- R8.1: `temp/078e-sounds/synth.py` (2A03 pulse and noise models, `ffmpeg` libvorbis): 3 explosions and 3 loops;
  each loop continues exactly after its last sample. Owner picked `pink_block_explode_2` and `laser_loop_3`, now
  `assets/sounds/pink_block_explode.ogg` and `laser_loop.ogg` (`SoundAsset` `PINK_BLOCK_EXPLODE_SOUND`,
  `LASER_LOOP_SOUND`). Sounds made in this session by Claude Opus 5.5.
- R8.2: 1 Claude Fable 5.2 art agent (`temp/078e-pink-block/BRIEF.md`): `PinkBlock` (cut gem, outline `940084`)
  and `PinkBlock/debris1` to `debris6`, one palette `940084 F878F8 F8D8F8`; its `check_art.py` PASS. Owner: use it.
  `Platforms1` repacked; `check_atlas.py`: the new regions equal their frames; old regions keep their pixels except
  the png8 shift of `Platform2` and `BreakableIce/SmallShards` (bug candidate in `TODO.md`).
- R8.3: `BreakableBlock`: a `PinkBlock` break spawns 2 `Explosion`s (no damage) at 0 or +-0.5 tile per axis and
  throws 3 different debris of 6 (`FallingPieceDef.DEBRIS`, no turn); one sound per break group
  (`breakWithConnectedBlocks` does the recursion). Other types keep their pieces and `THUMP_SOUND`.
- R8.4: `LaserBeamLoop` (an object in the session updatables, as `ToxicLiquidClock`): `LaserBeamer` joins in
  `onSpawn`, leaves in `onDestroy`, and requests the loop each update while it beams on camera; the loop starts on a
  request and stops in the first update with none, and when the last beamer leaves. The one-time
  `LASER_BEAM_SOUND` of `LaserBeamer` is gone; `WilyFinalBoss` keeps it. `LaserBeamLoopTest` (6 cases).
- R8.5: `README.md` credit lines (the block art in the Precious Woman line; the 2 sounds under "Sounds").
- Review before the play test (1 Sonnet agent; all uncommitted laser, beamer, block and map changes; checks 2, 3, 7):
  1. the loop started again during the death fade (the beamer stays `beaming` until `PLAYER_DONE_DYIN`): fixed, a
  beamer requests only while its target player is live and in play; 2. literal booleans with no name: fixed
  (`loop = true`, `loop = false`); 3. a second `explodeAndDie` in one frame doubled the burst: fixed, both
  `explodeAndDie` and `breakWithConnectedBlocks` return when the block is already destroyed (also for the other
  block types); 4. the Wily frame bottom is now 1 row higher than the default skin: accepted, the top meets the
  block; 5. the new files are untracked: they go in the commit after the play test. A mutation run
  (`LaserBeamLoop`: no play-once guard, no stop) failed 3 of 6 cases. Contract checks 2 and 7 and the play scripts
  updated for round 8.
- After the fixes: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule` passes. Play checks 2 and 7 handed over for round 8.
- Owner (play test): the backgrounds look good, but the stalagmites and stalactites end in a flat cut where they meet
  their column, so they do not look grown into it. Art round 9: a new Claude Fable 5.2 art agent
  (`temp/078e-precious-art/round9/BRIEF.md` of the main checkout); only the spire bases change.
- Art round 9 done (Claude Fable 5.2; `round9/checks9.txt` 43 PASS, 0 FAIL, 2 INFO): the mid and near cliff crystals
  grow as tufts out of their cliff side, with pointed feet and dither wedges in the rock; the far spires and the lake
  crystals keep their feet (hidden, fading or at the water line). Owner: use it. Integrated (uncommitted):
  `precious_art.py` is now the r9 copy (r7 kept as `precious_art_r7.py`); `MidCliffs`, `NearCliffs` and `BossRoom`
  copied, `Backgrounds12` repacked (only these 3 regions changed, each equal to its frame; the far bands are
  byte-equal); `check_backgrounds.py` 22 views PASS. No code change, so Gradle was not run.
- Owner: some black pixels of the moving `PreciousBlock` change between frames. Cause: a 20 px sparkle is black in
  frames 1 and 2 and `6888FC` in frame 3. Fixed by script (`temp/078e-precious-block-black/fix_black.py`, a
  mechanical copy of the black mask, no new artistic choice): black in frame 3 too, in the frame strip and in the
  `Platforms1` region (palette index of black; 0 atlas pixels changed outside the region; region equals the strip).
  The black mask is now equal in all 3 frames.
- Owner: the center of the `PreciousBlock` changes weirdly in some frames. Cause: frame 3 has its own center (facet
  lines and sparkle moved) and frame 2's blue colors, so the cycle reads pink, blue, blue, not the R2 A cycle pink,
  violet, white. Owner chose: an art agent makes all 3 frames frame 1's shape with a true pink, violet, white cycle.
  Art round 10: a new Claude Fable 5.2 agent (`temp/078e-precious-art/round10/BRIEF.md` of the main checkout).
- Art round 10 done (Claude Fable 5.2; `round10/checks10.txt` 10 PASS): all 3 `PreciousBlock` frames have frame 1's
  shape; pink `F878F8`/`F8D8F8`, violet `9878F8`/`D8B8F8`, white `B8B8F8`/`F8F8F8`. Owner: use it. Integrated
  (uncommitted): strip copied, `Platforms1` repacked; `PreciousBlock`, `PinkBlock` and the 6 debris regions equal
  their frames; the png8 shift again moves `Platform2` (230 px, RGB up to 18) and `BreakableIce/SmallShards` (bug
  candidate in `TODO.md`).
- Owner: 182a is merged; use the V ceilings in the map and make the `DrillHead`s target the V tips, as in 182a check 4.
  Merged `master` into the branch (`b6d0431ff`); the uncommitted `TextureAsset.kt` line was set aside as a patch
  (`temp/078e-v-ceilings/`) and applied again after the merge, no conflict. Script `temp/078e-v-ceilings/convert.py`
  (copy of the map before: `PreciousWoman_v2.before.tmx`): 42 V points (the 182 research counted 26 on the older
  map; the art now has 3 V pairs) become `CeilingPolygonBlock` objects (ids 908 to 949); 26 blocks end 1 row higher;
  each block's V cells were all in its bottom row, across its full width (asserted). Wall `ShieldBlock` 546 (`room3`,
  1 wide, rows 54 to 61) covers the left half of the V at x 144 and stays. 15 `HoverSpot`s move to the tip x
  (nearest tip, ties to the lower x). The map has no `update_distance`, so the 182a `TODO` at `checkHeadContacts`
  does not apply here. No gravity change in the level. Renders with block outlines: `temp/078e-v-ceilings/after_*.png`.
  Contract: in scope 13, check 8, intended changes. Gradle not run (play test in progress).
- Rounds 4 to 10 and the V ceilings committed (`fb30771d1`). Owner: play tests pass, ready to merge (2026-11-23).
  `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` passes on the branch.
  `master` had 3 docs-only commits since the last merge of `master`, so no new merge. Other active branches: only
  183b also changes `README.md` (credit lines); no Diffity thread open.
