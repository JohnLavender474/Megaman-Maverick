---
status: open
verified: yes
area: core
gameplay-change: unknown
found-by: item 014 research (2026-10-22)
related-items: [014]
---

# The `BLOCK`-`GRAVITY_CHANGE` contact rule never runs

## Where

- `MegaContactListener.kt:591-609` (begin) and `MegaContactListener.kt:1191-1208` (continue).
- `MegaContactFilter.kt:20-95`: the `filters` map.

## What happens

The rules swap the x and y of a block's `frictionToApply` when the gravity direction is horizontal.
But neither `BLOCK` nor `GRAVITY_CHANGE` is a key of `filters`, so `WorldSystem` never makes this
contact. The two rules are dead code.

## What should happen

The owner decides:

- If blocks in a gravity-change area must swap their friction, add `BLOCK` to a key set (for example
  make `GRAVITY_CHANGE` a key with the set `{BLOCK}`). This changes gameplay in the gravity rooms
  (for example the `MoonMan` stage).
- If not, delete the two rules.

## How to reproduce

Read the code above.

## Notes

Item `014` must keep the rules dead, or name this issue under "Intended changes".
