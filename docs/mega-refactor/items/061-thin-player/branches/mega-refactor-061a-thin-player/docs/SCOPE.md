# Scope: mega-refactor-061a-thin-player

What this branch changes and why. Written after the experimental branch `temp-thin-player` was built and tested,
because the owner asked for that order (item `061` `ITEM.md`, "Owner's input"). The owner approves it.

- Status: draft
- Approved: no

## Goal

A player entity no longer needs Megaman's shape. `MegaPlayer` is a thin slot view. What it used to forward is free
functions over the entity, read when called. Flags that several sources write become locks (a component and free
functions). The player-damaged notification is an immediate signal. The game plays the same, except in the overlap
cases under "Intended changes".

## Item

`061`: all of the "Proposed scope" in `ITEM.md`.

## Current state (on `master`)

- `MegaPlayer` (150 lines, 37 members) forwards to its entity. Its constructor requires `IDirectional`, `IFaceable`,
  `IDamageable`, `IHealthEntity`, `MegaGameEntity`, `IFocusable`, `BodyComponent`, `BehaviorsComponent`,
  `PlayerStateComponent`, `ControllerComponent` and two property lambdas (`029` `NOTES.md`, member inventory).
- `PlayerStateComponent` holds `ready`, `canBeDamaged`, `teleporting`, `movementAllowed`, Megaman's timers and the
  damage listeners; only `Megaman` honors its flags. The flags are "last writer wins".
- Bosses register `IPlayerDamageListener` on each player present at their spawn.

## In scope

The changes on `temp-thin-player` (details in its review notes, which move into this folder):

1. **Thin `MegaPlayer`:** `slot`, `entity`, `input`; nothing required at registration. Former members are functions
   in `players/MegaPlayerAccess.kt` with the same names: needed parts (body, direction, facing, focus, dead, health,
   fixtures) fail at the read; optional parts (light, gravity device, ready, teleporting, invincible, damaged,
   behaviors) give an empty value. `direction` and `facing` setters go (the two writers write through the entity).
   `spawn` goes (`MegaLevelScreen` calls `engine.spawn`). `damaged` moves to `AbstractHealthEntity`. `targetFor` gives
   the first player with a body.
2. **Player parts as components:** `PlayerComponent` (the marker that `asPlayer` reads), `PlayerLightComponent`,
   `GravityDeviceComponent`; `PlayerKeys` goes; `PlayerStateComponent` keeps only `ready` and `teleporting`.
3. **Locks:** a set of reasons per group; free functions `lock`, `unlock`, `clearLocks`, `isLocked`; they replace
   `canBeDamaged`, `movementAllowed`/`canMove` writes, `allowMovement` and `setAllBehaviorsAllowed`. A behaviors lock
   disallows all behaviors on each lock and allows them again when the group is free.
4. **Signal:** engine `events/Signal<T>` (immediate, connect order, deferred connect/disconnect during an emit, no
   re-entrant emit); `MegaPlayers.playerDamaged` replaces `IPlayerDamageListener`.

Cleanup in this branch:

5. **The lock mechanism moves to the engine**, in the `InputButton` pattern: the engine has `LockGroup` and
   `LockReason` interfaces (an explicit `index`), `LocksComponent`, and the free functions; the game has the enums
   `MegaLockGroup` and `MegaLockReason` (explicit indexes) and supplies the behaviors side effect through the
   component (for example a callback that `Megaman` sets when it adds the component). A check refuses a reason index
   outside the bit range. The exact form of the side-effect hook is a plan detail; it must keep "disallow on each lock,
   allow when free".
6. **Test leftovers:** the unused `spawn` override and `spawnedWith` in `MegaPlayersTest`'s `TestPlayerEntity`.
7. **Import order** in the files where the temp branch's script put imports out of order (for example `Megaman.kt`).
8. The review notes `docs/temp-thin-player-review.md` move into this branch folder (`docs/NOTES.md`).

New APIs and their first callers:

| API | First callers |
|---|---|
| `MegaPlayerAccess.kt` functions | the former `MegaPlayer` call sites (unchanged names) |
| `PlayerComponent`, `PlayerLightComponent`, `GravityDeviceComponent` | `MegaPlayer`, `asPlayer`; `Megaman` |
| Engine `LockGroup`, `LockReason`, `LocksComponent`, lock functions | `Megaman`, the level handlers, `MegaLevelScreen`, `WilyFinalBoss`, `SpiderWeb`, `SmallIceCube` |
| Engine `Signal<T>` | `MegaPlayers.playerDamaged` (`Megaman` emits, `AbstractBoss` listens) |

## Out of scope

Faction (`030`), the `megaman` downcast and `shooting` (`024`, `060`), facing and direction as components (`048`),
`FeetSandRule`'s co-op question, the `unregister` cleanup (`047`), `targetPlayer` on every entity (`047`),
`EventsManager` order (`053`).

## Intended changes

The old flags were "last writer wins"; the locks differ only when one source unlocks while another still holds the
group:

1. Damage: the end of a teleport, the end of the spawn sequence or `INTERMEDIATE_BOSS_DEAD` no longer makes Megaman
   damageable while a boss defeat, the level end or the Wily final boss holds the damage lock;
   `INTERMEDIATE_BOSS_DEAD` after a mini boss no longer clears another source's lock.
2. Movement: a spider web and a camera rotation at the same time both have to end before Megaman can move.
3. Behaviors: a web that breaks during the end-of-level sequence no longer turns behaviors back on.

## Affected areas and risks

- About 200 files on the temp branch (176 by an added import only). The engine move touches `core/.../locks/` and its
  callers again (imports and enum names).
- The behaviors side effect must keep its timing when it moves behind an engine hook.
- Reason indexes: a bit set in an `Int` holds 32 reasons; the check catches more.

## Overlap with other branches

No branch is active in a worktree. `temp-thin-player` is the source and is deleted after the merge.

## Open questions

None.
