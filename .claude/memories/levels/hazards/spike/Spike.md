# Spike

- **Layer:** `hazards`
- **Canonical size:** 32 × 32 px
- **Observed `spawn_type`:** `spawn_now` (typical) or `spawn_room`
- **Known properties:**
  - `region` — texture region override (e.g. `"CrystalSpikes"` for crystal-cave theme; omit for default)
  - `animation` — `"true"` to enable animated sprite, otherwise `"false"`
  - `animation_columns`, `animation_rows`, `animation_duration` — animation grid params
  - `sprite_height` — sprite height override in tiles (e.g. `"2"`)
  - `cull_out_of_bounds` — `determines whether to cull the entity when it's out of the camera
  - `gravity_on` — whether the spikes should have gravity and velocity
  - `collide_on` — `bool`; whether collision is enabled (e.g. `false` to disable)

Static instant-kill hazard. Place on floors, ceilings, or platforms.

Source: observed in multiple levels, notably `RodentMan_16x14.tmx`.
