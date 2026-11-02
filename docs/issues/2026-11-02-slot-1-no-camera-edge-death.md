---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: 048 research (2026-11-02)
related-items: [047, 051, 120, 121]
---

# With `--players 2`, only slot 0 dies past a camera edge

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt:333`. `player` is slot 0 (`:181`).

## What happens

The `CameraEdgeBlocks` callback tests `isPastCameraEdge` only for `player`, which is slot 0. Slot 1 can be past the edge of a scroll section and not die.

The owner's rule (`120`) is that each cause of death applies to each playable character. This site does not meet it. All other kills that loop over players include slot 1.

## What should happen

The callback tests each slot. The site needs a loop over the slots, and no new API.

## How to reproduce

Run a map with a scroll section with `--players 2`, and keep slot 1 past the trailing edge while slot 0 stays in view.
