# Process log: mega-refactor-076b-desert-man-backgrounds

## Status

- State: active
- Goal: Desert Man gets the new animated backgrounds of the `temp/076-desert-bg/` drafts (`SunriseHills` v3, `DesertSunSky` and `SunsetHills` from `sunset_v1`)
- Done:
- Brief: https://claude.ai/artifact/Hn3etdJpCL2J7ATqqFL429
- Next: merge
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-17

- Branch start. Art drafts made before the branch and accepted by the owner: `temp/076-desert-bg/sunrise_hills_v3/` and `temp/076-desert-bg/sunset_v1/` (main checkout).
- Owner decisions before the branch: the dusk mood; the sun and heat haze only in the sunset sky after the cave (not in `SunriseHills`); 8 frames; grow the atlas page.
- Planning: a dry run with `MaxRects` was stopped by the owner: pack with `Basic` only, and use a new atlas when frames do not fit (rule added to the root `CLAUDE.md`, `8b17bce25`). Basic dry run: `Backgrounds6` without the 3 regions 996x1880, `Backgrounds7` 1024x1988, no rotation, the 12 kept regions pixel-identical. Owner answers: D2 only the 3 changed regions move; D3 accept the 20 px height difference, check in play. `SCOPE.md` and `CONTRACT.md` approved; `PLAN.md` written (4 steps).
- Steps 1 to 3: the 3 frames moved to `frames/Backgrounds7/` with the accepted art; `Backgrounds7.tps` (copy of `Backgrounds6.tps`; its per-sprite settings map is empty; the 3 moved files are removed from the per-sprite settings of `Backgrounds6.tps`, which do not change the output). Both packed with the project settings (Basic): `Backgrounds6` 996x1880, `Backgrounds7` 1024x1988, no rotation. Script: the 12 kept regions keep names and pixels; the 3 new regions equal the drafts in every visible pixel (TexturePacker changes only the RGB of 67,240 fully transparent pixels of `SunsetHills`). Code: `BACKGROUNDS_7`, `DesertSunSky` 2x4 at 0.15 s, `SunriseHills` as `AnimatedBackground` 2x4 at 0.2 s, `SunsetHills` on `BACKGROUNDS_7`.
- Automated checks (Corretto 17): `engine:test` 710, `core:test` 2416, `core:smokeTest` 469, `lwjgl3:jar`, `detektMain`, `core:verifyRandomRule` pass.
- Review before the play test (1 subagent): code, atlases, asset loading and 5 of 7 checks PASS; no bug. 2 check wordings fixed: the sunset swap happens while the dark `room14` comes into view (check now starts in `room13`; D3 jump cannot show); the `room3` route keeps `SunriseHills`. 2 backlog candidates (pre-existing per-frame allocations in `AnimatedBackground`/`Background`; `allowRotation` in the `.tps` projects).

## 2026-11-17 (merge)

- Credits (owner, new rule in `docs/ways-of-working/art.md`): `README.md` credits the 3 new backgrounds as made by Claude Fable 5.2, directed by the developer, inspired by the Mega 8-Bit Pixel Pack by Super Icon Ltd (the hills) and by Capcom (the sunset).
- The owner asked to merge the branch (2026-11-17), with no open Diffity thread: the merge approval.
