---
name: level-designer
description: Use when asked to design, draft, or scaffold a new or existing level.
---

# Level Creator

## Overview

Produces or edits plain-text scaffold grids for every room in a new stage. These grids are 
design documents — they capture structure and intent before building/editing in Tiled.

## Character Legend

| Char | Meaning                                                                               |
|------|---------------------------------------------------------------------------------------|
| `\`  | Gate or opening from one room to the next                                             |                                                                                     |                                                                                     | Opening or gate between rooms                                                         | 
| `X`  | Block / platform (maps to `blocks` layer)                                             |
| `.`  | Notable object — enemy, hazard, item, or special (maps to `enemies`, `hazards`, etc.) |
| ` `  | Empty space / air                                                                     |
| `0`  | Player spawn at stage start (only one per level)                                      |
| `n`  | Player re-spawn point (n ≥ 1; each subsequent room transition gets the next number)   |

## Room Grid Format

Each room is a rectangular grid of the characters above, wrapped in a labeled fence.

See the following examples:
- Room 1:
  - Player spawns on a platform at the top-left of the room
  - Dots indicate enemy positions, in this case flying/floating enemies
  - Two platforms in the center of the room for the player to land on
  - An exit at the bottom-right of the room
- Room 2:
  - Player enters from bottom-left of the room, consistent with the bottom-right exit from room 1
  - Room has two openings: one at the top-right and another at the bottom-right, meaning this room forks off into 2 separate paths

```
=== Room 1 (16×14) ===
XXXXXXXXXXXXXXXX
X              X
X 0            X
XXXX           X
X     .        X
X              X
X    XXX       X
X              X
X  .           X
X       XXX    X
X              \
X              \
X              \
XXXXXXXXXXXXXXXX

=== Room 2 (16×14) → right of Room 1 ===
XXXXXXXXXXXXXXXX
X              \
X              \
X              \
X           XXXX
XXXX           X
X              X
X   XXX        X
X              X
X          .   X
\    XXX       X
\              X
\              X
XXXXXXXXXXXX\\\X

=== Room 3 Path 1 (16×14) → right of Room 2 ===
XXXXXXXXXXXXXXXX
\              X
\     .        X
\              X
X     XXXXXXX  X
X              X
X         .    X
X    XXX       X
X              X
X         .    X
X    XXXXX     X
X              X
X   .          X
X\\\XXXXXXXXXXXX

=== Room 3 Path 2 (16x14) → below Room 2 ===
XXXXXXXXXXXX\\\X
X              X
X              X
X           XXXX
X              X
X        .     X
X       XXX    X
X              X
X    .         X
X   XXX        X
X              \
X              \
X              \
XXXXXXXXXXXXXXXX

# Note: Room 4 is positioned where both paths converge. Its top aligns with the
# bottom exit of Room 3 Path 1 (cols 14-16) and its left aligns with the right
# exit of Room 3 Path 2 (rows 9-11). Verify exact pixel placement in Tiled.

=== Room 4 (16×14) → beneath Room 3 Path 1, right of Room 3 Path 2 ===
X\\\XXXXXXXXXXXX
X              X
X              X
X              X
X              X
X              X
X              X
X              X
X              X
X              X
\              \
\              \
\      1       \
XXXXXXXXXXXXXXXX

=== Room 5 (32×14) → right of Room 4 ===
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
X                              \
X    .          .    .         \
X         XXX                  \
X                    XXXX    XXX
X    XXXXX      .              X
X                              X
X    .                 .       X
X         XXXXX                X
X                    XXX       X
\              .               X
\                              X
\         .        XXXXX       X
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

## Rules

1. **Standard room size is 16 columns × 14 rows** (matches VIEW_WIDTH=16, VIEW_HEIGHT=14). Non-standard sizes must be noted in the label (e.g. `32×14`).
2. **Spawn placement**: `0` is the start-of-level spawn; use `1`, `2`, `3`… in the order the player reaches each new room for the first time.
3. **One `.` per notable object**: Don't try to name specific enemies in the grid — the grid captures placement and density, not entity type. Add a notes block after the room to describe what each `.` is.
4. **Vertical transitions** require matching row gaps; **horizontal transitions** require matching column gaps.

## Notes Block Format

After each room grid, add a brief notes block describing room and any special properties.

Example:
```
Notes — Room 2:
  • (col 5, row 4)  Sniper Joe, facing left
  • (col 10, row 4) Bat, ceiling-mounted
  • Room property: normal scroll (left→right)
```

## Workflow

### Step 1 — Gather requirements

Ask the user for:

**Level name** — ask the user to provide a name; present using `temp_<unique_id>` 
for when the user does not want to provide a name.

**Theme** — Ice, fire, factory, desert, cave, underwater, or free-form (no theme).
Suggest a theme based on the user's input for level name.

**Number of rooms** — Ask the user how many rooms should be generated. The level 
may be expanded with more rooms in later sessions.

**Level-design philosophy:**
Ask the user whether to apply philosophy rules on a room-by-room basis or if Claude should
generate rooms of various philosophies at will (with minimal guidance from the user).

| Philosophy         | Description                                                  |
|--------------------|--------------------------------------------------------------|
| Linear             | Single horizontal or vertical path                           |
| Branching          | Diverging paths, some optional or rejoining                  |
| Gauntlet           | Dense enemy patterns, minimal platforming                    |
| Z-pattern          | Platforms arranged so Mega Man traverses the room in a Z     |
| Hill climb/descend | Room focused on ascending or descending                      |
| Empty              | No enemies or objects; used as a checkpoint or breather room |
| Hybrid             | Combines multiple philosophies                               |

### Step 2 — Draft room grids

Output an `overview.txt` file which highlights the overall level including themes, 
philosophies, etc. Ouput a draft of each room grid in order from spawn to the end. 
Save each room as a file in a folder structure in the project `temp` folder, i.e.:
```
- Megaman-Maverick
    - temp
        - level_drafts
            - <level_name>
                - overview.txt
                - room1.txt
                - room2.txt
                - room3.txt                
```

### Step 3 — Revise

Prompt for user feedback and revise grids as needed. Repeat until approved.
