# 048: Replace entity inheritance with components

- Status: planned
- Type: refactor
- Depends on: none
- Related: `017`, `018`, `023`, `029`, `030`
- Branches: none yet

## Goal

Replace the `AbstractHealthEntity` / `AbstractEnemy` / `AbstractBoss` layers and the type checks on them with components (for example a health component).

## Owner's input

From the first request (summary): entities get health by extending `AbstractHealthEntity`; enemies must extend `AbstractEnemy` and bosses `AbstractBoss`; logic does type checks on these classes. Replace the layers with components and remove the type checks.

## Decisions

- (2026-10-22) Entity roles come from components, not from base classes or layers. An entity that is both an enemy and a block has both parts (a solid block fixture, health and damage), with no child entity, extra interface or flag swapping. Code that checks `EntityType` or `is AbstractEnemy` checks a capability instead.

## Open questions

None yet. The scan for the first branch will find them.

## Sources

- `NOTES.md` in this folder.
