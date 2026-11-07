# Contract: mega-refactor-081d-x-dash-sabre

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D4)

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] X tests: the ground dash (start delay, speed curve, end, cancel), the dash jump and the dash wall kick keep the
  dash air speed and give no air dash, the faster `SELECT` air dash, the knockback distance and the drag ending with the
  damage; no dash on `DOWN` + `A`.
- [ ] Megaman's tests pass unchanged (Megaman's `JumpBehavior.kt` passes his old rules for the 3 new `JumpParams` fields).
- [ ] `BlockSideRuleTest`: the seam keeps the sense; the last end and a kept block that no longer overlaps clear it.
- [ ] Mutation checks; the review before the play test. No game launch.

## Manual playthrough

Experimental checks first. From `.worktrees/mega-refactor-081d-x-dash-sabre`.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_081 --room ground_dash` | Dash on `SELECT` (`DOWN` + `A` does nothing; `SELECT` never pauses for X); the end slide; the dash jump over the 6-tile pit (a plain jump falls short, but a plain jump plus an air dash also crosses); no air dash after a dash jump; the faster `SELECT` air dash; dash into the ledge and off it. |
| `REFACTOR_TEST_081 --room walls` | The dash wall kick keeps the dash speed. |
| `REFACTOR_TEST_081 --room walls` | The 081d feel fixes: the kick shows the kick pose from its first frame, with no facing snap, and the pose stays while the kick rises; a slide down the first wall (2 blocks, seam 3 tiles above the floor) has no 1-frame flicker of the slide pose at the seam; after a kick or a push-off, X moves at the walk speed (no speed jump when control returns); the jump top has no extra hang. |
| `REFACTOR_TEST_081 --room ground_dash` | The brake pose at the end slide (also with `B`); a jump or a dash press on the landing frame is not lost (chain dash jumps); a jump with `DOWN` held; a turn and a walk start act on the press frame; no stop when the air dash ends with a side held. |
| `REFACTOR_TEST_081 --room targets` | Take a hit from the `Junkroid`: X is pushed back a short way. |
| `X6_INTRO --room room1` | The owner's tuning on the route to the gate: the step at tile 20 (a kick), the opening at the face (1 kick), the ladder top (walk over it with `DOWN` held; a dash skips the hole), the sabre block at tile 88 (jump and shoot), the 10-tile climb and its opening, the 14-tile climb. |
| `GLACIER_MAN --room room1` (control) | Megaman: knockback, ground slide, air dash as before. |
| `WILY_STAGE_2 --room room9` (control) | Megaman: jump at the left wall and wall slide down past the block seam 3 tiles above the floor: the slide does not break at the seam (the shared seam fix). Do not step into the floor gap: it is the drop to the boss room. |

## Intended changes

- 081d feel fixes (owner, 2026-11-06; list in `docs/NOTES.md`, "Decisions"): the X-only fixes, the walk speed for the
  kick-away and the push-off, the X6 jump top, the brake pose.
- Shared: the side sense of every body with side fixtures (Megaman, enemies) no longer drops for 1 frame at a seam
  between 2 blocks (`BlockSideRule`).
- Megaman: no change. `JumpParams` has 3 new fields; Megaman passes his old rules.

- The owner's X tuning (`a15fa6184`).
- X: the ground dash, the dash jump, the dash wall kick, the faster `SELECT` air dash, about 1.3 tiles of knockback.
