# Notes: mega-refactor-016d-system-services

## Decisions

- 2026-10-29 (owner, scope approval, verbatim): "On 2nd thought, I see no harm in continuing with the original
  recommendation of passing in `LevelContext`. I approve the scope". Results (`SCOPE.md`, "Open questions"):
  1. `players` and `gravity` are `val` fields built with the game object.
  2. `megaContactRules(players, gravity, debugFilter)`.
  3. `MegaPathfinderFactory(levelState)`.
  4. `LevelDefinition.screenOnCompletion: (LevelState) -> ScreenEnum`; the level screen passes `game.levelState`
     (`LevelContext` in the owner's words is this option; there is no `LevelContext` class).
  5. No temporary old-versus-new suite; permanent tests only.
  6. A new test map `RefactorTest016.tmx` (`REFACTOR_TEST_016`) with a `gravity_change` room.
  7. Root `CLAUDE.md` is updated in the last code step.

## Overlap with other branches

None. No other code branch is active (`SCAN.md` section 8).
