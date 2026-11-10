# 146: A marker `IPlayer` interface, so a player has a type

- Status: active
- Type: refactor
- Objectives: `O1`, `O8`
- Depends on: none
- Related: `029` (player contract, decision of 2026-10-23), `048` (components), `047c`
- Branches: `146a` (`mega-refactor-146a-player-marker`)

## Goal

A player has no type of its own today. It is a `MegaGameEntity` with a `PlayerComponent`, and its slot is an optional
component lookup:

```kotlin
val IGameEntity?.asPlayer: MegaGameEntity?
val IGameEntity.playerSlot: PlayerSlot?
```

So the type system cannot tell a player from any other entity. Player code checks at runtime and unwraps the slot, and
a helper can read as if it works for any entity when it works only for players (example: `Event.isDeathOf(entity:
IGameEntity?)`, added in `047c`). Counts at `047c` (2026-11-09, `core/src/main`): `asPlayer` 156 uses in 67 files,
`playerSlot!!` 26 uses in 16 files, 45 functions with a parameter `player: MegaGameEntity`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Engine: a marker `interface IPlayer : IGameEntity` with a non-null `slot` (from the `PlayerComponent` that
  `Players.register` adds; a read before the registration throws a custom exception). `Players<E : IPlayer>`.
- Core: `interface IMegaPlayer : IPlayer` with what the player call sites read (for example `IBodyEntity`,
  `IFaceable`, `IDirectional`); `Megaman` and `MegamanX` implement it; `MegaPlayers : Players<IMegaPlayer>`;
  `asPlayer` gives `IMegaPlayer?`; functions take `IMegaPlayer` in place of `MegaGameEntity`.
- First scan step: list the `MegaGameEntity` members (for example `services`) that the player call sites use. That
  list sets what `IMegaPlayer` must contain, because Kotlin has no type for "a `MegaGameEntity` that is also an
  `IPlayer`".
- Tests: about 54 `register` calls; `TestHealthPlayer`, `SecondPlayerEntity` and `mockk<MegaGameEntity>` players
  implement `IMegaPlayer`.
- Alternative (from the `029a` notes, option 2, not built): a `PlayerView` handle per player. It costs a second object
  per player and a move between the view and the entity at the call sites.

## Owner's input

2026-11-09, at the `047c` diff review, on `Event.isDeathOf(entity: IGameEntity?)`: "Code like this makes me wonder if we
need a dedicated `Player` class or interface. It reads so weird that the receiver param is `IGameEntity?` (nullable,
generic entity), so the func implies that it works for any given entity, but the implementation actually works only for
players." Then: "It's typical to have 'marker interfaces'. What would it look like if the `IPlayer` interface extended
`IGameEntity` and required the player component to be present?" Then: "No fix for now, go ahead and log a new item".

## Decisions

- 2026-11-10: the marker design. The engine gets `IPlayer : IGameEntity` with a non-null slot, and core gets
  `IMegaPlayer : IPlayer`. Not the `PlayerView` handle. This changes the `029` decision of 2026-10-23 for a marker
  that holds only the slot; the decision is also recorded in `029`.
- 2026-11-10: merge target: each branch squash-merges directly into `master`.
- 2026-11-10: the branch starts now, in parallel with `060a` and `081f` (not after `060a` merges). Expect conflicts in
  `players/`, `Megaman` and `MegamanX`; the branch merges `master` in after each of them lands.
- 2026-11-10: `047c`'s `Event.isDeathOf` changes in this item, to take the player type.

## Open questions


## Sources

- `NOTES.md` in this folder.
- `029` `ITEM.md` ("Decisions", 2026-10-23) and the `029a` branch `docs/NOTES.md` (the `PlayerView` options).
- `047c` session of 2026-11-09 (the scan counts above).
