---
name: tmx-visualizer
description: Use whenever you need to "see" the spatial layout of a TMX level as text — an
  ASCII grid where each tile is a bracketed cell and entities are short codes.
---

# TMX Visualizer — Megaman Maverick

## What this skill is for

A single-purpose **read** skill that turns a TMX into a human-readable grid so you can reason
about a room's geometry and entity placement.

### Invocations

```bash
# Grids + key to stdout (grids first, then a "===== KEY =====" legend).
utils/tmx-visualizer/run.sh assets/tiled_maps/tmx/LEVEL.tmx

# Write to files: -o is a base name -> <base>.viz.txt (grids) + <base>.legend.txt (key).
utils/tmx-visualizer/run.sh assets/tiled_maps/tmx/LEVEL.tmx -o /tmp/level

# Render just one room.
utils/tmx-visualizer/run.sh assets/tiled_maps/tmx/LEVEL.tmx --room room4
```

Prefer `-o /tmp/<name>` for non-trivial levels. The grids go to `<name>.viz.txt` and the legend
to `<name>.legend.txt` next to it.

## Output shape

Each `game_rooms` rectangle renders as its own labeled grid (top-down, matching Tiled: row 0 is
the room's top edge). Every tile (32 px) is a **5-char cell wrapped in brackets**:

```
=== room1  origin(1088,1152)px  16x14 tiles ===
[  X  ][  X  ][  X  ][  X  ] ... 
[  X  ][     ][     ][     ] ...
[  X  ][ E-B ][     ][PS-1] ...
[  X  ][  X  ][  X  ][  X  ] ...
```

- The header gives the room name, its pixel origin, and its size in tiles.
- An object that spans multiple tiles puts its code in the top-left **anchor** cell and fills
  the rest of its footprint with `.` (continuation). Plain blocks fill every covered tile with `X`.
- When two or more distinct objects occupy the same tile, the cell shows a single **combo key**
  for that exact set of objects (see "Combined tiles" below) — nothing is hidden.

## Standardized cell codes (this skill owns these)

The script generates codes; this table defines the **conventions** it follows. The emitted
`.legend.txt` is the per-file legend — the source of truth for a specific map.

### Fixed codes — identical in every map

| Cell      | Meaning                                             |
|-----------|-----------------------------------------------------|
| `[     ]` | empty space                                         |
| `[  X  ]` | plain `Block` (geometry) — fills every covered tile |
| `[  .  ]` | continuation cell of a multi-tile **named** entity  |
| `[PS-n ]` | player spawn #n (numbered in map order)             |

### Generated codes — `<C>-<tok>` (unique per file; legend dis-ambiguates)

`C` is a one-letter **category prefix** fixed by the object's layer. `<tok>` is a short token
(≤3 chars) derived from the entity's name and made unique within its category:

| Prefix | Layer / kind                              | Examples                            |
|--------|-------------------------------------------|-------------------------------------|
| `E`    | enemies                                   | `E-B` (Bat), `E-SJ` (SwinginJoe)    |
| `H`    | hazards                                   | `H-AGS` (AcidGoopSupplier)          |
| `I`    | items                                     | `I-HT` (HealthTank)                 |
| `S`    | specials                                  | `S-L` (Ladder), `S-TW` (ToxicWater) |
| `N`    | sensors                                   | `N-G` (Gate), `N-D` (Death)         |
| `B`    | **named** block-layer objects (not plain) | `B-LT` (LadderTop)                  |

### Combined (multi-object) tiles

When 2+ distinct objects share a tile, the cell shows one **combo key** standing for that whole
set, and the same key is reused everywhere that combination recurs. Two forms:

- **Deterministic** — the components' codes joined with `/`, used when it fits the 5-char cell.
  e.g. a Gate over a plain Block → `N-G/X`.
- **Synthetic** — when the joined form is too long (e.g. `B-LT/S-L` is 8 chars), a short `&n`
  tag is assigned instead (`&1`, `&2`, …).

Either way the combo is spelled out under a `-- combined (multi-object) tiles --` section in the
key file, e.g. `[N-G/X]  combined tile: sensors : Gate + Block (geometry)`. Combo keys are
per-file (the synthetic `&n` numbering depends on encounter order) — always trust the `.legend.txt`.
