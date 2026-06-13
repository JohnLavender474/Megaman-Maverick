---
name: design-patterns-moon-man
description: Design patterns from Moon Man — enemy/hazard placement, gap widths, platform sizes, co-occurrences
metadata:
  type: project
---

# Design Patterns: Moon Man

Source file: `assets/tiled_maps/tmx/MoonMan_16x14_v5.tmx`

## Overview
- Theme: Space / Moon — reduced gravity throughout, falling asteroids as environmental hazard, satellite decorations, Space background tile
- Total rooms: 17 (room1–room14, mini_boss_room, boss_room, rest_room_1)
- Standard room size: 16×14 tiles (for transition/single-screen rooms)
- Wider horizontal rooms: room1 (88×14), room4 (98×14), room6 (46×14), room9 (61×24), room10 (117×18), room13 (41×14)
- Vertical connector rooms: room3 (16×31), room5 (32×30), room12 (16×43)
- Transition style: horizontal/vertical auto-scroll between rooms; Gate sensors before mini_boss_room, boss_room, and room12/room14; PortalHopper teleportation between room7→room8→room9 and within room9, and room10→room11

## Global Mechanics
- Player gravity_scalar=0.35 and movement_scalar=0.8 applied throughout the entire level (moon low-gravity)
- AsteroidsSpawner: ceiling-mounted hazard area; each spawner is 320×32px (10×1 tiles); min_y reference object determines how far asteroids fall
- GravitySwitcharoo: gravity-flip pads used in room7 (×1), room9 (×3), room10 (×5); rooms 8/9 are a portal-and-gravity puzzle zone
- RocketPlatform (moving platforms): room1 (×1), room3 (×1), room4 (×2)
- Ladder: room3 (×1), boundary between room7 and room8 (×1)
- Rest room: rest_room_1 is a standard-sized empty room mid-level used as checkpoint

## Room Profiles
| Room | Size (tiles) | Enemy types (standard) | Hazard types | Floor gaps (tiles) | Water |
|------|-------------|------------------------|--------------|-------------------|-------|
| room1 | 88×14 | PopupCanon, Screwie | — | ~14, ~10, ~3, ~12 | no |
| room2 | 16×14 | — | — | none | no |
| room3 | 16×31 | Eyee, BunbyTank, PopupCanon | — | none (vertical) | no |
| room4 | 98×14 | MoonEyeStone, BunbyTank, Eyee | AsteroidsSpawner | 5, 18 (bridged), 4, 9 | no |
| room5 | 32×30 | AstroAssAssaulter, BunbyTank, PopupCanon | — | varies (vertical) | no |
| room6 | 46×14 | AstroAssAssaulter, BunbyTank | — | unknown | no |
| room7 | 16×14 | PopupCanon (ceiling) | — | none | no |
| room8 | 16×14 | — (portal/gravity room) | — | — | no |
| room9 | 61×24 | Eyee, PopupCanon, BunbyTank | — | varies | no |
| room10 | 117×18 | TellySaucer, Screwie, MoonEyeStone, AstroAssAssaulter, PopupCanon | AsteroidsSpawner | varies | no |
| rest_room_1 | 16×14 | — | — | none | no |
| room11 | 16×14 | — | — | none | no |
| mini_boss_room | 16×14 | MoonHead (mini-boss) | AsteroidsSpawner (child) | none | no |
| room12 | 16×43 | Starkner | — | none (vertical) | no |
| room13 | 41×14 | TellySaucer, Starkner | — | unknown | no |
| room14 | 16×14 | — | — | none | no |
| boss_room | 16×14 | MoonMan (boss) | AsteroidSpawnBounds (child) | none | no |

## Enemy Placement

### PopupCanon
- gravity_scalar=0.25 (reduced, appropriate for low-grav theme)
- Two orientations: floor-mounted (normal) and ceiling-mounted (direction=down)
- Floor-mounted: placed on raised ledges and mid-height platforms, not always at room floor level
- Ceiling-mounted variant appears starting in room7 and heavily in hard mode across rooms 3–10
- Platform width when used: typically 2–4 tiles wide ledge, or at the edge of larger platforms
- Elevated: yes, often on mid-height platforms 3–6 tiles above floor
- Typical count per room: 1–2 standard; 1–3 added in hard mode
- Hard-mode-only versions are very common — roughly half of all PopupCanon instances are hard_mode_only=true
- Co-occurs with: BunbyTank, MoonEyeStone (room4), Eyee (room9), TellySaucer (room10)

### Screwie
- type=blue, movement_scalar=0.65
- Can be oriented: down=false (floor/wall crawler) or down=true (ceiling crawler)
- Ceiling variant (down=true) used in room3 (vertical), room7/room6 area, room9
- Primarily hard-mode-only in rooms 4, 5, 6, 9, 10; standard placement only in room1 and room10
- Typical count per room: 1–3
- Elevated: variable — ceiling crawlers are at ceiling height; floor crawlers at floor level
- Co-occurs with: PopupCanon (room1), BunbyTank (room4), TellySaucer (room10)

### BunbyTank
- movement_scalar=0.5, roll_frame=0.25, shoot_frame=0.3
- Floor-only; always on solid ground, never elevated
- Platform width when used: 6–14 tiles wide (placed on large floor segments)
- Elevated: rare — one instance in room5 is on an upper platform
- Typical count per room: 1–3
- Co-occurs with: MoonEyeStone (room4), Eyee (room3/room4), AstroAssAssaulter (room5/room6), PopupCanon (room3/room9)

### MoonEyeStone
- Size: 32×32 pixels (1×1 tile)
- Floating; not placed on platforms — hovers in mid-air typically 2–3 tiles above floor or at mid-height
- Used exclusively in room4 (×5 instances) and room10 (×3 instances)
- In room4: clustered in groups of 2–3, spaced ~3–10 tiles apart horizontally, at y≈2496–2528 (ceiling-side of room4)
- In room10: scattered at y≈1824–1856, well above room floor
- Co-occurs with: AsteroidsSpawner (both rooms where it appears), BunbyTank (room4), TellySaucer (room10)

### Eyee
- Size: 24×24 pixels
- Floating with directional movement; movement_scalar=0.5
- Has x/y velocity properties: room3 (x=-4.25), room4 (x=-3.25), room9 (x=4.25 or y=-3.25)
- Used in room3 (×1), room4 (×1), room9 (×4)
- Elevated: yes — mid-air or near ceiling; not floor-level
- Typical count per room: 1–4
- Co-occurs with: BunbyTank (room3), MoonEyeStone (room4), PopupCanon (room9)

### AstroAssAssaulter
- Size: 16×16 pixels
- Flying enemy; placed at upper half of rooms
- Used in room5 (×2), room6 (×2), room10 (×1), room12 (hard, ×1), room13 (hard, ×1)
- In room5: paired — one at y≈2128, one at y≈2256, both on the upper-left side
- In room6: one at y≈2064, one at y≈2192 (slight vertical spread)
- Hard-mode-only in rooms 12 and 13
- Typical count per room: 1–2
- flag_right=false used on one instance (id=191) — faces left
- Co-occurs with: BunbyTank (room5/room6), PopupCanon (room5)

### TellySaucer
- Size: 16×16 pixels
- Flying/drifting; can_spawn_right=false on all instances
- Used in room10 (×5) and room13 (×1)
- In room10: spread across the wide room, clustered in pairs, at y≈1896–2136 (mid-height of room)
- Typical count per room: 1–5
- Co-occurs with: MoonEyeStone (room10), AstroAssAssaulter (room10/room13), Screwie (room10)

### Starkner
- Size: 32×32 pixels
- Used in room12 (×2) and room13 (×1)
- In room12 (vertical connector): placed at y≈864 and y≈1344, spaced about 15 tiles apart vertically
- In room13: at y=864, near the room ceiling
- Elevated: yes (upper portion of rooms)
- Co-occurs with: TellySaucer (room13), AstroAssAssaulter hard-mode (rooms 12–13)

## Hazard Placement

### AsteroidsSpawner
- Size: 320×32 pixels per spawner (10 tiles wide)
- Placement: ceiling-level — placed at or just above the room top edge (room4: y=2416; room10: y=1680)
- Count: 6 spawners in room4, 6 spawners in room10 — placed side-by-side covering most of the room width
- Each spawner references a min_y object that caps how far asteroids fall:
  - room4: min_y at y=3040 (far below room floor — asteroids pass through the whole room)
  - room10: min_y at y=2240 (near room floor, y range=1696–2271)
- cull_out_of_bounds=false, cull_out_of_bounds_children=false (stay active off-screen)
- spawn_type=spawn_room (activate when room is entered)
- Co-occurs with: MoonEyeStone and BunbyTank (room4), TellySaucer/Screwie (room10)
- Also used as a child entity in mini_boss_room (tied to MoonHead boss encounter)

## Gaps / Pits
- Death sensors use instant=true throughout
- room1: multiple gaps confirmed by death sensors at y≈3920; notable gaps ≈14 tiles, ≈10 tiles, ≈3 tiles, ≈12 tiles
- room4: gaps identified from block analysis — 5 tiles (small), 18 tiles (large, bridged by 2 RocketPlatforms), scattered 2–4 tile gaps, 9-tile gap near right side
- The 18-tile gap in room4 has two RocketPlatform entities that serve as traversal (x≈4456 and x≈4808)
- room9 and room10 have death zones confirmed by large death sensor rectangles; gravity-flip sections may have ceiling pits
- Spikes in pits: none observed — all pits use instant-death Death sensors, not spike entities

## Water
- No water entities (Water specials) found anywhere in this level.

## Notable Observations
- The entire level applies gravity_scalar=0.35 and movement_scalar=0.8 to the player, making Moon Man the only known level with a persistent low-gravity feel rather than per-room variation.
- room4 is the densest obstacle room: it combines AsteroidsSpawner ×6 (ceiling), MoonEyeStone ×5 (floating), BunbyTank ×3 (floor), RocketPlatforms ×2 (moving bridges), plus multiple hard-mode PopupCanon/Screwie additions.
- rooms 7–9 form a multi-room portal puzzle: room7 has a gravity-flip and downward PortalHopper, room8 is a bounce/portal transit room, room9 is a large multi-path room with GravitySwitcharoo ×3 and a PortalHopper chain. Enemies in room9 (Eyee, PopupCanon) are placed to intercept the player during gravity-flipped traversal.
- room10 is the widest room in the level (117×18) and reintroduces the AsteroidsSpawner pattern (same density as room4) combined with GravitySwitcharoo ×5, making it the most mechanically complex non-boss room.
- Mini-boss is MoonHead (uses AsteroidsSpawner child as an attack mechanism, with a defined Area for the encounter zone). Final boss is MoonMan (uses AsteroidSpawnBounds child to constrain asteroid spawning during the fight).
