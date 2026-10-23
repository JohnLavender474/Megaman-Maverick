# 029: `IPlayer` contract, players service and `CharacterDef`

- Status: planned
- Type: refactor
- Depends on: none
- Related: `007`, `012`, `014`, `016`, `024`, `030`, `031`, `037`, `038`, `047`
- Branches: none yet

## Goal

Remove the global `megaman` and allow many playable characters (Mega, Proto, ...) that share a minimal contract but can change behaviors and physics easily.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `IPlayer` (body, facing, direction, dead and ready, health and damage, faction, focus, `isDoing`, locks, loadout, input). `Megaman` implements it.
- `game.players` (`all`, `byIndex`, `nearestTo`, `primary`). Migrate the about 889 external `megaman` uses in 224 files.
- Move lives, tanks and enhancements to a `PlayerProfile`.
- `CharacterDef`: movement table, ordered ability set, body def, animations, loadout. `MegamanDef` keeps behavior identical (proven by replay).
- A prototype second character (Proto Man) proves the contract.

## Owner's input

Answer to review item 28 (2026-10-22, verbatim):

> yes, there needs to be an `IPlayer` contract so that I can create as many different playable characters as possible (Mega, Proto, etc.) They should all adhere to a minimal contract, but being able to customize behaviors, physics, etc. for each should be easy

From the first request (summary): the global `megaman` is reached everywhere through `game.megaman`. This blocks other playable characters and multiplayer.

## Decisions

- H (2026-10-22): lives are a shared pool or per player, by game mode. Use an interface (for example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap. There will be a maximum number of players.
- 2026-10-23: the target is 2 players, but the design must scale to 4 or more. No type, array or rule may assume exactly 2.
- 2026-10-23: `029` keeps the current level-flow rules (camera, room transitions, death and respawn, pause, HUD). Its focus is the interface and the removal of the coupling to `megaman`. Gaps and ideas for the co-op rules go in `NOTES.md`.
- 2026-10-23: `PLAYER_SPAWN` must split into a spawn event for each player and a separate level reset. Which item does the split is still open.

## Open questions

- Target selection: is "nearest" enough, or do some enemies need "last attacker" or "random"?
- Must camera focus follow one player or a group bounds?
- Do all characters share the slide and ladder rules (X6 shares the step routines)?

## Sources

- `NOTES.md` in this folder.
