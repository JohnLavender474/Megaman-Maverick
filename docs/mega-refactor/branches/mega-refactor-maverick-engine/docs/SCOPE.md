# Scope: mega-refactor-maverick-engine

What this work changes and why. This is a draft from a research pass. No branch exists yet, and
the user has not approved this scope. The research details are in `NOTES.md`.

- Status: draft
- Approved: no

## Goal

Give the engine its own identity as the "Maverick Engine", so that other developers can use it
and tell it apart from LibGDX and from the game code. Then move the generic host role (frame
loop, services, screens) out of `MegamanMaverickGame` and into the engine.

The work has two parts with different timing. They will probably be two branches (see
"Open questions"):

| Part | What | When |
|------|------|------|
| A | Package rename and naming | First, alone, while no other `mega-refactor-` branch is active |
| B | Engine host contract: `IMaverickGame` and `MaverickGame` | After narrow services, with or after the fixed tick, before or with the platform-neutral engine |

## Backlog items

These items come from the review discussion (2026-10-22). They are not in `BACKLOG.md` yet.

### Maverick Engine naming and package rename

Give the engine its own identity, so that other developers can tell it apart from LibGDX and
from game code.

- Rename the root package `com.mega.game.engine` to one neutral root (for example
  `com.maverick.engine`). About 304 package lines and 11,376 import lines change in about 760
  files. Do it in one mechanical commit and add it to `.git-blame-ignore-revs`.
- Keep the class names. Do not add a `Maverick` prefix to every class. Use the prefix only on new
  entry-point types (for example the host contract). The package solves collisions with LibGDX
  and Java (`Animation`, `Timer`, `Body`, `Fixture`, `Properties`, `Event`, `Pool`).
- Rename the Gradle module or artifact to `maverick-engine` and fix `eclipse.project.name` in
  `engine/build.gradle:2`. Decide whether `GameEngine` keeps its name.
- Do not rename the `core` package. `EntityType.PACKAGE_PREFIX` builds class names from it for
  TMX spawns.
- Do it on a dedicated branch when no other `mega-refactor-` branch is active.
- Dependencies: none. Do it before most other engine items to avoid conflicts. Decide on
  publishing first (affects the package name).

### Engine host contract (`IMaverickGame`)

Move the generic host role out of `MegamanMaverickGame` (1017 lines, 69 public members) into
the engine.

- Add a small interface `IMaverickGame` that only exposes services: entity engine, events,
  assets, audio, input, screen host, cameras and viewports, `post(runnable)`, clock, random and
  platform services. No game feature adds a member. New needs become a service.
- Add an optional abstract base `MaverickGame : ApplicationListener` that owns the frame loop,
  run queue, screen switching, viewport resize and ordered dispose. LibGDX `Game` features are
  not used today.
- Keep game-specific members in `MegamanMaverickGame`: game state, saves, level definitions,
  camera rotation and the property-bag keys.
- Entities and systems do not take the host contract. They take narrow services. Tests use
  small fakes instead of a relaxed mock of the whole game.
- Dependencies:
  - After "narrow services" (`LevelContext` instead of `game` everywhere; typed fields replace
    the `game` property bag).
  - With or after "one fixed simulation step" (the base owns the fixed tick).
  - Before or with "platform-neutral `engine` module" (platform services are injected through
    the base).
  - Soft dependency on "`IPlayer` contract and players service" (removes `megaman` from the
    host).

## Current state

Summary of the scan. The full tables are in `NOTES.md`.

- `MegamanMaverickGame` (`core/.../MegamanMaverickGame.kt:137-139`) extends LibGDX `Game`, but
  uses it only as an `ApplicationListener`. It never calls `setScreen`, `getScreen` or `super.*`.
  It keeps its own `screens` map (`:181-183`, `:882-894`).
- 506 files in `core/src/main` take `game: MegamanMaverickGame`. About 80% of the calls go to
  services that are already generic: `assMan` (482 refs), `eventsMan` (139), `audioMan` (92),
  cameras, screens and the controller poller. The game-specific part is small: `state`,
  `megaman`, levels, rooms, camera rotation and the property bag.
- `engine/` has no notion of a host. A search for `ApplicationListener`, `Game` and `Gdx.app` in
  `engine/src/main` finds nothing. A search for `megaman` or `maverick` finds nothing. Only the
  package root `com.mega` suggests Mega Man.
- The engine is MIT-licensed and has no assets.

## In scope

Part A:

- Rename the engine root package in `engine/src/main` and `engine/src/test`, and update all
  imports in `core/` and `lwjgl3/`.
- Update the KDoc links and fully qualified names (`LooseQuadtreeWorldContainer.kt:20`,
  `IContactFilter.kt:12`, `:21`, `CullableOnUncontainedTest.kt:14`, `:33`) and the path in the
  root `CLAUDE.md:19`.
- Rename the Gradle module or artifact base name to `maverick-engine`. Fix
  `eclipse.project.name` in `engine/build.gradle:2`.
- Add the rename commit to `.git-blame-ignore-revs`.
- Optional, if the user decides so: rename `GameEngine` (for example to `EntityEngine`) in the
  same commit.

Part B:

- Add `IMaverickGame` and the abstract `MaverickGame : ApplicationListener` to the engine.
- Make `MegamanMaverickGame` extend `MaverickGame`. Move the generic loop, run queue, screen
  host, viewport resize and ordered dispose into the base.

## Out of scope

- A rename of the `core` package (`com.megaman.maverick.game`). It breaks all TMX spawns.
- A `Maverick` prefix on existing engine classes.
- Publishing the engine (JitPack, Maven Central, own repo), unless the user adds it.
- The narrow services, fixed tick, `IPlayer` and platform-neutral engine work. They are separate
  items. Part B only depends on them.
- Any gameplay change.

## Affected areas and risks

- Part A touches about 760 files. Any branch that is active at that time gets a conflict in
  almost every file. Do it when no other branch is active, or freeze the other branches.
- Part A cannot change behavior if it only changes package names. No engine package string is
  built at runtime. Check: `./gradlew engine:test core:test lwjgl3:jar` and one game run.
- Part B changes the frame order code (`MegamanMaverickGame.kt:406-590`). A wrong order of the
  run queue, screen render, `eventsMan.run()` and pool reclaim can change gameplay. The run
  queue is drained LIFO today; keep that order unless a separate fix changes it.
- Part B done too early copies the 69 members of today's class into a new god interface.

## Overlap with other branches

No `mega-refactor-` branch is active (2026-10-22). Local branch `overhaul_1` is 9 commits ahead
and 957 behind `master`, with no engine changes.

## Open questions

1. Publishing: do you plan to publish the engine (JitPack or Maven Central) or move it to its
   own repo? This decides the root package (`com.maverick.engine` or
   `io.github.johnlavender474.maverick`). Decide before Part A, so that the package changes
   only once.
2. Host name: `IMaverickGame`/`MaverickGame`, or `IMaverickApp`/`MaverickApp`? Do you accept the
   `I` prefix for the interface?
3. `GameEngine`: keep the name, or rename it (for example `EntityEngine`) in the Part A commit,
   so that all mass renames happen at one time?
4. Base class: should `MaverickGame` own the frame loop (fixed tick, run queue, screen host,
   auto-perf policy), or do you want only the interface?
5. Branches: one branch for both parts, or two branches (`mega-refactor-maverick-engine` for
   Part A and a later branch for Part B)? The timing suggests two.
