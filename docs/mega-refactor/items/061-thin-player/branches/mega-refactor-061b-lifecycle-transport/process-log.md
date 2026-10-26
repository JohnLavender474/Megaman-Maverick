# Process log: mega-refactor-061b-lifecycle-transport

## Status

- State: planned
- Goal: `ready` and `teleporting` become entity state (`LifecycleComponent`, `TransportComponent`) read by free functions; no player wrappers
- Done: draft `docs/SCOPE.md`
- Next: the owner reviews the draft scope
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `447ae2b54`).
- Scan: item `061` `NOTES.md`, "`ready` and `teleporting` for any entity", checked again on `master`: every reader and
  writer of the player's `ready` and `teleporting`, the `ConstKeys.TELEPORTER` property, the `ON_TELEPORT_START`/`END`
  hooks and their callers, `AbstractEnemy`'s teleport hooks. Draft `docs/SCOPE.md` written.
