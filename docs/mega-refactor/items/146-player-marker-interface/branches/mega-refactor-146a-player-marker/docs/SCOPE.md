# Scope: mega-refactor-146a-player-marker

- Status: approved
- Approved: 2026-11-10 (first scope); 2026-11-10 (tags, D5-D7); 2026-11-10 (spawn flow, D10)

## Goal

Give a player a type. The engine gets a marker `IPlayer : IGameEntity` with a `slot`. `Megaman` and `MegamanX`
implement it. The player call sites take and return `IPlayer` in place of `MegaGameEntity`. No core `IMegaPlayer` (D1).
Refactor only: nothing changes in play.

## Item

`146`. From `ITEM.md`, proposed scope:

- Engine: a marker `interface IPlayer : IGameEntity` with a non-null `slot` (from the `PlayerComponent` that
  `Players.register` adds; a read before the registration throws a custom exception). `Players<E : IPlayer>`.
- Core (proposed; replaced by D1): `interface IMegaPlayer : IPlayer` with what the player call sites read; `Megaman` and `MegamanX` implement it;
  `MegaPlayers : Players<IMegaPlayer>`; `asPlayer` gives `IMegaPlayer?`; functions take `IMegaPlayer` in place of
  `MegaGameEntity`.
- Tests: the `register` calls; `TestHealthPlayer`, `SecondPlayerEntity` and `mockk<MegaGameEntity>` players implement
  `IMegaPlayer`.

Owner decisions of 2026-11-10 (in `ITEM.md`): the marker design (not `PlayerView`), merge directly into `master`, start
in parallel with `060a` and `081f`, and `Event.isDeathOf` changes here.

## Current state

The full scan, with `file:line` references, is in `temp/mega-refactor-146a-player-marker/scan.md` (main checkout).
Summary:

- Engine: `IPlayers<out E : IGameEntity>` (`engine/.../players/IPlayers.kt:6`), `Players<E : IGameEntity>`
  (`Players.kt:5`), `PlayerComponent` with `playerSlot: PlayerSlot?` and `isPlayer` (`PlayerComponent.kt:6-12`).
- Core: `MegaPlayers : Players<MegaGameEntity>` (`players/MegaPlayers.kt:21`); `asPlayer: MegaGameEntity?` and
  `rootPlayer` and `Event.isDeathOf(entity: IGameEntity?)` (`players/PlayerExtensions.kt`).
- Uses in `core/src/main`: `asPlayer` 91 (70 of them only a null check), `targetPlayer` 437 lines in 144 files,
  `playerSlot` 31 (26 with `!!`), `isPlayer` 7, `rootPlayer` 4, `isDeathOf` 2. `MegaPlayers` calls: `forEach` 60,
  `anyLive` 40, `get` 16, `all` 11, `isLive` 9, `any` 8, `forEachLive` 4.
- Reads on a player value: almost all go through extensions on `IGameEntity` (`body`, `facing`, `direction`,
  `inPlay`, `invincible`, `damaged`, `focus`, the fixture getters). `existenceState` is an `IGameEntity` member. The
  only `MegaGameEntity` member read is `services`, at `PlayerDamageNegotiator.kt:33`, which gets `this`.
- 14 groups of APIs take a player as `MegaGameEntity`, for example `ItemEntity.contactWithPlayer` (7 implementations),
  `AbstractBoss.onPlayerDamaged`, `PlayerRoster`, `DarknessV4`, `SpiderWeb`, the koopa shells, `MagFly`,
  `LevelRoomHandler`, the respawn and camera classes, and 6 screen `player` getters.
- `PlayerRoster` keeps `Megaman` and `MegamanX` both, and registers only one in slot 0 (`PlayerRoster.kt:28-50`). So a
  class that implements the player type is not always a player.
- Tests: 39 `register(` calls in 26 core test files and 15 in the engine `PlayersTest`. Player doubles:
  `TestHealthPlayer`, `SecondPlayerEntity`, `TestPlayerEntity`, `TestPlayer`, `FieldsTestPlayerEntity`,
  `RulesTestEntity`, `SitePlayer`, `Walker`, engine `ScoredEntity`; one `mockk<MegaGameEntity>` player
  (`LevelRoomHandlerTest.kt:71`).

## In scope

- Engine: `IPlayer`, `PlayerNotRegisteredException`, `IPlayers<out E : IPlayer>`, `Players<E : IPlayer>`, the inline
  helpers with the new bound. `PlayerComponent` stays the source of the slot.
- Core: `Megaman`, `MegamanX` and the player test doubles implement `IPlayer`. `MegaPlayers : Players<IPlayer>`.
  `asPlayer`, `rootPlayer`, `targetFor`, `targetPlayer` return `IPlayer`. `Event.isDeathOf(player: IPlayer)`.
- The 14 groups of APIs and the functions with a `player: MegaGameEntity` parameter take `IPlayer`.
- `playerSlot!!` on a value of the player type becomes `slot`.
- Tests: engine tests for `IPlayer.slot` (the throw before registration and after unregistration); the test doubles
  implement `IPlayer`; the `mockk` player becomes a real double.

## Out of scope

- The `is Megaman` / `as Megaman` checks (`O8-R2`, item `024`).
- Targeting design (item `112`): only the return type of `targetFor` and `targetPlayer` changes.
- Removal of `IMegaGameEntity` (item `072`).
- Any change to which entity is a player, to the roster, or to registration order.

## Affected areas and risks

- Many files change signatures (the call-site counts above). Compile errors find each one; the risk is in the places
  that still need a `MegaGameEntity` (for example `IOwnable<MegaGameEntity>` owners, event props, map keys).
- `asPlayer` must keep the component check. A bare `is IPlayer` would make the roster character that is not in a
  slot count as a player.
- Shadowing: if `IPlayer` extends `IBodyEntity`, `IFaceable` or `IDirectional`, about 560 reads move from the
  extension to the member. The design does not extend them.

## Overlap with other branches

- `060a` (`mega-refactor-060a-shared-run-body`): changes `Megaman.kt`, `megaman/components/BodyComponent.kt`,
  `ControllerComponent.kt` and new `players/PlayerBody.kt`, `PlayerGravity.kt`, `PlayerRun.kt`. Its functions use
  generic bounds (`fun <T> T.playerRun(...) where T : ...`), which can add `IPlayer` later. Conflict expected in
  the class header of `Megaman.kt` only. `060a` is near its play test; this branch merges `master` in after it lands.
- `081f` (`mega-refactor-081f-x6-intro-fixes`): changes `D1000*`, `MegamanXSprites.kt`, `MegamanXSabreSlash.kt`,
  `Stuns.kt`, `StunComponent.kt`, `PlayerDamageNegotiator.kt` and the D-1000 tests. Overlap: `Stuns.kt` and
  `PlayerDamageNegotiator.kt` (both read the player), and any `targetPlayer` lines in the `D1000` files.

- `081f` (tags, 2026-11-10): 3 new `081f` entities use `getTag()` and skip `super.init()`; see `NOTES.md`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | A marker interface, not a class layer. The player state stays in `PlayerComponent`; no logic moves into the interface. |
| `O1-R4` Typed state | helps | The player type replaces the nullable slot lookup and `!!`. |
| `O1-R5` Code only with a caller | no effect | See "New API and first caller". No `IMegaPlayer` (D1). |
| `O2-R4` Stable order, slot order | no effect | Iteration order of `Players` does not change. |
| `O2-R5` No logic assumes one player | no effect | No logic changes. |
| `O2-R9` Events name a slot | helps | `isDeathOf` compares the event slot with a typed `slot`. |
| `O5-R1` No per-frame allocation | no effect | Interfaces and casts allocate nothing. |
| `O6-R1` CI passes | no effect | Checked at merge. |
| `O8-R2` Capabilities, not character checks | no effect | The `is Megaman` checks stay; out of scope. |

## Decisions

Owner answers to the brief, 2026-11-10:

- D1: no core `IMegaPlayer` now. Use the engine `IPlayer` everywhere. A core type comes when its first member needs
  it (`O1-R5`). This replaces the `IMegaPlayer` of the item's proposed scope.
- D2: `IPlayer.slot` throws `PlayerNotRegisteredException` on an entity that is not in a slot. `asPlayer` keeps the
  component check. The sites that read a slot that can be absent keep `playerSlot`.
- D3: `targetFor` and `targetPlayer` return `IPlayer` in this branch. Item `112` still replaces the targeting rule.
- D5 (2026-11-10, tags): a subclass of `Block`, `AbstractBlock`, `Switch` or `Death` has its own name and the base
  name, in that order (for example `{"FeetRiseSinkBlock", "Block"}`). `AbstractBlock` itself gets
  `{"AbstractBlock", "Block"}`.
- D6 (2026-11-10, tags): two components. `TagsComponent` in `core` (`entities/state/`), fixed per class at `init()`,
  indexed by `MegaGameEntities`. `GroupsComponent` stays as it is.
- D7 (2026-11-10, tags): no single-tag accessor. Readers use `hasTag(x)` and `hasAnyTag(set)`; the index uses each
  tag; `PlayerDamageNegotiator` takes the first tag in order that has an entry; a class that reads its own tag uses
  its `TAG` constant; logs print all tags. `ITaggable` and `getTag()` are deleted.
- D10 (2026-11-10, spawn flow): `TellySaucer.canSpawn` picks its side from the live player nearest the spawn
  center, through a new `MegaPlayers.nearestLiveTo(point)` that `targetFor` also uses. New API, first callers:
  `targetFor`, `TellySaucer.canSpawn`.
- D9 (2026-11-10, during the Diffity round): a base override calls `super.defineTagsComponent()` and adds its `TAG`
  with `TagsComponent.add`, so the tags add up along the class chain, root first (for example
  `GroundSlideBlocker, Block, AbstractBlock`). The 7 classes with a different tag still replace the default. The owner
  finds tag changes at runtime advisable; `remove` waits for a caller (backlog candidate), and the index must then
  follow the changes.
- D8 (2026-11-10, step 7): `MegaGameEntity.init()` adds the `TagsComponent`. 52 entity `init()` overrides did not
  call `super.init()` (harmless while `GameEntity.init()` was empty); each gets the call. The tag golden smoke test
  calls `init()` on every class, so a class that skips `super.init()` fails it.
- D4 (2026-11-10, during step 3): `WarpSpotTest` passed a player to the block filters and to
  `playerAllowsDamageFrom`, which both took a `MegaGameEntity`. Neither needs that class. The block filters become
  `(IGameEntity, IGameEntity) -> Boolean`; the 2 tag filters (`Block`, `MoonScythe`) check `is ITaggable`.
  `playerAllowsDamageFrom` takes the receiver `IPlayer` and a `rules: SessionRules` parameter; `WarpSpotTest` gets the
  rules in its constructor. No cast helper from `IPlayer` to `MegaGameEntity`.
- Plan change (2026-11-10, step 4): the `LevelRoomHandlerTest` player stays a relaxed mock, typed `IPlayer`, not a
  real double. The test reads only the body component, which it stubs. The approved scope said "a real double".

## New API and first caller

| API | First production caller |
|-----|-------------------------|
| `IPlayer` | `Players<E : IPlayer>`, `asPlayer` |
| `IPlayer.slot` | `Event.isDeathOf` and the former `playerSlot!!` sites |
| `PlayerNotRegisteredException` | `IPlayer.slot` |

## Scope change 2026-11-10: tags

Owner: tags are a `core` idea (no code in `engine/` reads a tag), and a tags component is the right home, because it
can serve other purposes later. Investigation: `temp/mega-refactor-146a-player-marker/tags/report.md`.

Current state:

- `ITaggable` (`engine/.../common/interfaces/ITaggable.kt`) has no reader in `engine`. In `core`, `IMegaGameEntity`
  extends it; `MegaGameEntity.kt:78` gives the default `this::class.simpleName`.
- 155 overrides `override fun getTag() = TAG`. 144 repeat the class name with no subclass. 4 bases (`Block`,
  `AbstractBlock`, `Switch`, `Death`) give 34 subclasses the parent's tag. 7 differ from the class name; 2 of them
  have readers (`SniperJoe_OLD` -> `"SniperJoe"`, in 13 map `block_filters` and `DecoratedGravityForce`;
  `BigAssMaverickRobotHand` -> `"BigAssMaverickRobot/Hand"`, in the damage table).
- `getTag()` readers: 41 in `core/src/main`, 2 in tests. `MegaGameEntities` keeps a tag index (`add`/`remove` in
  `onSpawn`/`onDestroy`). `HealthBulb`, `Screw` and `WeaponEnergyBulb` use `getTag()` as an atlas key.
- `GroupsComponent` and `GroupsSystem` (engine) hold names per instance from map data; `reset()` clears them.

Design (owner, 2026-11-10):

- A `TagsComponent` in `core`. `MegaGameEntity.init()` adds it from `protected open fun defineTagsComponent()`.
  The default holds 1 tag, `this::class.simpleName` (the runtime class, so a subclass gets its own name). The tags are
  fixed at `init()`; `reset()` keeps them (a pooled entity keeps its tags).
- The 148 `getTag() = TAG` overrides that repeat the default go. The 7 that differ override `defineTagsComponent()`.
- `MegaGameEntities` indexes an entity under each of its tags.
- `ITaggable` is deleted when no reader is left.

In scope: the component, the default, the overrides, the index, the readers, the tests, `CLAUDE.md`.

Out of scope: a tag index as a system (like `GroupsSystem`); tags from map data; any change to `TAG` as the atlas
prefix or the log tag.

New API and first caller:

| API | First production caller |
|-----|-------------------------|
| `TagsComponent` | `MegaGameEntity.init()` |
| `defineTagsComponent()` | `MegaGameEntity.init()`; overrides in the 7 classes with a different tag |
| Several tags per entity | `Block`, `AbstractBlock`, `Switch`, `Death` (D5) |
| `hasTag`, `hasAnyTag`, `tags` | the block filters, `MoonScythe`, `Force`, `FallingIcicle`, `SpikeTeeth`, `TimberWoman`, `LevelDefinition`, `PlayerDamageNegotiator`, logs |

Objective check (tags):

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | helps | A component replaces an interface. |
| `O1-R4` Typed state | no effect | Tags stay strings. |
| `O1-R5` Code only with a caller | no effect | Several tags: first caller the 4 bases (D5). |
| `O3` web | risk | The default reads `simpleName`; a future minifier can rename classes (`088` `NOTES.md:70`: it ran on TeaVM). |
| `O5-R1` No per-frame allocation | no effect | Tags are built once at `init()`; block filters read them per collision with no allocation. |

## Scope change 2026-11-10: spawn flow (layers 1 and 2)

Found at the play test: `canSpawn` runs before `init()` (see `NOTES.md`, "Spawn flow research").

In scope:

- Layer 1: `GameEngine.spawn()` runs `init()` (once per instance) before `canSpawn`; `spawnNow` no longer inits. The
  factory preload sets `initialized` (fixes the double `init()`; bug candidate closed).
- Layer 2: the `canSpawn` contract: it reads `init()` data, the spawn props and the session, never per-spawn fields,
  and it writes nothing. `HeartTank` and `HealthTank` parse their value in `onSpawn` (no field write in `canSpawn`).
  `TellySaucer` chooses its side without its own body (D10). `Cart`'s `it != this` stays (identity, not state).
  `CLAUDE.md` states the contract.

Out of scope: class facts and spawn rules as data (`021`); stale per-spawn fields in other classes (`Elecn`,
`MagFly`, `Sealion`: bug candidates).

New API: none (one engine line moves).

## Open questions

None.
