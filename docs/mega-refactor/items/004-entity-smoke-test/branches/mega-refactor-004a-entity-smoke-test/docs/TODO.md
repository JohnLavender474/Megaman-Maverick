# TODO: mega-refactor-004a-entity-smoke-test

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- A Gradle test task that discovers 0 tests passes on Gradle 8.10.1. Measured: with `useJUnit()` in place of
  `useJUnitJupiter()`, `core:smokeTest` ran 0 tests and gave `BUILD SUCCESSFUL`. A break in Kotest discovery leaves CI
  green, also for `engine:test` and `core:test`. Add a check that each suite ran at least one test.
- `known-entity-failures.txt` stops stale lines, but a new line passes the suite. Only the file header and a code review
  stop it. Option: a fixed count in the test, so that an addition needs a visible edit.
- The class floor (more than 400 of 432) catches an empty scan, but up to 31 classes can drop out with no failure.
  Option: a tighter floor.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `JetMet`: `init()` throws, because the atlas has no frozen region (`JetMet.kt:111`, `JetMet.kt:293`). It is in the
  live map `Gauntlet2.tmx`.
- `ColtonJoe`: `init()` throws, because the atlas has no `ColtonJoe/frozen` region. Only `Test1.tmx` uses it.
- `TreeStump`: `TreeStump.kt:38` passes `TextureAsset.ENVIRONS_1.name`, not `.source`, so `init()` throws. No live map
  uses it.
- `SigmaRat`, `SigmaRatClaw`, `SigmaRatElectricBall`, `SigmaRatElectricBallExplosion`, `PenguinMiniBoss`, `Merserker`,
  `SniperJoe_OLD`, `SpikeBot_OLD`: `init()` throws on missing regions. No live map uses them. See `039` and `092`.
- `BombPenguinBot`, `CanonMet`: `init()` throws `NotImplementedError`. No code refers to them. See `039`.
- `ConveyorBelt` (`ConveyorBelt/RightPart-MoveRight`), `DesertMan` (`DesertMan/init`), `BulbBlaster`
  (`BulbBlaster/frozen`), `Coldier` (`Coldier/frozen`): the region is missing and `init()` does not throw. The effect
  in play is not verified.
