# Scope: mega-refactor-078e-precious-woman-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file. If the scope changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-20 (brief https://claude.ai/artifact/TCpMwbgT1TvzAp6WMPbXT9)

## Goal

The Precious Woman stage (`PRECIOUS_WOMAN`, `assets/tiled_maps/tmx/PreciousWoman_v2.tmx`) gets a "deep cave of jewels"
look: a jewel mine dug into a crystal abyss. One new original tileset in place of the 5 current ones (with no
Mega Man 5 crystal rips), varied rock, no back-wall tiles, and a new parallax background that shows where the back
wall was.

## Item

`078` (stage redesigns), sections "All stages: tileset art and backgrounds" and "Precious Woman". The owner decisions
of this branch are in `docs/NOTES.md` ("Decisions").

## Current state

Scan of `master` `75e937946`, 2026-11-20.

- Map: 225 x 100 tiles, 8 rooms (`room1` to `room7`, `boss_room`). Tile layers `tiles2` (under, 6060 cells) and
  `tiles1` (2665 cells).
- `tiles2`: 8 distinct tiles of `precious_woman_background_tileset`: the dark purple diamond-lattice back wall and
  dark steel pillars. It fills every room behind the play space.
- `tiles1`: 49 distinct tiles: `precious_woman_foreground_tileset` (16, the periwinkle rock),
  `CrystalManTileset` (19, Mega Man 5 rip: the dark crystal clusters and the pink and white crystal pillars),
  `precious_woman_platforms_tileset` (9, steel girders and thin ledges), `mm9jewel` (4, Mega Man 9 rip),
  `Tileset1` (1).
- Shared tileset files: `CrystalManTileset` is also linked by the old maps `PreciousMan.tmx` and `PreciousWoman.tmx`,
  `mm9jewel` by `WilyStage2.tmx`, `Test3.tmx`, `Test7.tmx` and the old `PreciousWoman.tmx`, `Tileset1` by 11 live
  levels. This branch does not edit them. The 3 `precious_woman_*` tilesets are used only by this map.
- Background: one `CrystalBKG` object (512 x 448 at `0,2752`, `columns=50`, `rows=1`), only behind `room1`'s row of
  the map, drawn by `Background` (`BackgroundLayerBuilder.kt:61`, `BACKGROUNDS_6`). Megaman Maker art.
- Entities that draw their own art in the stage: `PreciousBlock` (45), `PinkBlock` (14), `PreciousSpike` (16),
  `LaserBeamer` (8), `PreciousGemCanon` (8), `PreciousTron` (4), `DrillHead` (8), `PreciousWoman`.
- Before-renders: `temp/078e-precious-art/_before/` (main checkout).

## In scope

1. One new original tileset for the map (`precious_woman` rock, crystals, mine scaffolds, decor), built by a script in
   `temp/078e-precious-art/`. Several rock fill variants placed by position rules, so the rock does not repeat.
2. The map repainted by a gid mapping plus position rules. `tiles2` is emptied; a new `decor` tile layer holds the
   crystal clusters, lanterns, rails and a few scaffold towers. The map links only the new tileset afterwards.
3. Tiled tile animations: crystal glints, lantern flicker.
4. A new parallax background ("crystal abyss") behind every room, in place of `CrystalBKG` in this map: far layer as
   7 textures by map height (lake, 5 bands with daylight shafts, a darker boss band), mid and near cliff layers (round
   3 of the art). The boss room has one fixed picture (step-3 decision, `docs/NOTES.md`). A new atlas `Backgrounds12` with one `TextureAsset` entry. The backgrounds are custom map objects
   (`atlas`, `region`, `rows`, `columns`, `offset_x`/`offset_y`, `parallax_x`/`parallax_y`), which
   `BackgroundLayerBuilder` already reads: no builder change.
5. The `PreciousBlock` frames redrawn to match the new crystal columns (owner, Q7 of the round-1 review), and
   `PLATFORMS_1` repacked.
6. Credits in `README.md` (`docs/ways-of-working/art.md`; owner D1, 2026-11-20: the old stage tileset is a mix of
   mrgranade and Capcom art, and both stay credited next to Fable), under "Tilesets":

   ```markdown
   - Precious Woman (tileset, backgrounds and the moving `PreciousBlock`)
     - Made by Claude Fable 5.2, an AI model by [Anthropic](https://www.anthropic.com/claude), directed by the developer
     - Inspired by [mrgranade](https://www.fiverr.com/mrgranade): the old stage tiles
     - Inspired by Capcom: the old stage tiles
   ```

7. The 3 old `precious_woman_*` tileset files stay in the repo, unlinked (owner D2, 2026-11-20).
8. Art round 4 (owner, play-test round 1, 2026-11-20; `docs/NOTES.md`): the `PreciousBlock` frames, the pink slab
   tiles, the blue gem block tile and the crystal column tiles redrawn at 32 px per cell with more facets; one
   animated haze overlay (drifting motes, shaft shimmer; 6 frames of 512 x 448, 0.3 s each, parallax 0.15 / 1) in
   front of the far bands, in its own atlas `Backgrounds13` (it does not fit in `Backgrounds12` with `Basic`), with a
   `BACKGROUNDS_13` entry; every glint redrawn as a 4-point star.
9. Art round 5 (owner, play-test round 2, 2026-11-20; `docs/NOTES.md`): new art for `DrillHead` (a diamond-mining
   drill), `PreciousTron` (more detail), the default `LaserBeamer` skin, and the `Laser` beam: a pulsing, glowing red
   beam in place of the row of 4 px squares, for every `Laser` (also Wily Stage 1). The beam needs a change in the
   `Laser` draw code (no change to its body, damage, reflection or timing). Owner, 2026-11-22: `DrillHead` and
   `PreciousTron` go back to their `master` art.
10. Art round 6 (owner, 2026-11-22; `docs/NOTES.md`): all 078e art follows the NES rule of
    `docs/ways-of-working/art.md` (2x2 game px per NES pixel, only NES palette colors, NES color counts). The credit
    names Claude Fable 5.2 and Claude Opus 5.5 for this round.
11. Art round 7 (owner, 2026-11-22; `docs/NOTES.md`): Claude Fable 5.2 refines every tile of the mine tileset in the
    round-6 direction and palettes. 16 new fill tiles (`rock_p_*`, ids 560 to 575); 156 `tiles1` cells and 8 decor
    gems change. Backgrounds, haze, `PreciousBlock` and entities do not change.
12. Round 8, block break and laser sound (owner, 2026-11-22; approved 2026-11-23):
    - Why: the `PinkBlock` break (4 same brick pieces, one `THUMP_SOUND`) and the one-time laser sound feel weak.
      Model: the X6 junk block (`mmx-demake-tools/docs/enemies/junk-block.md`, "Main" step 3) and `Junkroid`.
    - Approaches: A, the X6 junk-block recipe (chosen); B, A with a 0.05 s chain ripple and a camera shake (path opens
      later); C, pieces cut from the block art (repeats). Sounds: synthesized (chosen) over reuse or owner files.
    - Break: each `PinkBlock` spawns 2 `Explosion`s at random offsets of 0 or +-16 game px in x and y, and 3
      `FallingPiece`s, each a random region of a pool of about 6 new debris sprites, with the `throwDebris` random
      speeds. The debris turns in 90-degree steps or not at all (a new `FallingPieceDef`). Connected blocks break in
      the same frame, as now. One explosion sound per break group (one call of `explodeAndDie` from outside),
      not one per block. Damage rules and the heart-tank block stay. Other `BreakableBlock` types keep their break.
    - Art (Claude Fable 5.2, one art agent; Fable is already confirmed for `078e`): a refined `PinkBlock` sprite
      and about 6 debris sprites, in the NES rules of `docs/ways-of-working/art.md` (2 px grid, NES palette, 3
      colors plus transparent per sprite palette). Credit line in `README.md`.
    - Sounds: a script in `temp/078e-sounds/` synthesizes NES-style sounds (2A03 pulse and noise channels) and
      writes `.ogg` with `ffmpeg`: 2 or 3 versions of a block explosion and of a seamless laser loop, for the owner to
      pick. New `SoundAsset` entries; `laser_beam.ogg` stays for `WilyFinalBoss`.
    - Laser loop: all `LaserBeamer`s (both skins). One shared loop plays while at least one beamer on camera has its
      beam on, and stops when none has (also on a level end, a death and a pause). Not per beamer, so the sound does
      not stack.
    - Left to the builder: the explosion volume, the debris sizes, the loop length, and where the shared loop
      lives (for example a small session-wide owner that the beamers report to each frame).

13. V-point ceilings (owner, 2026-11-23, after item `182` branch `182a` merged): each of the 42 V points of the
    crystal columns (tile pairs `392/393`, `402/403`, `412/413` in `tiles1`) becomes a `CeilingPolygonBlock`
    (`0,0 64,0 32,32`), with the `cull_*` and `spawn_*` properties of the block it replaces. The 26 rectangle blocks
    over the V rows end 1 row higher. The wall `ShieldBlock` 546 in `room3` stays (it covers half of a V). The 15
    `HoverSpot`s of the 5 `DrillHead`s under V points (`room1`: 47, 53, 82, 101; `room3`: 167) move in x so that
    each centre is under a V tip, as in `REFACTOR_TEST_182` room `drill_head`.

## Out of scope

- Gameplay, except in scope 13: block geometry, spawns, rooms, camera, hazards.
- The art of the entities listed above (blocks, spikes, enemies, the boss), except `PreciousBlock`, `LaserBeamer`
  and `Laser` (in scope 9). `DrillHead` and `PreciousTron` keep their `master` art.
- The shared tileset files and other maps. `CrystalBKG` stays in the atlas and the builder if another map uses it.

## Affected areas and risks

- `PreciousWoman_v2.tmx`, a new `.tsx` and `.png`, the `Backgrounds12` atlas, `TextureAsset.kt` (2 lines), the `Backgrounds13` atlas, the
  `PreciousBlock` frame strip and the `Platforms1` atlas, `README.md`.
- With no back wall, open air and the background must not read as solid, and rock must not read as air: the
  background stays darker and lower in contrast than the play-space tiles.
- The entity art (pink and purple gem blocks, spikes) must still read in front of the new background.
- A large parallax background over a 225 x 100 map: memory and draw cost on weak hardware (`O5`); prefer tiled
  regions over one large texture.

## Overlap with other branches

- `078d` (active, Reactor Man art): no shared files except a possible `TextureAsset` entry, `BackgroundLayerBuilder`
  entries and `README.md` credit lines (simple list conflicts).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O4` deeper levels | helps | the stage gets its own identity |
| `O9` player experience | helps | original art, a stronger mood |
| `O1-R5` code only with a production caller | no effect | the only code lines are the `BACKGROUNDS_12` and `BACKGROUNDS_13` entries, used by the map |
| `O2-R2` random calls through the engine API | no effect | no new random calls planned |
| `O2-R7` gameplay does not depend on rendering | no effect | art only |
| A branch keeps all behavior that its contract does not name | no effect | art only |

## Open questions

- None.

# Contract

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes.
- [x] `./gradlew core:smokeTest` passes.
- [x] `./gradlew detektMain core:verifyRandomRule` passes.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] Map script check: every object of every object layer other than `backgrounds` is the same as on `master`.
- [x] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

From `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-078e-precious-woman-art`:

1. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room1`: the lake and its light, 3 parallax layers when Megaman
   moves, star glints and lanterns, the drifting haze motes, no floor crystals; `DrillHead` hovers, flies and drills
   with its old art.
2. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room2`: climb the scaffold shaft; the far layer has no seam
   between bands; the new `LaserBeamer`s turn, and their beams pulse and glow, start at the ruby, end in a glow at
   the hit point, and still hurt and stop at blocks. Round 8: at every angle the beam steps like NES art (no turned
   pixel); one laser loop sound plays while a beamer on screen beams, with no stacking for 2 beamers, and stops when
   none is on screen, in a pause and at a death. Stand on a beamer's block: the feet rest on the top outline of the
   beamer art. A laser that hits a `PinkBlock` makes it burst: the new gem block art, 2 explosions, 3 different
   debris pieces each time, and the new explosion sound once for a group of connected blocks.
3. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room3`: the dying shaft, the dithered spire feet, the new
   faceted `PreciousBlock` color cycle and crystal columns; the `LaserBeamer` beam reflects off a `PreciousBlock` with
   the new look.
4. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room4`: climb on into `room5` (it has no player spawn); gem
   blocks, spikes and `PreciousTron` (old art) read in front of the art; it appears from a crystal cluster, winds up
   and throws.
5. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room6`: 4 daylight shafts; they do not look like a laser or a
   platform.
6. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7 --heartTanks all --healthTanks all`: go on to the fight;
   the darker background keeps the boss in front.
7. `./run-game-alpha.sh --level WILY_STAGE_1 --room room3`: the 2 `LaserBeamer`s keep their Wily skin; their beams
   have the new pulsing look and act as before (damage, blocks). Round 8: the laser loop sound; stand on each
   beamer's block: the feet rest on the top outline of the Wily beamer art.

8. `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room1 --debugShapes`: each `DrillHead` flies up into a V
   tip and drills with its drill in the crystal point; jump into the V points: Megaman stops on the faces, no wall
   slide on a V face. Repeat in `room3` (`--room room3`, the row of 6 V points).

## Behaviors to check

- Blocks, spikes, lasers and enemies act as before. `Laser` changes only its draw code: its body, damage, reflection
  and timing stay. `WhiteBurst` at a laser end keeps its `EXPLOSION` fixture; only its sprite is hidden there.

## Intended changes

- V-point ceilings, 2026-11-23: bodies stop at the V faces of the crystal columns, not at the tip line. Side
  effects: a laser that reaches a V row stops at the face (the V is a plain block, not a `ShieldBlock`, so it does
  not reflect), and a shot that hits a V face is blocked, not deflected. `DrillHead` hover spots sit under V tips.

- Precious Woman stage art: tiles, tile animations, background. No gameplay change.
- Round 4: detailed jewel pieces, an animated haze overlay, star glints.
- Round 6: all 078e art in NES pixel size, NES palette and NES color counts.
- Round 7: a refined tileset (cobbled rock, lit rims, clean columns, gems and mine pieces).
- Round 5: new default `LaserBeamer` art; a pulsing, glowing `Laser` beam in every
  level (Wily Stage 1 too), with an origin and an end glow in place of the `WhiteBurst` disc.
- Laser draw, 2026-11-22: the beam is a stepped pixel line of unrotated beam slices on the 2 px grid (no turned
  pixel at any angle); the glows snap to the grid; all lasers pulse on one shared clock.
- `LaserBeamer` on the pixel grid, 2026-11-22: an up-facing beamer's sprite is 10 game px below its center (was
  3.2), so the art top meets a block top 1.25 tiles above the center; the Wily frame moves up 1 row. No body change.
