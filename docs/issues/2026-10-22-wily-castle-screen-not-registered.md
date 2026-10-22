---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [008]
---

# `WILY_CASTLE_SCREEN` is in `ScreenEnum` but not registered

## Where

`MegamanMaverickGame.kt:381-398`

## What happens

`setCurrentScreen(WILY_CASTLE_SCREEN)` finds no screen.

## What should happen

Register it, or remove the enum value.

## How to reproduce

Read the code.
