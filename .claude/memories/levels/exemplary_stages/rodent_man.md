---
name: design-patterns-rodent-man
description: Design patterns from Rodent Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Rodent Man

Source file: `assets/tiled_maps/tmx/RodentMan_16x14.tmx`

## Overview
- Theme: Rodent/vermin — Ratton (rats), RatRobot, Bat, with sewer/underground feel inferred from entity names
- Total rooms: 18 (room1–room16, boss_room, heart_room)
- Standard room size: 16×14 tiles; larger rooms used for dense encounters (room1: 40×14, room2: 25×14, room4: 36×14, room7: 40×14, room13: 56×14)
- Transition style: Gate-based (6 Gate sensors, 12 SwitchGates opened via FloorButtons)
- Vertical traversal: Heavy use of Ladders (31) and LadderTop (20); multiple interactive block types
- No water present anywhere in the level

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|-------------|--------------|-------------------|-------|
| room1 | 40×14 | Ratton, Bat, RatRobot | Spike (hard-only) | — | — |
| room2 | 25×14 | Bat | Spike (hard-only), WanaanLauncher×2 | — | — |
| room3 | 16×14 | — | Spike (hard-only), WanaanLauncher×2 | — | — |
| room4 | 36×14 | Ratton, RatRobot×2, Bat | Spike (falling×2 + hard-only), WanaanLauncher | — | — |
| room5 | 16×14 | RatRobot, ShieldAttacker | — | — | — |
| room6 | 16×14 | ShieldAttacker, RatRobot | Spike (falling×2), WanaanLauncher | — | — |
| room7 | 40×14 | SniperJoe, RatRobot×2, ShieldAttacker, Bat×2, Ratton | Spike (hard-only×3), WanaanLauncher×2 | 1, 2 | — |
| room8 | 16×14 | RatRobot×2 | — | — | — |
| room9 | 16×14 | SniperJoe | Spike×10 (hard-only) | — | — |
| room10 | 16×14 | SniperJoe, ShieldAttacker | Spike×5 (hard-only) | 3 | — |
| room11 | 16×14 | SniperJoe, Bat | WanaanLauncher | 1 | — |
| room12 | 16×14 | — | Spike×5 (hard-only) | — | — |
| room13 | 56×14 | SniperJoe, RatRobot×4, ShieldAttacker×2, Bat, Ratton | Spike (falling×2 + hard×2 + non-collide×9), WanaanLauncher | — | — |
| room14 | 16×14 | Bat×4, RatRobot | Spike×3 (hard-only) | — | — |
| room15 | 16×14 | BigJumpingJoe, Ratton, Bat | — | — | — |
| room16 | 16×14 | — | — | — | — |
| boss_room | 16×14 | — (RodentMan boss spawned separately) | — | — | — |
| heart_room | 19×14 | Ratton, Bat×2 | — | — | — |

## Enemy Placement

### Ratton
- Platform width when used: 3–19 tiles (elevated); 11–49 tiles (floor level)
- Elevated on narrow platforms in rooms 1 (6 tiles), 7 (3 tiles), 13 (14 tiles); floor-level in rooms 4, 15, heart_room
- Typical count per room: 1
- Co-occurs with: Bat (most rooms), RatRobot, WanaanLauncher, Spike, SniperJoe, ShieldAttacker

### RatRobot
- Platform width: always ground-level (no narrow elevated platform detected)
- Never elevated; patrols floor
- Typical count per room: 1–4; 4 additional RatRobotSpawner entities create dynamic/respawning instances
- Co-occurs with: Bat, Ratton, ShieldAttacker, SniperJoe, Spike, WanaanLauncher

### Bat
- Platform: none (airborne enemy, no platform association)
- Never elevated in detection sense; flies freely
- Typical count per room: 1–4 (room14 has 4)
- Co-occurs with nearly every other enemy type; appears in 10 of 18 rooms

### ShieldAttacker
- Platform: none (ground-level)
- Never elevated; introduced in room5 and appears through room13
- Typical count per room: 1–2
- Co-occurs with: RatRobot, SniperJoe, Ratton, WanaanLauncher, Spike

### SniperJoe
- Platform width: 1–8 tiles; most commonly 2 tiles wide — always elevated
- Always placed on a small raised ledge to serve as a stationary sniper above the main floor
- Typical count per room: 1
- Co-occurs with: Bat, RatRobot, ShieldAttacker, Ratton, WanaanLauncher, Spike

### BigJumpingJoe
- Platform width: full floor (~49 tiles) — floor-level only
- Appears exclusively in room15 (pre-boss encounter room); 1 instance
- Co-occurs with: Ratton, Bat

## Hazard Placement

### Spike
- Three distinct variants observed (all 32×32, region: Spike7):
  1. **Static hard-mode-only** (gravity_on=false, hard_mode_only=true): Most common; wall/ceiling mount; used in 11 rooms
  2. **Falling** (gravity_on=true, no hard_mode_only flag): Active in normal mode; placed overhead to drop; rooms 4, 6, 13
  3. **Non-collide decorative** (collide_on=false, gravity_on=false, instant=false, hard_mode_only=true): 9 instances in room13 only; visual-only spikes
- Placement: ceiling and walls; never confirmed on floor via properties
- Co-occurs with: WanaanLauncher (rooms 2, 4, 6, 7, 13), all enemy types

### WanaanLauncher
- Size: always 64×32
- Each instance references a unique sensor object (trigger zone for activation)
- Placement: typically wall-mounted (inferred from 32-high footprint)
- Appears in 7 rooms: 2, 3, 4, 6, 7, 11, 13
- Co-occurs with: Spike, Bat, RatRobot, ShieldAttacker, SniperJoe, Ratton

## Gaps / Pits
- Observed floor gap widths: 1 tile (rooms 7, 11), 2 tiles (room7), 3 tiles (room10)
- Gaps are narrow — no large pit sections detected; most rooms have solid floors
- Spikes present in same rooms as gaps: yes (room7 and room10 have both gaps and hard-mode spikes)

## Water
- No water pools anywhere in the level

## Interactive Block Mechanics
The level is unusually heavy on interactive block types (noted for design context):
- BreakableBlock: 42 instances across multiple rooms
- PushableBlock + FloorButton + SwitchGate: 10/10/12 instances — puzzle sequences gating room progress
- FeetRiseSinkBlock: 10 instances — sinking floor sections
- DisappearingBlocks: 4 instances — timed platform sequences
- AnimatedBlock: 14 instances

## Notable Observations
- Spikes are almost exclusively hard_mode_only; the only normal-mode hazards are falling spikes (gravity_on=true) in rooms 4, 6, and 13, making normal mode significantly more forgiving for hazard pressure.
- SniperJoe is always placed on a 1–2-tile elevated ledge with no room to dodge; this is the canonical Rodent Man pattern for SniperJoe — pair it with ground enemies to split the player's attention.
- room13 (56×14) is the climax encounter: every enemy type appears together with 15 hazards including 9 non-collide decorative spikes, suggesting a spectacle room before the final approach.
- BigJumpingJoe in room15 acts as a pre-boss gatekeeper with no hazards, giving the designer a clean 16×14 arena for that fight.
