---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [012]
---

# A pause during a gate opening turns physics and behaviors on again when the game resumes

## Where

`LevelStateHandler.kt:18`, `:26`; `Gate.kt:64`, `:249`; `MegaLevelScreen.kt:666-678`, `:873`

## What happens

`GATE_INIT_OPENING` turns off World, Motion, Behaviors and both Pathfinding systems. The gate does not lock input, so the player can pause during the 0.5 s opening. `resume()` turns on every system that pause saved, which is all of them. The camera transition then runs with physics on, until `GATE_INIT_CLOSING`.

## What should happen

Resume restores the state from before the pause.

## How to reproduce

Walk into a boss or room gate and press START during the opening. Resume.

## Notes

Item `012` replaces these flags with named freezes.
