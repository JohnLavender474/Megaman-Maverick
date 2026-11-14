# Process log: mega-refactor-060c-protoman-buster

## Status

- State: active
- Goal: Proto Man plays like X6 Zero with a Mega Man buster: sabre and buster weapon slots, a passive shield
- Done: planning; steps 1 to 40; play test 7 passed; Diffity rounds 1-4 closed
- Brief: https://claude.ai/artifact/PbvwXioKroFpU9hhwxVBQY
- Next: merge into `master`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-11

- Branch start, after `060b` merged. Merge target: `master` (item decision).
- Planning scan (2 read-only agents; reports in `temp/mega-refactor-060c-protoman-buster/`: `scan-code.md`,
  `scan-sprites.md`, `contact_sheet_x4.png`, `preview_shoot_draft_x4.png`). The code agent's Fandom fetches failed;
  the main session read `Proto_Man` and `Proto_Buster` through the MediaWiki API (`temp/.../web/`): MM9/MM10 cap 2,
  charged shot like MM4.
- `SCOPE.md`, `CONTRACT.md` (draft) and `BRIEF.src.html`; brief rendered and published.
- Owner answers (none as recommended; "a first try from the gut, plan to refine or pivot"): D1 release, no cap; D2 full
  only; D3 new classes `ProtoBuster` and `ProtoStrike`, D3a explicit entries, D3b the Strike always explodes; D4
  recoloured charge frames. `SCOPE.md` and `CONTRACT.md` approved; brief version 2 published to the same link.
- `PLAN.md`: 9 steps. The rule values stay in `ProtomanValues`, and the class entries are a step of their own, so a
  pivot stays cheap.
- Steps 2 to 8 done (commits `5c8d7578c`, `4b9073b58`, `888ec95c5`, `22cd6ac02`); details in `docs/NOTES.md`. Art: 2
  draft rounds plus the jump scarf, owner choices (gold charge, v3 cannon, cleanup round, scarf).
- With the 2 key tests, `core:test` ran out of its 1 GB heap (4 of 4 runs). While I looked into it, I found 8 stuck
  test JVMs of other sessions (44 h to 9 days old, about 7 GB); the owner had them stopped. A `pkill -f` of mine may
  have stopped one of them (PID 3267829) before that. Two fixes went to `master` first, each in its own branch:
  `fix/test-jvm-oom-hang` (a test JVM exits on OOM; 20-minute test timeout) and `fix/core-test-heap-leak` (clear the
  recorded calls of every mock after each spec; MockK 1.14.11 never frees a mock). A user hook stops test JVMs older
  than 60 minutes. Issues: `docs/issues/2026-11-11-test-jvm-hangs-on-oom.md`, `2026-11-11-core-test-heap-leak.md`.
- `master` taken in (`cb2539260`, `a752e0dc7`). `engine:test` 675, `core:test` 1380, `core:smokeTest` 454, `lwjgl3:jar`
  pass.
- Review before the play test (2 read-only agents; reports in `temp/mega-refactor-060c-protoman-buster/`:
  `review-diff.md`, `review-checks.md`). X and Megaman play the same (line-by-line comparison with `master`); every
  manual check is valid, the levels load, nothing throws at start.
  - Fixed: `TimberWoman` and `SpikeTeeth` name the player shots by tag, which the step-6 scan missed: the new tags are
    added (`class-entries.md`). A shot during the step held the step for the 0.3 s shoot pose: the step end now comes
    from the playing animation (new test; break check: the 2 step tests fail with the old rule). The Strike showed
    upside down to the left: mirrored on X. `PlayerBuster.shotOf` is private.
  - Contract text corrected: 1 Strike kills `PicketJoe` (its explosion adds 10 and 10, as Megaman's full shot), pellets
    hit him only while he throws, the step blocks shots from the spawn, `SniperJoe` spawns when the view reaches him.
  - Open for the owner: a Strike on a shield explodes, and the explosion can hurt the enemy behind it; at a full charge
    the step frame is skipped.
  - Not done: a test that the Strike explodes on a kill, and a test that lists each class-keyed file (the play test
    covers the first; the review covered the second by hand).
  - Bug candidate: `ProtomanTest` and `MegamanXTest` fail alone (also on `master`): `docs/TODO.md`.
  - `core:test` 1381, `core:smokeTest` 454, `lwjgl3:jar` pass.

### 2026-11-12: play test round 1, charge cue

- Owner: a 0.5 s full charge, and no feedback before the full charge. Decided: full-charge only; `TIME_TO_HALF_CHARGE`
  becomes `CHARGE_CUE_TIME` (0.15 s), when the charge sound starts (`docs/NOTES.md`, Decisions; `docs/SCOPE.md`).
- Changed: `ProtomanValues`, `Protoman.kt` (the charge component), `ProtomanTest` (the renamed constant, and a new test
  that the charge sound starts at the cue time). Uncommitted while the Diffity round is open.
- Owner: the charge sound means "a charged shot is ready", so a sound at 0.15 s before a pellet confuses. Changed:
  `playerChargeComponent(player, halfTime: Float?, fullTime)`; Proto Man passes `halfTime = null`, so the sound starts
  at the full time. `CHARGE_CUE_TIME` removed. Tests: the pellet and sound tests check just before and at the full
  time. `players/README.md` updated.
- Owner idea: a code style doc that the repo `CLAUDE.md` points to (answered in the session).

### 2026-11-12: scope change (owner): Proto Man fights like X6 Zero

- At the play test the Strike was too strong. The owner changed the item: Proto Man plays like X6 Zero; `060f` folds
  into this branch (master `43ad12a31`). Owner answers: `B` sabre (3-slash floor combo, 1 slash elsewhere, purple),
  `UP`+`B` standing buster shot on the ground only, no charge, no Strike.
- Checkpoint commit `c02f2e4f0` (round 1 charge change, not built). Diffity round 1 closed with 0 threads; server
  stopped.
- Scan: classic enemies take no sabre damage today (no slash entry in `StandardDamageNegotiator`).
- `SCOPE.md` "Scope change", new `CONTRACT.md`, brief updated (same link, version 3), decisions B1 to B4 all as
  recommended; `PLAN.md` steps 10 to 19.

### 2026-11-12: steps 10 to 14

- Steps 10-11 (`e908fe62d`): the charge and the Strike removed; the standing shot on `UP`+`B`. Break check: with
  the ground rule, the run lock and the jump end removed, the 3 matching tests fail.
- Step 12 (`56fa9995a`): `PlayerSabre` and `PlayerSabreSlash` shared; X's 43 sabre tests pass with renamed types
  only. Owner change: one move list per state (LibGDX `Array`), and the style doc on `master` (`a6970cd59`).
- Step 14 (`60791ba70`): half-charge entries for `ProtoBuster` and the slash in 11 tables (`docs/class-entries.md`).
- Step 15 (art): owner answers: the pose map (cells 187-191, 172-176, new from 184-186, 197-201), Tengu Blade arcs in
  purple, and the v2 restyle first. Round 1 drafts by a background agent in
  `temp/mega-refactor-060c-protoman-buster/sabre/v2/`.

### 2026-11-12: steps 15 to 19 and the review before the play test

- Art: 5 rounds with the owner (v2 bodies by hand, the arms shortened, own stances, arcs over the body at X's scale,
  placed on him, pointed crescents). Packed into `Protoman_v2` (`edfbc0f72`); `ProtomanSabreMoves.kt` is generated
  from the art's `arcs.json` by `temp/mega-refactor-060c-protoman-buster/scripts/gen_sabre.py`.
- Steps 13 and 16 (`edfbc0f72`): Proto Man's sabre and combo tests; break check: the chain and the air state each
  fail their tests. Step 17 (`189a08393`): the `sabre` room. Step 18 (`decc33ecb`): `players/README.md`.
- Automated checks: `engine:test` 675, `core:test` 1385, `core:smokeTest` 453, `lwjgl3:jar` built.
- Review before the play test, 2 agents:
  - Diff: no crash, no unplanned change for X or Megaman. Fixed: tests for a gate opening (ends the slash and the
    shot pose; break check fails it) and the `HoverGunner` slash and `ProtoBuster` damage; a test that a state with
    no move list throws `SabreStateMissingException`; named `loop = false` in the new calls; the arc follows Proto
    Man's air sprite drop (`SabreParams.spriteOffsetY`, X passes 0). For the play test: he can jump at any time
    during a floor slash (X has a jump-cancel time).
  - Manual checks: all OK except the `sabre` check, which asked to walk off the ledge during the combo; he cannot
    move during a floor slash, so the check now uses a jump. Room transition check moved to the room `transition`.

### 2026-11-13: play test round 2, Diffity round 2 closed, scope change 2

- Play tests 1 to 4 and the sabre art rounds 6 to 24 (art in `temp/mega-refactor-060c-protoman-buster/sabre/v2/`,
  notes in its `notes.md`).
  - Approved by the owner: slash 1 round 10 and round 14 height, slash 2 round 21 (rounds 22 and 23 rejected, round
    24 = round 21), slash 3 round 21 legs and round 20 free arm, the air-land move (option A, Zero's landing).
  - Damage changed to full charge at play test 1, then to half and full per hit by scope change 2.
- Diffity round 2: 0 threads; the owner closed the round. The fixes are committed with this entry. `core:test` (1844
  tests), `core:smokeTest` and `lwjgl3:jar` pass.
- A neutral review of the moveset (a read-only subagent) led to scope change 2 (see `docs/SCOPE.md`). The brief was
  updated and published again to the same link. New item `153` on `master` (`e13419951`).
- Plan steps 20 to 26 written in `docs/PLAN.md`. The reason: scope change 2.

### 2026-11-13: plan steps 20 to 25

- Step 20 (`e5623b158`): `SabreMove.fullStrength` (generated by `gen_sabre.py`: only `SLASH_3` is full; X's 3 moves
  are full). `PlayerSabreSlash.fullStrength` is read by the 4 tables where half and full differ
  (`StandardDamageNegotiator`, `AbstractBoss`, `BigAssMaverickRobot`, `WoodCrate`); the other slash entries and the
  X6 enemies keep one value.
- Step 21 (`a931edb82`): `ProtomanWeapon` (`SABRE`, `BUSTER`), `SELECT` on release with `NEXT_WEAPON`, the ping,
  `SABRE` at spawn.
- Step 22 (`9f4ad413a`): the buster slot shoots in every state with no lock; `UP`+`B` slashes once
  (`PlayerSabre.start(chains = false)`); run and air offsets 0.5 and 0.1 (X's); `run_shoot`, `run_trans_shoot` and
  `jump_shoot` restored from `ebcffebb6` and packed; `TODO` for the ladder rule (`060d`).
- Step 23 (`319cde2e9`): the shield (fixture, guard timer 0.2 s, guard poses, timed `WhiteBurst`). Headless tests
  dispatch a real `Bullet` through `ProjectileRule` against the shield (it bounces, `DINK_SOUND`, one burst, no
  damage) and through `DamageRule` from behind (he takes damage). Not feasible headless: a full `WorldSystem` step
  with Proto Man spawned, so the order of the shield and body contacts in one step is not tested (see the `TODO` at
  the shield).
- Step 24 (`d4ec3e842`): the `Hud` atlas (owner choice over `Bits`), the icon in `PlayerStatsHandler`, and
  `HudIconsSmokeTest` (the icon regions of each slot). The handler needs the game object, so no unit test draws it.
- Step 25: docs only; the `buster` room already has a `SniperJoe`.
- Break checks (each test seen to fail with its rule removed, then the rule restored):
  - Step 20: without the strength read in `PlayerSabreSlash`, the 2 Proto Man damage tests fail (hit 1 and 2, air).
  - Step 21: without the `NEXT_WEAPON` check, the damaged gate, the ping and the spawn reset, their 4 tests fail.
  - Step 22: with the run lock back, a ground-only shot, the old `stand_shoot` key and no `chains` flag, 5 tests fail.
  - Step 23: with the guard reset only out of play, the fixture always on, no facing sign, no owner check and no
    guard keys, 8 tests fail; with no `playerCanAttack` gate on the shot, "fires no shot while damaged" fails.
  - Step 24: with a wrong region name, `HudIconsSmokeTest` fails.

### 2026-11-13: review before the play test of steps 20 to 25, and the fixes

- Automated checks first (`core:test` 1413, `core:smokeTest` 454, `lwjgl3:jar`), then 2 review agents: the diff
  and the manual checks.
- Findings and fixes (`58de0953f` for the code, the next commit for the map and the docs):
  - M1: the deflect spark had an active `EXPLOSION` fixture, so some projectiles exploded on it and a `SmallIceCube`
    shattered. Fix: `WhiteBurst` takes `ConstKeys.COLLIDE`; the spark passes `false`.
  - M2: the spark showed for every projectile on the shield, also with no `hitShield` reaction. Fix:
    `ShieldDeflects` checks after the contact pass that the projectile turned back or died.
  - L1: the guard stayed on while frozen or teleporting, and came back 0.2 s into a room pan. Fix: these break it.
  - L2 (owner): `SELECT` does nothing during a slash.
  - L3: the shoot and guard keys were built as strings on each read. Fix: constant `ObjectMap`s.
  - Owner: half values for the 4 X6 sabre targets.
  - Style: `WhiteBurst` keeps one `Timer`; `guarding` is `internal`; imports sorted; `Array` imported in
    `ProtomanTest`. `PlayerSabreSlash.fullStrength` keeps its default (see `docs/NOTES.md`).
  - Test gaps: `WhiteBurstTest` (the life timer, the fixture on and off); the shield for gravity `LEFT`, which found
    that the shield was turned twice (fixed); a break check for the bounce.
  - Map: the picks have no shield reaction, the `buster` room's `SniperJoe` shoots into the `PicketJoe`'s shield,
    and "hit 3 hurts more" could not be seen. Fix: the new room `shield` and a `Robbit` in the `sabre` room;
    `CONTRACT.md` checks updated.
- Break checks (each failed with its rule removed, then restored):
  - the `COLLIDE` prop removed from the spark: "shows the spark with no active explosion fixture" fails;
  - the deflect check always true: the trajectory and the `Picket` spark tests fail;
  - one check in place of 2: the trajectory spark test fails;
  - `hitShield` removed from `ProjectileRule`: "bounces a Bullet from the front" fails (and `FriendlyFireTest`);
  - frozen, teleporting and room transition removed from the guard break: their test fails;
  - the `sabre.active` gate removed from `SELECT`: "does not switch during a slash" fails;
  - the full value used for the half hit in the 4 X6 tables: their 4 tests fail;
  - no destroy at the end of the `WhiteBurst` time: "destroys itself after its duration" fails;
  - the old double turn of the shield: the gravity `LEFT` test failed before the fix.

### 2026-11-13: re-review of the review fixes

- One read-only agent re-reviewed `a1ee0f5ce..c39525b90`: no blocking issue.
  - Low 1: the `shield` check said that the guard jump pose blocks; in a jump a shot can pass under the shield and hit
    his legs. Fixed: the contract line names it.
  - Low 2: a projectile that dies in the same frame for another reason can show a spark. Accepted: added to the
    existing `060e` `TODO` at the shield fixture.
- Comment and docs only, so the checks of `c39525b90` stand (1426 core tests, 454 smoke tests, the JAR).
- Next: Diffity round 3 and the play test.

### 2026-11-13: play test 5, Diffity round 3 closed, scope change 3

- Diffity round 3: 0 threads; closed by the owner.
- Owner at play test 5:
  - the HUD icon goes under the health bar;
  - the shield covers the full body, with the arm drawn correctly;
  - pull-out frames before the guard;
  - the guard becomes a held `DOWN`.
- Owner answers: hold `DOWN`; he stands still while guarding; no passive guard in the air; `DOWN`+`A` does nothing
  until the `060d` slide.
- `SCOPE.md` "Scope change 3", the contract status and the brief were updated, and the brief was published again to
  the same link.
- Plan steps 27 to 31 were added. The reason: scope change 3.

### 2026-11-13: plan steps 28 to 31 (scope change 3)

- Step 28 (`e9d5e2728`): the guard is held on `DOWN`, with pull-out frames first; it plants him on the ground and
  keeps his arc in the air; no attack while guarding; `DOWN` + `A` on the ground does nothing (`TODO` `060d`); a
  `TODO` for no guard on a ladder (`060d`). The passive timer is gone. `DOWN` does not drop Proto Man through a
  one-way platform, so a guard there works (see `docs/NOTES.md`).
- Step 29 (`0ae9cb3bc`): round 27 art (not approved yet) packed; the full-body `SHIELD` fixture from the art boxes;
  placement tests for each gravity direction and the jump box.
- Step 30 (`409427ff3`): the HUD icon under the health bar.
- Step 31: `CONTRACT.md` (tests and the `shield`, `buster` and `walk` checks), `NOTES.md`, `players/README.md`.
- Break checks (each failed with its rule removed, then restored):
  - the guard without `DOWN` (passive): "is off at the spawn and with no DOWN" fails, and "runs and jumps" fails;
  - no plant on the ground: "stops a run on the ground" fails;
  - no `B` gate: "does not attack on B while DOWN is held" fails;
  - `downBlocksGroundJump` back to `false`: "does not jump on A while DOWN is held on the ground" fails;
  - no pull-out wait: "shows the pull-out frames first" fails;
  - no turn while planted: "stops a run on the ground, and left and right only turn him" fails;
  - `guarding` without the live `DOWN` read: "drops at once when DOWN is released" fails;
  - no stop at the guard start: "stops a run on the ground" fails;
  - no facing sign on the shield offset: "puts the shield on his facing side" fails;
  - no air sprite drop in the shield offset: "is the round 27 jump box in the air" fails.

- Round 28 guard art (the cleanup of round 27, owner request) installed and packed. Shield boxes: stand x 30-36,
  y 25-47 (14 x 46 world px, 19 px in front of the body center); jump x 32-38, y 21-47 (14 x 54 world px, 23 px in
  front, 5 px below the center). The placement tests follow the new boxes.

### 2026-11-13: review before the play test of `6ab9e36f7..9bc5a9712`, and the fixes

- 1 review agent: 3 low findings and 1 doc gap.
  - The guard pose showed 1 frame before a world step ran with the shield on (the system order is World, Updatables,
    Animations). Fix: the pose and the fixture read one stored state (`guardUp`, then `shieldUp` in the pre-step).
    Test: frames in the real order (pre-step, updatables, animation key); the guard pose shows only with the shield
    on. Break check: with `guarding` read from `guardUp`, the test fails.
  - A landing during the air pull-out restarted the animation. Fix: the frame time carries between
    `guard_jump_start` and `guard_stand_start`, and between `guard_jump` and `guard_stand`. Break check: with no carry
    for guard keys, "keeps the frame time between the air and ground guard keys" fails.
  - The shield top is 2 px under the damage-box top: accepted, it follows the art box (`docs/NOTES.md`).
  - Doc gap: `SpringBouncer` and `SpiderWeb` also read `DOWN`; added to `docs/NOTES.md`.


### 2026-11-14: plan steps 32 to 36 (scope change 4, round 34 art; not committed, Diffity round 4 is open)

- Step 32: the round 34 guard strips (6) and back-shield strips (13) copied into `Protoman_v2/`; `guard_stand_start`
  and `guard_stand` deleted; the atlas packed with `pack_atlas.py` (24 regions, 214 x 1987). New smoke test
  `ProtomanGuardStripsSmokeTest`: each guard strip has the frame count of the code.
- Steps 33 to 35: the crouch guard (`BODY` and `DAMAGEABLE` 19 art px high while he guards on the ground; the
  physics body keeps its size), the 7x18 shield boxes, the 4 and 3 frame pull-outs, the put-away with its cuts and
  breaks. Decisions in `docs/NOTES.md` (scope change 4).
- `ProtomanTest` changes: 2 existing tests needed more steps for the longer pull-out ("stops a run on the ground",
  "is off during a shot", which now steps through the whole shot).
- Break checks (each run with the rules removed, then restored; `ProtomanTest` has 106 tests):
  - run 1, no crouch box, 2-frame pull-outs, no break cut of the put-away, a put-away after a release in the
    pull-out: 14 failed (the 2 pull-out tests, "plays no put-away for a release in the pull-out", the 4 break tests
    of the put-away, the crouch test, the head-height shot, the 4 feet tests, "covers his crouched body height");
  - run 2, no input cuts, and a crouch box during the put-away: 4 failed (the cuts on `A`, `LEFT`, `RIGHT`, "stands up
    at once on the release");
  - run 3, no put-away: 4 failed (the ground and air put-away tests, the held-`A` test, "stands up at once").
  - Not seen to fail: the cut on `B` (the slash it starts also breaks the guard, so the break path cuts it too) and
    "ends the put-away on a hit" (the hit drops the guard through `endAttacks`).

### 2026-11-14: review of scope change 4 (not committed), the fixes, and the `shield` room

- 1 review agent: 11 findings, all low. All fixed (not committed; Diffity round 4 is open):
  - The crouch box covered art rows 29..47 and the shield 30..47, so a 2 world px band at the top was open from the
    front. `GUARD_CROUCH_ART_ROWS` is 18 (1.125 tiles), and the test compares the shield top edge with the
    damage-box top edge, with no slack.
  - The last put-away frame showed one step less: the timer updated and was checked in the same pass. The check now
    runs before the update; the tests assert 4 frames on the ground and 3 in the air.
  - The put-away started from `guardUp`, also on the step after the pull-out timer ended and before the guard pose
    showed. `shieldUp` cannot be the trigger: the body pre-step runs before the guard update and clears it on the
    release step. So a new private `guardShown` keeps `shieldUp` of the last guard step (the step the guard pose
    showed). New test: "plays no put-away for a release on the step after the pull-out ends".
  - `crouched` is `private` (no test reads it); `turn()` passes `loop = false`; the frame-time self-assert is gone.
  - Docs: players `README.md` (the crouch box pre-step runs after the `playerBody` `DEFAULT` pre-step); `NOTES.md`
    (the crouch `BODY` box also changes the other `BODY` contact rules, accepted; the put-away trigger; `crouched`);
    `SCOPE.md` (pull-out 4 and 3 frames, art round 34); `CONTRACT.md` (`shield` check).
- Map: in `RefactorTest060.tmx`, room `shield`, a second `SniperJoe` (id 62) at col 217 on a new 2x0.75 tile ledge
  (block id 61, cols 217-218, top 0.75 tile over the floor; tiles painted on row 4). The spawn `7` and the first
  `SniperJoe` moved 5 tiles right (cols 222 and 229), so the first one is still 7 tiles right of the spawn. Bullet
  check: the left bullet centre is 1.4 tiles over his floor (0.75 + 0.65), bullet 1.225..1.575: over the crouch top
  (1.125) and into the stand body (top 1.5). A 1 tile ledge would overlap the stand body by only 0.025 tile.
- Break check (the 3 rules removed in one run, then restored): 9 failed (the 2 put-away count tests, the new
  put-away trigger test, the crouch test, the 4 feet tests, the shield top test).

### 2026-11-14: plan steps 37 to 40 (scope change 5, round 35 art; not committed, Diffity round 4 is open)

- Step 37: round 35 copied into `Protoman_v2/` (changed: `stand`, `run_trans`, `run`, `jump`,
  `guard_crouch_start`, `guard_crouch_end`, `guard_jump_start`, `guard_jump_end`; new: `crouch_shoot`,
  `crouch_slash`, `crouch_slash_arc`; the other strips were byte-equal). Atlas packed with `pack_atlas.py`
  (27 regions, 212 x 2199). `gen_sabre.py` also reads `guard/round35/arcs_crouch.json` and writes `CROUCH`;
  the 5 existing moves of `ProtomanSabreMoves.kt` are byte-identical (diff: only the new move and `ALL`).
- Step 38: crouch attacks. Shared: `SabreState.CROUCH`, `PlayerSabre.start(state)`, the floor stop for `CROUCH`;
  `BusterOffset`, `BusterParams.crouchOffset` (X: `null`), `PlayerBuster.shoot(status, crouched)` and
  `BusterCrouchOffsetMissingException`. Proto Man: the `B` branch for the crouch, `crouchShot`, the crouch box
  during the attack, the `crouch_shoot` key, the turn during the crouch shot. Decisions in `docs/NOTES.md`.
- `ProtomanTest`: 15 new tests (the crouch attacks); "does not attack on B while DOWN is held" now covers the air
  only. `ProtomanGuardStripsSmokeTest`: 1 new test for the 3 crouch attack strips.
- Break checks (each run with the rules removed, then restored; files compared byte-equal with the copies after):
  - run A, no crouch box during the attack, no `crouch_shoot` key, no turn in the crouch shot: 6 failed;
  - run B, `chooseState` does not keep `CROUCH`, the crouch shot from the stand offset: 7 failed;
  - run C, no hold-still in the crouch attack and no floor stop for `CROUCH`: 2 failed;
  - run D, no crouch branch on `B`: 12 failed.
  - Not seen to fail alone: the clear of `crouchShot` when the pose ends or he leaves the ground, and the floor
    stop for `CROUCH` without the hold-still rule.
- Step 39: room `shield`: spawn `7` col 222 -> 227, `SniperJoe` 58 col 229 -> 237 (`level-builder`
  `translate-x`); the 2 ledge cells painted with `Tileset1` local id 10 (24 px of art, the block height). The level
  analyzer reads the room with its spawn and 2 `SniperJoe`s. Layout and height arithmetic in `docs/NOTES.md`.
- Step 40: `CONTRACT.md` (scope change 5, the automated checks, the `shield` and `walk` checks), `SCOPE.md`,
  `PLAN.md`, `NOTES.md`, `BRIEF.src.html` (text only; not rendered or published), `players/README.md`.
- Automated checks (Corretto 17, `--no-daemon`): `engine:test` 675, `core:test` 1473 (`ProtomanTest` 121),
  `core:smokeTest` 462, all passed; `lwjgl3:jar` built.

### 2026-11-14: review of scope change 5 (not committed), and the test fixes

- Review: 1 agent, 4 low findings and 1 owner question. Test-only fixes: `ProtomanTest` 9 new tests
  (`BusterCrouchOffsetMissingException` on a crouched shot with no crouch offset; a jump ends the crouch slash;
  `DOWN`+`B` in a standing shot pose fires a crouch shot; `UP`+`DOWN`+`B` is a crouch attack, both slots; a hit and
  a room transition end each crouch attack and put the stand boxes back). `ProtomanGuardStripsSmokeTest` reads the
  crouch counts from `ProtomanSabreMoves.CROUCH`. Each new test failed with its rule removed, except the sabre hit and
  room transition tests: `PlayerSabre.update` also ends the slash when `playerCanAttack` is false (they failed with
  that end removed too). Main code restored byte-equal. Corretto 17, `--no-daemon`: `engine:test` 675, `core:test`
  1482 (`ProtomanTest` 130), `core:smokeTest` 462, all passed; `lwjgl3:jar` built.

### 2026-11-14: owner changes at play test 7, play test passed, Diffity round 4 closed

- Correction to the entry above: `ProtomanGuardStripsSmokeTest` cannot read `ProtomanSabreMoves` (it is `internal`
  and the smokeTest source set does not see it; the earlier run was up to date). It checks the crouch strips with
  literals (`crouch_slash` 4 frames, arc 4 of 106x40), as `CONTRACT.md` says.
- Owner changes: the in-play hit-box drawing of `PlayerSabreSlash` removed; `PROTO_SABRE_SOUND`
  (`proto_sabre.ogg`, an 8-bit version of the X6 Shadow Giga slash, +2 dB; the owner allows this one X6-derived
  sound, see `NOTES.md`); the sabre's half hits 3/10/15 → 4/12/20, X6 enemies 2/3 of full; art round 36 (`sabre_2`
  head turn toned down) and round 37 (air pull-out: right hand, halfway frame facing the camera); left and right turn
  him during a floor slash (new test, seen to fail with the rule removed).
- Automated checks (Corretto 17, `--no-daemon`): `engine:test` 675, `core:test` 1482, `core:smokeTest` 462, all
  passed; `lwjgl3:jar` built.
- Play test 7: passed (owner, 2026-11-14).
- Diffity round 4: 0 threads; closed. The owner chose to commit and merge with no further diff round.

### 2026-11-14: `master` taken in before the merge

- `git merge master` (merge commit `036b74937`, parents `0ed32aa66` and `c6de9d876`): `137a`, `138a`, `139a`,
  `081f`, `081g` and the 2 engine small-bug branches.
- Conflicts and resolutions:
  - `Junkroid.kt`, `HoverGunner.kt`, `MetaWheelF.kt`: imports only. `FallingPiece` and `EnemyShot` from `master`,
    `PlayerSabreSlash` and `ProtoBuster` from `060c`; the 060c damage rows are as before.
  - `D1000.kt`, `D1000Core.kt`: `060c` only renamed the slash class; `master` (`081f`) rewrote the fight. Took
    `master`, with `PlayerSabreSlash` in place of `MegamanXSabreSlash` (damage rows, the core's open-hit rule, the
    slash dodge lookup by tag). The D-1000 tests (`D1000Rig`, `D1000EyeTest`, `D1000AttacksTest`) got the same
    rename; the dodge test spawns the slash with `ConstKeys.PARENT` (the X sabre) in place of `SabreKind.AIR`.
  - `MegamanXSabre.kt`, `MegamanXSabreSlash.kt` (modify/delete): kept deleted. The `081g` item-number change of the
    slash `TODO` (item `141`) is already in `PlayerSabreSlash`.
  - `MegamanX.kt`: the crouch and climb animations from `master` next to `MegamanXSabreMoves.ALL`; the animation
    predicate from `master` (room transition, still climb) with `sabre.move`.
  - `MegamanXSprites.kt`: `master`'s key order (`damaged || stunned`, climb, ladder pose, crouch) with `sabre.move`.
  - `MegamanXSabreTest.kt`: `master`'s shielded D-1000 sabre test with the 060c `slash()` helper.
  - `MegamanXBuster.kt`: the 060c params file, with the X offsets below.
  - `MegamanXClimbCrouch.kt` (no text conflict, compile error): import of the shared `players.setSideSpeed`.
  - `entity-tags.txt`, `MegamanXController.kt`, `players/README.md`: merged with no conflict; both sides kept.
- The 3 `081g` X rules in the 060c structure:
  - Shot on a ladder: `BusterParams.climbOffset` (new, `BusterOffset(SHOT_OFFSET_X, SHOT_OFFSET_Y_CLIMB)`) and
    `PlayerBuster.shoot(status, climbing = true)`; a ladder shot with no climb offset throws
    `BusterClimbOffsetMissingException` (tested in `ProtomanTest`). Proto Man passes `climbOffset = null`.
  - Shot in a crouch: X's `crouchOffset = BusterOffset(SHOT_OFFSET_X + SHOT_OFFSET_X_CROUCH, SHOT_OFFSET_Y)`. X's
    `B` actuator calls `MegamanX.shoot(status)`, which passes `crouched` and `climbing` from the `CROUCHING` and
    `CLIMBING` behaviors. Same position as `master`, except one accepted edge case (`TODO` in `MegamanXBuster.kt`):
    a crouched shot no longer adds the air offset (`+0.1`) when the feet sensor misses the ground during a crouch.
  - No slash on a ladder: `megamanXSabreParams` `canStart` (`MegamanXSabreMoves.kt`) tests `AIR_DASHING` and
    `CLIMBING`.
  - `players/README.md`: the buster paragraph names the climb offset.
- Automated checks (Corretto 17, `--no-daemon`): `engine:test` 689, `core:test` 1900 (`MegamanXClimbCrouchTest` 16,
  `MegamanXSabreTest` 43, `ProtomanTest` 130), `core:smokeTest` 462, all passed; `lwjgl3:jar` built. `engine:test`
  ran in the first run on the merged tree (that run stopped at the `setSideSpeed` compile error in `core`) and was
  up to date in the second; the other test tasks ran in the second run.
