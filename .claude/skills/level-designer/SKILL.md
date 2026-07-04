---
name: level-designer
description: Use when asked to design, draft, scaffold, create, or edit a new or existing level.
---

# Level Designer

## Overview

Produces or edits plain-text scaffold grids for every room in a new stage. These grids are 
design documents — they capture structure and intent before building/editing in Tiled.

## Character Legend

| Char            | Meaning                                                            |
|-----------------|--------------------------------------------------------------------|
| `\`             | Gate or opening from one room to the next                          |
| `>` `<` `^` `v` | Player enter/exit direction: inward for entrance, outward for exit |
| `X`             | Block / platform (maps to `blocks` layer)                          |
| `E`             | Enemy (maps to `enemies` layer)                                    |
| `I`             | Item (maps to `items` layer)                                       |
| `D`             | Death hazard — spikes, lava, or anything that kills on contact     |
| `L`             | Ladder (for the player to climb up/down)                           |
| ` `             | Empty space / air                                                  |
| `0`             | Player spawn at stage start (only one per level)                   |
| `n`             | Player re-spawn point (n ≥ 1)                                      |

## Room Grid Format

Each room is a rectangular grid of the characters above, wrapped in a labeled fence.

## Example

```
=== Room 1 (16×14) ===
XXXXXXXXXXXXXXXX
X              X
X 0            X
XXXX           X
X              X
X              X
XXXXXXXX       X
X              X
X        E     X
X    XXXXXXXXXXX
X              \
X             >\
X        E     \
XXXXXXXXXXXXXXXX

- Player spawns at top-left of the room on a platform ledge hanging off the wall
- Player descends and moves along platforms in a Z, facing 2 enemies along the way
- Player exits the room rightward via an opening at the bottom-right of the screen

=== Room 2 (16×14) → right of Room 1 ===
XXXXXXXXXXXXXXXX
X              \
X             >\
X      E       \
X          XXXXX
XXXXX          X
X    E      E  X
X   XXX   XXXXXX
X              X
X       XX     X
\    XXXXX     X
\>             X
\        E   v X
XXXXXXXXXXXX\\\X

- Player enters the room from the bottom-left of the screen and immediately faces an enemy stationed on the ground
- Player has two routes to choose from: ascend up along platforms, or descend at bottom-right

=== Room 3 Path 1 (16×14) → right of Room 2 ===
XXXXXXXXXXXXXXXX
\              X
\>    E        X
\              X
XXXXX   XXXXX  X
X   XDDDX      X
X   XXXXX      X
X              X
X   XXXXXXXXXXXX
X         E    X
XXXXXXXXXXXXX  X
X              X
X v     E      X
X\\\XXXXXXXXXXXX

- Player enters from top-left of the screen and immediately faces a floating enemy and a small pit of spikes
- Player descends the room in a Z pattern, encountering 2 enemies along the way
- Player exits the room downward at the bottom-left of the room

=== Room 3 Path 2 (16x14) → below Room 2 ===
XXXXXXXXXXXX\\\X
X            v X
X              X
X           XXXX
X              X
X        E     X
X       XXX    X
X              X
X    E         X
X   XXX        X
X              \
X             >\
X              \
XXXXXXXXXXXXXXXX

- Player descends into room from top-right of the screen and lands on a platform protruding from the wall
- Player descends down the room, encountering two enemies each stationed on floating platforms
- Player exits the room rightward at the bottom-right corner of the room

=== Room 4 (16×14) → beneath Room 3 Path 1, right of Room 3 Path 2 ===
X\\\XXXXXXXXXXXX
X v            X
X              X
X              X
X              X
X              X
X              X
X              X
X              X
X              X
\              \
\>            >\
\      1       \
XXXXXXXXXXXXXXXX

- Player enters either via bottom-left or top-left depending on which path he chose in room 2
- Player checkpoint located at the bottom-middle of the room
- Player exists the room rightward via a gate at the bottom-right of the room

=== Room 5 (32×14) → right of Room 4 ===
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
X                              \
X      E                      >\
X      X   XXX         E       \
X      XDDDXXXXXXX   XXXXX  XXXX
XLXXXXXXXXXX                   X
XL             E               X
XL   E        XXX      E       X
XL        XXXXXXXXXXXXXXXXXXXXLX
XXXXXXXXXXX                   LX
\                    E        LX
\>   XXX    XXX    XXXXX     XXX
\    XXXDDDDXXXDDDDXXXXXDDDDDXXX
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

- Player enters the room from the bottom-left of the room and must jump over spikes.
- Room features a gauntlet of many enemies, platforms, and ladders the player must ascend.
- Player exits the room rightward via a gate at the top-right of the screen.
```

## Rules

1. **Standard room size is 16 columns × 14 rows** (matches VIEW_WIDTH=16, VIEW_HEIGHT=14). 
   Non-standard sizes must be noted in the label (e.g. `32×14`).
2. **Spawn placement**: `0` is the start-of-level spawn; use `1`, `2`, `3`… in the order the 
   player reaches each new room for the first time.
3. **One character per object**: Add a notes block after the room to describe each object in
   more detail if needed, e.g., what kind of enemy is intended for a particular `E`.
4. **Vertical transitions** require matching row gaps; **horizontal transitions** require 
   matching column gaps.
5. **Entry/exit arrows**: Mark every room transition opening/gate with a direction arrow 
   (`>` `<` `^` `v`) in the cell immediately next to the opening, pointing into the room for
   a room entrance and pointing out of the room for a room exit. Whether an opening is considered
   an entrance or an exit depends on the flow of the level. 

## Notes Block Format

After each room grid, add a brief notes block describing room and any special properties.

Example:
```
Notes — Room 2:
  • E (col 5, row 4)  Sniper Joe, facing left
  • E (col 10, row 4) Bat, ceiling-mounted
  • Room property: normal scroll (left→right)
```

## Workflow

### Step 1 — Gather requirements

Ask the user for:

**Scaffold or fill in?** — Ask whether each room should be a bare scaffold (outer border only, 
no interior content) or a filled draft (platforms, enemies, and spawn points placed). A scaffold 
is useful when the user wants to place content themselves in Tiled; a filled draft is useful when 
the user wants a starting point to iterate from.

**Level name** — ask the user to provide a name; present using `temp_<unique_id>`for when the user 
does not want to provide a name.

**Theme** — Ice, fire, factory, desert, cave, underwater, or free-form (no theme). Suggest a theme 
based on the user's input for level name.

**Number of rooms** — Ask the user how many rooms should be generated. The level may be expanded 
with more rooms in later sessions.

**Level-design philosophy:**
Ask the user whether to apply philosophy rules on a room-by-room basis or if Claude should
generate rooms of various philosophies at will (with minimal guidance from the user). A room
can have one or more philosophies applied, or none - none meaning either no edits made to the
room or no content in the room besides the entrance(s)/exit(s) and borders of the room.

| Philosophy            | Description                                                        |
|-----------------------|--------------------------------------------------------------------|
| Linear                | Single horizontal or vertical path                                 |
| Branching             | Diverging paths, some optional or rejoining                        |
| Gauntlet              | Dense enemy patterns, minimal platforming                          |
| Z-pattern             | Platforms arranged so Mega Man traverses the room in a Z           |
| Hill climb/descend    | Room focused on ascending or descending                            |
| Empty                 | No enemies or objects; used as a checkpoint or breather room       |
| Spike corridor        | Narrow passage lined with spikes; tests precise movement           |
| Disappearing blocks   | Platforms appear/vanish in sequence; tests memorization and timing |
| Moving platforms      | Platforms moving over instakill pits; tests jump timing            |
| Obstacle course       | Hazard-focused (spikes, cannons, crushers); few or no enemies      |
| Precision pit         | Tight jumps over instant-death gaps; minimal enemies               |
| Hybrid                | Combines multiple philosophies                                     |

### Step 2 — Draft room grids

Output an `overview.txt` file which highlights the overall level including themes, 
philosophies, etc. Output a draft of each room grid in order from spawn to the end. 
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

### Step 4 — Determine Next Step

Prompt the user to answer which step should be taken next.
- Create a new TMX file with the designs
- Edit an existing TMX file with the designs
- Something else (or nothing)...
