# Contract: mega-refactor-029e-player-profile

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: draft
- Approved: no

This branch moves the lives and the owned weapons into `GameState`, makes the player read the weapons
and the heart-tank max health from it, moves every profile read to `game.state`, and makes `ItemEntity`
take the player view. With one player and all data shared, the lives, weapons, tanks, enhancements, items,
pause screen, game over, save and load must act as before.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 113 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test passes, and `contact-rule-selection.txt` is unchanged.
- [ ] A test shows that `GameState.toString()` gives the same string as on `master` for the same data
      (levels, heart tanks, health tanks, enhancements, currency, difficulty), also with lives and weapons
      set, and that `fromString` gives back the same owned weapons as the reward table.
- [ ] Tests for: the lives calls (start 3, bounds 0 to 9, `addLife` returns `false` at 9, the slot is
      ignored, `livesExhausted`, `resetLives`, `setLives`); the owned weapons (reward table on level add
      and remove, override, reset, `MEGA_BUSTER` always owned) and `hasWeapon` reading them; the max health
      equal to `START_HEALTH + count × HEALTH_BUMP` after heart tanks are added and removed;
      `addToHealthTanks` (the `MegamanHealthTankTest` cases, moved to `GameState`).
- [ ] A grep finds no profile read through `megaman` or `Megaman` (`lives`, `removeOneLife`,
      `isAtMinLives`, `resetLives`, `hasEnhancement`, `hasHeartTank`, `hasHealthTank`, `hasAnyHealthTanks`,
      `addToHealthTanks`, `hasWeapon`, `putWeapon`, `clearWeapons`), and no `029e` `TODO`.
- [ ] `ItemEntity` and `PlayerItemRule` do not name `Megaman`; `PlayerItemRule` has no cast and no `029`
      `TODO`.
- [ ] `TODO`s exist for: per-player lives (`047`) at the lives count; the game-over rule with more players
      at `livesExhausted` (`047`); late join (heart tanks, weapons) at the player registration (`047`);
      the read-through max health at the max-health set (backlog candidate); the one-up player and the
      `HealthBulb` drop odds player (`047`).
- [ ] A review shows that each reset path (new game, load, level override, game over,
      `resetPlayerProgress`) resets the same mix of state, lives, weapons and current weapon as before.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline play-through;
see "Test maps" in `docs/mega-refactor/CLAUDE.md`). The plan adds a room `lives` (a `Life` item) to
`RefactorTest029.tmx`; a short launch checks that it loads on the baseline code.

`REFACTOR_TEST_029` applies its level override at the start: lives 3, weapons `FRIGID_SHOT` and
`MOON_SCYTHES`, no tanks.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `lives` | The pause screen shows `03`. Taking the `Life` plays the one-up sound and the pause screen shows `04`. |
| `REFACTOR_TEST_029`, `player_death` | Each death lowers the lives in the pause screen by 1. When the last life is lost, the game-over screen opens. |
| `REFACTOR_TEST_029`, `pickups` | The pause screen shows only the buster, `FRIGID_SHOT` and `MOON_SCYTHES`, and each can be selected and fired. Taking the `HeartTank` makes the health bar 2 units longer and fills it. Taking the `HealthTank` shows it in the pause screen; a `HealthBulb` at full health fills the tank; using the tank from the pause screen refills health. After a death, the taken `HeartTank` and `HealthTank` do not come back. The `Screw` adds currency with its sound; the `WeaponEnergyBulb` refills a used weapon. |
| `REACTOR_MAN`, `room5` | Go up into the alt path. The mini-boss `ReactorMonkey` with heart tank `E` drops it when defeated, if it is not owned. (The room before the mini-boss room has no player spawn; `room5` is the nearest earlier room with one.) |
| Main menu, "Start new game", then any robot master level | Only the buster is in the pause screen; lives `03`. |
| Main menu, "Load save file" (a save with defeated levels) | The pause screen in the next level shows the weapons of the defeated levels and the saved heart and health tanks (the tanks empty); lives `03`. |
| `REFACTOR_TEST_029`, end of the level, then save | The save screen opens and saving works. (The save string itself is checked by a test.) |

## Behaviors to check

- The game boots, and a level starts with no exception.
- After a robot master is defeated, the get-weapon screen shows the new weapon, and the next level has it.
  (Check this only if a boss level is played for another reason; the reward table move is covered by a
  test and a review.)
- Item drops from enemies still appear. The odds are random, so they are checked by review: the `Life`
  odds read the shared lives, the `HealthBulb` odds read the target player's health ratio.
- The enhancement effects (damage scaling, jump boost, slide boost, faster charge) read `game.state`; no
  pickup exists, so they are checked by review.

## Intended changes

None.
