# Notes: 105 Fail a test task that runs 0 tests

Found on 2026-11-01 by the final review of `mega-refactor-004a-entity-smoke-test`, on `master` at `b8afd2632`.

Measured: with `useJUnit()` in place of `useJUnitJupiter()`, `core:smokeTest` ran 0 tests, wrote 0 XML files and gave
`BUILD SUCCESSFUL` on Gradle 8.10.1. The class-count floor in `EntityInitSmokeTest` does not run in that case.
