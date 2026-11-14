# Test debt: mega-refactor-077a-intro-boss

Closed on 2026-11-15 by the final pass (`docs/WAYS_OF_WORKING.md`, "Gameplay iteration"). Every line below is
covered by a test that runs in `core:test`. Each new or changed test of a rule was seen to fail once with the rule
broken in the code (the mutation column), and the code was restored (checked with `git diff`). The process log entry
of 2026-11-15 (final pass) has the runs.

Short names: `Fight` = `BigAssMaverickRobotFightTest`, `Parts` = `BigAssMaverickRobotPartsTest`, `Damage` =
`BigAssMaverickRobotDamageTest`.

## Rules

| Rule | Test | Mutation that made it fail |
|---|---|---|
| The clap closes on the target center, 2 tiles from each wall, between floor + 0.5 tile and 1.5 tiles under the ceiling | `ClapAimTest` (6 cases); `Fight` "closes twice on the player center ..." | wall gap removed in `ClapAim.plan` |
| Each fist starts 4 tiles to its side, inside the arena margin; no fist moves away from the meet point | `ClapAimTest` "holds the meet point ... left wall, and the near fist starts at its stop"; `Fight` "closes twice ..." (x of each fist per frame in `CLAP_CLOSE`) | start not held at the stop |
| Both fists lock, close and hit together, also when one fist has a shorter way | `Fight` "closes twice ..." (same state each frame; the first clap has a 1.5-tile and a 3-tile way) | `conductClap` steps on `any` hand done; `conductClap` steps the left hand only |
| The second clap aims again at the target position at that time | `Fight` "closes twice ..." (second impact at the moved player) | second clap reuses the first meet point |
| After the second clap, both hands go back to `IDLE` | `Fight` "closes twice ..." (2 impacts, then `IDLE`) | `CLAPS` = 3 |
| A head stun during a clap does not stop either hand | `Fight` "keeps both hands moving through a head stun in the clap" | `conductAttack` after the stun return |
| No clap while a player stands on a hand, and none in phase 1 | `Fight` "never claps while a player stands on a hand", "never claps in phase 1"; `Parts` "has no clap in phase 1" | phase-1 clap weight 1 |
| The orb volleys pause during a clap | `Fight` "closes twice ..." (no volley orb during the clap) | volley not paused in a clap |
| Damage on this boss (pellet 3, half 4, full 6, explosion 3 / 4, specials 3, Axe 4, big Reactor 6, explosions and shards 2, `ReactorExplosion` 3), and `ProtoBuster` 4, sabre half 4 / full 6 | `Damage` (all cases); `ProtomanTest` "BigAssMaverickRobot takes 4 from a half hit and 6 from a full hit ..." | `Axe` 3, `ProtoBuster` 3, half sabre 5 |
| A shock wave stops after the wave life of its row (0.5 / 0.6 / 0.6 / 0.8 s), or at a path end | `Parts` "makes a pillar each 0.08 s up to the wave life ...", "stops at the path end ..."; `Fight` floor slam (2 x 8 pillars) and wall slam (7 up to the wall top, 7 down and along the floor) | wave life ignored in `ShockWaveRow.update` |
| Every damager of this fight takes 1 bit; an orb of `GutsTank` or `WilyFinalBoss` takes 3 | `Fight` "is 1 bit from each damager of this fight, and 3 from an orb of another boss"; `PlayerRulesTest` "gives the table value ..." | hand 2; other owner 1 |
| Each pillar plays `SHOCK_WAVE_HIT_SOUND` once; no electric pulse at the wave start | `Fight` floor slam and wall slam (sound requests of the hand) | hit sound removed; electric pulse added at the wave start |
| The intro fill of a 2-phase boss fills the phase-2 color first, then the phase-1 color over it; a 1-phase boss as before | `BossBitsBarTest` "the intro fill" (2 cases) | fill phase counted from phase 1 |
| The tuning rows hold the 2026-11-15 values | `Parts` "the tuning rows" (2 cases) | `ATTACK_WAIT_HARD` 2; phase-1 clap weight 1 |
| An aimed orb takes its trajectory from the supplier when its hold ends; an orb with no supplier keeps its spawn trajectory | `Fight` "the aimed orb" (2 cases) | supplier called each frame after the hold; spawn trajectory not applied |
| During the death the head blinks each 0.05 s | `Fight` "shows the defeated face, blinks the head ..." | blink removed |
| During the whole death the head shows `defeated` and explosions keep coming | `Fight` "shows the defeated face ..." (each frame; each 0.5 s); "pops the chains ..." (last blast) | face `stunned`; explosions only in the first 1.5 s |
| A volley fires 2 / 3 / 3 / 4 orbs (normal 1, normal 2, hard 1, hard 2), one for each aim of the row (review before the merge, 2026-11-15) | `Fight` "the orb volleys" (4 cases, through `updateVolley` with both hands held by players) | `ORB_GAP` 2.5, `ORB_GAP_PHASE_2` 1.1, `ORB_GAP_HARD` 0.7, `VOLLEY_DUR_HARD_PHASE_2` 1.5; `WIDE` back in the `HARD` row |
| An orb supplier that is not a `(from, out) -> out` function throws `InvalidOrbSupplierException` at spawn | `Fight` "throws InvalidOrbSupplierException ..." | the plain cast, with no catch |
| The intro fill takes `max x phases x DUR_PER_BIT` (1-phase and 2-phase boss) | `BossHealthHandlerTest` (2 cases, through `set`, `update` and `finished`) | `bits = max` |

Known gap (review before the merge, 2026-11-15): the phase that the bar shows during the fill (`fillPhase` before
the timer ends, `healthPhase` after it) is read only by the private phase supplier of the `BossBitsBar`. No public
state shows it, so no test covers that gate without a wider visibility. `BossBitsBarTest` covers the fill order
through the companion functions.

## Tests that an iteration round broke or made stale

| Line | Result |
|---|---|
| Draft pincer clap tests in `Fight` | Run and pass; with `setSeed(3L)` a clap starts within 90 s. Sync and closing checks added |
| `ClapAimTest` draft cases | Run and pass (6 cases) |
| `Parts`: 3 cases for the clap renames | Run and pass |
| `Damage` values | Set to the values of the 2026-11-14 round; `ProtoBuster` case added |
| `Fight` slam wave counts and the death test | Counts set by the wave life; the death test takes the defeat explosions that keep coming at the last blast; new death test for the face, the blink and the explosions |
| `Parts`, `SlamAimTest`: `ShockWaveRow.start` life | The path tests pass a 10 s life, so that only the path end stops them |
| `LaunchOptionsResolverTest` `--difficulty` cases | Run and pass; `savesAllowed` with `--difficulty` seen to fail with the rule removed |
| Not listed: `PlayerRulesTest` "gives the table value for a tag in the table" (expected 2) | Set to 1, the value of the 1-bit round |
| Not listed: `Fight` `hit()` (assumed 2 per pellet) | Now 3 per pellet; "blocks a second hit ..." starts at 27 damage |
