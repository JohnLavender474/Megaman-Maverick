# Plan: mega-refactor-060l-protoman-one-input

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

## Steps

### 1. Tests first: the new input in `ProtomanTest`

- Changes: rewrite the slot cases and the slot loops of `ProtomanTest` to the new input (`B` the buster, `UP`+`B`
  the sabre); keep a map of each removed case to its kept or new case in `docs/NOTES.md`; add the new cases of
  `CONTRACT.md`. The test file does not compile until step 2, so steps 1 and 2 are one commit.
- Check: none alone.
- Done: yes

### 2. The one input in `Protoman`

- Changes: the `B` order of `SCOPE.md` item 1; `slideAttack()` (D2); `crouchAttack()` is the crouch shot only (D1);
  `weapon`, `selectNextWeapon()` and the `SELECT` actuator go (`TODO` for `060h` at the controller component);
  `ProtomanWeapon.kt` goes.
- Check: `core:test` green; each new case fails on a mutation of its rule.
- Done: yes

### 3. The crouch slash goes (D1)

- Changes: `SabreState.CROUCH` and its reads in `PlayerSabre.kt`, `ProtomanSabreMoves.CROUCH`, its move list entry, the
  crouch slash animation keys and any table row that names it (`060c` `docs/class-entries.md`). The frames stay in
  the atlas.
- Check: `core:test`, `core:smokeTest` green; no reader of the removed names (`grep`).
- Done: yes

### 4. HUD and the `Hud` atlas go (D3)

- Changes: `protomanIcons` and the `as? Protoman` draw line in `PlayerStatsHandler.kt`; `TextureAsset.HUD`,
  `Hud.txt`, `Hud.png`, `assets/sprites/frames/Hud/`, `HudIconsSmokeTest.kt`; `assets/assets.txt` from the build.
- Check: `core:smokeTest`, `lwjgl3:jar`; no reader of `TextureAsset.HUD` (`grep`).
- Done: yes

### 5. Docs

- Changes: `players/README.md` if it names the slots or `SabreState.CROUCH`; `docs/NOTES.md` decisions and the case
  map; the process log.
- Check: `grep` for `ProtomanWeapon`, `weapon slot`, `SabreState.CROUCH` in code and READMEs.
- Done: yes

### 6. Review before the play test

- Changes: fixes from the review by subagents (branch diff; each manual check).
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar --rerun` green.
- Done: yes
