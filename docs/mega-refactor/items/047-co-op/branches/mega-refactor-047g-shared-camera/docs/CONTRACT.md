# Contract: mega-refactor-047g-shared-camera

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: draft
- Approved: <date, or "no">

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

| Level | What to check |
|-------|---------------|

## Behaviors to check

Behaviors that are not tied to one level, for example the pause menu, death and respawn, game
over, or the save state.

## Intended changes

Gameplay changes that this branch makes on purpose. Leave empty if there are none.
