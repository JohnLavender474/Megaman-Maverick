# Reactor Monkey drafts, round 4: the green mohawk

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Built on round 3 and on the current
`stand` and `throw` frames (original artist: not known to me, ask the owner).
Rebuild: `python3 work/detect.py && python3 work/mohawk.py && python3 work/finalize.py`.
Compare: `before-after.png` (round 3 above round 4), `contact-sheet.png`.

## What changed since round 3

Only the mohawk. Every other pixel of the monkey is the round 3 pixel (head, visor, grille, red face, pads, torso, belt,
loincloth). Frame names, counts and timings are the same; see `../round-3/notes.md` for the frame table and the code hooks.

- **Added to every strip with the head:** `intro_drop`, `intro_land`, `intro_roar`, `dying`, `collapse`, `dead`,
  `chest_beat`, `mad_stand`, `mad_throw`, `ball_hit`, `mad_ball_hit`, and the 5 `_damaged` copies.
- **Extra drafts, not in the order:** `stand`, `throw`, `stand_damaged` (1 cell, as today), `throw_damaged`: the current
  game frames with the same mohawk. Without them he has a mohawk only in the new animations.
- Copied as they are: `steam`, `steam_jet`, `ball_mad/`. `demo_mad_stand_with_steam.gif` is not copied (it would show no mohawk).

## Spike layout

- 3 chunky spikes on the crest of the dome, in a row from the brow (left) to the back (right): a short front spike, a tall
  middle spike, a medium back spike. The tips sweep back 1 to 2 px. The base is 13 px wide, on the top of the 19 px dome.
- Each spike column goes down to the dome outline, so the base follows the curve of the dome. Nothing is drawn below the
  dome top: the visor, eye slit and grille are untouched.
- Size: 12 px tall in the calm poses, 15 px in the mad poses (the front spike splays forward, the back spike back).
- Colors: `B8F818` lit, `00A800` shade on the right side of each spike (light from the upper left), `000000` outline.
  `006800` is the shade of a dim (flickering) spike in `dying`, `collapse` and `dead`. The ball's `005800` is not used.
- Edges step at most 1 px per row, except the bent tips of the drooped spikes.

## Per-pose behavior

| Strip | Mohawk |
|---|---|
| `stand`, `throw` 3 | calm, 12 px; `throw` 3 leans back 2 px |
| `throw` 1, `mad_throw` 1 | pressed flat under the held ball (6 px), drawn in front of the ball's bottom edge |
| `throw` 2, `mad_throw` 2 | head is tucked behind the ball: 2 small spikes show at the left of the ball |
| `intro_drop` | mad size, streams up in the fall |
| `intro_land` | squashed (6 px); 2 px taller on the rebound; calm |
| `intro_roar` | calm; rises; mad size on the 2 roar cells, 1 px wobble; down; calm |
| `chest_beat` | calm; rises; mad size and leaning back 3 px with the fists high, shaking forward on each chest hit; mad |
| `mad_stand`, `mad_throw` 3 | mad size, the tips tremble 1 px between the 2 cells |
| `ball_hit`, `mad_ball_hit` | squashed; whipped forward 4 px as the head snaps back; flops back with the back spike bent over; flops forward; middle spike bent over, front spike hooked |
| `dying` | one or two spikes bent over in each cell, and one spike dim (`00A800` / `006800`) in cells 2, 3, 4, a different one each time |
| `collapse` | droops; trails up and back as the head drops; whips forward; squashed on the floor hit; bounce; flat |
| `dead` | 3 px flat teeth lying back, front half dim |
| `*_damaged` | round 3 map, and every green becomes `FCFCFC`; the outline stays black |

## Notes

- Nothing is cut off: the highest mohawk pixel is 52 px or more below the cell top (`chest_beat` 3, 5, 7). No frame was shifted.
- Sparks of `ball_hit`, `mad_ball_hit` and `dying` that the mohawk would touch moved up by a few px (about 40 px in all).
- `throw` 1 and `mad_throw` 1: about 50 px of the ball's bottom edge are now mohawk and outline. The ball in these cells is
  part of the monkey frame, so the code needs no change.
- `ball_hit_damaged`: 7 lime spark pixels of round 3 are white now ("all greens turn white").
- Code hooks: none. If the owner takes the mohawk, `stand`, `stand_damaged`, `throw` and `throw_damaged` in
  `assets/sprites/frames/Bosses1/ReactorMonkey/` must be replaced by the drafts here too.

## check.py result

Cell size, NES palette, allowed colours, no semi-transparent px, feet on the bottom row: all ok for all 20 strips
(`work/check-result.txt`). No green is left in any `_damaged` strip. Strict "3 colours per 8x8" is not met, as in the
current art and in round 3 (35 to 45 % of the areas of the coloured strips).
