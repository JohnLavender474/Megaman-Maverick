# Contract: mega-refactor-062d-queued-spawn-query

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: everything. The pool return of a rejected spawn frees an entity in exactly the cases it
does today: `!engine.hasClaimOn(entity)` equals the old `!entity.existenceState.liveNow &&
!entity.engine.contains(entity, containedIfQueuedToSpawn = true)` on every path. No engine action and no state
changes.

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [ ] The build passes and the jar builds.
- [ ] All engine and core tests pass, 0 failures. The counts go up only by the new tests.
- [ ] The compiler warnings are the same set as the baseline (counted at plan step 1).
- [ ] **Temporary contract suite** `TempHasClaimOnContractTest` (engine test sources): on at least 10,000 seeded random
  call sequences over several entities (`spawn`, `destroy`, `update`, `reset`, a refused `canSpawn`, and calls from
  inside `onSpawn`/`onDestroy`), before every call, `engine.hasClaimOn(e)` equals the old condition
  `e.existenceState.liveNow || engine.contains(e, containedIfQueuedToSpawn = true)` for every entity. The suite counts
  how often the two parts of the old condition decide the answer (live now only; in the spawn queue only; inside the
  entity's own `onDestroy`), and each must occur at least once. It runs while the old parameter still exists, and it
  is deleted, with the parameter, before the merge; that commit runs the other checks again.
- [ ] **Permanent engine tests** for `hasClaimOn` on each path: never spawned; spawn queued; live; destroy queued;
  respawn queued; inside the entity's own `onSpawn` and `onDestroy`; a spawn and then a destroy before the update
  (claimed until the destroy runs); after the destroy (not claimed); after `reset()`.
- [ ] Greps: no `containedIfQueuedToSpawn`; `GameEngine.contains` is `internal`; `hasClaimOn` has one production
  caller (the pool return).
- [ ] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

A short sanity pass. The owner plays these once, after the implementation, from the worktree, on normal difficulty.
In these rooms, the objects marked `hard_mode_only` are rejected at each spawn try, and the pool return runs.

- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: the normal enemies come, and no hard-only `Bat` or
  `NuttGlider` shows; leave the room and come back a few times; no crash.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: the same; the hard-only `SniperJoe` and `HeliMet` do not
  show.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: shoot many shots (pooled bullets); the shot limit
  works.

## Behaviors to check by review

- The pool return in `MegamanMaverickGame.kt` reads `!entity.engine.hasClaimOn(entity)` and nothing else changed in
  `onSpawnRejected`.

## Intended changes

None.
