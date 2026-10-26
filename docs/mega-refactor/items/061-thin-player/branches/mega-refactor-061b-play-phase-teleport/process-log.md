# Process log: mega-refactor-061b-play-phase-teleport

## Status

- State: merged
- Goal: `ready` and `teleporting` become entity state (`PlayPhaseComponent`, `TeleportableComponent`) read by free functions; no player wrappers
- Done: all steps. Merged into `master`.
- Next: none. Merged.
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
- Step 2 (baseline, worktree on `3fc3d7056`): clean build passes; 501 engine and 159 core tests, 0 failures; 114
  warnings.
- Step 3: `core/.../entities/state/`: `PlayPhase`, `PlayPhaseComponent` (`reset()` keeps the value), `PlayPhases.kt`
  (`playPhase` with `IN_PLAY` without the component, `inPlay`, `setPlayPhase` which requires the component).
  `Megaman` adds the component; `Megaman.ready` goes; 22 lines in `entities/megaman/` read `inPlay` (same negation; the
  debug strings say `inPlay=$inPlay`; the log text "megaman is not ready" stays). Writers: `PlayerSpawnEventHandler`
  (`PRE_PLAY`, `IN_PLAY`), `PlayerDeathEventHandler` and `EndLevelEventHandler` (`POST_PLAY`). Outside readers read
  `player.entity.inPlay` / `targetPlayer.entity.inPlay` (`FeetSandRule`, `DarknessV2`, `DarknessV4`, `BuilderKibbo`,
  `MegaLevelScreen` checkpoint text and pause, `BulletBillLauncher`, `LaserBeamer`, `TubeBeamer`); `Rain` reads
  `it.inPlay` (a non-player has no component and reads in play, as `?: true` gave). `MegaPlayer.ready` and
  `PlayerStateComponent.ready` go; the dead `players.ready` imports in `SigmaRatClaw` and `BigAssMaverickRobotHand`
  go. Imports: the compiler-reported ones inserted in sorted order (scratchpad `add_imports2.py`), the in-place ones
  moved to their sorted place. Tests: `PlayPhasesTest` (4); `MegaPlayersTest` and `MegamanStunTargetTest` lose their
  `ready` cases. One-for-one check (scratchpad script over `git diff master`): 35 code lines match after undoing the
  renames; the rest are imports, one comment, and the planned `Rain` line. Build passes; 501 engine and 162 core
  tests, 0 failures. Launches of `REFACTOR_TEST_029 --room movement` and `INTRO_STAGE --room room12` ran with no
  exception until the timeout.
- Step 4: `entities/state/TeleportableComponent.kt` (`teleporter`, `reset()` clears it) and `Teleports.kt`
  (`teleporter`, `teleporting`, `beginTeleport` and `endTeleport`, which require the component, call the entity's
  start or end hook first and then set or clear the teleporter). `PipePortal`, `PortalHopper` and `CapsuleTeleporter`
  call `beginTeleport`/`endTeleport`; `PipePortal`'s entry write is now `beginTeleport`'s and its exit write sets the
  component (players only, as before). `Megaman` adds `TeleportableComponent`; its hooks lose the flag and property
  writes (the start hook's unused parameter is `_`); `onSpawn` clears the teleporter where it reset the flag and
  removed the property; `onDestroy` loses its property removal (the component's `reset()` clears it before
  `onDestroy`). `Megaman.teleporting`, `MegaPlayer.teleporting`, `PlayerStateComponent` and `ConstKeys.TELEPORTER` go;
  `SpritesComponent` and `MegamanAnimationKeySupplier` read `teleporter`; `PreciousGem` reads `owner?.teleporting`
  (its `owner` is already an `IGameEntity`). Megaman's 19 internal `teleporting` reads keep their text and now resolve
  to the free function. `AbstractEnemy` loses its two teleport hooks. Plan detail (no approval needed):
  `setStandardOnTeleportStartProp` and `setStandardOnTeleportEndProp` in `GameEntityUtils` lose their only caller and
  go (`standardOnTeleportStart`/`End` and the continue helper stay). Tests: `TeleportsTest` (5: defaults, the hook
  order, no hooks still marks, no component fails, `reset()` clears); `MegaPlayersTest`, `MegamanStunTargetTest` and
  `PlayerItemRuleTest` lose `playerState`. Clean build passes; 501 engine and 167 core tests, 0 failures; 110
  warnings (114 on `master`: the six unchecked hook casts in the three teleporters become two in `Teleports.kt`).
  Greps: no `ConstKeys.TELEPORTER`, `PlayerStateComponent`, hook cast outside `Teleports.kt`, `players.ready` or the
  two removed helpers. Launches of `REFACTOR_TEST_031 --room pipe_portal`, `MOON_MAN --room room11` and
  `WILY_STAGE_3 --room room3` ran with no exception until the timeout.
- Step 5, automated contract checks (clean build on `62837deaa`): build passes; 501 engine and 167 core tests, 0
  failures (`master`: 501 and 159); 110 warnings (fewer than the 114 on `master`); `lwjgl3:jar` builds;
  `contact-rule-selection.txt` unchanged; `PlayPhasesTest` (4) and `TeleportsTest` (5) pass; greps: no
  `PlayerStateComponent`, `ConstKeys.TELEPORTER`, `MegaPlayer.ready`/`teleporting`, `Megaman.ready`/`teleporting`
  property, no teleporter reads the hooks, no `players.ready` import, no teleport hooks in `AbstractEnemy`; one for one:
  the 35 `ready` lines (step 3) and the `teleporting` reads (unchanged text in Megaman; `teleporter` in place of the
  `TELEPORTER` property in the two sprite readers; `owner?.teleporting` in `PreciousGem`). Waiting for the owner's
  play test.
- Step 6, play test: the owner played the manual checks in `docs/CONTRACT.md` and approved the merge ("Play tests
  pass, approved").
