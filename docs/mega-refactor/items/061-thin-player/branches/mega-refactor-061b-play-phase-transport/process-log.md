# Process log: mega-refactor-061b-play-phase-transport

## Status

- State: planned
- Goal: `ready` and `teleporting` become entity state (`PlayPhaseComponent`, `TransportComponent`) read by free functions; no player wrappers
- Done: draft `docs/SCOPE.md`
- Next: the owner approves `docs/SCOPE.md` (1 open question: the package)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `447ae2b54`).
- Scan: item `061` `NOTES.md`, "`ready` and `teleporting` for any entity", checked again on `master`: every reader and
  writer of the player's `ready` and `teleporting`, the `ConstKeys.TELEPORTER` property, the `ON_TELEPORT_START`/`END`
  hooks and their callers, `AbstractEnemy`'s teleport hooks. Draft `docs/SCOPE.md` written.
- The owner asked if `Lifecycle` can be confused with spawning, spawned and dead; a review (item `NOTES.md`,
  "'Lifecycle' versus spawning, spawned and dead") found three separate axes. The owner chose the rename to
  `PlayPhase { PRE_PLAY, IN_PLAY, POST_PLAY }` with `inPlay` as a pure phase check. `docs/SCOPE.md` updated (names,
  a "Three axes" section, the timing facts, the boss note for `048`, open question 1 closed). The folder and the
  branch name change from `061b-lifecycle-transport` to `061b-play-phase-transport` (no branch existed yet).
