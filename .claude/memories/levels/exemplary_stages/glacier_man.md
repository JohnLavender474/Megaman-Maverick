---
name: design-patterns-glacier-man
description: Design patterns from Glacier Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Glacier Man

Source file: `assets/tiled_maps/tmx/GlacierMan_16x14_v2.tmx`

## Overview
- Theme: Ice/snow with an underwater mid-section. Ice-themed enemies (SnowheadThrower, IceSkaterPeng, IceFox, Coldier, IcicleTelly, Sealion), aquatic enemies (Tropish, BigFishNeo, YellowTiggerSquirt, Eyee) in the underwater room.
- Total rooms: 13 (room1–room12, boss_room)
- Standard room size: 16×14 tiles; large rooms are 143×14 (room1), 75×14 (room6, room11), 118×14 (room10), 136×25 (room8 — underwater, taller)
- Transition style: mixed — small 16×14 rooms between long corridors act as gates/transitions (room2–room5, room7, room9, room12)

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|-------------|--------------|-------------------|-------|
| room1 | 143×14 | SnowheadThrower, IceSkaterPeng, ShieldAttacker, Sealion, SpikeBot, IcicleTelly | — | 3, 33, 11, 3, 13 | small pools (3–4 wide, 2–3 deep); large pools (15 wide×3, 22 wide×3) |
| room2 | 16×14 | — | — | — | 22×3 (transition pool) |
| room3 | 16×14 | IceFox | — | — | — |
| room4 | 16×14 | IceFox, ShieldAttacker | — | — | — |
| room5 | 16×14 | Coldier | — | — | — |
| room6 | 75×14 | IceFox, Coldier, Bat, ShieldAttacker | IceCubeMaker ×2 | 10 | — |
| room7 | 16×14 | — | — | — | 16×27 (full-room, transition to underwater) |
| room8 | 136×25 | Tropish, BigFishNeo, YellowTiggerSquirt, SpikeBot, Eyee | UnderwaterFan ×7, SeaMine ×37 | 15, 31 | full underwater (multiple 16×27 segments) |
| room9 | 16×14 | — | — | — | 16×19 + 11×19 |
| room10 | 118×14 | SnowheadThrower, Coldier, ShieldAttacker, IceSkaterPeng, Bat, IceFox, SpikeBot | IceCubeMaker ×2 | 9, 3, 4, 3, 10, 3 | — |
| room11 | 75×14 | SnowheadThrower, BigFishNeo, IceSkaterPeng, Sealion, YellowTiggerSquirt, SpikeBot, IcicleTelly | — | — | 39×8 (large pool) + 3×8 |
| room12 | 16×14 | — | — | — | — |
| boss_room | 16×14 | — | — | — | — |

## Enemy Placement
### SnowheadThrower
- Platform width when used: 2–6 tiles (consistently elevated)
- Elevated: yes — always placed on raised platforms above floor level
- Typical count per room: 1–4
- Co-occurs with: SpikeBot, IceSkaterPeng (room1); Coldier, IceSkaterPeng, Bat, SpikeBot, IceFox (room10)

### IceSkaterPeng
- Platform width when used: 7–41 tiles (needs wide surfaces to skate)
- Elevated: mixed — elevated in room1, floor-level on large flat sections in room10
- Typical count per room: 1–2
- Co-occurs with: SnowheadThrower, SpikeBot (room1, room10)

### SpikeBot
- Platform width when used: 2–7 tiles (small elevated platforms); also floor-level on very wide platforms (21 tiles)
- Elevated: mostly yes
- Typical count per room: 1–5
- Co-occurs with: SnowheadThrower, IceSkaterPeng, Bat throughout

### Coldier
- Platform width when used: 4–21 tiles; appears both elevated and floor-level
- Elevated: mixed
- Typical count per room: 1–5
- Co-occurs with: IceFox, Bat, ShieldAttacker (room6, room10)

### IceFox
- Platform width when used: 2–9 tiles; consistently elevated
- Elevated: yes
- Typical count per room: 1–2
- Co-occurs with: Coldier, ShieldAttacker, Bat (room6, room10); solo or with ShieldAttacker in transition rooms (room3, room4)

### IcicleTelly
- No platform (floating enemy)
- Elevated: n/a
- Typical count per room: 2–3
- Co-occurs with: SnowheadThrower, SpikeBot, IceSkaterPeng (room1, room11)

### Sealion
- No platform (water-based / floor-hugging)
- Typical count per room: 1–2
- Co-occurs with: SnowheadThrower, IceSkaterPeng (room1, room11)

### ShieldAttacker
- No platform detected (floor-level, ground-pacing)
- Typical count per room: 1–2
- Co-occurs with: IceFox, Coldier, Bat (mid section); alone in small transition rooms

### Bat
- No platform (flying)
- Typical count per room: 3–6
- Co-occurs with: Coldier, IceFox (room6, room10)

### Tropish
- No platform (underwater swimmer)
- Typical count per room: 3
- Co-occurs with: BigFishNeo, YellowTiggerSquirt, Eyee (room8 only)

### BigFishNeo
- No platform (underwater swimmer)
- Typical count per room: 2
- Co-occurs with: Tropish, YellowTiggerSquirt, Eyee (room8); also in room11 on floor near water

### YellowTiggerSquirt
- No platform (underwater shooter)
- Typical count per room: 2–5
- Co-occurs with: Tropish, BigFishNeo, Eyee (room8); also in room11 near water

### Eyee
- No platform (underwater floater)
- Typical count per room: 3–4
- Co-occurs with: Tropish, BigFishNeo, YellowTiggerSquirt (room8 only)

## Hazard Placement
### IceCubeMaker
- Size: 32×32 (1 tile)
- Key properties: `run_bounds` (object reference), `ignore_hit_1` and `ignore_hit_2` (object references to blocks or bounds), `spawn_room`, `spawn_type: spawn_room`
- Always placed in pairs (2 per room); the two in a room share a run_bounds region
- Placement: mid-section long corridors (room6, room10) — never in transition rooms or the opening room
- Co-occurs with: IceFox, Coldier, Bat, ShieldAttacker

### UnderwaterFan
- Size: 32×32 (1 tile)
- Directions observed: up, down, right
- Placement: room8 (underwater section only), scattered throughout the corridor
- Count per room: 7 (room8)
- Co-occurs with: SeaMine, aquatic enemies

### SeaMine
- Size: 16×16 (half-tile)
- Properties: some have `room: room7` (pre-placed in adjacent room for cross-room effect); many are `hard_mode_only: true`
- Normal-mode mines: ~7 (including the room7-attributed ones); hard-mode-only mines: ~30
- Placement: room8 (underwater) only — densely packed in hard mode
- Co-occurs with: UnderwaterFan, aquatic enemies

## Gaps / Pits
- Observed gap widths: 3, 4, 9, 10, 11, 13 tiles (common); 15, 31, 33 tiles (large — room1 and room8)
- The 33-tile gap in room1 is unusually wide (likely a water pit or a fall section)
- Spikes in pits: not detected by analysis (no spike hazard layer entries in floor regions)
- Small gaps (3–4 tiles) are the most common in room10; room1 has more varied and wider gaps

## Water
- Small surface pools (room1): 3–4 tiles wide, 2–3 tiles deep — decorative/hazard pools along floor
- Medium transition pool (room1/room2): 15–22 tiles wide, 3 tiles deep
- Underwater transition rooms (room7, room9): full-width pools 16 tiles wide, 19–27 tiles deep — these mark the entry/exit of the underwater section
- Full underwater corridor (room8): multiple 16-wide×27-deep segments covering the 136-tile corridor; room is 25 tiles tall (not the standard 14)
- Aquatic mid-section pool (room11): 39 tiles wide × 8 tiles deep (large navigable pool with aquatic enemies inside)

## Notable Observations
- The level is structured in two distinct halves: an ice/snow surface section (rooms 1–6) and a return section (rooms 10–11) separated by a full underwater corridor (room8, 136×25 tiles).
- SeaMines are aggressively gated behind hard mode: ~30 of ~37 SeaMines in room8 carry `hard_mode_only: true`, making the underwater section dramatically more dangerous on hard difficulty.
- IceCubeMaker always appears in pairs sharing a `run_bounds` reference and always in the longer corridors (75+ tiles), never in small transition or intro rooms.
- The `blocks:Sealion` entry in room1's entity_counts suggests at least one Sealion is spawned from a block (or listed under blocks layer by mistake) — worth verifying if Sealion has a blocks-layer spawn variant.
- room1 contains `projectiles:FallingIcicle` (5 instances) listed under the projectiles layer rather than hazards — these are pre-placed falling icicles, a pattern not seen in other rooms.
