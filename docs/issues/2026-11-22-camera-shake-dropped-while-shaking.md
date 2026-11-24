---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-078g-timber-walker (2026-11-22)
related-items: [078, 180]
---

# A camera shake request during a running shake is dropped

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`, the handling of the shake event.

## What happens

The level screen starts a camera shake only when the last shake is finished. A second request while a shake runs
(for example within 0.5 s) is dropped. In the `TimberWalker` fight, a saw chop soon after a hop landing gets no shake.

## What should happen

The owner decides: restart the shake, keep the stronger or longer one, or keep the current rule.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_078 --room walker_gate --heartTanks all --healthTanks all`: when the
walker chops soon after a hop landing, the chop has no shake.

## Notes

Smoothness review finding 9 of `078g`. The rule is shared by all entities that ask for a shake.
