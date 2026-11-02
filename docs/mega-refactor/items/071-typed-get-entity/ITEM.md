# 071: `getEntity()` without a cast to `MegaGameEntity`

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `063`, `064`, `072`
- Branches: none yet

## Goal

`IFixture.getEntity()` and `IBody.getEntity()` (`core/.../world/body/IFixtureExtensions.kt:17`,
`IBodyExtensions.kt:65`) cast the stored entity to `MegaGameEntity`. Callers could use a contract type with no hidden
cast, or the cast could move to the few places that need it. `IMegaGameEntity` is not an option, because `072` deletes
it.

## Proposed scope

Not approved. Scan the callers of both functions and the members they read.

## Owner's input

## Open questions

## Sources

- `064b` branch `docs/TODO.md` (backlog candidate, 2026-10-28).
