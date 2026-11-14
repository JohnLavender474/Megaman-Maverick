# Plan: mega-refactor-060c-protoman-buster

Scope and contract: approved 2026-11-11. The owner's answers are a first try; steps 4 and 5 keep a pivot cheap (values,
then the class entries as a step of their own).

New API and its first caller:

| API | First callers |
|---|---|
| `players/PlayerCharge.kt` (builder, `chargeStatus`, `stopCharging`) | `Megaman`, `MegamanX`, `Protoman` |
| `players/PlayerBuster.kt` (+ its params and the `B` actuator) | `MegamanX`, `Protoman` |
| `ProtoBuster`, `ProtoStrike` | `Protoman` (through `PlayerBuster`) |

## Steps

1. **Art drafts (owner rounds, in parallel with steps 2 to 4).** A subagent drafts the v2 shoot frames (`stand_shoot`,
   `run_trans_shoot`, `run_shoot` x4, `jump_shoot`) by the `060b` method, and the `_charge_full` recolours (stand, run,
   jump) with 2 or 3 colour options. Files in `temp/mega-refactor-060c-protoman-buster/v2/`. The shot frames
   (pellet, Proto Strike) are cut from the sheet as they are. Check: the owner approves the drafts.
2. **Test room.** A subagent adds the room `buster` to `RefactorTest060.tmx` with the `level-builder` skill: a flat
   floor, a player spawn, a `PicketJoe` and a `SniperJoe`, painted `tiles`. Check: the level analyzer reads the room,
   its spawn and 2 enemies.
3. **`PlayerCharge`.** Move the charge builder, `chargeStatus` (with a `chargeable` gate) and `stopCharging()` to
   `players/`. Megaman and X call them; Megaman keeps his weapon gate and his other charge reads. Check: `core:test`
   (with `MegamanXTest`); no other diff in behavior.
4. **`PlayerBuster`.** Move `MegamanXBuster` to `players/PlayerBuster` with `BusterParams`: fire rule (press or
   release), cap (or none), full cap, waits, pose times, speeds, charge levels, spawn offset, and the spawn function
   of each level. X passes his current values. Check: the 9 "the buster" tests in `MegamanXTest` pass unchanged.
5. **`ProtoBuster` and `ProtoStrike`.** The 2 classes in `entities/projectiles/`, their regions, `entity-tags.txt`.
   `ProtoBuster` follows `Bullet`'s rules for a player shot; `ProtoStrike` explodes on its first hit (the full
   `ChargedShotExplosion`), also on a kill and on a shield. Check: `core:smokeTest`; a `ProtoStrike` test.
6. **Class-table entries.** Go through the 68 lines in 35 files that name `Bullet` or `ChargedShot`. For each line
   about a player shot, add `ProtoBuster` (with `Bullet`'s value) or `ProtoStrike` (with the full `ChargedShot`
   value); leave lines about enemy bullets. Write the list with the choice per line in `docs/NOTES.md`. Check: a test
   that `StandardDamageNegotiator` gives the same damage for the new classes as for `Bullet` and the full
   `ChargedShot` at each size; `core:test`.
7. **Proto Man's buster.** `Protoman` gets the charge (step 3), a `PlayerBuster` with `ProtomanValues.BUSTER`
   (release, no cap, full only, Megaman's numbers), the `B` actuator, the charge stops (hit, room transition, death,
   destroy), the shoot and charge keys and the regions from step 1. Check: buster tests in `ProtomanTest` through
   `InputFrame` (release fires, cooldown, no cap, full charge gives a Strike, a hit drops the charge);
   `core:smokeTest`.
8. **Docs.** `players/README.md` (buster and charge), branch `docs/NOTES.md`, process log. Check: the README states
   the rules and names no counts.
9. **Review before the play test.** Automated checks, then review subagents (diff; the `REFACTOR_TEST_060` checks;
   the X and Megaman control checks). Then the play checks and Diffity.

## Plan after the scope change (2026-11-12)

Steps 1 to 9 above are done (the first scope). These steps replace the rest. Each step ends with its check.

10. **Remove the charge and the Strike.** `ProtoStrike` (class, art, tag line, the entries in 26 files), Proto Man's
    charge component and `stopCharging` calls, the gold charge frames, `run_shoot`, `run_trans_shoot`, `jump_shoot`;
    revert the nullable half time. Check: `core:test` and `smokeTest` pass; no `ProtoStrike` in `core/src`.
11. **Standing shot.** `UP`+`B` on the ground fires one `ProtoBuster` through `PlayerBuster` (no charge levels for
    Proto Man); the shot pose locks left and right; `A` ends the pose. Check: the new `ProtomanTest` buster tests,
    each seen to fail once.
12. **Share the sabre.** `MegamanXSabre` and `MegamanXSabreSlash` become `players/PlayerSabre` and
    `players/PlayerSabreSlash` with `SabreParams` and `SabreMove`; X passes his data. Check: X's sabre and buster
    tests pass unchanged (only renamed types); `entity-tags.txt` updated.
13. **Combo.** The floor move list and the queued press; a state change ends the combo. Check: combo tests with a
    3-move test list, and X's tests still pass.
14. **Damage.** `PlayerSabreSlash` and `ProtoBuster` at half-charge values in `StandardDamageNegotiator` and in each
    class-keyed table that names the half `ChargedShot` (list in `docs/class-entries.md`). Check: the damage tests.
15. **Art.** Body frames from the sheet row "FLAME SWORD/TENGU BLADE" with the hilt edited; purple arcs from the
    Tengu Blade arcs; the third floor slash by hand. A contact sheet for the owner, then pack `Protoman_v2`. Check:
    the owner's answer on the contact sheet; `smokeTest` finds every region.
16. **Proto Man's sabre.** `PROTOMAN_SABRE` data (frame times, arc sizes, hit boxes from the arcs), the `B` actuator
    (shot or sabre), the animation keys, the reset on hit, room transition and death. Check: `ProtomanTest`.
17. **Test map.** A `sabre` room (enemies, a ledge) and the `buster` room for the standing shot in
    `RefactorTest060.tmx`. Check: the review before the play test.
18. **Docs.** `players/README.md` (sabre), the branch docs. Check: the docs name no removed type.
19. **Checks and review.** The automated checks, the review before the play test, then the play test and Diffity.

## Plan after scope change 2 (2026-11-13)

Steps 10 to 18 are done, and step 19 (checks, review, play test) produced the art rounds 6 to 24 and this change.
These steps replace the rest. Each step ends with its check.

20. **Damage per hit.**
    - `SabreMove` gets a full-strength flag; `PlayerSabreSlash` exposes the strength of its current move.
    - Each class-keyed table entry for `PlayerSabreSlash` reads it, as `ChargedShot` entries read `fullyCharged`.
    - Proto Man: hits 1 and 2 and the air slash are half strength, hit 3 is full. All of X's moves are full.
    - Check: damage tests for half and full on `LARGE`, `MEDIUM` and `SMALL` targets; the X tests are unchanged.
21. **Weapon slots.**
    - A `ProtomanWeapon` enum (`SABRE`, `BUSTER`), the current slot, a `SELECT` actuator gated by
      `selectButtonAction == NEXT_WEAPON`, `SELECT_PING_SOUND`, and a reset to `SABRE` at spawn.
    - Check: `ProtomanTest` slot tests, each seen to fail once.
22. **The buster that shoots while moving.**
    - Remove `canShootStanding`, `shootStanding` and the run lock.
    - `B` with the buster slot shoots in every state; `UP`+`B` starts one slash (no chain).
    - Set the run and air shot offsets.
    - Restore `run_shoot`, `run_trans_shoot` and `jump_shoot` from `ebcffebb6`, add the `_shoot` keys, and pack.
    - Add a `TODO` for the ladder rule at the `B` actuator (`060d`).
    - Check: `ProtomanTest` buster tests; `smokeTest`.
23. **Shield.**
    - A `SHIELD` fixture on the facing side, from the round 25 guard art box.
    - A guard timer (about 0.2 s) that restarts on a run, an attack and a hit.
    - The fixture is active only while the guard is on; `guard_stand` and `guard_jump` show while it is on.
    - A `WhiteBurst` spawns at a deflect.
    - Pack the guard art.
    - Check: `ProtomanTest` shield tests, with a real `Bullet` from the front and from behind, each seen to fail once;
      `smokeTest`.
24. **HUD icon.**
    - `PlayerStatsHandler` draws Proto Man's slot icon (2 small icons) next to the health bar. A `TODO` names item
      `153` for the other players and the fade.
    - Check: `smokeTest` finds the icon regions; a test reads the icon for each slot, if the handler can be built
      headless.
25. **Test map and docs.**
    - The `buster` room gets a `SniperJoe` for the shield.
    - Update `players/README.md` (slots, shield) and the branch notes. Add the `081g` ladder note to `TODO.md`
      ("Backlog candidates") and `NOTES.md` ("Overlap with other branches").
    - Check: the review before the play test.
26. **Checks and review.** The automated checks, the review before the play test, then the play test and Diffity.

## Plan after scope change 3 (2026-11-13, play test 5)

27. **Guard art.**
    - A full-body shield, held by a fully drawn free arm.
    - 1-2 pull-out frames on the ground and in the air.
    - A guard pose on the ground and in the air.
    - Check: the owner's answer on the art sheet.
28. **Held guard.**
    - `DOWN` held raises the guard.
    - Pull-out frames first, then the shield fixture turns on.
    - He stands still on the ground; in the air he keeps his arc.
    - No attack while guarding.
    - The existing breaks drop it.
    - No guard on a ladder (`TODO` for `060d`).
    - On the ground, `A` does nothing while `DOWN` is held (`TODO` for the `060d` slide).
    - The passive guard timer goes.
    - Check: `ProtomanTest` guard tests, each seen to fail once.
29. **Full-body shield fixture** from the new art box. Check: the placement tests for each direction.
30. **HUD icon under the health bar.** Check: the review before the play test.
31. **Docs and the contract checks**, then the review before the play test, the play test and Diffity round 4.

## Plan after scope change 4 (2026-11-13, play test 6)

32. **Round 34 art.**
    - Copy the 6 guard strips and the 13 back-shield strips of `guard/round34/` into `Protoman_v2/` (not the `*_arc`
      strips); delete `guard_stand_start` and `guard_stand`; pack with `pack_atlas.py`.
    - Check: `ProtomanGuardStripsSmokeTest` (each guard strip has the frame count of the code; no stand guard strip);
      `smokeTest` finds every region.
33. **Crouch guard.**
    - While he guards on the ground (pull-out and hold), the `BODY` and `DAMAGEABLE` boxes are 19 art px high, feet
      on the floor, for each gravity direction. The physics body keeps its size.
    - The ground keys become `guard_crouch_start` (4 frames) and `guard_crouch` (2, the `stand` times); the ground
      pull-out is 4 x 0.05 s, the air pull-out 3 x 0.05 s.
    - Check: `ProtomanTest` crouch and pull-out tests, each seen to fail once.
34. **Shield size.** The 7x18 round 34 boxes (`GUARD_CROUCH_SHIELD_BOX`, `GUARD_JUMP_SHIELD_BOX`). Check: the
    placement tests for each direction and the cover test of the crouched body.
35. **Put-away.**
    - After a release of the full guard: `guard_crouch_end` (4 frames) on the ground, `guard_jump_end` (3) in the air,
      0.05 s each; the shield is off at once and he stands at once.
    - `B`, `A` (a new press), left and right cut it; a break ends it.
    - Check: `ProtomanTest` put-away tests, each seen to fail once.
36. **Docs and checks.** `CONTRACT.md`, `NOTES.md`, `BRIEF.src.html`, the process log; the automated checks; then the
    review before the play test, the play test and Diffity round 4.

## Plan after scope change 5 (2026-11-14, play test 7)

37. **Round 35 art.**
    - Copy the guard strips, the 13 back-shield strips of `guard/round35/back/` and the new `crouch_shoot`,
      `crouch_slash` and `crouch_slash_arc` into `Protoman_v2/`; pack with `pack_atlas.py`.
    - Add the crouch slash to `gen_sabre.py` (it also reads `guard/round35/arcs_crouch.json`) and regenerate
      `ProtomanSabreMoves.kt`; the existing moves stay byte-identical.
    - Check: `ProtomanGuardStripsSmokeTest` finds the 3 new strips with their frame counts; `smokeTest`.
38. **Crouch attacks.**
    - `SabreState.CROUCH` and `PlayerSabre.start(state = ...)`; Proto Man's state rule keeps `CROUCH` while that
      slash runs on the ground; the crouch slash stops him as a floor slash.
    - `BusterParams.crouchOffset` and `PlayerBuster.shoot(status, crouched = true)`; Proto Man's crouch offset from
      the `crouch_shoot` muzzle point.
    - `B` with `DOWN` held on the ground attacks from the crouch; the crouch boxes stay for the whole attack; the
      `crouch_shoot` key; left and right turn him during the crouch shot.
    - Check: `ProtomanTest` crouch attack tests, each seen to fail once.
39. **Test room `shield`.** Spawn in the middle, a `SniperJoe` by each wall out of the camera at the spawn, the
    ledge painted with a 24 px tile. Check: the level analyzer reads the room.
40. **Docs and checks.** `CONTRACT.md`, `NOTES.md`, `SCOPE.md`, `BRIEF.src.html`, `players/README.md`, the process
    log; the automated checks; then the review before the play test, the play test and Diffity round 4.
