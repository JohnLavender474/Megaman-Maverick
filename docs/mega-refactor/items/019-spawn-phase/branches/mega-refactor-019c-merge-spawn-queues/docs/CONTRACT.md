# Contract: mega-refactor-019c-merge-spawn-queues

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.

## Manual playthrough

| Level | What to check |
|-------|---------------|

## Behaviors to check

Behaviors that are not tied to one level, for example the pause menu, death and respawn, game
over, or the save state.

## Intended changes

Gameplay changes that this branch makes on purpose. Leave empty if there are none.
