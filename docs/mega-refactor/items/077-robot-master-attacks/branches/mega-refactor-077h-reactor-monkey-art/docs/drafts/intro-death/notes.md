# Reactor Monkey: intro and death drafts (round 1)

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Inspired by the current
`stand`/`throw` frames (original artist: not known to me, ask the owner). Rebuild: `python3 work/build.py`.
All strips: 160 x 160 cells, vertical, facing left, feet on the bottom row, no `_damaged` copies.

| Strip | Cells | Seconds per cell | Play | What each cell shows |
|---|---|---|---|---|
| `intro_drop` | 1 | held while he falls | hold | Arms up in a V, legs spread, tail streaming up, mouth open. |
| `intro_land` | 3 | 0.06, 0.08, 0.12 | one-shot | 1 deep impact crouch, tail whipped up. 2 rebound. 3 the `stand` pose, 3 px lower. |
| `intro_roar` | 6 (brief: 5) | 0.12, 0.08, 0.3, 0.3, 0.12, 0.12 | one-shot | 1 rises. 2 full height, mouth open, visor flashes white. 3 and 4 roar (arms and tail shake, mouth 1 px wider in 3). 5 comes down. 6 `stand` pose 2 px taller, then `stand` frame 1. |
| `dying` | 4 | 0.08 each | loop | Leans back and near fist jerks up; sags; lurches forward and far fist lifts; sags. Visor: cracked, lime flicker, dim, lime. Sparks and green seam glow change each cell. |
| `collapse` | 6 | 0.1 x4, 0.12 x2 | one-shot | 1 sags forward. 2 elbows buckle. 3 chest low, rump up, visor dim. 4 chest and head hit the floor, visor dark. 5 rump drops. 6 small settle, tail falls. |
| `dead` | 1 | held | hold | Heap, face down, one arm stretched ahead, tail flat, visor dark. |

GIFs: `intro.gif` (drop, land, roar, then `stand`), `death.gif` (dying x4, collapse, dead), plus one per strip.

## What the code must do

- Intro: show `intro_drop` while he moves down from above the room; on landing play `intro_land` with the screen
  shake and dust at the feet; then `intro_roar` (roar sound at cell 2; an extra shake on cells 3 and 4 is optional);
  then `stand`. In `intro_drop` the feet end 1 px above the bottom row (he is in the air).
- Death: loop `dying` under the explosions, then `collapse` once, then hold `dead` while the alpha fades. If the
  fade must stay as short as today, play only `dying` and skip `collapse`/`dead` (owner decides).
- `dead` and the last collapse cells are wider than `stand` (x 5 to 151 of the cell) but stay inside 160 px.

## Deviations and limits

- `intro_roar` has 6 cells, not 5 (two roar cells so the hold can shake).
- The current art has no reactor on the body, so the flash is on the visor only.
- `intro_land` 3, `intro_roar` 6, all `dying` cells and `collapse` 1 are edits of the real `stand` pixels (rows
  removed or repeated, whole-pixel leans, parts moved). The other cells are new drawings from spheres and
  capsules in the same colours; they are plainer than the old art (less brown rim detail). No rotated pixels.
- Effects palette: `FCFCFC`, `B8F818`, `00A800`.

## check.py result

Size, NES palette, 2 palettes (body `000000 503000 940084 D800CC`, effects), no semi-transparent px: all ok.
Strict rule "3 colours per 8x8" fails as in the current art, because the body palette of the brief has outline
plus 3 colours: new strips 25 to 39 % of areas over 3 colours; current `stand` 35 %, `throw` 31 to 41 %.

```
collapse.png     cells=6 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 151/488 | bottom row used=True
dead.png         cells=1 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 16/62 | bottom row used=True
dying.png        cells=4 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 175/446 | bottom row used=True
intro_drop.png   cells=1 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 34/131 | bottom row used=False
intro_land.png   cells=3 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 91/295 | bottom row used=True
intro_roar.png   cells=6 NES=ok two-palettes=ok semi-alpha=0 8x8 over (body 3 + outline, fx 3)=0 | strict >3 colours in 8x8: 196/679 | bottom row used=True
RESULT: ok
```
