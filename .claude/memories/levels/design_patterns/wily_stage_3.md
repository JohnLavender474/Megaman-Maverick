---
name: design-patterns-wily-stage-3
description: Design patterns from Wily Stage 3 — enemy/hazard placement, gap widths, platform sizes, room variety
metadata:
  type: project
---

# Design Patterns: Wily Stage 3

Source file: `assets/tiled_maps/tmx/WilyStage3_v4.tmx`

## Overview
- Theme: Fire/Inferno (InfernoOven, FireballBar, InfernoMan boss) mixed with ice enemies (IceFox)
- Total rooms: 16 (room1–room9, intro_room, intro_room_2, inferno_man_room, moon_man_room, timber_woman_room, precious_woman_room, wily_room)
- Room size variety:
  - Standard: 16x14 tiles (most transition/boss rooms)
  - Wide horizontal: 52x14 (room1), 32x14 (room2), 113x63 (room3)
  - Tall vertical: 16x31 (intro_room), 16x62 (room9), 16x137 (room5)
- Transition style: gate (sensors:Gate entities present at room exits)
- Structure: horizontal gauntlet (rooms 1–3) → InfernoMan boss → tall vertical shaft (room5) → MoonMan boss → re-match boss corridor (timber_woman_room, precious_woman_room) → Wily boss

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|-------------|--------------|-------------------|-------|
| intro_room | 16x31 | none | none | none | no |
| intro_room_2 | 16x14 | none | none | none | no |
| room1 | 52x14 | Bat, GreenUziJoe, ShieldAttacker, IceFox | WanaanLauncher | 3, 7, 9, 9 | no |
| room2 | 32x14 | ShieldAttacker, IceFox | InfernoOven, WanaanLauncher | none | no |
| room3 | 113x63 | ShieldAttacker, IceFox | WanaanLauncher, FireballBar, InfernoOven | none | no |
| room4 | 16x14 | none | none | none | no |
| inferno_man_room | 16x14 | none | none | none | no |
| room5 | 16x137 | IceFox, AstroAssAssaulter, ShieldAttacker, BunbyTank | WanaanLauncher, FireballBar, InfernoOven, Saw | none | no |
| room6 | 16x14 | none | none | none | no |
| moon_man_room | 16x14 | none | none | none | no |
| timber_woman_room | 16x14 | none | Spike | none | no |
| precious_woman_room | 16x14 | none | Spike | none | no |
| room7 | 16x14 | none | none | none | no |
| room8 | 16x14 | none | none | 4 | no |
| wily_room | 16x14 | none | none | none | no |
| room9 | 16x62 | none | none | 4 | no |

## Enemy Placement

### Bat
- No platform detected (ceiling-hanging or flying)
- Appears only in room1, count: 2
- Co-occurs with: GreenUziJoe, ShieldAttacker, IceFox, WanaanLauncher

### GreenUziJoe
- Platform width when used: 4 tiles
- Elevated above floor level
- Appears only in room1, count: 1 (paired with ConveyorBelts and wide gaps)
- Co-occurs with: Bat, ShieldAttacker, IceFox

### ShieldAttacker
- No platform detected (flying/ceiling type — always elevated: false, plat: null)
- Appears in room1 (2), room2 (1), room3 (3), room5 (2)
- Typical count per room: 1–3
- Co-occurs with: IceFox, WanaanLauncher, InfernoOven, FireballBar

### IceFox
- Ground patrol on elevated platforms; platform widths: 2–13 tiles (commonly 3–8)
- Always placed on elevated platforms above floor
- Appears in rooms 1, 2, 3, 5; count per room: 1–4
- Co-occurs with: ShieldAttacker across all rooms; AstroAssAssaulter, BunbyTank in room5

### AstroAssAssaulter
- Elevated platforms only, narrow: 2–4 tiles wide
- Exclusive to room5 (16x137 vertical shaft), count: 3
- Co-occurs with: BunbyTank, IceFox, ShieldAttacker, full hazard suite

### BunbyTank
- Ground patrol on elevated platforms; platform widths: 3–11 tiles
- Exclusive to room5, count: 4
- Co-occurs with: AstroAssAssaulter, IceFox, ShieldAttacker, full hazard suite

## Hazard Placement

### WanaanLauncher
- Size: 64x32 px (2x1 tiles); each instance has a linked `sensor` child object (child id in properties)
- Appears in rooms 1, 2, 3, 5; typically 5–6 per large room
- Placement: floor/platform level (launch projectiles upward)
- Co-occurs with: InfernoOven, FireballBar in rooms 3 and 5

### InfernoOven
- Size: 64x64 px (2x2 tiles); `direction` property: "up", "down", or "left"
- spawn_type: spawn_room (respawns when room re-entered)
- Appears in rooms 2, 3, 5; 3–6 per room
- Directions used: up and down paired (crossfire); left seen in room3
- Co-occurs with: WanaanLauncher, FireballBar

### FireballBar
- Size: 32x32 px (1x1 tile pivot point); spawn_type: spawn_room
- Appears in rooms 3 (6 instances) and 5 (2 instances)
- Co-occurs with: WanaanLauncher, InfernoOven

### Saw
- Size: 32x32 px (1x1 tile); type: "p" (path-based saw); spawn_type: spawn_room
- Exclusive to room5, count: 3
- Co-occurs with: full hazard suite (WanaanLauncher, InfernoOven, FireballBar) and dense enemies

### Spike
- Size: 32x32 px (1x1 tile); direction: "down" (ceiling spikes); region: "Spike5"; instant: false
- Exclusive to boss re-match rooms (timber_woman_room, precious_woman_room): 4 ceiling spikes each
- No enemies in these rooms — spikes are the only obstacle before the boss encounter

## Gaps / Pits

- Observed gap widths: 3, 4, 7, 9 tiles
- Large gaps (7–9 tiles) in room1 alongside ConveyorBelts — conveyor belts likely assist crossing
- 4-tile gaps in room8 and room9 (final corridor before Wily)
- No spike pits detected in analysis (spikes are only in boss rooms as ceiling hazards)

## Water
- No water present in any room

## Notable Observations
- room1 uses 5 ConveyorBelt blocks in combination with large floor gaps (up to 9 tiles wide), making gap traversal a puzzle rather than a pure platforming challenge
- room5 (16x137 tiles) is the densest room: 14 enemies and 17 hazards in a single tall vertical shaft — the only room with BunbyTank, AstroAssAssaulter, and Saw together
- The level functions as a boss gauntlet: InfernoMan, MoonMan, and two re-match boss rooms (TimberWoman, PreciousWoman) before the Wily encounter, following classic Wily Castle design
- WanaanLauncher always carries a sensor child object reference in its properties — the sensor likely defines the detection zone and is required for the launcher to function
