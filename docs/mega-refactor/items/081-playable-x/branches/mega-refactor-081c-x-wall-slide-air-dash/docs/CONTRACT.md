# Contract: mega-refactor-081c-x-wall-slide-air-dash

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D4)

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] Baseline tests on Megaman's wall slide, air dash and `aButtonTask`, written on the old code, pass unchanged
  after the change.
- [ ] New X tests: the grab time, the slide speed, the kick speeds and the kick lock, the air dash speed, length and
  1 per air (a new one after a landing or a wall grab), the shared behaviors on a test player.
- [ ] Mutation checks: each new rule broken once makes at least 1 test fail.
- [ ] The review before the play test finds that every manual check runs as written. No game launch.

## Manual playthrough

Experimental checks first. From `.worktrees/mega-refactor-081c-x-wall-slide-air-dash`.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_081 --room walls` | Grab a wall, then slide. Kick up the 9-tile wall (5 kicks) and the chimney. Release the wall: X pushes off. The 2.5-tile step: 1 jump and 1 kick. The opening at the top of the 4-tile face: the first kick falls a little short; kick again, then slide down the upper face with right held and X drops into the opening. The blue `no_side_touchie` wall: from the top of the chimney, jump right into it with right held and press `A`: no grab and no kick. |
| `REFACTOR_TEST_081 --room dash` | Air dash over the 3, 4 and 5-tile pits with `A`, with `U` (the dash button, also while rising), and with `SELECT` when the select action is air dash. Start the dash near the top of the jump for the 5-tile pit. 1 per air; a new one after a wall grab (dash into the tunnel block's left face, hold right to grab, release, dash left). |
| Controls screen (main menu, keyboard settings) | The new `DASH` row shows `U`; it can be remapped. |
| `X6_INTRO --room room1` | From the start to the gate: the step at tile 20 (jump and kick), the face at tile 50 (2 kicks, then slide into the opening), shoot the sabre block and hold `DOWN` on the ladder top, shoot the next sabre blocks, the 10-tile climb (enter the gap at its top from a kick at 8 to 10 tiles), the 14-tile climb (kick and hold toward the ledges at 5 tiles), the gate. |
| `GLACIER_MAN --room room1 --weapons RUSH_JET,RODENT_CLAWS,NEEDLE_SPIN` (control) | Megaman: wall slide, wall jump, air dash with `A`, and with `SELECT` set to air dash; the air dash in water; the Rush Jet dash; the claws stop on a wall. |

## Behaviors to check

- Megaman on every other level: no change.

## Intended changes

- X gets the wall slide, the wall kick and the air dash. Megaman does not change.
- A new controller button `DASH` (keyboard `U`, pad `R1`) in the controls screen. Only X reads it; Megaman ignores it.
