---
status: closed
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [007, 008]
---

# `replace("\\s+", "")` treats the pattern as literal text

## Where

`GameState.kt:293`, `DesktopLauncher.kt` (the `logLevels` parse)

## What happens

Kotlin `String.replace(String, String)` does not use a regex, so whitespace is not removed.

## What should happen

Use `replace(Regex("\\s+"), "")`.

## How to reproduce

Read the code. A save string or a `--logLevels` value with spaces is not cleaned.

## Notes

Items `007` and `008` touch both places.

## Progress

- 2026-10-22: the `DesktopLauncher.kt` part (`--logLevels`) is fixed in `mega-refactor-008a` (squash
  `c731cf9ab`). `GameState.kt:293` is still open (item `007`).

## Short contract

Fixed in branch `fix/small-bugs-3`. The short contract is in `2026-10-28-pathfinding-component-keeps-old-path.md`.

## Resolution

Fixed in `fix/small-bugs-3`, squash commit `c52ce18f6` (2026-11-16). All 9 `replace("\\s+", "")` calls in `core`
use `Regex("\\s+")`: `GameState.fromString`, `LightSourceUtils`, `ElecDevil`, `DesertMan`, `BulbBlaster`, `Block`
(3 calls) and `Laser`. `GameStateSaveStringTest` reads a save string with whitespace. No live map has whitespace in
these values, so no gameplay change.
