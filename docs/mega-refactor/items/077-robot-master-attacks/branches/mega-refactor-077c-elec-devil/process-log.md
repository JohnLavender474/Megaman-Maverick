# Process log: mega-refactor-077c-elec-devil

## Status

- State: merged
- Goal: a richer `ElecDevil` fight (Wily stage 1): 2 phases, eye pattern deck, Live Wire, Tether Claw, art polish
- Done: all steps in scope; final test pass; review; squash merge into `master` (2026-11-19)
- Next: none. Spark Serpent, Circuit Floor and Storm Rain go to a later branch of item 077
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-18

- Idea round with the owner before the branch start (idea list and contact sheet of the current frames:
  `temp/077-elec-devil/` in the main checkout). Owner choices are in `docs/NOTES.md` "Decisions".
- Branch start on `master`. Merge target: `master` (item decision of 2026-11-13).
- Scan of `ElecDevil*`, the boss room of `WilyStage1_v2.tmx`, the `077a`/`077b` shared parts. Owner answers to 4
  planning questions (phases, path format, shots against cubes, art drafts by agents); all recommended options.
- `SCOPE.md`, `CONTRACT.md`, `PLAN.md` written. No brief: every decision is answered (as in `077b`).
- `PLAN.md` step 2 (map agent; no Gradle, no launch): `RefactorTest077.tmx` (rooms `elec_devil` and `elec_devil_boss`,
  copied from `room10` and `boss_room` 429 with `DarknessV2` key 1), `REFACTOR_TEST_077` in `LevelDefinition` and
  `LevelCompletionScreensTest`, polylines `SerpentPathA` and `SerpentPathB` (test map 7, 8; stage 499, 500, layer
  `children`) and `serpent_paths` on the `ElecDevil` object in both maps. Check script:
  `temp/077c-elec-devil/check_maps.py` in the main checkout (0 failures). The build runs at the review step.
- Art drafts (3 Fable 5.2 agents): body polish, transitions and death, effects, in `temp/077c-elec-devil/art/` of the
  main checkout; final transition strips on the new body in `art/final/`. Owner choices in `docs/NOTES.md`.
- `PLAN.md` steps 3 and 4 (implementation agent): 2 health phases (2 x damage table, phase tell), `ElecDevilTuning`
  rows (phase 1 = `master` values), a cycle with a phase-2 pick point (LAUNCH only until steps 5 and 6), Eye Spread
  (3 `ElecBall`s, `OrbAim` with a new `leadTime` parameter), Live Wire (`LiveWireLinks`, a `GameLine` damager per
  link, placeholder `Bolt` art), `PlayerDamageNegotiator` entries (3, the current values). `ElecDevilFightTest` 5
  tests. Mutation run: 3 mutations (Live Wire gap, lead time row, phase tell sound), each failed its test; code
  restored. `core:test` passed (agent). Main session: `core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule`
  passed.
- Agent open questions for round 1: health scaling per phase (kept per phase); Eye Spread balls at one moment or with
  a gap; HAND shots keep the `master` rule.
- Play round 1, owner note: some Live Wire arcs cannot be dodged. Fix (gameplay round, no Gradle, no tests): a link
  joins only 2 pieces of one band, `JUMP_OVER` (rows 0 to 1) or `CROUCH_UNDER` (rows 2 to 5); each link picks a band
  at random, then a pair in it. Random targets keep the band (linked pieces take their target rows first). Before
  the second piece of a `JUMP_OVER` pair, the launch delay is set so the pair passes a player in at most 0.35 s
  (full jump keeps the feet above 1.69 tiles for 0.51 s; with an air dash, 0.76 s). Numbers:
  `temp/077c-live-wire-dodge/jump_sim.py` in the main checkout. Rules in `docs/TEST-DEBT.md`.
- Art review (read-only agent): the orb palette is near NES (3 colors), but the orb is drawn 1:1 next to the 2x body;
  the placeholder arc (yellow `Bolt`, stretched) fits neither. Owner accepted the recommendations: arc option B and
  one speed ramp per phase. `ElecDevilLiveWireArc` and `ElecDevilLiveWireCap` added to `Projectiles1` (copies of the
  drafts), atlas repacked with `Basic`; check script `temp/077c-live-wire-arc/check_regions.py` in the main checkout:
  110 old regions, same names and pixels, 0 failures. `ElecDevilBodyPiece` draws up to 16 arc tiles of 32 px and 2
  caps; the `Bolt` placeholder and its `TODO` are gone. No Gradle (gameplay round).
- Owner answers to the 3 questions with no recommendation: Eye Spread with a short gap (0.1 s per ball, code done),
  orb and arc at 2x, all ElecDevil art in exact NES colors. Arc code changed for 16x8 art tiles and 8x8 caps drawn
  at 2x. Art agent (Fable 5.2): recolor of the ElecDevil frames and `ElecBall`, redraw of the orb, arc and cap at
  half resolution, repack of `Bosses3` and `Projectiles1`; work folder `temp/077c-nes-2x-art/` in the main checkout.
- Art agent result: 82 frame files recolored to FCEUX NES entries (mapping in `temp/077c-nes-2x-art/nespal.py`;
  agent choices: light blue `$21`, dark blue `$0C`, green `$2B`, gray `$10`, `#00E8E8` to `$2C`, eye `$27`/`$17`,
  pupil `$16`); orb, arc and cap redrawn at half resolution (3 colors each). `ElecExplosion` (used only by
  `ElecDevilBodyPiece`) recolored too. `Projectiles1` and `Explosions1` repacked with `Basic`. `Bosses3` cannot be
  repacked with `Basic` (5 D-1000 strips do not fit; the sheet came from the `081f` `MaxRects` recipe), so the 79
  `ElecDevil` regions were written into the old sheet in place, `Bosses3.txt` unchanged. Checks
  (`temp/077c-nes-2x-art/run_checks.sh`, run again by the main session): `Bosses3` 89 equal + 79 changed,
  `Projectiles1` 107 + 5, `Explosions1` 24 + 1, 0 failures. Backups in `temp/077c-nes-2x-art/backup/`.
- `078a` merged on `master`. Owner chose to take `master` before the round-1 play test. Round-1 work committed
  (`6c013e38d`), then `master` merged. Conflicts: `WilyStage1_v2.tmx` (took `master`'s map, added the serpent paths
  again as 508 and 509, no duplicate ids) and `Projectiles1` (repacked with `Basic`; vs `master` 109 regions equal and
  3 changed as expected, vs the branch 112 equal; 2 regions added on each side). Phase-2 call to `powerDown()` and a
  generator in the `RefactorTest077` boss room. Inputs: `temp/077c-take-master/` in the main checkout.
- Owner: the new body art goes into round 1. Art agent (Fable 5.2) snapped the body and transition drafts to NES
  (78 files, 0 non-NES colors, 0 partial alpha, silhouettes unchanged; `temp/077c-body-art-nes/` in the main
  checkout). ElecDevil frames moved from `frames/Bosses3/` to `frames/Bosses4/` with a new `Bosses4.tps` and
  `TextureAsset.BOSSES_4`; `ElecDevilBody` and `ElecDevilBodyPieces` read it. Checks: `Bosses4` before the new art
  equal to the 79 old ElecDevil regions; after the new art, 24 changed regions and 55 equal (pieces of the legs and
  body, which the drafts keep). `Bosses3` sheet not repacked (`Basic` fails by 1 sprite). `appear` 8 frames of 0.05 s,
  new `defeated` animation. Credits wait for step 7 (owner to name the original artist).
- Owner: the eye is too cute; glow while orbs are shot. Art agent (Fable 5.2) redrew the eye in 6 strips and drew
  `shoot.png` (`temp/077c-eye-redraw/` in the main checkout; 0 non-NES pixels, changes only in the eye area, pieces
  unchanged). `ElecDevil.isEyeSpreadShooting()`; `ElecDevilBody` plays `shoot` (4 frames, 0.075 s, loop) in `STAND`
  while it is true. `Bosses4` repacked: 73 regions equal, 6 changed, `shoot` added.
- 2026-11-19, owner: more variety for the eye attack, and the claw as an attack. Owner chose (recommended options)
  the colour pattern deck (Fan blue, Ring red, Stall green, Bounce yellow, phase 2 only) and the Tether Claw (windup,
  low and fast out, hold, low and fast back; harmless tether), in round 2 now. `PLAN.md` step 4b.
- Art agent (Fable 5.2, `temp/077c-tether-claw/art/` in the main checkout): `charge_<color>` and `shoot_<color>` for 4
  colors (blue replaces the orange eye of round 1 for Fan; agent recommendation), `ElecBall` orb and hit in red, green,
  yellow, `hand_windup`, `hand_fired`, `hand_recoil`, `claw`. Agent checks: exact FCEUX colors, no pixel change
  outside the changed parts. `charge.png`, `shoot.png`, `hand.png` removed from the frames (no reader).
- Code: `EyePattern`, `ClawPhase`, `ElecDevilClaw` (new entity, tag list and player damage entry updated),
  `LiveWireArcSprites` (the arc drawing, moved out of `ElecDevilBodyPiece` and shared with the claw tether);
  `ElecBall` takes color, speed, stall and bounce props; `HAND` eye shots removed. Atlas check by script: `Bosses4`
  77 regions equal, 3 removed, 15 added; `Projectiles1` 114 equal, 6 added. No Gradle (play round).
- Owner: the tether looks like a damager; redesign the claw so the only dodge is under the high end of the slope.
  `ElecDevilClaw`: aim state with a flickering guide line during the windup (the claw spawns at `HAND` start), then a
  straight path socket → far wall and back; the claw rectangles act only after `fire()`; the tether is 2 `GameLine`
  damagers. `LiveWireArcSprites.alpha`. `ElecDevil`: `spawnClaw` at `HAND` start, `fire()` at the windup end;
  `CLAW_WALL_POCKET` and `CLAW_DIVE_X` removed. Windups 1.4 / 1.3 / 1 / 0.9 s. No Gradle (play round).
- Owner play test of the redesign: "looks good so far", 3 nitpicks. (1) The flying claw looks smaller than the claw
  in the windup (windup claw about 29x43 art px, `claw.png` about 27x17). (2) The body frames still show a fist; the
  firing hand must be a claw in every frame. Both went to an art agent (Fable 5.2, drafts in
  `temp/077c-claw-frames/art/` of the main checkout). (3) Normal rows: `clawDelayDur` 0.2 s (new `ClawPhase.DELAY`,
  body plays `stand`) before the windup and the guide line, so the orbs of `CHARGE` can be dodged first; hard rows 0.
- Art agent done (0 non-NES px, 0 changed px outside the hand area). Frames copied into `frames/Bosses4/ElecDevil/`,
  `Bosses4` repacked (1950x1070): 70 regions equal, 19 changed (as expected: `stand`, 4 `charge_*`, 4 `shoot_*`,
  `appear`, `turn_to_pieces`, `defeated`, `claw`, 6 pieces). Backup `temp/077c-claw-frames/atlas-backup/`.
  `ElecDevilClaw`: hitbox 1.625x1.8125 tiles, sprite 3 tiles centred on the body, the claw starts and ends with its
  tether point on the socket. No Gradle (play round).
- Owner: claw size and claw frames look good; ready to merge. Answers: steps 5 and 6 move to a new branch of item
  077; the original ElecDevil art is by boberatu (`README.md`); no Diffity review. Round 2 committed (`19122f3d0`);
  `master` merged in (1 conflict: `nextobjectid` of `WilyStage1_v2.tmx`, took 511 from `master`; 0 duplicate object
  ids). End of the play-test phase.

## 2026-11-19 (merge phase)

- `README.md`: Elec Devil credit, original by boberatu, updated by Claude Fable 5.2 (directed by the developer).
- Final test pass (implementation agent): every `docs/TEST-DEBT.md` line has a test. New specs `LiveWireLinksTest`,
  `ElecDevilLiveWireFightTest`, `LiveWireArcSpritesTest`, `ElecDevilArtTest`, `ElecDevilClawTest`; `ElecDevilFightTest`
  broken tests updated to the current design. Rigs: `BossRig` records spawn props and times and calls
  `GameObjectPools.performNextReclaim()` each tick (without it the full `core:test` run ran out of heap: 7.1M
  `Vector2`); `RangeScriptedRandom` scripts `nextInt`. Not tested as written: the "no sparks" part of the generator
  rule (only the spark sprites' `hidden` flag); the claw safe place is tested as a shape, not against the tile
  figures in `NOTES.md`.
- Mutations: 33 in 6 batched runs, each failed at least one test; production code restored after each run.
  M1 band check on one piece only, M2 fallback ignores the band, M3 `CROUCH_UNDER_MIN_ROW = 1`, M4
  `JUMP_CLEAR_TIME = 0.6`, M5 pass gap without body width, M6 launch delay without `trailBehind`, M7 target row not
  removed, M8 linked pairs take any band, M9 arc tile not cut, M10 arc drawn with link gone, M11 no power-down, M12
  `appear` 0.06 s, M13 `defeated` loops, M14 no "never previous", M15 Fan no gap, M16 Ring 30°, M17 Stall no pause,
  M18 Bounce not mirrored, M19 bounce always flips x, M20 no `EYE_SHOOT_MIN_DUR`, M21 balls always blue, M22 charge
  always blue, M23 claw no hold, M24 claw sprite in aim, M26 tether half thickness 12, M27 hand fixtures in OUT, M28
  defeat keeps claw, M29 destroy keeps claw, M30 claw damage 2, M31 half windup, M32 guide alpha 1, M37
  `HAND_OFFSET_Y = -2.5`, M38 STAND 2x.
- Bug found by the test pass: the first STAND of each spawn showed `shoot_blue` (`isEyeSpreadShooting()` was true with
  0 shots, time 0 < `EYE_SHOOT_MIN_DUR`). Fix: `isEyeSpreadShooting()` needs `eyeSpreadShots > 0`. Test "plays the
  state animation in the first STAND, before any CHARGE" enabled.
- Spec notes from the test pass (code kept, tests follow the code): the claw spawns at the windup start, after
  `ClawPhase.DELAY` (added in round 2), not at the `HAND` start; the hand fixtures also act in `DELAY`;
  `BOUNCE_COOLDOWN` has no effect with 1 bounce (both boss rooms have one floor block).
- Automated checks after the bug fix: all pass (`engine:test core:test core:smokeTest lwjgl3:jar detektMain
  core:verifyRandomRule`).
- Review before the merge (1 Sonnet agent, no Gradle, no launch): no bugs; every manual check resolves (levels,
  rooms, spawns, props, object references, atlas regions, entity tags, damage entries). Findings and what was done:
  (1) `CONTRACT.md` stale (Serpent rows, no eye patterns or claw): updated; (2) no damage test: new
  `ElecDevilDamageTest` (24 tests: boss table = 2 x `master` per weapon, phase count and defeat, player damage 3 per
  damager), 3 mutations (`Axe` 4 to 3, sabre 4 to 3, `ElecBall` 3 to 2), each failed its test; (3) tether pixel
  constants: now tiles scaled by `ConstVals.PPM` (same values); (5) unnamed `loop` booleans in `ElecDevil` and
  `ElecDevilClaw`: named. Kept: (4) `ElecBall.DART_SPEED` key, built from `ConstKeys.END` and `ConstKeys.SPEED`;
  (6) `bounceOff` corner heuristic (1 floor block per room); (7) `BossRig` pool reclaim per tick (fixes the heap).
- Overlap with active branches: see `docs/NOTES.md` (`078c` `Projectiles1`).
