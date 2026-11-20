# Plan: mega-refactor-077f-glacier-man-art

How to do the work in `SCOPE.md`. Art goes to one Fable art agent (`docs/ways-of-working/art.md`); the main session
keeps the packing and the checks.

## Steps

### 1. Reference sheets

- Changes: contact sheets in `temp/mega-refactor-077f-glacier-man-art/`: all Glacier Man frames, `SmallIceCube`,
  `FallingIcicle`, `IcicleShards`, the palette of the Glacier Man frames.
- Check: the sheets show every existing frame at 3× with names.
- Done: yes (2026-11-19)

### 2. Art agent: Glacier Man throws (`chunk`, `aim`, `aim_up`, `heave`)

- Changes: drafts in `temp/`, then strips in `assets/sprites/frames/Bosses1/GlacierMan/`.
- Check: preview GIFs at game scale next to `stand` and `stand_shoot`; same palette, outline and proportions.
- Done: yes (2026-11-19, round 1; owner review in step 5)

### 3. Art agent: Icicle Rain (`GlacierMan/pound`, `FallingIcicle/form`)

- Changes: strips in `assets/sprites/frames/Bosses1/GlacierMan/` and `Projectiles2/`. Round 2 (2026-11-19): `pound`
  in place of the dropped `heave` and `GlacierIceChunk`.
- Check: mock-up GIF pound → icicles form, shake, drop.
- Done: yes (2026-11-19, rounds 2 to 7; round 6 kept)

### 4. Art agent: death (`freeze`, `frozen_crack`, `frozen_shatter`)

- Changes: strips in `assets/sprites/frames/Bosses1/GlacierMan/`.
- Check: mock-up GIF freeze → crack → shatter with `IcicleShards` pieces.
- Done: yes (2026-11-19, round 1; owner review in step 5)

### 5. Owner review of the previews

- Changes: redraws from the owner's notes.
- Check: the owner accepts each strip.
- Done: yes (2026-11-19)

### 6. Pack the atlases

- Changes: repack `Bosses1` and `Projectiles2` with `Basic` (`docs/ways-of-working/assets.md`).
- Check: script: each old region keeps its name and pixels; each new region is present with its frame count.
- Done: yes (2026-11-19)

### 7. Credits and final frame list

- Changes: README `CREDITS` (lines in `SCOPE.md`); `NOTES.md` "Final frame list".
- Check: `./gradlew core:smokeTest lwjgl3:jar`.
- Done: yes (2026-11-19)
