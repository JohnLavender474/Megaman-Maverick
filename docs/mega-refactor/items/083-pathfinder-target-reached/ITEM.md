# 083: `PathfinderResult.targetReached` means what it says

- Status: idea
- Type: refactor
- Depends on: none
- Related: `016`
- Branches: none yet

## Goal

The engine `Pathfinder.call()` returns `targetReached = false` when it finds a path to the target, and `true` only when
the start is the target or the target is farther than the max distance. The name says the opposite of what the value
means in the common case. No production code reads the flag today, so there is no gameplay effect. Rename it to what it
means, fix the value, or remove it (a field only with a caller).

## Proposed scope

Not approved. Check the engine `Pathfinder` and `PathfinderResult`, the tests, and any reader; choose rename, fix or
remove.

## Owner's input

## Open questions

## Sources

- `016d` branch `docs/TODO.md` (backlog candidate, 2026-10-29); found while writing `MegaPathfinderFactoryTest`.
