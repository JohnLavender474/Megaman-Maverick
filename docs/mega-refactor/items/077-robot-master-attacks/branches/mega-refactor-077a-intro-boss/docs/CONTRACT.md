# Contract: mega-refactor-077a-intro-boss

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. Do
the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-13 (owner, brief D1 to D4)

The values below are those of the play-test rounds of 2026-11-14 and 2026-11-15 (`process-log.md`, and the code in
`BigAssMaverickRobotTuning.kt`). The first drafts are in `NOTES.md` ("Tuning rows"). Q1 to Q4 in `SCOPE.md` can
change this file.

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes, with these new tests:
  - [x] Health phases (through the public damage path, with a real projectile and `testServices()`): a 2-phase boss
    that takes 30 damage is in phase 2 with 30 health, not `defeated`, and no `BOSS_DEFEATED` was submitted; health 2
    and a hit of 3 give phase 2 with 29; `onHealthPhaseStart` runs once per new phase; 0 in the last phase gives
    `defeated` and one `BOSS_DEFEATED`; a 1-phase boss behaves as today; the whole-fight `getHealthRatio` at each
    phase edge; a respawn after `LEVEL_RESET` starts in phase 1 with full health.
  - [x] Bar runs: the run lengths and regions for (phase, phases, health), including the last phase and 0.
  - [x] Damage map of `BigAssMaverickRobot`: pellet 3, half charge 4, full charge 6 (round of 2026-11-14; first
    draft 2 / 3 / 4), charge explosion 3 / 4, special weapons 3, `Axe` 4, big Reactor shot 6, explosions and shards
    2, `ReactorExplosion` 3, `ProtoBuster` 4 and sabre half 4 / full 6 (after the merge of `060c`), and
    `DAMAGE_INCREASE` scaling.
  - [x] Each line of `docs/TEST-DEBT.md` (the rules of the iteration rounds of 2026-11-14 and 2026-11-15) has a test
    that was seen to fail with the rule broken.
  - [x] Aim points: lead, current, near and wide stay inside the room and inside their ranges.
  - [x] Motion slots: each mode's slot at `t = 0` and at the cycle end; the mode switch only at a cycle end.
  - [x] Slam aim (2026-11-14): the start point puts the target on one of the 5 directions and inside the arena; the
    hit point is on a floor or wall edge; a corner gives the floor.
  - [x] Pincer clap (2026-11-14): the meet point and the 2 start points stay inside the arena margin; the hands
    meet at the meet point; the second clap aims again.
  - [x] Perimeter path (2026-11-14): a path offset maps to the right side and point at each corner; a wave turns at
    a corner and stops at each wall top.
  - [x] Hand states: a hand that a player stands on gets no attack; punch, slam and clap reach `IDLE` again; the
    damager and the platform are on in the states of the table in `NOTES.md`.
  - [x] `BigAssMaverickRobotHandTest` passes with its new spawn setup.
- [x] `./gradlew core:smokeTest` passes (the new bit region is checked by a smoke or unit test that reads it from
  `Bits`).
- [x] `./gradlew lwjgl3:jar` builds.
- [x] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from the worktree:
`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-077a-intro-boss`

Experimental checks:

- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --heartTanks all --healthTanks all`: the intro fill in 2
  passes of 2.25 s (the phase-2 color fills first, then the phase-1 color fills over it); the hands orbit, then
  blend into the hover drift, out of step, and back; punches with a pull-back tell, a recoil and a quick return;
  slams: the fist slides onto a straight or 45 degree line with you, flashes, holds 0.5 s (0.45 s in phase 2) and
  dives along it past you to the floor or a wall; the shock waves run along the floor and the walls, turn at the
  corners and stop after 0.5 s (0.6 s in phase 2) or at the ceiling; a floor wave is cleared by a jump; stand
  against a wall (touching it), or jump beside a wall at the fist's height, to get a wall hit; a step away from the
  wall gives floor hits; the fist rests on the floor or the wall as a step; the orbs aim in a mix (one leads, one at
  you); a pellet takes 3 bits, a half charge 4, a full charge 6; every hit from the boss (head, hands, orbs, shock
  waves) takes 1 bit from you. At 0 in the first bar: the bar rolls over at once in the phase-2 color, with a shake
  and a flash, no pause; phase 2 is faster, has the pincer clap (twice: both hands go to your height on each side of
  you, flash, and close on that point; move or jump away after the flash) and 3-orb volleys. At 0 in phase 2: the
  death (the aghast face, the head blinks, explosions for the whole 4 s, chains pop one by one, hands bounce, head
  shakes and sinks, a last blast, the orb ring). The body stays.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --weapons all --heartTanks all --healthTanks all`: each
  special weapon takes 3 bits (the Axe 4, the big Reactor shot 6, explosions and shards 2); the Axe no longer
  kills in 1 hit.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --players 2 --heartTanks all --healthTanks all` (a second
  pad for slot 1): the hand attacks and the orbs go after both players in turn; a hand under either player never
  attacks; with both players on the hands, only the orbs come.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --difficulty hard --heartTanks all --healthTanks all`
  (faster), or
  `./run-game-alpha.sh --screen SELECT_DIFFICULTY_SCREEN --level INTRO_STAGE --room room12 --heartTanks all --healthTanks all`
  and choose hard: the hard rows (3-orb volleys in phase 1, 4-orb volleys in phase 2, shorter tells, shorter
  waits). The staging starts the level at `room12` (review of 2026-11-15).

Control checks:

- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14 --heartTanks all --healthTanks all`: a 1-phase boss: the bar
  is one color, fills as today, and empties as today; the boss dies once at 0.
- `./run-game-alpha.sh --level X6_INTRO --room room2 --heartTanks all --healthTanks all`: `D1000` (direct
  `translateHealth` calls): its bar, its half-health overdrive and its defeat as today.

## Behaviors to check

- A player death in the fight: `LEVEL_RESET`; the retry starts in phase 1 with a full bar.
- A death in phase 2: the same.
- The level end after the defeat: the outro waits 1 s more (4 s defeat) and goes on to the next screen as today.
- Pause during the roll-over and during the death: the game resumes in the same state.

## Must not change

- Every boss other than `BigAssMaverickRobot`: health, damage, bar look and fill, defeat (`healthPhases = 1`).
- `BitsBar` and the player health and weapon bars.
- The fight plans and steps in `fights/` (no new step).
- This boss's entrance (fall, ceiling pieces, shake, hands fly out, arm orbs reveal), its fight music, its stun on a
  full charge, the head look, the shields on the hands.
- No attack with a hand that a player stands on. The platform follows the hand in every state.
- The headless body stays drawn after the death until `LEVEL_RESET`.
- `GutsTank` and `WilyFinalBoss` orbs: 3 to a player, default orb path.

## Intended changes

| Change | Before | After |
|---|---|---|
| Health phases on `AbstractBoss` | 1 bar of 30 | per-boss phases; this boss 2 x 30 with a two-color bar |
| Pellet / half / full charge on this boss | 1 / 2 / 3 | 3 / 4 / 6 (`ProtoBuster` 2 -> 4; sabre half / full 2 / 3 -> 4 / 6) |
| Charge explosion on this boss | 1 / 2 | 3 / 4 |
| Special weapons on this boss | size table (Axe 30, Needle 20, ...) | 3 each; `Axe` 4; big Reactor shot 6; explosions and shards 2; `ReactorExplosion` 3 |
| Hand idle motion | mirrored circle only | orbit and hover drift, with blends |
| Hand attacks | punch (1 s still tell, 1 s wait, 4 tiles/s return) | punch (pull-back tell, recoil, spring return), slam aimed on a 0/45/90 degree line at the player, to the floor or a wall, with shock waves over the walls and the floor (2026-11-14), pincer clap on the player position in phase 2, twice (2026-11-14; replaces the double clap) |
| Hand choice | 76/101 nearest hand, else random | the free hand nearest to the target player |
| Returning hand | hurts, no platform | harmless, carries a rider |
| Orb aim | the player center at spawn | mixed (lead, current, near, wide), set at launch |
| Orb fire sound | none at spawn | `BLAST_2_SOUND` |
| Co-op targets | nearest player | alternate in slot order |
| Death | 3 s, X eyes, all chains pop at once | 4 s sequence with an aghast face, a head blink and explosions for the whole 4 s |
| Panic attack under 25% (item `077`) | not built | replaced by phase 2 for this boss |
| `BigAssMaverickRobotOrbExplosion` pulse slicing | 1x3 on a 2-frame strip (no caller) | 1x2 |
| Damage to the player from this boss | head and hand 2; this boss's orbs 3 | head and hand 1; this boss's orbs 1; shock wave pillar 1 |
| Intro fill of a phased boss | one pass of the max (no phases; D2 planned one pass over a shown phase-2 color) | one pass per phase, last phase first (owner, 2026-11-15; replaces D2) |
| Shock wave sound | none | `SHOCK_WAVE_HIT_SOUND` per pillar, no electric pulse |
| QoL: `--difficulty normal\|hard` launch option | none | sets the difficulty at start; `saveState()` writes nothing while it is given |
