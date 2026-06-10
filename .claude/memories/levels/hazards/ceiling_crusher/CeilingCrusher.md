# CeilingCrusher

- **Layer:** `hazards`
- **Canonical size:** 32 × 32 px
- **Observed `spawn_type`:** `spawn_room`
- **Known properties:**
  - `height` — int string; how many tiles the crusher drops (e.g. `"4"`)
  - `spawn_room` — required when `spawn_type=spawn_room`

Drops from the ceiling when the player walks beneath it. Retracts after a delay.
Effective in corridor rooms where the player must move quickly or wait for a safe window.

Source: observed in `GlacierMan_16x14.tmx` and others.
