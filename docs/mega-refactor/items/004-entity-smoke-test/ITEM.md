# 004: Entity build-and-init smoke test

- Status: done
- Type: test
- Objectives: `O6`
- Depends on: `002`
- Related: `005`, `006`, `021`, `028`, `039`, `105`, `106`, `107`
- Branches: `mega-refactor-004a-entity-smoke-test` (merged 2026-11-01)

## Goal

Build and `init()` every entity class with real atlas data in a headless test, so that a missing region or a bad constructor fails in CI.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A separate `core` JVM test suite `smokeTest` (`core/src/smokeTest/kotlin`, package `...smoketests`) with the headless backend and desktop natives. A hyphen is not legal in a package name, so `smoke-tests` cannot be used.
- A shared headless harness with real atlas region data on stub textures. Items `005` and `006` reuse it.
- One test per concrete entity class: pool fetch plus `init()` with no exception.
- A known-failures allowlist that may only shrink.

## Owner's input

Answer to review item 3 (2026-10-22, verbatim):

> Sure, can this be part of a new `src/test/.../smoke-tests/` folder? Is it appropriate to run in a unit-test environment or an e2e environment?

## Decisions

- B (2026-10-22): use a separate `smokeTest` JVM test suite in `core`.
- Merge target (2026-11-01): each branch squash-merges directly into `master`, with no parent branch.
- Real textures (2026-11-01): the suite decodes the real PNGs and has no stub code. The measured load time was the same as with stubs, and a missing PNG now fails.
- Silent misses (2026-11-01): a `findRegion` miss fails the test, also when `init()` does not throw. A miss through `containsRegion` does not count.
- Known failures (2026-11-01): the 18 classes that failed went on `known-entity-failures.txt`, with bugs filed. The branch fixed none of them. Retired classes are on the same list, so no separate list is needed.

## Open questions

None. The two open questions closed with the decisions above.

## Sources

- `NOTES.md` in this folder.
