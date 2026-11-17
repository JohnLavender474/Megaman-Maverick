# 161: A type-aware check for direct random calls

- Status: active
- Type: tooling
- Objectives: `O6`, `O2`
- Depends on: `011`
- Related: `100`
- Branches: `mega-refactor-161a-random-call-lint` (active)

## Goal

Keep rule `O2-R2` (random values only from the seeded source) true with a check that sees types, not only text.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `RandomSourceGuardTest` (`core/src/test`, from `011a`) scans the source text. It does not catch a call with no
  receiver (`shuffle()` or `random()` inside `with(array)` or in an `Array` subclass) or a call with an argument
  (`list.shuffle(rng)`).
- Option: a detekt `ForbiddenMethodCall` rule (with type resolution) for `MathUtils.random*`, `kotlin.random.Random`,
  `java.util.Random` and the collection `random()`/`shuffle()`/`shuffled()` functions, allowed only in
  `engine/.../common/random/`. Then the source-scan test can go, or stay as a fast first check.

## Owner's input

- 2026-11-14 (`011a` `D4`): the owner chose the core source-scan test for `011a`; the review before the play test
  named detekt as the type-aware alternative. Backlog candidate from `011a` `docs/TODO.md`.

## Open questions

- From `045b` (2026-11-17): `SpawnAllocationGuardTest` (constructor calls in `onSpawn`, `onDestroy` and `onEvent`, with a baseline file) is a text scan too; a `TODO` at the test moves it to this detekt setup.

- From `045a` (2026-11-16): `PoolOwnershipGuardTest` (a one-line text scan for pooled default parameters) has the same gap as the old random scan: a default split over 2 lines, or one that calls a scratch helper, passes. The detekt setup of this item could cover it.

- Does the project want detekt in the build (CI time, a baseline for the existing code)?

## Decisions

- (2026-11-16) Merge target: each branch of this item squash-merges directly into `master`. The owner asked for the work end to end by a subagent; Claude chose the recommended target, and the owner can change it.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/011-game-random/branches/mega-refactor-011a-game-random/docs/TODO.md`
