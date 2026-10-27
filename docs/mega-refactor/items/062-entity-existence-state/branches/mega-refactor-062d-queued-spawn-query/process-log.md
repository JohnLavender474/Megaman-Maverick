# Process log: mega-refactor-062d-queued-spawn-query

## Status

- State: active
- Goal: replace `engine.contains(..., containedIfQueuedToSpawn)` and the pool return of a rejected spawn with the existence state
- Done: planning (scope, contract, plan approved); steps 1 to 5
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master`. Worktree `.worktrees/mega-refactor-062d-queued-spawn-query`.
- Scan: `engine.contains(..., containedIfQueuedToSpawn)` has one production caller, the pool return of a rejected
  spawn (`MegamanMaverickGame.kt:927-933`); one engine test uses `engine.contains(e)`. `UNSPAWNED` disagrees with
  "the engine has no claim" in one case: a spawn queued, then a destroy before the update (`destroy()`, and
  `destroyNow` with both queues); no `core` reader compares with `UNSPAWNED`. Draft `docs/SCOPE.md` with 3 open
  questions (a sixth state or a named queue query; its name; `contains` internal or removed).
- Owner: option (b), a named engine query (`hasClaimOn`), not a sixth state. `docs/SCOPE.md` updated; open questions
  3 (`contains` internal or removed) and 4 (the name) remain.
- Owner (2026-10-27, "Yes"): `contains` becomes `internal`; the query is `hasClaimOn`. Scope approved. Draft
  `docs/CONTRACT.md`: a temporary suite comparing `hasClaimOn` with the old condition before every call on random
  sequences, permanent per-path tests, and three short manual checks in rooms with `hard_mode_only` objects (rejected
  spawns on normal difficulty; found in the live TMX files).
- Contract approved (owner, 2026-10-27: "yes"). Draft `docs/PLAN.md` (5 steps).
- Plan approved (owner, 2026-10-27: "yes").
- Step 1 (baseline): clean build passes; 540 engine and 172 core tests, 0 failures; 110 unique warnings (scratchpad
  `062d-warnings-baseline.txt`).
- Step 2: `GameEngine.hasClaimOn`; six permanent tests in `EntityExistenceStateTest` ("hasClaimOn"); the temporary
  `TempHasClaimOnContractTest`: 10,000 sequences, 2,329,776 checks before every call (also inside `onSpawn` and
  `onDestroy`), all equal to the old condition; deciding parts: live now only 47,804, spawn queue only 159,072, entity
  set only 57,658; rejected spawns 34,365. 547 engine tests, 0 failures.
- Step 3: the pool return calls `!entity.engine.hasClaimOn(entity)`. All tests pass.
- Step 4: `GameEngine.contains(entity)` without the parameter, `internal`; the temporary suite deleted. Greps: no
  `containedIfQueuedToSpawn`; `hasClaimOn` has one production caller.
- Step 5 (automated): clean build passes; 546 engine and 172 core tests, 0 failures; the same 110 warnings; jar
  builds; `contact-rule-selection.txt` unchanged.
- Step 5: owner play test (2026-10-27): "All tests pass".
