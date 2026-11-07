# Notes: mega-refactor-081d-x-dash-sabre

## Decisions

- D1 (owner, 2026-11-06): the sabre goes to its own branch `081e`, after this one.
- D2 (owner, 2026-11-06, for `081e`): the sabre slashes on `UP` + `B` (open question 11). No new button.
- D3 (owner, 2026-11-06): X's knockback like Megaman's: 8 / 5 t/s and a 6/s side drag while damaged (about 1.3 tiles).
- D4 (owner, 2026-11-06): the ground dash starts on `R` and on `DOWN` + `A` on the ground (open question 9). The dash
  jump works from either; the dash wall kick and the faster air dash stay on `R`.
- Owner (2026-11-06, at the play test): D4 reversed. "Proper X controls are incompatible with the NES controller":
  the ground dash starts only on `R`; `DOWN` + `A` is removed (and with it `JumpParams.downBlocksGroundJump`, added
  for it the same day). A jump during the dash time (from the dash start until the end slide) is a dash jump. Open
  question 9 of `081` is closed: `R` only.
- Owner (2026-11-06): X's movement logic must match `mmx-x6` (values may differ). A review (`temp/081-x6-review/review.md`)
  found 8 differences and 5 smaller ones; the owner chose to match X6 in all of them, also where Maverick was more
  responsive:
  - wall: a grab while rising after the wall delay; the grab finishes its time without input; the push-off comes only
    from the slide; a ceiling or a wall during the kick lock ends the kick; a kick without `R` gives the air dash
    back; no air dash during a push-off;
  - ground dash: the opposite side starts the end slide (no turn); the forward side does not cut it; `R` in the end
    slide starts a new dash; a dash off a ledge uses up the air dash; the dash end with a side held goes into the walk
    start;
  - landing with a side held starts the walk start again;
  - the `R` air dash starts at 12.5 t/s and slows at 21.1 t/s^2 to 8 t/s (this replaces the earlier flat speed).
  Kept: a jump in the dash start delay is a dash jump (owner, earlier); any block side is a wall (X6 has non-cling
  walls; no X map needs them yet); the facing during the grab (looks only).
- Owner (2026-11-06, at the play test): "make it so that SELECT is ALWAYS dash when playing as X", with a `TODO`
  that records the many changes of the dash button and that no adequate solution exists yet. Done: X's dash
  (ground dash, dash jump, dash wall kick, the fast air dash) reads `SELECT`, whatever the select action is; `A` in the
  air still starts the plain air dash (X6). The `R` button of `081c` had no reader left, so it is removed
  (`MegaControllerButton`, the pad default); the generic parts of `081c` stay (the controls screen layout follows the
  button count; the old-save clash rule). `SELECT` never pauses the game for X (`MegaLevelScreen`, an `is MegamanX`
  workaround with a `TODO`). At the merge: `CLAUDE.md` (root), `069` and `081` `ITEM.md` must drop the `R` button.
- Claude's calls: the `R` air dash gets a flat faster start (no decel), so Megaman's air dash code does not change; the
  ground dash has its own `BehaviorType.GROUND_DASHING` (`GroundSlideBlocker` assumes Megaman's shrunk slide body).
- Owner (2026-11-06, "Apply all", after the play test reports of a slow dash, a facing snap on the dash kick, and
  less fluid controls; investigations in `temp/081d-investigate/`): X-only fixes, each from X6 (`mmx-x6`
  `Player.java`, the lines in `temp/081d-investigate/fluidity.md`):
  - the kick frame shows the kick pose, not 1 frame of the wall slide pose with the wall flip;
  - a jump or dash press on the landing frame is kept for 1 frame (X6 `toLand()` tries the ground moves in the landing
    update); `JumpParams.startPressed`, Megaman passes `isJustPressed(A)`;
  - X jumps with `DOWN` held on the ground (X6 crouch jump); `JumpParams.downBlocksGroundJump`, Megaman `true`;
  - a side press moves X on its press frame, and a turn acts on the press frame;
  - no 1-frame side stop when the air dash or the wall slide ends with a side held;
  - after the kick lock, the kick pose stays while the kick rises (X6 keeps animation 14 until `speedY <= 0`);
  - the ground dash end slide shows the brake pose `dash_end` (X6 animation 17: 4 frames of 0.05, 0.0333, 0.1167 and
    0.0833 s; `DASH_END_TIME` is their sum).
- Owner (2026-11-06, "Fix in 081d"): the side sense of a body no longer drops for 1 frame at a seam between 2 blocks
  of a wall. Cause: the world calls the contact ends after the begins and continues, so the end of the old block
  cleared the sense that the new block had just set. `BlockSideRule` keeps the touched block fixtures of each side
  fixture and clears the sense only when none is left. This is shared: it applies to Megaman and to every entity with
  side fixtures. `WorldSystem.reset()` frees contacts with no end call, so the end also drops kept blocks that no
  longer overlap the side fixture.
- Owner (2026-11-06, "Yes to both"): the kick-away and push-off speeds are the walk speed (X6 uses one speed for
  walk, air control, kick-away and push-off); X gets the X6 jump top: the jump end keeps a falling speed
  (`speedY = min(speedY, 0)`), so no extra hang frame at the apex. `JumpParams.endKeepsFall`, Megaman `false`.
- Brake art: the owner drew `dash_end.png` (96 x 96, 2 x 2 frames, row by row) from the X6 frames; Claude drew
  `dash_end_shoot.png` from it. Both are in `assets/sprites/frames/MegamanX/` and are not made by the slicer data
  file (it has a comment). The atlas was packed again with `TexturePacker` from every strip in the frames folder, with
  the slicer settings and `filter: Nearest, Nearest`. The 39 old regions have the same pixels as before. The
  `.piskel` source stays in the main checkout, not committed.
- Owner (2026-11-07, "Ground move", review finding): after a plain jump, an `A` or `SELECT` press on the landing
  frame gives the ground move, as in X6 (the landing update tries the ground moves). Before an `A` or `SELECT` air
  dash starts, X probes 1.5 nominal frames below the feet for a block; if one is there, the press waits 1 frame for
  the landing; with no landing, the air dash starts 1 frame later.

## Overlap with other branches
