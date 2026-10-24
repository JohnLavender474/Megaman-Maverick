# Process log: mega-refactor-029f-level-players

## Status

- State: merged
- Goal: Level screen, handlers, HUD, pause and menus use the players service; remove `game.megaman` and the `megaman` extension
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-24

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `5da7a6db5`. Findings and open questions in `docs/NOTES.md` ("Scan"). No scope yet.

- The owner accepted the scan recommendations 1-7. Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written.
- The owner approved `docs/PLAN.md`. Branch start (step 1): `ITEMS.md` row and `ITEM.md` set to active; worktree created.
- Step 2 (baseline, worktree on `b8e10df06`): clean build passes; 470 engine and 138 core tests, 0 failures; 114 warnings.
- Step 3: `MegaPlayer` gets `currentHealth`, `maxHealth`, `translateHealth`; `players/PlayerExtensions.kt` gets `val MegaPlayer.megaman: Megaman` with the `024`/`029g` `TODO`. `PlayerStatsHandler` takes the game; it, `LevelPauseScreen` and `GetWeaponScreen` read the lead player through a private `player` property (`PlayerSlot(0)`, `047` `TODO`); health through the view, weapons and ammo through `.megaman`; the pause health check `hasMaxHealth()` became `currentHealth >= maxHealth` (the same expression as `IHealthEntity.hasMaxHealth`). `resetPlayerProgress` selects the buster for each player. Unused imports removed (`slotOf`, `Megaman`); the `PlayerSlot` import in `LevelPauseScreen` moved into sorted order. Two new tests in `MegaPlayersTest`. Build passes; 470 engine and 140 core tests, 0 failures. A launch of `--room pickups` ran with no exception until the timeout.
- Step 4: `MegaPlayer` gets `focus` (the entity must be `IFocusable`; checked at registration) and `spawn(props)` (through its `MegaGameEntity`). `MegaLevelScreen` reads the lead player through a private `player` property with one `047` `TODO` that lists the level rules that use it; `players.forEach` for `canBeDamaged` on `BOSS_DEFEATED` and `INTERMEDIATE_BOSS_DEAD` and for `running = false` after a room transition; the projectile cleanup keeps a projectile when `owner.asPlayer != null`. Each changed line differs only in the reference (diff reviewed). The test player entity in `MegaPlayersTest` now implements `IFocusable`; two new tests (focus and spawn; an entity that is not focusable fails to register). Build passes; 470 engine and 142 core tests, 0 failures. A launch of `--room lives` ran with no exception until the timeout.
- Step 5: the spawn, death and end-level handlers read the lead player through a private `player` property (one `047` `TODO` each) in place of their `game.megaman` fields; generic state through the view (`ready`, `canBeDamaged`, `setAllBehaviorsAllowed`, `body`, `direction`, `facing`; `isFacing(LEFT)` became `facing == LEFT`); the sprite priority, flip and current weapon through `.megaman`; the event slot from the view. Plan detail (no approval needed): the handlers write `ready`, so `MegaPlayer.ready` became writable through `PlayerStateComponent` (as `canBeDamaged` already is; `Megaman.ready` delegates to the same field); the `MegaPlayersTest` ready test now also writes it. Review of the read order: the handlers are made in `MegaLevelScreen.init`, which `postCreate` calls after `players.register`; the old fields read `game.megaman` at construction, the new property reads slot 0 on each use, and the slot holds the same Megaman. Each changed line differs only in the reference (diff reviewed). Build passes; 470 engine and 142 core tests, 0 failures. A launch of `--room player_death` ran with no exception until the timeout.
- Step 6: `MegamanMaverickGame.megaman` became a local value in `postCreate`; the extension `IMegaGameEntity.megaman` is gone; `AbstractItem` reads `game.gravity.direction`; `MegamanTrailSprite` (V1) is commented out in full with a `TODO`, with its `DecorationsFactory` pool entry and import. The build then showed 40 files that still imported the removed extension without using it (left from `029b` to `029d`); a script removed that one import line in each (the diff has no other change in them). The contract grep finds no `game.megaman`, no field and no extension. Build passes; 470 engine and 142 core tests, 0 failures. A launch of `--room lives` ran with no exception until the timeout. Root `CLAUDE.md` update noted in `docs/TODO.md` for `master`.
- Step 7, automated checks (on `a5eea8f67`): clean build passes; 470 engine and 142 core tests (138 on `master` plus 4 new), 0 failures; 114 warnings (same as `master`); `lwjgl3:jar` builds; `contact-rule-selection.txt` unchanged; the grep finds no `game.megaman` and no `megaman` extension in `core` and `lwjgl3`; the only `megaman` fields left are the weapon handler, jetpack handler and damage negotiator (their own player) and the local value in `postCreate`; the `TODO`s exist (lead slot in the level screen, the three handlers, the HUD, pause and get-weapon screens; the accessor; the commented-out trail sprite). The diff review of steps 3 to 6 showed only reference changes, unchanged handler order and checks, and no lead-player read before the registration.
- Step 7, play test: the owner played every manual check in `docs/CONTRACT.md` (`lives`, `player_death`, `pickups`, `GLACIER_MAN` `room1` and `room12`, `REACTOR_MAN` `room5`, `WILY_STAGE_3` `room2`, `MOON_MAN` `room6`) and reported "All tests pass". The contract checks are done.
- Step 8: the owner approved the merge. Squash commit `6716d074f` on `master`. The root `CLAUDE.md` was updated on `master` (the `docs/TODO.md` task; it also named the renamed `MegamanHealthTankTest`). No backlog or bug candidates.
