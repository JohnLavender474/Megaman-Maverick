---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-146a-player-marker spawn-flow audit (2026-11-10)
related-items: []
---

# Some entities keep per-spawn fields from their last pooled life

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/Elecn.kt:81` (`zigzagUp`)
- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/MagFly.kt:70` (`flash`)
- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/Sealion.kt:94` (`ballInHands`)

## What happens

These fields change during play and are not reset in `onSpawn`, so a pooled instance starts its next life with the
value from the last one. A sample of 21 flagged fields found these 3; the audit infers about 10-15 classes in total
(`temp/mega-refactor-146a-player-marker/spawn-design/4-lifecycle-audit.md`, not committed).

## What should happen

`onSpawn` sets every per-spawn field (CLAUDE.md, "Pooled entities are reused").

## How to reproduce

Not checked in play. `Sealion` may correct itself in the first frames (inference).
