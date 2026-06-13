---
name: design-patterns-timber-woman
description: Design patterns from Timber Woman — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Timber Woman

Source file: `assets/tiled_maps/tmx/TimberWoman_16x14_v4.tmx`

## Overview
- Theme: Forest/lumber industry — Saw hazards, LumberJoe enemies, NuttGlider aerial units, bee-type CannoHoney; bats in wooded cave sections
- Total rooms: 16 (room1–room14 + heart_room + boss_room)
- Standard single-screen size: 16×14 tiles
- Transition style: gate — three empty 16×14 rooms (room2, room9, room14) divide the level into four segments: intro run → short connector rooms → saw-intro vertical section → water/saw gauntlet → final run

## Room Profiles
| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|-------------|--------------|-------------------|-------|
| room1 | 117×14 | Bat, NuttGlider, CanonHopper, ShieldGuardBot | — | 11, 6, 6 | — |
| room2 | 16×14 | — | — | — | — |
| room3 | 16×14 | CannoHoney | — | — | — |
| room4 | 16×14 | CannoHoney, ShieldGuardBot | — | — | — |
| room5 | 58×14 | Bat, CannoHoney, CanonHopper, ShieldGuardBot | — | — | — |
| room6 | 32×25 | LumberJoe, ShieldGuardBot | — | — | — |
| room7 | 16×16 | LumberJoe | Saw | — | — |
| room8 | 110×14 | Bat, CannoHoney, LumberJoe | Saw | — | — |
| room9 | 16×14 | — | — | — | — |
| room10 | 92×21 | Bat, LumberJoe | Saw | 10 | 20×5, 28×4 |
| room11 | 16×14 | LumberJoe | — | — | 9×18 |
| heart_room | 16×14 | — | Saw | — | 9×18 |
| room12 | 16×14 | CannoHoney, ShieldGuardBot | — | — | — |
| room13 | 76×14 | Bat, CannoHoney, CanonHopper, LumberJoe, MockingByte | — | — | — |
| room14 | 16×14 | — | — | — | — |
| boss_room | 16×14 | — | — | — | — |

## Enemy Placement

### Bat
- No fixed platform (aerial enemy, placed mid-air)
- Appears in every major horizontal room (room1, room5, room8, room10, room13)
- Typical count per room: 3–13
- Co-occurs with: all other enemy types; present in nearly every large room

### NuttGlider
- Platform width when used: 2–6 tiles (elevated platforms only)
- Always elevated above floor
- Typical count per room: 1–3
- Co-occurs with: Bat, CanonHopper, ShieldGuardBot
- Note: exclusive to room1 (intro segment only; not reused later)

### CanonHopper
- Platform width when used: 6–11 tiles (always elevated when platform detected)
- Always placed on elevated platforms
- Typical count per room: 1–2
- Co-occurs with: Bat, CannoHoney, ShieldGuardBot, LumberJoe, MockingByte
- Note: appears in room1, room5, room13; skips the saw/water mid-section

### ShieldGuardBot
- No elevated platform (ground-level only)
- Typical count per room: 1–2
- Co-occurs with: CannoHoney (rooms 4, 12), LumberJoe (room6), Bat/CanonHopper (rooms 1, 5)
- Note: acts as a shield/blocker paired with ranged enemies

### CannoHoney
- No elevated platform (ground-level only)
- Typical count per room: 1–2
- Co-occurs with: ShieldGuardBot (rooms 4, 12), Bat/CanonHopper (rooms 5, 13), LumberJoe (room8)
- Note: frequently paired with ShieldGuardBot as a ranged-plus-shield combo

### LumberJoe
- Ground-level primarily; occasionally elevated on 2–3 tile platforms
- First appears in room6 (mid-level introduction); absent from the intro segment
- Typical count per room: 1–3
- Co-occurs with: ShieldGuardBot (room6), Saw hazards (rooms 7, 8, 10), Bat (rooms 8, 10, 13), CannoHoney/CanonHopper/MockingByte (room13)

### MockingByte
- No fixed platform (aerial/special movement)
- Typical count per room: 3
- Co-occurs with: Bat, CannoHoney, CanonHopper, LumberJoe
- Note: exclusive to room13 (penultimate major room; late-game escalation only)

## Hazard Placement

### Saw
- Size: 32×32 px (1×1 tile); spawn property `type: "p"` on all instances
- Placement: mixed — floor, ceiling, and mid-air (patrol path inferred from `type: "p"`)
- Co-occurs with: LumberJoe (rooms 7, 8, 10); standalone in heart_room
- Escalation pattern: introduced at 2 in room7, held at 2 in room8, peaks at 12 in room10, 3 in heart_room
- Note: no saws in the first half of the level (rooms 1–6)

## Gaps / Pits

- Observed gap widths: 6, 10, 11 tiles
- room1 has three gaps: one wide pit (11 tiles) followed by two medium gaps (6 tiles each)
- room10 has one large gap (10 tiles) adjacent to water pools
- All other rooms: no floor-level gaps detected
- Spikes present in pits: no (open pits only, no spike hazard in gap floors)

## Water

- Pool widths: 9–28 tiles
- Pool depths: 4–18 tiles
- Shallow pools (room10): 20×5 and 28×4 tiles — wide, low obstacles spanning the floor
- Deep pools (room11, heart_room): 9×18 tiles — narrow, very deep vertical wells
- All water appears in the second half of the level (rooms 10–heart_room)

## Notable Observations

- Saw escalation is the dominant difficulty driver in the back half: zero saws through room6, then a hard spike to 12 Saws in room10 is the level's peak hazard density.
- The level uses three empty gate rooms (room2, room9, room14) as explicit pacing breaks, cleanly dividing the level into an intro horizontal run, a short-room connector segment, a saw/water gauntlet, and a final mixed-combat run to the boss.
- NuttGlider and MockingByte each appear in exactly one room (room1 and room13 respectively), bookending the level as theme-setter and late-game escalator without recurring throughout.
- Large horizontal rooms (92–117 tiles wide) drive most of the enemy count; single-screen rooms are used for introductions, transitions, and boss approaches.
