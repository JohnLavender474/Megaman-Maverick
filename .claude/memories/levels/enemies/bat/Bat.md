# Bat

- **Layer:** `enemies`
- **Canonical size:** 16 × 16 px (also seen at 24×24 and 32×32 in some levels)
- **Observed `spawn_type`:** absent (in-camera)
- **Known properties:**
  - `type` — variant string (e.g. `"Snow"`)

Flying enemy. Hangs from ceilings until the player passes, then dives.
Effective in vertical rooms and tight corridors. Should spawn from a 
ceiling; the top of the spawn rectangle should be in line with the 
bottom of the ceiling rectangle.

Source: observed in `DesertMan.tmx`, `GlacierMan_16x14.tmx`.
