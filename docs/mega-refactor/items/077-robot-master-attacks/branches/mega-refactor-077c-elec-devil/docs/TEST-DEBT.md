# Test debt: `mega-refactor-077c-elec-devil`

The rules of behavior that the final test pass turns into tests (see `docs/ways-of-working/gameplay.md`).

## Rules that must stay true

- Live Wire: a link joins 2 pieces whose start rows and target rows are all in one band: rows 0 to 1
  (`JUMP_OVER`) or rows 2 to 5 (`CROUCH_UNDER`).
- Live Wire `CROUCH_UNDER`: the lowest point of each piece circle and of the arc is above the top of a crouching
  Megaman's damage box (1 tile) at all times.
- Live Wire `JUMP_OVER`: the time from the first piece reaching a player until the second piece leaves that player is
  at most `JUMP_CLEAR_TIME - JUMP_TIMING_SLACK`, at every piece speed of every tuning row; first launch and
  cross-column pairs included.
- `JUMP_CLEAR_TIME` is not more than the time Megaman's feet stay above the top of a row-1 piece (1.69 tiles) in a
  full jump, from `MegamanValues` and `STANDARD_RESISTANCE_Y` (simulation on 2026-11-18: 0.51 s).
- `CROUCH_UNDER_MIN_ROW` is the lowest row whose piece circle clears a crouching Megaman.
- In a random-target launch, the target rows of each column are still a permutation of its rows.
- When no pair in the queue has both pieces in one band, a launch has fewer links than its tuning row.

## Existing tests that this round broke

- `ElecDevilFightTest` "links only pairs of consecutive pieces ...": calls `LiveWireLinks.choose` with the old
  parameters, and expects exactly `liveWireLinks` links (now fewer when the bands do not allow them).

## Rules added later in round 1

- Eye Spread: the 3 balls leave `EYE_SPREAD_SHOT_GAP` apart, in the aim order of the tuning row, toward the targets
  taken at the end of `CHARGE`; 1 `VOLT` sound per ball. (Breaks the `ElecDevilFightTest` "Eye Spread" test, which
  expects 3 balls right after `CHARGE`.)
- Live Wire arc: the drawn tiles cover the line from one piece center to the other, with no gap and no overlap past
  the far center; a cap at each end; hidden when the link is gone.
- Phase 2: at the phase-2 start, each `WilyGenerator` goes to `POWER_DOWN` (no light events, no sparks after it);
  with no generator, the phase start does not fail.
- `appear` and `defeated` each have 8 frames in `Bosses4`; the `appear` animation ends within the shortest
  `APPEAR_DUR` of all tuning rows; `defeated` does not loop.
- The body shows `shoot` while the boss is in `STAND` and Eye Spread shots are due (from the end of `CHARGE` to
  `EYE_SPREAD_SHOT_GAP` after the last ball), and the state animation at all other times.

## Rules added in round 2 (eye patterns, Tether Claw)

- Each `CHARGE` picks a pattern from the tuning row's `eyePatterns` with `services.random`, never the previous one;
  phase 1 rows hold Fan, Ring, Stall; phase 2 rows also Bounce.
- The body plays `charge_<color>` in `CHARGE` and `shoot_<color>` while the shots are due (at least
  `EYE_SHOOT_MIN_DUR`); every `ElecBall` of the pattern has the same color.
- Fan: as the round-1 Eye Spread rule. Ring: `RING_BALLS` balls at once, evenly spaced, one aimed at the player,
  speed `RING_SPEED`, 1 `VOLT` sound. Stall: 3 balls toward the player's side at `STALL_ANGLES`, stop after
  `STALL_MOVE_DUR`, wait `STALL_PAUSE_DUR`, then fly at the player's position of that moment. Bounce: 2 balls aimed
  at the floor mirror of the current and the lead point; each bounces once off a block, then hits.
- `HAND` has no timer: windup (`clawWindupDur`), the claw out and back, recoil (`CLAW_RECOIL_DUR`), then the next
  state. The claw spawns at the start of `HAND` in its aim state (no sprite, no damage, a flickering guide line at
  `GUIDE_ALPHA` from the socket to the end point). At the end of the windup it flies in a straight line from the hand
  socket to the far room edge (its side touches the wall, its bottom on the floor) at `clawSpeed`, holds
  `clawHoldDur` and returns the same way.
- The claw and the tether hurt (player damage table: 3); the claw blocks shots (shield). The tether hitbox is 2
  lines, `TETHER_HALF_THICKNESS` either side of the line from the socket to the claw back. The only safe place is
  under the high end of the slope, near the boss. The hand fixtures act only in windup and recoil. A defeat or a
  boss destroy removes the claw.
- Broken by round 2: `ElecDevilFightTest` cycle test (the `HAND` duration is no longer fixed) and the Eye Spread test
  (phase 1 now picks a random pattern). `ElecDevilTuning` lost `handDur` and `eyeShotDelay`.
