# Plan: mega-refactor-029g-behavior-split

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-25

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`. Smoke launches are short (about 20 seconds); a clean exit with no exception is
a pass. The owner plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass.

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| The 8 behavior factory functions | `defineBehaviorsComponent` |

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029g` row in `ITEMS.md` to `active` with the start
  date; mark `029g` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state to `active`.
  Then `git worktree add -b mega-refactor-029g-behavior-split .worktrees/mega-refactor-029g-behavior-split
  master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 142 core) and the warning count
  (expected 114) in the process log.
- Done: yes

### 3. Test room (subagent)

- Changes: a subagent adds the room `movement` to `RefactorTest029.tmx` with the `level-builder` skill: a player
  spawn, a floor, two tall walls with a gap between them wide enough to wall jump, a passage one tile high, a
  `Ladder` that reaches a ledge, and a `Water` pool deep enough to swim. Properties of `Ladder` and `Water` are
  copied from `Test1.tmx`. Painted tiles on every block cell. No other room changes.
- Check: a short launch of `--room movement` shows no exception.
- Done: yes

### 4. Tests on the current code

- Changes: a new test in `core/src/test/.../entities/megaman/` that builds a `Megaman` (as
  `MegamanHeartTankTest` does) and calls `defineBehaviorsComponent()`:
  - the behavior keys are in the order `WALL_SLIDING`, `SWIMMING`, `JUMPING`, `AIR_DASHING`, `GROUND_SLIDING`,
    `CROUCHING`, `CLIMBING`, `JETPACKING`;
  - two `Megaman` objects get a different behavior object for each key.
- Check: both tests pass on the code before the split. Build and tests pass.
- Done: yes

### 5. Split the behaviors

- Changes:
  - A new folder `M/behaviors/` with 8 files (`WallSlideBehavior.kt`, `SwimBehavior.kt`, `JumpBehavior.kt`,
    `AirDashBehavior.kt`, `GroundSlideBehavior.kt`, `CrouchBehavior.kt`, `ClimbBehavior.kt`,
    `JetpackBehavior.kt`). Each holds one `internal fun Megaman.<name>Behavior(): IBehavior` that returns the
    behavior object, with the body moved from `M/components/BehaviorsComponent.kt` with no change.
  - The constants that only one behavior uses (its `MEGAMAN_*_BEHAVIOR_TAG`, `MEGAMAN_LADDER_MOVE_OFFSET` if
    only the climb uses it) move to that behavior's file.
  - `defineBehaviorsComponent` keeps the 8 `addBehavior` calls in today's order, each with its factory call, and
    gets the `TODO`: the weapon checks inside the factories (`NEEDLE_SPIN`, `RUSH_JET`, `RODENT_CLAWS`) belong to
    the loadout (`024`).
  - The imports follow the code.
- Check:
  - a script compares each moved body with the original on `master`, with all whitespace removed; the only
    differences are the wrapping function and the `return`;
  - the step 4 tests pass with no change to them;
  - `git diff master --stat` shows no change in `MegamanValues.kt`, `Megaman.kt` or the other components;
  - build and tests pass; a launch of `--room movement` shows no exception.
- Done: yes

### 6. `MegaPlayer.megaman` `TODO`

- Changes: the `TODO` in `players/PlayerExtensions.kt` names `024` for the loadout reads and a second character
  for the `running` and sprite reads, not `029g`.
- Check: a grep finds no `029g` in `core/src/main`. Build passes.
- Done: yes

### 7. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test. Write the results in the
  process log.
- Done: yes

### 8. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's explicit
  approval. The backlog candidate (Proto Man) becomes a new item. With this merge, item `029` is `done`.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: yes
