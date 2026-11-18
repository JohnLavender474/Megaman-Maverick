---
status: open
verified: yes
area: core
gameplay-change: no
found-by: `mega-refactor-079a-smarter-met` (2026-11-18)
related-items: [079]
---

# The `MetHelmet` spin lags 1 frame when the helmet seats or comes to rest

## Where

`MetHelmet.update` (`entities/hazards/MetHelmet.kt`): it calls `updateLook` before it changes `status`.

## What happens

In the frame in which the helmet seats on its `Met` or comes to rest, the sprite keeps its last spin angle. The
helmet is upright from the next frame.

## What should happen

The helmet is upright in the same frame in which it seats or comes to rest.

## How to reproduce

Read the code. In the game: `./run-game-alpha.sh --level REFACTOR_TEST_079 --room charged_stagger`, hit the `Met`
with a charged shot, and step through the frames when the helmet lands.

## Notes

Visual only. A likely fix: call `updateLook` after the `status` change.
