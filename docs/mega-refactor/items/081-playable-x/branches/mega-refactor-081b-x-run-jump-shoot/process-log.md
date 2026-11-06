# Process log: mega-refactor-081b-x-run-jump-shoot

## Status

- State: merged
- Goal: phase 1 of X: `MegamanX` in slot 0 on `X6_INTRO`, with run, jump and the buster
- Done: steps 1 to 10 (automated checks, mutation checks, review before the play test)
- Next: none (phase 2 of X is the next branch)
- Brief: https://claude.ai/artifact/KLCG7r1DVWGzvef4NRTYUg
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch start on `master` after `081a` (`7a577b4cc`) and the X6 intro level (`820a2b605`) merged. Owner (2026-11-06): "Merge the x1 level branch into `master`, then start 081b".
- Branch took in `master` at `c9d99c6bc` (fast-forward; `038a` merged after the branch start).
- Scan by a subagent: notes in `temp/081b/scan.md` (git-ignored). Draft `SCOPE.md`, `CONTRACT.md` and `BRIEF.html`,
  published as a private Artifact page.
- Owner (2026-11-06): D1 to D3 answered as recommended (see `docs/NOTES.md`, "Decisions"). `SCOPE.md` and
  `CONTRACT.md` approved. `PLAN.md` written: 10 steps.

## 2026-11-06 (work)

- Step 1: a subagent built `RefactorTest081.tmx` (96 x 16 tiles) with the `level-builder` CLI and a helper script
  in `temp/081b-map/`. Rooms `flat` (spawn `0`, steps and a drop), `slope` (spawn `1`, 2 `GroundSlope`s: 1/4 up, 1/2
  down), `targets` (spawn `2`, 1 `Junkroid`, 2 `SabreBlock`s). New entry `REFACTOR_TEST_081`; `core:test` passes.
  The slope boxes are painted with the square tile; the owner can paint slope tiles in Tiled.
  `SabreBlock` needs a damager whose `owner.asPlayer` is not null, so X's shots must have X as the owner.
- Plan order: step 3 (`MegamanX`) went before step 2 (the swap), because the swap needs the class. Step 5 started
  with `MegamanJumpTest` (18 cases) on the old `jumpBehavior` (commit `83cb44901`); it passes unchanged after the
  change (commit `c158bab60`), so Megaman jumps the same. The shared jump is `players/playerJumpBehavior(JumpParams)`;
  `bodyOverGround` moved from `Megaman` to `players/PlayerBodies.kt` (`IBodyEntity`), 7 imports changed.
- `MegamanX` in `entities/megamanx/` (class, values, body, controller, buster, sprites). It calls all `081a` shared
  functions and uses `PlayerDamageNegotiator(this)`. The damage, stun and freeze values are Megaman's for now.
- Buster input changed from the plan to the X6 input: a press shoots, a hold charges, a release after a half or full
  charge shoots the charged shot. The contract is updated.
- `PlayerCharacter`, `LevelDefinition.characters` (`X6_INTRO` and `REFACTOR_TEST_081` set X), `PlayerRoster` in
  `players/` with `putInSlot0` (throws for 2 players on an X level and for a live outgoing player). The game makes X
  in `postCreate` and calls the swap in `startLevel`. `resetPlayerProgress` resets the weapon of each `Megaman` in the
  roster. The focus is set at the top of `MegaLevelScreen.start()`.
- Screen workarounds (`is Megaman`, `TODO` for `024`): `PlayerStatsHandler` weapon bar, `ADD_WEAPON_ENERGY` in
  `MegaLevelScreen`, the beam-out key in `EndLevelEventHandler`, 2 sites in `LevelPauseScreen`. `O8-R2`: 0 to 5
  `is Megaman` lines.
- Plan change: no test of `resetPlayerProgress` on the game object (not practical with `testServices()`); the roster
  test covers the read.
- New tests: `PlayerRosterTest` (8), `MegamanXTest` (27). `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`
  passes: `engine` 639, `core` 1010, `smokeTest` 449 (`EntityInitSmokeTest` builds `MegamanX` with the real atlas).
- Step 9: `players/README.md` names X as the second class, and adds `PlayerJump` and `PlayerRoster` with 2 rules.
  `docs/NOTES.md` lists the workarounds for the `024` and `029` docs at the merge. No README for `entities/megamanx/`:
  it is one player class split over files, like `entities/megaman/`. A `TODO` added at X's stun bounce (body up).
- Step 10: 16 mutation checks (`temp/081b/mutate.py`), each caught by at least 1 test: the roster 2-player throw and
  live check; the walk start speed; the stop when not running; the air gravity; the ladder-top rule; no air drag; the
  shot limit, the wait and the full flag; no shot on a release with no charge; the charge drop when hurt; the walk
  start animation key; X's jump speed; the shared jump `canStart` and wall jump speed.
- Review before the play test, agent 1 (branch diff). It confirmed Megaman's jump is the same in all 4 directions,
  and that the `setFocus` move and the X level start path are safe. Fixes:
  - Bug: with `DOWN` held, X sank through a ladder top at the ground gravity (about 2 s), because the feet-gravity
    consumer kept the one-way block. The consumer now drops it while X passes through. New test; it fails without the
    fix. The `TODO` now says that X passes every one-way block, not only a ladder top.
  - Bug: the charging sound went on after X's death, because `stopSound` on a destroyed entity only queues a request.
    `onDestroy` now stops it through `services.audio`, as `Megaman` does.
  - Risk: X's body now has the side, damageable and feet-gravity properties that `EntityQueries` reads.
  - Risk: `--players 2` with a level that has no `MEGAMAN` is now a launch error (exit code 2), in
    `resolveLaunchOptions`. 2 new tests. At the merge, the root `CLAUDE.md` launcher text gets this rule.
  - Nits: `TODO`s for the teleporter callbacks and the "body points up" test; X plays the currency pickup sound.
  - Not changed: X stops for 1 frame on a jump next to a wall (`wallJumpHorizontal = 0`); not visible.
- Review before the play test, agent 2 (the manual checks). 5 of 8 checks OK. Findings:
  - `X6_INTRO` `room1`: X cannot pass the 2.5-tile step at tile 20 (X's jump is 2.05 tiles). Owner (2026-11-06):
    accept for phase 1 (D4). The `room1` check now covers the start area only.
  - `X6_INTRO` boss room has no floor (on `master` since the X6 intro merge). Owner (2026-11-06): fix it here (D5). New
    block 287 at the painted floor, y 512 px; the `D1000` object moved from y 240 to 336 so its floor is 512 too.
  - The ladder top and a sure weapon-energy pickup move to the test map: `REFACTOR_TEST_081` `flat` has a `LadderTop`
    1 tile above the floor (painted with ladder tile gid 131) and a `WeaponEnergyBulb` in a new `items` layer.
  - The slow sink through a ladder top was the same bug that agent 1 found; fixed.
  - Not changed: in `room2`, the floor block 235 is 1 tile below the painted floor and the gate trigger is above
    standing height, so the player jumps into the gate (also Megaman; on `master`). Bug candidate in `docs/TODO.md`.
- After the fixes: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes (`engine` 639, `core` 1013,
  `smokeTest` 449). Re-check of the fixed map parts (Claude, no agent): the `D1000` object (y 336 to 512) is inside
  `boss_room` (y 160 to 608); block 287 runs from x 6240, under gate 14, to the right wall 222; the test map parses,
  the `items` layer has a builder, and the ladder top is 1 tile above the floor, under X's jump.
- Owner (2026-11-06): a simplified 2-frame stand for X (D6). New strip `stand_simplified.png` (frames 1 and 3 of
  `stand.png`); the atlas repacked with it; X plays it at 1.5 s and 0.15 s, as Megaman's stand. Notes in
  `docs/NOTES.md` and a comment in `MegamanX.yaml`, because a slicer pack would drop the strip.
- Owner (2026-11-06): `jump_v2` (frames 0-4 of `jump.png`), atlas repacked, X's rise is frames 0-3 and the fall is
  frame 4. Notes in `docs/NOTES.md` and in the `MegamanX.yaml` comment.
- Owner (2026-11-06): `jump_shoot_v2` (frames 0-4 of `jump_shoot.png`), atlas repacked, X's shoot rise is frames
  0-3 and the shoot fall is frame 4.

## 2026-11-06 (merge of `master`)

- Owner: "merge `master` into the branch". `master` had `038b` (the respawn routine sends `LEVEL_RESET`;
  `PLAYER_SPAWN` removed; `playerSpawnEnd` changed), `047g` docs, the brief template, and `def3f36ea` (the reworked
  `jump.png` frames 1-5 and a repacked `MegamanX` atlas).
- 1 conflict, `MegamanX.png` (both sides repacked). Resolved: `jump_v2.png` cut again from the reworked `jump.png`
  (frames 0-4), then the atlas packed from all 39 strips in the frames folder. Every region on `master` has the same
  pixels; against the branch, only `jump` and `jump_v2` changed.
- After the merge, `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes (`engine` 639, `core` 1013,
  `smokeTest` 449). `MegamanX` compiles against the new `playerSpawnEnd` with no change.

## 2026-11-06 (merge)

- Owner (2026-11-06): the play checks pass ("Tests passed"); this is the merge approval.
- `master` did not move since the last merge of `master` into the branch.
- Overlap with the active branch `047g` (shared camera): it sets a 2-player group focus in `MegaLevelScreen.init()`,
  and this branch sets the focus to slot 0 at the top of `start()`. Owner (2026-11-06): merge `081b` now and warn
  `047g`. When `047g` takes in `master`, it must move its focus choice into `start()`
  (`setFocus(gameCamera, playersFocus ?: player.focus)`, with the 2-player check made at `start()`), or the `start()`
  call replaces the group focus with slot 0. Written in the `081` `NOTES.md` and in `process-log.md` on `master`.
  `047g` also changes `LaunchOptionsResolver.kt`, its test and `MegamanMaverickGame.kt`, in other places.
