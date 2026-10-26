# Plan: mega-refactor-061b-play-phase-transport

How to do the work in `SCOPE.md`. Written ahead of time on `master`.

- Status: draft
- Approved: no

Build with `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from `build/test-results`. Smoke launches are
short (about 20 seconds); the timeout with no exception is a pass. Each step is its own commit; after each code step,
the build and the tests pass and the game plays the same.

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. New APIs and their first callers: see `SCOPE.md`.

## Steps

### 1. Branch start

- Changes: on `master`, docs only: the `061b` row to `active` with the start date; the `ITEM.md` branches line; the
  branch `process-log.md` to `active`. Then
  `git worktree add -b mega-refactor-061b-play-phase-transport .worktrees/mega-refactor-061b-play-phase-transport master`.
- Check: `git worktree list`.
- Done: no

### 2. Baseline

- Changes: none. A clean build in the worktree.
- Check: the test counts (expected 501 engine, 159 core) and 114 warnings, in the process log.
- Done: no

### 3. Play phase

- Changes:
  - `C/entities/state/PlayPhase.kt` (the enum), `PlayPhaseComponent.kt` (`reset()` keeps the value), and the free
    functions `playPhase`, `inPlay`, `setPlayPhase` on `IGameEntity`.
  - `Megaman` adds `PlayPhaseComponent`; `Megaman.ready` goes; the 26 internal reads become `inPlay` (the same
    negation), including the debug strings.
  - Writers: `PlayerSpawnEventHandler` (`ready = false` → `PRE_PLAY`, `ready = true` → `IN_PLAY`),
    `PlayerDeathEventHandler` and `EndLevelEventHandler` (`false` → `POST_PLAY`), through `player.entity`.
  - Outside readers: `player.ready` → `player.entity.inPlay` (and `targetPlayer.entity.inPlay`); `Rain` reads the
    entity it touches (`inPlay`), in place of `it.asPlayer?.ready ?: true`.
  - `MegaPlayerAccess.kt` loses `ready`; `PlayerStateComponent` loses `ready`; the dead `players.ready` imports in
    `SigmaRatClaw` and `BigAssMaverickRobotHand` go.
  - Tests: play phase defaults, set and read, the write without the component fails, `reset()` keeps the value;
    `MegaPlayersTest` loses its `ready` cases.
- Check: build and tests pass. A script lists every former `ready` read and write (file, line, negation, value) and
  the new line, and they match one for one. Short launches of `REFACTOR_TEST_029 --room movement` and
  `INTRO_STAGE --room room12` show no exception.
- Done: no

### 4. Transport

- Changes:
  - `C/entities/state/TransportComponent.kt` (`reset()` clears), the free functions `teleporter`, `teleporting`,
    `beginTeleport(entity, teleporter)` (calls the entity's `ON_TELEPORT_START` hook, then sets the teleporter) and
    `endTeleport(entity)` (calls the `ON_TELEPORT_END` hook, then clears it). Both require the component.
  - `PipePortal`, `PortalHopper`, `CapsuleTeleporter` call `beginTeleport`/`endTeleport` in place of reading the
    hooks; `PipePortal`'s two `TELEPORTER` writes set the component's teleporter (players only, as today).
  - `Megaman` adds `TransportComponent`; its hooks no longer set the flag or the property; `Megaman.teleporting`,
    `PlayerStateComponent` and `ConstKeys.TELEPORTER` go; `onSpawn`/`onDestroy` clear the teleporter where they
    removed the property (the component's `reset()` also clears it on destroy).
  - Readers: Megaman's 19 internal reads use the `teleporting` function; `SpritesComponent` and
    `MegamanAnimationKeySupplier` read `teleporter`; `PreciousGem` reads `(owner as? IGameEntity)?.teleporting`;
    `MegaPlayerAccess.kt` loses `teleporting`.
  - `AbstractEnemy` loses its two teleport hooks.
  - Tests: transport defaults; the order (the start hook sees `teleporting == false`, the end hook sees `true`);
    `reset()` clears; no hooks still marks; `beginTeleport` without the component fails.
- Check: build and tests pass; the one-for-one list for the `teleporting` reads; greps (no `ConstKeys.TELEPORTER`,
  no teleporter reads the hooks, no `PlayerStateComponent`). Short launches of `REFACTOR_TEST_031 --room pipe_portal`,
  `MOON_MAN --room room11` and `WILY_STAGE_3 --room room3` show no exception.
- Done: no

### 5. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build; results in the process log; then the manual
  checks for the owner.
- Done: no

### 6. Owner's play test and merge

- Check: the owner plays the manual checks and approves the merge; then "Branch merge" in
  `docs/mega-refactor/CLAUDE.md`.
- Done: no
