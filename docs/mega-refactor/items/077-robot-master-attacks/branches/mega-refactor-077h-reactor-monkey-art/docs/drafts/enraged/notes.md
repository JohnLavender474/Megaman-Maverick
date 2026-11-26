# Reactor Monkey: enraged-phase drafts (round 1)

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Built from the current `stand`/`throw`
frames (same pixels for body, legs, tail; new arms, face, mouth). All cells 160 x 160, vertical strips, facing LEFT, feet on
the bottom row. Rebuild: `python3 work/build.py && python3 work/sheet.py`.

| Strip | Cells | s/cell | Loop | Frames |
|---|---|---|---|---|
| `chest_beat` | 8 | 0: 0.1, 1: 0.1, 2: 0.3, 3-6: 0.07, 7: 0.12 | one-shot; loop 3-6 (about 4 times) | 0 `stand` 1 with red visor eyes; 1 rises to a crouch, head now red; 2 upright, both fists high, roar; 3 left fist hits chest (white impact marks); 4 both fists out; 5 right fist hits; 6 = 4; 7 crouch again, then `mad_stand` |
| `mad_stand` | 2 | 0.08 | loop | red head, slanted red eyes, bared grille teeth, body 2 px lower; cell 1 = upper body 1 px left (tremble) |
| `mad_throw` | 3 | 0.1 | one-shot | `throw` with the mad face (in cell 1 only the small head top shows, as in `throw`) |
| `steam` | 5, 32 x 32 | 0.09 | one-shot | puff grows, rises, breaks up, fades to grey bits |
| `steam_jet` | 3, 16 x 16 | 0.06 | one-shot | short jet, long jet, puff |
| `mad_stand_damaged`, `mad_throw_damaged` | 2, 3 | as above | | brief's map plus F83800 and A81000 -> FCFCFC |
| `ball_mad/0..3.png` (optional) | as `ReactorMonkeyBall` | same | same | red orb (005800 -> A81000, 787878 -> F83800); flash trefoil F8B800, FCA044, FCFCFC |

## What the code must do

- Enrage: play `chest_beat` once (cells 0-2, loop 3-6, then 7), shake the screen on cells 3 and 5, then use `mad_stand` /
  `mad_throw` in place of `stand` / `throw`.
- Steam: the art draws the LEFT ear's steam; flip it horizontally for the right ear.
  - `steam`: origin = cell px (16, 31) (bottom center), about 8 px outward and 8 px up from the ear point (see the demo GIF).
  - `steam_jet`: origin = cell px (15, 15) (bottom right corner) on the ear point; it shoots up-left.
- Ear points (art px from the cell's top-left, art facing left; left ear, right ear):
  - `mad_stand` 0: (45, 89), (66, 89). Cell 1: (44, 89), (65, 89).
  - `mad_throw` 0: (65, 81), (86, 81). Cell 1: (88, 63), (101, 63). Cell 2: (42, 98), (63, 98).
- `demo_mad_stand_with_steam.gif` shows one possible timing (jet, then puff, every 0.8 s).

## Check (`python3 check.py`)

- Palette: every color of every file is in the NES list (non-NES = none). The current ball flash frames use F86A00 and
  F80000, which are not NES colors; `ball_mad` replaces them.
- Colors: body = 000000, 503000, 940084, D800CC (as today); effects = A81000, F83800, FCFCFC.
- Per-8x8 rule (3 colors plus transparent): NOT met, and the current art does not meet it either: `stand` has 75 of
  220 blocks over 3 colors (max 5), `throw` 159 of 435. New: `mad_stand` 78 of 214 (max 6), `mad_throw` 162 of 435,
  `chest_beat` 325 of 814. `steam` and `steam_jet` meet it (3 colors). A strict fix means a redraw of the body.

## Deviations and open points for the owner

- The whole head is red (not only the visor slit), because it reads at a glance. A visor-only version is a small change.
- The transitions `stand` -> crouch -> upright pop between poses (as `stand` -> `throw` does today); there is no in-between.
- "Head thrown back" is drawn as a head that lifts 3 px with a wide open mouth, not as a tilted head.
- `mad_stand` has 2 cells; the stance is 2 px lower than `stand`.
