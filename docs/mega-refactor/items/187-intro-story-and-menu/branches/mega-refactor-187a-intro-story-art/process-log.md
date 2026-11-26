# Process log: mega-refactor-187a-intro-story-art

## Status

- State: active
- Goal: First round of NES-style art for the intro story slides: the city by day, dusk and night, and the exploding city with a billboard screen and a screen carried by a flying Met, as exported layers.
- Done: item `187` and this folder created on `master`; the art, the previews and the style guide added; draft PR opened (brief: in chat, `claude-web.md`).
- Next: the owner's review of the PR; then a round that converts the art to the NES master palette and draws slides 2, 3, 4, 6 and 7.
- Blocked:

## Entries

## 2026-11-26

- Item `187` created from the owner's request. Branch started from `master` (`2189b7a`). No code changes: art and docs only.
- Art added from the Fable rounds (the first stills were redrawn in the NES style from the owner's reference sheets; then the city, the explosions, the screens and the Met). 19 PNG files in `assets/sprites/frames/IntroStory/`, the GIF and strips in `docs/previews/`.
- Art checks (script in the session, not committed): sizes and 2x copies exact; no semi-transparent pixels; the text band of the burning city is black. **Palette against the NES master palette of `art.md`: not met.** Ten colors are outside it (`0070EC`, `183C5C`, `4CDC48`, `7C0800`, `D82800`, `E0F8A0`, `F0BC3C`, `FC7460`, `FC9838`, `FCE4A0`); they come from the ripped reference sheets. The rule of 4 colors for each 16x16 area was not met either in an earlier round (about 8% of the cells of the day city and 13% of those of the burning city had more than 4 colors); it was not measured again on these final files.
- Not run: the Gradle checks (the wrapper cannot download Gradle in the cloud, `claude-web.md`); CI of the push is the automated check.
