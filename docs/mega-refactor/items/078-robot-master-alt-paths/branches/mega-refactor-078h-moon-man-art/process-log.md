# Process log: mega-refactor-078h-moon-man-art

## Status

- State: active (PR open, waiting for the owner's review and play test)
- Goal: Moon Man art make-over: a new lunar rock set in the existing tile slots and a layered parallax space background.
- Done: tileset, scattered rock in `MoonMan_16x14_v5.tmx`, 5 background layers packed and wired (`Space` replaced in `Backgrounds6`; `MoonNebula`, `MoonSky`, `MoonComets`, `MoonRidge` in the new atlas `Backgrounds14`), README credits.
- Next: owner review on the PR; play test of the stage; check the ridge layer in the tall shafts; repack `Backgrounds14` with TexturePacker on a laptop (optional).
- Blocked: Gradle checks could not run in the cloud session (see the entry of 2026-11-25); the CI of the PR is the automated check.

## Entries

## 2026-11-25

- Branch cut from `master` (`a5e61cd`) in `.worktrees/mega-refactor-078h-moon-man-art/`. It started as `art-moon-man-redraw` and was renamed to the `078h` convention the same day, once tied to item `078`.
- One Fable 5.2 art agent (owner asked for Fable by name) did the first pass. Result: `check.py` passes (2 px grid, NES palette, 3 colors + backdrop per area, at most 4 palettes on a screen); only `assets/tiled_maps/png/MoonManTiles.png` changed in git. Nothing wired in code yet.
- Docs: item `078` `ITEM.md` gained the Moon Man art entry; `SCOPE.md`, `NOTES.md` and `TODO.md` written for this branch. No `BRIEF.src.html` yet (the owner has not approved a scope).
- Owner, 2026-11-25: the original tileset is by SamuelX4; the original background is by Capcom, with no credit needed because the new background is original. `NOTES.md` and `SCOPE.md` updated.
- Owner (from the phone, Claude on the web): recommendations accepted. Tileset with the gap rows of tiles 10-12 filled; the rock tiles scattered in the map by `temp/moon-man-redraw/scatter_map.py` (seed 78, 3496 tiles, unflipped only; block objects unchanged); ridge layer kept for the play test.
- Packing: TexturePacker is not installed in the cloud session. `Space` is a same-size image, so it replaces the old region in place in `Backgrounds6.png` (no region moved; the 11 other regions are byte-equal) and in `frames/Backgrounds6/Space.png`. The 4 new layers are in a new atlas `Backgrounds14` (`frames/Backgrounds14/`, `texture_packer/Backgrounds14.tps`, `TextureAsset.BACKGROUNDS_14`); its page and `.txt` were made by a script (512x758, regions at 2 px spacing, no rotation), not by TexturePacker.
- Wiring: new `Background` subclasses `MoonNebula`, `MoonSky`, `MoonComets`, `MoonRidge` (rows and columns sized to the parallax travel of the map, not 100x100), registered in `BackgroundLayerBuilder`; 4 marker objects next to `Space` in the `backgrounds` layer. `Space` parallax 0.1 -> 0.02 and twinkle 0.25 s -> 0.5 s.
- Checks: `check.py` passes on the final art. Not run: `./gradlew` (the proxy blocks `services.gradle.org`, so the wrapper cannot download Gradle), so no compile, no tests, no smoke test, no game launch. The new classes are copies of the pattern of `DesertCanyon`, `SunriseHills` and `Space`.
