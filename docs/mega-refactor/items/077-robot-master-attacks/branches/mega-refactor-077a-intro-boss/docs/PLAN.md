# Plan: mega-refactor-077a-intro-boss

How to do the work in `SCOPE.md`. Written 2026-11-13 after the owner approved the brief (D1 to D4). It needs no
approval. When the plan changes, write the reason in the process log. The designs are in `NOTES.md`.

## Steps

### 1. Health phases on `AbstractBoss`

- Changes: `healthPhases` (default 1), `healthPhase`, `onHealthPhaseStart(phase)`, the `translateHealth` override
  (roll over at 0 in a phase that is not the last, carry the extra damage), `getHealthRatio` over the whole fight,
  reset to phase 1 at spawn. Defeat only at 0 in the last phase.
- Check: new unit test (carry-over, exact 0, last phase, ratio, hook called once per phase, 1-phase boss unchanged);
  `core:test`.
- Done: yes

### 2. Phase-2 bit art

- Changes: one new 8x2 red-orange region in `assets/sprites/frames/Bits/`, packed into `Bits.png` and `Bits.txt`
  (keep the PNG format of the file; D1).
- Check: the region loads in a test or smoke test; the PNG size stays near the old size.
- Done: yes

### 3. `BossBitsBar` and `BossHealthHandler`

- Changes: new `BossBitsBar` (2 runs per frame, pure run math); `BossHealthHandler.set` takes `AbstractBoss`; the
  intro fill shows the phase-2 color from the start and fills the phase-1 bits over it in 2.25 s (D2). A 1-phase
  boss draws as today. D2 was replaced by the owner decision of 2026-11-15: the fill runs in passes, one per phase,
  last phase first.
- Check: unit test of the run math; `FightStepsTest` still passes.
- Done: yes

### 4. Damage table of `BigAssMaverickRobot`

- Changes: pellet 2, half charge 3, full charge 4, the charged-shot explosion values from `SCOPE.md`, and the special
  weapon table (D3). Proto Man rows stay with `060c` (note for the second merge).
- Check: unit test of the damage map per damager class.
- Done: yes

### 5. Boss package: tuning rows and motion slots

- Changes: `entities/bosses/bigassmaverickrobot/` (or the package name in `SCOPE.md`): the tuning rows for
  normal and hard, phase 1 and phase 2; the ORBIT slot (today's circle), the HOVER slot (eased drift, out of step),
  BLEND; a package `README.md`.
- Check: unit tests of the slot functions (orbit equals today's path; hover stays within its bounds).
- Done: yes

### 6. Hand state machine and the boss's attack choice

- Changes: hand states from `NOTES.md` (IDLE by mode; punch WIND_UP, LAUNCH, RECOIL, RETURN; slam RISE, HANG, FALL,
  REST, RETURN; clap SPREAD, HOLD, CLOSE, IMPACT, RETURN; DEFEATED); the boss orders attacks, picks the target player
  and the free hand (never a hand that a player stands on); the platform follows the hand in every state.
  `BigAssMaverickRobotHandTest` moves to the new spawn props.
- Check: unit tests of the transitions and the "no attack with a ridden hand" rule; `core:test`, `core:smokeTest`.
- Done: yes

### 7. Mixed-aim orbs

- Changes: an orb takes its aim when it starts to move, from the boss (lead, current, near, wide, per the tuning
  row). Other orb users keep the old path.
- Check: unit test of the aim function (lead point from position and velocity; the kinds per volley).
- Done: yes

### 8. Slam shock waves

- Changes: the slam spawns `BigAssMaverickRobotOrbExplosion` shock waves along the floor; fix the `ElectricPulse`
  frame count (2 frames, not 3).
- Check: `core:smokeTest`; unit test of the spawn positions if practical.
- Done: yes

### 9. Phase 2 start

- Changes: `onHealthPhaseStart(2)`: camera shake, sound, head flash, orange arm orbs; the next decision reads the
  phase-2 row; the next cycle end reads the phase-2 mode schedule.
- Check: unit test that the hook switches the tuning row.
- Done: yes

### 10. Richer death

- Changes: the death timeline in `NOTES.md` in place of the X-eyes; the body stays on screen (on purpose).
- Check: `core:test`, `core:smokeTest`.
- Done: yes

### 11. Automated checks and the review before the play test

- Changes: `entity-tags.txt` if a class or tag changed; fixes from the review.
- Check: `engine:test core:test core:smokeTest lwjgl3:jar`; review subagents on the diff and each manual check in
  `CONTRACT.md`; commit; Diffity.
- Done: yes (commit `d5fdab1a9`, Diffity open)

### 12. Aimed slam and perimeter shock waves (owner, 2026-11-14, from the play test)

- Changes: `NOTES.md` "Aimed slam and perimeter shock waves": the slam aim function (5 directions, start point, hit
  point by ray to `arena`); the hand slam rows move along `d` and rest on a floor or a wall; `ShockWaveRow` walks the
  open perimeter and turns at the corners; a wall pillar is drawn and hit-boxed at 90 degrees.
- Check: unit tests of the aim function and the perimeter path (each with a mutation check); the fight tests for a
  floor hit and a wall hit; `engine:test core:test core:smokeTest lwjgl3:jar`; a review subagent on the new diff and
  the changed manual check; the owner plays check 1 again.
- Wait: no Gradle in the worktree until the owner ends the current play test.
- Done: yes (code, tests, mutation checks and automated checks, 2026-11-14; uncommitted while the Diffity round is
  open). Still to do: the review subagent on the new diff and the changed manual check; the owner plays check 1 again

### 13. Pincer clap (owner, 2026-11-14, from the play test)

- Changes: `NOTES.md` "Pincer clap": replace the double clap with a clap that closes on the player position, twice;
  remove the mirror hand and the clap hold rows; add the close speed to the phase-2 rows; one pure function for the
  meet and start points.
- Check: unit test of the function (mutation check); fight tests for both claps (the second aims again, the hands
  meet at the meet point, the damage on contact); `engine:test core:test core:smokeTest lwjgl3:jar`; a review
  subagent on the diff; the owner plays check 1 again.
- Done: yes (code and the `lwjgl3:jar` build, 2026-11-14; uncommitted). Tests deferred until the moveset is final
  (owner, 2026-11-14): `ClapAimTest` and 3 fight tests are written and not run, no mutation checks. Still open: the
  review subagent on the diff; the owner plays check 1 again.

### 14. Final pass (owner: moveset final, "ready to merge", 2026-11-15)

- Changes: merge `master` (with `060c`) into the branch; the `ProtoBuster` and sabre rows of the boss damage map;
  each line of `docs/TEST-DEBT.md` made into a test, and the stale tests fixed; `BossHealthHandler` gets 2 pure fill
  functions so that a test can read the fill order (no change of behavior).
- Check: one mutation check per new or changed rule test; `engine:test core:test core:smokeTest lwjgl3:jar`.
- Done: yes (2026-11-15; process log "final pass").

