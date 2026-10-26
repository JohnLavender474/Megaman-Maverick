# Process log: mega-refactor-061b-play-phase-teleport

## Status

- State: active
- Goal: `ready` and `teleporting` become entity state (`PlayPhaseComponent`, `TeleportableComponent`) read by free functions; no player wrappers
- Done: planning (scope, contract, plan approved); step 1
- Next: step 2 (baseline)
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
  branch name change from `061b-lifecycle-transport` to `061b-play-phase-teleport` (no branch existed yet).
- The owner approved `docs/SCOPE.md` with the package `C/entities/state/` ("Yes"). Draft `docs/CONTRACT.md` written;
  the teleporter rooms come from a scan of the live maps: `MOON_MAN` `room11` (`PortalHopper`), `WILY_STAGE_3`
  `room3` (`CapsuleTeleporter`), `PRECIOUS_WOMAN` `room2` (`LaserBeamer`), `INTRO_STAGE` `room3` (`BuilderKibbo`).
- The owner agreed that reads without the component give the default and writes require it; `docs/SCOPE.md` gets a
  paragraph on it (a clarification, not a scope change), and the transport writes require the component too. The
  owner approved `docs/CONTRACT.md` (its transport check now includes "`beginTeleport` without the component fails").
  Draft `docs/PLAN.md` written (6 steps).
- The owner chose the name `TeleportableComponent` (not `TransportComponent`); the docs follow, the approvals stay
  (a name only). The folder and branch name change to `061b-play-phase-teleport` (no branch existed yet). The owner
  approved `docs/PLAN.md`. Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree created.
