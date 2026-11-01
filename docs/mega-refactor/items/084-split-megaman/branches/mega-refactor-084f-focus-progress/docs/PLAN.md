# Plan: mega-refactor-084f-focus-progress

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-01

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`. Line numbers are at `6eb3884bd`.

Each step is one commit. Each step ends with `./gradlew engine:test core:test lwjgl3:jar`, plus the check that the step names. Where a step changes a rule or adds an API, the test comes first, and it must fail for the expected reason before the code changes.

## Steps

### 1. Dead `ConstKeys.FOCUS`

- Changes: delete the read at `M/Megaman.kt:553`, the `removeProperty` at `:389`, and the constant at `C/Constants.kt:88`.
- Check: the dead-key `grep` in `CONTRACT.md`.
- Done: yes

### 2. `FocusComponent`

- Changes: tests first: `FocusComponent` gives the values of its hooks, `IGameEntity.focus` gives the component, and it throws if the component is missing. The position rule has a test for each of the 4 directions, written against the current `Megaman.getFocusPosition()` so that it passes before the move. Then add `C/entities/state/FocusComponent.kt` and `defineFocusComponent()` in `M/components/`, with the bounds and the position rule from `M/Megaman.kt:550-564`. `Megaman.init()` adds the component. `IGameEntity.focus` reads it. `M/handlers/MegamanWeaponsHandler.kt:383` reads `megaman.focus.getFocusPosition()`. Remove `IFocusable` from the class line, and point the 4 direction tests at the component.
- Check: the class line has 9 interfaces. The 4 direction tests pass with no change to their expected values.
- Done: yes

### 3. `PlayerProgressComponent`

- Changes: tests first: each hook runs once for its members, and the other members call no hook. Then add `C/entities/state/PlayerProgressComponent.kt` and `definePlayerProgressComponent()` in `M/components/`, with the 3 rule bodies from `M/Megaman.kt:566-585` and `setMaxHealthFromHeartTanks()` with its `TODO`. `Megaman.init()` adds the component. `C/MegamanMaverickGame.kt:444` adds the component to `GameState`, at the same line. Remove `IGameStateListener` from the class line. `MegamanHeartTankTest` adds the component as the listener; its expected values do not change.
- Check: the class line has 8 interfaces. New tests for the Mega hooks: a level defeat sets `weaponsAttained`, and the sound plays only if the currency is below the max.
- Done: yes

### 4. Final checks

- Changes: none in code, unless a check fails.
- Check: all automated checks in `CONTRACT.md`. Review the full diff against the "Frame order" section. Build, then `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room heart` and `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_corridor`, one launch each. Compare the diff with `master`, and merge `master` first if it changed the same files (`031e` is the known overlap). Then the owner's play test.
- Done: yes
