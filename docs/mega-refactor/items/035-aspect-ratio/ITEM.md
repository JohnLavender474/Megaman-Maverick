# 035: Aspect-ratio policy for phones

- Status: planned
- Type: research
- Depends on: none
- Related: `019`, `031`, `032`, `034`, `036`, `054`
- Branches: none yet

## Goal

Decide how the fixed 16×14 view works on wide phone screens, without separate level designs.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- The review recommends: the gameplay view stays 16×14 tiles on all platforms, with pillarbox bars on wide screens. No separate mobile release.
- Touch controls (the existing `ScreenController`) go in the side bars. Hide them when a gamepad connects.
- Optional border art and HUD in the bars.
- Keep the view size in one place, and keep spawn and checkpoint rules independent of the camera size, so a wider view stays possible later.

## Owner's input

Answer to review item 34 (2026-10-22, verbatim):

> this is tricky since so much of the level design is oriented around a fixed aspect ratio. should I plan on having a different "mobile-friendly" release of the game for Android? It would have altered level designs etc. in order to accomodate the different aspect ratio and other diffs. What do you think?

## Decisions

- J (2026-10-22): commit to the 16×14 view for now. Do not couple platform-agnostic code to that size: the view size comes from config, and spawn, cull and checkpoint rules do not depend on the camera size. A later Android version with smaller screens and rooms must stay possible.

## Open questions

- Integer scaling (sharp pixels, smaller image) or fill the height?
- HUD in the side bars on phones, or in the game view as today?
- Is a tablet (4:3) a target?

## Sources

- `NOTES.md` in this folder.
