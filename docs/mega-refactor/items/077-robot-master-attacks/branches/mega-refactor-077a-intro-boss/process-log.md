# Process log: mega-refactor-077a-intro-boss

## Status

- State: active
- Goal: A richer intro-stage boss fight (`BigAssMaverickRobot`) and shared boss health phases
- Done: branch start; scan; `SCOPE.md`, `CONTRACT.md` approved; `PLAN.md` written; `PLAN.md` steps 1 to 11
  (commit `d5fdab1a9`; Diffity open); step 12 (commit `8ee7abc7d`); step 13 (uncommitted)
- Brief: https://claude.ai/artifact/GerPzr5o6ywUnUCgpUuuvH
- Next: the squash merge into `master` (owner: "ready to merge", 2026-11-15); the final pass and the review before
  the merge are done (see the entries "final pass" and "review before the merge")
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-13

- Branch start. Owner chose `077` while `060c` runs in another session; one branch per boss, this one for the
  `INTRO_STAGE` boss. Merge target: directly into `master`.
- Research before the branch (subagents, read-only): `temp/077-intro-boss/boss-review.md` and
  `overdrive-health.md`.
- Owner answers (2026-11-13): scope is the shared health phases, the hand state machine (orbit kept, plus new
  motions), the damage change (2/3/4), mixed-aim orbs and a hand slam, and a richer death. Phase 2 replaces the
  panic attack for a phased boss. Proto Man buster 3 on this boss (when `060c` merges); fix the special-weapon
  damage table in this branch.
- Planning steps 1 and 2 (subagent, read-only on code): scan of the boss, hand, orb, shock wave art,
  `AbstractBoss`, `AbstractHealthEntity`, `BitsBar`, `BossHealthHandler`, `fights/`, the `INTRO_STAGE` boss room
  and the `060c` diff. Research claims checked at the code; corrections in `docs/NOTES.md`. Drafts:
  `docs/SCOPE.md`, `docs/CONTRACT.md`, `docs/NOTES.md` (hand and mode state machines, health phases, bar, tuning
  rows, overlap with `060c`). Next: the brief with Q1 to Q4.
- Brief published (https://claude.ai/artifact/GerPzr5o6ywUnUCgpUuuvH). Owner answers: D1 shared red-orange phase-2
  bits; D2 phase-2 color shown, phase 1 fills over it; D3 special weapons 2, Axe 3, big Reactor 4, explosions 1 to 2;
  D4 one branch. Scope and contract approved. `PLAN.md` written (11 steps).
- Owner (after the brief): the hands must act in concert; for example the partner hand moves down while the
  attacking hand winds up. Added "Hand pair choreography" to `docs/NOTES.md` (conductor in the boss, partner reaction
  rows for punch and slam, the clap in lockstep, rider damping). The brief diagram was updated and published again.
  This adds to `PLAN.md` step 6; the implementation agent was told.
- 2026-11-14 (implementation subagent, steps 1 to 10, uncommitted for review):
  - Step 1: health phases on `AbstractBoss` (`healthPhases`, `healthPhase`, `healthBarRegions` with a fail-fast check
    in `init`, `onHealthPhaseStart`, `translateHealth` and `getHealthRatio` overrides; `TODO` for `depleteHealth`).
    Test `BossHealthPhasesTest` (8 cases) on a test boss through `takeDamageFrom` with real bullets.
  - Step 2: `boss_phase_2` bit (`#D82800` outer, `#FC9838` inner): `assets/sprites/frames/Bits/boss_phase_2.png`, packed
    into `Bits.png` (palette mode `P` kept, 20 -> 22 colors, 295 -> 314 bytes, height 74 -> 80) and `Bits.txt`. 8x
    preview in `temp/mega-refactor-077a-intro-boss/`. The smoke test now reads every boss's bar regions from `Bits`.
  - Step 3: `BossBitsBar` (pure `runs` function, 2 runs per frame); `BossHealthHandler.set` takes `AbstractBoss`. The
    D2 fill falls out of the run math (phase-2 color over the unfilled bits). Test `BossBitsBarTest`.
  - Step 4: the boss damage map (D3 and pellet 2 / 3 / 4, explosion 2 / 3) on a `BossDamageNegotiator` base;
    `TODO` for the `060c` rows. Test `BigAssMaverickRobotDamageTest` (17 cases, with `DAMAGE_INCREASE`).
  - Step 5: package `entities/bosses/bigassmaverickrobot/` with `BigAssMaverickRobotTuning` (4 rows),
    `BigAssMaverickRobotMotion` (modes, slots, schedule, spring) and `README.md`. Test `BigAssMaverickRobotMotionTest`
    (the orbit matches the old `RotatingLine` path).
  - Step 6: hand rewritten as the `HandState` enum with one `goTo` and one state clock; the boss is the conductor
    (target alternation in slot order, free hands, weighted choice, partner offsets from `HandChoreography`, clap
    mirror). Motion by velocity in the body pre-process. Tests `BigAssMaverickRobotFightTest` (on `BossRig`) and
    `BigAssMaverickRobotPartsTest` (choreography, attack choice); `BigAssMaverickRobotHandTest` uses the new props.
  - Step 7: `OrbAim` and `IOrbLauncher`: the orb asks its owner for the trajectory at launch; `BLAST_2_SOUND` at spawn.
    Other orb users keep the old path.
  - Step 8: `ShockWaveRow` (1 tile apart, 0.08 s); `BigAssMaverickRobotOrbExplosion` sliced 1x2, dissipate 0.5 tile,
    damage 2, damager only in the pulse.
  - Step 9: `onHealthPhaseStart`: shake 0.5 s, `QUAKE_SOUND`, 0.6 s `stunned` flash, orange arm orbs.
  - Step 10: 4 s death: chains pop hand to shoulder 0.15 s apart, hands bounce once, head shakes then sinks 1 tile,
    a last blast at 3.5 s; the arm orbs skip `onBossDefeated`.
  - Changes against the designs, and why: `docs/NOTES.md` "Implementation notes".
- 2026-11-14 step 11, automated checks: `engine:test` 675 tests, 0 failed; `core:test` 1845, 0 failed; `core:smokeTest`
  452, 0 failed; `lwjgl3:jar` builds. Mutation checks in 3 batches (`temp/mega-refactor-077a-intro-boss/mutate.py`,
  53 mutations): every one of the 63 new test cases failed under at least one mutation; all mutations restored. The
  `AbstractBoss` mutations also failed the `D1000` defeat and shield tests (the 1-phase path is covered). Open: the
  review before the play test, the commit and Diffity.

## 2026-11-14 (review fixes, uncommitted)

- Fixes from the review of steps 1 to 10:
  - Clap flash: `flashAt` spawns the orb with `ConstKeys.HIT` = `true`, and the orb's `onSpawn` enters the hit state
    (hit body size, `hit` animation, 0.2 s life, no hit sound). The field set after `spawn` was lost, because the
    engine runs `onSpawn` on the next update, so the flash orb stayed on screen.
  - `BossRig` queues spawns and runs `onSpawn` at the start of the next `tick()` (as `GameEngine.update`), and calls
    `canSpawn`. `processSpawns()` is for the projectile helpers and the bosses that a test spawns without a tick.
    `blockOf` in the fight test now pairs the hands and the blocks by spawn order (the old helper relied on the
    synchronous order). The rig has 2 player slots (`addPlayer`).
  - Head stun: the conductor runs before the stun gate; only the volley and the attack wait pause.
  - Co-op: `handTarget` moves to the next player only when an attack is ordered.
  - Health phases: the roll-over leaves at least 1 health in the next phase, so one large hit cannot start phase 2
    and defeat the boss.
  - `BigAssMaverickRobotTuning`: the unread `hard` and `phase` fields are removed.
  - Orb coupling: the hand spawns its arm orbs with `BigAssMaverickRobotOrb.POP_ON_BOSS_DEFEAT` = `false`; the orb no
    longer imports the hand. `IOrbLauncher` moved to `bigassmaverickrobot/`. `GutsTank` and `WilyFinalBoss` orbs keep
    the default.
  - Arena from map references (owner request, the `TimberWoman` pattern): `IntroStage_v3.tmx` object 335 has
    `ceiling` -> 332, `ground` -> 329, `left_wall` -> 328 and `right_wall` -> 331. New key `ConstKeys.CEILING`; the
    wall keys are `left_wall` and `right_wall` (`BigAssMaverickRobot.LEFT_WALL`, `RIGHT_WALL`). The constants
    `FLOOR_OVER_LANDING`, `ARENA_HALF_WIDTH` and `ARENA_HEIGHT` are removed. The map gives the same arena as before
    (x 10176 to 10624, floor at world y 1760, height 376 px = 11.75 tiles).
  - A double blank line in `BigAssMaverickRobot.kt` removed. `NOTES.md` "Implementation notes" and the package
    `README.md` updated.
- New tests (6): the arena from spawn props; a stun during a double clap keeps the mirror hand in step; the co-op
  hand target; a second hit in the same frame as a roll-over (through `canBeDamagedBy`, then `takeDamageFrom`); the
  charge explosion 2 / 3; the roll-over cap (hits of 60 and 75). The clap test now checks that each flash is in the
  hit state one step after its spawn and is destroyed after its life.
- Mutation checks (`temp/mega-refactor-077a-intro-boss/mutate-review-fixes.py`, 8 mutations, each reverts one fix):
  each one failed its target test; all restored.
- Automated checks: `engine:test` 675 tests, 0 failed; `core:test` 1851, 0 failed; `core:smokeTest` 452, 0 failed;
  `lwjgl3:jar` builds.

## 2026-11-14 (play test feedback: aimed slam)

- Commit `d5fdab1a9`: steps 1 to 10 and the review fixes. Diffity opened (base `035e5dbec`).
- Owner, during the play test: the slam had no real tell of where it hits, and could only hit the floor. Asked for a
  slam toward the player that can hit a wall, and shock waves around the room perimeter (`SuctionRoller` corner turn).
- Owner asked about art: only 90 degree steps without pixel rotation. Finding: both hand frames are a front view of
  the fist, so a dive at any angle needs no fist art; 45 degree steps are kept for readability. Owner took all
  recommendations: align (the tell), lock with a flash, dive; waves on the floor and the walls, turn at the corners,
  stop at the ceiling; a wall hit rests as a step.
- Scope change: `NOTES.md` "Aimed slam and perimeter shock waves", `CONTRACT.md` (manual check 1, 2 unit test lines,
  intended changes row), `PLAN.md` step 12, brief updated and published again. Uncommitted while the Diffity round
  is open.

## 2026-11-14 (plan step 12: aimed slam and perimeter shock waves)

- Implementation subagent, uncommitted (the Diffity round is open).
- New pure functions in `bigassmaverickrobot/`: `SlamAim.plan` (5 dive directions, start point, stop point, hit
  point and surface) and `ArenaPerimeter` (path length, offset of a hit point, offset to point and side).
  `ShockWaveRow` does not change: the hand walks it over the perimeter path.
- Hand: `slam(target, tuning)` takes the player center; `RISE` eases to the start point, `HANG` is the lock (clap flash
  at its start, `lockDur`), `FALL` dives at 18 tiles/s to the stop point, `REST` rests on the floor or a wall. Both
  waves of a slam start at the hit offset, one each way. `punchDirection` is now `attackDirection`.
- Boss: the slam order passes the player center; `SLAM_EDGE_GAP` removed.
- Tuning: `hangDur` is now `lockDur` (0.4 s; 0.3 s in normal phase 2, hard, hard phase 2).
- Pillar: a wall pillar sets `Body.direction` `RIGHT` or `LEFT`, so its body, damager and sprite turn 90 degrees, with
  the base on the wall.
- `DIP` uses the vertical part of the dive direction.
- Differences from the design: `docs/NOTES.md` "Implementation notes (2026-11-14, plan step 12)". The optional
  exhaust puff is not added. `entity-tags.txt` does not change (no class or tag change).
- New tests: `SlamAimTest` (14: each direction, the wall face stop, the lead shortened at the ceiling, a grid of fists
  and targets, the corner, the ties, the skip of a direction with the target behind; the perimeter points, the corner
  and wall-top gaps, the offsets, a floor wave and a wall wave walked to the path ends); 2 fight tests in place of the
  old slam test (a floor hit and a wall hit: the pillar sides and turns, the rest place, a rider on the resting fist,
  the step that follows the fist, the lock flash, back to `IDLE`); 1 parts test (`DIP` by the dive direction).
- Mutation checks (`temp/mega-refactor-077a-intro-boss/mutate-slam.py`, log `mutate-slam.log`): 14 mutations, each
  failed at least one of its target tests; all files restored (checked against a backup copy).
- Automated checks: `engine:test` 675 tests, 0 failed; `core:test` 1867, 0 failed; `core:smokeTest` 452, 0 failed;
  `lwjgl3:jar` builds.

## 2026-11-14 (review of plan step 12: findings and fixes)

- Review findings: (1) a boss defeat during a side or diagonal `FALL` (or a punch `LAUNCH`) kept the hand
  `velocity.x` in `DEFEATED`, where only gravity runs, so the fist slid into or through a wall; (2) `SlamAim` accepted
  a start a fraction of a tile from the target, and the fist hurts in `RISE`, so it could hit before the lock flash;
  (3) the `best = 0` fallback in `SlamAim` was silent and reachable only with bad map data; (4) `EPSILON` and `TIE`
  were raw pixel values, and `plan` made a `Vector2` per call; (5) the contract said "stand near a wall" for a wall
  hit, but a floor player gets one only when touching the wall.
- Fixes, uncommitted (the Diffity round is open): the hand zeroes its velocity on entering `DEFEATED`; `SlamAim`
  accepts a direction only when the start is at least the fist half size plus 1 tile from the target along `d`, and
  throws `IllegalStateException` when no direction passes; `EPSILON` and `TIE` are in tiles scaled by `ConstVals.PPM`;
  the candidate vector is a field. `CONTRACT.md` manual check 1 and the `NOTES.md` implementation table and open risks
  are updated (new risk: a low wall stop puts the step block at about the head of a player against the wall).

## 2026-11-14 (play test feedback: clap)

- Commit `8ee7abc7d`: step 12 and its review fixes.
- Owner, play test: in phase 2 both fists shook and ran into the center and never aimed at Mega Man. Cause: the
  double clap aimed only in height (floor + 1 to 5 tiles) and met below the head with a 1.5-tile gap, a safe spot.
- Owner chose to replace it with a pincer clap on the player position (twice, slam-style lock). `NOTES.md` "Pincer
  clap", `CONTRACT.md` (check 1, intended changes, a unit test line), `PLAN.md` step 13.

## 2026-11-14 (plan step 13: pincer clap)

- Code: `ClapAim` (new pure function: meet point, start and stop of each fist); hand states `CLAP_ALIGN`,
  `CLAP_LOCK`, `CLAP_CLOSE`, `CLAP_IMPACT`; the boss conductor changes the clap step of both hands at the same time
  and aims the second clap again at the target player; the mirror hand, the clap holds and the clap height constants
  are removed; tuning field `clapSpeed` (16 tiles/s) replaces `clapHolds`. `NOTES.md` "Pincer clap" implementation
  notes; package `README.md`.
- Owner, during the step: tests deferred until the moveset is final. No test run, no mutation checks. `ClapAimTest`
  and 3 new fight tests stay as written, not run. The check is the `lwjgl3:jar` build.

## 2026-11-14 (iteration: easier fight, master-style death)

- Commit `e84159d7b`: step 13, pincer clap (tests deferred).
- Owner, play test: the fight is good but much harder than before; the death had fewer explosions than `master`; the
  severed head must keep the aghast face. Fast iteration round (no tests; `lwjgl3:jar` builds):
  - Damage about +50%: pellet 3, half 4, full 6, charge explosion 3 / 4; specials 3, Axe 4, big Reactor 6,
    explosions and shards 2.
  - Slower attacks: punch launch 6 to 9 tiles/s (hard 8 to 12), slam dive 13.5 tiles/s, clap close 12 tiles/s, lock
    0.5 / 0.4 s (normal phases 1 / 2), phase-2 attack wait 2.25 s.
  - Shock waves stop 0.6 s after they start (`ShockWaveRow.LIFE`).
  - Death: explosions for the whole 4 s (as `master`), and the head shows the `defeated` face the whole time.
  - `TEST-DEBT.md` updated.

## 2026-11-14 (iteration: 1-bit damage to the player)

- Owner: every damager of this fight takes 1 bit from the player. `PlayerDamageNegotiator`: head and hand 2 -> 1;
  new entries for the shock wave pillar (1; its own `getDamageToMegaman` value is removed, so the table is the only
  source) and for `BigAssMaverickRobotOrb` (1 when the owner is this boss or its hand, else 3, the old `MEDIUM` size
  damage for `GutsTank` and `WilyFinalBoss`). Rule recorded in `docs/WAYS_OF_WORKING.md` on `master`. No Gradle run
  (fast iteration rule).

## 2026-11-14 (iteration: shock wave sound)

- Owner: the shock wave uses the sound of `temp/proto-sabre-sfx/combo_2.ogg`, once per pillar. `combo_2` is
  `variant_2_heavy_hit` 3 times (README in that folder), so the single hit `variant_2_heavy_hit.ogg` (217 ms) is
  copied to `assets/sounds/shock_wave_hit.ogg` as `SoundAsset.SHOCK_WAVE_HIT_SOUND` (volume 0.6). `spawnPillar` plays
  it; the `MM3_ELECTRIC_PULSE_SOUND` at the wave start is removed (`POUND_SOUND` at the impact stays). No Gradle run.

## 2026-11-14 (end of session: open list from the owner)

- Fast iteration rounds since `e84159d7b`: `d944168b7` (easier, master-style death), `5c35c88d9` (1-bit damage),
  `966603ddf` (shock wave sound). None of them ran Gradle; the owner's run compiles.
- Open for the next session (owner):
  1. Balance of fun and fairness, with NORMAL and HARD each tuned on their own (the 4 tuning rows).
  2. A more mature death scene: the X eyes of the `defeated` face look silly. The owner asked for a surprised,
     aghast face on the severed head; this needs new head art or another existing frame.
  3. With more than one health phase, the intro fill of the bar loops through each phase (this changes D2: today
     the phase-2 color shows from the start and only the phase-1 bits fill). Shared `AbstractBoss` / `BossBitsBar`
     behavior, so every phased boss gets it.
- The Diffity server is stopped for the night (no open threads). Start it again with `diffity-open` from the worktree.

## 2026-11-15 (iteration: tuning per mode, death face, phased bar fill)

Owner's 3 points of 2026-11-14; the owner left points 1 and 2 to Claude. No Gradle run (fast iteration rule).

1. Tuning per mode (normal 1 / normal 2 / hard 1 / hard 2). Aim: normal is fair for a first boss; hard is a clear
   step up but every attack keeps a readable tell.
   - Attack wait 3 / 2.25 / 1.75 / 1.25 s (hard was 1 / 0.75: too little time to read a tell).
   - Wind-up 0.75 / 0.6 / 0.55 / 0.45 s; lock 0.5 / 0.45 / 0.4 / 0.35 s; rest 1.2 / 1 / 0.9 / 0.7 s.
   - Wave pairs 1 / 1 / 1 / 2 (normal phase 2 was 2); new per-row wave life 0.5 / 0.6 / 0.6 / 0.8 s.
   - Clap close 12 tiles/s normal, 14 hard; orb gap 1 / 0.9 / 0.6 / 0.4 s; hard phase-2 volley delay 1.5 s.
   - Unchanged: weights, punch speeds (6 to 9 normal, 8 to 12 hard), dive 13.5 tiles/s, 1-bit damage to the player.
2. Death face: `Head/defeated` redrawn as an aghast face (wide eyes, small trembling pupils, open mouth) in the frame
   file and in the `Bosses3` atlas (palette mode `P` kept; no pixel outside the region changed). The old frame 2 was
   empty, which made the X-eyes face blink; now both frames show the face. Explosions run the whole 4 s (last round).
3. `BossHealthHandler`: the intro fill loops through the phases, last phase first (replaces D2).

## 2026-11-15 (iteration: no IOrbLauncher)

- Owner: remove `IOrbLauncher` (one implementer; it named the coupling but did not remove it). The boss now passes a
  trajectory function as the spawn prop `ConstKeys.SUPPLIER` (`(from, out) -> out`); the orb calls it when its hold
  ends. The orb no longer imports the `bigassmaverickrobot` package, and `ConstKeys.MODE` / `PLAYER` are no longer
  orb props. `GutsTank` and `WilyFinalBoss` pass no supplier and keep their path. No Gradle run.
- The 2 research agents (`ArenaPerimeter` reuse, package folder naming) stopped when the owner interrupted a turn;
  no report. To dispatch again.

## 2026-11-15 (small QoL change: `--difficulty` launch option)

- New dev start option `--difficulty normal|hard` (case-insensitive; a bad value exits with code 2). It sets
  `GameState.difficultyMode` before the first screen, after the level override. A play test can now start a level
  in hard mode with no visit to `SELECT_DIFFICULTY_SCREEN`. A choice in that screen and a save load from the main
  menu replace the value. With the option, `saveState()` writes nothing, so a play test does not change the save.
- No change to gameplay, to the save format or to the difficulty screen. Tests added to `LaunchOptionsResolverTest`;
  not run (no Gradle during the play test).

## 2026-11-15 (iteration: head blinks at the defeat)

- Owner play test with `--difficulty hard`: plays well; hard is a bit tough, kept until player feedback.
- Owner: the head blinks at the defeat like the hands. The head sprite is hidden in every second 0.05 s step of
  `defeatTime` (`DEFEAT_BLINK`, the hand `BLINK_DELAY` value). No Gradle run.


## 2026-11-15 (final pass)

- Owner: play test done, the fight is approved ("ready to merge"). No gameplay change in this pass.
- **Merge of `master`** (18 commits, with `060c` Proto Man buster and sabre): merge commit `87bbd87d0`.
  - `AbstractBoss.kt`, `SoundAsset.kt`: merged by git with no conflict (`060c` adds `ProtoBuster` and
    `PlayerSabreSlash` rows to `BOSS_DMG_NEG` and `PROTO_SABRE_SOUND`; `077a` adds the phases and
    `SHOCK_WAVE_HIT_SOUND`).
  - `BigAssMaverickRobot.kt`: 2 conflicts, both simple. Imports: both sides kept. Damage map: `master` kept the old
    `StandardDamageNegotiator` override map with `ProtoBuster` 2 and sabre 2 / 3; `077a` rewrote the map. Kept the
    `077a` map and added the 2 `060c` classes: `ProtoBuster` 4, `PlayerSabreSlash` half 4 / full 6 (reason in
    `docs/NOTES.md`, "Proto Man rows on this boss"). The overlap `TODO` is removed.
  - `060c` also changed `StandardDamageNegotiator` and `BOSS_DMG_NEG`; no effect on this boss, which lists both
    classes in its own map.
- **Tests** (`docs/TEST-DEBT.md` is closed; the table there names the test and the mutation for each line):
  - Fixed: `ShockWaveRow.start` life in `BigAssMaverickRobotPartsTest` and `SlamAimTest`; the damage values in
    `BigAssMaverickRobotDamageTest`; the fight `hit()` (3 per pellet); the slam pillar counts (now set by the wave
    life); the last-blast count in the death test; `PlayerRulesTest` (the boss gives 1, not 2; not in the debt list).
  - New: wave life per tuning row and the path end; the tuning rows; clap sync and closing per frame; no clap in
    phase 1; 1-bit damage from every damager of the fight and 3 from an orb of `GutsTank` and `WilyFinalBoss`; the
    pillar hit sound and no electric pulse; the aimed orb with and without a supplier; the death face, blink and
    explosions; the intro fill order; `ProtoBuster` 4 and the sabre 4 / 6 (`ProtomanTest`).
  - Production change for a test, no change of behavior: `BossHealthHandler.phaseAtFillBit` and `countAtFillBit`
    (the 2 fill formulas moved from the timer lambda into the companion, so that `BossBitsBarTest` reads the fill
    order through the same code).
  - Draft tests now run and pass: the 3 clap fight tests (with `setSeed(3L)` a clap starts within 90 s),
    `ClapAimTest` (6), the 3 renamed `PINCER_CLAP` / `CLAP_*` cases, the `--difficulty` cases.
- **Mutation checks**: 5 runs (2 full `core:test`, 3 with `kotest_filter_specs` on the fight test), 25 mutations,
  each seen to fail its test. Each run restored the 10 changed production files from copies in
  `temp/077a-final-pass/backup/`, and `git diff` then showed only the `BossHealthHandler` change.
- **Edge found by a test** (no change): `ShockWaveRow.update` spawns before it checks the life, so a pillar that is
  due in the frame where the wave life ends still spawns. With the hard phase-2 life of 0.8 s and a 1/64 s step, a
  wave makes 11 pillars (the 11th at 0.8125 s), not 10. The test states this rule: a pillar each 0.08 s up to the
  wave life, at most one frame after it.
- **Automated checks**: `engine:test core:test core:smokeTest lwjgl3:jar` pass. `engine:test` 689 tests (83
  specs), `core:test` 2013 (174 specs), `core:smokeTest` 462 (4 specs); 0 failures, 0 skipped; the JAR builds.
- Not done in this pass: a new review subagent on the diff since step 11 (the review before the play test of
  `d5fdab1a9`); the contract box for it stays open.

## 2026-11-15 (review before the merge: findings and fixes)

- **Review**: 2 code-review subagents (the review before the play test, on the branch diff and on the contract's
  manual checks). Findings:
  1. `CONTRACT.md` is stale against the play-test rounds (values, manual checks, "Must not change", "Intended
     changes").
  2. D2 is replaced by the fill in passes, but `SCOPE.md`, `PLAN.md`, `BRIEF.src.html` and the NOTES "Tuning rows" and
     "Death" sections do not say so.
  3. The `WIDE` aim of the `HARD` row never fires: a 0.6 s gap in a 2 s volley gives 3 orbs.
  4. The cast of the orb's `ConstKeys.SUPPLIER` prop gives a bare `ClassCastException` for a wrong value.
  5. The `BossHealthHandler` test reads only the companion helpers, not the fill time through `set` and `update`.
  6. Import order in `PlayerDamageNegotiator.kt` and `BigAssMaverickRobot.kt`.
  7. `SHOCK_WAVE_HIT_SOUND` has `0.6f` as its second value, which is `seconds` (the play length that the audio
     manager allows), not a volume.
- **Owner decisions** (2026-11-15): hard phase 1 keeps 3-orb volleys; the half Proto sabre stays 4 (buster 4, full
  sabre 6); merge without a Proto Man play test.
- **Fixes**:
  1. `CONTRACT.md`: values from the play-test rounds; damage rows; manual checks 1, 2 and 4 (with the faster
     `--difficulty hard` command; the `SELECT_DIFFICULTY_SCREEN` staging starts at `room12`); "Must not change" keeps
     only the `GutsTank` and `WilyFinalBoss` orbs (3 to a player); "Intended changes" updated, with 4 new rows (damage
     to the player, intro fill in passes, shock wave sound, `--difficulty`). The box of the review before the play
     test is ticked.
  2. A note at D2 in `SCOPE.md`, `PLAN.md` and `BRIEF.src.html`; a "Superseded" note at the top of the NOTES "Tuning
     rows" and "Death" sections.
  3. `WIDE` removed from the `HARD` row (no gameplay change: it never fired). New test `Fight` "the orb volleys" (4
     cases: 2 / 3 / 3 / 4 orbs, each equal to the aims of the row), through the real `updateVolley` with a player on
     each hand, so that no hand attack or clap pauses the volley.
  4. New `InvalidOrbSupplierException` (`entities/projectiles/`), thrown at spawn with the `ClassCastException` as its
     cause; the message names the `(from: Vector2, out: Vector2) -> Vector2` type. No earlier pattern for typed
     function props exists in the repo. The JVM checks only the `Function2` type, so a 2-argument function with other
     argument types still passes the cast. New test `Fight` "throws InvalidOrbSupplierException ...".
  5. New `BossHealthHandlerTest` (2 cases, 1-phase and 2-phase boss): through `set` and `update`, `finished` is
     false just before `max x phases x DUR_PER_BIT` and true after it. The phase gate of the bar has no public state;
     it stays a known gap in `docs/TEST-DEBT.md`.
  6. Imports sorted: `PlayerDamageNegotiator.kt` (the `contracts`, `enemies` and `explosions` lines);
     `BigAssMaverickRobot.kt` (`players.PlayerSabreSlash` before `projectiles`, `projectiles.Axe` first in
     `projectiles`, `state.BossPhase` after `projectiles`).
  7. `SHOCK_WAVE_HIT_SOUND` `seconds` is `0.25f` (the file is 217 ms).
- **Correction** to the entry "2026-11-14 (iteration: shock wave sound)": "volume 0.6" is wrong. The value `0.6f` is
  `seconds`, the play length that the audio manager allows before it stops the sound; it is now `0.25f`.
- **Mutation checks** (3 `core:test` runs; each mutation made with an edit and reverted, then `git diff` showed only
  the fixes):
  - Run A: `bits = max` in `BossHealthHandler.set` failed the 2-phase case; the plain cast with no catch failed the
    exception test; `ORB_GAP_HARD` 0.5 failed the tuning-rows test, but not the volley test: the `HARD` row has 3
    aims, so a 0.5 s gap still fires 3 orbs.
  - Run B: `ORB_GAP` 2.5, `ORB_GAP_PHASE_2` 1.1, `ORB_GAP_HARD` 0.7 and `VOLLEY_DUR_HARD_PHASE_2` 1.5 failed each of
    the 4 volley cases (1, 2, 2 and 3 orbs).
  - Run C: `WIDE` back in the `HARD` row failed the hard phase-1 case ("each aim of the row fires once": 3, not 4).
- **Automated checks**: `engine:test core:test core:smokeTest lwjgl3:jar` pass. `engine:test` 689 tests (83 specs),
  `core:test` 2020 (175 specs), `core:smokeTest` 462 (4 specs); 0 failures, 0 skipped; the JAR builds.
