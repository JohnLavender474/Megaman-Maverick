# 004: Entity build-and-init smoke test

- Status: planned
- Type: test
- Depends on: `002`
- Related: `005`, `006`, `021`, `028`, `039`
- Branches: none yet

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

## Open questions

- Stub textures (fast, checks names) or real PNG decode (slower)?
- Which entity classes are known to be retired? They go on the allowlist (see `039`).

## Sources

- `NOTES.md` in this folder.
