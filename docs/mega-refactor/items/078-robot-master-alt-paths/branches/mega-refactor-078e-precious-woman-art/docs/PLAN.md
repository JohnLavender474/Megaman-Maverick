# Plan: mega-refactor-078e-precious-woman-art

How to do the work in `SCOPE.md`. Written after the brief was approved (2026-11-20). It needs no approval. When the
plan changes, write the reason in the process log.

Inputs: the round-3 art and its generator `make_precious_r3.py` in `temp/078e-precious-art/` of the main checkout
(`STYLE.md` holds every art decision). Build scripts and check output go in `temp/mega-refactor-078e-precious-woman-art/`
of this worktree. Read `docs/ways-of-working/assets.md`, `levels.md` and `temp-files.md` first. No game launch.
Gradle: mind `docs/ways-of-working/git.md` and the memory limits (no run under about 2 GiB available).

## Steps

### 1. Tileset into the assets

- Changes: a build script (a copy of `make_precious_r3.py`) writes the final tileset image to
  `assets/tiled_maps/png/` and a `.tsx` with the Tiled tile animations (glints, lantern flicker) to
  `assets/tiled_maps/tsx/`.
- Check: the tileset image is pixel-equal to `tiles/round3/` (or round 2, where round 3 left the tiles unchanged);
  every animation frame id points into the image; the round-3 tile checks pass on the final files.
- Done: yes (2026-11-20)

### 2. Map repaint

- Changes: `PreciousWoman_v2.tmx` links only the new tileset; `tiles1` remapped by `mapping3.tsv` and the neighbor
  rules; `tiles2` emptied (the layer stays, with no cells); a new `decor` tile layer after `tiles1` from
  `decor_cells2.tsv` (round 3 equals round 2).
- Check: a render of each room from the TMX equals the round-3 room render without backgrounds; every object of every
  object layer other than `backgrounds` is equal to `master`; no cell gid points outside the new tileset.
- Done: yes (2026-11-20)

### 3. Backgrounds

- Changes: the 9 textures (7 far, mid, near) as frames under `assets/sprites/frames/Backgrounds11/PreciousWoman/`,
  packed into a new `Backgrounds12` atlas; `BACKGROUNDS_12("Backgrounds12.txt")` in `TextureAsset`; the `CrystalBKG`
  object replaced by custom background objects (`atlas`, `region`, `rows`, `columns`, `offset_x`/`offset_y`,
  `parallax_x`/`parallax_y`, `priority`): one per far band at its map rows, plus the mid and near layers. Work out the
  offsets from `Background.move()`/`set()` (the background camera starts at `initPos` and moves by the game camera
  delta times the parallax), as in `078b` step 4.
- Check: a script that applies the `Background` math to the object data renders the 22 round-3 views and compares them
  with the round-3 renders; the far band joins show no seam; the `--room` start of each play check gives the same
  image as a walk-in.
- Changed (2026-11-20): `078d` merged with `Backgrounds11`, so this atlas is `Backgrounds12`. The boss room has one
  fixed picture in place of the dim far band (`docs/NOTES.md`, step-3 decision), so the 9 frames are 6 far bands, mid,
  near and `BossRoom`.
- Done: yes (2026-11-20)

### 4. `PreciousBlock` frames

- Changes: `blocks/round2/PreciousBlock.png` replaces `assets/sprites/frames/Platforms1/PreciousBlock.png`; repack
  `Platforms1`.
- Check: the new region equals the frame strip; every other `Platforms1` region is equal to `master` (report any
  palette shift, as in `078b` step 5); `core:smokeTest` passes.
- Done: yes (2026-11-20; `core:smokeTest` runs in step 6)

### 5. Credits

- Changes: the `README.md` lines of `SCOPE.md`, in scope 6.
- Check: the lines match `docs/ways-of-working/art.md`.
- Done: yes (2026-11-20)

### 6. Automated checks and the review before the play test

- Changes: none, unless a finding needs a fix.
- Check: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` passes; one
  Sonnet review agent checks the diff and every manual check (`docs/mega-refactor/CLAUDE.md`); the play scripts in
  `temp/play/`; the contract's manual checks filled in from the brief.
- Done: yes (2026-11-20)

## Round 8: block break and laser sound (2026-11-23)

Implements scope 12 of `SCOPE.md`. Steps R8.1 and R8.2 do not depend on each other and run first. Work files go in
`temp/078e-sounds/` and `temp/078e-pink-block/` of this worktree. No game launch. Gradle only with the owner's OK
while the play test of this worktree is open (memory rule).

### R8.1 Sounds

- Changes: `temp/078e-sounds/synth.py` (numpy, 44.1 kHz mono, NES 2A03 pulse and noise channel models) writes 3
  versions of `pink_block_explode` (0.4 to 0.8 s: a noise burst with a falling period, plus a low pulse thump) and 3
  versions of `laser_loop` (a pulse tone with a fast duty or pitch wobble), each as `.wav` and as `.ogg` through
  `ffmpeg -c:a libvorbis -q:a 5`. The owner listens and picks one of each.
- Check: the script prints for each file the length, the peak (at most -1 dBFS), and for each loop the jump between its
  last and first sample (no click: under 1% of full scale) with a length of a whole number of wave periods.
- Done: yes (2026-11-23)

### R8.2 PinkBlock and debris art

- Changes: one Claude Fable 5.2 art agent (brief in `temp/078e-pink-block/BRIEF.md`; it gets `docs/ways-of-working/art.md`
  and `assets.md`): a refined `PinkBlock` frame (16x16 art px, drawn 2x on a 32 px block, as now) and 6 debris
  frames (`PinkBlock/debris1` to `debris6`, 3x3 to 8x8 art px, drawn 2x by `FallingPieceDef.DEBRIS`, which does not
  turn), all from one sprite palette of at most 3 NES colors plus transparent. The main session puts the frames in
  `assets/sprites/frames/Platforms1/` and repacks `Platforms1` with TexturePacker.
- Check: `scan_nes_grid.py` and `scan_nes_palette.py` pass on the frames; `check_platforms.py` shows that no other
  region of `Platforms1` changed in pixels; the owner picks from a contact sheet at 8x.
- Done: yes (2026-11-23)

### R8.3 PinkBlock break

- Changes: `BreakableBlock` (`entities/blocks/BreakableBlock.kt`): for `PRECIOUS_TYPE`, the break spawns 2
  `Explosion`s (`ConstKeys.DAMAGER pairTo false`) at offsets of 0 or +-16 game px picked per axis with
  `services.random`, and throws 3 different keys of the 6 debris keys with `FallingPiece.throwDebris` (loaded with
  `FallingPiece.load(services, keys, TextureAsset.PLATFORMS_1)`). The other types keep `breakApart`.
  `explodeAndDie()` plays the sound once for the group: the recursion over the connected blocks goes through a private
  function that does not play a sound. `PINK_BLOCK_EXPLODE_SOUND` (the R8.1 pick) for `PRECIOUS_TYPE`,
  `THUMP_SOUND` for the others.
- Check: `core:smokeTest` (the debris regions load); the review before the play test.
- Done: yes (2026-11-23)

### R8.4 Laser loop

- Changes: `LASER_LOOP_SOUND` in `SoundAsset` (the R8.1 pick). A new `LaserBeamLoop` (`entities/hazards/`, an
  `Updatable` that holds the audio): `request()` marks this frame; `update(delta)` plays the sound with `loop = true`
  when a request came since the last update and it is not playing, and stops it when none came. `LaserBeamer.onSpawn`
  puts the one instance in `services.updatables` under its key; its update calls `request()` while `beaming` and
  `overlapsGameCamera()`, in place of the one-time `LASER_BEAM_SOUND`. A pause stops the beamer updates, so the loop
  stops; it starts again after the pause. `WilyFinalBoss` keeps `LASER_BEAM_SOUND`.
- Check: `core:test`; a unit test of `LaserBeamLoop` with a mocked `MegaAudioManager`: a request plays once, a second
  frame with a request does not play again, a frame with no request stops.
- Done: yes (2026-11-23)

### R8.5 Credits, checks and handover

- Changes: `README.md` credit lines for the new block art and the sounds (Claude Fable 5.2 for the art; the model
  that wrote the synth script for the sounds); contract checks 2, 3 and 7 updated; play scripts rewritten.
- Check: `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule` (with the owner's OK), one
  Sonnet review agent on the round-8 diff and the manual checks.
- Done: yes (2026-11-23)
