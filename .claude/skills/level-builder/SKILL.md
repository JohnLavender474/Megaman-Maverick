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

Follow the design rules in the `level-designer` skill (Rules 6–13). In particular, for a
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

### Iteration 3 — Player spawns
Add the stage-start spawn `0` and any respawn points `1..n` with `add-spawn`, each a
single tile placed directly above a plain block (never a conveyor or other special
floor). Confirm.

### Iteration 4 — Enemy & hazard markers
Drop single-tile placeholder markers with `add-marker` (enemies/hazards), plus any
`TODO` markers. Remind the user these are placeholders to finalize manually in Tiled.

## Verifying

After building, read the level back with the `level-analyzer` skill if present to 
sanity-check the layout as a plain-text grid before handing off to Tiled.
