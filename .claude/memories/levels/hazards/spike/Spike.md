# Spike

- **Layer:** `hazards`
- **Canonical size:** 32 × 32 px
- **Observed `spawn_type`:** `spawn_now` (typical) or `spawn_room`
- **Known properties:**
  - `region` — texture region override (e.g. `"CrystalSpikes"` for ice theme; omit for default)
  - `animation` — `"true"` to enable animated sprite
  - `animation_columns`, `animation_rows`, `animation_duration` — animation grid params
  - `sprite_height` — sprite height override in tiles (e.g. `"2"`)
  - `cull_out_of_bounds` — `"false"` to prevent despawn when off-screen

Static instant-kill hazard. Place on floors, ceilings, or platforms.
Use `spawn_now` so spikes are always present regardless of camera position.

Source: observed in multiple levels; full property set from `GlacierMan` family.
