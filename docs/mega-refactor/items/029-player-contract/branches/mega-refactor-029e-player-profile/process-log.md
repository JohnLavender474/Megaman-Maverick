# Process log: mega-refactor-029e-player-profile

## Status

- State: merged
- Goal: Lives, tanks, enhancements and weapons owned in one session profile; `ItemEntity` takes the player view
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `b3bc97f80`. Findings and open questions in `docs/NOTES.md` ("Scan"). No scope yet.

- The owner answered the scan questions 1-7 (`docs/NOTES.md`, "Decisions"). Draft `docs/SCOPE.md` written.
- The owner proposed that the player reads the owned weapons from `GameState` (no copy), with heart tanks too if easy, and shared data separate from per-player data (option (a)). Scope updated.
- The owner approved `docs/SCOPE.md`, and asked for a `TODO` for the read-through max health (backlog candidate in `docs/TODO.md`).
- Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written.
- The owner approved `docs/PLAN.md`. Branch start (step 1): `ITEMS.md` row and `ITEM.md` set to active; worktree created.
- Step 2 (baseline, worktree on `ed0ed0651`): clean build passes; 470 engine and 113 core tests, 0 failures; 114 warnings.
- Step 3: a subagent added the room `lives` to `RefactorTest029.tmx` with the `level-builder` skill (map width 316 to 332 columns; room at x=10112, 16x14; floor, ceiling and walls painted with tile 175; spawn `15`; one `Life` with the properties from `IntroStage_v3.tmx`). No existing object or tile changed (compared by layer and id). A launch of `--room lives` ran with no exception until the timeout.
- Step 4: `GameStateSaveStringTest` pins today's save string; it passed on the code before the changes (3 tests). Lives moved into `GameState` (`getLives`, `addLife`, `removeLife` with the slot; `livesExhausted`, `resetLives`, `setLives`; `047` `TODO`s at the count and at `livesExhausted`). `addToHealthTanks` and `hasAnyHealthTanks` moved to `GameState`; `MegamanHealthTankTest` became `GameStateHealthTankTest`. New `GameStateLivesTest`. Plan deviation (no approval needed): `Megaman.lives` was a field with five direct users, so those five moved to their final form in this step, not in step 7: `Life` (`addLife` with the slot), `AbstractEnemy` one-up (`addLife(targetPlayer.slot)`, `047` `TODO`) and `Life` drop odds (`getLives(targetPlayer.slot)`), `LevelPauseScreen` (`getLives(PlayerSlot(0))`), the level override (`setLives`). Build passes; 470 engine and 123 core tests, 0 failures.
- Step 5: the reward table moved to `levels/LevelRewards.kt`. `GameState` holds the owned weapons (`containsWeapon`, true for `MEGA_BUSTER` at all times; `addWeapon`; `clearWeapons`); `addLevelDefeated` adds the reward weapons before it tells the listeners, `removeLevelDefeated` removes them. `MegamanWeaponsHandler` makes one entry for each weapon at creation and `hasWeapon` reads `game.state`; `putWeapon`, `removeWeapon`, `Megaman.putWeapon`, `Megaman.clearWeapons` and `Megaman.onRemoveLevelDefeated` are gone. `Megaman.onAddLevelDefeated` only sets `WEAPONS_ATTAINED`. `resetPlayerProgress` calls `state.clearWeapons()` and selects the buster; the level override calls `state.addWeapon`.
  - Plan detail (no approval needed): `GameState.reset()` does not clear the set. Today `reset()` removes only the reward weapons (through the level removals), and weapons from a level override stay until `resetPlayerProgress` (for example "Start new game" after an extra level keeps them). `clearWeapons()` keeps that mix exactly. The scope line "`reset` clears the set" is done this way.
  - Review of the current-weapon fallback: `removeWeapon` set the current weapon to the buster when the lost weapon was selected. The losses happen only in the reset paths (new game, load, `resetPlayerProgress`). `resetPlayerProgress` selects the buster as `clearWeapons` did. New game and load reach a level only through a spawn, and `Megaman.onSpawn` selects the buster and fills all ammo (`Megaman.kt`, `onSpawn`). `canFireWeapon` also checks `hasWeapon`. So no play state differs.
  - Tests: `GameStateWeaponsTest` (6), `MegamanWeaponsHandlerOwnershipTest` (3). Build passes; 470 engine and 132 core tests, 0 failures. A launch of `--room pickups` ran with no exception until the timeout.
- Step 6: Megaman's heart-tank listener sets the max health to `START_HEALTH + count × HEALTH_BUMP` from `game.state` (`setMaxHealthFromHeartTanks`), with the read-through `TODO`. The late-join `TODO` is at the player registration in `MegamanMaverickGame`. New `MegamanHeartTankTest` (4: add and remove, a repeated add, all tanks give `MAX_HEALTH`, reset). Build passes; 470 engine and 136 core tests, 0 failures.
- Step 7: every caller of the `Megaman` profile wrappers reads `game.state` (script for the repeated pattern, then a line-by-line review of the diff: each changed line differs only in the receiver and the call name). `MegaLevelScreen` removes the life of the slot in `PLAYER_JUST_DIED`. The wrappers (`hasAnyHealthTanks`, `hasEnhancement`, `hasWeapon`, `hasHeartTank`, `hasHealthTank`, `addToHealthTanks`, `isAtMinLives`, `removeOneLife`, `resetLives`) are gone. `MegaPlayer.healthRatio` added; the `HealthBulb` drop odds read `targetPlayer.healthRatio` with a `047` `TODO`. The `029e` `TODO`s in `AbstractEnemy`, `AbstractBoss` and `ReactorMonkey` are gone. The contract grep finds no profile read through `megaman` and no `029e` text in code (the read-through `TODO` was reworded to name the backlog candidate, not the branch). Removed the `kotlin.math.min` import that step 4 left unused in `Megaman`. Build passes; 470 engine and 136 core tests, 0 failures. Launches of `--room lives` and `--room player_death` ran with no exception until the timeout.
- Step 8: `ItemEntity.contactWithPlayer(player: MegaPlayer)`; the 7 items put `player.slot` in their event props (`Life`: `addLife(player.slot)`); script for the repeated pattern, and the diff has only these line kinds (the signature, the slot, the debug text, the imports). `PlayerItemRule` uses `asPlayer` and has no cast and no `TODO`. Removed the `megaman` extension imports that step 7 left unused in `HealthTank` and `HeartTank`. New `PlayerItemRuleTest` (2). Build passes; 470 engine and 138 core tests, 0 failures. A launch of `--room pickups` ran with no exception until the timeout.

## 2026-10-24
- Step 9, automated checks (on `7e318851e`): clean build passes; 470 engine and 138 core tests (113 on `master` plus 25 new), 0 failures; 114 warnings (same as `master`); `lwjgl3:jar` builds; `contact-rule-selection.txt` unchanged; the save-string test passes; the lives, weapons, heart-tank, health-tank and `PlayerItemRule` tests pass; the profile grep finds no read through `megaman` and no `029e` text; `ItemEntity` and `PlayerItemRule` do not name `Megaman`; the `TODO`s exist (per-player lives and `livesExhausted` in `GameState`, late join at the registration, the read-through max health in `Megaman`, the one-up and the `HealthBulb` odds in `AbstractEnemy`).
- Review of the reset paths (before, then after):
  - `resetPlayerProgress`: `state.reset()` (the levels and their reward weapons go), `Megaman.clearWeapons` (all weapons but the buster; buster selected), lives reset, `WEAPONS_ATTAINED` removed. After: `state.reset()` (the levels and their reward weapons go), `state.clearWeapons()`, buster selected, `state.resetLives()`, `WEAPONS_ATTAINED` removed. Same.
  - "Start new game": `state.reset()` and lives reset; weapons from a level override stay. After: the same (`reset()` does not clear the set).
  - Load: `state.reset()`, `fromString` (levels give their reward weapons; `WEAPONS_ATTAINED` set, then removed by the main menu), lives reset. After: the same, through the set.
  - Level override: `resetPlayerProgress`, then the loadout (weapons through `state.addWeapon`), difficulty, lives (`state.setLives`). Same.
  - Game over: lives reset only. Same.
  - The only shift: on "Start new game" and load, a selected weapon that is lost no longer switches to the buster at once; the next spawn selects the buster (as it always does), and no play state is reached in between.
- Step 9, play test: the owner played every manual check in `docs/CONTRACT.md` (`lives`, `player_death`, `pickups`, `REACTOR_MAN` `room5`, load, new game, save) and reported "All tests pass". The contract checks are done.
- Step 10: the owner approved the merge. Squash commit `eca1b3571` on `master`. The backlog candidate became item `059`. No bugs.
