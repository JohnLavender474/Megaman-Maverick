# Reactor Monkey art drafts: shared brief (branch `mega-refactor-077h-reactor-monkey-art`)

Owner request (2026-11-26): the Reactor Monkey mini boss (Reactor Man's stage) gets "phases". Today the fight is one loop:
he stands, catches a radioactive ball, throws it, repeats. He has no intro animation and no dying animation.
New: when he gets mad he beats his chest like an ape, and he has a red mad face with cartoon steam coming out of his ears.
We need DRAFT art for the new animations. Drafts only: this is a first pass the owner reviews from a phone.

## Read first (repo: /home/claude/megaman-maverick, main checkout, branch master)

- `docs/ways-of-working/art.md` (NES pixel size, NES palette, no rotated pixels, one pixel grid, credit rules)
- `docs/ways-of-working/temp-files.md`, `docs/ways-of-working/claude-web.md`
- Current frames: `assets/sprites/frames/Bosses1/ReactorMonkey/{stand,stand_damaged,throw,throw_damaged}.png`
- Contact sheets of them at 3x: `temp/077h-reactor-monkey-art/ref/` (look at these images first)
- Ball: `assets/sprites/frames/Projectiles1/ReactorMonkeyBall/` and `ref/ball_x4.png` (green radioactive orb)
- Code that plays the frames: `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/ReactorMonkey.kt`

## Format of the existing art (match it)

- One PNG per animation: a vertical strip of 160 x 160 px cells (`stand` = 2 cells, `throw` = 3 cells).
  A new strip may use a wider cell ONLY if a pose cannot fit in 160 x 160; say so in your notes.
- The art faces LEFT. The code flips it when he faces right.
- The cell is drawn at 320 x 320 game px, so 1 art px = 1 NES pixel (2 game px). Single art px, no sub-pixel detail.
- The sprite is anchored at BOTTOM_CENTER of the body (the code shifts it 16 game px away from the facing side).
  Keep his feet on the bottom row of the cell and the body mass at the same horizontal place as in `stand`, so that
  frames of different strips can swap without a jump. Compare each new frame to `stand` frame 1.
- Colors today: `000000` outline, `503000` brown shade, `940084` dark magenta, `D800CC` magenta, `FCFCFC` white (visor glint).
- Each `_damaged` strip is the same frames with a mechanical color swap: D800CC and 940084 become FCFCFC, 503000 becomes 000000, 000000 stays.
  Make a `_damaged` copy of each new FIGHT-state strip (not of intro or death strips) with a script that applies that map;
  a Sonnet subagent may do that step.

## Rules that hold for all new art

- NES (2C02) palette only (list in `art.md`). Sprite palette rule: each 8x8 area of a frame uses at most 3 colors plus
  transparent; an entity uses at most 2 sprite palettes. The monkey body is one palette (outline black, 503000, 940084,
  D800CC or similar). A second palette is for effects (red face, steam, sparks, glow). Check by script.
- No rotated pixels: no free-angle turns. Draw each pose as pixel art with stepped edges.
- Every edge steps at most 1 px per row. Keep the chunky, heavy robot-gorilla look of the current frames.
- Fix problems in the art, not with offsets. No code. Do not touch `assets/` or any tracked file.
- Do not launch the game. Do not commit. Write only inside your own output folder (below).
- Do not guess the name of any original artist. Credit line for the model is "Claude Fable 5.2, an AI model by Anthropic,
  directed by the developer".

## Deliverables (in your output folder)

1. The strips as PNGs, final names, plus `_damaged` copies where the brief says.
2. `contact-sheet.png`: all frames at 3x on a grey (`5A5A6E`) background, labeled by strip and frame number,
   next to the current `stand` frame 1 for scale comparison.
3. One animated GIF per animation at the intended timing, 3x, grey background, looping (for the death and intro: play once, then hold the last frame ~1 s).
4. `notes.md`: for each strip the frame count, the intended seconds per frame, loop or one-shot, what each frame shows,
   what the code must do (for example "spawn steam puffs at these ear points", "hold last frame"), and any deviation
   from this brief. Keep it under one page. The owner skims.
5. A script `check.py` result in the notes: palette ok, per-8x8 color counts ok.

Draft quality bar: readable at the real size (about 1 tile = 16 art px; the whole monkey is about 5 tiles tall in the game),
strong silhouettes, a clear read of each state. Iterate on your own until the contact sheet reads well; look at your own
output as an image before you report. Final report to the main session: at most 120 words (output folder path, anything the
owner must decide).
