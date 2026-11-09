# Process log: mega-refactor-081e-x-sabre

## Status

- State: merged
- Goal: phase 4 of X: the Z-sabre slash on `UP` + `B`
- Done:
- Done: plan steps 1 to 6
- Next: none; the X6 intro stage and D-1000 fixes go to `081f`
- Brief: https://claude.ai/artifact/XGVgxiNzqo5jktnePHSchq
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-07

- Folder made at the `081d` merge. No branch exists yet.
- Branch start: worktree `.worktrees/mega-refactor-081e-x-sabre` from `master`.
- Scan (1 agent; `temp/081e/scan.md` in the main checkout). `SCOPE.md`, `CONTRACT.md` and the brief drafted; brief
  published.

## 2026-11-09

- Owner answers: B1 the art bounds; B2 both `SabreBlock` and `MetaWheelF` sabre-only (not the recommendation);
  B3 all as X6. `SCOPE.md` and `CONTRACT.md` approved. `PLAN.md` written.
- Step 1: test room `sabre` (1 agent, `level-builder`): tiles x 237-296, spawn 6, `SabreBlock`, a 7-tile wall, a step
  down, `Junkroid`, `MetaDridler`, `MetaWheelF` between 2 bumps, `HoverGunner`. Map width 239 -> 299.
- Steps 2 to 6: `MegamanXSabre` (one clock), `MegamanXSabreSlash` (`DAMAGER` box from the art alpha bounds, a test
  checks the table against the strips), the `B` rule, the X6 moves, `X6Damage.Table` (`SABRE_ONLY` for `SabreBlock`
  and `MetaWheelF`), `D1000` 2, the `D1000Core` stun. The 2 resolved `TODO`s in `SabreBlock` and `MetaWheelF` are
  removed.
- 35 new tests. 14 mutation checks (`temp/081e/mutate1.py`, `mutate2.py`): 12 caught at once; `airrelease` got a new
  test (then caught); the `release` check was dead code (the charge is stopped while the slash press is held), so it
  is removed. A read of the code found that the slash body updated only while on camera; it now always updates.
- The 4 Gradle tasks pass: `engine` 639, `core` 1209, `smokeTest` 451.
- Review before the play test (2 agents: the diff, the manual checks). Findings and fixes:
  - high (both agents): a charged shot's `ChargedShotExplosion` still damaged `SabreBlock` (up to 18 per full shot).
    `SABRE_ONLY` now gives it 0; `NORMAL` keeps it. 2 tests;
  - low: `pressHeld` was cleared only on the release frame; also cleared on the release-continued frame. 1 test;
  - low (accepted, told to the owner): `UP` + `B` gives a buster shot when the slash cannot start (ground dash, air
    dash, wall grab, kick lock); the hit box is 1 world step behind the art; with the art bounds (B1) the box times
    differ from X6 (floor 0 to 0.40 s, air 0.083 to 0.233 s, wall 0.067 to 0.383 s);
  - nits: import order, `abs` import;
  - contract: the 5 `SabreBlock` tiles in `X6_INTRO room1`; how to reach the `D1000` core with a wall slash.
- 2 mutation checks (`temp/081e/mutate3.py`), caught.
- Diff review round 1 (Diffity, 0 threads; the owner's questions came in the chat). Changes the owner decided:
  - sabre-only is a damage gate in `canBeDamagedBy` of `SabreBlock` and `MetaWheelF` (2 tests, 2 mutation checks in
    `temp/081e/mutate4.py`, caught);
  - the sabre does one static damage for every slash kind (`TODO`);
  - `X6Damage` and `X6_HEALTH` removed; each target hard-codes its damage (the same values as before);
  - the "X6" prefix removed from the code names: `DebrisPiece`, `Wreck`, `JunkShot`, `TickFrames`, `TickFrameDef`,
    with merge `TODO`s and 5 backlog candidates in `docs/TODO.md`; the atlas folders and the intro map spawner follow.
  - The 4 Gradle tasks pass: `engine` 639, `core` 1211, `smokeTest` 451. The owner must run again the checks in
    `X6_INTRO room1`, `X6_INTRO room2` and `REFACTOR_TEST_081 sabre`.
- Owner (2026-11-09): tests must drive the real flow; reflection shortcuts are a code smell. A read-only review (1
  agent) listed 7 reflection uses in `MegamanXSabreTest` (one, `D1000`, worked only through a compiler bridge method)
  and older ones in `MegamanXTest`, `MegamanXMovesTest` (081b, 081c), `FriendlyFireTest` (047b) and
  `EntityExistenceTest` (062a). Owner scope: this branch plus the older X tests. Done with no new production API:
  - the mocked `engine.spawn` runs `init()` and `onSpawn()` as `GameEngine.spawnNow` does, with stubbed atlas regions;
    the targets (`Junkroid`, `MetaWheelF`, `SabreBlock`, `D1000`) and the slash spawn for real;
  - damage is checked as the health drop after `takeDamageFrom`, and X is hurt through `takeDamageFrom` (a health
    `PointsComponent` in the setup); the hit box is read through the body's `DAMAGER` fixture and `body.preProcess()`;
  - `MegamanXSabreSlash.placeHitBox` is now `private`; the drag test is split in 2 (damaged, not damaged).
  - 9 mutation checks (`temp/081e/mutate5.py`, `mutate6.py`): 8 caught; `walkdamaged` is an equivalent mutation (the
    `damaged` test in `run()` repeats `playerCanMove`).
  - The 4 Gradle tasks pass: `engine` 639, `core` 1212, `smokeTest` 451.

- Play test rounds (owner, 2026-11-09). Changes the owner asked for, each recorded in `CONTRACT.md` and `NOTES.md`:
  - the sabre hold frame is 0.05 s on the floor and the wall; `UP` + `B` in the ground dash or its end slide ends the
    dash and starts a floor slash;
  - the air dash is 0.4 s at 10 t/s (`SELECT` start 14 t/s);
  - X's feel: rise gravity 42 and fall gravity 60 t/s², fall limit 15 t/s, the same jump, wall kick and knockback
    heights; walk 6 t/s, walk start 0.05 s (a first try at 53.3 / 75 / 18 was too aggressive);
  - a running slash switches kind on a state change (all pairs) with the swing phase mapped (`SabreKind.timeIn`); a
    break from X6 (1 read-only research agent; it found that X6 ends the slash in all 4 cases);
  - the ground dash is 11 t/s with a 15 t/s start; the wall kick side speed is 5 t/s, no longer tied to the walk.
- The X6 intro play test also found stage and boss issues. 2 read-only research agents found the causes: no
  `fade_out_music` on the intro `boss_room`; X's animation key has no room transition case; the `D1000` body never
  leaves (0, 0) (an `ABSTRACT` body placed only in its pre-process); `CROUCH_HEIGHT` is 20 px too high. The owner's
  answers (crouch 44 px, damage to X halved, the air pose holds in the gate, all 4 fix groups) go to `081f`, which
  starts after this merge.
- Deviation: the play-test changes had no subagent review before the owner played them; the owner played them and
  approved the merge.
- The 4 Gradle tasks pass: `engine` 639, `core` 1220, `smokeTest` 451, JAR built. One test fix first: the walk start
  key test stepped past the shorter walk start. 4 mutation checks (kind switch, rise gravity, phase map, dash end
  guard), all caught.
- Merge approval: the owner reported "081e tests are finished, ready to merge to `master`". Diffity: 0 open threads.
  `master` is in the branch. Overlap check: `134a` changes `MegamanXBody.kt` in an independent hunk (`drawBody`);
  `127a` has no shared file.
