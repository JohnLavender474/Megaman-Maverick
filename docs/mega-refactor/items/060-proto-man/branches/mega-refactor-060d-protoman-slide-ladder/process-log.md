# Process log: mega-refactor-060d-protoman-slide-ladder

## Status

- State: merged
- Goal: Proto Man's slide and ladder, with Megaman's shared rules
- Brief: https://claude.ai/artifact/CrX5wFKCXJHqVQcKDg3pF5
- Done: planning (scan, `SCOPE.md` and `CONTRACT.md` approved 2026-11-14, `PLAN.md`); `PLAN.md` steps 1-9
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-14

- Branch start. Merge target: `master` (item decision). Owner: Proto Man shares Megaman's slide and ladder
  rules.

## 2026-11-14 (planning)

- Scan: code scan and art scan in `temp/mega-refactor-060d-protoman-slide-ladder/` (`scan-code.md`,
  `sprites/notes.md`).
- Owner answered all questions (Q1 to Q11): see `docs/SCOPE.md`, "Decisions". `SCOPE.md` and `CONTRACT.md` set to
  approved 2026-11-14.
- Wrote `docs/SCOPE.md`, `docs/CONTRACT.md`, `docs/PLAN.md` (9 steps), `docs/BRIEF.src.html`, `docs/NOTES.md`; a bug
  candidate in `docs/TODO.md`. Rendered the brief to the temp folder with no `✗`.
- Room names of the control checks checked in the live TMX files: `REFACTOR_TEST_031` `ground_slide_blocker`,
  `REFACTOR_TEST_068` `slide`, `INTRO_STAGE` `room3`, `REFACTOR_TEST_081` `ladder`, `REFACTOR_TEST_060` `shield`; each
  has a player spawn.
- Claude's reading of "the crouch guard pulls out again": the full pull-out starts when the slide ends, with no
  0.15 s delay.
- Brief published: https://claude.ai/artifact/CrX5wFKCXJHqVQcKDg3pF5. Owner answers to the 3 open readings: the
  re-guard starts at once (as read); `UP`+`B` in a slide (buster slot) ends the slide and does the single slash (not
  the slide shot); no slide during a slash (as read). `SCOPE.md` and `CONTRACT.md` updated.

## 2026-11-14 (steps 1-3)

- Step 1: `MegamanGroundSlideTest` (46 cases) on the old `GroundSlideBehavior`, green on the old code. It drives a
  real `Megaman` with his real body (`defineBodyComponent`), the real jump and slide in a `BehaviorsSystem`, and
  `body.preProcess()` per frame.
- Recorded facts of the old code (not changed):
  - The cooldown timer starts unfinished, so a new behavior cannot slide on its first frame (the tests step 2 idle
    frames first).
  - `shouldPreserveMomentum(false)` is true except on snow, so the slide keeps its speed at the end on any floor
    (clamped to the run speed over ground; a momentum kill is asked in the air). Only snow stops it. The scope text
    "ice keeps the momentum" is a subset of this.
  - A gravity change locks `MOVEMENT` until the camera turn ends, so a slide cannot start in a turn; the tests clear
    the lock to test the start gravity rule.
- Step 2: `players/PlayerGroundSlide.kt` (`GroundSlideParams`: `minTime`, `maxTime`, `cooldown`, `speed`, `blocked`;
  `playerGroundSlideBehavior`). `Megaman.groundSlideBehavior()` is a thin call; the boost read has a `TODO` (item
  `024`). The debug-only side-block reads of the old act were dropped. `players/README.md` lists the slide.
  `MegamanGroundSlideTest` unchanged and green.
- Mutation check: 6 mutation batches of the shared slide, Megaman's params, `JumpBehavior` and `PlayerBody` (script
  and results in the main checkout `temp/mega-refactor-060d-protoman-slide-ladder/`); each of the 46 cases failed
  in at least one batch that broke its rule.
- Step 3: `RefactorTest060.tmx` width 240 to 312 tiles. Room `slide` (x 242-285, floor top 1 tile higher than the
  other rooms, as the `047` sand room): spawn `8`, flat run, `GroundSlideBlocker`, a 10-tile low ceiling with a
  1-tile gap, a 6-tile `IceBlock`, a 3-tile `QuickSand` pit with a `Death` under it. Room `ladder` (x 288-311): spawn
  `9`, a 5-tile floor `Ladder` to a ledge with a `LadderTop`, a 5-tile `Ladder` that hangs 3 tiles over the floor
  with a 2-tile platform at its top (as `REFACTOR_TEST_081` room `ladder`). Built with `level-builder`; named
  objects, properties and tiles by script. Read back with `level-analyzer`.
- Checks: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` after step 2 and at the end (the cached `engine:test` and `core:smokeTest` run again with `--rerun`): engine 689, core 1946, smoke 462 tests, 0 failed; the jar builds.

## 2026-11-14 (steps 4-8)

- Step 4: the owner's final art (round 1, not `groundslide_shoot_v3arm.png`) in `assets/sprites/frames/Protoman_v2/`;
  atlas repacked with the `060c` packer (33 regions, each byte-equal to its frame file). Muzzle points in
  `docs/NOTES.md`.
- Step 5: `GROUND_SLIDING` through `playerGroundSlideBehavior` (`blocked = { sabre.active }`), `crouchHeight` 0.9,
  `groundSliding` from the behavior, the `groundslide` keys, `BusterParams.slideOffset` with
  `BusterSlideOffsetMissingException` (X passes `null`). The slide guard break moved here from step 7: without it the
  guard pull-out on the held `DOWN` set the side speed to 0 on the first slide frame.
- Steps 6-7 (one commit): `CLIMBING` through `playerClimbBehavior` with Megaman's rules, the climb keys, the climb
  offset; the guard, the crouch attack and the crouch shot pose read the slide and the ladder; the `B` order of
  `SCOPE.md` item 6. The 3 `060d` `TODO`s are gone. Decisions (no `canStart` climb rule, no `onStart`, the sabre slot
  combo from a slide) in `docs/NOTES.md`.
- Step 8: `players/README.md` (the slide offset, the slide and ladder callers, the guard breaks); `NOTES.md`.
- Test heap: the full `core:test` ran out of its 1 GB heap at the end of `ProtomanTest`. Heap dump analysis (script
  `temp/mega-refactor-060d-protoman-slide-ladder/mutation-steps4-8/hprof_histo.py` in the main checkout): about 49 000
  MockK invocations with stack traces, kept by a statically held mock. Fix: `clearAllMocks(answers = false)` in the
  `afterEach` of `ProtomanTest`. Small docs fix out of scope: `core/src/test/CLAUDE.md` said the test heap is 512 MB;
  it is 1 GB, and the line now names the fix.
- Checks: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar --rerun`: engine 689, core 2004, smoke 468 tests, 0 failed; the jar builds.
- Mutation check (script and results in `temp/mega-refactor-060d-protoman-slide-ladder/mutation-steps4-8/` in the main
  checkout): 21 mutations of `Protoman.kt`, `ProtomanBuster.kt` and `PlayerBuster.kt` in 7 batches; each new rule
  made at least one named case fail. 2 cases did not fail at first and were fixed: the ladder foot case stepped twice
  (the second step grabs again with the feet), and the single slash case did not run the slash out.
- Slide shot height: Proto Man 0.8125 tile over the floor, Megaman 0.5 tile (+0.3125 tile); details in `NOTES.md`.


## 2026-11-14 (step 9)

- Review before the play test, 3 subagents (branch diff; `REFACTOR_TEST_060` checks; control checks), no game
  launch. Every manual check is valid, its room loads and holds the setup; no bug found. Control checks: no finding;
  the shared slide is the old one rule by rule.
- Fixed:
  - `CONTRACT.md` ice check: run speed (the end clamp), not slide speed, and only with `DOWN` released at the end; the
    sand pit sinks him into a `Death`.
  - `CONTRACT.md`: `DOWN` on the floor at a ladder foot grabs the ladder (Megaman's rule), so no crouch guard there.
  - `SCOPE.md` in-scope 5: no climb rule in `sabre.canStart` (step 7 decision).
  - `PlayerGroundSlide.kt`: blank lines between short statements (`CODE_STYLE.md`).
  - `ProtomanBuster.kt` `TODO` points to `docs/TODO.md`, which now lists the ladder-foot shot.
  - `ProtomanTest` `afterEach`: `clearAllMocks` with the same arguments as `ProjectConfig.kt`.
- Checks with `--rerun`: engine 689, core 2004, smoke 468 tests, 0 failed; the jar builds.

## 2026-11-14 (play test, scope change 1)

- Owner: `jump_shoot` fired from the wrong hand; redrawn (version A), installed, atlas repacked, no offset change.
- Owner: the jump family is smaller than the ground poses. Proportion audit by an art agent (temp `proportions/`);
  scope change 1 in `SCOPE.md`: rebuild `jump`, then the other 6 jump-family strips. Slide unchanged.
- Owner chose version D for `jump` (lean arms); D's body carried into the other 6 strips; all 7 installed, atlas repacked.
- Owner: the art loops with Claude; a new branch `060k` (after `060d` merges) is for the owner's own edits of the Proto Man frames, with Claude on the tooling only (atlas repack, size and frame-count checks, muzzle points and shot offsets). The scope change 1 strips stay in `060d`.

## 2026-11-15 (play test, bug)

- Owner: mashed `B` in a crouch shot keeps Proto Man crouched after `DOWN` is released. Cause: the `060c` clause
  `crouchShooting ||` in `crouchAttackWanted()`. Removed; new `ProtomanTest` case (standing shot and stand on `B`
  after the release). Details in `docs/NOTES.md`.
- Owner, scope change 2: the slide end felt stiff. Megaman's 0.15 s slide-to-crouch delay added to Proto Man
  (`crouchBlocked()` during the delay); `SCOPE.md`, `CONTRACT.md`, `NOTES.md` updated; the slide-end `ProtomanTest`
  case now checks the delay.
- Checks after both fixes: core 2005, smoke 468 tests, 0 failed. Mutation check: the old `crouchShooting ||` clause
  fails only the new crouch case; the delay rule removed fails only the slide-end case.
- Owner: the play test passes. Diffity round 1: 0 threads. The owner closed the review; the 2 fixes are committed.

## 2026-11-15 (merge)

- `master` merged into the branch (`5e6ca069b`, no conflicts; `master` changed `ProtomanTest` and
  `PlayerDamageNegotiator`). Checks with `--rerun`: engine 689, core 2125, smoke 468 tests, 0 failed; the jar builds.
- Overlap with `024a` (`GroundSlideBehavior.kt`, the slide block line): owner chose to merge `060d` first. The
  resolution for `024a` is in `docs/NOTES.md`.
