# Plan: mega-refactor-076b-desert-man-backgrounds

## Steps

### 1. Frames and projects

- Changes: remove `SunriseHills.png`, `SunsetHills.png` and `Desert/SunSky.png` from `assets/sprites/frames/Backgrounds6/`; add the accepted sheets as `assets/sprites/frames/Backgrounds7/` (same names); add `assets/sprites/texture_packer/Backgrounds7.tps` (a copy of `Backgrounds6.tps` with its folder and output).
- Check: the 3 new frames are byte-equal to the accepted drafts.
- Done: yes (2026-11-17)

### 2. Pack

- Changes: `TexturePacker` on both projects (Basic, the project settings, no overrides).
- Check: script: the 12 kept regions keep names and pixels; `Backgrounds7` has the 3 regions; no rotation; both pages at most 2048x2048.
- Done: yes (2026-11-17)

### 3. Code

- Changes: `TextureAsset.BACKGROUNDS_7`; `DesertSunSky.kt` (atlas, 2x4, 0.15 s); `BackgroundLayerBuilder.kt` (`SunriseHills` as `AnimatedBackground`, 2x4, 0.2 s; `SunsetHills` atlas).
- Check: `lwjgl3:jar` builds.
- Done: yes (2026-11-17)

### 4. Checks and review

- Changes: none.
- Check: the automated checks of the contract; the review before the play test; then the play test and the Diffity review.
- Done: no
