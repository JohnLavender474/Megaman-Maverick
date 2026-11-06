# Process log: mega-refactor-081a-player-lifecycle

## Status

- State: merged
- Goal: move the shared player lifecycle out of `Megaman` into functions that a second player class can call
- Done: steps 1 to 6, the play checks and the merge
- Next: none (`081b` starts X)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-05

- No branch exists yet. Scan of `master` at `444681c2d`: `Megaman.kt` (`init`, `onSpawn`, `onDestroy`,
  `onHealthDepleted`, `onEvent`, `invincible`, `canMove`, `canBeDamagedBy`, `takeDamageFrom`, `editDamageFrom`, the
  updatable), `MegamanDamageNegotiator`, the `084` shared functions in `entities/state/`, the item `NOTES.md`
  ("Silent failures for X", "Camera, rooms and slot 1"), `O1` and `O8`, and the other worktrees. Owner decisions
  (2026-11-05): `081a` is a refactor only, with no X code, and merges directly into `master`. Draft `SCOPE.md`.

## 2026-11-06

- Owner (2026-11-05): `invincible` and `canMove` stay rules computed at each read, as free functions; stun, freeze and
  damage as lock reasons go to the new item `126`. `MegamanDamageNegotiator` is renamed to `PlayerDamageNegotiator`,
  with direct overrides for a player class. `fix/lock-group-api` merged into `master` (`e38787ace`):
  `getLockGroup(group).isLockedBy(reason)`.
- Owner (2026-11-06): the 3 open questions of `SCOPE.md` closed as recommended, and the scope approved ("let's start the refactor branch"). Draft `CONTRACT.md`: automated tests for each shared function with mutation checks, 2 experimental play checks for the order in `onSpawn` (`REFACTOR_TEST_051 edge_rotated`) and `takeDamageFrom` (`PRECIOUS_WOMAN room7`), and control checks from the `084` contracts.
- Owner (2026-11-06): `CONTRACT.md` approved ("go for it"). Draft `PLAN.md`: 6 steps; the spawn is 2 shared calls so the gravity set stays before the lock clear; 2 order changes are named (the `aButtonTask` and weapon lines after the health set and lock clears; the damage signal before the sounds), each with a check.
- Owner (2026-11-06): `PLAN.md` approved. Step 1: branch start on `master`; worktree `.worktrees/mega-refactor-081a-player-lifecycle`.

## 2026-11-06 (work)

- Steps 2 to 5. New `players/PlayerRules.kt` (`playerInvincible`, `playerCanMove`, `playerAllowsDamageFrom`,
  `playerAfterDamage`, `playerEditDamage`) and `players/PlayerLifecycle.kt` (`playerSpawnStart`, `playerSpawnEnd`,
  `playerOnDestroy`, `playerOnRoomTransition`, `killPlayerIfOutsideLevel`). `MegamanDamageNegotiator` moved to
  `players/PlayerDamageNegotiator.kt`. `Megaman.kt` calls them (535 to 465 lines); 10 imports that the branch made
  unused went. `players/README.md` added.
- Plan change (step 2): the tests and the code were written together, with no red run first. The mutation checks
  below show that each new test can fail.
- Step 4 check: `aButtonTask`, `setAllToMaxAmmo` and the `currentWeapon` setter (`MegamanWeaponsHandler.onChangeWeapon`)
  read no health and no lock, so their move after the health set and the lock clears changes nothing.
- The `require` reads fail fast: 3 older tests built a partial `Megaman` and relied on the silent defaults
  (`MegamanRunTest`, `MegamanCrouchDelayTest`, `FriendlyFireTest`, 67 tests). A test helper
  `Megaman.addRuleComponents()` adds the components. The game `Megaman` always has them.
- The out-of-bounds error text is now "The player is outside the level bounds, kill it" (the contract is updated).
  The room-transition debug lines log under the tag `PlayerLifecycle`.
- New tests: `PlayerRulesTest` (27) and `PlayerLifecycleTest` (11), on `TestHealthPlayer`, a test player that is not
  `Megaman`. `MegamanInvincibleTest` passes with its recovery component added.
- Mutation checks, 13, each caught by at least 1 test: the recovery and play-phase terms of `playerInvincible`, the
  stun term of `playerCanMove`, the own-shot rule, the damage signal, the hard-mode +1, the negotiator own-shot rule,
  the movement lock clear and the gravity set at spawn, slot 0 only for `PLAYER_SPAWN`, the listener removal at
  destroy, gravity off at a room transition, and the out-of-level destroy.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes: `engine` 638, `core` 913, `smokeTest` 436, 0
  failures.
- Step 6: a `timeout 12` launch of each of the 9 contract rooms (`REFACTOR_TEST_051 edge_rotated`, `PRECIOUS_WOMAN
  room7`, `REFACTOR_TEST_084 knockback`, `carry_left`, `out_of_bounds`, `REFACTOR_TEST_029 stun`, `player_death`,
  `GLACIER_MAN room6`, `room1`) ended with code 124 and no exception. The only errors are the known map issues
  `FirstSpawn` (`PRECIOUS_WOMAN`) and `Sealion` (`GLACIER_MAN`), listed in `docs/issues/2026-10-22-misplaced-tmx-objects.md`.

## 2026-11-06 (merge)

- Owner (2026-11-06): the play checks pass ("Tests passed"), and the owner approved the merge.
- The branch did not do the "Review before the play test", because the rule came after its plan.
- `master` merged into the branch (`118j` and the owner commit `6c29604ef` "Megaman: tidy imports and blank lines").
  Resolved file: `Megaman.kt`, a simple conflict: the import block (kept the `master` form `players.*`, dropped
  `getPositionPoint`, which the branch made unused) and a blank line after `damageNegotiator`.
- After the merge, `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes: `engine` 638, `core` 923,
  `smokeTest` 436, 0 failures.
- Overlap: `038a` changes no file of this branch; its scope already names `081a` (`Megaman.kt` sends
  `PLAYER_JUST_DIED` and `PLAYER_SPAWN`, now through `playerOnDestroy` and `playerSpawnEnd`). `038b` must move the
  `PLAYER_SPAWN` submit in `playerSpawnEnd`. `x6-intro-extra-level` changes no file of this branch, but it must move
  its 7 entries of `MegamanDamageNegotiator` into `players/PlayerDamageNegotiator.kt` when it takes in `master`.
