---
name: design-patterns-desert-man
description: Design patterns from Desert Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Desert Man

Source file: `assets/tiled_maps/tmx/DesertMan_16x14_v4.tmx`

## Overview
- Theme: Desert → underground cave → water finale. Decorations are Lanterns (63 total). Environmental
  hazards are QuickSand (specials layer, persistent throughout) and FeetRiseSinkBlock. The level
  transitions from open sandy terrain into a dark underground section (DarknessV2 active rooms 6–14)
  and ends with a brief water pool before the boss.
- Total rooms: 17 (room1–room16 + boss_room)
- Standard room size: 16×14 tiles; wide scrolling rooms up to 206×14; vertical shafts at 19×42 and
  16×38
- Transition style: mixed — rooms 1–12 use no Gate sensors (auto/event-triggered); rooms 13–boss use
  Gate sensors (4 total)

## Section Breakdown
| Section                   | Rooms     | Theme / Mechanic                                         |
|---------------------------|-----------|----------------------------------------------------------|
| Open desert               | 1–2       | QuickSand pits, FeetRiseSinkBlocks, bright               |
| Descent / cavern entrance | 3–5       | CrumblingBlocks, Ladders, no QuickSand                   |
| Dark underground          | 6–14      | DarknessV2 active, QuickSand returns, carts (rooms 9–10) |
| Water finale              | 15–16     | Large water pool, Gate transitions                       |
| Boss                      | boss_room | Gate, QuickSand, FeetRiseSinkBlock                       |

## Room Profiles
| Room      | Size (tiles) | Enemy types                        | Special blocks/mechanics                                        | Floor gaps (tiles) | Water |
|-----------|--------------|------------------------------------|-----------------------------------------------------------------|--------------------|-------|
| room1     | 56×14        | LampeonBandito, Arigock, Cactus    | QuickSand ×3, FeetRiseSinkBlock ×3                              | 21, 2, 2           | —     |
| room2     | 100×14       | LampeonBandito, Jetto, Cactus      | QuickSand ×3, FeetRiseSinkBlock ×3                              | —                  | —     |
| room3     | 55×14        | TorikoPlundge, LampeonBandito      | CrumblingBlock ×3, Ladder ×1                                    | 4                  | —     |
| room4     | 16×14        | TorikoPlundge, Cactus, CarriCarry  | CrumblingBlock ×1, Ladder ×2                                    | —                  | —     |
| room5     | 16×14        | TorikoPlundge, Arigock, CarriCarry | CrumblingBlock ×1, Ladder ×1                                    | —                  | —     |
| room6     | 30×14        | CarriCarry                         | Darkness, QuickSand ×1, FeetRiseSinkBlock ×1                    | 2                  | —     |
| room7     | 16×14        | Bat, ScooperPete                   | Darkness, QuickSand ×1, FeetRiseSinkBlock ×1                    | —                  | —     |
| room8     | 71×14        | TorikoPlundge, Bat, ScooperPete    | Darkness, QuickSand ×2, FeetRiseSinkBlock ×2, CrumblingBlock ×1 | —                  | —     |
| room9     | 19×42        | Bat, Met                           | Darkness, CartV2 ×1, Ladder ×1 (vertical shaft)                 | —                  | —     |
| room10    | 206×14       | Bat, CartinJoe, ScooperPete, Met   | Darkness, CartV2 ×2, Ladder ×1                                  | 7, 4               | —     |
| room11    | 16×14        | —                                  | Darkness, Ladder ×2 (empty; checkpoint/gate)                    | —                  | —     |
| room12    | 16×38        | DarkSerket, LampeonBandito, Bat    | Darkness, Ladder ×2 (vertical shaft)                            | —                  | —     |
| room13    | 63×14        | DarkSerket, Bat                    | Darkness, QuickSand ×1, FeetRiseSinkBlock ×1, Gate ×1           | —                  | —     |
| room14    | 16×14        | TorikoPlundge                      | Darkness, QuickSand ×1, CrumblingBlock ×1, Gate ×2              | —                  | —     |
| room15    | 76×14        | Cokeyro, Arigock                   | QuickSand ×1, FeetRiseSinkBlock ×1, Gate ×2                     | 12, 9              | 30×9  |
| room16    | 16×14        | —                                  | QuickSand ×1, FeetRiseSinkBlock ×1, Gate ×2 (empty; checkpoint) | —                  | —     |
| boss_room | 16×14        | DesertMan (boss)                   | QuickSand ×1, FeetRiseSinkBlock ×1, Gate ×1                     | —                  | —     |

## Enemy Placement

### Bat (29 total — most common enemy)
- No platform needed; aerial spawner (plat_w_tiles = null for all)
- Appears from room7 onward through room13
- Densest in room10 (20 Bats) — fills aerial space during long cart-riding segment
- Co-occurs with: ScooperPete (rooms 7, 8, 10), Met (rooms 9, 10), TorikoPlundge (room8),
  CartinJoe (room10), DarkSerket (rooms 12, 13), LampeonBandito (room12)

### TorikoPlundge (11 total)
- Platform width when elevated: 4–5 tiles
- Some placed on ground-level platforms or in mid-air spawn (plat_w_tiles null)
- Appears in rooms 3–5, 8, 14 — transitional / mid-level role
- Co-occurs with: LampeonBandito (room3), Cactus + CarriCarry (room4), Arigock + CarriCarry
  (room5), Bat + ScooperPete (room8)

### LampeonBandito (9 total)
- Platform width: 2–8 tiles; most common is 4 tiles
- Always placed on elevated platforms
- Appears in rooms 1–3, 12 (re-introduced late alongside DarkSerket/Bat)
- Co-occurs with: Cactus (rooms 1, 2), Arigock (room1), Jetto (room2), TorikoPlundge (room3),
  DarkSerket + Bat (room12)

### Jetto (8 total)
- Aerial enemy — no platform (plat_w_tiles null for all 8 instances)
- Confined entirely to room2 (the wide 100×14 desert room)
- Co-occurs with: LampeonBandito, Cactus

### ScooperPete (7 total)
- Platform width: 2–8 tiles; commonly 2–4 tiles in room10
- Always elevated
- Appears in rooms 7, 8, 10
- Co-occurs with: Bat (rooms 7, 8, 10), TorikoPlundge (room8), CartinJoe + Met (room10)

### Met (7 total)
- Platform width: 9–36 tiles (broad floor-level surfaces)
- Placed at floor level or low elevation
- Appears in rooms 9, 10
- Co-occurs with: Bat (rooms 9, 10), CartinJoe + ScooperPete (room10)

### Cactus (6 total as enemy — distinct from blocks:Cactus)
- Platform width: 2–10 tiles
- Always elevated
- Confined to rooms 1–2 (open desert only)
- Co-occurs with: LampeonBandito, Arigock (room1), Jetto + LampeonBandito (room2)

### DarkSerket (5 total)
- Platform width when on platform: 5–18 tiles; some spawned mid-air (null)
- Appears only in rooms 12–13 (the dark underground finale before gates)
- Co-occurs with: LampeonBandito + Bat (room12), Bat (room13)

### CartinJoe (4 total)
- Platform width: 4–5 tiles; all elevated
- Confined entirely to room10 (the 206×14 cart-riding room)
- Always appears alongside CartV2 specials (mine carts) and Bat swarms
- Co-occurs with: Bat, ScooperPete, Met

### Arigock (4 total)
- Platform width: 2–14 tiles
- Appears in rooms 1, 5, 15 — scattered throughout level
- Co-occurs with: Cactus + LampeonBandito (room1), TorikoPlundge + CarriCarry (room5),
  Cokeyro (room15)

### CarriCarry (3 total)
- Platform width: 8–11 tiles
- Appears in rooms 4–6
- Co-occurs with: TorikoPlundge + Cactus (room4), TorikoPlundge + Arigock (room5)

### Cokeyro (3 total)
- Platform width: 3–4 tiles; all elevated
- Confined entirely to room15 (water room)
- Co-occurs with: Arigock, Water pool

## Hazard Placement
No entities on the `hazards` layer were detected in this level. Environmental danger comes
exclusively from QuickSand (specials), Death sensors in pits, and block types (FeetRiseSinkBlock,
CrumblingBlock).

### QuickSand (specials layer, 13 instances)
- Sizes range from 96×96 to 1600×32 px
- Placement: inside floor pits and under/adjacent to elevated platforms
- Appears in rooms 1, 2, 6–8, 13–16, boss_room — present in every section except the
  descent (rooms 3–5) and pure-ladder rooms
- Key property: fills pit floors so missed jumps result in quicksand rather than instant death

### FeetRiseSinkBlock (13 total)
- Sizes: 96×32 to 1600×32 px (all single-tile tall)
- Placement: floor-level in rooms 1–2 (desert surface); reused throughout cave section
- Also used as the boss room floor (1600×32 instance)

### CrumblingBlock (7 total)
- Sizes: 128×32, 160×32, 384×32 px
- Placement: rooms 3–5, 8, 14 — primarily in the transitional descent and cave sections

## Gaps / Pits
- Observed gap widths: 2, 4, 7, 9, 12, 21 tiles
- Room1 has a very wide 21-tile gap — likely a QuickSand pit spanned by platforms
- Room10 has a 7-tile and 4-tile gap; the analysis also detected a 65-tile apparent gap
  (likely caused by distant unconnected floor segments in this 206-tile room)
- Room15 has 12- and 9-tile gaps flanking the water pool
- QuickSand fills most visible pits — true instant-death drops are relatively rare
- Death sensors (21 total across level) mark genuine kill zones at the base of shafts and pits

## Water
- One pool only, located in room15 (76×14)
- Pool dimensions: 30 tiles wide × 9 tiles deep (960×288 px)
- Flanked by floor gaps (12 and 9 tiles) — player must navigate around or across the pool
- Co-occurs with: Cokeyro (elevated platforms near pool), Arigock

## Notable Observations
- DarknessV2 activates at room6 and persists through room14, covering the entire underground
  section; rooms 15–boss are bright again, creating a clear three-act feel.
- Cart-riding is a dedicated segment: rooms 9 (vertical shaft with one CartV2) and 10 (206-tile
  horizontal room with two CartV2s and 4 CartinJoe enemies) form a mine-cart sub-stage within
  the underground zone.
- Bat density escalates sharply in the dark section — room10 alone has 20 Bats, making aerial
  threats the primary challenge of the cave.
- QuickSand is the signature environmental hazard, appearing in 10 of 17 rooms including the
  boss room, always as a soft-kill pit filler rather than an active projectile hazard.
