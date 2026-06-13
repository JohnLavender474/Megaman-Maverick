---
name: design-patterns-intro-stage
description: Design patterns from Intro Stage — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Intro Stage

Source file: `assets/tiled_maps/tmx/IntroStage_v3.tmx`

## Overview
- Theme: Industrial/construction — enemies include SteamRollerMan, BuilderKibbo, DrillTankXT, BombChute
- Total rooms: 13 (room1–room12 + boss_room)
- Standard room size: 16×14 tiles; exceptions: room1 (76×14), room2 (59×14), room5 (36×14), room6 (46×14), room8 (86×14), room11 (16×20)
- Transition style: unknown (not inferable from entity data alone)
- Hazards: none — this level has 0 hazard entities across all rooms
- Water: none

## Room Profiles
| Room      | Size (tiles) | Enemy types                                                     | Hazard types | Floor gaps (tiles) | Water |
|-----------|--------------|-----------------------------------------------------------------|--------------|--------------------|-------|
| room1     | 76×14        | SniperJoe, ShieldAttacker, BombChute, SuctionRoller, HeliMet    | —            | 7, 3               | no    |
| room2     | 59×14        | SniperJoe, BombChute, SuctionRoller, BikerKibbo, HeliMet        | —            | —                  | no    |
| room3     | 16×14        | BuilderKibbo                                                    | —            | —                  | no    |
| room4     | 16×14        | ShieldAttacker, SniperJoe                                       | —            | —                  | no    |
| room5     | 36×14        | SniperJoe, DuoBallCanon, SuctionRoller, SteamRollerMan, HeliMet | —            | —                  | no    |
| room6     | 46×14        | DuoBallCanon, SteamRollerMan, BuilderKibbo                      | —            | 4                  | no    |
| room7     | 16×14        | ShieldAttacker, DuoBallCanon, SniperJoe                         | —            | —                  | no    |
| room8     | 86×14        | DuoBallCanon, SpikeCopter, BombChute, SuctionRoller, BikerKibbo | —            | 3                  | no    |
| room9     | 16×14        | (empty)                                                         | —            | —                  | no    |
| room10    | 16×14        | ShieldAttacker, SteamRollerMan, BuilderKibbo                    | —            | —                  | no    |
| room11    | 16×20        | ShieldAttacker, DrillTankXT                                     | —            | —                  | no    |
| room12    | 16×14        | (empty)                                                         | —            | —                  | no    |
| boss_room | 16×14        | (empty)                                                         | —            | —                  | no    |

## Enemy Placement

### SniperJoe
- Platform width when used: 3–14 tiles (observed: 3, 7, 9, 12, 12, 14)
- Elevated: yes in room2 (9-tile platform) and room5 (3-tile platform), floor-level otherwise
- Typical count per room: 1–2
- Co-occurs with: HeliMet, ShieldAttacker, BombChute, SuctionRoller, DuoBallCanon, SteamRollerMan, BikerKibbo

### HeliMet
- Platform width when used: none (aerial enemy, no ground platform detected)
- Elevated: n/a
- Typical count per room: 1
- Co-occurs with: SniperJoe, BombChute, SuctionRoller, BikerKibbo, DuoBallCanon, SteamRollerMan

### BombChute
- Platform width when used: none (no ground platform detected — likely ceiling-spawned dropper)
- Elevated: n/a
- Typical count per room: 1
- Co-occurs with: SniperJoe, HeliMet, SuctionRoller, BikerKibbo, SpikeCopter, DuoBallCanon

### SuctionRoller
- Platform width when used: 3–24 tiles (observed: 3, 4, 18, 19, 24)
- Elevated: yes in room1 (4-tile and 3-tile platforms); floor-level in room5 and room8
- Typical count per room: 1–2
- Co-occurs with: SniperJoe, HeliMet, BombChute, BikerKibbo, DuoBallCanon, SpikeCopter

### ShieldAttacker
- Platform width when used: none detected (likely a melee-patrol enemy, not placed on discrete platform)
- Elevated: no
- Typical count per room: 1–2
- Co-occurs with: SniperJoe (rooms 1, 4, 7), DuoBallCanon (room7), DrillTankXT (room11), SteamRollerMan (room10), BuilderKibbo (room10)

### BikerKibbo
- Platform width when used: 2–8 tiles (observed: 2, 4, 8); always elevated
- Elevated: yes in all occurrences
- Typical count per room: 1–2
- Co-occurs with: SniperJoe, BombChute, SuctionRoller, HeliMet, DuoBallCanon, SpikeCopter

### BuilderKibbo
- Platform width when used: none detected
- Elevated: no
- Typical count per room: 1
- Co-occurs with: DuoBallCanon, SteamRollerMan, ShieldAttacker

### DuoBallCanon
- Platform width when used: 3–29 tiles (observed: 3, 4, 4, 22, 29, 29); always elevated
- Elevated: yes in all occurrences
- Typical count per room: 1–3 (room8 has 3)
- Co-occurs with: SteamRollerMan, SniperJoe, SuctionRoller, HeliMet, ShieldAttacker, SpikeCopter, BikerKibbo, BombChute, BuilderKibbo

### SteamRollerMan
- Platform width when used: 8–18 tiles (observed: 8, 11, 18); floor-level
- Elevated: no
- Typical count per room: 1
- Co-occurs with: SniperJoe, DuoBallCanon, HeliMet, SuctionRoller, ShieldAttacker, BuilderKibbo

### SpikeCopter
- Platform width when used: none (aerial enemy, no ground platform detected)
- Elevated: n/a
- Typical count per room: 4 (appears only in room8, all 4 instances together)
- Co-occurs with: DuoBallCanon, BombChute, SuctionRoller, BikerKibbo

### DrillTankXT
- Platform width when used: 6–12 tiles (observed: 6, 10, 12); mix of floor and elevated
- Elevated: yes for 2 of 3 instances (10-tile and 12-tile platforms)
- Typical count per room: 3 (appears only in room11)
- Co-occurs with: ShieldAttacker

## Hazard Placement
None — Intro Stage contains zero hazard entities.

## Gaps / Pits
- Observed gap widths: 3 tiles (room1, room8), 4 tiles (room6), 7 tiles (room1)
- Spikes present in pits: no (no hazard entities exist)
- Only 3 of 13 rooms have any floor gaps

## Water
- No water entities in any room.

## Notable Observations
- Zero hazards and zero water across all 13 rooms — enemy combat and floor gaps are the only threats; the level reads as a pure enemy-introduction stage.
- room8 is the densest room (86×14 tiles, 12 enemies) and the only room where SpikeCopter appears, all 4 at once — it functions as a combat gauntlet before the end of the level.
- room11 is the only room with non-standard height (16×20 vs. the standard 16×14), and is dedicated exclusively to DrillTankXT (3 instances) with ShieldAttacker support — suggests a vertical pre-boss challenge room.
- DuoBallCanon is exclusively elevated, always placed on a raised platform (3–29 tiles wide); it never appears at floor level across any of its 6 placements.
- Three rooms (room9, room12, boss_room) are completely empty of entities — likely serving as transition corridors and the boss arena, which is populated via a separate spawner mechanism.
