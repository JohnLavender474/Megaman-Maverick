# Notes: 089 Conditional crush surfaces

Probe of 2026-10-31, on `051d`, from reading code and map data.

- The edge case table already filters by gravity in the test rooms: a surface does nothing when its edge has no block.
- No surface was found that kills with no solid nearby.
- A filter needs 4 production files, 1 new surface type and 4 test sites. Only a test map would use it.
- An air kill exists in `scroll_switch_sides`: a surface reaches about 0.35 tile past a block face. The owner played it and did not ask
  for a change. If it becomes a problem, delete the surface first, then consider a rule that crushes only a grounded player.
