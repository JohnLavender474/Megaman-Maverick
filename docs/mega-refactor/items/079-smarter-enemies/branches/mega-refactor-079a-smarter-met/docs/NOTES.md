# Notes: mega-refactor-079a-smarter-met

## Decisions

### 2026-11-18, round 1: the helmet is its own entity; stun limits

Owner: the helmet becomes a separate entity (body and shield fixtures, no block collision) that flies up and falls
back onto the head; the landing, not a timer, ends the stun; spinning stars while stunned; a push-down animation at
the landing. The `Met` listens to the helmet, with a minimum and a maximum stun time; the owner asked for the
derivation. The airborne helmet damages the player (owner) and a charged hit on it counts (owner).

Derivation (constants in `MetHelmet` and `Met`; the code computes the times from them):

- Helmet gravity `g = 15` tiles/s² (lighter than the `Met`'s 37.5, so the pop reads floaty). A launch speed `v0`
  gives the apex `h = v0^2 / 2g` and the flight back to the seat `T = 2 v0 / g`.
- Half charge: `h = 1.5`: `v0 = sqrt(2 x 15 x 1.5) = 6.71` tiles/s, `T_half = 2 x 6.71 / 15 = 0.89` s.
- Full charge: `h = 3`: `v0 = sqrt(2 x 15 x 3) = 9.49` tiles/s, `T_full = 2 x 9.49 / 15 = 1.27` s.
- The helmet moves once per frame (semi-implicit Euler); it lands at most 1 frame early.
- `T_min = T_half = 0.89` s: a stun never ends before the shortest natural stun. Case: a half charge takes 0.75 s
  (`MegamanValues.TIME_TO_HALFWAY_CHARGED`), so a 3rd charged hit can break the helmet in the air before 0.89 s; the
  `Met` stays dizzy until `T_min`, then goes desperate.
- `T_max = T_full + 0.5 = 1.77` s: the margin covers the catch-up of the helmet to a `Met` that moved (at first a
  full-charge knockback of `0.8` tile, `0.4` s at the homing speed of `2` tiles/s; the owner removed the knockback in
  round 1, so the margin now only covers a `Met` moved by a platform or a slope). At `T_max` a helmet still in the air starts the fetch (the snap-on `MetHelmet.seat()` was removed in round 1), so the `Met` is never
  stuck (also when it fell or was carried away).
- Landing animation: 3 frames x 0.08 s = 0.24 s, then `SHIELDING`. The `Met` is shielded in `HELMET_LAND`.

## Overlap with other branches

### 2026-11-18, round 1: the helmet drifts away (owner)

Owner: a knocked-off helmet needs a real cause to land away from its `Met` (weaker homing plus a drift), so that the
fetch, jump and cry paths happen in play. The helmet leaves with a sideways drift in the direction of the shot
(`DRIFT_SPEED_HALF` 1.5, `DRIFT_SPEED_FULL` 3 tiles/s) and steers back with an acceleration of `HOMING_ACCEL` 1.5
tiles/s² (target speed `dx x 4`, at most 2 tiles/s). It seats only within 1 tile of the head.

- Full charge: `x = 3 x 1.27 - 0.5 x 1.5 x 1.27^2 = 2.6` tiles at landing: it misses the head, rests and the `Met`
  fetches it.
- Half charge: `x = 1.5 x 0.89 - 0.5 x 1.5 x 0.89^2 = 0.74` tile: it lands back on the head.
- `T_min` and `T_max` keep their values; at `T_max` a helmet still in the air starts the fetch (chase).

Update (owner, same day): the helmet must land farther, so that the `Met` runs for it. `DRIFT_SPEED_HALF` 3,
`DRIFT_SPEED_FULL` 4.5 tiles/s, `HOMING_ACCEL` 1 tile/s². Half: `3 x 0.89 - 0.5 x 1 x 0.89^2 = 2.3` tiles; full:
`4.5 x 1.27 - 0.5 x 1 x 1.27^2 = 4.9` tiles. Both miss the head (seat needs < 1 tile), so both lead to a fetch.
