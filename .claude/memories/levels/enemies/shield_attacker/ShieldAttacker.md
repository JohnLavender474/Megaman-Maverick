# ShieldAttacker

- **Layer:** `enemies`
- **Canonical size:** 32 × 32 px (also seen at 32×40 and 16×16)
- **Observed `spawn_type`:** absent (in-camera) or `spawn_room`
- **Known properties:**
  - `value` — numeric string (e.g. `"2"`); the amount of tiles it
     will travel left or right before turning around.

Enemy with a shield that blocks frontal attacks. Moves back and forth
on an interval. Distance of each movement is determined by `value`.
Forces the player to use vertical shots or wait for an opening. 
Good for tight corridor encounters.

Source: observed in `DesertMan.tmx`, `GlacierMan_16x14.tmx`.
