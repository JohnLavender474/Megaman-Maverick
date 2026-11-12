# Process log: mega-refactor-081f-x6-intro-fixes

## Status

- State: merged (2026-11-12)
- Goal: fix the X6 intro stage and `D1000` problems that the `081e` play test found
- Done: the X6 intro fixes and the D-1000 fight, rounds 1 to 15c
- Next: none
- Brief: https://claude.ai/artifact/A3eG3TTkCDc7K5nYWKaZ1w
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-09

- Folder made at the `081e` merge. No branch exists yet.
- Draft `docs/SCOPE.md`: the research of 2 read-only agents during the `081e` play test, and the owner's answers. It
  still goes through the brief.
- Base: the owner chose to start `081f` from `master` after the `081e` merge.
- Branch start: worktree `.worktrees/mega-refactor-081f-x6-intro-fixes` from `master` (`838cd2953` + this commit).
- Scan: the draft references hold on `master` `fd03dfd06`; `JunkShot.Type.CORE` has one caller (`D1000Core`); no
  overlap with `127a`, `134a`, `compactor-level`. `SCOPE.md` and `CONTRACT.md` drafted; brief rendered (no `✗`) and
  published. The owner's 6 answers from the `081e` play test are in; brief D1 yes (tidy-up), D2 no map change.
  Scope and contract approved. `PLAN.md` written (7 steps).
- Plan steps 1 to 6 done (commits `5bbcd8679`, `8e0883df1`, `1884e5347`). Scope change from the owner during the work:
  the D-1000 vertical rule (4 answers; `NOTES.md`). Tests: 4 in `MegamanXTest` (transition key) and 6 in the new
  `D1000FightTest` (body at spawn, landing, 3 rise cases, damage to X), all through real spawns and updates.
- The first mutation run was refused by the auto-mode classifier: it restored files with `git checkout --` while the
  fixes were not committed, which would have lost them. The work was committed first, then the run repeated on a
  clean tree. 7 mutation checks: 6 caught; the pause test in the transition key was equivalent (the pause always
  runs inside `roomTransition`), so that test was removed from the code. The walk facing on each frame and the frame
  hold in the pause have no automated test (the D-1000 state machine needs a random action to reach the walk); the
  review and the play test cover them.
- The 4 Gradle tasks pass: `engine` 639, `core` 1230, `smokeTest` 451, JAR built.
- Review before the play test (1 agent, read only, no game launch). Findings:
  - the `boss_room` check had no player spawn and named the boss room; already fixed in `87bb57fc0` (`room2`, through
    the gate); the stale contract text, also already fixed;
  - the rise moved only up, so after a ram D-1000 could drop up to 56 px in one frame: now it moves both ways;
  - at the lowest hover (floor + 44) the stand and walk art sank 14 px, throw and ram 24 px, the stagger 58 px:
    owner chose the art limit (floor + 68) and a fall at once for a low stun;
  - a sabre or jump pose played on during the pan: now every pose except `run` holds for the whole transition;
  - the landing test compared a copied constant: it reads the `land` art now.
  Fixes in `c7512ae83`. 1 more mutation check (the art limit), caught. The 4 Gradle tasks pass: `engine` 639, `core`
  1230, `smokeTest` 451, JAR built. No automated test: the walk facing on each frame, the frame hold in a transition,
  the move down after a ram, and the low-stun fall (each needs the random action or the fight phase); the play test
  covers them.
- D-1000 pacing pass (owner request: the fight is slow; deviations from X6 allowed and marked `CUSTOM`). Measured
  from the code: the arena is 368 x 320 TMX px against the X6 368 x 208 px (1.0 wide, 1.54 high), and X walks 1.6
  times faster than in X6, so the X6 waits leave most of a cycle with no threat. Changes in `4b5e9b1b6`: 18 values
  (shorter waits, faster walk, rise, ram and core shots, shorter stun and entry) and 2 rules (a slam at once when X
  is within 60 px at the end of THINK, not just after a crouch; the body takes full charges and the sabre from the
  landing of a stun until the rise). The table is in `docs/NOTES.md`, "CUSTOM changes"; `docs/x6-intro/d-1000-plan.md`
  marks them. Estimated from the code (above half HP, X far): a THINK-to-THINK cycle 7.6 s to 4.5 s; closed time per
  core hit 5.0 s to about 0.9 s; entry 6.0 s to 3.9 s.
- Tests: 1 new in `D1000Test` (the slam rule over all `r` and both health bands), 1 new in `D1000FightTest` (closed
  in the stagger, open after the landing, stun damage 8 applied). 4 mutation checks, all caught: the slam rule
  inverted; the down steps empty; the down rule removed; the down steps from step 0 (open in the stagger). The 4
  Gradle tasks pass: `engine` 639, `core` 1232, `smokeTest` 451, JAR built. No automated test for the value changes;
  the play test covers them.
- Owner (2026-11-09, after the play test): 2 more changes in this branch: a faster, more fun D-1000 fight (deviations
  from X6 marked `CUSTOM`), and the new fight track `MMX6_d1000.mp3` without its fade, looping. 2 agents in this
  worktree (fight: code, tests, commits `4b5e9b1b6`, `b3e290edb`; music: no Gradle, no commit). Music: one loop file
  `MMX6_D1000.ogg` (phrases 2 to 5 of the source, 23.177 s to 117.030 s, 93.85 s, seam step 0.0099), wired in
  `FightPlans`; details in `NOTES.md` "Music".
- D-1000 attacks (owner request, 2026-11-09: new attacks, Overdrive, an adaptive director and eyeball attacks;
  deviations marked `CUSTOM`). Spec: the 3 design reports in `temp/081f-attacks/` (main checkout). `PLAN.md` steps 8
  to 15. Commits: `ac0be38db` (director), `893c5f346` (Bracket, Sweep, Pillar Fence, bomb flight), `b1ee4c773` (Wall
  Crash, Dazed Eye, Ricochet Ram), `b0df9090a` (Ceiling Crash), `0a16d058a` (`D1000Shockwave`), `49f015b66` (eyeball:
  Peekaboo, Spotter, Spread, Dive, contact damage), `2f5eeebac` (Overdrive). The rules and numbers are one `CUSTOM`
  row each in `docs/NOTES.md`; `docs/x6-intro/d-1000-plan.md` marks them.
- Tests: `D1000Test` now tests `D1000Director` (14 cases, over all 1000 rolls and 48 contexts). New
  `D1000AttacksTest` (20 cases) and the test rig `D1000Rig`: a real spawn of the boss, real updates of the boss and
  the core, a real `AnimationsSystem`, a seeded `UtilMethods.random` to pick the first action (the roll is found from
  `D1000Director.chooseAction`). The core slash test moved from `MegamanXSabreTest` to the rig (it set the old
  `vulnerable` field; now the eye opens through the real flow).
- The rig first ran out of heap (MockK keeps each call on a mock, and the entities stay in the static
  `MegaGameEntities`): it now clears the recorded calls every 30 ticks and removes its entities after each test.
- 49 mutation checks: 48 caught; 1 survived (the Bracket bonus when X is near: the test compared shares, which other
  weights also change); the test now compares the weights, and the repeat caught it.
- The 4 Gradle tasks pass: `engine` 639, `core` 1259, `smokeTest` 452 (with `D1000Shockwave`), JAR built.
- No automated test: the Hard second fence, the sprite look (the dim eye, the rotated wave art, the jitter), and the
  camera shakes; the play test covers them.
- Hit boxes from the art (owner request, 2026-11-09: "the damager/damageable/body fixtures are detailed and specific
  on the body and eyeball"). `PLAN.md` step 16, `NOTES.md` CUSTOM row "Hit boxes". A script in `temp/081f-fixtures/`
  (main checkout, not committed) reads the opaque pixels of each frame and fits up to 6 boxes per body frame (a row
  band split, then a local search on the edges); it writes the Kotlin tables and the contact sheets
  `sheet-body-boxes.png` and `sheet-core-boxes.png`. Result: 98.2 % to 98.9 % of the opaque pixels in a box for the
  upright frames, 96.4 % at least for all frames; 87 % to 92 % of the box area is opaque.
- The fixtures read the animator, the origin and the facing in the body pre-process. The world step runs before the
  updatables and the animation step, so the boxes match the frame that is on the screen during that world step. The
  test rig runs the updatables first and the eye after the body, so in the rig a facing change from the eye (the
  Spotter mark) shows in the boxes one tick later; the test accepts either facing on that tick only.
- Tests: new `D1000FixturesTest` (11 cases, `D1000Rig`). The rig texture mock now reports a 1024 px size, so the
  drawn frame can be read from the sprite region. The full `core:test` run first stopped with a full heap: MockK kept
  the recorded calls of mocks that the rig did not clear. `D1000Rig.dispose()` now clears the recorded calls of all
  mocks; the heap growth per rig test went from about 4 MB to less than 1 MB. The core eyeball contact test now
  expects 3 DAMAGER boxes.
- 8 mutation checks, all caught: a body box moved; the mirror flipped; the frame switch removed; the loop offset
  removed; the eye DAMAGEABLE on the ball boxes; the damager on in the stun; the eye box moved; the box offset from
  the body center removed.
- The 4 Gradle tasks pass: `engine` 639, `core` 1270, `smokeTest` 452, JAR built.

## 2026-11-09 (room tuning)

- Owner play test: the fight is too hard in the small room, and the debris does not fall from the top. Work in the
  sub-branch `mega-refactor-081f-room-tuning` (worktree `.worktrees/mega-refactor-081f-room-tuning`, from
  `b95e46575`), in parallel with `mega-refactor-081f-telegraphs` (pillar art, markers, warnings, tells).
- Map: `boss_room` 24 x 14 tiles, arena 21.5 x 12 tiles, map 222 tiles wide (`level_builder insert-cols`, `resize`,
  `translate-*`; half-tile sizes by hand; tiles by `temp/081f-room/paint.py` in the main checkout). Renders before
  and after, and the `level_analyzer` grid of the room: `temp/081f-room/` in the main checkout.
- Camera: no boss-room camera rule exists; the room transition puts the view at the room's left end, then
  `CameraFollow` and `CameraRoomClamp` follow X across the room. The room is one view high, so no vertical scroll and
  no camera zone.
- Code: every arena position is relative to the arena (entry at the center, the eye homes at center ± 3.25 tiles,
  fence lane count from the width). Debris at 0.25 tile under the ceiling, all Wall Crash pieces at one height, a
  small start speed and no off-screen cull so a piece off the screen still falls. The `D1000` body updates also off the
  screen. Normal is easier than Hard (`NOTES.md`, "Room tuning").
- Tests: rig arena 21.5 x 12; positions in the attack tests from the entry point; new tests: debris within 1 tile
  under the arena top (Wall Crash on Normal and Hard, Ceiling Crash), fence lanes across the full width with equal
  gaps, the combo gaps per difficulty, no Sweep after a slam on Normal. 3 mutation checks (debris 1.5 tiles down, a
  fixed 5-lane fence, the Sweep rule removed): all caught.
- No automated test: the `D1000` body `doUpdate` (needs the world system and a camera); the map paint; the camera in
  the room. The play test covers them.
## 2026-11-09 (telegraphs, branch `mega-refactor-081f-telegraphs`)

- Owner play test: the pillar art stopped at 6.44 tiles, and the fight needs warnings (bomb landing, next move,
  debris). A parallel branch (`mega-refactor-081f-room-tuning`) grows the room and tunes the difficulty, so this
  branch reads every height from the arena, keeps its `D1000.kt` changes to hook calls, and does not edit the TMX.
- `D1000Pillar`: the hit box and the art span the floor to the arena ceiling. The pillar frames repeat every 16 rows
  and `pillar_start`/`pillar_end` every 32 rows (measured), so the top 32 rows of each frame tile with no seam.
- New decorations `D1000BombMarker` (floor reticle at the landing x) and `D1000DebrisWarning` (0.5 s of dust and a
  shaking piece, then the piece falls). New `D1000Lamp`: a greyscale overlay of the lamp pixels of each body frame,
  tinted per attack family; THINK makes its plan 0.4 s early so the lamp can blink before the action. CUSTOM rows in
  `docs/NOTES.md` ("Telegraphs").
- Art: `D1000/lamp` (traced from the body art by a script: the lamp-colored pixels that connect in the lamp area of
  each frame), `D1000BombMarker/marker`, `D1000DebrisWarning/dust`. `Bosses3` repacked with TexturePacker (the flags
  of `sprite_slicer.py` `pack_atlas`, `--max-size 4096`, `--trim-sprite-names`, then `to_game_atlas()`); the recipe
  first rebuilt the old atlas byte for byte; all old region names stay. Contact sheet and scripts:
  `temp/081f-telegraphs/` in the main checkout.
- The first lamp strip was 4 rows too low (the script cut the frame from row 0, the code places it from row 4). The
  contact sheet showed it; a new data test now checks each strip pixel against the body art.
- `D1000Rig`: takes the arena height; ticks the warnings, markers and pillars; with `flyBombs` it moves the bombs by
  their velocity and gravity; runs the sprite pre-process. `D1000AttacksTest`: the Wall Crash and Ceiling Crash
  cases check the warnings (the pieces now come 0.5 s later); `overdriveFight` waits 1.15 s, not 1.25 s, because the
  Overdrive plan is now made at the THINK start, and the seeded roll must come before it.
- New `D1000TelegraphsTest` (11 cases). 15 mutation checks, all caught after one test change (the lamp-off case first
  survived, because the next THINK also turns the lamp off; the test now checks the lamp in the walk).
- The 4 Gradle tasks pass: `engine` 639, `core` 1281, `smokeTest` 454 (with the 2 new decorations), JAR built.
- No automated test: the look of the art in the game, the blink speed, and whether the lamp is large enough to
  read; the play test covers them.

## 2026-11-09 (telegraphs after the room-tuning merge)

- 8 `D1000TelegraphsTest` cases failed after the merge `a01ac6d41`. None was a bug in the merged code; each test
  assumed the old room or the old Normal timings:
  - The pillar, marker, Ceiling Crash and lamp cases (BRACKET, CEILING, the lamp-off case) placed X at 4 tiles
    from the left wall and seeded the roll for a MID distance. In the 21.5-tile room the boss enters at 10.75 tiles,
    so X was FAR, and the seeded roll picked another action. X now stands at the entry minus 2.5 tiles (as
    `MID_X_TILES` in `D1000AttacksTest`).
  - The Wall Crash case waited for 4 warnings; Normal now drops 3 pieces (Hard 4).
  - The lamp-off case waited 1 s for the first bomb; Normal THINK is now 0.8 s, so the first bomb comes at about
    1.2 s. The wait is now 2 s; the checks are the same.
- New: the lamp blinks the color of a queued combo follow-up in the last 0.4 s of the first action (`NOTES.md`,
  row "Combo follow-ups"). Accepted gap: SUMMON then RAM, with a `TODO` in `D1000.updateSummon`.
- Tests: 2 new cases (RAM then CROUCH, FENCE then RAM); 5 mutation checks, all caught (the tell removed, the bomb-end
  tell removed, the wall tell removed, no blink, the bomb-end tell 0.2 s late).

## 2026-11-10: second tuning

- Owner play test: "much improved"; asked for a shorter room, a slightly faster boss, a brighter eye, more eye shots
  and more debris. Values in `docs/NOTES.md`, "Second tuning".
- Tests: the debris counts and the walk distances updated; the rigs keep 21.5 tiles. No test for the eye pot shot yet.
- Checks: engine 639, core 1286, smoke 454, 0 failures; JAR built. Changes not committed (Diffity round open).

## 2026-11-10: X sprites round 3

- Owner asked for the artist's stand-shoot pose and for an alignment check of all X poses. Values in
  `docs/NOTES.md`, "X sprites round 3".
- New `stand_shoot` (row 11 frame 0 of `1LMX.png`); `jump_shoot` and `sabre_air` frames moved onto the `jump`
  frames; atlas repacked. Contact sheet and GIFs in `temp/081f-round3/x-sprites/`.

## 2026-11-10: body art round 3

- Owner: more cleanup and detail on the D-1000 body, glowing eyes and mouth, moving parts that tell the attacks.
  Values in `docs/NOTES.md`, "Body art round 3". Branch `mega-refactor-081f-mech-art`.
- The silhouette does not change, so the hit boxes do not change. New overlays: face, glow, mouth, screw, puffs; all
  use the lamp colors and the lamp lead.
- Work files: `temp/081f-round3/mech-art/` (main checkout): contact sheets `sheets/*.png`, GIFs `gifs/*.gif`.

## 2026-11-10: slam and orbs round 3 (branch `mega-refactor-081f-slam`)

- Owner: the orb pillars were impossible to dodge; the ram and slam impacts must shake the room, stun X on the
  ground, and drop debris that hangs before it falls. Values and math in `docs/NOTES.md`, "Slam and orbs round 3".
- X already had the shared stun (`StunComponent`, `STUN_PLAYER`); D-1000 reuses it with a longer duration on the
  event. Megaman keeps 0.25 s.
- Found: `SHAKE_CAM` with an interval of 0.1 s shows on 1 frame of 6, because the camera movers set the camera again
  each frame. D-1000 impacts use interval 0. Other shakes in the game are not changed.
- Checks: engine 639, core 1351, smoke 454, 0 failures. Mutation run (the old Bracket, Sweep and Fence values, a
  0.25 s stun, a 0.1 s shake interval, no shockwave wait): 46 failures (40 dodge, 3 slam, 3 attacks), so the new tests
  catch each change.

## 2026-11-10: eyeball round 3

- Owner play test: a bigger eye, a blink, an angry face before each shot, and a small drift and dodge. Values in
  `docs/NOTES.md`, "Eyeball round 3". Branch `mega-refactor-081f-eye`.
- Art: 24 x 24 frames, redrawn by hand (`temp/081f-round3/eye/`: contact sheet, GIF, scripts). Bosses3 repacked.
- Tests: the dive test allows the idle drift (± 0.41 tiles); the iris test reads the 24 px frame. New
  `D1000EyeTest`: the blink, the angry tell before each shot, the drift box, the dodge (shot, one roll per shot, a shot
  that flies away, the sabre) and no dodge while stunned or in the rage.
- Checks: engine 639, core 1296, smoke 454, 0 failures.
- Mutation checks: 4 (pot tell 0.05 s, blink open 60 ticks, no one-roll set, dodge while stunned or in the rage), all caught by `D1000EyeTest`.

## 2026-11-10: eye and shield round 4

- Owner requests: an 8-bit eye (whole-pixel steps, a hard-cut blink), the eye hurt by shots with the sabre as the
  stun weakness, and a shielded body. Values in `docs/NOTES.md`, "Eye and shield round 4". Branch
  `mega-refactor-081f-r4-eye`.
- Art: `D1000Core/core.png` is 2 frames (the half lid removed). Bosses3 repacked. Contact sheet:
  `temp/081f-round4/eye/contact_sheet.png`.
- Tests: `D1000ShieldTest` (new): the shield, the deflect, the downed body, hits to win. `D1000EyeTest`: the hard-cut
  blink, the stepped drift, the whole-pixel sprite, the eye damage and the sabre stun. The rig `hitBody` is now
  `hitEye` (the body no longer takes a full charge).
- Rig fact: the rig does not run the points system, so a boss in the rig does not set `defeated` when its health
  reaches 0. The hits-to-win tests stop on empty health.
- Mutation checks: 6 (no pixel snap, a bullet does 0 to the eye, no drift hold, a 4-tick shut frame, the sabre
  blocked by the shot time, the shield always off), all caught (8 failed tests in `D1000EyeTest`, `D1000ShieldTest`
  and `D1000AttacksTest`).
- Checks: engine 639, core 1382, smoke 454, 0 failures; `lwjgl3:jar` builds.

## 2026-11-10: round 4 study and anti-trap (branch `mega-refactor-081f-r4-study`)

- Owner: no traps (the eye, the body and the attacks must always leave an escape), and a study and cleanup of the
  fight. Values and the model in `docs/NOTES.md`, "Round 4: study and anti-trap". Study (state machine, attack
  table, scenarios): `temp/081f-round4/study/STUDY.md` (main checkout).
- New: `D1000Escape` (floor cells over time) and `D1000TrapGuard` (live hazards). The director refuses a move, a
  follow-up or an eye pot shot that leaves X no escape; the ram has a vertical check.
- Found: an eye shot that lands in a slam stun, a walk that runs the 7.5-tile body into X, short rams at a pinned X,
  a Ceiling Crash that corners X, the body moving onto X at the wall after a ram, Fence then Ram with a pillar on
  the wall X climbs, the Summon burst landing in the ram's hit stun. All fixed or refused.
- Eye change for the merge with the eye agent: `internal` on 14 constants of `D1000Core` and the pot shot asks
  `D1000.eyeMayShoot` before the tell and at the shot. No art, motion or damage change.
- Checks: engine 639, core 1395, smoke 454, 0 failures; `lwjgl3:jar` built. Mutation run (6 rules off): 11 failures,
  each rule caught.

## 2026-11-10: round 5 eye motion (branch `mega-refactor-081f-r5-eye-jitter`)

- Owner: "The eye jitters in place sometimes; its moves are kind of weird". Causes, evidence and values in
  `docs/NOTES.md`, "Eye motion round 5". Traces and plot: `temp/081f-round5/eye-jitter/` (main checkout).
- Found with frame traces of 12 rig fights: a state target that turns a move back on the next frame, the tell
  jitter end against the next move, a dodge cut after 1 frame by the rage, the Spotter copying each turn of X, and an
  uneven step rhythm at 2 and 4 tiles/s.
- Fixed in `D1000Core.kt` only: a 0.1 s wait before a slow move turns back, a dodge always finishes, a 0.25-tile aim
  step for the Spotter, drift 1.875 and home 3.75 tiles/s.
- Tests: `D1000EyeMotionTest` (new, 10 tests). With the old `D1000Core.kt`, 7 of them fail (5 fights, the drift
  rhythm, the Spotter tap).
- Checks: engine 639, core 1418, smoke 454, 0 failures; `lwjgl3:jar` built.

## 2026-11-10: round 5 aimed orbs and mouth charge (branch `mega-refactor-081f-r5-orb`)

- Owner: the orbs land at random spots, and the shot has no clear tell; wants an orb that charges and glows in the
  mouth, as Reactor Man does. Values in `docs/NOTES.md`, "Round 5: aimed orbs and the mouth charge".
- Bomb, Bracket and Sweep: each orb aims at X's place at its own release (Hard: a lead on the first orb). The Fence
  stays arena-wide, on a lane grid through X. Orb counts are the same as round 3 or lower.
- New art `D1000/charge` and `D1000/charge_glow` (traced from the `D1000Bomb` orb); Bosses3 repacked. Each orb
  charges in the mouth before its release (0.5 s, Hard 0.4 s; later orbs 0.3 s; Fence 0.6 s then 0.2 s).
- The trap guard checks each orb again at its release. No change to `D1000Core.kt`.
- Checks: engine 639, core 1427, smoke 454, 0 failures; `lwjgl3:jar` built. Mutation run (3 rules off): 7 failures, each rule caught.

## 2026-11-10: merge `master` (`d7e421d0f`)

- Merged `master` at `d7e421d0f` (42 commits since `fd03dfd06`; main change: item `146a`, the marker `IPlayer` and
  the tags in `TagsComponent`).
- Text conflict: 1, the imports of `D1000.kt`. Kept both: `IPlayer` (master) and `registeredSlots` (081f).
- Semantic fixes for the `146a` contract:
  - `D1000BombMarker`, `D1000DebrisWarning`, `D1000Shockwave`: removed the `getTag()` overrides (`ITaggable` is
    gone; the default tag is the class name, the same as each `TAG`), and `init()` now calls `super.init()`, which
    adds the `TagsComponent`. Before this fix, a spawn of these entities threw `MissingComponentException`.
  - `core/src/smokeTest/resources/entity-tags.txt`: added the tags of the 3 new entity classes.
  - `D1000FightTest`: the damage test runs `init()` on the `D1000`, `D1000Bomb` and `D1000Pillar` that it makes,
    because `PlayerDamageNegotiator` now reads the tags of the damager.
- Checks: engine 675, core 1526, smoke 454, 0 failures; `lwjgl3:jar` built.

## 2026-11-10: art round 6 (Fable 5.1, in the 081f worktree)

- X's stand-shoot key went back to `land_shoot` (commit `ef748350f`): the round 3 frame was a jump pose.
- Review of all D-1000 frames against the X6 originals: `temp/081f-round6/art/REVIEW.md`, with sheets and drafts.
- Eye redraw from the X6 core shapes (core, stare, angry, new `core_hit`); body clean-up of all 33 frames with
  identical alpha. Bosses3 repacked (153 regions, was 152). NOTES section "Art round 6".
- Not launched; `core:smokeTest` run on the repacked atlas.
- 2026-11-10: round 7 (Fable 5.1, in the 081f worktree). Owner play test: the dim eye read as invincible; X's
  health is low in the intro. The eye open rule (Peekaboo) is removed: the eye is open except while hurt or while
  the body is stunned. Sabre on the eye 2 + 4 on the landing (1/5), full charge 4, bullet 1; the damage to X from
  the body, the bomb, the pillar and the shockwave is 2. The eye boxes and 28 overlay pixels of the round 6 art are
  fixed (found by `core:test`, which the art round had not run). NOTES section "Round 7". core 1526, smoke 454.
- 2026-11-10: round 7a (Fable 5.1, in the 081f worktree). Owner play test: the orb's landing mark shows at the
  charge start (the orb is aimed then); the first charge 1.0 s (Hard 0.9), later charges 0.8 s; orbs 1.1 to 1.4 s
  apart (the Fence 0.5 s); the eye art has more black outlining. A plan-time refusal test went away: a live pillar
  ends before the slower Bracket lands, so the check is at the release (its own test). core 1526, smoke 454.
  The Flank attack (round 7b) runs in the helper worktree `081f-round7-flank`.
- 2026-11-10: round 7b, the Flank attack (Fable 5.1, branch `081f-round7-flank` from the parent at `1212eaa7c`).
  Owner: the eye gets beside X, pauses, glows, shoots orbs and escapes; the pause is the slash window. New
  `D1000EyeAttack.FLANK` and `D1000Action.FLANK` (weight as the Spread); the side away from the nearest wall, a wall
  or a live pillar blocks a side, both blocked gives the Spread. Values in `docs/NOTES.md`, "Round 7b: the Flank
  attack". `D1000.kt` has 3 small edits (the other round 7 agent changes the orb charge there). Not launched.
  Checks: core 1537 (11 new in `D1000FlankTest`; the `D1000TrapTest` Normal fight seed 7 -> 5), smoke 454, 0 failures.
- 2026-11-10: round 8b (Fable 5.1, in the 081f worktree). The Flank eye 1 tile up; the orb grows in the mouth
  (7 charge frames); the landing mark 32 x 12 with a pulsing diamond; X's air dash 13 tiles/s for 0.5 s. core 1537,
  smoke 454. Round 8a (body damage, the fairness study and the back-off) runs in the helper worktree
  `081f-round8-fair`.
- 2026-11-10: round 8a, body damage and the back-off (Fable 5.1, branch `081f-round8-fair` from the parent at
  `1b05f4ba1`). Owner: X damages the shielded body (bullet 1, charged shot 2; the shots are consumed, no bounce, the
  `SHIELD` fixture and `D1000Shield.kt` are gone; the downed table stays). Study of the cornered X in
  `temp/081f-round8/fair/STUDY.md` (main checkout): the model accepts a 0.44-tile sliver next to the body's damager
  as an escape after a walk or a Ceiling Crash drop at the wall. Built: the retreat (at the start of THINK, with X's
  pocket under 4.5 tiles, the body walks back with the lamp off until the pocket is 4.5 tiles), orb actions refused
  while X is still cornered, the eye's damager in X's band as a danger in the model. Values in `docs/NOTES.md`,
  "Round 8a". Not launched. Checks: core 1543 (4 new in `D1000CornerTest`, `D1000ShieldTest` rewritten), smoke 454,
  0 failures.
- 2026-11-10: round 9a (Fable 5.1, in the 081f worktree). The AI-generated TODO note (code, branch NOTES, item,
  ITEMS.md); the eye base art without the pupil and a pupil strip; the `core_spin` strip; the rubble pieces and the
  pile. core 1543, smoke 454. Round 9b (the defeat sequence, the eye flip and the pupil code) runs in the helper
  worktree `081f-round9-defeat`.
- 2026-11-10: round 9a, second pass. The spin is a true sphere turn (12 frames); a small antenna on the eye.
  core 1543, smoke 454.
- 2026-11-11: round 9b, the defeat sequence, the eye facing and the pupil (Fable 5.1, branch `081f-round9-defeat`
  from the parent at `ca6d1e63f`, with the eye strips of `d470a7d25`). Owner: the eye spins before it explodes; the
  body breaks apart into a pile of rubble; the eye faces X; the pupil follows X. Built: `D1000Core.DEFEAT` (the
  flight over the body, the 2.6 s spin with 5 to 2 ticks a frame, the explosion), `D1000.onDefeated` (the shake with
  sparks, `breakApart` into 6 `D1000RubblePiece`s, `showPile` with `D1000RubblePile`, the end 1.5 s later, cap
  `DEFEAT_DUR` 8 s), the eye facing with a 0.5-tile hysteresis held through attacks, the pupil sprite moved toward X
  inside the iris, `core_hit` in the hurt flicker. Values in `docs/NOTES.md`, "Round 9b". Not launched. Checks: core
  1557 (15 new in `D1000DefeatTest`), smoke 456, 0 failures.
- 2026-11-11: round 9b merged (`3a903f12a`). The antenna tip is the brown-orange of the underside, not the iris
  red, so the "eye box on the drawn iris" test checks every row again. engine 675, core 1557, smoke 456, JAR built.
- 2026-11-11 (end of the session): owner notes after the play test of `8e9315d1d`, for the next round:
  - "The fight is pretty good now. Still some things need to be ironed out, for example the mech's targeting system
    for pillar orbs can be off at some turns." Check: the orb is aimed at the charge start (round 7a), 1.0 s before
    the release, so X's move in the charge shows as a miss; and the Hard lead, the lane clamp at the walls, and the
    held orbs.
  - "The explosion effect when the mech is defeated looks kind of cheap. Rotating cut-out pieces of the body sprite
    looks bad. I was hoping for something like the X6-style explosion and debris effects that we already see with for
    example Junkroid." Redo the body break with the X6-style explosion and debris of `Junkroid` (its death effect and
    its debris entities), not the rotated cut-outs of round 9b; the eye spin stays.
  - "The debris that falls from the ceiling doesn't match the ceiling's palette, and it looks and feels a bit too
    contrived. Having the pieces be more scattered and random with randomized sizes and trajectories/impulses would
    help. Also, making the debris appear to be more integrated into the ceiling. Maybe we should plan on updating the
    ceiling's art to make it look more like a natural source of debris?" Sample the debris colors from the ceiling
    tiles; random sizes, spread and impulses; and plan a ceiling art pass (a cracked or loose-rock band) so the
    ceiling reads as the source.
- 2026-11-11: round 10b, the orb aim and the Junkroid-style break (Fable 5.1, branch `081f-round10-break` from the
  parent at `8ef94f027`). Owner: the pillar-orb aim is off at some turns; the body break with rotated cut-outs
  looks cheap, wanted the X6-style explosion and debris of Junkroid. Study in `temp/081f-round10/aim/STUDY.md`
  (main checkout): the mark was a 1.0 s old snapshot of X (1.4 to 1.6 s at the pillar damage), so a walking X saw
  every orb land behind it. Built: the mark 0.6 s before the release (the orb grows in the mouth first), a lead on
  every aimed orb (Normal 0.3 s, at most 1.5 tiles; Hard 0.4 s, at most 2 tiles); the break as a burst of 4
  explosions, 12 to 14 `DebrisPiece` body fragments with random points and impulses, a chain of 10 explosions over
  1 s with 5 smoke puffs, the pile at 0.7 s under the last explosions, `DEFEAT_DUR` 7 s; `D1000RubblePiece` removed.
  Values in `docs/NOTES.md`, "Round 10b". Not launched. Checks: core 1558, smoke 455, 0 failures.
- 2026-11-11: round 10b merged (`0d4a3ff9e`). The 6 unused `D1000Rubble` part frames removed (the pile stays);
  Bosses3 repacked (170 regions). core 1558, smoke 455.
- 2026-11-11: round 10c, the ceiling debris (Fable 5.1, branch `081f-round10-ceiling` from the parent at
  `8ef94f027`). Owner: the debris does not match the ceiling's palette and looks contrived; random sizes, spread and
  impulses; the ceiling as the source. Built: `D1000CeilingDebris` (a hazard that draws the round 10a pipe shards,
  1 damage, spins, breaks on the floor with a `DustPuff`), 2 or 3 shards per lane rolled in `D1000DebrisWarning`
  (one big, then small ones 0.05 to 0.15 s apart, a 0.3-tile spread on the Ceiling Crash lanes, start speed 0 to 2
  tiles/s, landings clamped out of the wall pocket), the dust strip in the ceiling palette, the trap guard's
  envelope forecast and its reads of the rolled pieces, a ceiling art plan. Values in `docs/NOTES.md`, "Round 10c".
  Not launched.
- 2026-11-11: round 10c merged (`14893faab`); Bosses3 repacked after the merge (170 regions, with the new dust and
  without the rubble parts). engine 675, core 1583, smoke 456, JAR built. The review of the D-1000 code is in
  `temp/081f-round10/review/REVIEW.md` (read-only, no change yet).
- 2026-11-11: round 11a (Fable 5.1, in the 081f worktree): the owner's small tunings (leads 1.5, the ram wind-up
  +0.3 s, the eye holds its shots in a volley, the eye over the body, the landing a quarter tile lower, the 3-frame
  blink). core 1583, smoke 456. Next: the refactor of the review's items 1 to 8 (helper worktree
  `081f-round11-refactor`), then the fan puffs and the eye charge glow on the refactored code.
- 2026-11-11: round 11c art: the eye charge glow strip; the 4 loose ceiling tiles in the tileset and 5 placed in the
  boss room ceiling row (owner: "Yes, install the ceiling tile variants"). The eye art is approved by the owner.
- 2026-11-11: round 11b, the refactor of the D-1000 code (Fable 5.1, branch `081f-round11-refactor` from the
  parent at `8181b95cf`). Owner: an easy to read state machine, values and behaviors in the form of the other
  bosses, easy manual tweaks; "fold these review items into the work now". Built, one commit per review item:
  named step enums; `D1000Tuning` with 4 rows in place of every `hard` and Overdrive branch; the orb schedule in
  `D1000Orbs`; the defeat in `D1000Defeat`; the ram, the slams, the Ceiling Crash and the orb volleys as
  `D1000Move` objects that hold their run and their forecast; the director's cue table and `D1000Action.family`;
  `D1000FightTest` on the rig; the duplicates removed; the helpers in `bosses/d1000/` with a package README. No
  behavior change; the tests changed only where they read a renamed symbol or a moved constant (listed in
  `docs/NOTES.md`, "Round 11b"). Not launched. core 1583, smoke 456, 0 failures.
- 2026-11-11: round 11d, the fan puffs and the eye glow (Fable 5.1, branch `081f-round11-puffs-glow` from the
  parent at `254d44be5`). Owner: the fan puffs always on while the body rises or floats; the eye charges and glows
  in the orb colors before it fires. Built, two commits: the puffs on every frame of the four hover poses (`stand`,
  `walk`, `throw`, `ram`) with their own clock, off in the fall, landing, crouch and stun poses, and the tell gate
  removed; the `D1000Core/glow` strip under the eye during the pot shot, burst (first shot), Spread and Flank
  tells, frames 0 to 3 over the tell and the flash in the last 3 ticks, hidden at the shot, on a held shot, in the
  blink, the vanish and the spin. Values in `docs/NOTES.md`, "Round 11d". Not launched. Checks: see the section.
- 2026-11-11: round 11d merged (fast-forward to `6c6f104d9`). Its 3 commits carry the trailer `Claude Opus 5.5`, but
  the agent that wrote them ran on Fable 5.1 (the rules file changed during its run); the history stays as it is.
  engine 675, core 1602, smoke 456, JAR built. From here the lead runs on Opus 5.5.
  Open for the owner: the review's items 9 to 14 (the engine `StateMachine` and `Timer`s, table-driven sprite
  pre-processes, ticks to seconds for item 140, a named eye-to-body contract, and the question whether the
  per-action forecast earns its cost against the live release checks alone; answer that first).
- 2026-11-11: round 12 (lead, Opus 5.5, in the 081f worktree): owner notes from the play test. The claw jet puffs
  removed, the fan puff stays. More backoff: owner chose the bigger retreat (fair pocket 7 tiles, cap 2 s). Found
  and fixed: the pocket measure changed with the stand frame, so with 7 tiles the retreat did not start and the
  orbs were refused; the pocket now uses the farthest hover reach. Values in `docs/NOTES.md`, "Round 12". core
  1603, smoke 456, 0 failures. Not launched. Not committed.
- 2026-11-11: round 13 (lead, Opus 5.5, in the 081f worktree): owner notes from the play test. The aimed orb mark
  shows from the charge start, follows X, and holds for the last 0.6 s (owner chose "Track, then lock"). The
  Spotter eye tracks until it is over X (6 tiles/s, 3 s at most), so the body no longer faces away from X and
  bombs empty spots. Values in `docs/NOTES.md`, "Round 13". core 1603, smoke 456, 0 failures. Not launched. Rounds
  12 and 13 not committed.
- 2026-11-11: round 14 (lead, Opus 5.5, in the 081f worktree): the Spotter flashlight (owner idea; owner chose
  "Lock, then flash" and "Cone to a lane spot"). New art `D1000Core/beam.png` drawn by script, Bosses3 repacked; the
  pupil looks down during the lock. Values in `docs/NOTES.md`, "Round 14". core 1606, smoke 456, 0 failures. Not
  launched. Rounds 12 to 14 not committed.
- 2026-11-11: round 14b: owner did not see the beam in play; a headless 6-fight run showed fights with no Spotter.
  Owner chose to raise the Spotter weight (Normal 10 to 18, Overdrive 9 to 15); the Dive weight 40 to 43 keeps its
  share. core 1606, smoke 456, 0 failures. Not committed.
- 2026-11-12: `D1000.md` (lead, Opus 5.5): a full Mermaid state diagram of the fight beside `D1000.kt`, linked from
  `d1000/README.md` (owner request).
- 2026-11-12: round 15 (lead, Opus 5.5): the eye guides every volley; the Spotter action is gone (owner, plan page
  https://claude.ai/artifact/NA3emVZQ6x1s2MEp5QXnhd). Signal with a bounce and a thruster flare, a fast flight,
  the beam lock, then the throw; role scripts (hunt, cut, feint, sweep, herd); new art `D1000Core/thruster.png`,
  Bosses3 repacked. Values in `docs/NOTES.md`, "Round 15". core 1614, smoke 456, 0 failures. Not launched. Rounds
  12 to 15 not committed.
- 2026-11-12: round 15b: off-target marks (cut, herd, feint) land 2 to 3.5 tiles from X's lead point and never under
  the body; a feint with no room hunts X. Values in `docs/NOTES.md`, "Round 15b". core 1616, smoke 456, 0 failures.
  Not committed. Owner idea for the next round: horizontal volleys at the walls.
- 2026-11-12: round 15c: the Wall volley (owner idea; owner chose "Wall to wall", "X's lead height", "Air-time cue").
  Horizontal pillars from the far wall at X's height band, only when X spends over 40 % of the last 6 s in the air.
  No new art (the pillar and marker art turned 90°). Values in `docs/NOTES.md`, "Round 15c". core 1619, smoke 456,
  0 failures. Not committed.
- 2026-11-12: owner: play checks of rounds 12 to 15c pass; commit, then "merge to `master`". The subagent review
  before the play test and the Diffity review were not run for rounds 12 to 15c (owner went straight to the merge).
  `master` merged into the branch (`9621dd2ea`): one conflict in `MegamanXSprites.kt` (`060j` moved X's sprite and
  animator setup into `MegamanX.kt`); this branch's room-transition run pose and stun pose moved to the new place;
  the D-1000 tests call X's body and stun members (no longer extension functions). Checks: engine 675, core 1674,
  smoke 457, JAR built. Overlap: `060c` renames `MegamanXSabreSlash` to `PlayerSabreSlash`, also in `D1000.kt` and
  `D1000Core.kt`; this branch adds new uses of the old name. Owner chose to merge now; `060c` does the rename on the
  new D-1000 lines when it next takes in `master`.
