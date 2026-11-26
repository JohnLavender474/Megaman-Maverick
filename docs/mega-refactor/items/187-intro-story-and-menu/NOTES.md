# Notes: 187 Intro story screen and main menu overhaul

Research for this item. Based on `master` commit `2189b7a` (2026-11-26).

## Current state

- `core/.../screens/other/OpeningScreen.kt`: seven slides of text (3 lines at most) with these hold times: 3, 2, 3, 1, 3.5, 2 and 3 s. Music `MM5_INTRO_MUSIC`. On completion it opens `MAIN_MENU_SCREEN`.
- `core/.../screens/other/AbstractStoryScreen.kt`: each slide fades in from black for 1 s, holds, and fades out for 1.5 s, so the screen runs about 35 s. START skips it. The text is `MegaFontHandle`s centered on the view. There are no images.
- `core/.../screens/menus/MainMenuScreen.kt` (312 lines) is static.
- The view is 16x14 tiles of 32 px (512x448), which is the NES screen of 256x224 doubled (`art.md`).

## Reference analysis (2026-11-26)

Three NES Mega Man intro sheets and a Mega Man 10 sheet that the owner gave were studied by a Fable agent. The full analysis is `docs/STYLE_GUIDE.md` in the branch folder of `187a`. The findings that matter:

- Flat colors only. The sky is two flat colors joined by a short banded bar. There are no gradients and almost no dither.
- Black is a structural color: 1 px outlines, whole shadow sides, window frames, ground masses (about 12% of the day city and 38% of the night city).
- One drawing, many palettes: the seven skyline variants differ only in their color tables. The day, dusk and night cities of `187a` do the same.
- The tall skyline uses 6 colors by day, 5 at dusk and 3 at night.
- Cutscene panels are 256 px wide and put the characters first, on a flat color.
- The bottom 64 rows of the 224 are black, and hold white 8x8 capital text (24 characters per line, 3 lines).
- The explosion of the Mega Man 4 intro is a white dome with thin arcs that peel off, in about 8 to 10 frames.

## Proposed picture for each story slide

| Slide | Text | Picture |
|-------|------|---------|
| 1 | In 20XX, Mega Man had saved the world countless times | Tall city by day, panned upward (done in `187a`) |
| 2 | Dr. Light had been dreaming | Workshop at night |
| 3 | New designs, new parts, a vision | Blueprint montage |
| 4 | But then... | Alarm flash, a hard cut with no fades |
| 5 | The world was thrust into chaos | The same city, exploding (done in `187a`) |
| 6 | Mega Man must face his nemesis | Hero close-up |
| 7 | Even as a Maverick in the eyes of the world | Hero on a rooftop in rain, in front of the screens |

Timing notes: slide 4 holds 1 s but spends 2.5 s on its fades, so it needs a cut. Crossfades fit better than a fade to black between each slide.
