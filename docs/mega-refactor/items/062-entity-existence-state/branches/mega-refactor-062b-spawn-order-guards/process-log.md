# Process log: mega-refactor-062b-spawn-order-guards

## Status

- State: active
- Goal: engine guards on the order of spawn and destroy calls (the last call wins; no second `onSpawn` or `onDestroy`)
- Done: branch start
- Next: scan (the callers of the odd paths), then `docs/SCOPE.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master` at `74b8c5a5c`. Worktree `.worktrees/mega-refactor-062b-spawn-order-guards`.
