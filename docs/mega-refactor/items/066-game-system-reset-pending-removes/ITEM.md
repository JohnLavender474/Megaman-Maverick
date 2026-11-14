# 066: `GameSystem.reset()` clears pending removes

- Status: active
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `064`
- Branches: `fix/engine-small-bugs-2` (short contract in `docs/issues/2026-10-22-cullables-system-early-return.md`)

## Goal

`GameSystem.reset()` clears `entities` and `entitiesToAdd`, but not `entitiesToRemove`. A remove that is deferred
during a system's own `update`, then `engine.reset()`, then a new spawn of the same entity, would drop the live entity
from that system at its next `update`. No code reaches this today: nothing adds or removes a member during the system's
own `update`.

## Proposed scope

Not approved. `reset()` also clears `entitiesToRemove`, with an engine test. Small enough to go with another engine
branch.

## Owner's input

## Open questions

## Sources

- `064a` branch `docs/TODO.md` (backlog candidate, 2026-10-27).
