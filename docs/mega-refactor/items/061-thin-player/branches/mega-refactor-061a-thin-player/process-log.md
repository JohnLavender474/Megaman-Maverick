# Process log: mega-refactor-061a-thin-player

## Status

- State: merged
- Goal: a thin `MegaPlayer` with free functions over the entity; locks (engine mechanism, game groups and reasons) and an immediate damage signal; based on `temp-thin-player`
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master`.
- The code exists on the experimental branch `temp-thin-player` (commits `10f77483d`, `7af7d3890`, `f5aa6c3f7` on
  `master` at `55e81840c`); the owner tested it ("Tests pass"). The owner chose a new item and asked for the cleanup
  to move the locks to the engine. Draft `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` written together for
  one review, because the work is already built and tested.
- The owner approved `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` together ("Yes"). Step 1 (branch start):
  `ITEMS.md` rows and `ITEM.md` set to active; worktree created; `temp-thin-player` merged with `--no-ff`.
- Step 2 (baseline, after the merge, on `5bddd6547`): clean build passes; 493 engine and 163 core tests, 0 failures;
  114 warnings. Note for the merge: the item `061` row in `ITEMS.md` has a double space before its start date (a
  `master`-only file; fixed at the merge).
- Step 3: `docs/temp-thin-player-review.md` moved into this folder's `docs/NOTES.md` (section "Notes from
  `temp-thin-player`").
- Step 4: the lock mechanism is in the engine (`engine/.../locks/`): `LockGroup` and `LockReason` interfaces with
  `index`; `LocksComponent(groupCount, onChange)` (one `Int` bit set per group; refuses a group index outside
  `0 until groupCount` and a reason index outside `0 until 32`; `onChange(group, true)` on each add,
  `onChange(group, false)` when a remove or a clear frees the group; `reset()` is silent); the free functions `lock`,
  `unlock`, `clearLocks`, `isLocked`. Core `locks/` holds `MegaLockGroup` and `MegaLockReason` (explicit indexes, the
  old order) and `behaviorsLockHook(entity)` (plan detail, no approval needed: the hook is the game's behaviors side
  effect, so it stays in core next to the enums). `Megaman` adds `LocksComponent(MegaLockGroup.entries.size,
  behaviorsLockHook(this))`. The 7 call-site files changed imports and enum names only. Tests: engine `LocksTest`
  (8: the reason set, a reason held once, a foreign remove, clear and reset, range checks, the hook calls, the
  functions); core `MegaLocksTest` (3: the enum indexes, the behaviors hook on each lock and when free, no effect for
  another group); the old core `LocksTest` goes. Build passes; 501 engine and 159 core tests, 0 failures. Launches of
  `REFACTOR_TEST_029 --room movement` and `INTRO_STAGE --room room12` ran with no exception until the timeout.
- The owner asked to rethink `ready` and `teleporting`, which apply to non-player entities too; a research agent works
  on it (read-only).
- Step 5: `MegaPlayersTest`'s `TestPlayerEntity` loses the unused `spawn` override and `spawnedWith`. The imports that
  this branch added (the temp branch's script appended them after the last import) moved to their sorted place in
  each file, by script, in 178 files; the other imports did not move (pure moves: insertions equal deletions in every
  file except `MegaPlayersTest`). Also `Megaman.kt`'s `ControllerComponent` import (added at the top in `031b`) moved
  to its sorted place. Clean build passes; 501 engine and 159 core tests, 0 failures; 114 warnings.
- Step 6, automated contract checks (clean build on `c8d8e4ee8`): build passes; 501 engine and 159 core tests, 0
  failures (`master`: 489 and 158); 114 warnings (same as `master`); `lwjgl3:jar` builds; `contact-rule-selection.txt`
  unchanged; the engine `LocksTest`, `SignalTest`, core `MegaLocksTest` and `MegaPlayersTest` pass; greps:
  `MegaPlayer.kt` has no `require` and no cast; no `PlayerKeys`, `IPlayerDamageListener`, `canBeDamaged` property or
  `allowMovement` in `core/src/main`; the only `setAllBehaviorsAllowed` call in `core/src/main` is in
  `behaviorsLockHook`; no `docs/temp-thin-player-review.md`. Waiting for the owner's play test.
- Step 7, play test: the owner played the manual checks in `docs/CONTRACT.md` ("Play tests pass") and approved the
  merge ("Yes to all").
