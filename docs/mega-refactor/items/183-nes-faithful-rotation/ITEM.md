# 183: NES-faithful rotation: no sprite drawn at an angle that is not a multiple of 90

- Status: active
- Type: feature
- Objectives: `O9`, `O4`
- Depends on: none
- Related: `058`, `168`, GitHub issue #4
- Branches: `mega-refactor-183b-charged-shot`, `mega-refactor-183c-needles`, `mega-refactor-183d-gravity-turn-flash`

## Goal

On the NES, a sprite never turns freely. A turn of 0, 90, 180 or 270 degrees keeps the pixels as
axis-aligned squares, so it is permitted. Any other angle (fixed, aimed, spinning, following a
velocity, or the camera blend during a gravity turn) shows rotated pixels and breaks the NES look.
This item removes every such angle from play (GitHub issue #4, "IMPROVE: Rotated sprites are not
faithful to the NES").

The 2026-11-23 scan (`NOTES.md`) found 30 sites that can draw at a free angle: the camera turn,
the backgrounds that follow it, 27 entity sprites and 1 test-only shape.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

Small branches, each one independent. The gravity turn is done alone, in its own branch.

| Branch | Content |
|---|---|
| `183d` | Gravity turn behind a screen flash (see below; first named `183a`) |
| `183b` | `ChargedShot`, `ChargedShotExplosion` and `ChargedShotResidual`: a small first batch |
| `183c` | Sites with 45-degree steps or a few fixed angles: a drawn 45-degree region plus turns that are multiples of 90 (the `HomingMissile` pattern) |
| `183e` and later | Spinning and aimed sites, a few per branch: snap each one to 8 or 16 directions with drawn regions; the count is set per entity |
| last | A guard that flags a sprite rotation that is not a multiple of 90 (a debug check, a detekt rule or a smoke test) |

### `183d`: gravity turn behind a flash

- The camera does not turn on screen. The screen flashes to a full colour, and while the flash
  covers the screen, the camera turns as it does today.
- The flash adds a hold before and after the turn: the screen fades to the colour, holds while
  the turn runs, and fades back. When the flash ends, the camera has its new orientation.
- The rotation logic stays the same; only what the player sees changes. The backgrounds turn
  with the camera, behind the same flash.
- It changes camera and screen code only, and no sprite art. It does not depend on `058` (who
  starts a turn).

## Owner's input

2026-11-23 (verbatim): "I think I might want to update the gravity turns so that instead of
visibly rotating the camera, the screen instead flashes fully white or some other color, and by
time the flash is finished, the camera is already rotated. This would just entail (a) a longer
delay at the tails of the gravity turn (before/after the flash), and (b) the same rotation as
occurs now, but hidden behind the flash."

2026-11-23 (verbatim): "We can fan out to branches so that the work is done in small batches, and
special cases like the gravity turn are done separately in isolation"

## Decisions

- 2026-11-23 (owner accepted the recommendation): one new item with branches, not a separate item
  for the flash. The gravity turn is its own branch (`183a`).
- 2026-11-23 (owner accepted the recommendation): the flash colour is white.
- 2026-11-23 (owner): defer `183a` (the gravity turn). Start with small batches first; the first branch is
  `183b`, the charged shot only.
- 2026-11-23 (owner accepted the recommendation): each `183` branch squash-merges directly into `master`.
- 2026-11-24 (owner): `183c` is the needle (`Needle`), with many directions so that its arc still looks natural.
- 2026-11-24 (owner): the gravity turn is branch `183d` (`mega-refactor-183d-gravity-turn-flash`), not `183a`. The
  sequence: the player touches a gravity change, a longer delay, the screen fades to white, the whirl
  (`GravitySwitchAura`) draws over the white (both are `FOREGROUND` sprites), the turn runs hidden under the white,
  and the white fades out on the turned view.
- 2026-11-24 (owner accepted the recommendations, after the `183d` scan): enemies, shots and non-player bodies stay
  frozen for the whole sequence; durations 0.25 s delay, 0.25 s fade in, 0.75 s turn, 0.25 s fade out; the sequence
  spawns one whirl at the player for every turn, in place of the 3 spawns per trigger; `MoonMan`'s gravity-change end
  state lasts the full sequence.

- 2026-11-24 (owner): `183d` is implemented but tentative, and set aside unmerged. The owner is not sure that
  players will enjoy a frequent white flash. The gravity turn may stay as it is today (a visible turn at a free
  angle), as the one exception to the item goal.

## Open questions

- `183d`: merge the white flash, or keep today's visible gravity turn as an exception to the goal.
- Fixed angles: which order of entity batches after `183b`.
- `Snow.kt:176` has a spin path behind `ROTATION_ENABLED` (off today). Recommendation: remove the
  path in a `183` branch.
- The guard: a debug check, a detekt rule or a smoke test.

## Sources

- `NOTES.md` in this folder (the full inventory, with `file:line` of each site).
- GitHub issue #4: https://github.com/JohnLavender474/Megaman-Maverick/issues/4
