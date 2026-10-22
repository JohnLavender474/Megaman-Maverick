---
status: open
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
