# Notes: mega-refactor-077b-desert-man

## Decisions

## Overlap with other branches

## Decisions

- 2026-11-15 (owner): scope = review ideas 1, 2, 3, 5 plus the long-punch arm gap; the Wily rematch gets the same
  fight; damage to the player stays 3, in the table, with a `TODO`.

## Long-punch arm gap (scan, 2026-11-15)

Facing left, world x relative to the body center `c` (the sprites are 3.5 tiles = 112 px; the regions are 64 px):

- The main `punch` frame (art columns 0..47 of 64) draws its arm to `c - 56`.
- Extension 1: rectangle bottom-left at `bodyLeft - 112` = `c - 136`; the rectangle is 3.5 px wide (a missing `x PPM`),
  so its center is `c - 134`; the 112 px sprite spans `c - 190 .. c - 78`.
- Gap: `c - 78 .. c - 56` = 22 px with no arm. Extension 2 joins extension 1 with no gap. The right side mirrors it.
- Fix: center extension `i` at `c -/+ i x 112`, so extension 1 spans `c - 168 .. c - 56`.

## Long-punch hit box after the gap fix (2026-11-15)

Facing left, with `n` extensions out (0, 1 or 2) and the body center `c` (body 48 px wide):

| Part | Left end |
|---|---|
| Drawn fist (main `punch` frame or the last extension; both draw the fist at the sprite edge) | `c - 56 - n x 112` |
| Hit box before (`LONG_PUNCH_EXTRA_WIDTH = 1.25`) | `c - 24 - n x 112 - 40` = `c - 64 - n x 112` (8 px past the fist) |
| Hit box now (`LONG_PUNCH_EXTRA_WIDTH = 1`) | `c - 24 - n x 112 - 32` = `c - 56 - n x 112` (at the fist) |

The hit box and the arm damageable box keep their height (0.5 tile, 0.25 tile under the body center). The extension
animation (`arm_extension_punch`, 8 frames of 0.0125 s) grows the drawn arm while the hit box is already at full
length: the same as before the fix.

## Implementation notes (2026-11-15, PLAN steps 1 to 5)

Differences from `SCOPE.md` and choices that the scope did not fix:

- Tuning rows: `entities/bosses/desertman/DesertManTuning.kt` (`NORMAL`, `NORMAL_PHASE_2`, `HARD`, `HARD_PHASE_2`,
  `of(hard, phase)`). The boss reads `tuning` in each guard of `STAND`, and copies it to `attackTuning` at each state
  change; a running attack keeps the row of its start. No package README: the package holds one class.
- Phase-2 start: the shake (0.5 s) and `QUAKE_SOUND` play at once. The `dance_flash` frame (0.6 s) waits for the next
  `STAND`, so that it does not replace a punch or a tornado frame; the stand timer waits for the flash.
- Damage table (2026-11-15, owner): each value is 2 × the `master` value, so that the 2 phases (60 bits) take the same
  number of hits as the 1 phase on `master` (item `077` decision of 2026-11-15). The first table (the `077a` scale:
  pellet 3, half 4, full 6) killed him with 10 full charges in place of 15. `MagmaExplosion` and `PreciousShard` stay
  at 0, as on `master`.
- Player damage table: `DesertMan` 3 (body, punch arm, tornado, erupt: all fixtures of the boss) and `Needle` by owner
  (`DesertMan` 3, other owners 3, the same value that the size rule gave). One `TODO` line above the 2 entries.
- Guards: every attack guard has a roll from the row (`<`, not `<=`). Burrow 0 in phase 1 (0.35 normal, 0.4 hard in
  phase 2), punch 0.75, tornado 0.6 (hard 0.7), dance 0.5, jump 0.81 (the old `getRandom(0, 100) <= 80`).
- Punch wind-up: the top frame of `punch_short` (the white fist) is held for the row's wind-up; the sound is
  `REV_SOUND` (no charge sound of a short length exists; to choose in play). The facing is set at the start of the
  wind-up and does not change. The arm fixtures stay off until the hold ends.
- Tornado: the spin-up (`tornado_start`, now a loop of its 2 frames) uses the row's time; the facing follows the
  player during the spin-up and locks at its end. The `tornado_punch` timer runs from the end of the spin-up (the
  short-punch damager is on for its 0.2 s only). The phase-2 turn happens when the target is more than 1 tile behind
  him, or when he hits a wall; the turn sets the speed to 2 tiles/s in the new direction, restarts the 1.25 s dash
  timer and plays `WIND_1_SOUND` again. The second wall ends the tornado.
- Burrow: 4 new states. `BURROW` (sink, 0.4 s: the one-shot `burrow` frames, which carry their own sand pile; the
  body moves down 1 tile into the sand, with no block collision and no gravity), `BURROW_TRAVEL` (the trail at
  8 tiles/s, hard 10, to the target x locked at the start of the travel and clamped to the arena from `left_wall` /
  `right_wall`), `BURROW_TELL` (0.6 s of splashes within 0.25 tile of the spot, `DIG_SOUND`), `ERUPT` (the one-shot
  `erupt` frames, 0.3 s, at the spot with no move; then a jump aimed at the target: up to 1 tile above it and toward
  its x, at most 12 tiles/s sideways; `tornado_punch` frames while rising, `jump` while falling; `STAND` on the
  sand). A `DustPuff` marks the sink point and the erupt point. From `BURROW` to the end of `BURROW_TELL` the body,
  damager and damageable fixtures are off and `canDamage` / `canBeDamagedBy` are false. The exact rules are in
  `TEST-DEBT.md`.
- The floor height for the trail is the body bottom at the start of the burrow (he stands on the sand when `STAND`
  ends). No TMX change.
- Dark room: in `BURROW_TRAVEL` and `BURROW_TELL` the boss sends a light source event (its own keys, radius 2) at the
  trail point, 0.5 tile above the sand; in `ERUPT` at its body center.
- `left_wall` / `right_wall` are now used (the arena clamp of the trail).
- The region list is explicit, so `DesertMan/init` is no longer requested; the line of `DesertMan` in
  `core/src/smokeTest/resources/known-entity-failures.txt` is removed (the file's rule: a listed class that passes
  fails the test). The smoke test did not run in this round.
- Sand splashes now come from `MegaEntityFactory` (was the deprecated `EntityFactories`). The needles still use
  `EntityFactories` (not in scope).
