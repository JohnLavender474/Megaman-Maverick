# Process log: mega-refactor-079a-smarter-met

## Status

- State: merging
- Goal: a smarter `Met`: pit avoidance with a shared `GroundProbe`, safe jumps, charged-shot stagger, helmet break and a desperate phase; hard mode seeks and dodges
- Done:
- Brief: https://claude.ai/artifact/1pBakUvAMMyhv7CJvr4DXw
- Next: owner play-test round 1
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-18

- Branch start on `master`. Merge target: `master` (owner, 2026-11-18).
- Owner answers before the brief: a charged hit staggers a helmeted `Met` with no damage; Claude draws the art drafts; one world-probe helper (`GroundProbe`); jump rules as proposed (gaps, steps, higher platform toward the player); a hard-mode `Met` seeks the player more and dodges some shots.
- Scan by 3 read-only subagents (break effect, ground and jump helpers, charged shots and `Met` use). `SCOPE.md`,
  `CONTRACT.md`, brief; owner answers D1 (shot bounces as today), D2 (SnowMet palette copy), D3 (dodge 1 in 3).
  Scope and contract approved. `PLAN.md` written.
- Plan step 1 done by a subagent: `RefactorTest079.tmx` (9 rooms, 196x18 tiles; verify script 88 PASS), the
  `REFACTOR_TEST_079` entry and its `LevelCompletionScreensTest` line. Turned `Met`s use `GravityChange` areas; the
  freeze check uses `--weapons FRIGID_SHOT`. Scripts in `temp/mega-refactor-079a-smarter-met/`.
- Plan step 2 done by a subagent: art drafts in `temp/079a-met/` (sources in `SOURCES.md`); owner approved the
  contact sheet as drawn (2026-11-18).
- Plan step 7 (art part): the 20 new frames copied into `assets/sprites/frames/Enemies1/Met/` and `SnowMet/`,
  `Enemies1` repacked with `Enemies1.tps` (`Basic`). The compare script
  (`temp/mega-refactor-079a-smarter-met/compare_atlas.py`) shows 370 old regions with the same names and pixels, 20
  new regions, 0 problems. README credit added; sources in `docs/ART-SOURCES.md`. The code that uses the regions
  comes in steps 4-6.
- Plan step 3 done by a subagent: `GroundProbe` (`checkAhead` gives `FLOOR`, `SAFE_DROP`, `PIT` or `WALL`),
  `JumpPlanner` (samples the arc with the engine fixed step and the `Body.process` order: gravity, clamp, move) and
  `GravityFrame` (local and world frames for the 4 directions). 27 Kotest cases in `GroundProbeTest` and
  `JumpPlannerTest` pass. A batch of 4 mutations (inactive `DEATH`, `COLLIDE_UP_ONLY` ground, in-flight solid check,
  `RIGHT` velocity map) failed exactly the 4 matching cases; sources restored, run green again. One accepted edge
  case has a `TODO` at `GroundProbe.isGround` (`COLLIDE_UP_ONLY` is never ground, also for a turned entity).
- Plan steps 4-6: `Met.kt` rewritten on `EnumStateMachineBuilder` (7 states; allowed edges in `TRANSITIONS`, entered
  by request). Pit avoidance and jumps through `GroundProbe` and `JumpPlanner` (gap, step, player platform, hop);
  charged hits through `HIT_BY_PROJECTILE` on the shield and body fixtures with a 0.6 s cooldown (one shot, two
  contacts; the full-charge explosion lasts 0.6 s); helmet break with `FallingPiece`; desperate run and pause; hard
  mode: shorter shield, longer run, face the player, dodge 1 in 3 player shots. `run_only` keeps the old path.
  Claude's call (changes the D1 note): a charged shot on the exposed body of a helmeted `Met` bursts, because
  `canBeDamagedBy` is false for it; on the helmet it bounces as today.
- Checks: `core:compileKotlin`, `core:smokeTest`, `detektMain`, `core:verifyRandomRule`, `LevelCompletionScreensTest`
  pass. Test lines for the final pass in `docs/TEST-DEBT.md`. Next: owner play-test round 1.

## 2026-11-18, play-test round 1

- Owner (`pit_edge`): the `Met` stops half off the ledge, and turns away from Mega Man after it shoots. The owner
  proposed left and right scanner fixtures plus a catch rectangle sorted by distance. Owner chose the probe with that
  geometry (AskUserQuestion): `GroundProbe.checkAhead` now scans a thin strip just past the front edge (lead = run
  speed x frame time) and a body-wide catch column for the drop and `DEATH` check. The `Met` no longer turns at a pit
  or a wall; a desperate `Met` stands, faces the player and keeps shooting. No Gradle run (gameplay rounds);
  `GroundProbeTest` cases to rewrite are in `docs/TEST-DEBT.md`.
- Owner question: does `Run` show for a frame at the ledge after the shot? Yes: `POP_UP` chose `RUNNING` without the
  ground check, and the pit check ran one frame later. Fix: the end of `POP_UP` runs the same check (`checkAhead`,
  gap or step jump); a blocked way goes straight to `SHIELDING`.
- Owner (round 1): the static `HelmetUp` frames read as frozen. New design (owner): the helmet is its own entity that
  flies up and lands back on the head; the landing ends the stun; spinning stars; a push-down landing animation.
  Owner answers: the airborne helmet's shield deflects, a charged hit on it counts (3rd breaks it in the air); it
  damages the player; the `Met` listens to the helmet with a min and max stun time (derivation in `docs/NOTES.md`:
  0.89 s and 1.77 s). Scope change written in `SCOPE.md`, `CONTRACT.md`, the brief (marked, republished),
  `TEST-DEBT.md`.
- Code: new `entities/hazards/MetHelmet.kt` (`BODY`, `SHIELD`, `DAMAGER`, `ABSTRACT` body moved per frame in the
  `Met`'s gravity frame, homes on the head at 2 tiles/s, gravity 15 tiles/s²); `Met` states `STAGGERED` replaced by
  `STUNNED` and `HELMET_LAND`; `PlayerDamageNegotiator` entry `MetHelmet` 2; `entity-tags.txt` line.
- Art round 2 by a subagent (`temp/079a-met/round2/`, sources in `docs/ART-SOURCES-ROUND2.md`); owner approved the
  eyes-visible landing end. Packed `Helmet`, `Stunned` (4), `HelmetLand` (3) for `Met` and `SnowMet`; removed
  `HelmetUpSmall` and `HelmetUpLarge`. Compare script: 386 old regions unchanged, 4 removed on purpose, 6 new. README
  credit line updated. Checks: `core:compileKotlin`, `core:smokeTest`, `detektMain`, `core:verifyRandomRule` pass.
- Not committed: the Diffity review is open.
- Owner rule change (round 1): on an exposed `Met`, a charged shot does its normal damage; the `Met` dies when the
  damage reaches its health, else it is also stunned and the hit counts toward the 3. Closed-helmet hits stay
  stun-only. Consequence (owner question): `chargedHitCooldown` and the `ChargedShotExplosion` and `ChargedShot` cases
  in `canBeDamagedBy` only served the old no-damage rule (the explosion bypassed it; one shot hit `SHIELD` and `BODY`
  receivers in one step; a no-damage hit starts no damage timer). With damage, `takeDamageFrom` counts once per real
  damage and the 0.1 s damage timer blocks repeats; the `BODY` receiver goes. One case is left: the shot that bounces
  off the closed helmet can hit the just-launched `MetHelmet`; the helmet ignores its launching shot (cleared when
  that shot is no longer live, for pooled reuse).
- Applied the damage rule (owner): `takeDamageFrom` counts a charged hit once per real damage; `chargedHitCooldown`,
  the `ChargedShotExplosion` and `ChargedShot` cases and the `BODY` receiver are gone; `MetHelmet` ignores the shot
  that launched it (`ConstKeys.SOURCE`).
- Owner (round 1): `MetHelmet` is a `DYNAMIC` body with `FEET` and `HEAD` (head on a block: bounced down at 3
  tiles/s; feet on a block: rests); no damage to the player (the `PlayerDamageNegotiator` entry is removed); a dying
  `Met` releases its helmet (falls free, rests, culled off camera). Backlog: Mega Man picks up and wears the helmet.
- Owner: the local-velocity read is a shared helper: `GravityFrame.velocityToLocal` (callers: `MetHelmet` twice,
  `Met.isShotComing`). `core:compileKotlin`, `core:smokeTest` pass. Uncommitted (Diffity open).
- Owner: a head bonk only cancels the helmet's upward speed; gravity takes over (the collision handler does not zero
  velocity, so the bonk handler does). `BONK_SPEED` removed.
- Owner (round 1): a helmet that misses the head no longer snaps back; the `Met` fetches it (`FETCH_HELMET`, worried
  run, no shots, jumps toward it), picks it up (`PICKUP_HELMET`), or cries (`CRY`) and goes desperate when it cannot
  (blocked way, helmet culled, 4 s timeout). Helmets change ownership: `MetHelmet` holds the hit count (`hits`,
  breaks itself on the 3rd charged hit), `claim`, `release`, `consume`, and tells its owner `onHelmetBroken` /
  `onHelmetLost` (culled). A desperate `Met` claims a free helmet it touches. Jump targets are generic
  (`tryJumpUpTo`, `planJump`, `isAhead` take the target bounds: the player or the helmet). Claude's call: a charged
  shot on a stunned (helmetless) `Met` only damages it. Art round 3 (worried run, pickup, cry) drawn by a subagent.
- Art round 3 approved by the owner with 2 side fixes (the art faces right): `WorriedRun`, `Pickup`, `Cry` for `Met`
  and `SnowMet` packed (new color `#3CBCFC`); compare script: 392 old regions unchanged, 6 new. README credit line
  updated; sources in `docs/ART-SOURCES-ROUND3.md`. `core:smokeTest`, `detektMain`, `core:verifyRandomRule` pass.
- Owner (round 1): a half charge on the closed helmet killed the `Met`: the stun exposed the body in the next step
  while the bounced shot still overlapped it (15 + explosion 10 per 0.1 s). Fix: a stunned `Met` stays shielded
  while the shot that stunned it is live and touches it (`stunShotTouchesMe`); the shot then only bounces.
- Owner: `charged_stagger` must let the `Met`s respawn without a restart. A subagent widened it to 40 tiles with
  `build_test079.py` (`Met`s 26 and 30 tiles from the left wall; later rooms shifted by 20 tiles; verify 92 PASS).
  The rule went to `docs/ways-of-working/levels.md` on `master` (owner guidance).
- Owner (round 1): the helmet spins in 45-degree steps in the air (one step per 0.05 s, the turn follows the
  facing); no push back on a charged hit (knockback removed); a free resting helmet draws at `PLAYGROUND -2` (behind
  Mega Man at 0 and the `Met`s at 4), upside down, with a slow brightness pulse (tint 0.6 to 1.0 over 1.2 s: the
  game has no item glow to copy, and a tint can only darken).
- Owner: weaker homing plus a drift in the shot direction (full charge lands the helmet about 2.6 tiles away; half
  charge back on the head; derivation in `docs/NOTES.md`). Test entities and rooms for the hard checks by a subagent.
- Test-only entities (rule "Test-only entities"): `TestChargedShooter` (fires real `ChargedShot`s owned by the target player, so they stun a `Met` like a player shot and never hurt Mega Man) and `TestHelmetMover` (moves the flying `MetHelmet` of its `target` `Met` to its rectangle once per launch), both in `entities/special/`, used only by `RefactorTest079` rooms `helmet_pit`, `helmet_platform`, `helmet_swap`.
- Owner (round 1): a helmetless `Met` never shoots (after a break or a lost helmet); it only runs with short pauses.
- Owner (round 1): a free helmet hovered: the upside-down flip moves the 11 helmet rows of the 16-row region to the top; the idle sprite now moves down by 5/16 of its height in the gravity frame (draw offset from game state). Older `Met` rooms widened for respawn by the room subagent.
- Owner: shots pass through an ownerless helmet on the ground (its `SHIELD` and `BODY` fixtures are off while idle); a helmet in the air keeps its shield.
- Owner: the helmet lands farther (drift 3 / 4.5 tiles/s, homing 1 tile/s²: about 2.3 / 4.9 tiles). New shared
  `entities/utils/HalfDomeShield` (callers: `Met` closed helmet and `MetHelmet`): a `SHIELD` circle of radius 0.6
  tile at the body bottom-center (the helmet pixels are 18 x 11 px = 1.13 x 0.69 tile), labelled
  `NO_PROJECTILE_COLLISION` with an exception that lets only shots whose top reaches the dome center collide.
  A read-only subagent investigates the inconsistent charged-shot result.
- Investigation (read-only subagent): the closed-helmet shot bounced off the `Met`, then off the `MetHelmet` it had
  just launched, back into the `Met` one step after it left it (`shielded` off) -> damage, bounce 3 -> explosion; the
  outcome depended on the shot's depth in its first steps. Fix (owner yes): `HalfDomeShield` takes a `passes`
  predicate; the helmet lets its launching shot through.

## 2026-11-18, final pass

- Owner: play checks pass, merge. Final pass before the merge (tests from `TEST-DEBT.md`, full checks, review).
- Review by 3 subagents: code diff (11 findings), contract play checks (1 map problem), test writer (stopped on 1
  production bug: the helmet drift read the shot velocity after its bounce, so it drifted toward the shooter).
- Fixed: drift side from positions; `run_only` keeps its speed off a ledge (no `stopRunning` in `SHIELDING` for it);
  `HalfDomeShield` attachment follows the gravity direction; `PICKUP_HELMET` edges to `STUNNED` and `DESPERATE_RUN`;
  `requestState` throws on a missing edge and passes a reused params array; desperate pit and wall plans only on the
  jump-check timer; a consumed helmet cannot be claimed; `stunShot` cleared in the update (pure getter);
  `jumpCheckTimer.resetDuration`; removed `MetHelmet.seat()` and `JumpPlan.landing` (no production caller); private
  helmet constants and `HalfDomeShield.fixture`; named `loop` args; `turned_met` ceiling zone 1 tile deeper.
- Owner answers: keep the 0.8 s hard-mode shield (now in the contract); a `running_allowed=false` `Met` still runs to
  fetch and when helmetless. Dismissed: `O2-R4` dodge order (`getOfType` is an `OrderedSet`, stable order).
- Accepted edge case with a `TODO`: the helmet keeps its launch gravity (`MetHelmet.onSpawn`).
- Final tests by the subagent (commit `2af3322e6`): `MetChargedHitTest` 26, `MetHelmetTest` 20, `MetMovementTest` 12,
  `HalfDomeShieldTest` 15, `GravityFrameTest` 13, `GroundProbeTest` 23, `JumpPlannerTest` 8, on a real-engine rig
  (`MetRig`). Not testable through public state: `requestState` throwing on a missing edge (private table). Mutation
  runs: 7 mutations, each failed its target cases (`temp/079a-tests/mutation-run.txt`), all restored. Full check
  `engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar`: BUILD SUCCESSFUL. Two 1-frame
  visual lags logged as bug candidates. Owner: play re-run passed; merge when the tests pass.
