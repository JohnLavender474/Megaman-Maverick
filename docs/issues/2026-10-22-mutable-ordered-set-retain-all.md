---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [018]
---

# `MutableOrderedSet.retainAll` removes nothing and reports the wrong result

## Where

`engine/src/main/kotlin/com/mega/game/engine/common/objects/MutableOrderedSet.kt:10-16`

## What happens

`retainAll` loops over the argument, not over the set. For each argument element that is not in the set, it calls `remove` (which does nothing) and sets `changed = true`. So it never removes anything, and it returns `true` when the argument has elements that are not in the set.

## What should happen

Keep only the elements that are in the argument collection.

## How to reproduce

Read the code, or write a unit test.

## Notes

Find the callers before a fix.
