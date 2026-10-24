# Contract: mega-refactor-029f-level-players

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: draft
- Approved: no

This branch changes how the level screen, its handlers, the HUD, the pause screen, the get-weapon screen and
the game setup reach the player, and removes `game.megaman` and the `megaman` extension. With one player the
lead slot, `players.forEach` and the typed accessor all give Megaman, so every level flow must act as before.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 138 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test passes, and `contact-rule-selection.txt` is unchanged.
- [ ] A grep finds no `game.megaman`, no `MegamanMaverickGame.megaman` field and no `IMegaGameEntity.megaman`
      extension in `core/src/main`, `core/src/test` and `lwjgl3/src`. The only fields named `megaman` left
      are the ones that hold their own player (weapon handler, jetpack handler, damage negotiator) and the
      commented-out `MegamanTrailSprite`.
- [ ] Tests for the new `MegaPlayer` reads (`currentHealth`, `maxHealth`, `translateHealth`, `spawn`,
      `focus`) and the `megaman` accessor.
- [ ] `TODO`s exist for: the lead slot at each lead-player property (`047`); the `megaman` accessor (`024`,
      `029g`); the level rules that use the lead slot (`047`); `MegamanTrailSprite` commented out.
- [ ] A review shows that each changed line differs only in the player reference, that the handler order
      and checks are unchanged, and that no lead-player read runs before the registration in `postCreate`.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline play-through; see
"Test maps" in `docs/mega-refactor/CLAUDE.md`). No new test rooms are needed.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `lives` | The level starts with the beam-in; Megaman lands and can move. The HUD shows the health bar and, with a weapon selected, the weapon bar. Pause opens; the health bar and the weapon ammo bars show; selecting a weapon works; unpause works. |
| `REFACTOR_TEST_029`, `player_death` | Death plays the death animation and fade; Megaman respawns with the beam-in; pause is not possible while dead. |
| `GLACIER_MAN`, `room1` | Walking on into `room2` (it has a player spawn) shows the checkpoint text. |
| `REFACTOR_TEST_029`, `pickups` | The `HealthBulb`, `WeaponEnergyBulb` and `HeartTank` fill the HUD bars as before. Using the health tank from the pause screen refills health one unit at a time. |
| `GLACIER_MAN`, `room12` | Go through the gate. The boss intro, the fight, and the defeat: Megaman cannot be damaged after the defeat, the beam-out plays in the color of the current weapon, then the get-weapon screen starts its animation from the current weapon. |
| `REACTOR_MAN`, `room5` | Go up into the alt path. After the mini-boss is defeated, the controls and music come back and Megaman can be damaged again. (The room before the mini-boss room has no player spawn; `room5` is the nearest earlier room with one.) |
| `WILY_STAGE_3`, `room2` | Room transitions: Megaman's own shots stay through a transition, enemy shots go; the camera follows Megaman. Walking on to `room8` sets gravity back up with the aura at Megaman. |
| `MOON_MAN`, `room6` | After the gravity switch, items (if any drop) take the world direction; the camera follows Megaman. |

## Behaviors to check

- The game boots, a level starts with no exception, and the main menu and level select work.
- The level-end flow of a robot master level leads to the get-weapon screen and then the save screen as
  before.

## Intended changes

None.
