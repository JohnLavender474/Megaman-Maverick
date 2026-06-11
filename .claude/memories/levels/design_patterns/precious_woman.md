---
name: design-patterns-precious-woman
description: Design patterns from Precious Woman — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Precious Woman

Source file: `assets/tiled_maps/tmx/PreciousWoman_v2.tmx`

## Overview
- Theme: Crystal/gem mining — enemies and hazards use the "Precious" prefix; DrillHead adds a drilling motif
- Total rooms: 8 (room1–room6, room7, boss_room); 5 main combat rooms, 1 vertical shaft, 1 gate corridor, 1 boss room
- Standard room size: 77–91 × 14 tiles (horizontal); room2 is a vertical shaft 16 × 38
- Transition style: Mixed — Ladders connect room3→room4 and room4→room5 (vertical traversal); Gates gate room6→room7 and room7→boss_room

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|--------------|-------------|--------------|-------------------|-------|
| room1 | 77×14 | PreciousGemCanon, DrillHead | PreciousSpike | 3, 3, 3, 5, 5 | none |
| room2 | 16×38 | PreciousGemCanon | LaserBeamer | — (vertical shaft) | none |
| room3 | 78×14 | PreciousGemCanon, DrillHead | LaserBeamer, PreciousSpike | 2, 3, 4, 4, 5, 5, 7, 10, 10 | none |
| room4 | 16×14 | — | — | — (ladder corridor) | none |
| room5 | 91×14 | PreciousTron, PreciousGemCanon, DrillHead | LaserBeamer, PreciousSpike | 2 | none |
| room6 | 88×14 | PreciousTron, PreciousGemCanon | LaserBeamer, PreciousSpike | 2, 3, 4, 12 | none |
| room7 | 16×14 | — | — | — (gate corridor) | none |
| boss_room | 16×14 | PreciousWoman (boss) | — | — | none |

## Enemy Placement

### DrillHead
- Platform under feet: never detected on a named block platform (plat_w_tiles = null in all instances) — likely spawns in open floor areas or descends from ceiling
- Elevated: never
- Rooms: 1, 3, 5 (first three combat rooms only; absent from rooms 6+)
- Typical count per room: 2–4 in room1, 2 in room3, 2 in room5
- Co-occurs with: PreciousGemCanon always

### PreciousGemCanon
- Platform width when on a block: 4–8 tiles typically (observed: 4, 5, 6, 7, 8); one outlier at 44 tiles (room6, the wide main floor)
- Elevated platforms: yes, in rooms 2 and 5 (2–4 tile platforms)
- Rooms: all 5 combat rooms (room1–room3, room5–room6); only enemy in room2
- Typical count per room: 4 in room1, 1 in room2, 1 in room3, 1 in room5, 1 in room6
- Co-occurs with: DrillHead (rooms 1, 3, 5), PreciousTron (rooms 5, 6)

### PreciousTron
- Uses Position waypoints (Position2–Position9 named entities) to define patrol paths — not placed on small named platforms in rooms 5
- Elevated on 3-tile platform in room6
- Rooms: 5, 6 (second half of level only)
- Typical count per room: 3 in room5, 1 in room6
- Co-occurs with: PreciousGemCanon always, DrillHead in room5

## Hazard Placement

### PreciousSpike
- Directions observed: none specified (floor spikes), "down" (ceiling spikes), "right" (right-wall spikes), "left" (left-wall spikes)
- Floor spikes: width 160–1152 px (5–36 tiles), height 32 px (1 tile)
- Ceiling spikes ("down"): width 192–576 px (6–18 tiles), height 32 px (1 tile)
- Wall spikes ("right"/"left"): width 32 px (1 tile), height 32–128 px (1–4 tiles)
- Placement: room3 is the spike gauntlet — 9 PreciousSpike instances using all four directions; room5 has 3 large floor spike strips (up to 36 tiles wide)
- Co-occurs with: LaserBeamer (rooms 3, 5, 6), always paired with floor gaps
- No spikes in floor pits — spikes are surface hazards only

### LaserBeamer
- Size: always 32×32 px (1×1 tile footprint)
- radius property: always 20
- spawn_type: always "spawn_room" (activates when player enters room)
- Directions: default (unspecified), "left", "down"; up to 8 obstacle references per instance
- Escalation across rooms: 2 in room2, 1 in room3, 1 in room5, 4 in room6 (peak density at end)
- Rooms: 2, 3, 5, 6; absent from room1 (introductory room)
- Co-occurs with: PreciousGemCanon always; PreciousSpike in rooms 3, 5, 6

## Gaps / Pits
- Observed gap widths: 2–12 tiles
- Early rooms (1): gaps cluster at 3–5 tiles
- Mid rooms (3): wide gaps introduced — up to 10 tiles; greatest variety (9 distinct gaps)
- Late rooms (6): single large gap of 12 tiles amid smaller 2–4 tile gaps
- No spike pits — spike hazards are placed on surfaces beside gaps, not inside them

## Water
- No water entities anywhere in the level

## Notable Observations
- DrillHead is an early-game-only enemy in this level: it appears only in rooms 1, 3, and 5 and is completely absent from room6 and the boss. PreciousTron takes over as the primary mobile threat in the back half.
- Room2 is a unique 16×38 vertical shaft traversed by Ladder, containing only a single PreciousGemCanon and two LaserBeamers — it functions as a brief vertical challenge break between the two main horizontal runs.
- Room3 is the spike gauntlet: 9 PreciousSpike instances in a 78-tile-wide room use all four directions (floor, ceiling, left wall, right wall), combined with 10-tile floor gaps and a LaserBeamer, making it the most hazard-dense room.
- Room6 contains a HeartTank (items layer), serving as a reward room immediately before the boss Gate sequence; it also has the highest LaserBeamer count (4) and the widest single gap (12 tiles).
