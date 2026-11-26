# Main menu title entrance: options (item 187, for 187c)

Draft for review. Nothing in the repo is changed. All files are in `temp/187a-title/`.

## 1. The current title

- Region `TitleScreenBackgroundv4` (UI_2 atlas; frame `assets/sprites/frames/Ui2/TitleScreenBackgroundv4.png`) is **256x256**, not 256x224. `MainMenuScreen` draws it at 512x512, centered on the 512x448 view. So the top 16 rows and the bottom 16 rows (NES px) are cut off. The visible frame is image rows 16 to 239. One image px = one NES px = 2x2 game px, so the art is already on the NES grid.
- Contents: a black backdrop, the title (image rows 26 to 85, so screen y 10 to 69), and the hero figure (rows 97 to 255, with the feet cut off by the crop). The title and the hero do not touch: rows 86 to 96 are all black. So the screen needs **no background repaint** to remove the title.
- The title has three parts:
  - "MEGAMAN": 7 italic letters in bands of `0070EC`, `1AD7F4` and `FCFCFC`. Each letter has a black outline and a down-left drop shadow, 1 to 4 px thick.
  - A red swoosh: a `D82800` wedge behind both words.
  - "MAVERICK": 8 letters with a red top, a salmon `FA9687` body, white bands and a 1 px black outline.
- Title colors: only `FCFCFC` is in the NES master palette. `0070EC`, `1AD7F4`, `D82800` and `FA9687` are not. The old art keeps its look (`art.md`), and every option keeps these colors exactly.
- The hero uses 8 more colors, which include the near-duplicates `0071EF`/`0070EC` and `FFFFFF`/`F8F8F8`/`FCFCFC`. The options do not change the hero.

### The cut (`src/cut.py` makes `letters/`)

| Layer | How it was cut |
|---|---|
| `plate.png` (256x256) | The original with rows 0 to 89 set to black: the hero on black. This is exact, because nothing else lies behind the title. |
| `swoosh.png` | The visible red parts, plus a **reconstructed** shape behind the letters. The left edge is x = y - 33 (measured on both visible ends). The right and lower edge is measured where it shows, and interpolated where letters hide it (rows 43 to 51 and 66 to 77). |
| `top_1_M` ... `top_7_N` | The 7 connected parts of the blue colors. Each gets the black outline pixels that are over the swoosh and nearest to it. It also gets a black halo of radius 3 on pixels that are black in the original. |
| `bot_1_M` ... `bot_8_K` | The non-black, non-swoosh parts in rows 66 to 86, merged by column range. Each gets the same outline rule, with a halo of radius 1. |
| `manifest.json` | The home x and y of each cut-out in the 256x256 image. Screen y = image y - 16. |

- The layers composite back to the original **pixel for pixel**. The script checks this, and the result is `composite == original: True`.
- What could not be separated cleanly:
  1. **The swoosh behind the letters is a guess.** It is a smooth fill between the visible edges. It shows only during the animations, while letters are away from home. An art agent should confirm it or redraw it.
  2. **The letters' black outlines exist in the art only where they lie over the swoosh.** Over the black backdrop they cannot be seen, so the halo there is made up (radius 3 or 1). It shows only when a letter passes over the swoosh or over another letter. The black gaps between neighboring letters over the swoosh go to the nearest letter. Some letters therefore have an uneven chunk of outline on one side, for example the right side of `top_2_E` (see `letters_preview_swoosh.png`).
  3. Single red pixels inside "MAVERICK" are classed as letter fill, not swoosh, because they touch the salmon border. One swoosh pixel at (87, 70) touches salmon. This does not change the result.
- A cleaner long-term source: the art agent redraws the title as layered source art: a full swoosh, 15 letters with a full outline and shadow, and the plate. The same step can fix the 16-row crop: a 256x224 frame, or a draw size of 512x448.

## 2. The options (GIFs in this folder, key frames in `options_contact_sheet.png`)

All options step at 20 fps (50 ms, one step per 3 NES frames at 60 Hz) and use whole NES pixels only. Nothing is turned. Each GIF starts with the empty plate (300 ms), then runs the animation, then holds the formed title for 1 s and loops. The menu text is left out.

| Option | What happens | Length |
|---|---|---|
| **A. Staggered fly-in** (`option_A_staggered_fly_in.gif`) | The swoosh slides in from the right (6 steps). The MEGAMAN letters race in from the left, last letter first, so a flying letter never crosses a landed one. The MAVERICK letters come in from the right, first letter first. Each letter eases out, overshoots 4 px, settles back at 1 px and then 0, and flashes white for 1 frame when it lands. | 2.0 s |
| **B. Block assembly** (`option_B_block_assembly.gif`) | The swoosh is cut into 8x8 blocks and the letters into 4x4 blocks (890 blocks). They fly home from off-screen above and to the sides, in a left-to-right wave. The swoosh comes first, then MEGAMAN and MAVERICK. Outline-only blocks do not fly; they appear when they land, so that no black "holes" cross the swoosh. The whole title flashes white for 1 frame at the end. | 1.6 s |
| **C. Drop and bounce** (`option_C_drop_and_bounce.gif`) | The swoosh slides in from the left. The MEGAMAN letters fall from above the screen, one every 3 steps. They fall under gravity (quadratic), then hop back up by 5, 6, 4, 0, 2 and 0 px. Small 2x2-px sparks (`FCFCFC` and `FCA044`) burst outward from both bottom corners on impact. The heavy letters (M, M, N; M and K of MAVERICK) shake the screen by 2 px, then by -1 px. The MAVERICK letters fall *behind* MEGAMAN, so they never cover it. | 2.8 s |
| **D. Beam-in** (`option_D_beam_in.gif`) | The swoosh is revealed by a left-to-right scan with a 2 px white leading edge. Then each letter "teleports" in. A 3 px beam drops from the top of the screen to the bottom of the letter in 2 steps (`1AD7F4`/white for MEGAMAN, `FA9687`/white for MAVERICK). Then the letter opens from its center column outward (half-width 1, 3, 6, 10, 15, full), with white columns at the edges of the band. The title ends with a palette flash in the style of Mega Man 4: white, normal, white, normal. | 2.3 s |
| **E. Raster weave and palette warm-up** (`option_E_raster_weave_palette.gif`) | The whole title is cut into 15 horizontal strips, 4 rows each. The strips slide in from alternating sides (even strips from the left, odd from the right), one step apart, like an NES mid-frame scroll split. The title starts as a two-tone blue silhouette (NES `0000BC`/`0058F8`). It warms up through an NES mid step (`0058F8`, `0078F8`, `3CBCFC`; `A81000`, `E45C10`, `F87858`) to full color, then flashes white for 1 frame. | 1.6 s |

## 3. How each would be built

### Shared parts (all options)

- **Package:** `core/.../screens/menus/title/`. It holds `TitleAnimation` (screen-facing: `reset()`, `update(delta)`, `draw(batch)`, `skipToEnd()`, `formed`) and the data of each option (`TitleAnimationDefs`). The step clock and the track types are generic, so they go in `core/.../screens/utils/timeline/` for use by `187b` (see below).
- **Step clock (fixed whole steps):** the screens get the variable `Gdx` delta; only `WorldSystem` runs a fixed step. So the clock does `acc += delta; while (acc >= STEP && step < lastStep) { acc -= STEP; step++ }`, with `STEP = 3f / 60f` (20 Hz). On a long hitch it jumps through the missed steps (a cap of about 10, then it snaps). Motion is read from integer tables by `step` and is never interpolated, so the stepped NES motion stays exact at any FPS, and the `--interpolation` mode does not touch it.
- **Data:** each track is `region` + `homeX`/`homeY` (NES px, from `manifest.json`) + `startStep` + `IntArray` offset tables (`dx`, `dy` per step) + optional `IntArray` flash steps or clip tables. The tables are precomputed when the def is built (the Python here computes the same numbers; the script can export them as JSON if the owner wants O7 "content from data" now).
- **Draw:** in `MainMenuScreen.draw`, in the same batch: the plate (in place of `background`), then the title layers in a fixed order (swoosh, MEGAMAN, MAVERICK), then `fontHandles` and the arrow only once `formed` is true. Every position is `nesPx * 2` in UI units. Screen y = `(224 - (imageY - 16) - h) * 2`, because LibGDX's y axis points up. Keep the 512x512 draw of the plate so that the crop stays the same.
- **Input (recommended):** the menu cannot be used during the entrance.
  - Any just-pressed menu button (START, A, B, SELECT) calls `skipToEnd()`, and that press is consumed. It does not select.
  - `getNavigationDirection`, `selectionRequested` and `backRequested` gain `titleAnimation.formed &&`, as they already do with `screenSlide.finished`.
  - This keeps a skip to one press and makes a selection by accident impossible.
- **`screenSlide` and `show()`:** the title is drawn in UI coordinates, so it moves with the settings slide like the background does today, and needs no change there. `show()` calls `titleAnimation.reset()` next to `screenSlide.reset()`. Reset sets the step and the accumulator to 0; it allocates nothing, because the tracks are immutable data. If the owner wants no replay when returning from a sub-screen, `show()` calls `skipToEnd()` instead, based on a flag set by the opening screen or by the boot (see the questions).
- **Shared with `187b` (story timeline):** `Timeline` = a list of `Beat(startStep, lengthSteps, kind)` on the same step clock. The kinds are `Move(track, dxTable, dyTable)`, `Reveal(track, clipTable)`, `Flash(track or group, steps)`, `PaletteVariant(group, variantRegion, steps)`, `Shake(table)`, `Sound(asset)`, `Text(...)` and `Wait`.
  - The title entrance is one `Timeline`, and each story slide is another.
  - A screen owns its timeline and calls `update` and `draw`, so one system serves both.
  - The `Sound` beat makes landing sounds free later.
- **Per-frame cost (O5-R1):**
  - All regions, tables and scratch `TextureRegion`s are made in `init()`.
  - `update` and `draw` loop by index over arrays, with no lambdas that capture, no `Vector2`, and no string keys.
  - A clip is `scratch.setRegion(region, srcX, srcY, w, h)` on a preallocated region, which does not allocate.
  - All frames are in one atlas, so the whole title is one texture bind.
- **Flash:** prefer a white copy of each layer, made by a script (a mechanical recolor to `FCFCFC`), packed next to it. There are 16 small regions, and no shader is needed. A flash shader (`mix(color, white, u_flash)`) is the alternative if the atlas must stay small.
- **Packing:** the frames go in `assets/sprites/frames/TitleLetters/` (new) or `Ui2/`, and the atlas is repacked with TexturePacker `Basic`. TexturePacker is not installed in the cloud, so this is a laptop step: put the PNGs in the frames folder, repack `Ui2` (or a new `TitleLetters` atlas plus a `TextureAsset` entry), and commit the `.png` and `.txt`. The cloud branch can carry the frames and the code, with the packed atlas added on the laptop.

### Per option

| | Art and assets | Code beyond the shared parts | Effort and risks |
|---|---|---|---|
| **A** | The plate, the swoosh and the 15 letters from `letters/`, plus 16 white copies. A Fable agent checks the swoosh guess and the uneven outlines. | One `Move` track per letter, with the overshoot table, and 1 flash step each. About 150 lines. | Low. The look is clean. Each letter must be fully outlined, or it looks cut over the swoosh while in flight. |
| **B** | The same frames as A. The blocks are `TextureRegion` sub-rectangles of the letter regions (890 of them, made once in `init`). No new art. | Structure-of-arrays `IntArray`s (home, start offset, start step, solid flag), one ease table, and 890 draws in one batch, which costs little. About 200 lines. | Medium. On a phone the look is busy, and it reads less as NES and more as "particles". The random seed must be fixed so the effect is the same every time. |
| **C** | The same frames, plus a 4-frame spark strip (small: a Fable agent draws it, NES colors). | Move tracks with a gravity table and a bounce table, spark tracks, and a `Shake` beat. The shake offset is added to every draw (the plate too), not to the camera, because `ScreenSlide` owns the camera. About 220 lines. | Medium. At 2.8 s it is the longest, and the shake can feel heavy on a menu that the player sees often. The drop order (MAVERICK behind MEGAMAN) must be kept. |
| **D** | The same frames, plus white copies. The beam is one 3x1 px region in each word's colors, stretched in height. A column of one color stretched vertically is still square NES pixels, not turned pixels. | `Reveal` tracks: a clip table of half-widths, drawn with `scratch.setRegion`. The leading edge is one column of the white copy. A `Flash` beat for the whole title. About 200 lines. | Low. It fits the series (the teleport beam). The palette flash ending can be shared with `187b`. |
| **E** | **No letter cut is needed.** The strips are sub-rectangles of the existing `TitleScreenBackgroundv4` region, so the current art and atlas are enough for the motion. The palette steps need 2 recolored copies of the 256x60 title band, plus a white copy. That is a mechanical recolor (a Sonnet subagent can apply a palette that a Fable agent picked), or a palette-swap shader. The plate is the same region drawn from row 90 down. | 15 `Move` tracks (one per strip) and 3 `PaletteVariant` beats. About 120 lines. | Lowest. The strips stay in the title rows and never cross the hero. The letters do not "come together" as letters (the strips do), so this is the weakest literal match to the brief. |

Who does what:
- A Fable art agent (one round): confirms or redraws the swoosh and the outlines, and draws the sparks for C or picks the palette steps for E.
- A Sonnet subagent: the white and recolored copies.
- A code branch (`187c`): the timeline, `TitleAnimation`, the `MainMenuScreen` wiring and the packing step on the laptop.

## 4. Verification (`src/verify.py`, read back from the GIF files)

| GIF | KB | Frames (after Pillow merges same frames) | Durations | Final == current title | 2x nearest | Colors |
|---|---|---|---|---|---|---|
| A | 113 | 41 | 300 lead, 39 x 50, 1050 hold | yes | yes | 13: the original art only |
| B | 107 | 32 | 300, 50/100, 1000 | yes | yes | 13: the original art only |
| C | 200 | 52 | 300, 50 x 50, 1300 | yes | yes | 14: original + NES `FCA044` |
| D | 99 | 46 | 300, 50/150, 1000 | yes | yes | 13: the original art only |
| E | 60 | 26 | 300, 50/150/250, 1000 | yes | yes | 20: original + NES `0000BC 0058F8 0078F8 3CBCFC A81000 E45C10 F87858` |

- All GIFs are 512x448. Every frame is an exact 2x nearest copy of a 256x224 frame, so there is no anti-aliasing.
- The first frame equals the empty plate. The GIFs loop forever.
- The last frame equals the visible 256x224 of `TitleScreenBackgroundv4` pixel for pixel.
- No color is outside the NES master palette and the original art.
- The "original art" colors are the 13 in the image. The title's own 5 colors include 4 that are not NES colors, as noted in section 1.

## 5. Comparison and recommendation

| | Look | Art effort | Code effort | Risk | NES fit |
|---|---|---|---|---|---|
| A fly-in | clean, lively | low (cut done) | low | low | good |
| B blocks | busy, "particles" | low | medium | medium | fair |
| C drop/bounce | playful, heavy | low + sparks | medium | medium (length, shake) | good |
| D beam-in | on theme, clean | low + white copies | medium | low | very good |
| E weave/palette | retro raster | lowest (no cut) | lowest | low | very good, but not letter-by-letter |

**Recommendation:**
1. **D (beam-in)** as the main entrance. It is the most Mega Man (teleport beam, then a palette flash), it reads well on a phone, and its timeline beats (reveal, flash) are the ones `187b` needs anyway.
2. **A** is the second choice if the owner wants motion over effects.
3. **E** is the fallback if no new art is wanted: it ships with the current atlas plus two small recolored bands.

## 6. Questions for the owner

1. Should the entrance play on every visit to the menu (also when backing out of Credits, Extras or the difficulty screen), or only after the story screen and the boot? The recommendation is only after the story and the boot. On a return, the title is formed at once.
2. Input during the entrance: is "any button skips to the formed title, and the menu appears after" what you want (recommended), or should the menu be usable while the title is still forming?
3. Is the reconstructed swoosh behind the letters acceptable? Or should the art agent redraw the title as layered source art, and at the same time fix the 16-row crop and the 4 title colors that are not NES colors? Recolored colors would change the "formed" look, which this task keeps exactly.

## Files

- `letters/`: the cut-outs and `manifest.json`.
- `letters_preview_all.png`: all layers on grey.
- `letters_preview_swoosh.png`: the full swoosh, with 2 letters.
- `option_*.gif` and `options_contact_sheet.png`: the 5 options.
- `src/`:
  - `cut.py`: the cut and the composite check.
  - `common.py` and `options.py`: the frames, GIFs and contact sheet.
  - `verify.py`: the checks.
  - `strips.py`: a debug sheet of all frames. It needs `options.py` run first.
  - `analyze.py`: the first analysis.
