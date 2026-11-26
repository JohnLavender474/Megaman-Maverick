# Scope: mega-refactor-187a-intro-story-art

First-round art for the intro story slides of item `187` (art only: the game code does not load it yet), and the main menu title entrance (the owner asked for it in this branch on 2026-11-26: it has code).

- Status: draft
- Approved: no

## Goal

NES-style pictures for two of the seven story slides, drawn and animated in layers, so that `187b` can show them behind the text. The art follows `docs/ways-of-working/art.md` and the reference analysis in `docs/STYLE_GUIDE.md` of this folder.

## Item

Item `187` (`ITEMS.md`). This branch takes the "art only" part of its proposed scope: the stills and animations. The story screen and the menu code are `187b` and `187c`.

## Current state

- `OpeningScreen` shows seven text slides on black; `MainMenuScreen` is static (see the item `NOTES.md`).
- The art is new: the reference sheets are not in the repo, and nothing in `assets/` is replaced.
- A Fable agent drew the art with Python scripts, in rounds, from the owner's notes. The main session changed a few parameters in those scripts (the removal of the background robots, the explosion shapes, the blank screens). The scripts are not committed (`temp-files.md`).

## In scope

- `assets/sprites/frames/IntroStory/`:
  - `city/`: `story_01_city_day.png`, `story_01_city_dusk.png`, `story_01_city_night.png` (256x448, one drawing in three color tables) and `story_05_city_burning.png` (256x224, the top 160 rows are art and the bottom 64 are black for the text).
  - `exploding_city/`: the layers of the exploding city as RGBA PNGs: static layers, sprite sheets with a JSON file for each animated layer, and `manifest.json` (the layer order, the timing and the palette). The timeline is 144 frames at 20 fps.
- `docs/previews/`: the GIF of the exploding city, a frame strip, the sprite preview of the screens and a contact sheet, for the review of this PR.
- `docs/STYLE_GUIDE.md`: the reference analysis.

- The main menu title entrance (the "beam-in", option D of `docs/TITLE_OPTIONS.md`):
  - `assets/sprites/frames/TitleLetters/` (the title cut into a plate, a swoosh and 15 letters, with white copies), the atlas `assets/sprites/sprite_sheets/TitleLetters.png` and `.txt`, and `assets/sprites/texture_packer/TitleLetters.tps`.
  - `core/.../screens/menus/title/` (`TitleAnimation`, `TitleLetter`, `README.md`), the `TextureAsset.TITLE_LETTERS` entry and the wiring in `MainMenuScreen`.
  - Owner decisions (2026-11-26): the entrance plays on every `show()`; the menu is locked until it ends; the cut of the current image is the source (no redraw).
  - A press of START, A, B or SELECT during the entrance skips to the formed title (my choice, not the owner's: the owner said only that the menu is locked until the entrance ends).

## Out of scope

- Code for the story art: no story screen, no loading of the story art (`187b`).
- Slides 2, 3, 4, 6 and 7 (the workshop, the blueprints, the alarm beat, the hero close-up, the rooftop).
- Other main menu changes (parallax, hero idle, cursor, attract mode, transition).
- The README credits (they are due when the art goes into the game; the draft lines are below).

## Affected areas and risks

- `assets/` grows by the new folder. Both platform bundles hold the whole `assets/`, so the bundles grow by the size of the files. Nothing reads the story art folders; the menu reads `TitleLetters` (about 40 KB). `generateAssetList` writes `assets/assets.txt` on each build and may list the files.
- The art does not meet every rule of `art.md` yet (see the contract).

## Overlap with other branches

- `185c-games-layout` (active) changes the title seams for screens. This branch changes no code.
- `078h-moon-man-art` (active) adds art in other folders.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` Code only with a production caller | no effect | No code. |
| `O6-R1` CI passes | no effect | CI runs on the push. |
| `O6-R2` Named checks | helps | The contract lists the art checks. |
| `O9-R1` Fully playable with an NES-style pad | no effect | |

## Open questions

See the item `ITEM.md` ("Open questions").

## Steps

1. Add the art and the previews to the branch.
2. Check the colors against the NES master palette (`art.md`), the 2 px grid and the text band, and write the result in the PR and in the process log.
3. Push the branch and open a draft PR.

## Draft credit lines

Due in the branch that wires the art into the game (`art.md`, "Credit the original and the update"):

```markdown
- Intro Story Art (city, exploding city, screens)
  - Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer
  - Inspired by Capcom, the intro of Mega Man 4 (sheet ripped by Mister Mike): the city and the explosion
  - Flying Met: the game's own JetMet frames (credited above for the enemies), scaled down and recolored
  - TODO (owner): the titles and the rippers of the other reference sheets
```

# Contract

## Automated checks

- [ ] CI of the push passes (`engine:test`, `core:test`, `core:smokeTest`, jars, `detektMain`). The Gradle wrapper may not run in the cloud (`claude-web.md`): if so, this is not run here.
- [ ] Art script: every PNG has its stated size; the 2x copies are exact; no anti-aliasing.
- [ ] Art script: the colors against the NES master palette (`art.md`). The result is in the PR. A color that is not in the palette is a finding for the next round.
- [ ] Title entrance: a script that re-implements `TitleAnimation.draw` from the packed atlas equals the option D GIF on every step, and the formed title equals `TitleScreenBackgroundv4` (done in the session; result in the process log).

## Manual playthrough

Run `./run-mega-man-maverick-debug.sh`. The art branch's story art is not loaded; check the menu:

1. The title entrance plays when the main menu opens: the swoosh scans in, the 7 MEGAMAN letters then the 8 MAVERICK letters beam in one after another (a beam falls, the letter opens from its center), then the title flashes white twice and is formed. The menu text and the arrow appear only after it.
2. The formed title looks like before (compare with `master`).
3. Pressing up, down, A or START during the entrance does not move or select; a press of START, A, B or SELECT skips to the formed title, and the menu does not select on the release of that press.
4. After the title is formed, the menu works as before (select, back, settings slide, extras, credits).
5. Open Credits or Extras and go back: the entrance plays again (every `show()`).
6. The entrance has the same speed at 30, 60 and 144 FPS (steps of 50 ms).

## Behaviors to check

Everything else in the main menu behaves as on `master`. The story art is not read.

## Intended changes

New art and docs (the story art, the title frames and atlas); the main menu plays the title entrance, hides its text until the title is formed and locks its input until then.
