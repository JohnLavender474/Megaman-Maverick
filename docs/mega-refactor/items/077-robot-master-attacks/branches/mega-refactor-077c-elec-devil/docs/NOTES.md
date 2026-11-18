# Notes: mega-refactor-077c-elec-devil

## Decisions

- 2026-11-18 (owner): signature transformation: **Spark Serpent**. The body cells join into a chain of cubes, linked
  by arcs, that moves through the room. The eye cell is the head and the weak point. Gaps in the chain let the player
  pass.
- 2026-11-18 (owner): the serpent path comes from TMX objects, not from the room perimeter at runtime, so that the
  level design controls the flow.
- 2026-11-18 (owner): smaller attacks: **Live Wire** (an arc links consecutive flying pieces), **Eye Spread / Storm
  Rain** (mixed aims: lead, current, near random, wide random), **Circuit Floor** (pieces embed in the floor and a
  current runs along them).
- 2026-11-18 (owner): no Blackout attack. The fight room is already dark, the boss cells and flying pieces are the
  light sources, and Megaman is always lit.
- 2026-11-18 (owner): art: same design, polish. Horn prongs with an arc, a larger eye with a colour telegraph, bolder
  circuit traces with a current pulse, a claw hand, a cell-by-cell assemble and split, a death of several frames.
- 2026-11-18 (owner, deferred): serpent cubes and player shots. Bullets and buster shots are destroyed by a cube.
  `MoonScythe` and `Axe` cut through the cubes and become "electrified". This needs new state and animations in the
  projectile classes, so it is not in this branch yet (see `TODO.md`).

## Overlap with other branches

- `078` (stage redesign; Wily Stage 1 art make-over, decision of 2026-11-18): the "power line" concept ends at the
  generator of the `ElecDevil` boss room. Both can touch the boss room of `WilyStage1_v2.tmx`. `078` is art only (the
  block geometry stays); this branch adds the serpent path objects. Agree the boss room art (walls, cables) with the
  `078` work.
- `118d` (boss fight routines) lists `ElecDevil`. A later move of the fight into routines must keep the new attacks.
