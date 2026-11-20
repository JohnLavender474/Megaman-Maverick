# Scope: mega-refactor-078e-precious-woman-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file. If the scope changes later, update this file and the brief, and ask the owner again.

- Status: draft (short design approved by the owner on 2026-11-20; the brief follows the round-1 art review)
- Approved: no

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
4. A new parallax background ("crystal abyss", 2 to 3 layers) behind every room, in place of `CrystalBKG` in this
   map. New background atlas regions and builder entries in `BackgroundLayerBuilder`.
5. Credits in `README.md` (`docs/ways-of-working/art.md`): the original by mrgranade and the update by Claude Fable
   5.2, directed by the developer; an "Inspired by" line if a background follows a source.

## Out of scope

- Gameplay: block geometry, spawns, rooms, camera, hazards.
- The art of the entities listed above (blocks, spikes, lasers, enemies, the boss).
- The shared tileset files and other maps. `CrystalBKG` stays in the atlas and the builder if another map uses it.

## Affected areas and risks

- `PreciousWoman_v2.tmx`, a new `.tsx` and `.png`, a background atlas, `TextureAsset.kt` (if a new atlas),
  `BackgroundLayerBuilder.kt`, `README.md`.
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
| `O1-R5` code only with a production caller | no effect | each new background entry is used by the map |
| `O2-R2` random calls through the engine API | no effect | no new random calls planned |
| `O2-R7` gameplay does not depend on rendering | no effect | art only |
| A branch keeps all behavior that its contract does not name | no effect | art only |

## Open questions

- The area split of the stage (if the areas differ in accent or depth of the abyss): round-1 art review.
- The number of parallax layers and their speeds: round-1 art review.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] Map script check: every object of every object layer other than `backgrounds` is the same as on `master`.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Filled in after the brief.

## Behaviors to check

- Blocks, spikes, lasers and enemies act as before.

## Intended changes

- Precious Woman stage art: tiles, tile animations, background. No gameplay change.
