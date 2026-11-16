# Process log: mega-refactor-077b-desert-man

## Status

- State: merged (2026-11-16)
- Goal: a richer Desert Man boss fight on the `077a` shared parts (health phases, tuning rows, tells)
- Done: all play-test rounds; the final test pass; the review; the merge
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-15

- Branch start on `master`. The owner chose Desert Man as the second boss of item `077`, because another agent
  will soon work on item `075` (a larger oasis section in Desert Man's stage). Overlap risk: both can touch the
  Desert Man TMX; this branch changes at most the boss object's properties in the boss room.
- Review of the fight (research subagent; `temp/077b-desert-man/boss-review.md`). Owner answers: ideas 1, 2, 3, 5 and
  the long-punch arm gap; the Wily rematch gets the same fight; damage to the player stays 3 with a `TODO`.
- Scan of the arm gap: extensions placed from the body edge, not the sprite's arm end (22 px gap); see `NOTES.md`.
- `SCOPE.md`, `CONTRACT.md`, `PLAN.md`, `TEST-DEBT.md` written. No brief: the owner answered every decision during the
  review, so the brief would only repeat them.
- `PLAN.md` steps 1 to 5 (implementation agent; fast iteration round: no Gradle, no tests, no launch):
  - `DesertMan.kt`: long-punch extensions centered from the body center with their real size; the hit box extra
    width 1.25 to 1 tile, so that it ends at the drawn fist; 2 health phases with the phase-2 bar region; the phase-2
    start (shake, `QUAKE_SOUND`, `dance_flash` at the next `STAND`); the class-keyed damage map; a roll on every
    attack guard; the punch wind-up (`punch_short` white fist, `REV_SOUND`, arm off); the tornado timer fix, the
    spin-up, the direction lock and the phase-2 turn; the burrow and erupt states with the dark-room light;
    `previousAttackState` reset in `onSpawn`; `left_wall` / `right_wall` used for the trail clamp.
  - New `entities/bosses/desertman/DesertManTuning.kt` (4 rows).
  - `PlayerDamageNegotiator.kt`: `DesertMan` 3 and `Needle` by owner 3, with the `TODO`.
  - `known-entity-failures.txt`: the `DesertMan` line removed (no request for the missing `init` region).
  - Differences from the scope and the values to watch: `NOTES.md` "Implementation notes". Spec lines:
    `TEST-DEBT.md`.

## 2026-11-15 (iteration: needle loop, tornado look)

- Commit `4d3c94ec1`: steps 1 to 5. Owner play test:
  - He could stand on one spot and dance (needles) again and again. Cause: the last `STAND` fallback guard was
    `DANCE` with no check of the previous attack. It is now `JUMP`, so the dance never comes twice in a row.
  - The spin-up looked like bouncing: the 2 `tornado_start` frames (low, high) looped. Now they play once (0.2 s,
    `TORNADO_RISE_DUR`), then the spinning `tornado` frames until he moves.
  - In the old fight the fist stayed forward for the whole moving tornado (the `tornado_punch` timer never updated);
    the owner prefers that look, so the timer update is removed again: the fist and the short-punch damager stay on
    while he moves.
- No Gradle run (fast iteration rule).


## 2026-11-15 (iteration: sink and rise art)

- Owner: the body only moved down, with no animation or effect. New frames in Desert Man's palette, drawn from his
  own parts (hood, fists, torso), cut at the sand line (frame row 55, the body bottom), with a sand mound and spray:
  `DesertMan/burrow` (4 frames: crouch with fists up, chest in, head and fists in, sand closes) and
  `DesertMan/erupt` (3 frames: hood tip through a sand bulge, head and fists burst out, jump pose in a spray). Both
  are 1-row strips (256x64, 192x64): the atlas has no free 64-wide column of that height. Drafts and the contact
  sheet: `temp/077b-desert-man/burrow-art/`.
- `BURROW`: the sprite stays at the sink point and plays `burrow` once; the body moves 1 tile down with block
  collision, gravity and all fixtures off until the end of `BURROW_TELL`. A `DustPuff` at the sink point.
- `ERUPT`: the body goes to the trail end on the sand, stops for 0.3 s while `erupt` plays once (a `DustPuff`,
  a splash and `DIG_SOUND`), then jumps at the row's impulse with `WIND_1_SOUND` (as before). During the 0.3 s the
  body, damager and damageable are on at the normal height.
- No Gradle run (fast iteration rule).

## 2026-11-15 (iteration: long punch rebuild)

- Owner: the long punch had many small faults; rebuild it from scratch, smooth, new art allowed.
- Faults of the old punch: the arm grew in fixed 112 px steps (one extension sprite each 0.2 s), so it looked
  choppy; 22 px gap near the body before the gap fix; the hit box was at full length while the extension frames
  still grew; the hit box sat about 7 px above the drawn arm (`ARM_OFFSET_Y`); the reach did not stop at the wall
  (the arm went into or past it); the wind-up showed the white streak of the old punch, not a held fist.
- New art (in the atlas and in `assets/sprites/frames/Bosses2/DesertMan/`): `punch_body` (the `punch` pose without
  the forearm), `punch_arm` (192x5 arm strip, black with a dashed white highlight), `punch_fist` (2 frames: green,
  white flash), `punch_speed` (2 frames of speed lines). Drafts and the 4x contact sheet:
  `temp/077b-desert-man/punch-art/contact-sheet.png`. The old regions stay in the atlas, unused.
- `PUNCH` now has 4 phases (`PunchPhase`): wind-up (fist pulls back, flashes, body trembles), extend (`pow2Out` ease
  to 9 tiles or the wall, row speed), hold (row time), retract (`pow2In` at 16 tiles/s). The arm is one sprite cut
  from the strip to the current length, from the shoulder, so it never has a gap. The arm and fist fixtures are
  set from the same length and the same art rows as the sprites. A wall stop gives a sand splash, a short camera
  shake and `POUND_SOUND`.
- Tuning rows: new `punchExtendSpeed` (24 / 28 / 28 / 32 tiles/s) and `punchHoldDur` (0.35 / 0.3 / 0.3 / 0.25 s).
  The punch guard range is now 8 tiles between centers (was 6 tiles between left edges).
- No Gradle run (fast iteration rule).

## 2026-11-15: smoother long punch, wider wind lines

- Owner: the punch looks good; make it smoother (about one step per 1/2 tile), expand the wind effect a little, and
  keep the fixtures up to date on each tick.
- The arm is drawn at its real length in each frame, not from frames. The faults: the drawn and hit length came from
  a copy made only in the body pre-process, so on a frame with no physics step the arm did not move; and the
  `pow2Out` start was 2x the average speed (about 0.8 tile per frame).
- Fix: the sprites and the fixtures read the arm length directly, and `updatePunchFixtures()` runs after each punch
  update and in the pre-process. The extend uses `sineOut` (retract `sineIn`), with a peak speed cap of 30 tiles/s,
  so a step is 0.5 tile or less at 60 FPS. The extend over 9 tiles now takes about 0.45 s in all rows (was 0.28 to
  0.37 s), so the cap removes the per-row extend speed difference.
- `punch_speed` art: 16x13 to 22x17 art px per frame (longer lines, more spread), new atlas spot.
- No Gradle run (fast iteration rule).

## 2026-11-15: needle throw attack

- Owner: Desert Man is too easy; add an attack where he winds up his fist and throws it forward (arm not stretched),
  and needles come out.
- New state `THROW` with 2 phases (`ThrowPhase`): wind-up (`throw_windup`: body leans back, fist cocked at the chest,
  white flash, tremble, `REV_SOUND`) and throw (`throw`: fist forward at normal arm length, `throw_burst` star at the
  fist, a straight needle fan aimed at the target, `BURST_SOUND`). More volleys repeat with a 0.3 s wind-up; even
  volleys are staggered so that the gap is on the old aim line.
- Tuning rows: new `throwChance`, `throwWindUpDur`, `throwHoldDur`, `throwVolleys`, `throwNeedles`,
  `throwNeedleSpread`, `throwNeedleSpeed` (values in `DesertManTuning`).
- Art: `throw_windup` (64x128, 2 frames), `throw` (64x64), `throw_burst` (14x42, 3 frames); strips in
  `assets/sprites/frames/Bosses2/DesertMan/`, drafts and the 4x contact sheet in `temp/077b-desert-man/throw-art/`.
  The atlas had no free 64x128 spot, so `Bosses2.png` grows from 1024x864 to 1024x1024 (new empty rows at the
  bottom; no existing pixel or region moved).
- No Gradle run (fast iteration rule).

## 2026-11-15: needle spin attack

- Owner: Desert Man is too easy; add an attack where he jumps, spins in the air and throws needles, as Mega Man's
  needle spin.
- Mega Man's `NEEDLE_SPIN` (`MegamanWeaponsHandler.spinNeedles()`): each shot throws a fan of `Needle`s over the
  upper half (15 tiles/s, gravity -15) and sets his y speed up, so repeated shots hover him; the art is a 4-frame
  yaw spin (front, side, back, side).
- New state `NEEDLE_SPIN`: a straight jump up (12 tiles/s), then at the top a burst each row delay; each burst
  throws a fan of `Needle`s and pops him up 5 tiles/s. The fans alternate (7 needles at 30-degree steps, then 6
  needles between them), so a safe gap of one burst is hit by the next. Then the spin goes on for 0.3 s, he falls
  with `jump` and lands in `STAND`. The guard in `STAND` is after the tornado and before the needle throw.
- Tuning rows: new `needleSpinChance` (0.45 / 0.5 / 0.5 / 0.55), `needleSpinBursts` (2 / 3 / 3 / 4) and
  `needleSpinBurstDelay` (0.45 / 0.4 / 0.4 / 0.35 s).
- Art: `needle_spin` (256x64, 4 frames: front, side, back, side mirrored; arms up, spin rings on the side frames).
  Strip in `assets/sprites/frames/Bosses2/DesertMan/`, drafts and the 4x contact sheet in
  `temp/077b-desert-man/spin-art/`. Atlas spot (130, 801), inside the rows that the throw work added.
- No new damage entry: `Needle.TAG` is 3 in `PlayerDamageNegotiator`.
- No Gradle run (fast iteration rule).

## 2026-11-15: needle spin tell, easier on NORMAL

- Owner: not enough tell or delay before the needle-spin jump; too hard for NORMAL (it may suit HARD).
- New tell: he stands still for the row's tell time (0.6 / 0.5 / 0.35 / 0.3 s) with the existing `jump_flash`
  frames (crouch, white flash) and `REV_SOUND`, then jumps.
- NORMAL rows are easier: burst delay 0.6 / 0.5 s (was 0.45 / 0.4 s), NORMAL_PHASE_2 bursts 2 (was 3), needle speed
  11 / 12 tiles/s (was 15). The HARD rows keep the old values, plus the shorter tell.
- No Gradle run (fast iteration rule).

## 2026-11-15: intro pose in place of the dance wiggle

- Owner: during the entrance and the health bar fill he loops the 2-frame `dance` wiggle, which looks unrefined.
- New region `DesertMan/intro` (448x64, 7 frames, 1 row): landing crouch with a sand spray, crouch with the spray
  thinning, stand, fist raised (the arm of `needle_spin` frame 0), a glint on the hood spike and the fist, a small
  hood glint, then the fist-raised pose again as the held frame.
- `INIT` uses `intro` (one shot, 0.85 s to the held frame) on the ground and `jump` in the air. The pose is complete
  before the 1 s `INIT` timer ends, so the fill (`HoldWorld` keeps the animations on) shows the held pose. Same
  pattern as `InfernoMan` (`init`, one shot).
- Strip in `assets/sprites/frames/Bosses2/DesertMan/intro.png`; atlas spot (387, 865). Drafts, `draw.py`,
  `atlas.py` and the 4x contact sheet in `temp/077b-desert-man/intro-art/`.
- No Gradle run (fast iteration rule).

## 2026-11-15: limb contrast on the sunset background (option A)

- Owner: after the cave, the sunset background is black in its lower half, so his black arms, legs and the long-punch
  arm vanish. An agent previewed 3 options over the real boss room; the owner chose A.
- Option A: black limb and shorts interiors became dark grey `(90,89,89)` (palette index 9, already in the palette);
  the 1 px black outline, the face mask, the flash/silhouette frames and parts only 1 to 2 px thick stay black.
- 20 strips, 47 frames, 3103 pixels; written into the same atlas regions; no pixel outside the Desert Man regions
  changed; `Bosses2.txt` unchanged. No code change.

## 2026-11-15: grey outline, intro arm, needle spin approach

- Owner: the grey reads better as an outline with black inside. The recolor now starts again from the black art: in
  each limb part (the old grey area plus its black border), the pixels next to transparency are grey and the rest is
  black. A cell edge does not count as transparency, so a part cut at the cell edge (the long-punch arm) has no grey
  bar there. Same 20 strips, same atlas regions.
- Owner: the raised arm in `intro` frames 3 to 6 was 1 px too thin and 1 px too long. The fist moved 1 px left and
  1 px down, and the arm is 1 px wider in each row.
- Owner: the needle spin made no move toward a far target. The jump now carries him toward the target while he
  rises (stop 3 tiles away, at most 6 tiles, inside the arena); he spins in place from the first burst.

## 2026-11-15: damage table 1-for-1 with `master`

- Owner: he died with fewer charged shots than on `master`. Cause: the `077a` scale (pellet 3, half 4, full 6) on
  2 phases (60 bits): 10 full charges in place of 15. Now each value is 2 × the `master` value, so every weapon takes
  the same number of hits. The rule for later bosses is an item decision on `master` (`ITEM.md`, 2026-11-15).

## 2026-11-15: needle spin needles at the dance impulse

- Owner: the needle spin needles take the same impulse as the dance needles. They now use `NEEDLE_IMPULSE`
  (15 tiles/s) in every row; the tuning field `needleSpinNeedleSpeed` (11 / 12 / 15 / 15) is removed.

## 2026-11-15: erupt jump aims at the target

- Owner: the erupt must jump high enough to reach Mega Man and move left or right toward him when he is not straight
  above. The y speed now lifts him to 1 tile above the target (at least the old impulse), and the x speed reaches
  the target x at the top of the jump (at most 12 tiles/s, inside the arena). X friction is off during the jump.

## 2026-11-15: needle spin warm-up frames

- Owner: the needle spin needs a clearer warm-up: arms crossed and knees tucked up. An art agent drew
  `needle_spin_tell` (ground crouch, arms crossed; normal and flash frames) and `needle_spin_tuck` (in the air, knees
  up, arms crossed) and packed them into `Bosses2` (no other region changed). The tell now shows `needle_spin_tell` in
  place of `jump_flash`, and the rise shows `needle_spin_tuck` in place of `jump`. The `jump_flash` region stays in the
  atlas, unused by Desert Man.
- Owner: the crossed arms seemed to grow out of the mid body, and the tuck frame read as a "fat balloon". Second pass
  (same agent, review first): each arm now starts at its `stand` shoulder joint, an upper arm goes down to an elbow
  at the side of the belly, and the forearms cross up over the chest to fists at the opposite shoulders. The tuck keeps
  the `jump` head and torso size, with thighs up and shins folded under. Same regions, repacked in place; no pixel
  outside them changed.
- 2026-11-16, owner: the crossed arms are too long. Third pass: forearm about 8.5 px (was about 12.5), elbow 1 px
  lower; the fists sit on the upper chest under the face mask; orange shows at the crossing. Same regions, in place.

## 2026-11-16: final pass and merge

- Owner: "I beat the boss. Very fun. Good job! Let's merge" (the fight is final; the play test passed).
- Merged `master` into the branch (`7087c1913`): one conflict in `DesertMan.kt`; `master` moved the random calls to
  `services.random` (`011a`); the branch's 8 `getRandom` calls now use `services.random.nextFloat`.
- Tests: an agent wrote 135 Desert Man tests in 7 files (`core/src/test/.../bosses/desertman/`) from `TEST-DEBT.md`,
  with a mutation check per file (8 mutations, each caught). Owner approved 2 `internal` members for the tests
  (`DesertManState`, `currentState`; no public flow shows the state), as in `077a`.
- Review (1 agent, read-only): 7 findings. Fixed: X friction not reset in `onSpawn`; the dead `tornado_punch` timer
  and `PUNCH_DUR`; the dead `jumpChance` roll and field; the 2 equal branches of the `Needle` player damage entry; the
  state-machine comments, a log variable, a named argument, a double blank line; stale `TEST-DEBT`, `CONTRACT` and
  `SCOPE` lines (the contract now names the throw, needle spin, long punch, aimed erupt, intro pose and the art). The
  manual check commands are valid (`room14`, `room16`, `WILY_STAGE_1` `room9`).
- The test pass found 2 small gaps, fixed: the punch reach used the trembling shoulder (up to 1 art px off at a wall),
  now the rest shoulder; the needle spin kept its x speed for the tick of the first burst, now 0 from that tick.
  Bug and backlog candidates in `docs/TODO.md` (light source keys parse; the dead extend speed).
- Automated checks: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` pass (2974 unit tests, 468 smoke
  tests, 0 failures; the `DesertMan` smoke line stays removed).
- Second `master` merge before the squash (`master` changed the sabre API and Proto Man): no conflict; the same checks
  pass again. Overlap: `fix/small-bugs-3` changes one line of `DesertMan.kt` (the light source keys regex, the bug
  candidate above); this branch does not touch that line.
