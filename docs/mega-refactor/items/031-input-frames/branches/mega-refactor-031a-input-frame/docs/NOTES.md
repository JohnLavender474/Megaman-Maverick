# Notes: mega-refactor-031a-input-frame

## Decisions

- 2026-10-25 (owner): Option 2 from the item `NOTES.md` ("Options for the first branch"): the frame and all gameplay
  reads.
- 2026-10-25 (owner, with the scope): `InputFrame` is a value class over one `Int`; the frame store lives in
  `MegaPlayers`; no frozen copy and no temporary suite in `031a` (the equivalence suite compares with the live
  poller).

## Where the moved readers can be checked in the game (2026-10-25, `master` `aa0442526`)

- `SpringBouncer`: only in `TEST_1` (`room_8`, `room23`). Neither room has a player spawn, so `--room` cannot go
  there.
- `PipePortal`: only in `SMB3_1` (`room1`, `underground_room_1`). Neither room has a player spawn. Portal props:
  `direction`, `this_key`, `next_key`, `spawn_type=spawn_now`.
- `PRESS_UP_FALL_THRU` blocks: only the top of `PunkCityLampPost` (`COLLIDE_DOWN_ONLY` and `PRESS_UP_FALL_THRU`,
  `PunkCityLampPost.kt:182`), in `INTRO_STAGE` `room1` (spawn `0`), `room2` (spawn `1`) and `room8` (spawn `4`).
- `GroundSlideBlocker`: in no map.
- `SpiderWeb`: only pooled in the deprecated `ProjectilesFactory`; nothing spawns it.
- The jetpack needs `RUSH_JET`, which the game gives only after `MOON_MAN`.

## Overlap with other branches

None active.
