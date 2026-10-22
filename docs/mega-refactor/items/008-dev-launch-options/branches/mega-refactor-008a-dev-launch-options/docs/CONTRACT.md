# Contract: mega-refactor-008a-dev-launch-options

The gameplay that this branch must not change, and how to check it. Do the checks before the
branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-10-22

The main rule: with none of the new options, the game boots and plays exactly as today. The new
options only act when they are given.

## Baseline

Take it on the branch before any change, with the command below and Corretto JDK 17. Expected
from the `001a` merge: `engine:test` 427 tests and `core:test` 49 tests, all pass. Write the
exact numbers in `NOTES.md`.

## Automated checks

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

- [x] The build succeeds and the JAR builds.
- [x] All baseline tests still pass (427 engine, 49 core).
- [x] New unit tests for the option checks and the staging rules pass. They cover: each option
  parsed case-insensitively; an unknown enum value gives an error that lists the allowed values
  and the nearest one; `--screen LEVEL_SCREEN` without a level is an error; `--room` without a
  level is an error; `--level L` alone starts L; `--screen S --level L` stages L and opens S;
  `--runType test` acts as `--level TEST_1`; `--logLevels "debug, log"` (with a space) gives two
  levels.

## Manual playthrough

Result (2026-10-22, owner): pass. All runs below work, including `--room` after the fix in
`638e745bf`.

| Run | What to check |
|-----|---------------|
| `./run-game-debug.sh` (no new option) | The game boots to the same first screen as today (`SIMPLE_INIT_GAME_SCREEN`), and a level plays as before. |
| `./run-game-test.sh` (`--runType test`) | `TEST_1` starts directly, as today. |
| `--level MOON_MAN` | Moon Man starts at its normal first spawn. |
| `--screen LEVEL_SELECT_SCREEN --level MOON_MAN` (or another level-select screen name) | The level select screen opens. Going on to a level start with no level choice loads Moon Man; choosing another level loads that one. |
| `--level MOON_MAN --room <a room with a player spawn>` | The level starts at the first player spawn in that room. After a death, the respawn uses the normal checkpoint rules. |
| `--level MOON_MAN --room <a room without a player spawn>` | The game stops with an error that lists the rooms that have player spawns. |
| `--level MOONMAN` | The game stops with exit code 2, the allowed values and "Did you mean MOON_MAN?". |
| `--screen WILY_CASTLE_SCREEN` | The game stops with an error: the screen is not registered. |
| `--logTags all` | Debug lines print for all tags. |
| `--autoPerf false` | The performance tier does not change during play. |

## Behaviors to check

- [x] Normal play from the main menu (new game or load, level select, a level, death and
  respawn) is unchanged when no new option is given.

## Intended changes

- New launch options: `--screen`, `--level`, `--room`, `--logTags`, `--autoPerf`, with checks.
- `--runType test` still works but is marked deprecated in its help text.
- `--logLevels` now removes spaces correctly
  (`docs/issues/2026-10-22-literal-whitespace-regex.md`, the `DesktopLauncher.kt` part only).
