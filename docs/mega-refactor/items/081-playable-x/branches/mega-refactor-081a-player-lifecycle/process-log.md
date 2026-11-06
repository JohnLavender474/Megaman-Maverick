# Process log: mega-refactor-081a-player-lifecycle

## Status

- State: planned
- Goal: move the shared player lifecycle out of `Megaman` into functions that a second player class can call
- Done: scan, `SCOPE.md` and `CONTRACT.md` approved, draft `PLAN.md`
- Next: owner approval of `PLAN.md`
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
