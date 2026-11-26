# Reactor Monkey drafts, round 3

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Built on the current `stand` and `throw`
frames (original artist: not known to me, ask the owner). Rebuild: `python3 work/finalize.py`. Round 2 audit: `../round-2/audit.md`.
Compare: `before-after.png` (round 2 above round 3, changed cells only), `contact-sheet.png`.

## What changed since round 2

1. **Raised arms** (`intro_drop`; `chest_beat` 3 to 7 and its `_damaged`; `ball_hit` 2, `mad_ball_hit` 2 and their `_damaged`):
   one arm design. The forearm and elbow are the original pixels of `throw` 1; the fist is the knuckle block of `stand`
   on the wrist, brown knuckles up. The hook and the loose brown block were leftovers of the hands that the ball hid; they are gone.
   Both pads stay on the torso and each elbow sits on its pad.
2. **Chest hit** (`chest_beat` 4 and 6): the round 2 capsule forearm is gone. The `stand` forearm lies level across the
   chest, its elbow end under the pad, with the same fist.
3. **`collapse` has 7 cells** (was 6): new cell 3, rump up. The hind shins repeat rows (15 px taller), the front is down
   on the far forearm, the head is low. Cell 2 now buckles at the knees. In cells 3 and 4 the back plate is a new drawing.
4. **Floor arm** (`collapse` 3 to 7, `dead`): level forearm with a flat underside on the floor, fist resting on its knuckles.
5. **Tail** (`collapse` 6, 7 and `dead`): drawn by pixel, 3 px of magenta in a 1 px outline as in the original: an arc
   that falls (cell 6), then a low wave on the floor with the curl at the tip (cell 7, `dead`).
6. **`chest_beat_damaged`** added.

Checked and left as in round 2: `intro_land`, `intro_roar`, `dying`, `mad_stand`, `mad_throw`, the arms-down cells of
`ball_hit` and `mad_ball_hit` (their arms are whole `stand` arms). Copied: `steam`, `steam_jet`, `ball_mad/`.

## Frame table (cells 160 x 160, vertical strips, facing left, feet on the bottom row)

| Strip | Cells | Seconds per cell | Play | Cells show |
|---|---|---|---|---|
| `intro_drop` | 1 | held | hold | upright, both fists up, legs spread, mouth open |
| `intro_land` | 3 | 0.06, 0.08, 0.12 | one-shot | deep crouch; rebound; `stand` 3 px low |
| `intro_roar` | 6 | 0.12, 0.08, 0.3, 0.3, 0.12, 0.12 | one-shot | rises; mouth open, visor flash; 2 roar cells; down; `stand` 2 px tall |
| `dying` | 4 | 0.08 | loop | lean back; sag; lurch; sag. Cracked visor, sparks, green seams |
| `collapse` | 7 | 0.1, 0.1, 0.12, 0.08, 0.08, 0.1, 0.14 | one-shot | sag; knees buckle; rump up, head low; topples forward; hits the floor; small bounce, tail falls; settle, tail flat |
| `dead` | 1 | held | hold | = `collapse` 7 |
| `chest_beat` | 8 | 0.1, 0.1, 0.3, 4 x 0.07, 0.12 | one-shot, loop cells 4 to 7 about 4 times | `stand`, red eyes; rises, red head; fists high, roar; left fist on chest; fists high; right fist on chest; fists high; down |
| `mad_stand` | 2 | 0.08 | loop | red head, bared teeth; cell 2 trembles 1 px |
| `mad_throw` | 3 | 0.1 | one-shot | `throw` with the red head |
| `ball_hit`, `mad_ball_hit` | 5 | 0.06, 0.14 x 4 | one-shot (cells 3, 4 can loop while dazed) | squashed, visor flash; knocked back, fists flung up, X eyes; dazed lean back; lean forward; sag |
| `*_damaged` | | | | of `chest_beat`, `mad_stand`, `mad_throw`, `ball_hit`, `mad_ball_hit`: brief's map, plus red and dark red to white |

GIFs: one per strip, plus `intro.gif` and `death.gif` (whole sequences).

## What the code must do

- Intro: hold `intro_drop` in the fall, `intro_land` on landing (shake, dust), `intro_roar` (sound at cell 2), then `stand`.
- Death: loop `dying` under the explosions, `collapse` once (thud at cell 5), hold `dead` during the fade.
- Enrage: `chest_beat` once (shake on cells 4 and 6), then `mad_stand` and `mad_throw` replace `stand` and `throw`.
- Ball knocked back: remove the ball on contact, play `ball_hit` (or `mad_ball_hit`), then the stand strip. If the hit lands in `throw`, start at cell 2.
- Steam ear points (art px): `mad_stand` 1: (45, 89) and (66, 89); cell 2: 1 px left. `mad_throw`: as in round 1.
- `collapse` 3 to 7 and `dead` are wider than `stand`: x 8 to 158 of the cell (the flat tail reaches 158).

## Open points

- The rump of `collapse` 3 and 4 is a plain striped plate, the roughest new drawing.
- The raised fist is the `stand` knuckle block as it is (no fingers drawn).
- `intro_drop` and `ball_hit` 2 lean their forearms by whole pixels (1 px per 5 or 6 rows); `chest_beat` arms are straight.

## check.py result

Cell size, NES palette, allowed colours, no semi-transparent px, feet on the bottom row: all ok for all 16 strips
(`work/check-result.txt`). Strict "3 colours per 8x8" is not met, as in the current art and in round 2 (new strips 34 to 43 %).
