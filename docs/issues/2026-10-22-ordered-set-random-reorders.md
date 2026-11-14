---
status: closed
verified: yes
area: engine
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [011]
---

# `OrderedSet.random()` changes the order of the set

## Where

`ObjectSetExtensions.kt:68-79`

## What happens

It removes the chosen item and adds it again, so each read changes the iteration order.

## What should happen

Read a random element without a side effect.

## How to reproduce

Read the code.

## Notes

Item `011` replaces random calls.

- Fix: `fix/engine-small-bugs`. The short contract is in `2026-10-22-insertion-order-priority-queue-size.md`.

## Resolution

Fixed in `fix/engine-small-bugs`, squash commit `4522cf9db` (2026-11-14). `random()` returns `orderedItems()[MathUtils.random(0, size - 1)]`, with the same single random call, and does not change the set. `ObjectSetExtensionsTest` checks that the order is unchanged and that an empty set throws.
