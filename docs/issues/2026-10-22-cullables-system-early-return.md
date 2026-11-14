---
status: closed
verified: yes
area: engine
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [017]
---

# `CullablesSystem` skips culling for the remaining entities after one entity without a cullable

## Where

`engine/.../cullables/CullablesSystem.kt:26`

## What happens

A `?: return` inside the per-entity loop is a non-local return. It exits `process` for all remaining entities in that frame, and also skips `diagnostics?.endEntry()`.

## What should happen

Skip only the current entity (`?: continue` or `return@forEach`).

## How to reproduce

Read the code. An entity in the system without the expected cullable stops culling for every entity after it in iteration order. `CullablesSystemTest` does not cover this case.

## Notes

Fixing it can make some entities cull that do not cull today, so it is a gameplay change. Check the result in play.

## Short contract

Branch: `fix/engine-small-bugs-2`, off `master`, in `.worktrees/fix/engine-small-bugs-2`. The branch fixes 2 small
`engine` issues and 2 small `engine` items together. This file holds the short contract for all of them:

- `2026-10-22-cullables-system-early-return.md` (this file)
- `2026-10-22-engine-gradle-dependencies.md`
- item `066` (`GameSystem.reset()` clears pending removes)
- item `083` (`PathfinderResult.targetReached`)

Intended changes:

- `CullablesSystem.process` skips only the entity that has no `CullablesComponent`, and goes on with the other
  entities. `diagnostics?.endEntry()` always runs. The null check is its own statement before the inner loop
  (`docs/CODE_STYLE.md`, "A guard is its own statement").
- `engine/build.gradle` takes `gdx-freetype` at `$gdxVersion` (as `core` does), and no longer has `gdx-tools:1.6.3`.
- `GameSystem.reset()` also clears `entitiesToRemove`.
- `PathfinderResult.targetReached` is removed. No production code reads it, and its value is the opposite of its name
  in the common case (`false` when a path to the target is found). The rule "write code only when other production
  code calls it" (`docs/mega-refactor/CLAUDE.md`) decides between rename, fix and remove. The tests that read it
  assert on `path` only.

Changed files: `CullablesSystem.kt`, `engine/build.gradle`, `GameSystem.kt`, `PathfinderResult.kt`, `Pathfinder.kt`,
and tests in `engine/src/test` (`CullablesSystemTest`, a `GameSystem` reset test, `PathfinderTest`,
`WorldPathfinderTest`, `PathfindingSystemDebugConsumerTest`).

Not changed, with the caller check on `master` `744f733d2`:

- Gameplay. An entity joins `CullablesSystem` only when it has a `CullablesComponent`
  (`GameSystem.qualifies`), and `CullablesComponent.cullables` is not nullable. So the early return runs only when
  the component is removed while the entity is in the system. The only code that removes it (`Force`,
  `ForceDecoration`, `GravityChange`) does so in `onSpawn`, before the entity joins the systems. So no live entity
  reaches the early return today, and the fix changes no culling in play. `gameplay-change` is set to `no`.
- No code adds or removes a system member during that system's own `update`, so `entitiesToRemove` is empty at each
  `reset()` today.
- No code in `engine`, `core` or `lwjgl3` imports `com.badlogic.gdx.tools`. `BitmapFontHandle` uses `gdx-freetype`,
  which stays.
- The pathfinding result path, the other systems and the other collections.

Checks:

- New unit tests fail before the fix and pass after it: an entity in `CullablesSystem` whose component was removed
  after it joined does not stop the culling of the entities after it, and `endEntry()` runs; `reset()` during a
  system's own `update`, then a new add of the same entity, keeps the entity in the system after the next `update`.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. The JAR build is the check for the dependency
  change.
- No play test: no gameplay change.

## Resolution

Fixed in `fix/engine-small-bugs-2`, squash commit `1525b0142` (2026-11-14). The null check is its own statement
(`if (cullables == null) return@forEach`) before the inner loop. A `CullablesSystemTest` case removes the component of
a member during the update and checks that the next member still culls and that `endEntry()` runs.
