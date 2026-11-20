# Process log: mega-refactor-077g-glacier-man

## Status

- State: merged (2026-11-20, squash `e832cda0c`)
- Goal: a richer Glacier Man fight: 2 phases, smarter move choice, chunk and aim throws, Icicle Rain, Sled Ram, freeze-and-shatter death
- Done: branch start; scan; `docs/SCOPE.md` (draft with contract); brief
  (https://claude.ai/artifact/48Cs2LR9WkMbzS9dPjUoAm)
- Done also: owner answers D1 to D4, `SCOPE.md` approved 2026-11-19, `PLAN.md`
- Done also: `PLAN.md` steps 1 to 10; play round 1 closed 2026-11-20 (moveset final, Diffity: 0 threads)
- Done also: step 11 final pass (2026-11-20)
- Done also: step 12 review before the play test (2026-11-20)
- Next: none (3 issues filed on `master`)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch folder prepared on `master`; no branch exists yet. The draft `docs/SCOPE.md` holds the fight design from the
  planning conversation with the owner. It waits for the art of `077f`.

## 2026-11-19 (merge of `077f`)

- `077f` merged: the art is on `master` (final frame list in the `077f` `docs/NOTES.md`). Icicle Rain starts with
  `pound` (owner, `077f` art review). Status `planned`.

## 2026-11-19 (branch start)

- Branch start on `master`: `ITEMS.md` row and `ITEM.md` set to `active`. Worktree
  `.worktrees/mega-refactor-077g-glacier-man/` from `master`. Merge target `master` (item decision 2026-11-13).

## 2026-11-19 (planning)

- Scan of `GlacierMan`, `FallingIcicle`, `OrbAim`, `PlayerDamageNegotiator`, the arenas, `RefactorTest077.tmx` and
  the other worktrees (no overlap; `NOTES.md`). `SCOPE.md` with contract; brief published.
- Owner answers: D1 keep the `FloatingCanHole` enemies as they are, no column skip; D2 `master` damage to the
  player; D3 yes, a double Icicle Rain under 25 % of phase 2 (added as scope item 8b); D4 1-for-1 hits to kill.
- `PLAN.md` written (12 steps, 4 subagent groups).

## 2026-11-19 (implementation)

- 4 subagents, one per plan group; the main session checked each result before the next group. No Gradle, no
  launch, no tests in this round (`gameplay.md`).
- A (step 1): rooms `glacier_man` (spawn 44) and `glacier_man_boss` (boss 29, walls 32/36, gate 41) in
  `RefactorTest077.tmx`, a copy of `GlacierMan_16x14_v2.tmx` `room12` and `boss_room`; map width 25 → 62;
  `GlacierManTileset` added at `firstgid` 673. Horizontal gate in the left wall, as in the stage map.
- B (steps 2 to 4): `GlacierManTuning`; boss damage 2 × `master`; `PlayerDamageNegotiator` entries with
  `fallbackDamage` for other owners; weighted pick, chains on a roll, `TELL` state, chunk/aim throws with `OrbAim`,
  aimed snowballs. Fix in passing: the walls array grew by one on each respawn.
- C (steps 5 to 7): `FallingIcicle` `FORM` and timed drop (`DELAY`, `DURATION`, `STATE` keys); Icicle Rain with a
  ceiling scan of block bodies (decision in `NOTES.md`), double rain under 25 %; Sled Ram.
- D (steps 8 and 9): `PHASE_START` pose (cyan blink, `CHILL_SHOOT_SOUND`, `SHAKE_CAM`); freeze, crack, shatter with
  `IceShard.spawn5`, then the base defeat flow (`defeatDur` 4.65 s).
- Commits: step 1, steps 2 to 4, steps 5 to 7, steps 8 and 9.

## 2026-11-20 (play round 1)

- Owner: the old `ice_blast_attack` strip (front view, sparkles in both hands) does not fit the new throws. Replace it
  with the raised-hand still frame of the throw, flashing as `brake_flash` does.
- New strip `GlacierMan/throw_flash` (2 frames): `chunk` frame 1, then the same frame with black → white (the
  `brake_flash` rule). `ice_blast_attack.png` removed; `Bosses1` repacked (`Basic`); a script check shows every other
  region keeps its name and pixels. Used for the ice-blast tell and the phase-2 start pose.
- Owner: the raised arm of the first `throw_flash` was "too long and noodly" (a consistent complaint). The tell now
  uses `pound` frame 1 (the approved short fist) with the cube on the fist (owner choice B, cube 2 px lower). Owner:
  "the tell looks right now".
- Owner chose to shorten the arms of `chunk`, `aim` and `aim_up` in this branch. A Fable art agent rebuilt all 13
  frames from the `stand` arm block and the `throw_flash` fist (moved or flipped, never stretched); `Bosses1`
  repacked, only the 3 strips changed. New cube spawn points: `chunk` (14, 28), `aim` (13, 20), `aim_up` (13, 32).
  Owner: "looks good".
- Owner: Sled Ram fired snowballs with no throw animation; remove them, Sled Ram is only the jets. Removed (the
  `ramShotsFired` flag and the `direction` parameter of `shoot` went with them). The normal `SLED` keeps its shots.
- Owner: after the ice-cube attack he showed the cube-holding frame with no throw. Cause found by scan: the phase-2
  start pose used `throw_flash` (fist + cube) and waits until the move in progress ends. Owner chose a raised fist
  with no cube: new strip `fist_flash` (`pound` frame 1, then black → white); `Bosses1` repacked, only the new region
  added.
- Owner: after the shatter the standard defeat explosions played; Glacier Man must only freeze and shatter. The
  explosions are opt-in per boss (each calls `explodeOnDefeat`), so `GlacierMan` stops calling it; no
  `AbstractBoss` change. Defeat time: the sequence + a 1 s hold (2.65 s, was 4.65 s).
- Owner: keep the defeat orbs, but under his frames and at the moment he shatters. The orb ring and its sound now
  spawn at the first `frozen_shatter` frame (with the shards), and `spawnDefeatOrbs` is cleared so `onDestroy` does
  not spawn them again. While defeated, his sprite draws in `FOREGROUND` at the orb priority + 1
  (`ExplosionOrb.SPRITE_PRIORITY_VALUE`, a new constant for the old literal 20).
- Round 1 closed: owner "looks good"; "new art and behaviors are ready to merge". Diffity: 0 threads.

## 2026-11-20 (final pass, step 11)

- 2 subagents wrote the fight tests from each `TEST-DEBT.md` line on a `GlacierManRig` (`077b` pattern): 8 specs,
  200 tests (`glacierman/` package and `projectiles/FallingIcicleTest`). Commits `a41279fa1`, then group 2.
- Mutation checks: 76 mutations in 15 Gradle runs; 75 failed their tests. M58 (the `JUMP` term of the phase-start
  rule) is an equivalent mutant: the rule also needs `FEET_ON_GROUND`. Lists with failing cases:
  `temp/077g-final-pass/mutations-group1.md` and `mutations-group2.md`.
- Spec lines corrected to the code: the cube spawn points of play round 1; the `GapingFish` icicle spawn is
  commented out on `master` (the default path is the map icicle); damage in the phase-start pose is possible, but the
  roll-over hit's 1.25 s damage timer covers the pose in play; the `NOTES.md` Sled Ram snowball decision is void.
- Bug candidates (`TODO.md`): the jump-over lands about 1 tile past the player (y drag), not 2; a standing player
  counts as "above" (`>=`, as on `master`). No production change in the final pass.
- Existing tests in the `TEST-DEBT.md` list: all pass with no change.
- Automated checks: `engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` pass;
  `entity-tags.txt` unchanged.

## 2026-11-20 (review before the play test, step 12)

- 1 Sonnet review agent: the diff, the contract, each manual check (no launch, no Gradle). 2 findings:
  - `GlacierMan.isOwnLiveIcicle` takes any `SPAWN_QUEUED` icicle as own; rare, no live map has the setup. Accepted
    with a `TODO` at the code; bug candidate in `TODO.md`.
  - Manual row 3 needs weapons: `--weapons all` added to the row in `SCOPE.md`.
- All 6 manual rows otherwise OK (commands, rooms, spawns, wall references, ceilings, tiles). The fix is a comment
  only, so the automated checks were not run again.

## 2026-11-20 (merge)

- Production code unchanged since the owner's round-1 approval ("ready to merge"), so no new play round: the final
  pass added tests, docs and one `TODO` comment.
- `master` (`109a` screen wrap: `Body`, `WorldSystem`, sprites) merged into the branch; automated checks pass again.
  No file overlap with the other active branches.
- Squash `e832cda0c`. Mutation logs moved to the main checkout `temp/077g-final-pass/`.
