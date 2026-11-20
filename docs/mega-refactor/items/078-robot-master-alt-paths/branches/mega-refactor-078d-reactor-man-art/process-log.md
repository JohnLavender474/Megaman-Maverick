# Process log: mega-refactor-078d-reactor-man-art

## Status

- State: active
- Goal: Reactor Man stage touch-up: one merged tileset with an accent palette and a moving motif per area, decor
  entities with no gameplay effect, backgrounds per area
- Done: branch start, draft `SCOPE.md`, round-1 to round-3 art (final set), brief approved, PLAN.md, PLAN steps 1 to 6
- Next: owner play test and diff review
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch start. Owner: "We should do some touch-ups to Reactor Man's tileset. The tileset is good, but it lacks some
  dynamic effects, and could use some overall refinement, especially to make the sections a bit more distinct from
  one another. The refinement should include "dynamic" elements like water, laser tubes and beamers, etc."
- Owner answers (2026-11-19): tile animations plus decor entities (no hazards); a palette and a motif per area
  (keep the tile shapes); one merged tileset built by a script. Owner approved the short design (`SCOPE.md`).
- Before-renders of the 19 rooms: `temp/078-reactor-art/_before/` (main checkout), script
  `temp/078-reactor-art/render_before.py`.
- Owner confirmed Claude Fable 5.2 for this branch (temporary rule in `docs/ways-of-working/art.md`).
- Round 1: one Fable art agent, brief in this entry's session; output in `temp/078-reactor-art/round1/`.
- Round 1 done (Claude Fable 5.2): `temp/078-reactor-art/STYLE.md`, `round1/`, `make_reactor_r1.py`; 6 owner
  questions in `round1/QUESTIONS.md`. Review page: https://claude.ai/artifact/MS6y4SoXyaVvKL4KDLrDkd
  (source `temp/078-reactor-art/review.src.html`).
- Owner answered the round-1 review with `/go-with-recs`, then changed the direction: one palette for all walls and
  tiles, one for all water; the motion stays; a redraw and refinement pass (`docs/NOTES.md`). The water art
  (`ToxicWater`, `ToxicWaterfall`) comes into scope: only this map uses them among the live levels.
- Round 2: a fresh Claude Fable 5.2 art agent; output in `temp/078-reactor-art/round2/`.
- Round 2 done (Claude Fable 5.2): `make_reactor_r2.py`, `round2/`, `STYLE.md` updated; 4 owner questions in
  `round2/QUESTIONS.md`. Review page republished as version 2 at the same link. Open points from the agent: the
  source of the old `ToxicWater`/`ToxicWaterfall` art (credit check before merge); `DrippingToxicGoop` keeps magenta
  pixels; `Water/ToxicSplash` (`Environs1`) recolored by a map (only the toxic water uses `SplashType.TOXIC`).
- Owner answered round 2: Q1 `classic` (not the recommendation), Q2 to Q4 as recommended (`docs/NOTES.md`).
- Owner asked for a redraw of the entrance city background (`BGNeedle`); a Claude Fable 5.2 art agent works in
  `temp/078-reactor-art/city/`.
- City background draft done (`temp/078-reactor-art/city/`, `NOTES.md`); added to the review page as version 3.
  Findings: today `BGNeedle` (256 x 224) is stretched to the 416 x 512 object; a true 2x draw needs the object at
  512 x 448 and `offset_y=0`; only `room1` shows the background. The source of `BGNeedle` is unknown (credit).
- Owner accepted the city recommendations C1 to C3 (`/go-with-recs`); owner: `BGNeedle` is Capcom (Mega Man 3), no
  credit for the new city art. Round 3 (final art set) started with a fresh Claude Fable 5.2 agent.
- Round 3 done (final art set in `temp/078-reactor-art/round3/final/`; all checks pass; the deep water green moved
  to `#307C08`). Brief published: https://claude.ai/artifact/QTKiXeXYNiAZRX91eh46jn (render in
  `temp/mega-refactor-078d-reactor-man-art/`).
- Owner accepted the brief recommendations D1 to D3 (`docs/SCOPE.md`, approved 2026-11-19). `docs/PLAN.md` written.
  Planning phase ends; implementation starts in a new session.

## 2026-11-19 (implementation)

Scripts and results in `temp/078-reactor-art/` (main checkout): `remap_078d.py`, `check_remap_078d.py`,
`map_objects_078d.py`, `check_objects_078d.py`, outputs in `remap_078d/`; atlas check in
`temp/mega-refactor-078d-reactor-man-art/check_atlases.py`.

- Step 1: round-3 water and decor frames into `Specials1`, `Decorations1`, `Environs1`; repacked (`Basic`). Atlas
  check: every old region keeps its name and visible pixels; the 5 replaced regions (the 4 of the plan and
  `ToxicWaterSplash`, which `STYLE.md` lists as optional; no code reads it) equal their frames; 3 new regions with
  the `STYLE.md` sizes. The check compares visible pixels only: TexturePacker writes a different RGB under alpha 0.
- Step 2: atlas `Backgrounds11` (5 regions, `.tps` copied from `Backgrounds10`, `Nearest`), `TextureAsset.BACKGROUNDS_11`.
- Step 3: merged tileset `reactor_man_tileset` (630 tiles, 1024 x 640, 119 Tiled animations). The frames of each
  cell come from `make_reactor_r3.tile_frames` (area of the room, clear-black rule cell by cell; outside rooms the
  next room's area; no room: the base tile), deduplicated by content. The spark overlay is in the new `decor` tile
  layer at the 3 cells of `placements_r1.json`, as 3 animation variants with the 500 ms phase steps of the round-3
  renders (Tiled animations share one clock). Plan change: `STYLE.md` names `assets/tiled_maps/png/<old file>` as the
  target of the 5 recolored images; not done, because the scope keeps the shared tileset files unchanged; the merged
  tileset holds the new art. Check: map attributes equal master, same painted cells and flip bits, every gid in the
  new tileset, each of the 19 rooms renders equal to the round-3 render at 24 times (0 to 2300 ms).
- Step 4: `CityBKG` per `city/NOTES.md` (region `ReactorCityBKG`, 512 x 448, 2 x 2 frames of 0.5 s, `offset_y` 0).
  4 area backgrounds, parallax 0.5 in x and y (Claude's choice; the brief named none). The background camera is
  object center + offset + parallax x game camera position (the prior position is zero at level start, a respawn
  does not reset it), so offset, rows and columns cover the camera range of the area rooms plus one view. The 4 start
  hidden (`hidden_backgrounds`). 13 `EventTrigger` pairs (hide the other 4, show the own): one in each room 2 cells
  inside the door of the 3 area borders with an open door (`room3`/`room4`, `room7`/`room8`, `room10`/`room11`),
  and one at each of the 10 player spawns (so a `--room` start and a respawn set the background). The
  `room13`/`room14` border has no open cell; the `room14` spawn trigger shows the core background after the pan.
  5 `DecorLaserTube` and 5 `SteamVent` objects at the cells of `placements_r1.json` (`room10`, `room4`). Check: 429
  master objects unchanged, 46 new objects as listed, triggers and hidden list name existing objects, regions exist
  with 256 x 224 frames.
- Step 5: `DecorLaserTube` (caps and glass segments from the object height, grow-only segment sprites, phase =
  map object id mod 4) and `SteamVent` (8 frames, then a wait of 1 to 3 s from `services.random`), both
  `PLAYGROUND` priority -10 (in front of the tiles, behind the player, as in the renders); `entity-tags.txt`.
  `core:smokeTest detektMain core:verifyRandomRule` pass.
- Step 6: README credits: the Reactor Man tileset entry gets "Original by" and "Updated by" lines; a new entry for
  the backgrounds, water and decor ("Inspired by" Balloon Cart for the area backgrounds). Owner: the old water and
  splash art is the developer's own work and needs no credit (`Water/ToxicSplash` keeps no credit line).
- Step 7: `engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` pass
  (`temp/mega-refactor-078d-reactor-man-art/gradle-checks.txt`). Review before the play test: 1 Sonnet agent.
- Review before the play test (1 Sonnet agent; report scripts in `temp/mega-refactor-078d-reactor-man-art/review/`):
  no bug, no contract risk; all 7 manual checks valid (rooms, spawns, triggers, background coverage, entities).
  Fixed: `DecorLaserTube` built one key string per glass segment on each spawn (now a list of the segment sprites);
  `core:smokeTest detektMain core:verifyRandomRule` pass again. Kept: the bare `IllegalStateException` of the tube
  size check (its message is clear); hidden segment sprites still animate (no visible effect); `ToxicWaterSplash`
  recolored and moved in the atlas (no reader); the background switch happens at the start of a pan.


## 2026-11-19 (play test 1)

Owner notes on the indoor areas (round 4 of the art):

- Pipes, girders and steel bars have no order and fade into nothing; with the backgrounds on their own parallax
  this shows more. The owner asks for a large redesign.
- Railing lights flash with no order; the owner wants a sequential strobe. Pipe flow lights must agree in direction:
  on a pipe with a turn, the lights of 2 parts run toward the joint.
- The reactor cores (dome pulse) glow as a whole; the owner wants only a window or a part to glow.
- Owner accepted the 4 round-4 recommendations R1 to R4 (`docs/SCOPE.md`). Round 4 goes to a fresh Claude Fable 5.2
  art agent; output in `temp/078-reactor-art/round4/`.
- Round 4 pilot done (Claude Fable 5.2): `make_reactor_r4.py`, `check_r4.py`, `temp/078-reactor-art/round4/`
  (`NOTES.md`, `QUESTIONS.md` Q1 to Q7, `checks.txt`: pass). New layout in `room4` and `room10`; rail chase (R2) and
  dome window (R4) for the whole stage; pipe flow per run (R3) only in the pilot rooms (it needs the run layout).
  179 new tiles; no tile of today changes. Preview map and tileset in `round4/preview/`; nothing in the worktree yet.
- Owner on the pilot: "looking better"; the fade-out/shadow at the top of the big pipes shows the parallax background
  through and must be opaque. Q1, Q7 (density), Q2, Q3, Q6 (look details) keep; Q4 decor moves accepted. Full job
  (all indoor rooms, with the opaque pipe tops) goes to a fresh Claude Fable 5.2 agent.
- Round 4b done (Claude Fable 5.2): `make_reactor_r4b.py`, `check_r4b.py`, `temp/078-reactor-art/round4b/`
  (`checks.txt`: pass in all 16 indoor rooms; luminance ratio 3.32 or more). Opaque tank, manifold and dome tops;
  3 tanks moved up to the ceiling (`room5`, `mini_boss_room_1`, `boss_room`). 287 new tiles (917 in all); ids 0 to
  629 unchanged. 6 owner questions in `round4b/QUESTIONS.md`. Nothing in the worktree yet.
- Owner accepted all round-4b recommendations (tank sleeve and tanks to the ceiling, one west downcomer in
  `room13`, lattices stay see-through, 72 solid flow cells unchanged, `room9`/`room12` manifold kept, unused tiles
  dropped in one remap when the art is final).
- Integration: the 3 preview files of `round4b/preview/` copied to `assets/tiled_maps/` (map: only `tiles2` and
  `tiles1` changed); decor moves applied (`SteamVent` 797 `y` 4576; `DecorLaserTube` 789 to 792 `x` 6592, 6976, 7360,
  7552). Checks: `check_objects_078d.py` pass; every gid and animation frame names a tile of the 917. The step-3
  check (`check_remap_078d.py`) now compares to round 3 and no longer applies. README: the tileset update line
  names the new layout. No code change, so no Gradle run.
- Owner: in `room10` sparks come out of the air (the round-4 layout removed the panels under the 3 spark cells).
  Fix: the 3 `decor` spark tiles moved onto the grille boxes (tile 459) at room cells 42,3, 39,11 and 53,7 (map
  242,81, 239,89, 253,85); each keeps its phase variant.
- Owner, play test of round 4b: girders do not align in the pit between `room10` and `room11`; on the fall into
  `room11` the shaft background (green far falls) appears suddenly and looks random; bring back some of the
  "weight" of the old tileset. Round 4c goes to a fresh Claude Fable 5.2 agent (`temp/078-reactor-art/round4c/`):
  grid alignment at every area border, a shaft background that continues the works background with no green falls
  (Claude's choice, so that the switch at the pit is not a jump), and a weight pilot in `room4` and `room10`.
- Round 4c done (Claude Fable 5.2): `make_reactor_r4c.py`, `check_r4c.py`, `r4c_base.py`, `temp/078-reactor-art/round4c/`
  (`checks.txt`: pass). Pit: `room11` girder columns at map columns 247 and 254 (in line with `room10`); `room4`
  row-2 chord end at the `room3` edge fixed. `ReactorShaftBKG` redrawn as a continuation of the works background (no
  green, 4 lamp frames). Weight pilot in `room4` and `room10`: variant A (companion shadow pipes) and B (A plus brown
  riser bundles); preview with B. 4 owner questions in `round4c/QUESTIONS.md`. Nothing in the worktree yet.
- Owner (2026-11-20): weight B for all indoor rooms; shaft lamps animated; shaft background object 755 offset
  `-120`, `-32.5`; pit fix as built. Roll-out of B goes to a fresh Claude Fable 5.2 agent (`round4d/`).
- Integration (part): new `ReactorShaftBKG` frame copied, `Backgrounds11` repacked (`Basic`); the atlas layout is
  unchanged and the region equals the frame. The offset of object 755 waits for the round-4d map.
- Round 4d done (Claude Fable 5.2): `make_reactor_r4d.py`, `check_r4d.py`, `temp/078-reactor-art/round4d/`
  (`checks.txt`: pass). Weight B in all 16 indoor rooms; 1082 tiles (165 new); 923 cells differ from the game map.
  2 rules by the agent: no riser bundle behind a `ToxicWaterfall`; `RISER_SKIP` in `alt_path_1_mini_boss_room_1`.
- Integration: the 3 round-4d preview files copied (they hold round 4c and 4d; map: only `tiles2` and `tiles1`
  changed); object 755 `offset_x` -120, `offset_y` -32.5 (a coverage computation of the 4 shaft rooms with
  `Background.kt` semantics: covered, least margin 192 px). `check_objects_078d.py` pass; every gid names a tile.
  3 open questions (round-4d `QUESTIONS.md`: bundles in 4 hall rooms, `mini_boss_room_2`, `boss_room`) for the play
  test; the map holds the recommendations.
- Owner: the waterfall bottom is flat and shows clear pixels above the wave crest; asked about a synced join
  sprite. Decision (owner, 2026-11-20): an isolated shared clock, 0.125 s per frame, and a backlog item with a
  `TODO` for one level clock of tiles and entities (`docs/TODO.md`).
- Join art (Claude Fable 5.2, `temp/078-reactor-art/join/`, `make_join.py`: pass): `ToxicWaterfallJoin` (64 x 48,
  4 frames) in `Decorations1`, repacked; 68 old regions keep size and visible pixels.
- Code: `ToxicLiquidClock` and `ToxicLiquidAnimator` (`entities/special/ToxicLiquidClock.kt`): one 4-frame clock in
  `services.updatables` while a `ToxicWater` or `ToxicWaterfall` lives. `ToxicWater` waves read it (same 0.125 s as
  before). `ToxicWaterfall` reads it (0.1 s before: the fall is 20 % slower), its sprites grow only (they were
  made again on each spawn), and with the map property `join` (new `ConstKeys.JOIN`) the bottom cell is the join
  sprite, 16 px lower into the wave row. Map: `join=true` on the 8 falls whose bottom is the top of a `ToxicWater`.
- Owner: the join looks good. New notes: the brown riser bundles stop above a barrel or on a floating platform;
  draw them on a separate tile layer behind the other tiles and let them pass through. The barrel tile has black
  pixels around the barrel that must be clear; scan for similar tiles. Goes to a fresh Claude Fable 5.2 agent
  (`temp/078-reactor-art/round4e/`).
- Round 4e done (Claude Fable 5.2): `make_reactor_r4e.py`, `check_r4e.py`, `r4e_base.py`, `temp/078-reactor-art/round4e/`
  (65 checks pass). New tile layer `tiles_back` (first in the file, so it draws behind `tiles2`): 52 riser bundles
  that pass behind objects and end only at a solid mass or a room edge. Black scan (`round4e/black_scan.md`): filler
  cleared in 25 new tile ids (barrels, platform end, tank end, ladder, spikes between the teeth); no tile of today
  changes. Integrated: 3 preview files copied (map: only tile layers and `nextlayerid` changed); 1107 tiles; every
  gid names a tile. `check_objects_078d.py` now lists the 8 `ToxicWaterfall` objects with `join` as changed (intended).
  Open: Q1 ladder and Q2 spikes (the map holds option A of each).
- Owner: Q1 ladder and Q2 spikes cleared (option A, already in the map).
- Owner, `room5`: the grey frame of the dome window between the 1-tile platforms reads as a platform (a death in the pit). Recolor of the window frame goes to a fresh Claude Fable 5.2 agent (`temp/078-reactor-art/round4f/`).
- Round 4f done (Claude Fable 5.2, `make_reactor_r4f.py`, `check_r4f.py`: pass): dome window frame in dome colors (variant A: black outline, rib-brown inner ring, magenta rivets), 15 window tile ids changed in place (834 to 841, 850, 851, 901 to 904, 908); only the tileset image changed. Integrated.
- Owner, `room5`: the first dome has a clear bottom-right corner. Cause: round 4b cleared the 6th body column of the dome (`tiles1` (224,154), (224,155) in `room5` and (214,88) in `room10`, old gid 456) as a layout cell, and the clear-black rule had made that tile half clear. Fix (main session, master pixels, no new art): new tile id 1107 = master `React_Man_1` gid 1307 (brown and black, opaque) in the 3 cells; the clear rows 30 and 31 of the rim tiles 848, 852, 853 and 906 take the master black (112 px). Tileset `tilecount` 1108.
- Owner: scan the other domes for clear gaps. Scan (`temp/078-reactor-art/work/scan_domes*.py`): each dome of `room4`, `room5`, `room10` and `room14`, clear pixels that are opaque on master. All are outside the dome outline (the removed black fill, intended) except a 2 x 10 px slit at the top-right shoulder (tile 802, x 0 to 1, y 14 to 23), in all 4 domes. Fix: the master black (20 px). Images: `temp/078-reactor-art/work/dome_scan_<room>.png`.
- Owner: the city background shows behind the ladder of the first indoor section (`room3`). Fix (main session, `temp/078-reactor-art/work/ladder_back.py`): `tiles_back` gets the steel backdrop tile (gid 42, the `tiles2` tile next to the ladder) behind the ladder cells: `room3` ladder 56 (col 84, rows 169 to 181) and `room1` ladder 39 (col 15, rows 177 to 181). The other 7 ladders (from `room4` on) stay clear: no backdrop tile is next to them and their area background shows (by design).
- Owner: fill behind the spikes of `room3` too. Fix (main session): `tiles_back` row 151 cols 77 to 84 = gid 41 (stripe tops under the platform), with `tiles2` row 152 cols 77 to 84 from 41 to 42 (the stripes run on); `tiles_back` row 157 cols 73 to 75 = gid 42. No clear cell is left in `room2` and `room3`.
- Owner: from `room3` to `room4` the city shows for a few frames before the hall background. Cause: the background `EventTrigger`s used the default in-camera spawn, so a trigger 1 tile inside `room4` fired only after the pan had scrolled it into view. Fix (main session): the triggers are now `spawn_type=spawn_room` (they fire on `BEGIN_ROOM_TRANS`, `PLAYER_READY` and `SET_TO_ROOM_NO_TRANS` for the listed rooms), 1 hide and 1 show trigger per area (City `room1` to `room3`; Hall `room4` to `room7` and the `alt_path_1` and `mini_boss_room_1` rooms; Works `room8` to `room10`; Shaft `room11` to `room13`, `mini_boss_room_2`; Core `room14`, `boss_room`). 22 position triggers removed, 10 kept (ids 757, 758, 763, 764, 767 to 770, 787, 788). Backup: `temp/078-reactor-art/work/ReactorMan_v3.before_bkg_rooms.tmx`.
- Owner: a backtrack from `room4` to `room3` shows the city again. Option A applied (owner accepted the recommendation): city trigger pair on `room1` only, hall pair on `room3` and the hall rooms; `room2` has none (`room2` and `room3` are 0 % clear, so a swap there is never seen). Option C (backgrounds drawn per room) is the preferred long-term fix: backlog candidate in `docs/TODO.md`, to become a new item at merge, in phases (owner). Decision R5 in `SCOPE.md`.
- Play check error (Claude): `--room room3` fails at launch (`room3` has no player spawn; rooms with spawns: `room1`, `room2`, `room4`, `room5`, `room7`, `room10`, `room12`, `room13`, `room14`). The `room3` checks start from `--room room2`.
- Owner: play test passed, ready to merge.
- Unused tiles dropped in one remap (round-4 decision; `temp/078-reactor-art/final_prune/prune.py`): used = the tile ids of the 4 tile layers and every frame of their animations. 1108 tiles to 467; png 1024 x 480. The map has no tile objects. Check `final_prune/verify.py`: 7826 tile cells, each cell's frame pixels and durations equal before and after, 0 mismatches. Id map: `final_prune/id_map.tsv`; before files: `final_prune/before/`.
- Merge prep: `master` (`bb9a14455`) shares files with the branch (atlases `Decorations1`, `Specials1`, `TextureAsset.kt`, `Constants.kt`, `entity-tags.txt`, `README.md`), so `master` was merged into the branch (`203f2678f`). Conflicts: only the packed sheets `Decorations1` and `Specials1` (png, txt); resolved by a repack (`Basic`) of the merged frames. Check `temp/078-reactor-art/merge/check_atlas_merge.py`: every region of both parents exists with the same visible pixels, except the 4 water regions this branch redrew (`ToxicWaterfall`, `ToxicWaterSplash`, `ToxicWater/water`, `ToxicWater/waves`); regions equal frame files (71 and 52). The text files merged with no conflict.
- Automated checks after the merge: `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass (`temp/mega-refactor-078d-reactor-man-art/gradle-merge-checks.txt`). Map object check: 2 intended failures (the 8 `ToxicWaterfall` objects with `join`; 22 of the 32 listed background triggers removed by the room-entry change); no master object removed or otherwise changed. No active branch shares files; `077d` (planned) edits the emitter tiles, now `reactor_man_tileset` ids 112 (`Reactor_Man_3` 210) and 115 (211).
