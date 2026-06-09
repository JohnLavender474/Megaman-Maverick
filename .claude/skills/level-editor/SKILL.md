---
name: level-editor
description: Use when reading, editing, or answering questions about levels, Tiled map (.tmx) files, 
  or spawning of entities — layers, rooms, spawners, object properties, entity placement and spawns, 
  and level structure.
---

# Level Editor — Megaman Maverick

## Overview

Available game levels are defined in `core/.../levels/LevelDefinition.kt`.  
The active TMX for each level is the `tmxMapSource` field on its entry.
TMX files are defined in `assets/tiled_maps/tmx`.

## File Format

TMX files are XML. Key XML elements:
- `<map>` — root element with width/height in tiles, tilewidth/tileheight in pixels (always 32px)
- `<layer>` — tile layers (visual tiles, e.g. `tiles1`, `tiles2`)
- `<objectgroup>` — object layers (logic/gameplay, identified by `name`)
- `<object>` — a rectangle or point within an objectgroup; has `id`, `name`, `x`, `y`, `width`, `height`
- `<properties>` / `<property>` — key-value pairs attached to an object

Coordinates are in pixels. Divide by 32 (PPM) to get tile units.

## Object Layers (Gameplay Logic)

Each `<objectgroup>` maps to a layer builder in `MegaMapLayerBuilders.kt`:

| Layer name    | Builder                  | Purpose                                  |
|---------------|--------------------------|------------------------------------------|
| `game_rooms`  | `GameRoomsLayerBuilder`  | Room boundary rectangles                 |
| `player`      | `PlayerLayerBuilder`     | Player spawn point(s)                    |
| `enemies`     | `SpawnersLayerBuilder`   | Enemy spawners                           |
| `blocks`      | `SpawnersLayerBuilder`   | Block spawners (unnamed → `Block` class) |
| `items`       | `SpawnersLayerBuilder`   | Item spawners                            |
| `hazards`     | `SpawnersLayerBuilder`   | Hazard spawners                          |
| `specials`    | `SpawnersLayerBuilder`   | Special entity spawners                  |
| `decorations` | `SpawnersLayerBuilder`   | Decoration spawners                      |
| `projectiles` | `SpawnersLayerBuilder`   | Projectile spawners                      |
| `backgrounds` | `BackgroundLayerBuilder` | Background layers                        |
| `foregrounds` | `ForegroundLayerBuilder` | Foreground layers                        |
| `sensors`     | `SensorsLayerBuilder`    | Sensor areas (e.g. Gate)                 |

## Rooms (`game_rooms` layer)

Each `<object>` in `game_rooms` is a rectangle defining one room. The `name` attribute is the room name 
(e.g. `room1`, `room2`, `Room3`). Room names are case-sensitive and must match references in spawner 
`spawn_room` properties.

## Spawners

Objects in spawner layers create in-game entities. To see which layers are spawner layers, 
see `core/.../screens/levels/tiled/layers/MegaMapLayerBuilders.kt`. The `name` attribute 
on each object is the entity class name (using `EntityType.getFullyQualifiedName(name)`). 
For certain layers, unnamed objects have a default entity: for example, in the `blocks` 
layer, an unnamed object defaults to the `Block` class.

### Spawn Types

Controlled by the `spawn_type` property on each object:

| `spawn_type` value | Trigger    | Notes                                                             |
|--------------------|------------|-------------------------------------------------------------------|
| *(absent)*         | In-camera  | Spawns when the game camera overlaps the object's rectangle       |
| `spawn_now`        | Immediate  | Spawns at level start; not added to spawner list                  |
| `spawn_room`       | Room entry | Requires a `spawn_room` property naming the target room           |
| `spawn_event`      | Game event | Requires an `events` property (comma-separated `EventType` names) |

See `core/.../screens/levels/tiled/layers/SpawnersLayerBuilder` which defines the logic for each rule.

**`respawnable` property** (boolean, default `true`): controls whether the entity re-spawns after being destroyed.

## Object-Type Properties

A property with `type="object"` contains the **id** of another object in the map. Use this to cross-reference objects:

```xml
<object id="180" name="HeliMet" x="1728" y="288" width="32" height="32">
  <properties>
    <property name="target_1" type="object" value="181"/>
    <property name="target_2" type="object" value="182"/>
  </properties>
</object>
```

To resolve: find the `<object>` whose `id` matches the value (search the whole TMX, any layer).

## Common Operations

**Find all enemies in a room:**
```bash
grep -A10 'name="enemies"' assets/tiled_maps/tmx/LEVEL.tmx | grep -B2 'spawn_room.*roomN'
```

**Find an object by id:**
```bash
grep 'id="42"' assets/tiled_maps/tmx/LEVEL.tmx
```

**List all rooms:**
```bash
grep -A20 'name="game_rooms"' assets/tiled_maps/tmx/LEVEL.tmx | grep '<object'
```

**Find all spawn_now entities:**
```bash
grep -B5 'spawn_now' assets/tiled_maps/tmx/LEVEL.tmx | grep 'name='
```

## Common Mistakes

- **Wrong room name casing**: `spawn_room` must match the room object's `name` exactly (case-sensitive). 
                               Some maps use `Room1`, others use `room1`.
- **Editing obsolete TMX**: Always edit the file referenced in `LevelDefinition`, not an older versioned file.
- **Missing spawn_room property**: An object with `spawn_type=spawn_room` must also have a `spawn_room` property; 
                                   omitting it causes a crash.
- **Object id conflicts**: Each `<object>` id must be unique across the entire map; Tiled manages this automatically 
                           but manual edits can break it.
