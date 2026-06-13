---
name: design-patterns-smb3-1
description: Design patterns from SMB3-1 (Mario level in Megaman engine) — structural patterns only; entity types are Mario-specific
metadata:
  type: project
---

# Design Patterns: SMB3-1 (Mario Level)

> **Note:** This is a Super Mario Bros. 3 World 1-1 recreation. Enemy and hazard types are
> Mario-specific and do not apply to Megaman level design. Gap/platform geometry and room
> structure may still be informative as a cross-reference.

Source file: `assets/tiled_maps/tmx/SMB3_1.tmx`

## Overview
- Total rooms: 5 (room1, underground_room_1, room2, room3×2 — two separate objects share the
  name "room3")
- Room sizes observed: 155×14, 47×14, 16×14, 16×14, 32×20 tiles
- Dominant room height: 14 tiles (all rooms except the top bonus area at 20 tiles)
- Transition style: mixed — PipePortals (underground warp) + GrowingVines (vertical bonus climb)

## Room Profiles

| Room | Size (tiles) | Enemy types | Hazard types | Floor gaps (tiles) | Notes |
|------|-------------|-------------|--------------|-------------------|-------|
| room1 | 155×14 | Goomba, GreenKoopa, RedKoopa, ChompFlower, FireFlower | BulletBillLauncher | 2, 3, 4 | Main overworld; 17 enemies total |
| underground_room_1 | 47×14 | GreenKoopa | — | — | Reached via PipePortal; coin diamond layout |
| room2 | 16×14 | — | — | — | Narrow vertical connector |
| room3 (lower) | 16×14 | — | — | — | Narrow vertical connector |
| room3 (upper/bonus) | 32×20 | — | BulletBillLauncher | — | Sky bonus area; tallest room |

## Gap / Pit Geometry

Pits are confirmed by InstantDeath sensors in room1:

| Gap location (pixel x) | Width (tiles) |
|------------------------|---------------|
| 2176–2304 | 4 |
| 3008–3072 | 2 |
| 3232–3328 | 3 |
| 4416–4512 | 3 |
| 4576–4640 | 2 |

- Observed gap widths: 2, 3, 4 tiles
- Pit style: bottomless (InstantDeath sensors fill the pit floor)
- All pits are in room1; none in underground or bonus rooms

## Platform Geometry

### CollideDownOnlyBlock (one-way platforms) — 11 instances in room1

| Approx x (px) | Width (tiles) | Height above floor (tiles) | Pattern |
|---------------|--------------|---------------------------|---------|
| 480 | 3 | 3 | Isolated |
| 544 | 3 | 5 | Isolated |
| 864 | 3 | 3 | Isolated |
| 928 | 4 | 5 | Isolated |
| 1024 | 4 | 7 | Isolated |
| 1024 | 5 | 2 | Low shelf |
| 2528 | 7 | 2 | Staircase row 1 |
| 2592 | 7 | 4 | Staircase row 2 |
| 2656 | 7 | 6 | Staircase row 3 |
| 4224 | 3 | 3 | Isolated |
| 4288 | 3 | 6 | Isolated |

- One-way platform widths: 3–7 tiles (most common: 3–4 tiles)
- Platform heights above floor: 2–7 tiles (most common: 3–5 tiles)
- Notable staircase-of-three pattern at x=2528–2880: three 7-tile wide platforms ascending in 2-tile height increments

### Solid staircase structures (brick steps, NOT one-way)

- Ascending brick staircase before pipe at x≈3136–3200 (steps 1–3 tiles tall, 1 tile wide each)
- Descending brick staircase at x≈3904–3968 (mirrored pattern)
- These are standard SMB3 stair-up / stair-down flanking a pipe — Mario-specific

## Special Blocks
- QuestionBlock: 7 total
  - 5 in a horizontal row at x=224–352, y=1568 (5 tiles above floor) — classic Mario ? row
  - 2 at x=4704–4736 near pipe exit
  - 2 vine-spawner QuestionBlocks (trigger GrowingVine → bonus area)
- SMB3_GoldBreakableBlock: 3 in a row at x=2368–2432 — Mario-specific breakable block type
- AbstractBlock: boundary/ceiling walls at edges of room1

## Pipe / Portal Structure
- PipePortal pair 1&2: pipe at (3600, 1656) → underground_room_1 entrance at (3600, 2000)
- PipePortal pair 3&4: underground exit at (4528, 2000) → overworld return at (4528, 1624)
- Two separate underground pipes create a loop: enter at x=3600, exit at x=4528
- GrowingVine: QuestionBlock at x=5248 spawns vine → room2 → room3 → bonus sky area

## Collectible Layout
- Coins in underground_room_1 form a diamond/arrow shape (16 coins total, manually placed)
- No auto-coin rows or loops — each coin is individually placed as a `spawn_now` entity

## Notable Observations
- **Room1 is exceptionally wide (155 tiles)** — an entire Mario level in one room. Megaman levels
  use much shorter rooms (typically 16–32 tiles wide). This single-room-per-level approach is
  Mario-specific and would be architecturally impractical in Megaman Maverick's room-transition
  model.
- **All enemy types are Mario-specific** (Goomba, Koopa, ChompFlower, BulletBillLauncher,
  FireFlower). None map to Megaman enemy classes.
- **CollideDownOnlyBlock is used heavily** (11 instances) for one-way jump-through platforms —
  equivalent to cloud/brick platforms in Mario. Megaman levels also use this block type but
  typically in smaller quantities.
- **Gap widths are intentionally gentle (2–4 tiles)** — consistent with World 1-1 beginner
  design. Megaman platforming gaps tend to start at 2–3 tiles and push to 5–6 tiles in later
  stages.
- **Pit style is strictly bottomless** — all pits use InstantDeath sensors, no spike pits or
  shallow water hazards in the main overworld room.
- **Underground room has no hazards and only 1 enemy** — it functions as a reward/shortcut room
  (coins + pipe skip), not a combat challenge. This is a common Mario design motif that has no
  direct Megaman equivalent.
