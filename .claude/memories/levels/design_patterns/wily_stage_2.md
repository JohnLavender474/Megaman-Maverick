---
name: design-patterns-wily-stage-2
description: Design patterns from Wily Stage 2 — enemy/hazard placement, gap widths, platform sizes, room variety
metadata:
  type: project
---

# Design Patterns: Wily Stage 2

Source file: `assets/tiled_maps/tmx/WilyStage2_v2.tmx`

## Overview
- Theme: Industrial sewage / aquatic hybrid. Main path is a toxic-factory sewer (pipe puffs, toxic barrels, rats, saws). Forked alternate path is fully underwater (sea mines, underwater fans, fish, submarine enemies).
- Total rooms: 15 (room1, room2, room3, room5, room6, fork_room, path1_room1, path2_room1, room4, rodent_man_room, room7, room8, glacier_man_room, room9, boss_room)
- Room size variety: fork_room (43x28 — double-height), path1_room1 (110x23 — widest/tallest combat room), room3 (67x14), room6 (55x14), path2_room1 (84x14). Standard short rooms are 16x14 or 22x14.
- Transition style: mixed — mostly auto/sequential, with a deliberate fork at `fork_room` that branches into path1 (underwater) and path2 (industrial). Two boss rooms: `rodent_man_room` and `glacier_man_room`.

## Room Profiles
| Room | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|--------------|-------------------|-------|
| room1 (22x14) | Bat, RatRobot, ToxicBarrelBot | — | 1 | — |
| room2 (22x14) | Bat, RatRobot, ToxicBarrelBot, SwinginJoe | — | — | — |
| room3 (67x14) | Bat, RatRobot, Ratton, ToxicBarrelBot, FloatingCanHole | Saw, PipePuff | 3, 17 | — |
| room5 (16x14) | — | — | — | — |
| room6 (55x14) | Ratton, ToxicBarrelBot, FloatingCanHole, SwinginJoe | Saw, PipePuff | — | — |
| fork_room (43x28) | SwinginJoe, ToxicBarrelBot, Saw (enemy), RatRobot | Saw, PipePuff | — | — |
| path1_room1 (110x23) | SubmarineJoe, BigFishNeo, YellowTiggerSquirt | SeaMine, UnderwaterFan | 3 | 94x16 tiles |
| path2_room1 (84x14) | ToxicBarrelBot, SwinginJoe, FloatingCanHole | Saw, PipePuff | 1, 2, 1, 2 | — |
| room4 (16x14) | — | — | — | — |
| rodent_man_room (16x14) | — | PipePuff | — | — |
| room7 (25x14) | — | — | — | — |
| room8 (16x14) | — | — | — | — |
| glacier_man_room (16x14) | FloatingCanHole | — | — | — |
| room9 (16x14) | — | — | 3 | — |
| boss_room (16x14) | — | — | — | — |

## Enemy Placement

### Bat
- Platform width when used: none (aerial / ceiling-hung, no platform match)
- Elevated: no (not placed on tiled platforms)
- Typical count per room: 1–2
- Co-occurs with: RatRobot, ToxicBarrelBot in early rooms

### RatRobot
- Platform width when used: none detected (floor-level patrol, wide floor tiles)
- Elevated: no
- Typical count per room: 1–2
- Co-occurs with: Bat, ToxicBarrelBot; also appears in fork_room (double-height room)

### ToxicBarrelBot
- Platform width when used: 1–18 tiles (commonly 2, 10–11 in longer rooms)
- Elevated: yes — consistently placed on raised platforms above floor
- Typical count per room: 1–2
- Co-occurs with: Bat, RatRobot in early rooms; SwinginJoe and FloatingCanHole in later rooms

### SwinginJoe
- Platform width when used: 2–11 tiles; at floor level (8 tiles) once in path2_room1
- Elevated: mostly yes; occasionally floor-level
- Typical count per room: 1–3
- Co-occurs with: ToxicBarrelBot, FloatingCanHole, Ratton

### Ratton
- Platform width when used: 2–14 tiles
- Elevated: yes — always on raised platforms
- Typical count per room: 1–2
- Co-occurs with: Bat, ToxicBarrelBot, FloatingCanHole, SwinginJoe

### FloatingCanHole
- Platform width when used: 1–2 tiles (very narrow ledges); also placed at floor level with no platform
- Elevated: mostly yes, occasionally at floor level
- Typical count per room: 1–3
- Co-occurs with: SwinginJoe, ToxicBarrelBot, Ratton; appears in glacier_man_room (pre-boss room)

### SubmarineJoe
- Platform width when used: none (water section, no platforms)
- Elevated: no
- Typical count per room: 3 (in path1_room1)
- Co-occurs with: BigFishNeo, YellowTiggerSquirt; exclusively in underwater path

### BigFishNeo
- Platform width when used: none (water section)
- Elevated: no
- Typical count per room: 4 (in path1_room1)
- Co-occurs with: SubmarineJoe, YellowTiggerSquirt; exclusively in underwater path

### YellowTiggerSquirt
- Platform width when used: none (water section)
- Elevated: no
- Typical count per room: 5 (in path1_room1)
- Co-occurs with: SubmarineJoe, BigFishNeo; exclusively in underwater path

### Saw (enemy layer)
- Appears in fork_room — distinct from Saw hazard; placed at floor level with no matching platform
- Count: 2 in fork_room
- Co-occurs with: SwinginJoe, RatRobot, ToxicBarrelBot; also coincides with Saw hazards in same room

## Hazard Placement

### Saw (type=p)
- Key property: `type: "p"` (32x32 px)
- Placement: floor/platform level; co-located with enemies in industrial rooms
- Rooms: room3, room6, fork_room, path2_room1
- Count per room: 1–3
- Co-occurs with: PipePuff (always), enemy patrols on nearby platforms

### PipePuff
- Key property: `direction` — all four directions observed (up, down, left, right); 16x16 px
- Placement: ceiling (down), floor (up), walls (left/right); freely mixed in same room
- Rooms: room3, room6, fork_room, path2_room1, rodent_man_room (pre-boss)
- Count per room: 4–16 (highest density in path2_room1 with 19 PipePuffs)
- Co-occurs with: Saw hazards in combat rooms; used alone in boss ante-room (rodent_man_room)

### SeaMine
- Key property: no extra properties; 16x16 px
- Placement: scattered throughout water volume in path1_room1
- Count: 34 mines in a single room (110x23 tiles)
- Co-occurs with: UnderwaterFan, SubmarineJoe, BigFishNeo, YellowTiggerSquirt

### UnderwaterFan
- Key property: `color: "gray"`, direction: down or left; 32x32 px
- Placement: within water area; 2 fans directing current down, 1 directing left
- Count: 3 total in path1_room1
- Co-occurs with: SeaMine; exclusively in underwater path

## Gaps / Pits
- Observed gap widths: 1, 2, 3, 17 tiles (most common: 1–3; one large 17-tile gap in room3)
- Spikes present in pits: no — pits are open falls; no spike hazards detected anywhere in level

## Water
- Pool widths: 94 tiles (one large pool in path1_room1)
- Pool depths: 16 tiles (half the room height of 23)
- Water is exclusive to the alternate fork path (path1_room1); the main path and path2 are entirely dry

## Notable Observations
- The level has a true branch: `fork_room` is a double-height room (43x28) that leads both up to path1 (underwater) and right/down to path2 (industrial), converging later. This is the only forking structure in the stage.
- `path1_room1` is dramatically denser in hazards than any other room (38 hazards — 34 SeaMines + 4 UnderwaterFans — plus 13 enemies), making the underwater path the more hazard-intensive choice.
- PipePuff direction variety increases with room complexity: early rooms use mostly up/down, while path2_room1 and fork_room deploy all four directions simultaneously.
- Boss ante-rooms (`rodent_man_room`, `glacier_man_room`, `room9`) are sparse by design — `rodent_man_room` has only 4 PipePuffs and no enemies; `glacier_man_room` has 2 FloatingCanHoles and no hazards; `room9` has a 3-tile gap but no enemies or hazards.
