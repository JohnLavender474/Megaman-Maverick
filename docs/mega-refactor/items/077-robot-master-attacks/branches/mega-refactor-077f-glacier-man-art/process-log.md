# Process log: mega-refactor-077f-glacier-man-art

## Status

- State: active
- Goal: new Glacier Man art for the `077g` fight: chunk and aim throws, the ground pound, icicles that form on the ceiling, the freeze-and-shatter death
- Done: branch start, scope, brief (https://claude.ai/artifact/Aj2pXdJgFijjvtRvDHRtjS), approved 2026-11-19;
  implementation `PLAN.md` steps 1 to 7 (art accepted, atlases packed, credits)
- Next: merge (no code, so no review before a play test and no play checks; the owner reviewed the art)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch start. Owner chose "art first" for the Glacier Man upgrade: this branch draws the frames, then `077g` codes
  the fight on the final art. The fight design is the draft `SCOPE.md` of `077g`.
- Scan, `SCOPE.md`, brief. Owner answers: D1 Fable for the art agent, D2 the frame list as listed. `PLAN.md` written.
- Step 1: contact sheets `glacier-man-frames.png`, `ice-props.png`, `glacier-man-palette.png` (+ `palette.txt`) in
  `temp/mega-refactor-077f-glacier-man-art/` (main checkout), script `contact_sheets.py`. Glacier Man palette: 6
  colors (`#000000`, `#fcfcfc`, `#3cbcfc`, `#ffffff`; `#fea22a` and `#b21030` only in `fired`).
- Steps 2 to 4: one Fable art agent for all three groups (owner D1), previews to the same temp folder.
- Steps 2 to 4, round 1 (Fable agent): 10 strips in `assets/sprites/frames/` (no frame count changed), previews
  `new-frames.png`, `strip-*.gif`, `mock-{a,b,c}-*.gif`, round notes `round-1-notes.md`, final frame table in
  `docs/NOTES.md`. Agent doubts for the owner: chunk color (cyan in flight, blue in his hands), side-to-front change
  in `heave` with no turn frame, `frozen_shatter` cut by rule.
- Owner round 1 review: the big-chunk start of Icicle Rain "looks off"; Glacier Man pounds the ground with his fist
  in its place (new strip `pound`). `heave` and `GlacierIceChunk` dropped (moved to `temp/.../dropped-round-1/`).
  Other round 1 strips accepted. `SCOPE.md`, `NOTES.md`, `PLAN.md` and the `077g` draft `SCOPE.md` updated.
- Round 2: a new Fable art agent for `pound` and the Icicle Rain mock-up.
- Round 2 (Fable agent): `pound` 5 frames, impact frame 2; previews `strip-pound.gif`, `mock-b2-icicle-rain.gif`,
  updated `new-frames.png`; notes `round-2-notes.md`. Agent doubts: the impact burst is inside the boss strip; the
  right spike overlaps the front leg; mock-up shake and icicle delay are start values for `077g`.
- Owner round 2 review of `pound`: arms too long ("long noodles") from the wind-up; arms blue in place of black as in
  his other strips; bend the knees more for the pound. Round 3: a new Fable art agent.
- Round 3 (Fable agent): `pound` redrawn (5 frames, impact frame 2): original arm length, black arms, deeper crouch
  from the `sled` pose, right spike removed. Previews `check-pound-r3.png`, `strip-pound.gif`,
  `mock-b2-icicle-rain.gif`; notes `round-3-notes.md`. Round 2 strip kept as `strips/.../pound-r2.png` in temp.
- Owner round 3 review: arms "still too long and noodly". Round 4: a new Fable agent; arms built only from the
  artist's own `stand`/`duck` arm pixels (moved, not stretched), the reach to the floor from the body.
- Round 4 (Fable agent): `pound` arm is the `stand` front-arm block (7 × 7), only moved, flipped or turned; hand
  6.4 px from the shoulder in every frame (as `stand`); deep bow at impact, hand 2 px above the floor. Open hand,
  not a fist. Previews `check-pound-r4.png`, `strip-pound.gif`, `mock-b2-icicle-rain.gif`; notes `round-4-notes.md`.
- Owner round 4 review: "better"; the punching arm is too skinny; a closed fist when he hits the ground; a light ice
  "shatter" effect in the strip at the impact (the floor is always ice). Round 5: a new Fable agent.
- Round 5 (Fable agent): `pound` arm 5 to 6 px wide (same length); new closed fist (7 × 6, 2 knuckle lines) in
  frames 0 to 3, on the floor line at impact; light ice shatter (crack flash, chips) in frames 2 to 4; round 4
  spikes, snow mounds and sparks removed. Previews `check-pound-r5.png`, `strip-pound.gif`, `mock-b2-icicle-rain.gif`;
  notes `round-5-notes.md`.
- Owner round 5 review: "almost there"; at the impact the fist on the ground shows no arm, so it reads as a
  detached fist. Round 6: a new Fable agent for frames 2 and 3 only.
- Round 6 (Fable agent): frames 2 and 3 only: bow 1 px less deep, fist 2 px forward on the floor line, a 6 px wide
  black arm over 6 rows from the chest to the fist; shatter moved with the fist. Previews `check-pound-r6.png`,
  `strip-pound.gif`, `mock-b2-icicle-rain.gif`; notes `round-6-notes.md`.
- Owner round 6 review: "close"; fist 1 px narrower and 1 px closer to him; arm straight (the bend is awkward).
  Round 7: a new Fable agent.
- Round 7 (Fable agent): frames 2 and 3: fist 6 × 6 (2 fingers), 1 px closer to him; arm one straight 6 px black
  block. Wind-up fist (frames 0 and 1) unchanged, 1 px wider. Previews `check-pound-r7.png`, `strip-pound.gif`;
  notes `round-7-notes.md`.
- Owner: go back to round 6 `pound` and move forward with it. Round 6 strip restored in `assets/`; round 7 kept as
  `strips/Bosses1/GlacierMan/pound-r7.png` in temp. Step 5 done: all strips accepted.
- Step 6: `Bosses1` and `Projectiles2` repacked with `Basic` (the `.tps` projects). Script
  `temp/.../pack/check_repack.py`: `Bosses1` 69 old regions and `Projectiles2` 44 old regions keep their names and
  pixels; new regions `GlacierMan/aim`, `aim_up`, `chunk`, `freeze`, `frozen_crack`, `frozen_shatter`, `pound`,
  `FallingIcicle/form`, with the frame counts of `docs/NOTES.md`. `Bosses1-0.png` grew from 1796 × 1436 to
  1894 × 1436 (under the 2048 limit).
- Step 7: README `CREDITS` Glacier Man entry (original and new frames). `./gradlew core:smokeTest lwjgl3:jar`
  passed (smoke tests 475, 0 failures).
