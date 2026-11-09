# 142: Remove the reflection from `FriendlyFireTest` and `EntityExistenceTest`

- Status: idea
- Type: test
- Objectives: `O6`
- Depends on: none
- Related: `047`, `062`, `081`
- Branches: none yet

## Goal

Tests must drive the real flow; a reflection shortcut to private state is a code smell (owner, 2026-11-09). `081e`
removed the reflection from the X tests. Two older tests still use it:

- `FriendlyFireTest` (`047b`): `getDeclaredMethod("getDamageTimer")` on `AbstractHealthEntity`
  (`FriendlyFireTest.kt:158`).
- `EntityExistenceTest` (`062a`): `getDeclaredField("shieldEntity")` on `FreezableEntityHandler`
  (`EntityExistenceTest.kt:50`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Change each test to drive the public flow, as the `081e` X tests do, with no new production API for the test.
- Each test still fails when the behavior that it checks breaks (a mutation check for each).

## Owner's input

Found in `081e` (2026-11-09) by a read-only review of the reflection in the tests. The owner kept these 2 tests out of
the `081e` scope. A backlog candidate in the `081e` branch `docs/TODO.md`.

## Open questions

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
