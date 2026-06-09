---
name: entity-spawning
description: Use when reading or modifying how entities are spawned in levels — including
  ISpawner implementations, SpawnersLayerBuilder, SpawnsManager, spawn types (camera,
  room, event, immediate), respawning behavior, player spawning, or the player spawn
  animation sequence.
---

# Entity Spawning — Megaman Maverick

## Overview

Entity spawning is a two-phase pipeline:
1. **Build phase** (level load): TMX objects → `ISpawner` instances collected by `SpawnersLayerBuilder`
2. **Runtime phase** (each frame): `SpawnsManager` polls spawners → produces `Spawn` objects → game engine processes them

---

## Core Types

| Type                      | File                                | Role                                                          |
|---------------------------|-------------------------------------|---------------------------------------------------------------|
| `Spawn`                   | `spawns/Spawn.kt`                   | Data class: `entity + properties` pair handed to engine       |
| `ISpawner`                | `spawns/ISpawner.kt`                | Interface: `Supplier<Spawn?>`, `UpdatePredicate`, `ICullable` |
| `Spawner`                 | `spawns/Spawner.kt`                 | Base impl; holds `spawn` reference, clears dead entities      |
| `SpawnerForBoundsEntered` | `spawns/SpawnerForBoundsEntered.kt` | Spawns on camera-overlap **enter** edge                       |
| `SpawnerForEvent`         | `spawns/SpawnerForEvent.kt`         | Spawns when a matching `Event` fires                          |
| `SpawnsManager`           | `spawns/SpawnsManager.kt`           | Frame-by-frame spawner loop                                   |
| `SpawnerFactory`          | `spawns/SpawnerFactory.kt`          | Convenience constructors                                      |
| `SpawnerShapeFactory`     | `spawns/SpawnerShapeFactory.kt`     | Resolves spawn shape from `MapObject`                         |

---

## ISpawner Contract

```kotlin
interface ISpawner : Supplier<Spawn?>, UpdatePredicate, ICullable {
    var respawnable: Boolean
    fun shouldTest(delta: Float): Boolean   // default true
    fun shouldBeCulled(delta: Float): Boolean // default false
    fun reset() {}
}
```

`Spawner` base logic in `test()`:
- If the entity that was previously spawned is now dead (`spawn?.entity?.dead == true`), clears `spawn` so the spawner can fire again.
- Returns `!spawned` — only triggers `spawnSupplier` when nothing is currently alive.

---

## SpawnsManager Update Loop

```
each frame:
  for each spawner:
    if shouldBeCulled → reset() + remove
    else if shouldTest && test → add Spawn to output array
      if !respawnable → reset() + remove
```

`SpawnsManager` does **not** construct entities — it only enqueues `Spawn` objects. The level screen processes the array after update.

---

## Spawn Types (TMX → Runtime)

Controlled by the `spawn_type` property on each TMX object. Resolved in `SpawnersLayerBuilder`.

### No `spawn_type` (camera overlap)
- Creates `SpawnerForBoundsEntered` via `SpawnerFactory.spawnerForWhenInCamera`.
- **Edge-triggered**: only fires on the frame when the spawn shape transitions from *not overlapping* to *overlapping* the camera. Moving back out and re-entering fires again (if `respawnable=true`).
- Shape source: `SpawnerShapeFactory.getSpawnShape(entityType, mapObject)` (defaults to the object's rectangle).

### `spawn_now`
- No `ISpawner` created. Entity is fetched from `MegaEntityFactory` and spawned immediately during level load (`entity.spawn(spawnProps)`).
- Not added to the spawner list; cannot respawn.

### `spawn_room`
- Creates `SpawnerForEvent` listening on `PLAYER_READY`, `BEGIN_ROOM_TRANS`, `END_ROOM_TRANS`, `SET_TO_ROOM_NO_TRANS`.
- Predicate checks `game.getCurrentRoom()?.name == roomName`.
- The `spawn_room` property value must match the room object's `name` **exactly** (case-sensitive).
- Optional `events` property overrides the accepted event set (comma-separated `EventType` names).

### `spawn_event`
- Creates `SpawnerForEvent` via `SpawnerFactory.spawnerForWhenEventCalled`.
- Requires an `events` property: comma-separated `EventType` names (e.g. `PLAYER_READY,BEGIN_ROOM_TRANS`).
- Fires when any listed event is dispatched.

---

## SpawnerForBoundsEntered — Edge Detection Detail

```kotlin
val wasEntered = isEntered
isEntered = thisBounds().overlaps(otherBounds())
val shouldSpawn = !wasEntered && isEntered   // only on transition in
```

`reset()` clears `isEntered`, so after a respawn the spawner is ready to detect the next entry.

---

## SpawnerForEvent — Queuing Detail

Events arrive via `onEvent()` and are queued in a local `Array<Event>`. On `test()`:
- Iterates the queue; if `predicate(event)` passes → calls `spawnSupplier()`, breaks.
- Clears the queue after processing regardless of result.
- Does **not** enqueue if already `spawned`.

Spawners must be registered with `game.eventsMan.addListener(spawner)` and unregistered on level cleanup via the `Disposable` stored in `returnProps[DISPOSABLES]`.

---

## shouldTest Predicate

`SpawnersLayerBuilder` sets a `shouldTestPred` based on entity type:

| Entity Types                               | Predicate                                                                  |
|--------------------------------------------|----------------------------------------------------------------------------|
| `BLOCK`, `HAZARD`, `SPECIAL`, `DECORATION` | Always `true`                                                              |
| `ENEMY`, `ITEM`, `PROJECTILE`              | `false` during room transitions (`game.isProperty(ROOM_TRANSITION, true)`) |

This prevents enemies from spawning mid-transition.

---

## Building a Spawner Programmatically

Use `SpawnerFactory` rather than constructing directly:

```kotlin
// Camera-based
val spawner = SpawnerFactory.spawnerForWhenInCamera(
    camera = game.getGameCamera(),
    spawnShape = myShape,
    spawnSupplier = { Spawn(entity, props) },
    respawnable = true
)

// Event-based (set of events)
val spawner = SpawnerFactory.spawnerForWhenEventCalled(
    events = objectSetOf(EventType.PLAYER_READY),
    spawnSupplier = { Spawn(entity, props) },
    respawnable = false
)

// Event-based (custom predicate)
val spawner = SpawnerFactory.spawnerForOnEvent(
    predicate = { event -> event.getProperty("room") == "room1" },
    eventKeyMask = objectSetOf(EventType.BEGIN_ROOM_TRANS),
    spawnSupplier = { Spawn(entity, props) }
)
```

Register event-based spawners with `game.eventsMan.addListener(spawner)`.

---

## Player Spawning

Player spawning is separate from entity spawning and uses two dedicated classes.

### PlayerSpawnsManager (`screens/levels/spawns/PlayerSpawnsManager.kt`)
- Holds a list of `RectangleMapObject` spawn points from the `player` TMX layer.
- On `set()`: sorts by name alphabetically; pops the first as the initial spawn.
- On `run()` (called each frame): checks if any remaining spawn point overlaps the camera. If so, updates `current` and calls `onChangeSpawn`.
- `currentSpawnProps` converts the current spawn object to `Properties` and adds its `BOUNDS` rectangle.

### PlayerSpawnEventHandler (`screens/levels/events/PlayerSpawnEventHandler.kt`)
- Orchestrates the beam-in animation sequence when the player spawns.
- Sequence: fade-in → pre-beam pause → beam descends from above → beam-land animation → player becomes ready.
- Fires `EventType.PLAYER_SPAWN` at start; fires `EventType.PLAYER_READY` and `EventType.TURN_CONTROLLER_ON` when done.
- During the sequence: `gravityOn=false`, `canBeDamaged=false`, `ready=false`, behaviors disabled.
- Call `init()` to start; poll `isFinished()` to detect completion; call `reset()` to abort.

---

## Entity Resolution in SpawnersLayerBuilder

Entity class is resolved by name using reflection:

```kotlin
Class.forName(entityType.getFullyQualifiedName(name)).kotlin as KClass<out MegaGameEntity>
```

- `name` = `<object name="...">` in the TMX.
- For the `blocks` layer, unnamed objects default to `Block.TAG`.
- All other layers require an explicit name; a missing name throws `IllegalStateException`.
- Failed class lookups are logged and skipped (not a crash) — watch logs for `SpawnersLayerBuilder` errors if an entity doesn't appear.

Entity instances are fetched from the pool: `MegaEntityFactory.fetch(clazz)`.

---

## respawnable Property

- TMX property key: `respawnable` (boolean, default `true`).
- Read in `SpawnersLayerBuilder`: `spawnProps.getOrDefault(ConstKeys.RESPAWNABLE, true, Boolean::class)`.
- When `false`: spawner is removed from `SpawnsManager` after first spawn (one-shot).
- When `true`: spawner stays in the list; entity re-spawns when it dies and the trigger condition fires again.

---

## Common Mistakes

| Mistake                                    | Fix                                                                                                               |
|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `spawn_room` entity never appears          | Check room name casing — `spawn_room` must exactly match `<object name="...">` in `game_rooms`                    |
| `spawn_event` entity never appears         | Verify `events` property lists valid `EventType` enum names (uppercase, comma-separated, no spaces)               |
| Entity appears then never respawns         | Check `respawnable=false` is not accidentally set                                                                 |
| `spawn_now` entity appears twice           | It is spawned during load AND found in camera — only use `spawn_now` for entities that manage their own lifecycle |
| Enemy spawns during room transition        | Expected behavior — enemy/item spawners gate on `!ROOM_TRANSITION` via `shouldTestPred`                           |
| Custom spawner events leak                 | Always pair `addListener` with a `Disposable` that calls `removeListener`                                         |
| `SpawnerForBoundsEntered` fires repeatedly | It is edge-triggered; if it fires every frame, check that `reset()` is clearing `isEntered`                       |
