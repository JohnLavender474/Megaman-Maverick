# 187: Intro story screen and main menu overhaul

- Status: active
- Type: feature
- Objectives: `O9`
- Depends on: none
- Related: `082`, `185`, `183`
- Branches: `mega-refactor-187a-intro-story-art` (active, 2026-11-26)

## Goal

Give the game a first impression with the look of an NES Mega Man intro. Today the opening "story" screen (`OpeningScreen`) is seven slides of white text on black, and the main menu (`MainMenuScreen`) is a static picture. The item adds pictures and animation to the story screen, and animation and flair to the main menu.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. A proposed split:

- `187a` (art only): the stills and animations for the story slides, drawn in the NES style, with the layers exported so that the game can animate them apart. First round: the city by day, dusk and night, and the exploding city with its two screens.
- `187b` (code): the story screen shows a picture behind each slide, with pans, crossfades and a palette flash, driven by a timeline (a list of beats: picture, motion, text, sound, wait).
- `187c` (code and art): the main menu: a parallax background, a logo entrance, an idle animation of the hero, a cursor, menu items that slide in, an attract mode after an idle time, and a transition into the game.

Ideas, not decisions: the same timeline could drive boss intros and the ending later; the screens of the story are all in the 256x224 NES frame, so the 2 px art grid of `art.md` holds.

## Owner's input

- 2026-11-26: "Mega Man Maverick new mega-refactor item: The intro "story" screen and the main menu need an overhaul. The "story" screen is just text while the main menu screen is static. Let's come up with some cool ideas on what images/animations to include in the story screen and how to add some flair and animations to the main menu screen."
- 2026-11-26: the first stills "have no Mega Man look and feel". The owner gave reference sheets of NES Mega Man intro art (city skylines, a fortress, character panels, a Mega Man 10 city with two screens). It is okay to depict Mega Man.
- 2026-11-26: "Create the new branch, update master as needed, then push a new PR."

## Decisions

- 2026-11-26 (owner): the city has no giant robots in the background.
- 2026-11-26 (owner): the burning city has no fortress or spaceship; the city explodes "in a generic fashion", with white round and dome-shaped explosions in the style of the NES intro.
- 2026-11-26 (owner): the cross-shaped antennas on the buildings are removed.
- 2026-11-26 (owner): the floating TV is replaced by two screens: a large billboard screen on a building and a smaller screen that a flying Met carries. The screens are blank white for now (no chart or ticker).
- 2026-11-26 (owner): the art is made in layers (sky, buildings, smoke, explosions, flames, lightning, each screen) so that the game can animate them apart.

## Open questions

- Merge target: `master` directly (recommended: `187a` is art only and safe to ship alone), or a parent branch for the whole item?
- Sources for the credits (`art.md`): the titles and the rippers of the reference sheets. The sheet of the Mega Man 4 intro names its ripper as "Mister Mike", and another sheet names "Rabbid4240"; the Mega Man 10 sheet names none. Which games are the other sheets from?
- The helmet mark of the Met in the repo art is a 3 px plus. The branch draws it as a dash (no cross shapes in the city art). Keep the plus (`KEEP_HELMET_PLUS`)?
- Slide 1 (the city by day) has no billboard building; slide 5 has. Add the building to slide 1, so that both slides show one city?
- The slides with characters (the workshop, the new designs, the alarm beat, the hero close-up, the rooftop) are not drawn yet. Which pose frames of the hero does the owner want, and are the existing hero frames of the game enough to place on the pictures?

## Sources

- `NOTES.md` in this folder.
