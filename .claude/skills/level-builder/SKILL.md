---
name: level-builder
description: Use when asked to build, scaffold, or structurally edit a level's Tiled TMX file (rooms, blocks, player spawns, enemy/hazard markers, or inserting/shifting sections).
---

# Level Builder

## Overview

Builds and edits Megaman-Maverick level TMX files through a Python CLI that makes
surgical, ID-safe mutations to a live `.tmx`. The tool handles the error-prone
mechanics — unique object IDs, `nextobjectid`/`nextlayerid` bookkeeping, and
tile→pixel coordinate math — so structural work never requires hand-editing XML.

This is the *building* counterpart to two other skills:
- `level-designer` produces the plain-text room grids (the design intent).
- `tmx_to_plaintext` reads an existing TMX back into plain text.
- **`level-builder` (this skill) writes/edits the actual TMX.**

## Tool

```
utils/level_builder/
├── run.sh              # wrapper entry point (run from anywhere)
└── level_builder.py    # the CLI
```

Stdlib-only (no venv/deps). Run from the project root; TMX files live under
`assets/tiled_maps/tmx/`.

```bash
utils/level_builder/run.sh --help            # list all commands
utils/level_builder/run.sh <command> --help  # help for one command
```

## Coordinate System

**All commands take tile units**, not pixels. Origin is the top-left; `col`
increases rightward, `row` increases downward — exactly like the `level-designer`
plaintext grids. The tool multiplies by 32 to write pixel values into the TMX.
A standard room is 16 cols × 14 rows.

## Commands

### Build

| Command                                                                      | Effect                                                                                                                          |
|------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `init <file> --width W --height H`                                           | Create a fresh minimal TMX (map size in tiles) with empty `game_rooms`, `blocks`, `player`, `enemies`, `hazards`, `TODO` layers |
| `add-room <file> <name> --col --row --w --h`                                 | Add a rectangle to `game_rooms`                                                                                                 |
| `add-block <file> --col --row --w --h`                                       | Add a rectangle to `blocks` (may span multiple rooms)                                                                           |
| `add-spawn <file> <name> --col --row`                                        | Add a single-tile `player` spawn (`name` = `0` for stage start, `1`,`2`,… for respawns), placed directly above a block          |
| `add-marker <file> --layer enemies\|hazards\|TODO --name <text> --col --row` | Add a single-tile, name-only marker with **no properties**                                                                      |

### Inspect / edit

| Command                                   | Effect                                                                                                                                        |
|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `list <file> <layer>`                     | Print objects in a layer with their ids and tile bboxes (use this to find ids)                                                                |
| `remove <file> --id <id>`                 | Delete an object by id                                                                                                                        |
| `insert-cols <file> --at-col C --count N` | Open an N-tile vertical gap at column C: shift every object **fully at/past** col C right by N; grow the map and shift tile-layer columns too |
| `insert-rows <file> --at-row R --count N` | Open an N-tile horizontal gap at row R: shift every object **fully at/past** row R down by N; grow the map and shift tile-layer rows too      |

## Enemies & Hazards — always human-finalized

The tool **never** writes enemy/hazard properties or chooses specific enemies. For
these layers it only drops single-tile placeholder markers whose `name` is `TODO`,
`?`, or a direct name like `SniperJoe` — with no `<properties>`. Configuring real
enemies/hazards (properties, targets, facing, hard-mode flags, etc.) is done by the
**user** in Tiled. Keep this boundary; do not invent enemy properties.

## TODO Layer

Use `add-marker --layer TODO` to flag anything needing follow-up. Keep each marker
name to **≤ 4–5 words** describing what belongs there (e.g. `"add spike pit here"`,
`"boss gate trigger"`).

## Straddle Behavior (insert-cols / insert-rows)

Only objects **fully at or beyond** the cut line move. An object that straddles the
line (starts before it, extends past it — e.g. a floor block spanning two rooms)
is **left in place, unchanged**, and reported in the command output so you can
eyeball it. Adjust straddlers by hand afterward if needed.

## Workflow — build in iterations, check in between each

Build a level in this order, pausing for user review after **every** iteration.
Do not proceed to the next layer until the user confirms.

### Iteration 1 — Rooms
Lay out the `game_rooms` rectangles with `add-room`. Rooms are rectangles; a level
is a connected arrangement of them (horizontal/vertical, branches, etc.). Confirm
the room topology with the user before adding any content.

### Iteration 2 — Blocks
Add floors, walls, ceilings, and platforms with `add-block`. Blocks may span across
room boundaries. Confirm before moving on.

### Iteration 3 — Player spawns
Add the stage-start spawn `0` and any respawn points `1..n` with `add-spawn`, each a
single tile placed directly above a block. Confirm.

### Iteration 4 — Enemy & hazard markers
Drop single-tile placeholder markers with `add-marker` (enemies/hazards), plus any
`TODO` markers. Remind the user these are placeholders to finalize manually in Tiled.

## Editing an existing level

The tool edits live TMX files surgically, so you can bounce between tool-driven and
manual Tiled edits on the same file.

**Insert a new room between two existing rooms** (e.g. between `room1` and `room2`,
where `room2` is directly right of `room1`):

1. `insert-cols <file> --at-col <shared-edge-col> --count <new-room-width>`
   — pushes `room2` and everything right of it over, opening a blank gap. Note any
   straddling blocks reported and fix them if needed.
2. `add-room` / `add-block` / `add-spawn` / `add-marker` into the new gap.

The same pattern with `insert-rows` inserts a vertical section between stacked rooms.
Room **names are not auto-renumbered** — rename by hand if you care about order.

## Verifying

After building, read the level back with the `tmx_to_plaintext` skill to sanity-check
the layout as a plain-text grid before handing off to Tiled.
