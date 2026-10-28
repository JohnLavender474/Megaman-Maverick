# 071: `getEntity()` without a cast to `MegaGameEntity`

- Status: idea
- Type: refactor
- Depends on: none
- Related: `063`, `064`
- Branches: none yet

## Goal

`IFixture.getEntity()` and `IBody.getEntity()` (`core/.../world/body/IFixtureExtensions.kt:17`,
`IBodyExtensions.kt:65`) cast the stored entity to `MegaGameEntity`. Callers could use an entity type
(`IMegaGameEntity` or a contract type) with no hidden cast, or the cast could move to the few places that need it.

## Proposed scope

Not approved. Scan the callers of both functions and the members they read.

## Owner's input

## Open questions

## Sources

- `064b` branch `docs/TODO.md` (backlog candidate, 2026-10-28).
