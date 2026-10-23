# 029: `IPlayer` contract, players service and `CharacterDef`

- Status: active
- Type: refactor
- Depends on: none
- Related: `007`, `012`, `013`, `014`, `016`, `024`, `030`, `031`, `037`, `038`, `047`, `053`, `057`
- Branches: `029a` (merged), `029b` (merged), `029c` to `029g` (planned)

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

Research round 2 (2026-10-23, verbatim):

> A lightweight comprehensive interface that defines the contract for any and all players in the game
>
> Support for co-op (for now, we can assume support for multiple players on the same screen, with multiplayer splitscreen and online as TODOs), so no logic can be tightly coupled to just a single player instance

> we can't predict each and every single "helper" function that enemies or other objects will need when it comes to querying players. Therefore, here's what I'm thinking: we have an interface and base class in the engine, and the base class provides out-of-the-box helper funcs while also facilitating the addition of custom helper funcs.

## Decisions

- H (2026-10-22): lives are a shared pool or per player, by game mode. Use an interface (for example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap. There will be a maximum number of players.
- 2026-10-23: the target is 2 players, but the design must scale to 4 or more. No type, array or rule may assume exactly 2.
- 2026-10-23: `029` keeps the current level-flow rules (camera, room transitions, death and respawn, pause, HUD). Its focus is the interface and the removal of the coupling to `megaman`. Gaps and ideas for the co-op rules go in `NOTES.md`.
- 2026-10-23: `PLAYER_SPAWN` must split into a spawn event for each player and a separate level reset. The split goes to `038` (inside the `013` flow). `029` only adds a player slot prop to the player events.
- 2026-10-23: the players service has no public `primary` or `first()` in its query API. Each caller says which player it wants (nearest, the one in the contact, any, all, the owner, slot `n`). A session-level lead slot for the camera and the save (see `047`) is a policy, not a query helper.
- 2026-10-23: the players service is an interface plus a base class in the engine. The base class gives standard helpers out of the box, and a game can add its own helpers. Not all helpers can be known ahead of time.
- 2026-10-23: `029` follows the components direction of `048`. There is no `IPlayer` interface on the player entity. The engine assumes only a slot and an entity (no body, no position). `PlayerView` is a small engine interface (choice A), and the game supplies its view type, which resolves the components it requires once, at registration. Engine helpers are deterministic `inline` extensions (slot order, ties to the lower slot); spatial helpers such as `nearestTo` are game helpers. Research: `029a` `docs/NOTES.md`.
- 2026-10-23: the game passes a players capacity of 1 until co-op (`047`).
- 2026-10-23: call sites name their intent (`targetFor`, `any`, the contact entity, `forEachPresent`, `require(PlayerSlot(0))`), not a rule and not `players[0]`. Targeting is a `TargetPolicy` with a game default (slot 0 in `029`) and an optional per-entity `TargetingComponent`; a policy returns one player. Details: "Targeting design" in `NOTES.md`.

## Branch split

Proposed 2026-10-23. The owner asked for the split. Each later branch gets its own scope, which can still change this list.

| Branch | Goal | Notes |
|---|---|---|
| `029a` `player-seam` | Player slot, small `IPlayer`, players service (engine interface plus base class); `Megaman` in slot 0; the global self-references in the player package; slot prop on the player events | No call site outside the player package moves |
| `029b` `enemy-players` | Enemies (about 395 lines, 104 files) use `game.players` helpers in place of `megaman` | Adds `targetFor`, `TargetPolicy` and the `SlotZero` default (see "Targeting design" in `NOTES.md`). Proximity uses `any`. `MegaPlayer` gets a required field only when a moved site needs it |
| `029c` `boss-players` | Bosses (about 140 lines, 22 files), `AbstractBoss`, the boss damage listener (`IMegamanDamageListener`) | |
| `029d` `world-players` | Special, projectiles, blocks, hazards, sensors, decorations, world code: `is IPlayer` checks, kill zones per contact player, stomp bounce, gravity devices, `HIT_BY_PLAYER` retype | Owner checks stay for `030` (open question) |
| `029e` `player-profile` | Lives, tanks, enhancements and weapons owned move to one session profile; `ItemEntity` retype (`Life` reads lives) | With `007` (save) |
| `029f` `level-players` | Level screen, handlers, HUD, pause, menus, camera focus use `game.players` (slot 0 by name where needed); remove `game.megaman` and the `megaman` extension | Rules unchanged (decision 2026-10-23) |
| `029g` `character-def` | `CharacterDef`, movement values table, ordered ability list, thin player base | Weapon couplings in behaviors overlap `024` |

Order: `029a` first. `029b`, `029c` and `029d` can run in any order after it. `029e` before `029f`, because the HUD and pause read the profile. `029g` last.

## Open questions

- Target selection: is "nearest" enough, or do some enemies need "last attacker" or "random"?
- Must camera focus follow one player or a group bounds? (Out of scope for `029`; the owner's direction is in `047`.)
- Do all characters share the slide and ladder rules (X6 shares the step routines)?
- `owner == megaman` checks: does `029d` change them to `is IPlayer` (the same result with one player), or do they all wait for `030` (faction)? If they wait, the `megaman` extension cannot be removed in `029f`.
- Proto Man prototype: part of `029` (after `029g`) or a separate feature item?

## Sources

- `NOTES.md` in this folder.
