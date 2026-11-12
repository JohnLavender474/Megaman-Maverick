# Plan: mega-refactor-081f-x6-intro-fixes

How to do the work in `SCOPE.md`. Write this file after the owner approves the brief. It needs
no approval. When the plan changes, write the reason in the process log.

## Steps

### 1. Boss room music fade

- Changes: `fade_out_music=true` (bool) on the `boss_room` object of `X6Intro_16x14_v1.tmx`.
- Check: the TMX parses (a test that loads the map properties, or the review before the play test).
- Done: yes

### 2. X's animation in a room transition

- Changes: `MegamanXSprites.kt`: `animationKey(priorKey)` holds the prior key while `roomTransitionPaused` and, during
  `roomTransition`, maps `stand`, `land_shoot`, `run_trans`, `run_trans_shoot`, `dash_end`, `dash_end_shoot` to `run`;
  the animator passes the prior key and does not advance during the pause.
- Check: `MegamanXTest` cases for each mapped key, an air key that stays, and the pause hold; a mutation check.
- Done: yes

### 3. `D1000` body at spawn

- Changes: `D1000.kt`: one `placeBody()`, called in `onSpawn` after `origin.set(...)` and in the pre-process.
- Check: `D1000Test`: after spawn, the body center is `origin + BODY_CENTER_ABOVE_ORIGIN`; a mutation check.
- Done: yes

### 4. `D1000` landing and walk

- Changes: `CROUCH_HEIGHT = 1.375f * PPM`, `HALF_WIDTH = 1.75f * PPM`, `facePlayer()` on each walk frame.
- Check: `D1000Test` cases for the landing height, the walk range at the wall and the turn mid-walk; mutation checks.
- Done: yes

### 5. Damage of `D1000` to X

- Changes: `PlayerDamageNegotiator` `D1000` 3 and `D1000Bomb` 3; `D1000Pillar.DAMAGE` 3; `JunkShot.Type.CORE` 2.
- Check: a test of the negotiator values through the real damage path; a mutation check.
- Done: yes

### 6. Tidy-up (D1)

- Changes: the 5 `TODO`s name the items `137` to `141`; the 2 `docs/x6-intro/` plans use the new names.
- Check: grep shows no `X6Shot`, `X6Scenery`, `X6Debris`, `X6Frames` in those docs.
- Done: yes

### 7. Checks and review before the play test

- Changes: none.
- Check: `engine:test core:test core:smokeTest lwjgl3:jar`; one review subagent over the diff and the 3 manual checks
  (no game launch); then the play checks and the Diffity review.
- Done: yes (review); the play test is next

## D-1000 attacks (owner request, 2026-11-09)

The owner chose new attacks, an escalation phase, an adaptive director and eyeball attacks for D-1000. Deviations
from X6 are allowed; each one is a `CUSTOM` row in `NOTES.md`. Spec: the 3 design reports in
`temp/081f-attacks/` (main checkout). New positions and sizes come from the named body constants or the arena, so
that the later 1.25x art swap changes only those constants.

New APIs and their first production caller:

| API | Caller |
|---|---|
| `D1000Director.chooseAction(D1000Context): D1000Plan` | `D1000.updateThink` |
| `D1000Core.summon(D1000EyeAttack)`, `spot()`, `daze(Vector2)`, `wantOpen` | `D1000` (SUMMON, SPOT, RAM, each update) |
| `D1000.onCoreMark(x)`, `D1000.overdrive` | `D1000Core` (SPOT marks, SPREAD second fan) |
| `D1000Shockwave` | `D1000` (crouch and ceiling landings in Overdrive) |

### 8. Director and context

- Changes: new `D1000Director.kt`: `D1000Context` (roll, tier, last 2 actions, distance band, X on a wall, X above
  the body, X near the eye, plans since the last combo) and `D1000Plan` (action and follow-up). Weights per tier in
  `D1000Action`; context changes the weights; no action 3 times in a row; combos. `D1000.updateThink` builds the
  context; the end of each action starts the follow-up; a stun clears it. The slam rule stays.
- Check: `D1000Test` director cases; a fight test that a combo follow-up starts and a stun clears it; mutation checks.
- Done: yes

### 9. Bomb patterns and the Pillar Fence

- Changes: `throwBomb(targetX)`; `D1000Bomb` lands at its target from any height; BRACKET (3 bombs), SWEEP (4 bombs,
  Overdrive), FENCE (rise to the middle top, 5 lanes toward X's wall; Hard adds 4 shifted lanes).
- Check: fight tests for the bomb count and landing x of each pattern; mutation checks.
- Done: yes

### 10. Ram additions

- Changes: Wall Crash (4 shootable junk pieces near the hit wall), Dazed Eye (`D1000Core.daze`), Ricochet Ram
  (Overdrive: re-aim, ram to the other wall).
- Check: fight tests for the debris count, the eye position, and both wall hits; mutation checks.
- Done: yes

### 11. Ceiling Crash

- Changes: new state CEILING: rise to the top, junk lanes at X, X ± 2.5 (Overdrive also X ± 5), track X, jitter,
  drop, crouch hold.
- Check: a fight test for the rise, the debris lanes and the landing; mutation checks.
- Done: yes

### 12. Shockwave

- Changes: new hazard `D1000Shockwave` (floor, then up the wall, 3 damage, `pillar_base` art rotated on the wall);
  2 waves at each crouch landing in Overdrive.
- Check: a test that drives the wave along the floor and up the wall; a fight test for the 2 waves; mutation checks.
- Done: yes

### 13. Eyeball rules, attacks and contact damage

- Changes: `D1000Core`: home anchor and glide; Peekaboo (open only while the body commits, 0.3 s flicker); SPOT
  (rail, track, 3 marks); SPREAD and DIVE; a DAMAGER fixture and a `PlayerDamageNegotiator` entry (2).
- Check: fight tests for each; mutation checks.
- Done: yes

### 14. Overdrive

- Changes: ENRAGE state (1.2 s) at the first THINK at half HP or less; then the Overdrive values and attacks.
- Check: a fight test for the rage and the faster values; mutation checks.
- Done: yes

### 15. Docs and checks

- Changes: `NOTES.md` CUSTOM rows, `docs/x6-intro/d-1000-plan.md`, the process log.
- Check: `engine:test core:test core:smokeTest lwjgl3:jar`.
- Done: yes

### 16. Hit boxes from the art (owner request, 2026-11-09)

- Changes: `D1000Frames` gets one box table per drawn frame (`BODY_BOXES`, `CORE_BOXES`, `EYE_BOXES`, in art px from
  the top-left of the frame). `TickFrames.artFrame` and `TickFrames.placeBox` find the drawn frame and place a box
  (mirrored by the facing). `TickFrameFixtures` keeps one fixture set of one type and moves it each update, with no
  allocation. `D1000` places BODY, DAMAGER and DAMAGEABLE sets, and `D1000Core` places a DAMAGEABLE (eye) and a
  DAMAGER (ball) set, in the body pre-process. The abstract body, the attack logic, the timings and the damage values
  do not change.
- New APIs and their callers: `TickFrames.artFrame`, `TickFrames.placeBox` and `TickFrames.maxBoxes` (`D1000`,
  `D1000Core`, `TickFrameFixtures`); `TickFrameFixtures` (`D1000`, `D1000Core`).
- Check: `D1000FixturesTest` (11 cases): the tables against the PNG art; the boxes follow the drawn frame through a
  fight, facing left and right; the throw boxes move at throw frame 2; the notch over the head is safe and the shell
  is not; the damager is off in the stun and the entrance; the eye box in the `core` frame and the ball boxes in the
  flash; a full charge through the eye only while it is open. 8 mutation checks.
- Done: yes
