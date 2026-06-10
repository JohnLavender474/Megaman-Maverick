# FireballBar

- **Layer:** `hazards`
- **Canonical size:** 32 × 32 px (spawn anchor point; actual bar extends outward)
- **Observed `spawn_type`:** `spawn_room`
- **Known properties (from `FireballBar.kt` `onSpawn`):**
  - `speed` — float; rotation speed. Negative = counter-clockwise (e.g. `"-4"`)
  - `flip` — bool; mirrors the bar
  - `angle` — float; starting angle in degrees (e.g. `"0"`)
  - `spawn_room` — required when `spawn_type=spawn_room`

Spinning fireball arm that rotates around a fixed pivot. Place the 32×32 anchor at the
pivot point. Good for open vertical rooms and large chambers. Needs to be accompanied
by a `blocks:FireballBlock` entity which is the block at the pivot.

Source: observed in `InfernoMan_16x24_v2.tmx`.
