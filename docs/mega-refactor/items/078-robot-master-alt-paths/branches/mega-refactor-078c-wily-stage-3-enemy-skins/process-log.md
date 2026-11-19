# Process log: mega-refactor-078c-wily-stage-3-enemy-skins

## Status

- State: merged
- Goal: a stage sprite skin for the enemies of Wily Stage 3 (and what they spawn), drawn by Claude Fable 5.2
- Done: branch start, planning (brief approved 2026-11-19)
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch start. Owner: "Go ahead and reskin the Wily Stage 3 enemies using Fable 5.2" (answers the `078b` D1 open question; the request names Fable, so it is the Fable confirmation of this branch).
- Scan, scope and contract (`docs/SCOPE.md`); brief published (3 decisions: skin of the 3 enemies with a `wily` skin, per-class skin sets, skins for the shots).
- Owner answers D1 to D3 (all recommendations). Scope approved; `PLAN.md` (5 steps).
- PLAN step 2 (code): `SpriteSkin.LAB`; `SpriteSkin.of(spawnProps, skins)` fails fast on a skin outside the class set (`SpriteSkinTest`); `SKINS` in the 10 skinned classes; `lab` skin in `Bat` (not with a `type`, for example `Snow`: fails fast), `IceFox`, `AstroAssAssaulter`, `BunbyTank`, `IceBomb`, `BunbyRedRocket`, `SuperCoolActionStarWarsSpaceLazer`, `StagedMoonLandingFlag` (flag and shield regions); spawners pass `color`. 39 `lab` regions listed in `temp/mega-refactor-078c-wily-stage-3-enemy-skins/lab_regions.txt` (main checkout) for step 3; `core:smokeTest` fails on them until then.
- 2026-11-19: code fix (owner decision D2): `SpriteSkin.of(tag, spawnProps, skins)` names the entity class in its error; 18 call sites pass `TAG`; `SpriteSkinTest` checks the class name.
- 2026-11-19: PLAN step 3 (assets): 39 `lab` frames in `assets/sprites/frames/<Atlas>/<Tag>/lab/`; `Enemies1`, `Enemies2`, `Hazards1`, `Projectiles1`, `Projectiles2` repacked (`Basic`). Script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_atlases.py` (main checkout): every old region keeps its name, size and offsets; old pixels equal except `Enemies2` (`png8`): 9 regions of `BigFishNeo`, `CrystalSpiky` and `DemonMet` shift by up to 14 per channel (RGB sum 24), alpha unchanged; each `lab` region equals its frame.
- 2026-11-19: PLAN steps 3 (map) and 4: `color=lab` on 46 placements in `WilyStage3_v4.tmx` (`WanaanLauncher` 18 in `hazards`; `IceFox` 9, `ShieldAttacker` 8, `BunbyTank` 5, `AstroAssAssaulter` 3, `Bat` 2, `UziJoe` 1 in `enemies`; no `Bat` has a `type`); script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_map.py` (main checkout): the only change is the added properties. Credit for the 13 `lab` skins in `README.md`.
- Automated checks: `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass (2557 core tests, 474 smoke tests).
- Review before the play test (1 Sonnet agent): no bug; the 3 manual checks pass; every live map's `color` value is in its class's skin set. Kept (nit): the `SKINS.forEach { skin -> }` lambda parameter shadows the `skin` field in 7 classes; it reads only its own parameter.
- Owner scope change: lab skins for `InfernoOven`, `RisingLavaRiver` and `FloatingEmber` (owner accepted the recommendations: embers too, molten lava in the furnace palette). `SCOPE.md`, brief and `PLAN.md` step 6 updated.
- 2026-11-19: PLAN step 6 (code and map): `SKINS` = `DEFAULT, LAB` in `InfernoOven`, `RisingLavaRiver` and `FloatingEmber`; `RisingLavaRiver` passes its skin to its embers (`LavaRiver` passes none, so its embers stay `DEFAULT`). `color=lab` on the 13 `InfernoOven` and 4 `RisingLavaRiver` placements of `WilyStage3_v4.tmx`; script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_map_hazards.py` (main checkout): the only change is the added properties. 7 `lab` regions listed in `temp/mega-refactor-078c-wily-stage-3-enemy-skins/lab_regions_hazards.txt`; `core:smokeTest` fails on them until the frames are packed. `core:test detektMain core:verifyRandomRule` pass (2557 core tests).
- 2026-11-19: PLAN step 6 (assets): 7 `lab` frames in `assets/sprites/frames/` (`Hazards1/InfernoOven/lab/`, `Hazards2/RisingLavaRiver/lab/`, `Decorations1/FloatingEmber/lab/`); `Hazards1`, `Hazards2`, `Decorations1` repacked (`Basic`). Script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_atlases_step6.py` (main checkout; also reads the `bounds:` format of `Hazards2`): every old region keeps its name, size and offsets; old pixels equal in all 3 atlases (no `png8` shift); each `lab` region equals its frame. Credit for the 3 hazard skins in `README.md` (original hazard sprites have no named artist in the README). `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass (718 engine, 2557 core, 474 smoke tests).
- Review before the play test of the hazard part (1 Sonnet agent): no bug; maps PASS; the Inferno Man control check moves to `--room room5` (its rising lava starts in `room6`).
- Owner: the original artist of the `InfernoOven`, `RisingLavaRiver` and `FloatingEmber` sprites is unknown; the credit line stays as it is.
- Owner scope change: a `lab` skin for `GravitySwitcharoo` (and its `GravitySwitchAura`). `SCOPE.md`, `NOTES.md`, `PLAN.md` step 7.
- 2026-11-19: PLAN step 7 (code and map): `SKINS` = `DEFAULT, LAB` in `GravitySwitcharoo` and `GravitySwitchAura`; the switch passes its skin to its aura (`MoonMan` and `LevelRoomHandler` pass none, so their auras stay `DEFAULT`). `color=lab` on the 21 `GravitySwitcharoo` placements of `WilyStage3_v4.tmx`; script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_map_gravity.py` (main checkout): the only change is the added properties. 4 `lab` regions listed in `temp/mega-refactor-078c-wily-stage-3-enemy-skins/lab_regions_gravity.txt`; `core:smokeTest` fails on them until the frames are packed. `core:test detektMain core:verifyRandomRule` pass (2557 core tests).
- 2026-11-19: PLAN step 7 (assets): 4 `lab` frames in `assets/sprites/frames/` (`Specials1/GravitySwitcharoo/lab/`, `Decorations1/GravitySwitchAura/lab/`); `Specials1`, `Decorations1` repacked (`Basic`). Script `temp/mega-refactor-078c-wily-stage-3-enemy-skins/build/check_atlases_step7.py` (main checkout): every old region keeps its name, size and offsets; old pixels equal in both atlases (no `png8` shift in `Specials1`); each `lab` region equals its frame. Credit for the Gravity Switcharoo and aura skins in `README.md` (same wording as the hazard line, no named artist). `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass (718 engine, 2557 core, 474 smoke tests).
- Review before the play test of the gravity switch part (1 Sonnet agent): no bug; maps PASS; Moon Man control check `--room room6` added.
- Owner, play test: the gravity switch draws behind Megaman (arrow `PLAYGROUND -2`, aura `PLAYGROUND -1`; Megaman is `PLAYGROUND 0`). Applies to every level, also Moon Man. Uncommitted during the Diffity round.
- Owner play test passed ("Ready to merge"); Diffity: no threads. Automated checks pass after the draw order commit. Squash-merged into `master`.
- `master` (with `077c`) merged into the branch before the squash. Resolved: `Projectiles1.png/.txt` (both sides repacked it): repacked with `Basic` from the merged frames folder; 123 regions = the union of both sides (115 and 120), none missing. All automated checks pass after the merge.
