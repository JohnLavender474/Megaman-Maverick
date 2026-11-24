# Notes: mega-refactor-078e-precious-woman-art

## Decisions

- 2026-11-20 (owner): background concept "crystal abyss": a deep chasm with crystal-lined cliffs in 2 to 3 parallax
  layers that fall away into dark, glowing veins, and a far lake that mirrors the gems.
- 2026-11-20 (owner accepted the recommendation): the stage reads as a jewel mine dug into the abyss. The girder
  platforms stay, redrawn as old mine scaffolds (lanterns, cart rails). The `tiles2` back wall goes; its steel
  pillars become a few scaffold towers in a decor layer, in front of the background.
- 2026-11-20 (owner): Claude Fable 5.2 draws the art (the confirmation of `docs/ways-of-working/art.md`).
- 2026-11-20 (owner approved the short design): the `078a`/`078b` method. One new original tileset built by a script
  in `temp/078e-precious-art/`; the map repainted by a gid mapping plus position rules; a new `decor` tile layer;
  Tiled tile animations (crystal glints, lamp flicker); new parallax background objects in place of `CrystalBKG`.
  Crystal decor as tiles, not entities. No Mega Man 5 shapes in the new crystals. Credits: the update by Claude Fable 5.2
  (corrected 2026-11-20: mrgranade made the boss sprite, not the stage tiles; see brief D1). Merge target: `master`.

- 2026-11-20 (owner accepted the recommendations of the round-1 review,
  https://claude.ai/artifact/7uufYEZeBb658d2gSffu5j):
  - Q0 B: round 2 adds more light to the background: brighter glowing veins, large distant crystal spires and light
    from the lake, still under the play tiles in contrast.
  - Q1 A: thick-wall rock keeps one brightness. Q2 A: 3 background layers in the play rooms. Q3 A: 2 layers in the
    boss room. Q4 A: the lake is fixed to the map (`room1` and the bottom of `room2`). Q5 A: scaffold towers and
    lanterns stay. Q6 A: cart rails on flat floors of 6 or more cells. Q8 B: no air crystals on floors (ceilings
    and walls only).
  - Q7 B: the `PreciousBlock` frames (`assets/sprites/frames/Platforms1/PreciousBlock.png`, 3 frames, atlas
    `PLATFORMS_1`) are redrawn to match the new crystal columns. This widens the scope (entity art); `PreciousSpike`
    stays.

- 2026-11-20 (owner accepted the recommendations of review page version 2): R1 B, a far-layer copy 1 step darker for
  the boss room; R2 A, the `PreciousBlock` keeps the color cycle; R3 A, the mist bands at the spire feet become a
  soft dithered fade.
- 2026-11-20 (owner): the background haze comes from surface cracks: thin shafts of daylight from high above, the
  haze brightest under them. `room1` keeps the lake light.

- 2026-11-20 (owner accepted the recommendations of review page version 3): S1 A, the far layer is 7 textures by map
  height so the light grows with the climb; S2 A, faint shafts behind the boss; S3 A, the lake keeps its level shore
  line; S4 A, the shaft strength stays as drawn (the contrast limit stays). The round-3 art is final for the brief.

- 2026-11-20 (owner, brief D1): "The tileset is a mix of mrgranade and Capcom. Keep them credited along with Fable
  5.2." The README gets a Made-by line for Fable and Inspired-by lines for mrgranade and Capcom.
- 2026-11-20 (owner accepted the recommendation, brief D2): keep the 3 unused `precious_woman_*` tileset files.
- 2026-11-20 (owner accepted the recommendation, step 3): `boss_room` and `room7` share the same map rows, and the far
  layer moves at 0.1 in x, so a far copy cannot be kept to the boss room by its place. The boss room gets one fixed
  picture: the round-3 boss view (dim far + mid) as one opaque region, fixed to the room rectangle above the other
  layers. No show or hide triggers. Accepted effect: in the pan from `room7`, the picture slides in with the room edge,
  so a hard seam shows at that edge for the length of the pan. Item `177` (backgrounds drawn per room) can replace it.
- 2026-11-20 (owner, play-test round 1): "Make the jeweled platforms more refined and detailed like in the original
  spritesheet. Let's make the hazy background animated. Update the sparkling sprites to be new." Answers: the
  platforms are the `PreciousBlock` frames, the pink slab tiles, the blue gem block tile and the crystal column tiles;
  the haze animates as one clear overlay layer of drifting motes and shaft shimmer in front of the far bands
  (recommended; the far bands stay still); all glints (gems and air crystals) become a 4-point star that grows and
  fades (recommended). Art round 4.
- 2026-11-20 (owner, play-test round 2): "Now redrill the following two enemies to be more refined and to match the
  stage's aesthetic more: DrillHead, PreciousTron, LaserBeamer, Laser. The drill head should be more appropriate for
  diamond mining. Precious Tron is overly simplistic. Laser and laser beamer need redo and refinement. The laser needs
  to be more than just pixel squares: pulsing, throbbing, glowing would be better." The list has 4 entries; all 4 are
  in scope. Answers (owner accepted the recommendations): the new beam applies to every `Laser`, also the 6
  `LaserBeamer`s of Wily Stage 1 (their `wily` beamer skin stays); the beam stays red: a near-white core, a red body
  and a red-orange glow that pulses. Art round 5; the beam needs a change in the `Laser` draw code.
- 2026-11-22 (owner): "Does the new artwork conform the NES graphics? It looks like the new artwork might have pixels
  are the too small. Scan through the artwork and fix it. Also, let's make sure the "art ways of working" doc(s)
  instruct to adhere to NES pixel sizes and color pallettes." Scan: the round-4 jewel tiles (34), the star glints (35
  tiles) and the `PreciousBlock` frames have detail finer than 1 NES pixel (2x2 game px); no 078e color is an exact
  NES (2C02) palette color, and up to 8 colors share one 8x8 area. Answers (owner accepted the recommendations): the
  rule is the NES palette with NES color counts, and all 078e art follows it (art round 6). The rule is in
  `docs/ways-of-working/art.md` on `master` (commit "docs: ways of working: art follows NES pixel size ...").
- 2026-11-22 (owner, round 6 V0): the round-6 agent reported that its session ran as Claude Opus 5.5 for part of the
  round, and Opus made most of the artistic choices. Owner: keep round 6 and credit both models. V1 to V5 of
  `review6.md`: integrated with the recommendations (A); the owner decides at the play test.
- 2026-11-22 (owner): bring back `DrillHead` and `PreciousTron` as they were before. Their frame strips and
  `Enemies1.png` are restored from `master` (`Enemies1.txt` did not change). The round-5 and round-6 drafts stay in
  `temp/078e-precious-art/entities/` of the main checkout.
- 2026-11-22 (owner): round 6 is directionally correct but not refined enough; a Claude Fable 5.2 agent (on usage
  credits) refines the tileset (art round 7, `review7.md`). Fable made all artistic choices. A lit stroke in
  `B8B8F8` on the rock needs P0, so fill cells under a pink gem or a vein use the new `rock_p_*` tiles, and 8 pink
  gems on a lit left rim become white gems. V1 to V5 of `review7.md` are integrated as drawn (A); the owner decides
  at the play test.
- 2026-11-22 (owner): the laser beams turn their pixels, which NES art never does; new rule "No rotated pixels" in
  `docs/ways-of-working/art.md` on `master`. Options (an Opus research agent; preview in
  `temp/078e-laser-research/` of the main checkout): A: 90-degree visual snap; B/C: 8 or 16 directions with a stepped sweep; D: stepped line of unrotated
  parts; E: procedural line; F: old squares. Owner accepted the recommendation: D, drawn by one `LaserBeamSprite`
  that writes its own quads (runs of 1-art-px columns of the beam frame, on the 2 px grid, in 90-degree steps of the
  frame), and one beam clock shared by all lasers. The damage line, the sweep and the reflections stay free-angle.
  Per-pixel quads were rejected: the same pixels at 9 times the quads.
- 2026-11-23 (owner accepted the recommendation): the `PreciousBlock` keeps its diagonal shimmer (each cell starts on
  frame `(row + col) % 3`, `PreciousBlock.kt`), so its 3 color pairs show at once: 3 sprite palettes for one entity.
  Accepted exception to the rule of at most 2 sprite palettes per entity in `docs/ways-of-working/art.md`.

## Sources of the art

- Current tilesets of the map: `precious_woman_foreground_tileset`, `precious_woman_background_tileset`,
  `precious_woman_platforms_tileset` (no credit line in `README.md`; its "Precious Woman" line credits mrgranade
  for the boss sprite), `CrystalManTileset`
  (Mega Man 5 rip), `mm9jewel` (Mega Man 9 rip), `Tileset1` (1 tile).
- Current background: `CrystalBKG` (`BACKGROUNDS_6`), Megaman Maker "Crystal Cave Background".

## Overlap with other branches

- `078d` (active, Reactor Man art): no shared files except a possible new `TextureAsset` entry and `README.md` credit
  lines (simple list conflicts).
