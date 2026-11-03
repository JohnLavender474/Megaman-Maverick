# Process log: mega-refactor-019c-merge-spawn-queues

## Status

- State: active
- Goal: merge the four spawn queues (`spawns`, the engine queue, `playerSpawnsMan`, `SPAWN_NOW`) into one spawn phase
- Done: branch start
- Next: scan, then the draft `docs/SCOPE.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-03

- Branch start on `master`. Merge target: `master` directly, as for `019a` and `019b`.
- Decision on `master` before the start (item `019`, 2026-11-03): a destroyed entity respawns in the next tick at the
  earliest. This branch must not add a spawner pass after `engine.update`.
