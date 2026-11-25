# Scope: mega-refactor-078h-moon-man-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief, not this
file. If the scope changes later, update this file and the brief, and ask the owner again.

- Status: draft
- Approved: no (the owner directed the first art pass on 2026-11-25; this scope is written from that request and the
  result, and is not yet approved as a brief)

## Goal

Fix three problems that the owner named in Moon Man's look: the rock textures repeat too much, there is too much
blue, and the background is too simple (no moons, no comets). New art in the NES style of `docs/ways-of-working/art.md`,
with no change to the block geometry or the gameplay.

## Item

Item `078` (Stage redesigns, epic), the Moon Man section, part 3: "Art make-over: the tileset and a layered space
background". Owner's text, 2026-11-25 (verbatim): "There are some issues with his tileset/background: overly
repetitive rock textures, too much blue, an overly simplistic background lacking moons, comets, etc. The background
can be built in multiple layers for different effects."

## Current state

- Live map: `MoonMan_16x14_v5.tmx` (`LevelDefinition.kt`, `MOON_MAN`). One tileset: `assets/tiled_maps/tsx/MoonManTiles.tsx`,
  image `assets/tiled_maps/png/MoonManTiles.png` (256x256, 32 px tiles, 8 columns; 64 tiles, though the `.tsx` says
  `tilecount="196"`).
- Most rock is painted with 4 tiles (ids 34, 35, 42, 43) in a 2x2 repeat, so large walls show a 64 px rhythm.
- One background object, `Space`, in the `backgrounds` layer (`BackgroundLayerBuilder.kt:38`, `Space.kt`). `Moon.kt`
  looks dead: the `Backgrounds6.txt` atlas has no `Moon` region and no map names it.
- First pass result (this branch, uncommitted at the time of writing): rock tiles 25-28, 33-36, 41-44, 49-52 are new
  pixels that join in any order; metal, tech, ladder and hazard tiles keep their shapes, snapped to the 2 px grid and
  recolored to NES colors; 5 background layers exist as PNGs with proposed parallax values. Details: `NOTES.md`.

## In scope

- The tileset image `MoonManTiles.png` (same size, same tile ids and roles).
- A map pass on `MoonMan_16x14_v5.tmx` that scatters the interchangeable rock tiles (no change to block objects).
- The 5 background layers: pack the PNGs, add one `Background` subclass for each new layer, register them in
  `BackgroundLayerBuilder`, and add the objects to the `backgrounds` layer of the map. `Space` gets the new far star
  field and its proposed parallax and animation values.
- README `CREDITS` lines (see `NOTES.md`).

## Out of scope

- Gravity puzzles and `Starkner` sections (other parts of the `078` Moon Man section).
- Any change to block geometry, enemies, rooms, camera sections or collision.
- New entities or decor (a flying comet sprite, animated tiles). Backlog candidates go in `TODO.md`.
- Other levels' tilesets (`MoonManTiles.tsx` is used by Moon Man only; confirm in the scan before the map pass).

## Affected areas and risks

- `assets/tiled_maps/png/MoonManTiles.png`, `MoonMan_16x14_v5.tmx`, `assets/sprites/frames/Backgrounds6/` (or a new
  atlas) and its packed atlas, `BackgroundLayerBuilder.kt`, `drawables/backgrounds/`, `README.md`.
- Risk: a tile whose shape changed meets its neighbors badly. The first pass kept every tile slot and checked 9 real
  screens, but not every room.
- Risk: the near ridge layer (`parallaxY = 0`) sits at the bottom of tall shafts (`room3`, `room12`).
- Risk: a layer drawn as a large texture costs fill rate on weak hardware (`O5`). Layers are 256x224 to 512x224 art px.
- The NES palette rule is checked per texture; where two parallax layers overlap, one screen area can hold colors of
  two palettes.

## Overlap with other branches

- Moon Man gravity-puzzle and `Starkner` work in `078` would edit the same TMX. No such branch is active now.
- `078f` (active, Timber Woman art) and any branch that edits `BackgroundLayerBuilder.kt` or `Background*.kt`
  (`046`, `167`, `177`): check for conflicts before the wiring step.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O4` (variety) | helps | New look for one stage. |
| `O5-R1` | no effect | The new `Background` subclasses must allocate nothing per frame; `167` tracks the base class. |
| `O5-R2` | waiting | Measure the frame cost of the 5 layers with `--diagnostics` on a low-performance run before and after the wiring. |
| `O6-R1` | helps | CI must pass. |
| `O6-R2` | helps | The art checks (`check.py`) and the manual playthrough below are the named checks. |
| `O9-R1` | no effect | No input change. |

## Open questions

1. ~~Who made the original `MoonManTiles` tileset?~~ Answered by the owner, 2026-11-25: SamuelX4. The original
   background is by Capcom; no Capcom credit is needed, because the new background is original.
2. Fill the 2 px transparent bottom rows of tiles 10, 11 and 12 (`MoonManTiles_gaps_filled_ALT.png`)? Agent and
   Claude recommend yes.
3. Scatter the rock variants in the map? Recommended: yes.
4. Keep the near ridge layer? Recommended: try it in play, drop it if it reads wrong.

## Steps

1. Owner answers the open questions; choose the tileset variant (gaps filled or not).
2. Commit `MoonManTiles.png`; scan which maps use `MoonManTiles.tsx`.
3. Map pass: scatter the rock tiles in `MoonMan_16x14_v5.tmx` with a script in `temp/` (output only is committed).
4. Pack the background layers; add the `Background` subclasses and register them; add the map objects.
5. Run `check.py` again on the committed art; run the automated checks below; measure with `--diagnostics`.
6. Owner play test of the whole stage; one fresh art agent per round of art feedback (`art.md`).
7. README credits, branch docs and process log; merge.

# Contract

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. Do
the checks before the branch merges, and write the result in the branch process log.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `temp/moon-man-redraw/check.py` passes on the committed tileset and layers (2 px grid, NES palette, 3 colors +
  backdrop per area, at most 4 palettes per screen).
- [ ] The tileset keeps its size and transparency mask (11 half-cut blocks of tile 55 excepted).
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

| Level | What to check |
|-------|---------------|
| `MOON_MAN` | Play the stage start to end: every platform edge, ladder, wall and ceiling joins without a seam; the player stands on the top art row of each block; ladders and the lattice columns line up; the 5 background layers scroll at different speeds, stars twinkle, nothing flickers or shows a seam on a loop. |
| `MOON_MAN` | `room3` and `room12` (tall shafts): the ridge layer does not read wrong. |
| `MOON_MAN` | Frame rate on `--performance low` is as before. |

## Behaviors to check

- Block geometry, enemies, rooms, camera sections and the boss fights are the same as on `master`.
- Pause, death and respawn in the stage: the backgrounds reset the same way as before.

## Intended changes

- The look of the Moon Man stage only: tiles, palette and background layers.
