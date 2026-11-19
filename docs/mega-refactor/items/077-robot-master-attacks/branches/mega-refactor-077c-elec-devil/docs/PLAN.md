# Plan: mega-refactor-077c-elec-devil

Fast iteration rounds as in `077b`: the play tests decide the tuning. Art drafts run in parallel with the code.

## Steps

### 1. Art drafts (agents, parallel)

- Changes: drafts in `temp/077c-elec-devil/art/` (main checkout): body polish, eye telegraph, claw, assemble/split,
  death, arc, floor current, bolt.
- Check: the owner picks and fixes; then the frames go into `assets/sprites/frames/Bosses3/ElecDevil/` and the atlas.
- Done: yes (2026-11-19; all ElecDevil art in `Bosses4`)

### 2. Test map

- Changes: `RefactorTest077.tmx` with an `ElecDevil` room (copy of `boss_room` 429 and its objects) and 2
  `serpent_paths` polylines; `LevelDefinition.REFACTOR_TEST_077`. Same polylines in `WilyStage1_v2.tmx`.
- Check: the review before the play test (no launch).
- Done: yes (2026-11-18)

### 3. Phases, tuning rows, damage tables, fight rig

- Changes: `healthPhases = 2`, `onHealthPhaseStart`, `ElecDevilTuning`, 2 × damage table,
  `PlayerDamageNegotiator` entries; the `Loop` becomes a cycle with a phase-2 pick; a test rig with a scripted
  `GameRandom`.
- Check: tests for the phase start and the pick per phase.
- Done: yes (2026-11-18; play-tested in rounds 1 and 2)

### 4. Eye Spread and Live Wire

- Changes: mixed-aim `ElecBall`s with the eye telegraph; arcs between consecutive flying pieces with gaps.
- Check: aim tests; play-test round 1.
- Done: yes (2026-11-18; play-tested in rounds 1 and 2)

### 4b. Eye patterns and Tether Claw (owner, 2026-11-19)

- Changes: the colour pattern deck for the eye (Fan, Ring, Stall, Bounce) with eye and orb colour strips; the
  Tether Claw in `HAND` (windup with a guide line, straight out along the slope, hold, back, recoil) as the entity
  `ElecDevilClaw` with a damaging Live Wire tether (redesign 2026-11-19);
  claw and colour art by an art agent.
- Check: play-test round 2.
- Done: yes (2026-11-19; owner: looks good, ready to merge)

### 5. Spark Serpent

- Changes: the chain of cells on a `serpent_paths` polyline, arcs, the head as the weak point, the reform on the far
  side; a `TODO` at the cube contact code for the backlog item.
- Check: path-read and chain tests; play-test round 2.
- Done: moved out (owner, 2026-11-19): a new branch of item 077 after the 077c merge

### 6. Circuit Floor and Storm Rain

- Changes: embedded pieces with a lit tell and a sequenced current; bolts with the 4 mixed aims and lit target spots.
- Check: aim tests; play-test round 3.
- Done: moved out (owner, 2026-11-19): with step 5

### 7. Art into the game, credits

- Changes: the approved frames, the atlas, the animations; the `README.md` credit lines.
- Check: `core:smokeTest`; play-test round 4.
- Done: no (credits only: `README.md` Elec Devil entry, original by boberatu)

### 8. Review before the play test, diff review, final test pass

- Changes: as in `docs/mega-refactor/CLAUDE.md`.
- Check: all automated checks; the owner's play test and Diffity review.
- Done: no (owner skips the Diffity review, 2026-11-19)
