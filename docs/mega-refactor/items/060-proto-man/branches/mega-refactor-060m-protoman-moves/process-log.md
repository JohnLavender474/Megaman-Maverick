# Process log: mega-refactor-060m-protoman-moves

## Status

- State: active
- Brief: https://claude.ai/artifact/NZ8P9STkM3fVKFwE1MEUdp
- Goal: Megaman's moves for Proto Man: wall slide, air dash, swim (no Rush Jet); the camera turn on a gravity change; the frames of these moves
- Done: planning (scan, `SCOPE.md` and `CONTRACT.md` approved 2026-11-16, `PLAN.md`); steps 1 to 7 (art round, Megaman tests, swim and `A` task in `players/`, test rooms, Proto Man's moves, attacks and guard, art install); steps 1-8
- Next: the owner play test and Diffity round 1
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-16

- Branch start. Merge target: `master` (item decision). Owner: the new frames of this branch (wall slide, air dash,
  swim and any other new move) are drawn by a Fable 5.2 art agent.

## 2026-11-16 (planning)

- Scan: the wall slide and the air dash are shared (`PlayerWallSlide`, `PlayerAirDash`; Megaman and X); the swim and
  the `A` task are Megaman code (the task is set in 8 files). Overlap: `060k` (atlas), `024b` (`Protoman.kt`, the
  weapon blocks).
- Brief published: https://claude.ai/artifact/NZ8P9STkM3fVKFwE1MEUdp. Owner answers: D1 share the `A` task; D2 shots
  and slashes; D3 the guard in all 3 moves (the owner's choice; Claude agreed), with a shielded air dash and a new
  wall guard strip; D4 one art round. `SCOPE.md` and `CONTRACT.md` approved 2026-11-16; `PLAN.md` written (8 steps).


## 2026-11-16 (step 4)

- `RefactorTest060.tmx` (commit `605e0969d`): map 312x18 to 388x32 tiles (columns and rows added at the right and the
  bottom; no old object moved). Room `wall` (tile x 314, 28x28, spawn `10`): a 5-tile shaft between the room wall and
  a 20-tile inner wall, a ledge on top of the inner wall, and a 13-tile free wall with open floor on both sides. Room
  `airdash` (tile x 344, 44x14, spawn `11`): 3 bottomless gaps of 3, 6 and 6 tiles, each with an instant `Death` at
  room bottom + 48 px.
- Gap math (Megaman values, same as Proto Man's for run, jump and gravity; 1/150 s step with friction): a full jump
  at run speed covers 4.44 tiles (about 5.4 with the 1-tile body), a jump with one air dash at the apex covers 6.87
  (about 7.9). So gap 1 needs a jump, gaps 2 and 3 need a jump and a dash.
- Check: `level-analyzer` read-back; each room has its spawn inside, 0 unpainted block cells, no overlap with other
  rooms. Notes, analyzer output and scripts: `temp/mega-refactor-060m-protoman-moves/rooms.md` (main checkout).

## 2026-11-16 (steps 2-3)

- Step 2 (commit `f39c0c510`): `MegamanSwimTest` (19) and `MegamanAButtonTaskTest` (31) on the old code, with a new
  rig `MegamanSpawnedRig` (a fully spawned Megaman: `init()`, `onSpawn()`, real body, behaviors and receivers). All
  green on the old code. Mutations: 54 on the old code, 51 killed (2 of them after a new case each), 3 equivalent
  (details in `docs/NOTES.md`). The step 2 commit message says "56 run": the 54 plus the 2 reruns.
- Bug candidate in `docs/TODO.md`: the swim stroke pushes the wrong way in `DOWN` and `RIGHT` gravity. Kept as is
  (rule for rule), with a `TODO` in `players/PlayerSwim.kt`.
- Step 3: `players/PlayerSwim.kt` (`SwimParams`, `playerSwimBehavior`) and `players/PlayerAButton.kt`
  (`AButtonTask`, `PlayerAButtonComponent`, the rule functions); Megaman calls them. `MegamanAButtonTask.kt` and
  `entities/megaman/constants/AButtonTask.kt` removed. Design and the list of calls for Proto Man: `docs/NOTES.md`.
  `players/README.md`: the status, the diagram and a rule for the `A` task.
- The step 2 specs changed only in imports. 6 older Megaman test rigs changed in their setup (the component), listed
  in `docs/NOTES.md`.
- Owner rule (2026-11-16): a move "rule for rule" needs no new mutation runs. A run on the new code was already going
  and was stopped after 30 of 33 (29 killed; `n-M1` survived, see `docs/NOTES.md`). The stop left one mutation in
  `AirDashBehavior.kt`; it was restored and checked.
- Checks: `engine:test core:test core:smokeTest lwjgl3:jar --rerun` green (engine 698, core 2305, smoke 467 tests).

## 2026-11-16 (step 1, art round)

- Fable 5.2 art agent: 12 draft strips in `temp/mega-refactor-060m-protoman-moves/art/` (main checkout), with
  `contact_sheet.png` and `NOTES.md` (frame counts, sources, muzzle points). The wall is on the left of the frame and
  he faces right, as Megaman's `wallslide`. The air dash bodies come from the fan sheet (BREAK DASH, PROTO SHIELD
  DASH); `sabre_wall_arc` is `sabre_air_arc` 1 px lower.
- Owner pick: all 4 groups (wall slide, wall slash, air dash, swim) install as they are; each needs touch-ups later
  (`060k`).

## 2026-11-16 (steps 5-7)

- Step 7 first (art install): the 12 strips of the art round copied into `assets/sprites/frames/Protoman_v2/`; the
  atlas repacked with the `060c` packer (`temp/mega-refactor-060c-protoman-buster/scripts/pack_atlas.py`, main
  checkout): 45 regions, 214 x 3065, each region byte-equal to its frame file (the packer asserts it). Before the
  copy, the packer rebuilt the old atlas byte for byte from the old frames.
- Step 5: Proto Man's `WALL_SLIDING` (with the wall jump), `SWIMMING` and `AIR_DASHING` through the shared moves, with
  Megaman's numbers in `ProtomanValues`; behaviors in Megaman's order; the `A` task component and every call of the
  step 3 list, and `GravityDeviceComponent`. The camera turn is one shared function, `turnBodyAndCamera`
  (`players/PlayerGravity.kt`); Megaman's setter calls it rule for rule. Both `TODO`s at Proto Man's setter and his
  water hook are gone.
- Step 6: `B` shots with 3 new `BusterParams` offsets (fail-fast exceptions; X passes `null`); the wall slash
  (`ProtomanSabreMoves.WALL`, `SabreState.WALL`); the air slash in a dash and in water; the wall guard, the shielded
  dash (new air dash shield box) and the air guard in water; the pose keys. Choices and the shot offset math:
  `docs/NOTES.md`. 2 backlog candidates in `docs/TODO.md`.
- `ProtomanTest`: 169 to 218 cases. Mutations: 12 runs, 41 mutations, 40 killed, 1 redundant condition removed
  (`docs/NOTES.md`).
- Checks: `engine:test core:test core:smokeTest lwjgl3:jar --rerun` green (engine 698, core 2354, smoke 467 tests).
- Commits: one for the art (step 7) and one for the code of steps 5 and 6: the two steps change the same lines of
  `Protoman.kt`, so they were not split.

## 2026-11-16 (step 8, review before the play test)

- 2 review subagents (branch diff; the manual checks), no game launch. No bug. Megaman and X play the same rule for
  rule; Proto Man's `A` task calls match Megaman's except the teleport (backlog).
- Fixed: a Megaman camera turn test (`MegamanCameraTurnTest`, 5 cases; one batched mutation run: the jetpack reset and
  the camera start, 4 cases fail); `MegaPlayer.airDashConsumed` removed (test-only caller); the `players/README.md`
  diagram edge and teleport note; a long `TODO` line; the contract control checks: X in `REFACTOR_TEST_081` `walls`,
  Megaman in `WILY_STAGE_3` `intro_room` and `room2` (gravity switches) and `GLACIER_MAN` `room11` (swim).
- Checks with `--rerun`: engine 698, core 2359, smoke 467 tests, 0 failed; the jar builds.

