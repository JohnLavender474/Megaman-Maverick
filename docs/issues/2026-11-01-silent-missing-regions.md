---
status: open
verified: no
area: content
gameplay-change: unknown
found-by: mega-refactor-004a-entity-smoke-test (2026-11-01)
related-items: [004]
---

# 4 entities in live maps have a missing region, and `init()` does not throw

## Where

The regions come from `findRegion`, which returns null. The result goes into a map or a nullable field, so `init()`
does not throw.

## What happens

- `ConveyorBelt`: no `ConveyorBelt/RightPart-MoveRight` in `Platforms1.txt`. 6 live maps use it. `ConveyorBelt.kt:140`
  selects `rRight`.
- `DesertMan`: no `DesertMan/init` in `Bosses2.txt`. 2 live maps use it.
- `BulbBlaster`: no `BulbBlaster/frozen` in `Enemies2.txt`. 2 live maps use it.
- `Coldier`: no `Coldier/frozen` in `Enemies2.txt`. 2 live maps use it.

The effect in play is not verified. Each miss may be harmless, or it may crash or show nothing when the code reads the
region. `BulbBlaster` builds its `frozen` animation from `darkRegion` (`BulbBlaster.kt:213`), so that miss may be harmless.

## What should happen

Each region exists, or the code does not ask for it.

## How to reproduce

Remove a class from `core/src/smokeTest/resources/known-entity-failures.txt` and run `./gradlew core:smokeTest`. The test
names the missing region.
