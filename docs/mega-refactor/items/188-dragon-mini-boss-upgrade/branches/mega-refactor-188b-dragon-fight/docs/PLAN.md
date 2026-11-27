# Plan: mega-refactor-188b-dragon-fight

How to do the work in `SCOPE.md`.

## Steps

### 1. Atlas

- Changes: move the frames to `assets/sprites/frames/MechaDragon/` (`MechaDragon/<strip>.png`,
  `MechaDragonFlame.png`); remove their entries from `Bosses1.tps`; add `MechaDragon.tps`; generate
  `sprite_sheets/MechaDragon.txt` and `.png` with `temp/dragon-code/make_atlas.py`; add `TextureAsset.MECHA_DRAGON`.
- Check: `temp/dragon-code/check_atlas.py` reads the atlas back and compares each region with its source pixels;
  running the generator twice gives the same bytes.
- Done: yes

### 2. Map

- Changes: `LeftLandTarget` (943) and `RightLandTarget` (944) in `children`; props `left_land_target`,
  `right_land_target` on `399`; `nextobjectid` 945.
- Check: script parse of the TMX: ids unique, the props point to the new objects, the bottom edges are the ledge tops.
- Done: yes

### 3. Pure parts

- Changes: `mechadragon/MechaDragonArt.kt`, `MechaDragonDirector.kt`, `MechaDragonTuning.kt`.
- Check: their specs (step 6).
- Done: yes

### 4. Flame projectile and damage entries

- Changes: `projectiles/MechaDragonFlame.kt`; `PlayerDamageNegotiator` entries; `entity-tags.txt`.
- Check: `MechaDragonFlameTest`; smoke test in CI.
- Done: yes

### 5. MechaDragon

- Changes: regions from the new atlas; sprite layouts; air and ground fixtures; states `LAND`, `BREATH`, `CHOMP`,
  `TAKEOFF`; volley aim mix; high lane and charge tell; drips; panic; hard rows.
- Check: careful read against the engine APIs (no compiler here); CI compile; play checks 1 to 3.
- Done: yes

### 6. Tests

- Changes: `MechaDragonDirectorTest`, `MechaDragonArtTest`, `MechaDragonAtlasTest`, `MechaDragonFlameTest`,
  `MechaDragonTuningTest` in `core/src/test/.../entities/bosses/` (and `projectiles/`).
- Check: CI (`core:test`).
- Done: yes

### 7. Credits and play scripts

- Changes: README `CREDITS`; `temp/play/play-1.sh` to `play-3.sh`.
- Check: read back.
- Done: yes
