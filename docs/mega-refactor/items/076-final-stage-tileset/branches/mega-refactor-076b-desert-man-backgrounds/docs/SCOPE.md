# Scope: mega-refactor-076b-desert-man-backgrounds

- Status: approved
- Approved: 2026-11-17

## Goal

Desert Man gets the new animated backgrounds that the owner accepted on 2026-11-17: `SunriseHills` (the dusk sky at
the start, 8 frames) from `temp/076-desert-bg/sunrise_hills_v3/`, and the sunset after the cave, `DesertSunSky`
(8 frames, sun with heat haze) and `SunsetHills` (a static silhouette) from `temp/076-desert-bg/sunset_v1/` (main
checkout).

## Item

`076`: Better tileset art for every level. Decision of 2026-11-17: the item also covers the level backgrounds where
they do not match the new tiles; Desert Man first: a simpler, Mega Man-like style, hills not buildings, the sunset
with heat haze only after the cave section.

## Current state

- `TextureAsset.BACKGROUNDS_6` (`Backgrounds6.txt`, one 1770x1412 page) holds 15 regions, packed by TexturePacker from
  `assets/sprites/frames/Backgrounds6/` (`assets/sprites/texture_packer/Backgrounds6.tps`, algorithm `Basic`, maximum
  2048x2048, no rotation, no trim). `queueAssets()` loads every `TextureAsset` entry.
- `DesertSunSky.kt`: `AnimatedBackground` on region `Desert/SunSky`, `animRows = 3`, `animColumns = 1`,
  `duration = 0.1f`, parallax 0.
- `BackgroundLayerBuilder.kt`: `SunriseHills` (priority 2) and `SunsetHills` (priority 1) are static `Background`s,
  16x32 tiles, 50 columns, parallax x 0.1.
- `AnimatedBackground` gives each copy one `Animation`; `Animation` cuts the region row by row
  (`splitAndFlatten`). The 4x2 sheets of the drafts are in this order (checked).
- Only Desert Man maps use these 3 regions. Other live levels use other `Backgrounds6` regions: `INTRO_STAGE`
  (`PunkCity`), `MOON_MAN` (`Space`), `INFERNO_MAN` (`InfernoBKG`), `PRECIOUS_WOMAN` (`CrystalBKG`).
- Dry run (Basic only): the 3 new sheets do not fit `Backgrounds6` at 2048x2048. With the 3 regions in a new atlas,
  `Backgrounds6` packs to 996x1880 and `Backgrounds7` to 1024x1988; no region is rotated; the 12 regions that stay in
  `Backgrounds6` are pixel-identical to today.

## In scope

1. A new atlas `Backgrounds7`: the frames folder `assets/sprites/frames/Backgrounds7/` (`SunriseHills.png` 1024x1024,
   `SunsetHills.png` 256x512, `Desert/SunSky.png` 1024x448), the project `assets/sprites/texture_packer/Backgrounds7.tps`
   (a copy of `Backgrounds6.tps` with its folder and output), and `assets/sprites/sprite_sheets/Backgrounds7.txt`
   and `.png`. A new entry `TextureAsset.BACKGROUNDS_7`.
2. The 3 old frames leave `assets/sprites/frames/Backgrounds6/`; `Backgrounds6` is packed again with its own project.
3. `DesertSunSky.kt`: `BACKGROUNDS_7`, `animRows = 2`, `animColumns = 4`, `duration = 0.15f`.
4. `BackgroundLayerBuilder.kt`: `SunriseHills` becomes an `AnimatedBackground` on `BACKGROUNDS_7` (`animRows = 2`,
   `animColumns = 4`, `duration = 0.2f`), the other values as today; `SunsetHills` reads `BACKGROUNDS_7`.

## Out of scope

- The map (no change to the triggers or background objects), the tiles, `Desert/Canyon` and `Desert/NoSunSky`
  (unchanged, they stay in `Backgrounds6`), other levels, the `Background` and `AnimatedBackground` classes.

## Affected areas and risks

- Files: 2 Kotlin files plus `TextureAsset.kt`; the frames, projects and atlases of `Backgrounds6` and `Backgrounds7`.
- Risk: a region name changes or a kept region changes in the repack. Check: names and pixels compared by script.
- Risk: the sunset silhouette is 20 px higher than the dawn one, so a switch while the hills show makes it jump. The
  review found that the swap happens while `room14` (a dark room) comes into the camera, so no jump can show.
- Risk: memory: one more 1024x1988 texture is loaded at start (all atlases load at start).

## Overlap with other branches

Checked 2026-11-17: no active branch changes these files or atlases.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` Code only with a production caller | no effect | `BACKGROUNDS_7` has 2 callers in the branch. |
| `O2-R7` Gameplay does not depend on rendering | no effect | Draw only. |
| `O5-R1` No per-frame allocation | no effect | `Animation.update` and `setRegion` per copy, as `DesertSunSky` today; no new allocation. |
| `O6-R1` CI passes | no effect | Checked before the merge. |
| `O6-R2` Named checks | helps | Play checks for each background and control checks for the repacked atlas. |
| `O7-R3` No level-identity checks | no effect | No level check in code. |
| Other always-on rules | no effect | |

## Decisions

- D1 (owner, 2026-11-17): pack with `Basic` only; a new atlas when the frames do not fit (rule in the root `CLAUDE.md`).
- D2: move only the 3 changed regions to `Backgrounds7`; `Desert/Canyon` and `Desert/NoSunSky` stay (owner, 2026-11-17).
- D3: accept the 20 px height difference of the two silhouettes; check at the play test (owner, 2026-11-17).

## Open questions

None.
