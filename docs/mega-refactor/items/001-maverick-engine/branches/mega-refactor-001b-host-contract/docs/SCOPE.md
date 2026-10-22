# Scope: mega-refactor-001b-host-contract

What this branch changes and why. This is a draft from a research pass. The branch does not
exist yet, and the user has not approved this scope. The research is in
`items/001-maverick-engine/NOTES.md`.

- Status: draft
- Approved: no

## Goal

Move the generic host role out of `MegamanMaverickGame` (1017 lines, 69 public members) into the
engine, as a small interface `IMaverickGame` and an optional abstract base `MaverickGame`.

## Item

Item `001` (Maverick Engine), the host contract part:

- Add a small interface `IMaverickGame` that only exposes services: entity engine, events,
  assets, audio, input, screen host, cameras and viewports, `post(runnable)`, clock, random and
  platform services. No game feature adds a member. New needs become a service.
- Add an optional abstract base `MaverickGame : ApplicationListener` that owns the frame loop,
  run queue, screen switching, viewport resize and ordered dispose. LibGDX `Game` features are
  not used today.
- Keep game-specific members in `MegamanMaverickGame`: game state, saves, level definitions,
  camera rotation and the property-bag keys.
- Entities and systems do not take the host contract. They take narrow services (`016`).

## Current state

- `MegamanMaverickGame` (`core/.../MegamanMaverickGame.kt:137-139`) extends LibGDX `Game`, but
  uses it only as an `ApplicationListener`. It never calls `setScreen`, `getScreen` or `super.*`.
  It keeps its own `screens` map (`:181-183`, `:882-894`).
- 506 files in `core/src/main` take `game: MegamanMaverickGame`. About 80% of the calls go to
  services that are already generic. The game-specific part is small: `state`, `megaman`,
  levels, rooms, camera rotation and the property bag.
- Full tables are in `items/001-maverick-engine/NOTES.md`.

## In scope

- Add `IMaverickGame` and the abstract `MaverickGame : ApplicationListener` to the engine.
- Make `MegamanMaverickGame` extend `MaverickGame`. Move the generic loop, run queue, screen
  host, viewport resize and ordered dispose into the base.

## Out of scope

- The narrow services (`016`), fixed tick (`010`), `IPlayer` (`029`) and platform-neutral engine
  (`032`). This branch depends on them.
- Any gameplay change.

## Affected areas and risks

- The frame order code changes (`MegamanMaverickGame.kt:406-590`). A wrong order of the run
  queue, screen render, `eventsMan.run()` and pool reclaim can change gameplay. The run queue is
  drained LIFO today. Keep that order unless a separate bug fix changes it.
- If this branch starts before `016`, the contract copies the 69 members of today's class and
  becomes a new god interface.

## Overlap with other branches

Blocked until `016` is merged. Check `010` and `032` for overlap when this branch starts.

## Open questions

1. Host name: `IMaverickGame`/`MaverickGame`, or `IMaverickApp`/`MaverickApp`? Do you accept the
   `I` prefix?
2. Should `MaverickGame` own the frame loop (fixed tick, run queue, screen host, auto-perf
   policy), or do you want only the interface?
