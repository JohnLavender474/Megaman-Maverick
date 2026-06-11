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

For any **read-only** inspection of a TMX (listing rooms, enumerating entities in a room,
looking up an entity's size or properties, finding canonical entity sizes across files), load the
`tmx-analyzer` skill — it wraps a script that produces a compact JSON summary far cheaper to read
than the raw XML. This skill (`level-editor`) covers **edits** and the XML/schema mechanics that
edits require.

## Before Editing — Required Prompts

At the **start of any editing session** (the first time the user asks for any modification to a TMX
in this conversation), ask the user **how the edits should be applied**:

> "Should I (a) make the edits in place on `<filename>.tmx`, or (b) make a temp copy
> (`<filename>_temp<NNN>.tmx`) and edit that instead?"

- If **(a) in place**: edit the file directly. The user is responsible for any version control.
- If **(b) copy**: duplicate the source TMX to a new file with a `_temp<NNN>` suffix (use a random
  3-digit number; verify the target path doesn't already exist) in the same directory, then apply
  all edits to the copy. Confirm the copy path back to the user and use it for the rest of the
  session unless the user redirects.

Do not ask this question again later in the same session unless the user switches to a different
source TMX.

If the user gives a clear directive up front ("edit X.tmx directly" or "copy X.tmx first"), skip
the question and proceed.

## How to Make Edits — Programmatic vs Generative

Match the edit method to the size and shape of the change:

**Prefer programmatic edits** (a one-off shell script — `python`, `sed`, `xmlstarlet`, etc.) when
the change is **bulk or mechanical**:
- Shifting many objects by a constant `dx`/`dy` (e.g. moving a whole room and its contents)
- Renaming a room and updating every `spawn_room` reference
- Bumping every object id by a constant to merge in another map's objects
- Resizing or repositioning many objects of the same type
- Any change that touches more than ~10 objects in a structured way

For programmatic edits:
1. Describe the script's intent to the user before running it.
2. Use Python with `xml.etree.ElementTree` (or `lxml`) when the change requires parsing structure;
   avoid `sed` for anything that depends on XML semantics.
3. Run the script, then verify by re-reading the affected region of the TMX.
4. If editing in place (mode (a)) and the change is sizeable, suggest the user commit or stash
   before you run the script.

**Prefer generative edits** (the `Edit` tool, one object at a time) when:
- The change is a small, localized tweak (a single property, a single object's coordinates)
- The change requires judgement per-object (e.g. picking different enemy types per room)
- Fewer than ~10 objects are affected

When in doubt between the two, ask the user. Generative edits are easier to review; programmatic
edits are more reliable at scale.

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

### Entity Properties

The `<properties>` block on a spawner object is passed to the entity's `onSpawn(props)` function as 
key-value pairs. To know which properties a given entity supports — their expected types, defaults, 
and effects — **read the entity's `onSpawn` function** (and any helpers it delegates to, such as 
`super.onSpawn`). Property keys are typically constants defined in `Constants.kt` (`ConstKeys`).

Workflow:
1. Resolve the entity class from the object's `name` attribute (under `core/.../entities/`).
2. Open the class file and locate `onSpawn(spawnProps: Properties)`.
3. Note every `spawnProps.get(...)`, `spawnProps.getOrDefault(...)`, and `containsKey(...)` call — 
   each one corresponds to a property you can set in the TMX.
4. Check the parent class's `onSpawn` too (e.g. `AbstractEnemy`, `AbstractProjectile`) for inherited 
   properties like `cull_out_of_bounds`, `cull_events`, etc.

If a property is missing from `onSpawn`, setting it in the TMX has no effect.

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

## Room Events (`event` property)

A room object in `game_rooms` may carry an `event` property that fires when the camera finishes 
transitioning into that room (see `MegaLevelScreen.cameraManagerForRooms.endTransition`):

| `event` value | Effect                                                                                                      |
|---------------|-------------------------------------------------------------------------------------------------------------|
| `boss`        | Fires `ENTER_BOSS_ROOM`. Room must also have an `object`-type property pointing to the boss spawner object. |
| `success`     | Fires `VICTORY_EVENT` → ends the level (no boss required).                                                  |

Additional room-level properties:
- `fade_out_music` (bool) — fades music on transition into the room (typical for boss rooms)
- `music` (string, `MusicAsset` name) — music to play while the room is current
- `megaman_direction` (string, `Direction` name) — flips Megaman's gravity direction on entry

## How a Level Ends

A level ends when `VICTORY_EVENT` is fired. There are exactly **two ways** to trigger it:

1. **Success room** — the player enters a room whose `event` property is `success` (no boss involved). 
   Useful for "walk to the exit" finales or non-combat conclusions.
2. **Non-mini boss defeat** — the player defeats a boss whose spawn properties satisfy both:
   - `mini` is `false` (or absent — default is `false`), and
   - `end` is `true` (or absent — default is `true`)
   
   See `AbstractBoss.getEventOnDefeated()` in `core/.../entities/contracts/AbstractBoss.kt`:
   ```kotlin
   if (!mini && end) EventType.VICTORY_EVENT else EventType.INTERMEDIATE_BOSS_DEAD
   ```
   A boss with `mini=true` or `end=false` fires `INTERMEDIATE_BOSS_DEAD` instead and the level 
   continues. This is how mini-bosses work.

**Implications when editing:**
- Every shippable level needs at least one of these two terminators, or it cannot be completed.
- If you mark a boss as `mini=true`, make sure there is still a final (non-mini, `end=true`) boss 
  in a room downstream OR a `success` room downstream.
- The boss spawner object (referenced from the boss room's `object` property) carries the `mini` 
  and `end` properties — not the room itself.

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

## Python Editing Boilerplate

When writing programmatic edits, reuse these patterns rather than re-deriving them.

### Standard script skeleton

```python
import xml.etree.ElementTree as ET

TMX = "assets/tiled_maps/tmx/LEVEL.tmx"
ET.register_namespace("", "")          # prevents ET from injecting ns0: prefixes
tree = ET.parse(TMX)
root = tree.getroot()

# ... make changes ...

tree.write(TMX, xml_declaration=True, encoding="UTF-8")
```

Always pass `xml_declaration=True, encoding="UTF-8"` to `tree.write` — omitting either
produces a file Tiled can't open.

### `make_object` helper

Drop this into any script that adds objects:

```python
def make_object(oid, name, x, y, w, h, props=None):
    """
    props: list of (key, value) or (key, value, type_str) tuples.
    type_str is only needed for Tiled typed properties (e.g. "object", "bool", "int").
    """
    attribs = {"id": str(oid), "x": str(x), "y": str(y),
               "width": str(w), "height": str(h)}
    if name:
        attribs["name"] = name
    obj = ET.Element("object", attribs)
    if props:
        pe = ET.SubElement(obj, "properties")
        for k, v, *rest in props:
            typ = rest[0] if rest else None
            pa = {"name": k, "value": str(v)}
            if typ:
                pa["type"] = typ
            ET.SubElement(pe, "property", pa)
    return obj
```

Usage: `make_object(41, "room3", 2112, 1152, 512, 448, props=[("event", "success")])`

### Tile CSV editing

```python
data_el = root.find('.//layer[@name="tiles1"]/data')
rows = [row.split(",") for row in data_el.text.strip().split("\n")]

# row index r  →  pixel y = r * 32
# col index c  →  pixel x = c * 32
rows[r][c] = "190"   # set a tile
rows[r][c] = "0"     # clear a tile

data_el.text = "\n" + "\n".join(",".join(row) for row in rows) + "\n"
```

### Adding a new objectgroup layer

```python
new_layer = ET.Element("objectgroup", {"id": "9", "name": "enemies"})
new_layer.append(make_object(...))

# Insert after the last existing objectgroup
last_og_idx = max(i for i, c in enumerate(root) if c.tag == "objectgroup")
root.insert(last_og_idx + 1, new_layer)
```

### Bumping nextobjectid / nextlayerid

Always update these on `<map>` after adding objects or layers, or Tiled will conflict
on next save:

```python
root.set("nextobjectid", str(max_new_id + 1))
root.set("nextlayerid",  str(max_new_layer_id + 1))
```

### Finding and removing an object by id

```python
for og in root.iter("objectgroup"):
    for obj in list(og):
        if obj.get("id") == "36":
            og.remove(obj)
```

## Common Mistakes

- **Wrong room name casing**: `spawn_room` must match the room object's `name` exactly (case-sensitive). 
                               Some maps use `Room1`, others use `room1`.
- **Editing obsolete TMX**: Always edit the file referenced in `LevelDefinition`, not an older versioned file.
- **Missing spawn_room property**: An object with `spawn_type=spawn_room` must also have a `spawn_room` property; 
                                   omitting it causes a crash.
- **Object id conflicts**: Each `<object>` id must be unique across the entire map; Tiled manages this automatically 
                           but manual edits can break it.
