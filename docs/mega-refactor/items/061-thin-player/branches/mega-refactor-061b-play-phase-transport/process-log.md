# Process log: mega-refactor-061b-play-phase-transport

## Status

- State: planned
- Goal: `ready` and `teleporting` become entity state (`PlayPhaseComponent`, `TransportComponent`) read by free functions; no player wrappers
- Done: `docs/SCOPE.md` approved; draft `docs/CONTRACT.md`
- Next: the owner approves `docs/CONTRACT.md`; then the draft `docs/PLAN.md`
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
- The owner approved `docs/SCOPE.md` with the package `C/entities/state/` ("Yes"). Draft `docs/CONTRACT.md` written;
  the teleporter rooms come from a scan of the live maps: `MOON_MAN` `room11` (`PortalHopper`), `WILY_STAGE_3`
  `room3` (`CapsuleTeleporter`), `PRECIOUS_WOMAN` `room2` (`LaserBeamer`), `INTRO_STAGE` `room3` (`BuilderKibbo`).
