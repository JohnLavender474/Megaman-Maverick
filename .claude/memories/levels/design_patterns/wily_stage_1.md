---
name: design-patterns-wily-stage-1
description: Design patterns from Wily Stage 1 (WilyStage1_v2.tmx) — enemy/hazard placement, gap widths, platform sizes, room variety
metadata:
  type: project
---

# Design Patterns: Wily Stage 1

Source file: `assets/tiled_maps/tmx/WilyStage1_v2.tmx`

## Overview
- Theme: Industrial/fortress recycling enemies from previous robot master stages (ReactorMan and DesertMan appear as mid-bosses). Dark zone in the second half suggests a power-outage or underground environment.
- Total rooms: 13 (room1–room10, reactor_man_room, desert_man_room, boss_room)
- Room size variety:
  - Standard corridor: 16x14 tiles (rooms 6, 9, 10, boss_room, antechambers)
  - Wide horizontal corridors: room1 (104x14), room7 (125x14), room8 (67x14), room3 (57x14), room5 (51x14)
  - Tall vertical shaft: room2 (58x28), room4 (16x36)
- Transition style: Gate-based throughout. Six Gate objects total; two carry `boss_key` properties (boss_key=1 after reactor_man_room opens room7; boss_key=2 after desert_man_room opens room10/boss_room). One Gate has `direction: up` for the final upward entry into boss_room.

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Special mechanics |
|------|-------------|-------------|--------------|-------------------|-------------------|
| room1 | 104x14 | GreenUziJoe, SpikeCopter, DuoBallCanon | WanaanLauncher | 4 | EventTrigger |
| room2 | 58x28 | ShieldAttacker, DragonFly | — | none | 5x PropellerPlatform (vertical ascent) |
| room3 | 57x14 | GreenUziJoe, ShieldAttacker | LaserBeamer, AcidGoopSupplier, WanaanLauncher | 2, 4, 6 | Ladders |
| room4 | 16x36 | GreenUziJoe, ShieldAttacker | LaserBeamer, AcidGoopSupplier | none | PropellerPlatform, Ladder (vertical shaft) |
| room5 | 51x14 | DuoBallCanon, GreenUziJoe | LaserBeamer, AcidGoopSupplier | 2 | PropellerPlatform |
| room6 | 16x14 | — | — | none | Antechamber (EventTrigger) |
| reactor_man_room | 16x14 | — (ReactorMan boss) | AcidGoopSupplier (2) | none | Mid-boss room |
| room7 | 125x14 | GreenUziJoe, DuoBallCanon, DragonFly | WanaanLauncher | 2, 10, 12, 31 | 3x PropellerPlatform, FeetRiseSinkBlock+QuickSand |
| room8 | 67x14 | DuoBallCanon, BulbBlaster, GreenUziJoe | LaserBeamer | 4, 7, 11 | 3x FeetRiseSinkBlock+QuickSand, DarknessV2, LaserBeamer with light_keys |
| room9 | 16x14 | — | — | none | DarknessV2 antechamber |
| desert_man_room | 16x14 | BulbBlaster (2) | — | none | FeetRiseSinkBlock+QuickSand, DarknessV2, mid-boss room |
| room10 | 16x14 | — | — | 3 | DarknessV2 antechamber |
| boss_room | 16x14 | — (ElecDevil boss) | — | none | DarknessV2, entered via upward Gate |

## Enemy Placement

### GreenUziJoe
- Platform width when used: 3–6 tiles (one outlier on a 31-tile floor block); almost always elevated
- Elevated: yes in the majority of placements; occasionally on floor-level platforms
- Typical count per room: 1–3
- Co-occurs with: DuoBallCanon (rooms 1, 5, 8), WanaanLauncher (rooms 1, 3, 7), LaserBeamer (rooms 3, 4, 5, 8)
- Rooms: 1, 3, 4, 5, 7, 8

### DuoBallCanon
- Platform width when used: 2–4 tiles; always elevated
- Elevated: yes (all observed instances)
- Typical count per room: 1–3
- Co-occurs with: GreenUziJoe (rooms 1, 5, 7, 8), DragonFly (room 7)
- Rooms: 1, 5, 7, 8

### SpikeCopter
- Aerial enemy — no platform; not elevated relative to floor
- Typical count per room: 4 (all in room1)
- Co-occurs with: GreenUziJoe, DuoBallCanon, WanaanLauncher
- Rooms: 1 only

### DragonFly
- Aerial enemy — no platform detected in any instance
- Typical count per room: 3–4
- Co-occurs with: ShieldAttacker (room 2), GreenUziJoe + DuoBallCanon (room 7)
- Rooms: 2, 7

### ShieldAttacker
- No platform detected (possibly mid-air or wall-hugging placement)
- Typical count per room: 3–5
- Co-occurs with: DragonFly (room 2), GreenUziJoe (rooms 3, 4)
- Rooms: 2, 3, 4

### BulbBlaster
- No platform detected (ceiling-mounted turret)
- Typical count per room: 2–4
- Co-occurs with: DuoBallCanon + GreenUziJoe (room 8); solo in desert_man_room antechamber
- Rooms: 8, desert_man_room

## Hazard Placement

### WanaanLauncher
- Size: 64×32 px (2 tiles wide, 1 tile tall)
- Each instance references an external `sensor` object by id to determine fire trigger
- Placement: floor-mounted
- Typical count per room: 3
- Co-occurs with: GreenUziJoe, DuoBallCanon, SpikeCopter (room 1); GreenUziJoe + LaserBeamer (room 3); DragonFly + GreenUziJoe (room 7)
- Rooms: 1, 3, 7

### LaserBeamer
- Size: 32×32 px (1 tile)
- Key properties: `radius: 20.0`, `ignore1` (object reference), `spawn_room` + `spawn_type: spawn_room`
- Room8 variant has `light_keys: 1` — destroying it toggles the DarknessV2 lighting key
- Typical count per room: 1–2
- Co-occurs with: AcidGoopSupplier (rooms 3, 4, 5), WanaanLauncher (room 3)
- Rooms: 3, 4, 5, 8

### AcidGoopSupplier
- Size: 16×8 px (sub-tile)
- No extra properties observed (plain instances)
- Typical count per room: 2–3
- Co-occurs with: LaserBeamer (rooms 3, 4, 5); alone in reactor_man_room (2 units flanking the boss area)
- Rooms: 3, 4, 5, reactor_man_room

## Gaps / Pits
- Observed gap widths: 2, 3, 4, 6, 7, 10, 11, 12, 31 tiles
- Room7 contains a 31-tile gap — the widest in the level; it is bridged by PropellerPlatforms
- Room3 has the densest gap sequence: three gaps (2, 4, 6 tiles) in a 57-tile corridor
- No spike hazards observed in pits (falls are lethal by death sensor, not spikes)

## Water
- No water pools found in this level.

## Special Mechanics

### PropellerPlatform
- Size: 32×8 px (1 tile wide, 0.25 tile tall)
- Movement: horizontal oscillation defined by trajectory property (e.g. `2,0,4;0,0,1;-2,0,4;0,0,1` = move at 2 tiles/s for 4s, pause 1s, return)
- Used exclusively for vertical traversal in rooms 2 and 4, and over large gaps in rooms 5 and 7
- Count: 5 in room2, 1 in room4, 1 in room5, 3 in room7 (total 10 across the level)

### FeetRiseSinkBlock + QuickSand
- FeetRiseSinkBlock sizes: 128–768 px wide (4–24 tiles), 32 px tall; rise=3, fall=-0.5, min=0.5–2.0
- QuickSand always appears directly on top of FeetRiseSinkBlocks at matching x/width; depth ~3 tiles
- These pairs form "sinking platform over quicksand" traps — standing on the block causes it to sink
- First appears in room7 (1 large 24-tile block); continues through room8 (3 blocks: 4, 3, 12 tiles wide) and desert_man_room (1 block ~14 tiles wide)

### DarknessV2
- A single DarknessV2 spans rooms 8–10, desert_man_room, and boss_room (key=1)
- The LaserBeamer in room8 carries `light_keys: 1` — destroying it disables the darkness
- No equivalent light mechanic exists in rooms 9–boss_room once the LaserBeamer is gone

### Ladders
- Ladders appear in rooms 3 and 4 connecting floor zones during vertical traversal
- Always paired with a LadderTop block at the upper landing

## Notable Observations
- Wily Stage 1 recycles two robot masters as mid-bosses (ReactorMan, DesertMan) using the same gate structure as stage-final boss rooms, gated behind `boss_key` properties.
- The second half of the level (rooms 8 onward) is cloaked in permanent darkness until the player destroys the LaserBeamer in room8 — a unique single-switch darkness toggle not seen in typical robot master stages.
- Room7 is the widest room in the level (125 tiles) and the most mechanically dense: PropellerPlatforms over a 31-tile gap, FeetRiseSinkBlock+QuickSand, WanaanLaunchers, and multiple flying + platform enemies.
- Room2 is the only tall vertical room (58×28) in the level and uses only PropellerPlatforms for ascent with no floor-gap hazards — functioning as a breather puzzle room between the opening corridor and the hazard-heavy rooms 3–5.
