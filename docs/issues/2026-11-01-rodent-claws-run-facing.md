---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-084g-movement-state scan (2026-11-01)
related-items: [024, 084]
---

# With left and right both held during a `RODENT_CLAWS` shot, the facing goes right and never left

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/ControllerComponent.kt`: the guard in `run(delta, side)`, at its `TODO`.

## What happens

When the run guard stops the run during a `RODENT_CLAWS` shot, the facing changes. With `LEFT` and `RIGHT` both held, the right side sets the facing, and the left side does not. So the facing goes right, and it never goes left.

The old `runLeft` and `runRight` differed here. This is probably an accident of the copy. `084g` kept it on purpose, and `MegamanRunTest` pins it.

## What should happen

The same rule on both sides. With the last-pressed rule, the case with both held does not occur.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons RODENT_CLAWS`: face left, slash on the ground, and press both `LEFT` and `RIGHT` during the slash. Mega faces right.

## Notes

`fix/left-right-last-pressed` closes this issue. See `2026-11-01-left-right-both-held.md`.

## Resolution

Fixed by `fix/left-right-last-pressed` (squash `b094f8423`). The side-specific condition and its `TODO` are gone, and the facing follows the last press on both sides. The owner's play test passes (2026-11-01).
