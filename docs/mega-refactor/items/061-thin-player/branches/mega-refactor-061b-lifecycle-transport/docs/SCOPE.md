# Scope: mega-refactor-061b-lifecycle-transport

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and `PLAN.md`. The user
approves it. If the scope changes later, update this file and get approval again.

- Status: draft
- Approved: no

## Goal

`ready` and `teleporting` stop being player state. They become entity state that any entity can have: a lifecycle
phase and a transport state, each a small component read by free functions on `IGameEntity`. Callers ask the entity,
not a player view. The game plays the same.

## Item

`061`. Decisions of 2026-10-26 in `ITEM.md`: `LifecycleComponent` with `Lifecycle { ENTERING, ACTIVE, LEAVING }` (an
entity without it reads as `ACTIVE`); `TransportComponent` holding the teleporter (`teleporting` means a teleporter
is set); no player wrappers for entity questions. Research: item `NOTES.md`, "`ready` and `teleporting` for any
entity".

## Current state

Based on `master` at `447ae2b54`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

- `C/players/PlayerStateComponent.kt` holds `ready` and `teleporting`. It has no `reset()`, so both keep their value
  across a destroy until `Megaman.onSpawn` or the next spawn sequence.
- **`ready`** (the player's "in play"):
  - Writers: `PlayerSpawnEventHandler` (`false` at the start of the sequence, `true` at the end of the beam),
    `PlayerDeathEventHandler` (`false`), `EndLevelEventHandler` (`false` at the beam-out). All through
    `MegaPlayer.ready` (`C/players/MegaPlayerAccess.kt`).
  - Readers inside Megaman (26 lines in `C/entities/megaman/`) through `Megaman.ready`: `invincible`, `running`,
    `spawningTimer`, the body, the controller, the behaviors, the sprites, the weapons.
  - Readers outside (through `MegaPlayer.ready`): `MegaContactRules` (`FeetSandRule`), `Rain` (`?: true` for
    non-players), `DarknessV2`, `DarknessV4`, `BuilderKibbo`, `TubeBeamer`, `LaserBeamer`, `BulletBillLauncher`,
    `MegaLevelScreen` (checkpoint text, pause).
  - Dead imports of `players.ready` in `SigmaRatClaw` and `BigAssMaverickRobotHand` (they read the boss field).
- **`teleporting`:**
  - Writers: Megaman's `ON_TELEPORT_START` and `ON_TELEPORT_END` hooks (`C/entities/megaman/Megaman.kt:501-518`) set
    the flag and the `ConstKeys.TELEPORTER` property; `PipePortal` writes the property again when it pulls the entity
    in (right after the start hook) and when it pushes it out (`PipePortal.kt:124,148`); `Megaman.onSpawn` resets the
    flag and removes the property; `onDestroy` removes the property.
  - Readers inside Megaman (19 lines): the controller, jump, air dash (`AirDashBehavior.kt:117` reads it in the
    behavior's `end`), jetpack; `SpritesComponent.kt:100` and `MegamanAnimationKeySupplier.kt:42-43` read the flag and
    the teleporter.
  - Reader outside: `PreciousGem.kt:395` (`owner.asPlayer?.teleporting`).
  - The three teleporters (`PipePortal`, `PortalHopper`, `CapsuleTeleporter`) read the start and end hooks from the
    entity's properties and call them. Only Megaman has a `TELEPORTER_LISTENER` fixture, so only he teleports.
    `AbstractEnemy` sets the standard hooks (`AbstractEnemy.kt:165-166`), but no enemy can reach a teleporter.
- **Order to keep:** Megaman's start hook calls `resetBehavior(AIR_DASHING)` before it sets `teleporting = true`, and
  the air dash's `end` reads `teleporting`. So the flag must turn on after the start hook's earlier steps, and off
  after the end hook's earlier steps.

## In scope

1. **Lifecycle** (core, a new package, for example `C/entities/state/`): `enum class Lifecycle { ENTERING, ACTIVE,
   LEAVING }`; `LifecycleComponent(var phase = ENTERING)` with a `reset()` that keeps the value (as today); free
   functions `IGameEntity.lifecycle` (`ACTIVE` without the component), `IGameEntity.inPlay` (`lifecycle == ACTIVE`;
   name: see "Open questions") and `IGameEntity.setLifecycle(phase)`.
   - Writers: the spawn sequence start → `ENTERING`, the end of the beam → `ACTIVE`, death → `LEAVING`, the level end
     → `LEAVING`.
   - Readers: every `ready` reader reads `inPlay` on the entity (`player.entity.inPlay` outside Megaman; `inPlay`
     inside). `Megaman.ready` and `MegaPlayer.ready` go. `Rain` reads the entity directly (a non-player without the
     component reads as in play, as `?: true` gives today).
2. **Transport** (same package): `TransportComponent(var teleporter: ITeleporterEntity? = null)` with a `reset()`
   that clears it; free functions `IGameEntity.teleporter` and `IGameEntity.teleporting` (`teleporter != null`), and
   `beginTeleport(entity, teleporter)` / `endTeleport(entity)`, which call the entity's start or end hook and then set
   or clear the teleporter (the order above). The three teleporters call these functions in place of reading the
   hooks. `PipePortal`'s two writes set the teleporter on the component (players only, as today). `Megaman`'s hooks
   no longer touch the flag or the property; `Megaman.teleporting`, `MegaPlayer.teleporting` and
   `ConstKeys.TELEPORTER` go. `SpritesComponent` and `MegamanAnimationKeySupplier` read `teleporter`. `PreciousGem`
   reads `(owner as? IGameEntity)?.teleporting`.
3. `Megaman` adds both components; `PlayerStateComponent` goes.
4. Dead code: the `players.ready` imports in `SigmaRatClaw` and `BigAssMaverickRobotHand`; the teleport hooks in
   `AbstractEnemy.kt:165-166`.
5. Tests: the lifecycle and transport functions (defaults without the component, set and read, `reset()` behavior);
   `beginTeleport`/`endTeleport` order (the hook runs before the teleporter is set and before it is cleared);
   `MegaPlayersTest` loses its `ready`/`teleporting` cases.

New APIs and their first callers:

| API | First callers |
|---|---|
| `Lifecycle`, `LifecycleComponent`, `lifecycle`, `inPlay`, `setLifecycle` | the three level handlers; the `ready` readers |
| `TransportComponent`, `teleporter`, `teleporting` | Megaman's controller, behaviors and sprites; `PreciousGem` |
| `beginTeleport`, `endTeleport` | `PipePortal`, `PortalHopper`, `CapsuleTeleporter` |

## Out of scope

- Boss `ready` and `betweenReadyAndEndBossSpawnEvent` (item `048`, where `AbstractBoss` is broken up).
- The enemy spawn blink (until a reader outside `AbstractEnemy` needs it).
- `ITeleporterEntity.isTeleporting` stays per teleporter (a global check in `shouldTeleport` would change play).
- An engine move of `Lifecycle` (with item `012`, when an engine system reads it).
- The other `MegaPlayerAccess.kt` functions (a later `061` branch).

## Intended changes

None. One timing detail that no reader sees: the teleporter is cleared when Megaman is destroyed (today the flag stays
`true` until his next spawn). While he is destroyed, no Megaman code runs, and `PreciousGem` destroys itself on
`PLAYER_JUST_DIED`.

## Affected areas and risks

- About 30 files: the new package, `Megaman` and 10 files in `C/entities/megaman/`, the three level handlers,
  `MegaLevelScreen`, the eight outside `ready` readers, the three teleporters, `PreciousGem`, `AbstractEnemy`, the two
  dead imports, `MegaPlayerAccess.kt`, tests.
- **Hook order** (see "Current state"): a wrong order changes the air dash at a teleport start.
- **Defaults:** without the component, `inPlay` is `true` and `teleporting` is `false`. Megaman always has both
  components, so his answers do not change.

## Overlap with other branches

No branch is active in a worktree.

## Open questions

1. **The name of "in the `ACTIVE` phase".** Proposal: `inPlay`. Reason: `isActive` already names methods on behaviors,
   fixtures, `Death`, `InfernoDeath` and `GroundSlideBlocker`, so `entity.isActive` would be easy to misread. Other
   choice: compare the phase (`entity.lifecycle == Lifecycle.ACTIVE`) at each call site.
2. **The package for the two components.** Proposal: `C/entities/state/`.
