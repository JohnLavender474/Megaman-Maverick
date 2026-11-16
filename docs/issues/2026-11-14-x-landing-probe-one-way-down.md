---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: mega-refactor-060d-protoman-slide-ladder (2026-11-14)
related-items: [081]
---

# X's landing probe skips one-way blocks while `DOWN` is held

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/megamanx/MegamanXMoves.kt`, the landing probe
(`!(fixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) && input.isPressed(MegaControllerButton.DOWN))`).

## What happens

The probe does not count a one-way block (`COLLIDE_DOWN_ONLY`) while `DOWN` is held. Since `081g` D4, `DOWN` no
longer drops X through one-way blocks. So with `DOWN` held over a one-way block, the probe can report no floor where
X stands on one. Scan inference from the `060d` scan; not seen in the game.

## What should happen

The probe agrees with the current one-way rule: a one-way block under X is a floor also with `DOWN` held.

## How to reproduce

Not reproduced. Play X on a one-way platform with `DOWN` held, and look at the moves that read the probe (landing).
