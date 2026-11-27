# Plan: mega-refactor-190a-rush-jet-summon

How to do the work in `SCOPE.md`. Gradle cannot run in the cloud session (`claude-web.md`, rule 6): the CI of the PR
is the build and test check of each step.

## Steps

### 1. Placeholder atlas and test map

- Changes: a script in `temp/190a/` draws the "dog" frames (NES palette, 2 px grid) into `assets/sprites/frames/Rush/`
  and packs `assets/sprites/sprite_sheets/Rush.txt` and `Rush.png` in the libGDX format (`Nearest`, 2 px padding);
  `assets/sprites/texture_packer/Rush.tps` by hand; `TextureAsset.RUSH`. `RefactorTest190.tmx` with the rooms `flat`,
  `pit`, `ledge`, painted tiles, and `LevelDefinition.REFACTOR_TEST_190`.
- Check: a script reads the atlas back and checks the region names and sizes; a script checks that each room holds a
  spawn and that only spawn `0` is near the origin.
- Done: yes

### 2. The freeze routine

- Changes: `levels/LevelFreezeRoutine.kt`; `EventType.START_FREEZE_ROUTINE`; `MegaLevelScreen` runs it (systems off
  but sprites, update when not paused, systems on at the end).
- Check: CI build.
- Done: yes

### 3. The handler and the def field

- Changes: `WeaponDef.summonsRush`; `MegamanRushHandler` with its phases; `Megaman` makes it, resets it on destroy,
  and `shoot()` calls `summon` first; `RUSH_JET` sets `summonsRush` and `onDeselect` dismisses Rush.
- Check: `MegamanRushHandlerTest` (phases, summon rules, dismiss in each phase) through a small interface to Rush
  (`RushHandle`: arrive, leave, gone), so the test needs no entity. (One implementation: it narrows the handler's need
  of `Rush` to the calls it makes, the case the process allows.)
- Done: yes

### 4. The gated moves and look

- Changes: `Megaman.weaponMovesReady` and `Megaman.drawnWeapon`; `JetpackBehavior`, `AirDashBehavior`,
  `MegamanJetpackHandler`, `currentLook`, the Megaman animation key and `MegamanTrailSpriteV2` read them.
- Check: CI build; `MegamanWeaponsTest` and `MegamanAirDashTest` still pass.
- Done: yes

### 5. The Rush entity

- Changes: `entities/special/Rush.kt` (body with a feet fixture, block filter, sprites and animations, the states, the
  join routine); `entity-tags.txt`.
- Check: `core:smokeTest` (CI).
- Done: yes

### 6. Review and PR

- Changes: the review before the play test (one Sonnet agent), fixes, play scripts, push and a draft PR.
- Check: CI green on the PR head; review findings fixed.
- Done: no
