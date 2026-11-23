# Scope: mega-refactor-078g-timber-walker

- Status: play round 2 (no brief: the owner asked for a first pass to play; see the process log, 2026-11-22)
- Approved: no

## Goal

A first pass of the Timber Walker mini boss fight, playable in a test room of a new test map `RefactorTest078`. The
design comes from the walker mockup (rounds 1 to 5, `temp/078-timber-walker-miniboss/NOTES.md` in the main
checkout, page https://claude.ai/artifact/37YRaAp6NdsPLCKaJeFYY8) and the owner's answers. The fight is tuned in
play rounds (`docs/ways-of-working/gameplay.md`). No real level uses the walker in this branch.

## Owner decisions on the mockup

| Round | Question | Answer |
|---|---|---|
| 3 | Met while the guard is closed | A: face shows in the walk, hidden in idle, crouch and land |
| 3 | Weak point | A: the guard opens after the saw strike, while the saw is stuck |
| 3 | Size | A: 7.3 x 6.1 tiles (the hull and legs) |
| 4 | Second attack | A: both the log throw and the hop with a chip wave |
| 4 | Turn | A: the 2-frame turn |
| 4 | Defeat | A: the Met jumps out, cries and runs away; the wreck stays for a moment |
| 5 | R1 saw reach | B: 8 tiles, with a third telescoping section |
| 5 | R2 saw thrust height | C: both heights (low, 1:4 down; level, 2.6 tiles), with 2 aim poses as the tell |
| 5 | R3 claw arm use | B: the log throw, the high reach, and also a grab: the claws take Megaman and throw him |

Round-3 and round-4 answers are the recommended defaults that the later rounds were drawn with (the owner went on
with "Looking good" each time).

## Owner notes, play round 1 (decisions for round 2)

| Note | Decision |
|---|---|
| 1. Larger walker that holds the right side | Art round 7: about 9.5 x 6.8 tiles. Home: the back against `right_wall`. `WALK` goes at most 2 tiles toward the player, then back home. Body, hit boxes and sprite come from the anchors. |
| 2. Shield: the guard is the target | Each player shot on the closed guard counts 1, a `ChargedShot` 2 (half or full charge). At 4 the guard opens, the Met is stunned 2.0 s (the only damage window), recovers (0.6 s asked; 0.89 s in the art-round-7 row), the guard closes, the count resets. The chop is a plain attack. `HURT` and the "2 attacks between chops" rule are gone. |
| 3. Higher hop | The hop goes to the player's x (inside the arena, at least 2 tiles from home), then hops back home. Apex: the walker's feet 4 tiles above the floor; air time 1.6 s. A shadow on the floor shows the landing point. The arena interior is 12 tiles tall. |
| 4. Telegraphs | Before each arm attack, a tell (0.58 s in art round 7) with a sound (lever: `REV_SOUND`, button: `BUTTON_SOUND`). |
| 5. Smoothness | State, animation key, frame index and facing change in one call, before the sprite reads them. Review findings 1, 3, 4, 5 and 8 fixed in code; 2 in the frame data (foot line). |

## The fight (round 2)

The walker faces left for the whole fight. Each attack ends in `IDLE`; from `IDLE` the walker goes home first if it
is away, else it picks the next attack.

| State | Animation | What happens |
|---|---|---|
| Intro: far walk | `far_walk` | In the background, from the `far_start` point (left side) to 2 tiles before home |
| Intro: jump in | `far_jump`, `jump_mid_1`, `jump_mid_2`, `jump` | Grows in 4 drawn sizes while it jumps to home |
| Intro: land, turn, roar | `land`, `turn`, `roar_start`, `roar`, `roar_end` | Shake at the landing; the 2-frame turn to face left; the roar; then the fight starts (`isReady`) |
| `IDLE` | `idle` | A short wait, then pick |
| `WALK` | `walk` | Toward the player (at most 2 tiles from home) before `SAW_CHOP` or `CLAW_HIGH`; or back home |
| `TELL` | `tell_lever_low`, `tell_lever_level`, `tell_lever_chop`, `tell_button_claw` | 0.5 s, with a sound, before each arm attack |
| `SAW_THRUST_LOW` | `saw_low` | The ram shoots out low; jump over it |
| `SAW_THRUST_LEVEL` | `saw_level` | The ram shoots out level; stay low |
| `SAW_CHOP` | `attack_saw` | The saw chops down, sticks, recovers |
| `LOG_THROW` | `attack_log` | The claw throws a log in an arc at the player |
| `HOP_CROUCH`, `HOP_AIR`, `HOP_LAND` | `hop_crouch`, `hop_air` (`jump`, then `hop_tuck`), `jump`, `hop_land` | Hop to the player (shadow at the landing point); the legs tuck in the air and extend again before the touch-down; chip waves at the landing; next pick hops back home (no chip waves) |
| `CLAW_HIGH` | `claw_extend` | A high player in reach: the claw reaches up |
| `CLAW_GRAB`, `GRAB_MISS`, `GRAB_THROW` | `grab_reach`, `grab_miss`, `grab_throw` | Grab, swing and throw (damage) |
| `GUARD_OPEN` | `guard_open` | After 4 guard hits; door sound |
| `MET_STUNNED` | `met_stunned`, `met_hit` | 2.0 s; the Met is damageable (damage blink, `met_hit` with its `hit_flash` at a hit) |
| `MET_RECOVER` | `met_recover` | 0.89 s: the Met gets up and pushes the close button |
| `GUARD_CLOSE` | `guard_close` | Door sound; then `IDLE` |
| low health | `damaged_*` | Below half health, every row that starts or ends on `idle_1` uses its `damaged_` copy |
| defeat | `defeat` | Shake with explosions, both arms fall off, the hull drops |

Overlays at the cab box: `guard_crack_1` to `guard_crack_3` show the count (1, 2, 3 or more); `guard_hit` flashes
0.1 s at each counted hit.

Picks (player at the walker's home):

| Player | Candidates |
|---|---|
| Under the hull (right of its front edge) | hop, log |
| 2.5 tiles or more above the floor | log; claw high if in reach (7.9 + 1 + 2 tiles of walk) |
| 8.5 tiles or more away | low thrust, level thrust, log |
| 6 to 8.5 tiles | chop, hop |
| Under 6 tiles | low thrust, level thrust, hop |
| Any of the 3 above, 6.5 to 9 tiles and under 1 tile high | + grab |

Distances are from the walker's pivot (`BODY_X`), which is 2.4 tiles behind the hull front. Round-7 reaches from
the pivot: chop 7.5, claw high 7.9 forward, grab 6.4 to 9.2, thrusts 3.6 to 10.4 tiles.

Rules:

- The hull, the legs and the closed guard are a shield: shots bounce off. Only a player shot (or a shot of a
  player's shot) on the closed guard counts. The Met is damageable only in `MET_STUNNED`.
- If the count reaches 4 during an attack, the guard opens when that attack ends (in `IDLE`), or at once in `WALK`,
  or at the end of a `TELL` (the attack after the tell is dropped).
- The walker does not attack while the guard is open, opening or closing.
- The same attack never comes 3 times in a row.
- The walker never walks or hops past the arena walls (`left_wall`, `right_wall`); the hop target is clamped before
  lift-off.
- The hull and the legs hurt at contact. The saw, the claws, the log, the log pieces and the chip wave each hurt.
  Damage per damager is in `PlayerDamageNegotiator`.

Numbers (round 2, art round 7): guard 4 hits (charged shot 2), stun 2.0 s, Met recover 0.89 s (`met_recover` row);
hop: the floor line rises 3 tiles, the tucked feet are 1.75 tiles above it, so the gap under the feet at the apex is
4.75 tiles (rule: 3.5 or more); air time 1.6 s; the feet are above 1.75 tiles (a standing Megaman, 1.5 tiles, plus
0.25) from 0.09 s (the tuck) to 1.32 s: a 1.23 s window; the legs span 7.1 tiles; walk range 2 tiles at 2 tiles/s;
tells 0.58 s (`tell_*` rows).

## In scope

- The boss entity and its parts (saw, claws, log, chip wave), its frames in an atlas, its fight plan entry (mini boss
  plan), its damage table entries.
- The new art of round 6 for R1 to R3 (a Claude Fable 5.2 art agent): the 8-tile saw with a third section, the
  level thrust and its aim pose, the grab frames.
- Test map `RefactorTest078.tmx`, `LevelDefinition.REFACTOR_TEST_078`, room `timber_walker` (the arena, 12 tiles of
  interior height) and the room before it with the player spawn and the boss gate.
- Art round 7 (another agent, done): the larger walker (9.4 x 6.9 tiles) and the frames of the round-2 animation names.

## Out of scope

- The Timber Woman stage redesign and the walker's place in it.
- Unit tests, mutation checks and the review before the play test (they come when the owner calls the moveset
  final; the spec lines go in `TEST-DEBT.md`).

## Contract (first pass)

Intended changes: a new mini boss, new frames and an atlas, a new test map and level entry, new damage table
entries, a new fight plan entry. Nothing else changes.

Checks:

1. `./run-game-alpha.sh --level REFACTOR_TEST_078 --room walker_gate --heartTanks all --healthTanks all`: walk
   right into the arena; the walker walks in the background from the left, jumps in, lands with a shake, turns and
   roars; then the fight runs every state of the table above. Shoot the guard 4 times (or 2 charged shots) to open
   it; run or slide under the hop.
