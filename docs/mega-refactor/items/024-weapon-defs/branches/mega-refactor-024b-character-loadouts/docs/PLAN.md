# Plan: mega-refactor-024b-character-loadouts

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

## New API and its first caller

| API | First caller |
|---|---|
| `CharacterLoadout<O : MegaPlayer>` (`players/`): weapons in select order, rewards per level, a `WeaponLook` per def, the get-weapon region prefix | `GameState.ownsWeapon`, `ControllerComponent.setToNextWeapon`, `MegamanWeaponsHandler`, the screens |
| `WeaponLook<in O>` (`weapons/`): the look fields that leave `WeaponDef` | `MegamanAnimations`, `MegamanAnimationKeySupplier`, `EffectComponents`, `SpritesComponent`, `AnimationsComponent`, `MegamanTrailSpriteV2`, `PlayerStatsHandler`, `LevelPauseScreen`, `GetWeaponScreen`, `EndLevelEventHandler` |
| `WeaponDef<in O : MegaPlayer>`, `WeaponAirDash<in O>` | `MegamanWeapons` (`WeaponDef<Megaman>`) |
| `MegaPlayer.character`, `MegaPlayer.loadout` | `GameState.ownsWeapon` callers, the reward flow, `--weapons` |
| `GameState.ownsWeapon(character, def)`, `addWeapon(character, def)` | `MegamanWeaponsHandler.hasWeapon`, `MegamanJetpackHandler`, the launch weapons, `applyLevelOverride` |
| `PlayerWeaponsComponent` and the `weapons` extension (`null` for a player with none) | `PlayerStatsHandler`, `MegaLevelScreen`, `LevelPauseScreen`, `resetPlayerProgress`, `EndLevelEventHandler`, `GetWeaponScreen` |
| `MegaPlayer.shooting` | `EntityQueries.shooting` (`AbstractEnemy.isMegamanShootingAtMe`) |
| `WeaponDef.preventsWhileFiring(move)` | `currentWeaponPrevents` |

An API with no caller at the end of the work is removed.

## Steps

### 1. Pin today's behavior

- Changes: tests on today's code: the reward weapons of each level, the owned weapons after a completed level, the
  `--weapons` results, and every look string of Megaman's 10 weapons (animation keys, trail regions, HUD bit regions,
  pause and get-weapon regions).
- Check: `./gradlew core:test` passes on today's code.
- Done: yes (2026-11-15)

### 2. Test map room for X and a `Met`

- Changes: a subagent with the `level-builder` skill adds a room `met` to `RefactorTest081.tmx`: a player spawn, a
  floor with painted `tiles`, and one `Met` in front of the spawn.
- Check: the review before the play test reads the room (no game launch).
- Done: yes (2026-11-15)

### 3. The owner type on the def

- Changes: `WeaponDef<in O : MegaPlayer>`, `WeaponAirDash<in O>`; `MegamanWeapons` defs are `WeaponDef<Megaman>`;
  the readers take `WeaponDef<*>` where they need no owner. The `TODO`s in `WeaponDef.kt` go.
- Check: diff read (the build runs at step 6).
- Done: yes (2026-11-15)

### 4. `CharacterLoadout`, `WeaponLook`, the character of a player

- Changes: `CharacterLoadout` and `WeaponLook`; the look fields move from `WeaponDef` into Megaman's loadout with the
  same values; `LevelRewards.weaponsFor` moves into the loadout rewards; `MegaPlayer.character` and `loadout`;
  Megaman's loadout, and empty ones for X and Proto Man. The readers of the look fields read the player's loadout.
- Check: diff read.
- Done: yes (2026-11-15)

### 5. Owned weapons per character

- Changes: `GameState` derives owned weapons from the shared completed levels and each character's rewards, plus a
  per-character set for `--weapons` and `LoadoutOverride` (LibGDX collections). `alwaysOwned` counts only in the
  loadout that lists the def. `--weapons` gives each def to the characters whose loadout lists it.
- Check: diff read.
- Done: yes (2026-11-15)

### 6. The weapons capability and the screens

- Changes: `PlayerWeaponsComponent` (current weapon, select, ammo, add energy, reset), added by `Megaman` and backed by
  his handler. The 5 workarounds, `GetWeaponScreen` and the beam-out read it. The reward flow moves from
  `Megaman.onLevelCompleted` to the level flow, from the slot-0 character's loadout. The select cycle, the energy
  fill and the handler state map read the loadout list.
- Check: `./gradlew lwjgl3:jar core:test` passes; `grep` finds no `is Megaman` or `as Megaman` for weapons.
- Done: yes (2026-11-15)

### 7. `MegaPlayer.shooting`, purity

- Changes: `MegaPlayer.shooting` in the 3 player classes; `EntityQueries.shooting` reads it. `movesBlockedWhileFiring`
  becomes private with `preventsWhileFiring(move)`.
- Check: `./gradlew core:test` passes.
- Done: yes (2026-11-15)

### 8. New tests and the README

- Changes: tests for ownership per character, the reward per character (1 and 2 players), `--weapons` per character,
  a shared `WeaponDef<MegaPlayer>` in two loadouts, `isMegamanShootingAtMe` with X and Proto Man. Mutation check. The
  `weapons/README.md` shows the loadout, the look and the owner type.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Done: yes (2026-11-15)

### 9. Review before the play test

- Changes: code-review subagents on the diff and on each manual check (`docs/mega-refactor/CLAUDE.md`).
- Check: all findings fixed or answered; the process log has the result. Then the play checks and `diffity-open`.
- Done: yes (2026-11-15)
