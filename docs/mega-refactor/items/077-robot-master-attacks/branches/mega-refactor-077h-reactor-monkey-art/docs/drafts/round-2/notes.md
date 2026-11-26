# Reactor Monkey drafts, round 2

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Built on the current `stand` and `throw`
frames (original artist: not known to me, ask the owner). Rebuild: `python3 work/finalize.py`. Audit of round 1: `audit.md`.

**Fix for the owner's feedback:** every frame is now cut from the original pixels: head (dome, visor, tusks, jaw), the 2
ringed shoulder pads, torso plate or ribbed back, belt and loincloth, arms, legs, tail. Parts only move, lose or repeat
rows, or lean by whole pixels. Hand-drawn: the mouths, the eyes, the forearm of a chest hit, the effects. In upright
poses both pads sit at head height on the torso, as in `throw` 1, and the arms hang from under them.

All cells 160 x 160, vertical strips, facing left, feet on the bottom row.

| Strip | Cells | Seconds per cell | Play | Cells show |
|---|---|---|---|---|
| `intro_drop` | 1 | held | hold | `throw` 1 body, arms up in a V, legs spread, mouth open |
| `intro_land` | 3 | 0.06, 0.08, 0.12 | one-shot | deep crouch with tail up; rebound; `stand` 3 px low |
| `intro_roar` | 6 | 0.12, 0.08, 0.3, 0.3, 0.12, 0.12 | one-shot | rises; upright, mouth open, visor flash; 2 roar cells (head up, arms and tail shake); down; `stand` 2 px tall |
| `dying` | 4 | 0.08 | loop | lean back, near fist up; sag; lurch, far fist up; sag. Visor cracked, lime, dim, lime; sparks, green seams |
| `collapse` | 6 | 0.1, 0.1, 0.1, 0.08, 0.1, 0.14 | one-shot | sag; elbows buckle; chest drops, far arm slides out; chest and head hit the floor; small bounce; settle, tail flat |
| `dead` | 1 | held | hold | = `collapse` 6 |
| `chest_beat` | 8 | 0.1, 0.1, 0.3, 4 x 0.07, 0.12 | one-shot, loop cells 4 to 7 about 4 times | `stand` with red eyes; rises, red head; arms high, roar; left fist on chest; arms high; right fist on chest; arms high; down |
| `mad_stand` | 2 | 0.08 | loop | `stand` 2 px low, red head, slanted eyes, bared teeth; cell 2 trembles 1 px |
| `mad_throw` | 3 | 0.1 | one-shot | `throw` with the red head |
| `ball_hit`, `mad_ball_hit` | 5 | 0.06, 0.14 x 4 | one-shot (cells 3 and 4 can loop while dazed) | hit: squashed, visor flash; knocked back upright, head back, arms flung up, X eyes; dazed lean back; lean forward, eyes dim; sag, then `stand` |
| `*_damaged` | | | | of `mad_stand`, `mad_throw`, `ball_hit`, `mad_ball_hit`: brief's map, plus red and dark red to white |

Unchanged from round 1 (copied): `steam`, `steam_jet`, `ball_mad/`. GIFs: one per strip, plus `intro.gif` and `death.gif`.

## What the code must do

- Intro: hold `intro_drop` while he falls, `intro_land` on landing (shake, dust), `intro_roar` (sound at cell 2), then `stand`.
- Death: loop `dying` under the explosions, `collapse` once, hold `dead` during the fade.
- Enrage: `chest_beat` once (shake on cells 4 and 6), then `mad_stand` and `mad_throw` replace `stand` and `throw`.
- Ball knocked back: remove the ball on contact, play `ball_hit` (or `mad_ball_hit` when enraged), then the stand strip.
  Cell 1 is on all fours; if the hit lands during `throw`, start at cell 2.
- Steam ear points (art px, left ear, right ear): `mad_stand` 1: (45, 89), (66, 89); cell 2: 1 px left.
  `mad_throw`: as in round 1 (the poses did not move).
- `collapse` 3 to 6 and `dead` use x 5 to 141 of the cell: wider to the front than `stand`.

## Deviations and open points

- `intro_drop` has its feet on the bottom row (round 1: 1 px above).
- `collapse` has no "rump up" cell: the hips of the original cannot rise that far without a redraw.
- The chest-hit forearm is the only new limb drawing (2 cells). The flat arm of `collapse` and the flat tail of `dead`
  are the original parts turned 90 degrees.
- Stars of `ball_hit` are white and lime; of `mad_ball_hit` white and red.
- No `_damaged` copy of `chest_beat`: say if he can take damage during it.

## check.py result

Cell size, NES palette, allowed colours (body `000000 503000 940084 D800CC`; effects `FCFCFC F83800 A81000 B8F818 00A800`;
the ball inside `mad_throw`), no semi-transparent px, feet on the bottom row: all ok (`work/check-result.txt`).
Strict "3 colours per 8x8": not met, as in the current art (`stand` 75 of 220 areas, `throw` 159 of 435); new strips 33 to 44 %.
