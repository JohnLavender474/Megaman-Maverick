---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [037]
---

# Question: coins and level points reset only at level start, not on respawn

## Where

`MegaLevelScreen.kt:476-478`

## What happens

`start()` resets them; the respawn path does not.

## What should happen

Unknown. The owner decides.

## How to reproduce

Read the code.
