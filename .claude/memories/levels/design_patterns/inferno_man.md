---
name: design-patterns-inferno-man
description: Design patterns from Inferno Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Inferno Man

Source file: `assets/tiled_maps/tmx/InfernoMan_16x14_v2.tmx`

## Overview
- Theme: Fire / lava industrial — furnaces (InfernoOven), flowing lava (LavaRiver), spinning flame wheels
  (FireballBar), meteor showers (InfernoMeteorShower), and rising lava (RisingLavaRiver)
- Total rooms: 13 (room1–room11, mini_boss_room, boss_room)
- Standard room size: 16×14 tiles (512×448 px); several oversized rooms exist (see table)
- Transition style: gate (all inter-room passages are Gate sensors; mini_boss_room→room9 gate requires boss_key=1)
- No Water entities anywhere in the level (specials layer contains only Ladder objects)

## Room Profiles

| Room           | Size (tiles) | Enemy types                            | Hazard types                                             | Floor pits (tiles wide) | Notes                                                                             |
|----------------|--------------|----------------------------------------|----------------------------------------------------------|-------------------------|-----------------------------------------------------------------------------------|
| room1          | 143×14       | Popoheli, FireDispensenator, SniperJoe | LavaRiver, InfernoOven, FireballBar                      | 13, 13, 19              | Starting room; long horizontal; 3 lava pits with InfernoIceBlocks                 |
| room2          | 16×14        | SniperJoe                              | InfernoOven                                              | 0                       | Short vertical connector; 1 oven fires downward                                   |
| room3          | 29×14        | FireDispensenator                      | InfernoOven, FireballBar                                 | 0                       | Connecting room with ladder; 2 ovens                                              |
| room4          | 109×14       | DemonMet                               | InfernoMeteorShower, FireballBar, LavaRiver, InfernoOven | 13                      | Long horizontal; meteor shower + 6 FireballBars + lava pit; InfernoChainPlatforms |
| room5          | 16×14        | none                                   | none                                                     | 0                       | Rest/transition room; checkpoint 2                                                |
| room6          | 16×41        | FireDispensenator, DemonMet, SniperJoe | RisingLavaRiver, FireballBar                             | 0                       | Tall vertical room; rising lava chase                                             |
| room7          | 16×26        | FireDispensenator, SniperJoe           | FireballBar, InfernoOven                                 | 0                       | Tall room; 3 FireballBars staggered vertically                                    |
| room8          | 16×14        | none                                   | none                                                     | 0                       | Post-mini-boss corridor; checkpoint 4                                             |
| mini_boss_room | 16×14        | MechaDragon (mini boss)                | LavaRiver (mostly inactive)                              | 0                       | MechaDragon fight; InfernoIceBlocks over lava at bottom                           |
| room9          | 26×14        | Popoheli, FireDispensenator            | LavaRiver, FireballBar                                   | ~18                     | Wide lava floor; InfernoIceBlock (16 tiles) over pit; InfernoChainPlatform        |
| room10         | 36×38        | DemonMet, FireDispensenator            | RisingLavaRiver, FireballBar                             | 0                       | Large room; second rising-lava chase; FireballBlock trigger                       |
| room11         | 16×14        | none                                   | none                                                     | 0                       | Pre-boss corridor; FrozenBlocks (2×1-tile stacks)                                 |
| boss_room      | 16×14        | InfernoMan (boss)                      | LavaRiver (inactive until fight), MeteorSpawner          | 0                       | Boss fight; LavaRiver activates during fight                                      |

## Enemy Placement

### FireDispensenator
- Size: 16×16 px
- Placed on a ledge or block face (y aligned with the top of the supporting surface)
- Each instance has a `scanner` child object that sets the detection range
- Has an `ignore_blocks` property listing block IDs the shots should pass through
- Rooms: room1 (3 normal + 2 hard_mode), room3 (1), room6 (3), room7 (1), room9 (1), room10 (2), room4 (1 hard_mode)
- Co-occurs with: FireballBar (most rooms), InfernoOven (rooms 1, 3, 7), DemonMet (room6, room10)
- Typical count per room: 1–3

### Popoheli
- Size: 32×32 px
- Always paired with a Trigger child and is marked `triggerable=true` — dormant until the player crosses the trigger
- Rooms: room1 (2), room9 (1)
- Co-occurs with: FireDispensenator (room1), LavaRiver pits (room1), FireballBar (room9)
- Typical count per room: 1–2

### SniperJoe (type=fire)
- Size: 16×16 px
- All instances use `type=fire` (fire shield variant)
- Rooms: room1 (1, on elevated block over lava pit area), room2 (1), room6 (1), room7 (1), room4 (1 hard_mode)
- Platform width when used: not elevated in most cases (floor level); room2 instance is on a 16-tile-wide standard platform
- Co-occurs with: FireDispensenator (rooms 6, 7), FireballBar (room6), InfernoOven (rooms 2, 7)
- Typical count per room: 1

### DemonMet
- Size: 16×16 px
- Has 1–2 `target` child objects that define the patrol or flight path
- Rooms: room4 (1), room6 (1), room10 (2)
- Platform width when used: placed on ledge/platform above floor; room4 instance is on elevated block ~4 tiles above floor
- Co-occurs with: FireballBar (rooms 4, 6, 10), FireDispensenator (rooms 6, 10)
- Typical count per room: 1–2

## Hazard Placement

### FireballBar
- Size: 32×32 px (pivot point; arm extends outward)
- Key property: `angle` (float) — only one instance (room1, id=122) uses angle=90 for a different initial spin phase; all others use default angle
- Placement: mounted on a block face — wall, elevated block, or ledge; always at the block coordinate (not offset)
- Most common placement: on the face of a raised block above the main floor, approximately 1–2 tiles above ground
- Room occurrences (normal mode): room1 (5), room3 (1), room4 (6), room6 (1), room7 (3), room9 (2), room10 (1)
- Hard-mode-only FireballBars: room1 (+7), room3 (+2), room4 (+3), room6 (+1) — significant additional density
- Co-occurs with: InfernoOven (rooms 1, 3, 7), InfernoMeteorShower (room4), RisingLavaRiver (rooms 6, 10)

### InfernoOven
- Size: 64×64 px (2×2 tiles)
- Key property: `direction` — "down" fires downward, "up" fires upward, absent/default fires horizontally
- Placement: mounted flush on block face in direction of fire; ceiling-mount for direction=down, floor-mount for direction=up
- Rooms: room1 (4: 2 default + 2 down), room2 (1 down), room3 (2 default), room4 (1 up), room7 (1 up)
- Co-occurs with: FireballBar (rooms 1, 3, 7), LavaRiver pits (room1), FireDispensenator (rooms 1, 7)

### LavaRiver
- Size per segment: 64×32 px (top tiles) or 32×32 px (fall_start tiles); spawned via spawn_room
- Key properties: `type` ("top" or "fall_start"), `left` (bool, direction of flow), `active` (bool, default true)
- Placement: floor-level pit hazard; always accompanied by InfernoDeath or Death sensors in the same x-range
- Room occurrences: room1 (3 pits), room4 (1 pit), room9 (1 wide pit), mini_boss_room (mostly inactive), boss_room (all inactive until fight)
- InfernoIceBlocks are placed directly over every LavaRiver pit as an alternate traversal surface
- Co-occurs with: InfernoChainPlatform (over pits in rooms 1 and 4)

### InfernoMeteorShower
- Single instance in room4, placed at the room's top-left corner (x=4288, y=1152)
- References 9 spawner child objects (spawner1–spawner11 with some gaps) spread across the full 109-tile room width
- This is a room-wide ceiling hazard — the only InfernoMeteorShower in the level
- Co-occurs with: FireballBar (×6), LavaRiver (floor pit), DemonMet, InfernoChainPlatform

### RisingLavaRiver
- Two instances: room6 (16×41 tiles) and room10 (36×38 tiles)
- Each instance is a pair: one "inner" object (full body) + one "top" object (lava surface row)
- Room6 rising lava: width=1024 px (32 tiles), height=512 px (16 tiles body), rise_room="room6"
- Room10 rising lava: width=1152 px (36 tiles), height=512 px (16 tiles body), rise_room="room10"
- spawn_type="spawn_now" — always present, not room-gated
- Co-occurs with: FireballBar (both rooms), FireDispensenator (both rooms), DemonMet (room10)

### InfernoChainPlatform
- Size: 64×64 px (2×2 tiles)
- Always uses `spawn_type=spawn_room` with its room name
- Placed directly over lava pits as the traversal surface above the LavaRiver
- Room1: 7 chain platforms spaced irregularly across the 3 pits (at x=960, 1120, 1824, 2112, 3328, 3520, 3744)
- Room4: 6 chain platforms spaced across the 1 pit and nearby area (at x=5088, 5216, 5376, 5856, 5984, 6304)
- Room9: 1 chain platform at x=9760

## Gaps / Pits

- Room1 observed gap widths: 13 tiles (×2), 19 tiles (×1); all 3 pits are InfernoDeath (lethal lava)
- Room4 observed gap width: 13 tiles (×1); InfernoDeath
- Room9 observed gap width: ~18 tiles; lava tiles partially active=false (some sections inactive by default)
- Lava hazards confirmed in pits: yes — InfernoDeath sensors over all active lava pits; instant kill
- InfernoIceBlocks provide destructible/freezable bridging surfaces over every pit; placed at floor level (y ≈ room_bottom - 32px)

## Water

- No water pools anywhere in the level
- The specials layer contains only Ladder objects (3 ladders in the room1/room2/room3 vertical section)

## Notable Observations

- FireballBar is the dominant recurring hazard — it appears in 7 of 13 rooms (normal mode) and hard mode nearly doubles its count in rooms 1 and 4. When designing similar rooms, expect 1–6 FireballBars per combat room.
- The level uses two distinct large-room archetypes: wide horizontal scrollers (room1 at 143 tiles, room4 at 109 tiles) for gap-jumping sections, and tall vertical rooms (room6 at 41 tiles, room10 at 38 tiles) for rising-lava chases — each archetype has exactly one RisingLavaRiver.
- InfernoIceBlock is a stage-specific block placed over every LavaRiver pit. It covers the full pit width (matching the InfernoDeath zone width exactly) at floor depth. This is unique to InfernoMan; no other stage in the sample uses this pattern.
- MechaDragon mini-boss (mini=true) gates room9. Its properties include 7 block references, hover/charge target pairs, and a block_on_spawn. The mini_boss_room lava (y=1632) is mostly inactive (active=false) except for 2 central tiles — lava activates as part of the fight.
- Hard-mode-only content is substantial: 9 FireballBars, 6 FireballBlocks, and 3 FireDispensenators are flagged `hard_mode_only=true`, adding significant density to rooms 1, 3, and 4.
