# Process log: mega-refactor-045a-pool-ownership

## Status

- State: active
- Goal: Pool ownership rules and debug checks for `GameObjectPools`; per-frame allocations of pooled types move to the pool or owned fields; a `CODE_STYLE.md` rule
- Brief: https://claude.ai/artifact/HrVFKuwFiz2JPhvGJbYQ5o (draft; decisions are Claude's choice, pending owner confirmation)
- Done: scan, research (`docs/NOTES.md`), scope, contract, brief, implementation, docs, automated checks, review before the play test
- Next: owner's confirmation of D1 to D6, play test and diff review
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-16

- Branch start on `master`. The owner asked for the work end to end by a subagent (2026-11-16). Merge target: directly into `master` (Claude's recommendation; the owner can change it).

## 2026-11-16 (subagent session)

- Scan: 1,611 constructor calls of pooled types in `engine/src/main` and `core/src/main` sorted by a script
  (`temp/mega-refactor-045a-pool-ownership/scan_new.py`, output `scan-new.tsv`); 19 `free()` sites; about 60
  non-reclaim fetches; 4 hidden pooled defaults; stored-scratch scans for fields and spawn props.
- An Explore subagent located the live rooms of the changed entities (read-only; table saved to
  `temp/mega-refactor-045a-pool-ownership/entity-rooms.md`).
- Research on the lifetime value (owner request): keep 3 (`docs/NOTES.md`). Sizes measured on the JVM.
- Decisions D1 to D6 taken as Claude's choice, pending owner confirmation (`docs/SCOPE.md`). No questions asked, as the
  owner requested.
- Brief rendered (one `✗`, a long chip that scrolled sideways at 390 px; fixed) and published.
- Implementation: scratch-only `GameObjectPools` with `--poolChecks`; `OwnedArray`; all free pairs and non-reclaim
  fetches removed; stored-scratch fixes; hidden defaults removed; per-frame sweep of the hot sites; `CODE_STYLE.md`
  rule; `core/.../utils/README.md`; tests (`GameObjectPoolsTest`, `OwnedArrayTest`, `PoolOwnershipGuardTest`, a
  `WorldPathfinderTest` case).
- The first `core:test` run hung in `IdentityMap.locateKey` (the engine `Pool` double-free set) when the new pool
  test reclaimed the objects that earlier specs had fetched. Measured: `IdentityMap` grows far faster than linear
  (`docs/NOTES.md`). `GameObjectPool` now keeps a plain `Array` free list. The engine `Pool` case is a bug candidate in
  `docs/TODO.md`. The hung test worker of this worktree was stopped by its PID.
- Automated checks after the first commit: `engine:test` 699 passed, `core:test` 2,149 passed, `core:smokeTest` 468
  passed, `lwjgl3:jar` built.
- Review before the play test, 2 general-purpose subagents (no game launch, no Gradle):
  - Diff review: 2 stored-scratch bugs the branch missed (`SpikeBall` pendulum anchor, `UnderwaterPenguinBot` trigger
    box and start; both `TEST_1` only), 1 old bug next to a changed line (`MockingByte` never cleared its return-position
    list), contract-only violations (`FallingLeaves` cull lambda, `CaveRock.trajectory`, sensor and boss `BOUNDS`
    props as scratch), style (blank lines in `RodentMan`, 2 unused imports), notes (`OwnedArray.add` keeps old values;
    `--poolChecks` memory growth; guard test limits; `DarknessV4` old bugs). All fixed or logged in `docs/TODO.md`;
    `CaveRock.trajectory` (never read) removed. Engine changes confirmed equal in results.
  - Play-check review: 12 of 16 checks OK. Fixed: the `Saw` check moved to `TIMBER_WOMAN room8` (the
    `REFACTOR_TEST_084` saws are rotation saws), `TIMBER_WOMAN room13` needs `--difficulty hard` for its `Bat`s,
    the checkpoint check now uses `WILY_STAGE_3 room5`, `--debugShapes` added to the `ElecDevil` and `Popoheli` checks,
    the `RatRobot` text names one robot and the `Bat` in `RODENT_MAN room1`.
- Brief re-rendered and republished to the same link (the `TIMBER_WOMAN` command).
- Automated checks after the review fixes: `engine:test` 699 passed (no engine change since the last run),
  `core:test` 2,149 passed, `core:smokeTest` 468 passed, `lwjgl3:jar` built.

## 2026-11-16 (Diffity round 1)

- Owner comment on `SpikeBall.kt` (`getCenter(Vector2())` in `onSpawn`): a `new` per spawn defeats the pool. Owner
  choice: this branch fixes the rule and refactors `SpikeBall` as the model; the sweep of the other sites is `045b`
  (branch folder on `master`, `f1ee439e0`, blocked on this branch).
- `docs/CODE_STYLE.md`: the table row "once per spawn, event or level load: an owned field, or a plain `new`" is
  split; a pooled entity makes its objects once (new rule with the `SpikeBall` example). A plain `new` stays only
  outside pooled entities (level load, screen build).
- `SpikeBall`: one anchor field, one `Pendulum` and one `MotionDefinition` made in `init()`; chain sprites in an owned
  list that grows to the longest chain and hides the extra links; `onDestroy` no longer clears them. Same swing and
  chain as before. `core:test` 2149 and `core:smokeTest` 468 pass (no JAR during the review round). Uncommitted until
  the round ends.
- No live level could reach a `SpikeBall` by `--room` (only `Test1.tmx` `room11`, with no player spawn; my first
  command named `TIMBER_WOMAN room8`, which has `Saw`s, not `SpikeBall`s; the owner caught it). Owner choice: a test
  room and a unit test. A subagent built `assets/tiled_maps/tmx/RefactorTest045.tmx` with the room `spike_ball` (2
  `SpikeBall`s, chain 3 and 4, `spawn_room` spawners, which spawn on `PLAYER_READY`) and the `LevelDefinition` entry
  `REFACTOR_TEST_045` (added to `LevelCompletionScreensTest`). New `SpikeBallTest`: spawn, destroy, spawn again with a
  shorter and a longer chain; the same pendulum, anchor and chain sprites are reused, and extra links are hidden. It
  fails with the hide rule removed. `core:test` 2150 and `core:smokeTest` 468 pass. New contract check
  `REFACTOR_TEST_045 spike_ball`.
- Owner play test of `REFACTOR_TEST_045 spike_ball`: `ClassCastException` at `SpikeBall.onSpawn` (`LENGTH` read as
  `Float`). An old fault, also on `master`: both maps that place a `SpikeBall` (`Test1.tmx` `room11`, and the new map,
  which copied it) gave `length` as `type="int"`, so no `SpikeBall` could spawn (no play run reached `Test1` `room11`).
  Fixed in the data: `type="float"` in both maps, as `Saw` reads it. The review of the new room checked the spawn
  trigger but not the property types.
- Diffity round 1 result (2026-11-16): 0 anchored threads; 1 chat comment (the `SpikeBall` per-spawn `new`), fixed as
  above, with the sweep moved to `045b`. Owner play test: `REFACTOR_TEST_045 spike_ball`, `PRECIOUS_WOMAN room4`,
  `RODENT_MAN room1` and `REFACTOR_TEST_029 enemy_aim` pass (the owner ran these 4 of the contract checks). Merge
  approved. `engine:test` 699, `core:test` 2150, `core:smokeTest` 468, `lwjgl3:jar` pass.
