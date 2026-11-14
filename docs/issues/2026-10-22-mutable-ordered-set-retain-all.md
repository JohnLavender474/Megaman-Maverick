---
status: closed
verified: yes
area: engine
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [018]
---

# `MutableOrderedSet.retainAll` removes nothing and reports the wrong result

## Where

`engine/src/main/kotlin/com/maverick/game/engine/common/objects/MutableOrderedSet.kt:10-16`

## What happens

`retainAll` loops over the argument, not over the set. For each argument element that is not in the set, it calls `remove` (which does nothing) and sets `changed = true`. So it never removes anything, and it returns `true` when the argument has elements that are not in the set.

## What should happen

Keep only the elements that are in the argument collection.

## How to reproduce

Read the code, or write a unit test.

## Notes

Find the callers before a fix.

- Fix: `fix/engine-small-bugs`. The short contract is in `2026-10-22-insertion-order-priority-queue-size.md`.

## Resolution

Fixed in `fix/engine-small-bugs`, squash commit `4522cf9db` (2026-11-14). `retainAll` walks the set from the end and removes each element that is not in the argument; it returns `true` only when it removed one. `MutableOrderedSetTest` has 3 cases.
