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

None yet. The scan for the first branch will find them.

## Sources

- `NOTES.md` in this folder.
- `items/001-maverick-engine/NOTES.md`: how code reaches the game object, and the member counts.
