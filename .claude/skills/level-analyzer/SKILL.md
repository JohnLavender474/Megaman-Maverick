---
name: level-analyzer
description: Use when asked to read, parse, display, or analyse an existing level from its Tiled TMX file.
---

# TMX To Plain-Text

## Overview

Converts an existing Tiled TMX level file into the plain-text room grid format
used by the level-designer skill. The output is read-only — it shows what is
already built in Tiled, not a design draft.

## Tool

Use the `run.sh` wrapper:

```bash
utils/level_analyzer/run.sh <path/to/level.tmx>
# or to save to a file if needed for inspection later:
utils/level_analyzer/run.sh <path/to/level.tmx> <output.txt>
```

TMX files live under `assets/tiled_maps/tmx/`.

## Character Legend

| Char | Meaning                                                                  |
|------|--------------------------------------------------------------------------|
| `X`  | Block / platform (`blocks` layer, non-Ladder objects)                    |
| `L`  | Ladder (`blocks` or `specials` layer, objects named `Ladder`)            |
| `E`  | Enemy (`enemies` layer)                                                  |
| `D`  | Death hazard (`hazards` layer or `sensors` layer objects named `Death`)  |
| `I`  | Item (`items` layer)                                                     |
| `\`  | Gate or room opening (adjacency-detected or `sensors` named `Gate`)      |
| `0`  | Player spawn — stage start (`player` layer, object named `"0"`)          |
| `n`  | Player respawn point `n` (`player` layer, objects named `"1"`, `"2"`, …) |
| ` `  | Empty space / air                                                        |

## Layer Mapping

| TMX layer    | What it becomes                                                           |
|--------------|---------------------------------------------------------------------------|
| `game_rooms` | Room bounding boxes (defines the grid size for each room)                 |
| `blocks`     | `X` (rectangles); `L` (objects named `Ladder`); `LadderTop` skipped       |
| `specials`   | `L` for objects named `Ladder`                                            |
| `hazards`    | `D` (point-mapped to the tile at the object's center)                     |
| `sensors`    | `D` for objects named `Death` (rect); `\` for objects named `Gate` (rect) |
| `enemies`    | `E` (center tile)                                                         |
| `items`      | `I` (center tile)                                                         |
| `player`     | `0`/`1`/`2`/… by object name                                              |

Layers not in this table are ignored.

## Gate Detection

After all layer objects are placed, the script inspects every pair of rooms
for shared pixel edges. Any border tile on a shared edge that is not blocked
by `X` on both sides is marked `\`. This means:

- A fully walled border will show no `\`.
- A border where blocks leave a gap becomes a run of `\` characters.
- `sensors` named `Gate` (locked stage gates) are also marked `\`.

Direction arrows (`>` `<` `^` `v`) from the level-designer format are **not**
emitted by this tool — room flow must be inferred from the level layout or
noted by hand.

## Room Sizes

Standard rooms are 16×14 tiles (512×448 px). The room label omits the size
for standard rooms and adds `(cols×rows)` for non-standard ones:

```
=== room5 (32×14) ===
=== room11 (16×20) ===
=== boss_room ===          ← standard 16×14, size omitted
```

## Workflow

### Step 1 — Run the tool

```bash
utils/tmx_to_plaintext/run.sh assets/tiled_maps/tmx/IntroStage_v3.tmx
```

Capture the output or pipe it to a file. Show it to the user inline if they
want to review or discuss the level.

### Step 2 — Interpret and discuss

After showing the output, point out:

- Room count and any non-standard sizes
- Where gates (`\`) connect rooms (adjacency topology)
- Enemy distribution across rooms
- Spawn point locations
- Any anomalies (unexpectedly empty rooms, overlapping objects, etc.)

### Step 3 — Hand off to level-designer (optional)

If the user wants to edit or extend the level, the plain-text output can be
treated as a starting draft in the level-designer skill format. Remind the
user that direction arrows are absent and should be added by hand if needed.
