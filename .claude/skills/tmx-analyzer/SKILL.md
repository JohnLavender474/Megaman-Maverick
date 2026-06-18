---
name: tmx-analyzer
description: Use whenever you need to read or inspect a TMX level file without editing it —
  listing rooms, enumerating entities in a room, looking up an entity's size or properties,
  finding canonical entity sizes across multiple TMX files, or building an assessment of a
  level's structure. The skill wraps the `utils/tmx-analyzer/run.sh` script, which produces a
  compact JSON summary far cheaper to read than the raw XML. The other level skills
  (`level-creator`, `level-editor`) defer to this skill for all read-side work.
---

# TMX Analyzer — Megaman Maverick

## What this skill is for

A single-purpose **read** skill. You should reach for it any time a task asks you to:

- **Summarize** a level: room chain, sizes, what's in each room.
- **Look up** a specific entity by id, or filter entities by layer/name within a room.
- **Compare** how an entity class is used across many TMX files (canonical sizes,
  representative property blocks) — this is the cross-file lookup case that the level-creator
  skill's "flesh-out" phase relies on.
- **Assess** an existing level before editing or expanding it (room chain, theme inference,
  difficulty arc, seam identification).

It is **not** for edits. Any time you need to write/modify a TMX, fall back to raw XML via the
`level-editor` skill's edit workflow.

For a **spatial** view of a level — an ASCII grid of each room rather than a JSON summary — use
the companion `tmx-visualizer` skill. This skill answers "what/where is entity X"; that one
answers "what does this room look like".

## The script

The analyzer lives at `utils/tmx-analyzer/run.sh` (auto-managed venv). It is a thin wrapper
around `utils/tmx-analyzer/analyze.py`; that script's module docstring is the authoritative
contract for the output JSON shape — re-read it if anything below is unclear.

### Invocations

```bash
# Full-level summary JSON to stdout.
utils/tmx-analyzer/run.sh assets/tiled_maps/tmx/LEVEL.tmx

# Write to a file (preferred when the JSON is large — keeps it out of context).
utils/tmx-analyzer/run.sh assets/tiled_maps/tmx/LEVEL.tmx -o /tmp/level.json

# Narrow rooms[] to a single room (entity_details and entity_classes still cover the whole map).
utils/tmx-analyzer/run.sh assets/tiled_maps/tmx/LEVEL.tmx --room room4
```

Prefer `-o /tmp/<name>.json` for any non-trivial level — the JSON for a dense level can be
thousands of lines, and reading it back via `Read`/`jq` keeps your context tidy.

Search for entities by name via the `name` attribute.

## Sub-agent

For assessments, hand the analysis to an `Explore` sub-agent:

```
Explore sub-agent prompt (level summary / assessment):
  Run: `utils/tmx-analyzer/run.sh assets/tiled_maps/tmx/<FILE>.tmx -o /tmp/<name>.json`
  Read: `/tmp/<name>.json`.
  Summarize: (1) room chain with pixel dimensions, (2) entity counts per room per layer,
  (3) any structural observations (gate placement, spawn types, boss room, etc.).
  Reply a markdown table + bullet notes + relevant information.
```
