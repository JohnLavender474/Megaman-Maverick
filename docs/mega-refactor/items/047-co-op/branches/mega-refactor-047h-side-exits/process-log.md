# Process log: mega-refactor-047h-side-exits

## Status

- State: active
- Goal: shared screen phase 2: with 2 players, a side room exit holds every player until all live players are at it, then the transition carries them
- Done:
- Next: the scan, `docs/SCOPE.md`, `docs/CONTRACT.md` and the brief
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch started from `master`. Cause found at the `047g` route play test: in `coop_start`, slot 1 ran off the screen into `coop_wide`, because the view edge rules skip a view edge at a room edge and the transition reads only slot 0. Owner's decision: "gather at the exit" (see the item `ITEM.md`, "Decisions", 2026-11-06).
