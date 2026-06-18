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
than the raw XML. For a **spatial** view — an ASCII grid of each room, useful for confirming an
edit landed where you expect — load the `tmx-visualizer` skill. This skill (`level-editor`)
covers **edits** and the XML/schema mechanics that edits require.

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

### Sub-agent handoff

Hand off programmatic edits to a sub-agent. Provide the sub-agent the specification of what to change.

**Prompt template — fill in the bracketed fields and pass to `Agent` with `subagent_type: "claude"`:**

```
You are implementing a TMX level edit for the Megaman Maverick project.
Working directory: /home/johnlavender/IdeaProjects/Megaman-Maverick

TARGET FILE: [TMX_PATH]

WHAT TO DO:
[Describe the operations precisely: which objects to add, which to shift, which rooms to
create, what properties each object needs. Be specific — object coordinates, layer names,
entity class names, property keys and values.]

ENTITY PROPERTIES NEEDED:
[Paste the compact properties table produced by the entity-lookup sub-agent, or list the
relevant spawn_room, spawn_type, and entity-specific properties for each entity type.]

APPROVED ASCII MOCKUP (reference geometry — the edit must match this):
[Paste the approved ASCII grid for each affected room.]

INSTRUCTIONS:
1. Write a Python script at /tmp/tmx_edit.py using the boilerplate in
   .claude/skills/level-editor/SKILL.md (standard skeleton + make_object helper +
   XML declaration fixup). For object-only edits, use the raw-text preservation pattern
   so tile layers are never touched by ET.
2. Run: python3 /tmp/tmx_edit.py
3. Verify affected rooms with the visualizer:
   utils/tmx-visualizer/run.sh [TMX_PATH] --room [ROOM_NAME] -o /tmp/verify
4. Run the web viewer validate on the result:
   utils/tmx-webview/run.sh --validate /tmp/verify.viz.txt
5. Fix any discrepancies between the visualizer output and the approved mockup.
6. Report back in ≤15 lines: what was added/changed, any issues encountered,
   final file path. Do not include raw TMX, script code, or visualizer grids.
```

**After the sub-agent returns:** review its summary. If it reports issues, notify the
user and ask if the sub-agent process should retry or be killed.

### Sub-agent handoff for post-edit verification

When verifying that a specific room matches an expected layout after any edit, 
use an `Explore`sub-agent.

**Prompt template — pass to `Agent` with `subagent_type: "Explore"`:**

```
Verify a TMX room edit in the Megaman Maverick project.
Working directory: /home/johnlavender/IdeaProjects/Megaman-Maverick

1. Run: utils/tmx-visualizer/run.sh [TMX_PATH] --room [ROOM_NAME] -o /tmp/verify
2. Read /tmp/verify.viz.txt
3. Check these specific expectations:
   [LIST EACH EXPECTATION AS A BULLET, e.g.:
   - Row 1 (ceiling) has [X] cells at columns 0–1 and cols 58–59 open
   - B-ICP anchors appear at approximately rows 3, 7, 12 (±1 row acceptable)
   - N-ID sensor spans the full width of the last 2 rows
   - No cells appear outside the room boundary]
4. Report: PASS or FAIL. If FAIL, list each failed check with the row/col where it failed.
   Keep the entire response under 10 lines.
```

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

**For 1–2 entities:** look up directly in the main context.

Workflow:
1. Resolve the entity class from the object's `name` attribute (under `core/.../entities/`).
2. Open the class file and locate `onSpawn(spawnProps: Properties)`.
3. Note every `spawnProps.get(...)`, `spawnProps.getOrDefault(...)`, and `containsKey(...)` call — 
   each one corresponds to a property you can set in the TMX.
4. Check the parent class's `onSpawn` too (e.g. `AbstractEnemy`, `AbstractProjectile`) for inherited 
   properties like `cull_out_of_bounds`, `cull_events`, etc.

If a property is missing from `onSpawn`, setting it in the TMX has no effect.

**For 3+ entities:** use an `Explore` sub-agent to avoid loading multiple Kotlin files into the main
context. Pass the following prompt to `Agent` with `subagent_type: "Explore"`:

```
Look up the TMX spawn properties for these Megaman Maverick entities:
[LIST_ENTITY_NAMES — e.g. SniperJoe, FireDispensenator, DemonMet]

Working directory: /home/johnlavender/IdeaProjects/Megaman-Maverick

For each entity:
1. Find its Kotlin file under core/src/main/kotlin/com/megaman/maverick/game/entities/
2. Read its onSpawn(spawnProps) function
3. Also read the immediate parent class onSpawn for inherited properties
   (parent is usually AbstractEnemy, AbstractBoss, AbstractHazard, or AbstractBlock)
4. Return a compact table with columns:
   entity | property_key | type | default | notes

Return only the table. No file contents, no code snippets.
```

Use the returned table directly when writing the TMX objects or Python script.

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

# Python's ET emits single-quoted declaration: <?xml version='1.0' encoding='UTF-8'?>
# Tiled rejects this — always fix it immediately after writing.
with open(TMX, "r", encoding="utf-8") as f:
    _content = f.read()
_content = _content.replace(
    "<?xml version='1.0' encoding='UTF-8'?>",
    '<?xml version="1.0" encoding="UTF-8"?>'
)
with open(TMX, "w", encoding="utf-8") as f:
    f.write(_content)
```

Always pass `xml_declaration=True, encoding="UTF-8"` to `tree.write`, and always run the
fixup block above immediately after — omitting either step produces a file Tiled can't open.

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

Tiled's native format ends every row with a **trailing comma** (`0,0,0,`). The patterns below
preserve this so a round-trip through ET produces a byte-identical tile section.

```python
data_el = root.find('.//layer[@name="tiles1"]/data')
# Strip trailing comma before splitting so len(cells) == layer width exactly.
rows = [row.rstrip(",").split(",") for row in data_el.text.strip().split("\n") if row.strip()]

# row index r  →  pixel y = r * 32
# col index c  →  pixel x = c * 32
rows[r][c] = "190"   # set a tile
rows[r][c] = "0"     # clear a tile

# Restore the trailing comma on each row when joining.
data_el.text = "\n" + "\n".join(",".join(row) + "," for row in rows) + "\n"
```

When extending the map width, pad each row **after** the rstrip:

```python
for row in rows:
    while len(row) < new_width:
        row.append("0")
```

If you have no reason to touch the tile layers at all (object-only edit), **remove the `<layer>`
elements before parsing and re-insert the original raw text after writing** to avoid any
round-trip corruption:

```python
# Preserve tile layers as raw text; remove from tree so ET never touches them.
import re

with open(TMX, "r", encoding="utf-8") as f:
    raw = f.read()

layer_blocks = re.findall(r' <layer\b.*?</layer>\n', raw, re.DOTALL)
for block in layer_blocks:
    raw = raw.replace(block, "")

# Parse the stripped file, make object-only edits, write.
root = ET.fromstring(raw)
# ... edits ...
out = ET.tostring(root, encoding="unicode")
out = '<?xml version="1.0" encoding="UTF-8"?>\n' + out

# Re-insert tile layer blocks just before </map>.
for block in layer_blocks:
    out = out.replace("</map>", block + "</map>", 1)

with open(TMX, "w", encoding="utf-8") as f:
    f.write(out)
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
- **Touching tile layers unnecessarily**: ET round-tripping a `<layer>` drops trailing commas and emits a
                                          single-quoted XML declaration, both of which corrupt the file for Tiled.
                                          If your edit is object-only, use the raw-text preservation pattern above.

## Minimum Viable TMX Reference

`assets/tiled_maps/tmx/Test8.tmx` is the smallest complete, loadable level. Use it as the
structural template for any new TMX. It demonstrates: a tile layer with real tile data (non-zero
values), a `game_rooms` layer, a `blocks` layer with unnamed geometry rectangles, a `sensors`
layer with a `Gate`, a `specials` layer with `Ladder` and `Water`, and a `player` spawn.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<map version="1.10" tiledversion="1.12.2" orientation="orthogonal" renderorder="right-down" width="100" height="50" tilewidth="32" tileheight="32" infinite="0" nextlayerid="9" nextobjectid="41">
 <tileset firstgid="1" source="../tsx/Tileset1.tsx"/>
 <layer id="1" name="tiles1" width="100" height="50">
  <data encoding="csv">
0,0,...(50 rows × 100 cols, trailing comma on each row)...
  </data>
 </layer>
 <objectgroup id="2" name="game_rooms">
  <object id="22" name="room1" x="1088" y="1152" width="512" height="448"/>
  <object id="35" name="room2" x="1600" y="1152" width="512" height="448"/>
 </objectgroup>
 <objectgroup id="5" name="blocks">
  <object id="23" x="1536" y="1312" width="32" height="288"/>
  <object id="24" x="1152" y="1536" width="384" height="64"/>
  <object id="25" x="1152" y="1152" width="960" height="64"/>
  <object id="26" x="1088" y="1152" width="64" height="448"/>
  <object id="27" x="1184" y="1408" width="96" height="32"/>
  <object id="28" x="1344" y="1344" width="64" height="192"/>
  <object id="29" name="LadderTop" x="1152" y="1408" width="32" height="8"/>
  <object id="36" x="2048" y="1216" width="64" height="320"/>
  <object id="37" x="1632" y="1536" width="480" height="64"/>
  <object id="38" x="1568" y="1216" width="64" height="96"/>
  <object id="39" x="1568" y="1216" width="64" height="384"/>
 </objectgroup>
 <objectgroup id="8" name="sensors">
  <object id="40" name="Gate" x="1568" y="1216" width="64" height="96">
   <properties>
    <property name="room" value="room2"/>
   </properties>
  </object>
 </objectgroup>
 <objectgroup id="6" name="specials">
  <object id="33" name="Ladder" x="1152" y="1408" width="32" height="128"/>
  <object id="34" name="Water" x="1408" y="1376" width="128" height="160"/>
 </objectgroup>
 <objectgroup id="4" name="player">
  <object id="32" name="0" x="1216" y="1504" width="32" height="32"/>
 </objectgroup>
</map>
```

Key observations from this file:
- Layer ordering: `<layer>` (tiles) comes **before** all `<objectgroup>` elements
- Unnamed `<object>` entries in `blocks` are plain `Block` geometry (no `name` attribute)
- `Gate` in `sensors` needs a `room` property naming which room it locks
- The `player` spawn uses `name="0"` (the spawn index, not the entity class)
- Tile data rows end with a trailing comma — this is Tiled's native format and must be preserved
