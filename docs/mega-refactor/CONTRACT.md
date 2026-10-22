# Contract

The gameplay that must not change. Do these checks on a branch before it merges into `master`,
and write the result in the branch process log. Change this file only on `master`.

## How to run

- Debug run: `./run-game-debug.sh`
- Alpha run: `./run-game-alpha.sh`
- Engine tests: `./gradlew engine:test`

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew lwjgl3:jar` builds.

## Manual playthrough

TODO: list the levels to play through and what to check in each.

| Level | What to check |
|-------|---------------|

## Behaviors to check

TODO: list the behaviors to check that are not tied to one level (for example the pause menu,
death and respawn, game over, the save state).
