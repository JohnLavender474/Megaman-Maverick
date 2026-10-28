# 067: PPM 16 with 1x pixel art

- Status: planned
- Type: refactor
- Depends on: none
- Related: `028`, `033`, `040`, `046`, `054`
- Branches: none yet

## Goal

`ConstVals.PPM` changes from 32 to 16, and all pixel art is scaled by 0.5 (from 2x art to 1x art) with no loss. The game
must look and play the same.

## Proposed scope

Not approved. Each branch `SCOPE.md` decides its final scope. Research: `NOTES.md` (2026-10-28). The sprite art is
already 1x in the files (94 % of 2,227 live atlas regions are native 1x, drawn at 2 world units per texel); only the
tilesets are 2x (32x32 tiles; 4,348 of 4,638 used tiles halve without loss, 290 do not, 278 of them in
`RumblecadeFactory`); 119 atlas regions are true 2x and halve without loss, 20 more are 2x at an odd offset or size.

Phase 1 (option C, owner decision 2026-10-28): `PPM = 16` with the map scaled at load time. No art changes.

1. `067a`: scale the map at load time (objects and the tile renderer by `PPM / tilewidth`); fix the quadtree ppm.
2. `067b`: fix the uses of `PPM` that are not lengths (friction rates, `RotatingLine` angular speeds, sine frequencies).
3. `067c`: fix pixel numbers and integer math (fonts, `splitIntoCells`, `MockingByte`, epsilons); decide the
   `--pixelPerfect` policy.
4. `067d`: a trace test at `PPM` 32 and 16 (fixed timestep, scripted input; ties to `006`, `010`, `055`).
5. `067e`: change `PPM` to 16; the owner's play test.

Phase 2 (owner decision 2026-10-28): the source art matches the world scale, in Tiled and in the game assets.

6. Halve the 2x tilesets to 1x (16x16 tiles) where lossless; decide the 290 tiles that are not lossless.
7. Convert the live TMX maps to 16-pixel tiles (halve coordinates, sizes and pixel-valued properties; 20 objects have
   odd or decimal values) and remove the load-time map scale from `067a`.
8. Halve the true 2x atlas regions (119 lossless; decide the 20 at an odd offset or size), with their `.tps` sources.
9. Update the tools that assume 32-pixel tiles: `utils/level_builder` (`TILE = 32`), `utils/level_analyzer`, the
   Tiled extensions, and the level skills.

## Owner's input

2026-10-28 (verbatim): "Right now, PPM is set to 32px. All artwork is scaled 2x. I'd like to change this so that the PPM
is 16px. This will require scaling all pixel art by 0.5x. The scaling cannot be lossy."

2026-10-28 (verbatim, about option C): "I think option C makes sense as an intermediary, but ideally, the original art
used in Tiled and in the game's assets also match what is expected. Therefore, I think 067 should be expanded with more
branches that do this work after the branches you proposed."

2026-10-28 (verbatim): "I agree that we will need to be cautious about certain artwork and do things on a case by case
basis (or set by set where each set is of artwork that matches in specs). I know that in GIMP, there is a way to scale
images with \"none\" instead of \"linear\" or \"cubic\" which results in \"lossless\" scaled images. Worst case scenario,
art has to be scaled back down at runtime which isn't too bad."

2026-10-28 (verbatim): "I should also mention that certain tilesets actually haven't been scaled yet to match the
32px requirement. For example, you referenced the Rumblecade tileset; that tileset needs to be scaled 2x, but I haven't
done that yet. Ideally, we should focus only on art that is used in the game already. The Rumblecade level isn't used in
the game (its extra-level entry should be commented out), so it doesn't count"

## Decisions

- 2026-10-28 (owner): the item covers only art that the game uses. `RUMBLECADE_FACTORY` is commented out on `master`
  (direct change), so `RumblecadeFactory` and its tileset are out. Without it, 12 used tiles do not halve without loss
  (`MoonManTiles` 5, `Gravity Man Stage` 5 (only `Test1`), `mm9jewel` 1, `UnderwaterFanPillar_Gray` 1), plus the 20
  atlas regions at an odd offset or size.
- 2026-10-28 (owner): phase 1 is option C (`067a` to `067e`). Phase 2 then makes the source art and the maps 1x, after
  phase 1. Not chosen: option A (halve all art at once; the 1x art cannot halve) as the first step.

## Open questions

1. The 12 tiles and the 20 atlas regions that do not halve without loss: decided case by
   case, or set by set (art with the same specs). Fallback (owner): keep the 2x art and scale it down at runtime. Note:
   a 0.5x scale with no interpolation (GIMP "None", nearest neighbor) is lossless only where every aligned 2x2 block
   has one color; in a mixed block it keeps one of the four pixels and drops the others. A phase 2 branch checks each
   image with the 2x2 test from the research scripts before and after.
2. Phase 2 branch letters and order (tilesets and maps together, or tilesets first).

## Sources

- `NOTES.md` in this folder.
