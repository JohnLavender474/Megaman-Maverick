# Notes: mega-refactor-078a-wily-stage-1-art

## Decisions

- 2026-11-18 (owner, play test): the cable light is no longer art only. Lit cable windows send a light 1 tile across, cuts 2 tiles across. Data: tile properties `light`, `light_x`, `light_y` in `wily_stage_1_tileset.tsx` (written by the merge script); `DarknessV2` reads them at spawn.

- 2026-11-18 (owner, brief): animated corridor and cave walls (option C); cave cables over the ceiling shadow stay; no crest at gate 266.

- 2026-11-18 (Claude, planning): delete the 928 transparent `clear` cells (they draw nothing); the girder lamps stay static (no blink); keep the translucent `cave_back_under_ceiling` tile; `boss_room` uses `bg_boss_room.png` behind the generator entity; `077c` reaches the generator by tag (`MegaGameEntities.getOfTag`) and calls `powerDown()`, with no new event type.

## Overlap with other branches

- `077c` (`ElecDevil`): phase 2 turns the generator off through `WilyGenerator.powerDown()`. The branch that merges second wires the call.

## Art sources

- All new pixels: Claude Fable 5.2, directed by the developer; drawn in code in `temp/078-wily1-art/` (scripts and
  the `Sources` section of each area `NOTES.md`). No pixel is copied.
- Inspired by the old room art: `MM10Endless` (AxeW1eld3r, MM10 Endless Attack rip), `Reactor_Man_1` and
  `Reactor_Man_3` (Balloon Cart), `Tileset1` (spike, ladder; maker not known), the sky `BKG22` (maker not known).
- 2026-11-18 (owner): credit only the known sources; leave out `Tileset1` and `BKG22`.
- Frames: `assets/sprites/frames/Backgrounds8/`, `Backgrounds9/`, `Decorations2/` come from
  `temp/mega-refactor-078a-wily-stage-1-art/make_frames.py`; the map objects from `map_objects.py` there.
