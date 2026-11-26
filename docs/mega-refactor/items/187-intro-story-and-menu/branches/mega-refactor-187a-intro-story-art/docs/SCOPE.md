# Scope: mega-refactor-187a-intro-story-art

First-round art for the intro story slides of item `187`. Art only: the game code does not load any of it yet.

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

## Out of scope

- Code: no screen, no `TextureAsset`, no atlas, no loading of the art.
- Slides 2, 3, 4, 6 and 7 (the workshop, the blueprints, the alarm beat, the hero close-up, the rooftop).
- The main menu art.
- The README credits (they are due when the art goes into the game; the draft lines are below).

## Affected areas and risks

- `assets/` grows by the new folder. Both platform bundles hold the whole `assets/`, so the bundles grow by the size of the files. Nothing reads the folder. `generateAssetList` writes `assets/assets.txt` on each build and may list the files.
- The art does not meet every rule of `art.md` yet (see the contract).

## Overlap with other branches

- `185c-games-layout` (active) changes the title seams for screens. This branch changes no code.
- `078h-moon-man-art` (active) adds art in other folders.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` Code only with a production caller | no effect | No code. |
| `O6-R1` CI passes | no effect | Assets and docs only; CI runs on the push. |
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

## Manual playthrough

None: nothing loads the art. Review the pictures in the PR and in the previews.

## Behaviors to check

The game builds and starts as before (nothing reads the new folder).

## Intended changes

New files only: the art and the docs.
