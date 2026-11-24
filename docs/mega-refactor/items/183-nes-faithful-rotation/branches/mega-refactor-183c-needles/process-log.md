# Process log: mega-refactor-183c-needles

## Status

- State: merged
- Goal: `Needle` draws 32 directions from drawn frames, with turns only in multiples of 90 (or a mirror)
- Done: planning (scope approved 2026-11-24 with `/go-with-recs`: D1 a, D2 a, D3 a, D4 a); steps 2, 4, 5, 6; review before the play test; play test; diff review
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-24

- Branch started from `master` at `a5e61cdb0`. Merges directly into `master` (item decision, 2026-11-23).
- Small branch: no HTML brief, no `PLAN.md`; steps in `SCOPE.md`. The owner accepted the recommendations with `/go-with-recs` (D1 to D4 in `SCOPE.md`).
- Owner confirmed the Fable use, and named the source of the straight needle frames (the Megaman spritesheet authors).
- Step 2: one Fable art agent drew `default_1` to `default_4` (the needle axis through the turn center (8, 8); frame 4 equal to its own diagonal mirror; `default` not shifted) and made the ice frames by color map (`FCA044` to `A4E4FC`). Files, `check.py`, previews and notes in `temp/mega-refactor-183c-needles/art/` (main checkout). The agent notes that frame 2 (22.5 degrees) reads as a triangle with one upright side, which is the true shape of an 8x7 needle at that angle.
- Step 4: frames copied as `*_step<n>.png` (TexturePacker reads a `_<n>` suffix as an animation index), `Projectiles2` repacked with its `.tps`. `compare_atlases.py`: 44 old regions keep name and pixels; `Needle/ice` changes (intended, D2); 8 new regions.
- Step 5: `NeedleDrawDirection` and `NeedleTest` (32 steps x 3 angle offsets for the rule, 32 flown needles for the sprite flip and turn, 1 velocity case). The first version read the frame through the sprite region; it failed because `DesertManNeedleSpinTest` loads the static regions first.
- Mutation check (1 run): the mirrored turn without `+ 90`: 24 cases failed (the 12 mirrored steps in both describes). Code restored.
- Checks: `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar`, `detektMain`, `core:verifyRandomRule` pass.
- Step 6: README credit under "Megaman".
- Step 7: review before the play test, 1 Sonnet agent. No bug. Notes: (1) a zero velocity draws frame 0 turned 270, as on `master`; (2) `NeedleDrawDirection` in `Needle.kt` as the scope says. `DESERT_MAN` has no gravity change, so the turned-gravity part of check 2 is its own check in `REFACTOR_TEST_183` `gravity_left` (agent's proposal). Checks reordered from the simplest setup; the boss check adds the tanks. Play scripts `temp/play/play-1.sh` to `play-5.sh`.
- Play test: checks 1 to 5 pass (owner, 2026-11-24). Diff review: no threads.
- Merge: `master` changed since the branch start by docs only (`178b` branch start); no merge of `master` into the branch. Active branches `078f` and `178b` touch no file of this branch. Squash-merged into `master`.
