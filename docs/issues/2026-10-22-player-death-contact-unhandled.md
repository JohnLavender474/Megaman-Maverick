---
status: open
verified: yes
area: core
gameplay-change: no
found-by: item 014 research (2026-10-22)
related-items: [014, 020]
---

# The filter makes `PLAYER`-`DEATH` contacts that no rule handles

## Where

- `MegaContactFilter.kt:22-26`: `PLAYER pairTo objectSetOf(BODY, ITEM, DEATH)`.
- `MegaContactListener.kt`: no branch matches `PLAYER` with `DEATH` in any phase.

## What happens

Each sub-step, `WorldSystem` makes a contact when Megaman's `PLAYER` fixture overlaps a `DEATH`
fixture, and calls the listener for it. No rule matches, so the contact does nothing. The death
effect comes from the `DEATH` × {`FEET`, `SIDE`, `HEAD`, `BODY`} rule.

## What should happen

Remove `DEATH` from the `PLAYER` set. This removes the useless contacts and has no effect on
gameplay.

## How to reproduce

Read the code above.

## Notes

The cost is small (one player, a few death fixtures). Item `014` can remove the entry as an intended
change with no gameplay effect, or leave it.
