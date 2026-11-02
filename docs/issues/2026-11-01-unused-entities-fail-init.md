---
status: open
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor-004a-entity-smoke-test (2026-11-01)
related-items: [004, 039, 092]
---

# 10 entity classes that no live map uses fail `init()`

## Where

The classes on `core/src/smokeTest/resources/known-entity-failures.txt`.

## What happens

`init()` throws for each class. No live map places any of them:

- Missing regions: `PenguinMiniBoss`, `SigmaRat`, `SigmaRatClaw`, `SigmaRatElectricBall`,
  `SigmaRatElectricBallExplosion`, `Merserker`, `SniperJoe_OLD`, `SpikeBot_OLD`.
- `NotImplementedError` from `TODO()`: `BombPenguinBot` (`BombPenguinBot.kt:87`) and `CanonMet` (`CanonMet.kt:33`).

Code refers to some of them (`EntityFactories` entries, and `SwingingPlatform` for `SniperJoe_OLD`). `Merserker`,
`BombPenguinBot` and `CanonMet` have no references in code.

## What should happen

Each class is either removed (see `039`, `092`) or works.

## How to reproduce

Remove a class from `known-entity-failures.txt` and run `./gradlew core:smokeTest`.
