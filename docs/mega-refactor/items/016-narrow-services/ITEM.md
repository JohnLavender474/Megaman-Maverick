# 016: Narrow services instead of `game` everywhere

- Status: planned
- Type: refactor
- Depends on: none
- Related: `001`, `011`, `028`, `029`, `049`
- Branches: none yet

## Goal

Entities and systems take small services (for example `LevelContext`) instead of the whole `MegamanMaverickGame`, and typed fields replace the `game` property bag.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Add `LevelContext` and other narrow services: level, rooms, world container, map load result, players, random, texture cache.
- Typed fields replace the `game` property bag (58 property calls).
- Entities never take the host contract (`001b`). Tests pass 2–3 small fakes instead of a relaxed mock of the whole game.

## Owner's input

Answer to review item 15 (2026-10-22, verbatim):

> yes

## Open questions

- How does `LevelContext` hold the players service? Option from `029` (2026-10-23): a generic context, so that core sees its concrete service and engine code stays generic:

  ```kotlin
  class LevelContext<out P : IPlayers<out PlayerView>>(val players: P /* , rooms, camera, ... */)
  typealias MegaLevelContext = LevelContext<MegaPlayers>   // core
  ```

  - Core call sites see `MegaPlayers`, so its members (`targetFor`, and later its targeting policy field) stay usable; nothing has to become an extension only to be reachable through the interface.
  - An engine system can take `LevelContext<IPlayers<PlayerView>>`; because `P` is `out`, a `LevelContext<MegaPlayers>` passes.
  - Tests need no fake: `MegaPlayers` is a plain class with no game dependency, so a test builds a real one with test entities (see `MegaPlayersTest`).
  - Cost: the type parameter spreads to every holder of the context; the core `typealias` hides it in game code.
  - Context: `targetFor` stays a member of `MegaPlayers` (owner decision, 2026-10-23); the alternative was an extension on `IPlayers<MegaPlayer>` so that code holding the interface could call it.

## Sources

- `NOTES.md` in this folder.
- `items/001-maverick-engine/NOTES.md`: how code reaches the game object, and the member counts.
