---
status: open
verified: yes
area: core
gameplay-change: no
found-by: `mega-refactor-079a-smarter-met` (2026-11-18)
related-items: [079]
---

# A desperate `Met` at a pit shows `NoHelmetRun` for 1 frame

## Where

`Met.enterState` (`entities/enemies/Met.kt`), the `DESPERATE_RUN` case: it sets `blockedAhead = false`. The next
update computes `blockedAhead` again.

## What happens

A desperate `Met` that stands at a pit changes from `DESPERATE_PAUSE` to `DESPERATE_RUN`. For 1 frame,
`blockedAhead` is `false`, so the animation key is `NoHelmetRun`. From the next frame it is `NoHelmetStand` again.
This repeats on each `DESPERATE_PAUSE` -> `DESPERATE_RUN` change.

## What should happen

The `Met` shows `NoHelmetStand` in every frame while the way ahead is blocked.

## How to reproduce

Read the code. In the game: `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_break`, break the helmet,
and watch the `Met` at a pit edge frame by frame.

## Notes

Visual only.
