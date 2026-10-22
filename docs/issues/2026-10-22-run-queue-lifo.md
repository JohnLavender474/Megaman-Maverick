---
status: open
verified: yes
area: core
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [001, 010]
---

# `runQueue` runs the last added runnable first

## Where

`MegamanMaverickGame.kt:419-421`

## What happens

The queue is drained with `removeLast()`, so runnables run in the reverse order of submission.

## What should happen

Probably first in, first out.

## How to reproduce

Read the code.

## Notes

Check the callers that queue more than one runnable in a frame before a fix.
