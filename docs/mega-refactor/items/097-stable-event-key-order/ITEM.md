# 097: Stable order for event keys in `EventsManager`

- Status: idea
- Type: refactor
- Objectives: `O2`, `O1`
- Depends on: none
- Related: `053`, `057`
- Branches: none yet

## Goal

Each listener reads its events through `eventKeyMask`, a hash-ordered `ObjectSet` (`EventsManager.kt:72`). Two event keys in one frame can arrive in a different order on each run. Make the order stable, so that `O2-R4` and `O1-R3` hold here.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Find the listeners that get two keys in one frame, and check if the order matters today.

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
