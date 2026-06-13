---
name: design-patterns-reactor-man
description: Design patterns from Reactor Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Reactor Man

Source file: `assets/tiled_maps/tmx/ReactorMan_v3.tmx`

## Overview
- Theme: Industrial/nuclear reactor — toxic water pools, acid goop dispensers, dripping goop puddles, pipe-mounted laser beamers, barrel bots, floating can turrets
- Total rooms: 19 (14 main-path rooms + 2 alt-path rooms + 2 mini-boss rooms + 1 boss room)
- Standard single-screen room size: 16×14 tiles (512×448 px)
- Wide horizontal rooms: 52–82 tiles wide × 14 tall (rooms 1, 4, 5, 10, 12)
- Vertical transition corridors: 16 tiles wide × 22–46 tiles tall (rooms 3, 8, 11, 13)
- Transition style: mixed — camera-based for standard corridor links; Gate sensor for all boss/mini-boss rooms; Trigger/Target objects for scrolling sequences in room2

## Room Profiles
| Room                        | Size (tiles) | Enemy types (regular)                                           | Hazard types                                                               | Floor gaps              | Water                                 |
|-----------------------------|--------------|-----------------------------------------------------------------|----------------------------------------------------------------------------|-------------------------|---------------------------------------|
| room1                       | 69×14        | RollingBot, UFOhNoBot, SwinginJoe, TankBot                      | —                                                                          | 4 gaps: 4/3/2/3 tiles   | —                                     |
| room2                       | 16×14        | —                                                               | —                                                                          | —                       | —                                     |
| room3                       | 16×30        | SwinginJoe (×3)                                                 | —                                                                          | —                       | —                                     |
| room4                       | 82×14        | ToxicBarrelBot, UFOhNoBot, FloatingCanHole, RollingBot, TankBot | AcidGoopSupplier (×3), DrippingToxicGoop (×3)                              | none (toxic water pits) | 5 pools, 8–18 tiles wide × 7 deep     |
| room5                       | 80×14        | ToxicBarrelBot, SwinginJoe, FloatingCanHole, TankBot            | AcidGoopSupplier (×3), DrippingToxicGoop (×2)                              | —                       | 2 pools (10–14 tiles wide × 4–7 deep) |
| alt_path_1_room_1           | 16×14        | SwinginJoe (×2)                                                 | —                                                                          | —                       | —                                     |
| alt_path_1_mini_boss_room_1 | 16×14        | ReactorMonkey (mini boss)                                       | —                                                                          | —                       | —                                     |
| mini_boss_room_1            | 16×14        | ReactorMonkey (mini boss)                                       | —                                                                          | —                       | —                                     |
| room6                       | 16×14        | ToxicBarrelBot, SwinginJoe                                      | —                                                                          | —                       | —                                     |
| room7                       | 16×14        | — (gate room for mini-boss pair)                                | —                                                                          | —                       | —                                     |
| room8                       | 16×22        | SwinginJoe (×1)                                                 | TubeBeamerV2 (×2, rightward)                                               | —                       | —                                     |
| room9                       | 16×14        | —                                                               | AcidGoopSupplier (×1), DrippingToxicGoop (×1), TubeBeamerV2 (×2, downward) | —                       | —                                     |
| room10                      | 60×14        | RollingBot, SwinginJoe, FloatingCanHole, ToxicBarrelBot         | AcidGoopSupplier (×1), DrippingToxicGoop (×1), TubeBeamerV2 (×5, mixed)    | 2 pits                  | —                                     |
| room11                      | 16×27        | SwinginJoe, ToxicBarrelBot                                      | TubeBeamerV2 (×5, mixed)                                                   | —                       | —                                     |
| room12                      | 52×14        | TankBot, ToxicBarrelBot                                         | AcidGoopSupplier (×3), DrippingToxicGoop (×3), TubeBeamerV2 (×4)           | —                       | —                                     |
| mini_boss_room_2            | 16×14        | FloatingCanHole (×1), ReactorMonkey (mini boss)                 | —                                                                          | —                       | —                                     |
| room13                      | 16×46        | ToxicBarrelBot, SwinginJoe                                      | TubeBeamerV2 (×3, mixed)                                                   | —                       | 1 pool (7 tiles wide × 6 deep)        |
| room14                      | 16×14        | — (pre-boss corridor)                                           | —                                                                          | —                       | —                                     |
| boss_room                   | 16×14        | ReactorMan (main boss)                                          | —                                                                          | —                       | —                                     |

## Enemy Placement

### SwinginJoe
- Most common enemy in the level; appears in 10+ rooms
- Found throughout: vertical corridors (room3, room8, room13), wide horizontal rooms (room4 not present, room5, room10), and narrow transition rooms (room6, alt_path_1_room_1)
- Typical count per room: 1–3 regular; up to 3 hard-mode-only additions
- Platform width when used: appears on elevated ledges and standard floor segments of varied widths
- Co-occurs with: every other enemy type; almost never alone in a wide room
- Hard-mode additions: 8 extra SwinginJoes added across rooms 1, 4, 8, 9, 10, 11, 12, 13

### RollingBot
- Appears in room1 (×2), room4 (×1), room10 (×1); one hard-mode-only in room1
- Placed on floor-level platforms; no elevated placement observed
- Co-occurs with: SwinginJoe, TankBot, UFOhNoBot in wide rooms

### UFOhNoBot
- 4 instances: room1 (×2), room4 (×1), room5 (×1)
- All three in room1 and room4 use Trigger/Target/Start waypoint system (patrol routes)
- room1 has one free-hovering instance (no waypoints, wait=false) and one patrolling instance
- Platform width irrelevant (aerial enemy)
- Co-occurs with: RollingBot, SwinginJoe

### ToxicBarrelBot
- 9 instances spread across rooms 4, 5, 6, 10, 11, 12, 13
- Found on floor-level or near toxic water edges — always in rooms with ToxicWater or DrippingToxicGoop nearby
- Typical count per room: 1–2
- Co-occurs with: AcidGoopSupplier/DrippingToxicGoop pairs, TubeBeamerV2 (in later rooms)

### FloatingCanHole
- 5 instances: room4 (×2), room5 (×1), room10 (×1), mini_boss_room_2 (×1)
- Aerial turret; placed mid-air (no platform required); size 32×32
- Co-occurs with: other mid-tier enemies; never alone in a room (always 1–2 per room)

### TankBot
- 5 instances: room1 (×1), room4 (×1), room5 (×1), room12 (×1), plus one hard-mode in room10
- Placed on floor; one hard-mode-only instance
- Appears in wide horizontal rooms; 1 per room maximum (regular)
- Co-occurs with: mixed enemy groups; often the "tank" anchor in a wide room

### ReactorMonkey (mini boss)
- 3 instances: alt_path_1_mini_boss_room_1 (boss_key=2, heart_tank=e), mini_boss_room_1 (boss_key=1), mini_boss_room_2 (boss_key=3)
- All use `mini: true` flag
- Accessed through Gate sensors keyed to boss_key values
- Room7 acts as hub for mini_boss_room_1 and alt_path_1_mini_boss_room_1 (two gates)

## Hazard Placement

### AcidGoopSupplier
- Small ceiling-mounted dispenser: 16×8 px
- Always paired with a DrippingToxicGoop puddle directly below it on the floor
- Appears in: room4 (×3), room5 (×3), room9 (×1), room10 (×1), room12 (×3)
- Positioned at roughly y = floor_y − ~160 px from floor (ceiling mount or upper wall area)
- Co-occurs with: DrippingToxicGoop (1-to-1 relationship), ToxicBarrelBot, ToxicWater

### DrippingToxicGoop
- Floor-level toxic puddle: 64×40 px
- Always paired with an AcidGoopSupplier above it (same x alignment within ~16 px)
- Appears in all same rooms as AcidGoopSupplier
- Spaced roughly every 3–5 tiles apart when multiple in one room

### TubeBeamerV2
- Laser beam emitter: 32×32 px, fires in a fixed direction
- Appears only in rooms 8–13 (second half of the level)
- Each instance references an `ignore` block object (the platform/wall it fires through) and a `spawn_room`
- Directions used: right, left, down (all four are plausible but up not seen here)
- Room 11 has the densest cluster: 5 beamers with right/left/down mix
- Count per room: 2 (rooms 8, 9) → 5 (rooms 10, 11) → 4 (room 12) → 3 (room 13)
- Co-occurs with: SwinginJoe, ToxicBarrelBot; no TubeBeamers in easy intro rooms

## Gaps / Pits
- room1 has 4 floor gaps covered by instant-death Death sensors: ~4, 3, 2, and 3 tiles wide
  - The widest gap (between x=608 and x=832) contains a small sub-platform (64×32) partway across
- room4's toxic water areas are backed by instant-death sensors (widths 2–14 tiles); these act as water pit deaths
- room10 has 2 pit Death sensors (each 128 px = 4 tiles wide) near x=6912 and x=7360
- Spikes present in pits: no — pits use instant-death sensors or ToxicWater, not spike hazards

## Water (ToxicWater)
- Pool widths: 7–18 tiles wide
- Pool depths: 4–7 tiles deep (most are 7 tiles = 224 px; two shallower at 4–6 tiles)
- Concentrated in room4 (5 adjacent/near-adjacent pools spanning ~1856 px of floor) and room5 (2 pools)
- One isolated pool in room13 (7×6 tiles)
- ToxicWaterfall decoration objects (visual only, 64 px wide, variable height) appear near water edges and in alt-path corridors

## Notable Observations
- The level has two separate mini-boss branch paths: an alt-path (alt_path_1_room_1 → alt_path_1_mini_boss_room_1) and a hub room (room7) with two gates leading to two separate mini_boss_rooms; all three ReactorMonkeys must be defeated before the main boss gate opens.
- TubeBeamerV2 is the signature late-game hazard — it appears exclusively in the second half (rooms 8–13) and escalates from 2 beamers per room to 5, making vertical/tight corridors increasingly dangerous.
- AcidGoopSupplier + DrippingToxicGoop is always deployed as a matched pair, functioning as a floor-ceiling goop hazard combo; they co-occur with ToxicWater areas and ToxicBarrelBots to reinforce the acid/toxic theme.
- Hard-mode difficulty layer adds 9 extra enemies (8 SwinginJoes + 1 TankBot), all marked `hard_mode_only: true`; the additions are distributed across both wide and narrow rooms, maintaining SwinginJoe as the dominant hard-mode fill enemy.
