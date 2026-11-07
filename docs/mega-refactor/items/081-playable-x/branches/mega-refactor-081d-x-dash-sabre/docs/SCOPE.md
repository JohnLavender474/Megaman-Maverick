# Scope: mega-refactor-081d-x-dash-sabre

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D4)

## Goal

Phase 3 of X (the ground dash, the dash jump, the dash wall kick, the faster dash start on `SELECT`), the Z-sabre slash
(phase 4), and less knockback when X takes damage. The owner's X tuning (`a15fa6184`) is the first commit. Megaman plays
the same.

## Item

`081`, "Phases" 3 and 4. Owner (2026-11-06): "fold in a few additional changes to the next phase": the owner's X
tuning, the Z-sabre slash, and more resistance or friction on X's knockback.

## Current state

The full scan with `file:line` evidence is in `temp/081d/scan.md` (git-ignored, 2026-11-06).

- Knockback: X uses Megaman's knockback (8 t/s side, 5 t/s up). X's body has no side friction and the side-zero rule
  skips `damaged`, so X keeps 8 t/s for the 0.5 s damage time: about 4 tiles. Megaman goes about 1.27 tiles (drag 6/s
  plus block friction). X6: 4.69 t/s with a 21.1 t/s² decel, about 0.5 tiles, no lift.
- Ground dash (X6): 0.083 s start delay, 12.19 t/s down to 7.73 t/s over 0.5 s, then a 0.283 s end slide; the body
  box does not shrink. A jump during a dash is a dash jump (air speed 7.73 t/s, no air dash after it); the dash wall
  kick also keeps the dash speed. X-only; it shares almost nothing with Megaman's ground slide.
- `CLAMP_X` 16 caps speeds above 16 t/s. `WALL_KICK_AWAY_SPEED` is still the X6 3.75.
- Sabre: a separate slash entity owned by X breaks `SabreBlock` and cuts `X6Shot` with no change to them (they take a
  player-owned damager). Damage entries: `X6Damage` (4 classes), `D1000`, `D1000Core`. Art: `sabre_floor`, `sabre_air`,
  `sabre_wall` and their `_arc` strips are packed.
- The owner's body size (1 x 1.5 tiles) makes the 2-tile side openings in `X6_INTRO` harder to enter (feet window 0.81
  to 0.5 tiles); no route breaks.

## In scope

D1 (owner): the sabre goes to its own branch `081e`, right after this one. This branch:

- The owner's X tuning (`a15fa6184`).
- The ground dash (X-only behavior, new `BehaviorType.GROUND_DASHING`): starts on the ground on `SELECT` only (was `R`; changed 2026-11-06) (D4, changed
  2026-11-06 at the play test); a 0.083 s start delay; 12.5 t/s down to 8 t/s at 21.1 t/s^2; 0.5 s; ends when none of `SELECT`, `LEFT`,
  `RIGHT` is held, on the opposite side, on a wall; then a 0.283 s end slide; off a ledge, normal air speed. The
  buster works during it. The body does not shrink.
- The dash jump: a jump during a dash (after the start delay), or with `SELECT` held, gives the dash air speed (8 t/s) while
  a side is held, and no air dash after it; cleared on landing and on a wall grab.
- The dash wall kick: `SELECT` held at the kick gives the dash away speed (8 t/s) during the lock, then the dash air speed,
  and no air dash after it.
- The faster `SELECT` air dash: a flat start speed of 12.5 t/s (Claude's call: no decel, so Megaman's code does not
  change; the owner tunes by feel).
- X's own knockback (D3): 8 / 5 t/s and a side drag of 6/s while damaged (Megaman's `STANDARD_RESISTANCE_X`), about
  1.3 tiles. X-only.
- Test room `ground_dash` in `RefactorTest081.tmx`; tests.

All start values are in `MegamanXValues` for the owner to tune.

## Out of scope

- Megaman's ground slide and knockback; `CharacterDef`; crouch and climb (open question 10); open question 12.
- The sabre (`081e`, D2: `UP` + `B`). Dash sounds (no assets).

## Overlap with other branches

- `compactor-level` (not a mega-refactor branch): no shared file.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` | helps | X gets its moves without a copy of `Megaman.kt`. |
| `O8-R4` | waiting | The sabre is not a `WeaponDef` until `024`. |
| `O9-R1` | no effect | D4 (changed at the play test): the dash is on `SELECT` only (was `R`; changed 2026-11-06); X needs more than an NES pad, as decided on 2026-11-01. |

### New APIs and their first callers

| API | Production caller |
|---|---|
| X's `groundDashBehavior()` | X's behaviors component |
| X's dash-speed state (air speed after a dash jump or kick) | X's jump `onJump`, `MegamanX.run` |
| `BehaviorType.GROUND_DASHING` | X's behaviors component, X's animation key and body rules |

## Open questions

None. D1 to D4 answered 2026-11-06 (see `NOTES.md`, "Decisions").
