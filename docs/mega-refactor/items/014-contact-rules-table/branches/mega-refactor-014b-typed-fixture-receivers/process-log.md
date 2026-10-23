# Process log: mega-refactor-014b-typed-fixture-receivers

## Status

- State: merged
- Goal: One typed receiver registration in place of the hand-written extension trios
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- No branch exists yet. The folder was prepared on `master`.
- Scan done. Draft `docs/SCOPE.md` written for the owner to review.
- Scope approved by the owner (function types, migrate all now, key in the engine). Draft `docs/CONTRACT.md` written.
- Contract accepted: the owner asked for the plan. Draft `docs/PLAN.md` written.
- Plan approved by the owner. Step 1: branch start on `master`, worktree created.
- Step 2: baseline: 427 engine and 77 core tests, 133 compiler warnings.
- Step 3: `PropertyKey`, `PhasedPropertyKey` and typed `IPropertizable` accessors in the engine, with 9 tests
  (436 engine tests). No overload was ambiguous, so the typed accessors keep the names `putProperty`,
  `getProperty`, `hasProperty`, `removeProperty`.
- Step 4: `Receivers.kt` (17 keys, function types with named parameters) and the four `setReceiver`/`receiver`
  functions. The old receiver functions switched to the keys internally. Build, tests and a 25-second
  `TEST_1` launch pass.
- Step 5: the 96 reads in the listener and the filter. Deviation from "by hand": the 37 plain
  `if (x.hasK(...)) x.getK(...)` pairs were rewritten by a script (a repeated pattern), and every line of the
  diff was reviewed. The special cases were written one by one: velocityAlteration `!!`, stickToBlock
  `?: true`, the force alteration fallback to `VelocityAlterator.alterate`, the projectile exception
  `!= true`, the filter `== false`. A safe call `?.invoke(args)` evaluates `args` only when the receiver is
  present, as the old `if (has)` did. The two `hitByLaser` reads are gone.
- Step 6: 195 registration rewrites by script (calls, labelled returns, imports), with no unrelated import
  changes. By hand: `AbstractItem` computed its stick value at registration time, so the new lambda captures
  it in a local `val` instead of computing it on each call. A check of the original code found no nested
  registration lambdas, so no `return@setReceiver` label binds to a different lambda than before. 20 lines
  that grew past 120 characters were wrapped (`Cokeyro`'s line was already long and stays).
- Step 7: the old receiver functions, `hasConsumer`, the exception helpers and `hitByLaser` are deleted.
  `ConstKeys.HIT_BY_LASER` is now unused and stays, as the plan says for the `ConstKeys` values.
- Step 8 (automated): clean build passes; 436 engine and 77 core tests, 0 failures; 115 compiler warnings
  (133 before; the 18 removed are the old unchecked casts; no new warnings); the contract greps find no old
  receiver code (two false positives: a private function of the same name in `AsteroidSpawnersBlock`, and the
  display names of two keys in `Receivers.kt`); a 30-second `GLACIER_MAN` launch shows no exception.
- Step 8 (play test, partial): the owner played sections of `GLACIER_MAN`: conveyor belts, ice blocks, water and death fixtures work as expected.
- Step 8 (play test): the owner played the dark sections of `DESERT_MAN` (quicksand, carts): working as expected. The owner declared the branch finished.
- Step 9: the owner approved the merge (2026-10-22). `master` gained only docs commits since the branch start, so no merge of `master` into the branch.
