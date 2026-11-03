---
name: level-builder
description: Use when you need to build, scaffold, or structurally edit a level's TMX file 
  (rooms, blocks, player spawns, enemy/hazard markers, or inserting/shifting sections). 
  Prefer to use this skill instead of generating code to edit layers. It is expected 
  that the builds/edits will be rough, and the user will need to do manual edits.
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

**Always run `--help` (both the top-level and per-command form) before using this
tool**, rather than relying on memory of its commands or coordinate conventions.
The script is the source of truth for available commands, arguments, and the
tile/row/col coordinate system — those details live in its `--help` output and
are not duplicated here, so they can't drift out of sync with this doc.

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

Follow the design rules in the `level-designer` skill (Rules 6–16). In particular, for a
bottomless pit, the `Death` rectangle is off-screen: `y` = room bottom + 48 px, height
16 px, `instant=true`. `add-death` takes whole tiles only, so set this `y` and height on
the object after you add it. Extend floors, ceilings and outer walls past the room edges.

Paint the blocks. `init` makes a map with no tile layer, and `blocks` objects are invisible in
the game, so a map without painted tiles shows empty rooms. Every level and test room needs a
tile layer (CSV, named `tiles`, below the object layers) with at least one tile, and in practice
a tile on every cell that a block covers. The CLI cannot paint tiles; add the layer with a small
script or by hand, and raise `nextlayerid`. Use a tile from a tileset the map already has, for
example tile 175 of `Tileset1.tsx`, the most common floor tile in `Test1.tmx`. See the `tiles`
layer of `Test1.tmx` for the format.

#### Pushable blocks

A `PushableBlock` needs a 2×1 block bump between it and any wall that it can reach
(`level-designer` Rule 15). Add the bump with `add-block --w 2 --h 1`, on the floor, flush with the
block's start or its stop point. If a spawn stood on those tiles, move the spawn up onto the bump.

#### Clearance

After you add a block, box or platform, check the gap between it and the objects around it. A gap
that the player must pass is at least 3 tiles high (`level-designer` Rule 16). The CLI does not
check this, so read the positions back, for example with the `list` command.

#### Quicksand

The top of a `QuickSand` object is at least 1 tile below the top of the blocks on each side of it
(`level-designer` Rule 14). Raise the side blocks, for example with `translate-height --upward`, and
do not put the sand level with the floor.

#### Ladders

A ladder has three parts. Add all three. Without the tiles the ladder is invisible, and without
the `LadderTop` the player cannot stand on top of the ladder.

1. A `Ladder` object in the `specials` layer. It is 1 tile wide (32 px). Its bottom is on the
   floor surface. Its top edge is level with the top surface of the ledge or floor that it
   reaches, and that ledge touches the ladder's side. No properties.
2. A `LadderTop` object in the `blocks` layer. It has the same `x` and `y` as the `Ladder`,
   width 32 px, height 8 px, and no properties. It is a one-way block (`entities/blocks/LadderTop.kt`):
   the player stands on it from above and climbs down through it.
3. A ladder tile painted on every cell the `Ladder` covers, in a tile layer, for example `tiles`.
   Do not paint the cell above the ladder top. Use one of the `Tileset1.tsx` ladder tiles,
   local ids 128-132 (row 4 of the tileset). The live stages use local id 130
   (`TimberWoman_16x14_v4.tmx`, `RodentMan_16x14.tmx`), 131 (`IntroStage_v3.tmx`) and 132
   (`DesertMan_16x14_v4.tmx`). The gid is the tileset's `firstgid` + the local id. When
   `Tileset1.tsx` has `firstgid="1"`, local id 130 is gid 131. If the map does not have
   `Tileset1.tsx`, add `<tileset firstgid="N" source="../tsx/Tileset1.tsx"/>`, with `N` greater
   than the last gid of the tileset before it.

`add-ladder` adds parts 1 and 2. Its `--row` is the bottom row of the ladder and `--h` is the
height in tiles, so the top edge is at row `row + h`. Make that row the top surface of the ledge.
The CLI cannot paint part 3; paint the tiles with a small script or by hand, as for the blocks.

Some older stages use a wider `LadderTop` (40 px, 8 px of it on the ledge). Use the 32 px form
for new ladders.

#### Slopes

Slopes are for extra levels only (X6-style). The main stages have no slopes. A slope is a
`GroundSlope` block (`entities/blocks/GroundSlope.kt`): a straight, stepped floor run.

1. Put each slope in an object layer named `blocks_slopes` or `blocks_slopes_<n>`. These layers go
   to the blocks builder, and a separate layer lets you see the slopes by a toggle of the layer.
2. Each slope is a plain rectangle object named `GroundSlope`, the bounding box of one run. An
   object with no name in a `blocks_*` layer spawns as a plain `Block`, a full solid box.
3. Give it the string property `side`: `right` if the run rises to the right, `left` if it rises
   to the left (the high side).
4. Height / width is 1/4 (shallow, for example 8×2 or 4×1 tiles) or 1/2 (steep, for example 4×2 or
   2×1). Another ratio logs an error, and the game uses the nearer of the two.
5. The low end of the run touches a floor whose top is the bottom of the rectangle. The high end
   touches a flat block whose top is the top of the rectangle, or a wall. Put solid blocks under
   the rectangle, unless the slope is a floating platform.
6. Two runs can touch to make a chain, for example a shallow run that goes on as a steep run.
7. Keep every slope layer visible when you save. The game builds only visible layers.

The CLI cannot make a named object with a property in a custom layer. Add the slopes with a small
script (unique ids, raise `nextobjectid`, and `nextlayerid` for a new layer) or by hand in Tiled.
A `GroundSlope` draws its own filled triangle, so do not paint tiles in its box. See
`RefactorTest068.tmx` for examples of each case.

### Iteration 3 — Player spawns
Add the stage-start spawn `0` and any respawn points `1..n` with `add-spawn`, each a
single tile placed directly above a plain block (never a conveyor or other special
floor). Slot 1 spawns 1 tile to the right of each spawn with no `player_2` property, so
check that tile too (`level-designer` Rule 9). The CLI cannot write `player_2`; add the
rectangle and the object property by hand or with a small script. Confirm.

### Iteration 4 — Enemy & hazard markers
Drop single-tile placeholder markers with `add-marker` (enemies/hazards), plus any
`TODO` markers. Remind the user these are placeholders to finalize manually in Tiled.

## Verifying

After building, read the level back with the `level-analyzer` skill if present to 
sanity-check the layout as a plain-text grid before handing off to Tiled.
