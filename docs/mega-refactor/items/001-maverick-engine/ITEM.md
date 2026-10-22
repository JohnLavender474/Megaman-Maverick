# 001: Maverick Engine

- Status: planned
- Type: refactor
- Depends on: `001a`: none. `001b`: `016`, `010`, `032`
- Related: `029`
- Branches: `mega-refactor-001a-package-rename` (blocked by decision P), `mega-refactor-001b-host-contract`
  (blocked)

## Goal

Give the engine its own identity as the "Maverick Engine", so that other developers can use it
and tell it apart from LibGDX and from the game code. Then move the generic host role (frame
loop, services, screens) out of `MegamanMaverickGame` and into the engine.

## Proposed scope

From the research. Not approved. Each branch `SCOPE.md` decides its final scope.

The work has two branches with different timing:

| Branch | What | When |
|--------|------|------|
| `001a` | Package rename and naming | First, alone, while no other `mega-refactor-` branch is active |
| `001b` | Engine host contract: `IMaverickGame` and `MaverickGame` | After `016`, with or after `010`, before or with `032` |

`001a`, Maverick Engine naming and package rename:

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

`001b`, engine host contract (`IMaverickGame`):

- Add a small interface `IMaverickGame` that only exposes services: entity engine, events,
  assets, audio, input, screen host, cameras and viewports, `post(runnable)`, clock, random and
  platform services. No game feature adds a member. New needs become a service.
- Add an optional abstract base `MaverickGame : ApplicationListener` that owns the frame loop,
  run queue, screen switching, viewport resize and ordered dispose. LibGDX `Game` features are
  not used today.
- Keep game-specific members in `MegamanMaverickGame`: game state, saves, level definitions,
  camera rotation and the property-bag keys.
- Entities and systems do not take the host contract. They take narrow services (`016`).

## Owner's input

From the request (2026-10-22, summary): consider making the main game an interface (preferred)
or an abstract class, as part of the engine, named `MaverickEngine`. Consider using the keyword
`Maverick` in the engine packages and names, so that other developers can tell the engine apart.

## Decisions

- M (2026-10-22): the host types are `IMaverickGame` and `MaverickGame` (Game, not App).
- O (2026-10-22): the engine base class owns the game loop and the orchestration of systems and entities, so that client games do not set this up.
- (2026-10-22) The Maverick Engine is meant for Mega Man and Mega Man-like games. The name sets it apart from other Mega Man engines (for example Mega Man Maker).
- L, N (2026-10-22): on hold. The owner is thinking about keeping engine and game in one project (open decision P in `ITEMS.md`), which changes the rename.

## Open questions

1. Publishing: do you plan to publish the engine (JitPack or Maven Central) or move it to its
   own repo? This decides the root package (`com.maverick.engine` or
   `io.github.johnlavender474.maverick`). Decide before `001a`, so that the package changes only
   once.
2. `GameEngine`: keep the name, or rename it (for example `EntityEngine`) in the `001a` commit,
   so that all mass renames happen at one time?

## Sources

- `NOTES.md` in this folder.
