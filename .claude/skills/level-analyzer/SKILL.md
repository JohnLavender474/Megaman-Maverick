---
name: level-analyzer
description: Use when asked to read, parse, display, or analyse an existing level from its Tiled TMX file.
---

# Level Analyzer

Reads a Tiled TMX level into a plain-text room grid and/or structured JSON. The
output is read-only — it reflects what is built in Tiled, not a design draft.

## Always start with `--help`

Before running anything, read the tool's own docs — they are the source of truth
for arguments, outputs (plain text vs. JSON), the analyzed layers, and the
character legend:

```bash
utils/level_analyzer/run.sh --help
```

Then run it:

```bash
utils/level_analyzer/run.sh <path/to/level.tmx>            # plain text to stdout
utils/level_analyzer/run.sh <path/to/level.tmx> <out.txt>  # writes out.txt + out.json
```

TMX files live under `assets/tiled_maps/tmx/`. Use the JSON output when you need
per-object ids, properties, or room membership; use the plain text to eyeball
layout.

## After running

Show the output inline if the user wants to review it, then point out room count
and non-standard sizes, gate (`\`) connections between rooms, enemy/spawn
placement, and any anomalies (empty rooms, overlaps). Room flow direction arrows
are not emitted — infer them by hand. If the user then wants to edit or extend
the level, hand off to the `level-designer` skill.
