---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [026]
---

# `InsertionOrderPriorityQueue.size` is always 0

## Where

`engine/src/main/kotlin/com/maverick/game/engine/common/objects/InsertionOrderPriorityQueue.kt:20`

## What happens

`override val size = queue.size` reads the size once, at construction, so it is always 0.

## What should happen

`override val size get() = queue.size`.

## How to reproduce

Read the code, or add items and read `size`.

## Notes

Item `026` deletes this class. Check the callers of `size` before a fix.

## Short contract

Branch: `fix/engine-small-bugs`, off `master`, in `.worktrees/fix/engine-small-bugs`. The branch fixes 5 small
`engine` issues together. This file holds the short contract for all of them:

- `2026-10-22-insertion-order-priority-queue-size.md` (this file)
- `2026-10-22-mutable-ordered-set-retain-all.md`
- `2026-10-25-game-line-rotate-origin-y.md`
- `2026-10-22-float-round-locale.md`
- `2026-10-22-ordered-set-random-reorders.md`

Intended changes:

- `InsertionOrderPriorityQueue.size` reads the live size of the queue.
- `MutableOrderedSet.retainAll` removes each element that is not in the argument, and returns `true` only when it
  removed one.
- `GameLine.rotate(rotation, originX, originY)` rotates about the world point (`originX`, `originY`), the same as
  `GameRectangle.rotate` and its callers (`Body.getBounds`, `Fixture.getShape`) expect. The line keeps its origin
  relative to `position` (`calculateWorldPoints` pivots on `position + origin`), so `rotate` stores
  `originX - position.x` and `originY - position.y`. With only `originY = originY`, a line with a non-zero `position`
  (for example after `setCenter`) still turns about the wrong point.
- `Float.round` (`FloatExtensions.kt`) rounds with `BigDecimal` and `RoundingMode.HALF_UP` (the rounding of `%.Nf`),
  not through a formatted string. It is kept: `PendulumTest` and `AnimationTest` call it.
- `OrderedSet.random()` (`ObjectSetExtensions.kt`) reads the element at a random index through `orderedItems()`, and
  does not change the order of the set.

Changed files: `InsertionOrderPriorityQueue.kt`, `MutableOrderedSet.kt`, `GameLine.kt`, `FloatExtensions.kt`,
`ObjectSetExtensions.kt`, and new or extended tests in `engine/src/test`.

Not changed, with the caller check on `master` `035e5dbec`:

- Gameplay. No code reads the `size` of the drawable queues (`MegamanMaverickGame.addDrawable`, `MegaLevelScreen`
  use only `add`, `poll` and `isEmpty`). No code calls `retainAll` on a `MutableOrderedSet`. The only `GameLine`
  fixtures (`Laser`) set `attachedToBody = false`, so `Fixture.getShape` does not rotate them, and no other code calls
  `rotate` on a `GameLine`. The callers of `OrderedSet.random()` (`AsteroidsSpawner`, `DrillHead`, `ElecDevil`) keep a
  uniform pick with the same one `MathUtils.random` call; only the order of the set after the call changes.
- The other collection classes and shapes.

Checks:

- New unit tests fail before the fix and pass after it: `size` after adds and polls; `retainAll` keeps the common
  elements, removes the others, and gives the right result; a `GameLine` with a non-zero `position`, rotated about a
  world point with `originX != originY`, gives the expected world points; the `GameRectangle.rotate` tests still pass
  (its lines have `position` zero, so the change does not affect them); `random()` leaves the order of the set unchanged and gives
  only elements of the set.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- No play test: no gameplay change.
