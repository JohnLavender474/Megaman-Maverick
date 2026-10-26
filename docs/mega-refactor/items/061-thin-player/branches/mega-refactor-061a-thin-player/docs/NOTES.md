# Notes: mega-refactor-061a-thin-player

## Decisions

- 2026-10-26 (owner): a new item; the temp branch's work is the base; the cleanup moves the locks to the engine.

## Notes from `temp-thin-player` (moved from its review file)

An experimental branch, outside the item process (owner's request, 2026-10-26). If the owner accepts it, an item
branch takes these changes, and this file moves into that item's branch folder. Base: `master` at `55e81840c`.

The direction comes from item `029` `NOTES.md`: "`MegaPlayer` review and engine patterns", "Functions, components and
events instead of classes", and "Correction: 'or null' lookups versus query filters".

### What changed

#### 1. A thin `MegaPlayer`

`MegaPlayer` (`core/.../players/MegaPlayer.kt`) has only `slot`, `entity` and `input`. It requires nothing at
registration. It binds its input source to the entity's `ControllerComponent` if there is one, and adds a
`PlayerComponent` (the player marker) to the entity.

Every former member is a free function or extension property in `players/MegaPlayerAccess.kt`. Each one reads the
entity when it is called, not at registration. The call sites keep their names (`player.body`,
`targetPlayer.facing`), so 176 files changed only by an added import.

| Former member | Now | Kind |
|---|---|---|
| `body` | `entity.require(BodyComponent::class).body` | needed: throws `MissingComponentException` if absent |
| (new) `hasBody` | `entity.hasComponent(BodyComponent::class)` | query filter (used by `targetFor`) |
| `direction`, `facing`, `focus`, `dead` | read the entity's `IDirectional`, `IFaceable`, `IFocusable`, `MegaGameEntity` | needed: `IllegalStateException` naming the part |
| `direction` and `facing` setters | removed; the two writers (`WorldGravity`, `WilyFinalBoss`) write through the entity, and skip an entity without the role | — |
| `healthRatio`, `currentHealth`, `maxHealth`, `translateHealth`, `depleteHealth` | read the entity's `IHealthEntity` | needed |
| `invincible` | `IDamageable.invincible`, `true` if absent | optional |
| `damaged` | `AbstractHealthEntity.damaged` (moved up from `Megaman`), `false` if absent | optional |
| `ready` (get/set), `teleporting` | `PlayerStateComponent` (now only these two flags); `false` if absent | optional (the setter needs the component) |
| `light` | `lightOrNull` from `PlayerLightComponent`; the four `Darkness*` callers use `?: PlayerLight.STANDARD` | optional |
| `onGravityDeviceSwitch` | `GravityDeviceComponent.onSwitch`, no effect if absent | optional |
| `isBehaviorActive`, `isAnyBehaviorActive` | `BehaviorsComponent`, `false` if absent | optional |
| six fixture getters | unchanged lookups on `body` (Megaman's fixture names; item `014`/`060`) | needed |
| `shooting` | `PlayerExtensions.kt`, next to the `megaman` accessor: `(entity as? Megaman)?.shooting == true`, with a `TODO` for item `024` | optional, Megaman-only |
| `spawn` | removed; `MegaLevelScreen` calls `engine.spawn(player.entity, props)` (the same as `GameEntity.spawn`) | — |
| `canBeDamaged`, `allowMovement`, `setAllBehaviorsAllowed` | locks (below) | — |
| `addDamageListener`, `removeDamageListener` | the `playerDamaged` signal (below) | — |

Other player parts as components:

- `PlayerComponent(player)` replaces the `VIEW` property. `asPlayer` reads it.
- `PlayerLightComponent` and `GravityDeviceComponent` replace the `LIGHT` and `GRAVITY_DEVICE_SWITCH` property
  lambdas. `PlayerKeys` is gone.
- `PlayerStateComponent` no longer borrows Megaman's timers. It holds only `ready` and `teleporting`.
- `targetFor` gives the first player (slot order) whose entity has a body, else slot 0 (with one player: slot 0, as
  before).

#### 2. Locks: components and free functions, no base class

`core/.../locks/`: `LockGroup` (`DAMAGE`, `MOVEMENT`, `BEHAVIORS`), `LockReason` (`SPAWN`, `TELEPORT`,
`CAMERA_ROTATION`, `BOSS_DEFEATED`, `LEVEL_END`, `WILY_FINAL_BOSS`, `SPIDER_WEB`), `LocksComponent` (one `Int` bit
set of reasons per group; no allocation; `reset()` clears all), and the free functions `lock`, `unlock`,
`clearLocks`, `isLocked` on any `IGameEntity`.

- A group is locked while any reason holds it. A reason is held once: a second `lock` with the same reason needs
  only one `unlock`. (A count per reason, as in GAS, would leave `SpiderWeb` locked for good: each web locks, but the
  release happens once, when the last web is gone.)
- `lock(BEHAVIORS, …)` calls `setAllBehaviorsAllowed(false)` on every call, as each old call did (it also ends active
  behaviors). `unlock(BEHAVIORS, …)` allows behaviors again only when no reason holds the group.
- Readers: `Megaman.invincible` (`isLocked(DAMAGE)` in place of `!canBeDamaged`), `Megaman.canMove` (now a `val`:
  `!isLocked(MOVEMENT) && !stunned && !damaged && !frozen`), `SmallIceCube` (`isLocked(DAMAGE)`).

The writers, one for one:

| Old write | New |
|---|---|
| `Megaman.onSpawn`: `canMove = true`, `canBeDamaged = true` | `clearLocks(MOVEMENT)`, `clearLocks(DAMAGE)` |
| Megaman teleport start / end: `canBeDamaged = false / true` | `lock / unlock(DAMAGE, TELEPORT)` |
| Megaman camera rotation start / `END_GAME_CAM_ROTATION`: `canMove = false / true` | `lock / unlock(MOVEMENT, CAMERA_ROTATION)` |
| `PlayerSpawnEventHandler` start / end: `canBeDamaged`, `setAllBehaviorsAllowed` `false / true` | `lock / unlock(DAMAGE, SPAWN)`, `lock / unlock(BEHAVIORS, SPAWN)` |
| `EndLevelEventHandler`: `canBeDamaged = false`, `setAllBehaviorsAllowed(false)` | `lock(DAMAGE, LEVEL_END)`, `lock(BEHAVIORS, LEVEL_END)` |
| `MegaLevelScreen` `BOSS_DEFEATED` (not mini) / `INTERMEDIATE_BOSS_DEAD`: `canBeDamaged = false / true` | `lock / unlock(DAMAGE, BOSS_DEFEATED)` |
| `WilyFinalBoss`: `canBeDamaged = false` | `lock(DAMAGE, WILY_FINAL_BOSS)` |
| `SpiderWeb` stick / last web gone: `setAllBehaviorsAllowed`, `allowMovement` `false / true` | `lock / unlock(BEHAVIORS, SPIDER_WEB)`, `lock / unlock(MOVEMENT, SPIDER_WEB)` |

Locks that are never unlocked (`LEVEL_END`, `WILY_FINAL_BOSS`): `DAMAGE` is cleared by `Megaman.onSpawn`, as the old
flag was set to `true` there. `BEHAVIORS` (`LEVEL_END`) is cleared when the level screen is left:
`setCurrentScreen` calls `reset()` on the old screen, `MegaLevelScreen.reset()` calls `engine.reset()`, which destroys
Megaman and resets his components.

#### 3. An immediate signal for the player-damaged notification

- Engine `events/Signal<T>`: listeners run at once, in connect order. A connect or disconnect during an emit applies
  after that emit. An emit from one of its own listeners throws.
- `MegaPlayers.playerDamaged` (one reused `PlayerDamaged` payload) replaces `IPlayerDamageListener` and the
  per-player `damageListeners`. `Megaman.takeDamageFrom` calls `players.onPlayerDamaged(...)` at the same point as
  before. `AbstractBoss` connects in `onSpawn` and disconnects in `onDestroy`; `PreciousWoman` overrides the same
  `onPlayerDamaged`. The old `TODO` ("a player who registers during a boss fight is not listened to") no longer
  applies: the signal belongs to the players service, not to each player.

### Where play can differ from `master`

With one player, every single source behaves as before. The old flags were "last writer wins"; the reason sets
differ only when one source unlocks while another source still holds the same group. These overlaps are not known
to happen in normal play; please watch for them:

1. **Damage lock overlaps.** On `master`, the end of a teleport, the end of the spawn sequence, or
   `INTERMEDIATE_BOSS_DEAD` makes Megaman damageable even if another source still holds the lock (a boss defeat, the
   level end, the Wily final boss). Now he stays undamageable until every reason is gone. `INTERMEDIATE_BOSS_DEAD`
   after a mini boss (which never locked) also no longer clears another source's lock.
2. **Movement: spider web during a camera rotation.** On `master`, the end of the rotation frees movement while a web
   still holds Megaman (behaviors stay off), and a web that breaks during a rotation frees movement mid-rotation.
   Now movement stays locked until both are gone.
3. **Behaviors: a web that breaks during the level end.** On `master`, the last web's release turns behaviors back on
   during the end-of-level sequence. Now they stay off.

All three are arguably fixes, but they are gameplay changes in edge cases.

### Not changed (later items)

- Faction (`030`): `asPlayer` still means three things (side, player-body rules, player state).
- The `megaman` downcast (19 uses; loadout `024`, look `060`) and `shooting`.
- Facing and direction stay interfaces on the entity (`048` would make them components); the functions read the
  interface, so only their bodies change later.
- The `FeetSandRule` co-op question (`players.any { it.ready }`), the late `unregister` cleanup (`TODO` in
  `MegaPlayer`, `047`).
- `targetPlayer` on every entity (`047`).
- `EventsManager` delivery order (hash order across keys; `053`).

### Checks done

- Clean build passes; 493 engine tests (4 new: `SignalTest`) and 163 core tests (new `LocksTest`; `MegaPlayersTest`
  rewritten: an entity with no parts registers, needed parts throw at the read, optional parts give their empty
  value, `targetFor` picks the first player with a body, the damage signal order), 0 failures. 114 warnings, the
  same as `master`. `lwjgl3:jar` builds.
- Short launches with no exception: `REFACTOR_TEST_029` `movement` and `gravity_switch`, `INTRO_STAGE` `room12`,
  `REFACTOR_TEST_031` `pipe_portal`, `DESERT_MAN` from its start, and the start screens.
- Performance: `player.body` is now one component lookup per read (it was a cached field); the light and the gravity
  device are one lookup each (they were cached lambdas). With one player this is a few microseconds per frame.

### Manual checks for the owner

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/temp-thin-player`:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: all movement, shooting, charge, weapon switch,
  pause; take damage from an enemy (knockback, blink, recovery).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: the gravity switch changes direction and the
  A-button task.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: a room transition with a direction held.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: enter a pipe and come out of the linked pipe (a
  `PipePortal` is a teleporter: it uses the `TELEPORT` damage lock).
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the boss intro lock, the fight (Megaman takes damage), the
  boss defeat (Megaman cannot be damaged after it), the end-of-level beam-out; then the next level starts with
  working controls.
- Die once, and check the respawn: Megaman cannot be damaged and cannot act during the spawn sequence, and can after
  it.
- `SpiderWeb`: nothing spawns it today (only the deprecated `ProjectilesFactory` pools it), so its lock changes are
  checked by review only.
- A dark room (`Darkness*`, for example in `DESERT_MAN` or `WILY_STAGE_1` rooms 7 and 8): the light around Megaman
  while charging and with the jetpack.
- `PreciousWoman`: she laughs when Megaman takes damage while she stands.

## Overlap with other branches

None active.
