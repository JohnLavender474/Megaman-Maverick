---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [036]
---

# Question: a checkpoint can change silently while Mega is not ready

## Where

`MegaLevelScreen.kt:230-245`

## What happens

During the beam-in or the death animation, the current spawn still changes, but no "CHECKPOINT" text shows.

## What should happen

Unknown. The owner decides.

## How to reproduce

Read the code.
