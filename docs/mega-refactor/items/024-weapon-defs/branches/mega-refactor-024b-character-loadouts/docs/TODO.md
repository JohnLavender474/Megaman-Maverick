# TODO: mega-refactor-024b-character-loadouts

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `LoadoutOverride` and `ResolvedLaunch`: `weapons` is an `ObjectSet` (this branch), while `heartTanks`,
  `healthTanks` and `enhancements` stay Kotlin `Set` and `Map`. Move them all to LibGDX collections (and to
  read-only views where a `LevelDefinition` entry holds them), in one change with their readers.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Reactor Shot (owner, play test 2026-11-16): the 4 small burst pieces sometimes die in mid-air, as if they hit a block
  that the sprite does not touch. Seen in `TEST_1` `room35` and not in `INTRO_STAGE`; a weapon change is not the cause.
  Not caused by this branch: `ReactorManProjectile.kt` and the projectile base classes are the same as on `master`.
  Measured: a small piece has a 16 px body and `PROJECTILE` fixture (`SMALL_SIZE = 0.5f`); its art is 14 px wide (7 px
  in a 16 px cell drawn at 2x), so the fixture is only 1 px larger on each side. Candidates: a block with no painted
  tile (for example a room ceiling `AbstractBlock`), or another entity's `BODY` or shield fixture (`hitBody`,
  `hitShield`). Check with `./run-game-alpha.sh --level TEST_1 --room room35 --weapons all --debugShapes`.
