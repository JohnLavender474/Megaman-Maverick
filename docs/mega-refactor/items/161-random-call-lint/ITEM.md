# 161: A type-aware check for direct random calls

- Status: done
- Type: tooling
- Objectives: `O6`, `O2`
- Depends on: `011`
- Related: `100`
- Branches: `mega-refactor-161a-random-call-lint` (merged 2026-11-17)

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

- Answered (2026-11-17): detekt is in the build. Measured in `161a`: `core:detektMain` 1m27s, `engine:detektMain` 32s
  (21s on a second run), `lwjgl3:detektMain` under 1 s; the tasks are up to date when sources, classpath and config do
  not change. No baseline: only one rule is on, with 0 findings.
- The text-scan guards of `045a` and `045b` (`PoolOwnershipGuardTest`, `SpawnAllocationGuardTest`) and the `O2-R1`
  search move to item `164`.

## Decisions

- (2026-11-17, owner) `161a` merged after the diff review; its decisions `D1` to `D5` accepted: detekt `2.0.0-alpha.3`, `RandomSourceGuardTest` deleted, a fixture check (`core:verifyRandomRule`), main sources only, `SeededRandom(...)` forbidden outside the random package.

- (2026-11-16) Merge target: each branch of this item squash-merges directly into `master`. The owner asked for the work end to end by a subagent; Claude chose the recommended target, and the owner can change it.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/011-game-random/branches/mega-refactor-011a-game-random/docs/TODO.md`
