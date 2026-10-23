# Scope: mega-refactor-029a-player-seam

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Add the seam that the other `029` branches build on: a player slot, a player view and a players service, kept as small and abstract as possible in the engine. The game supplies its own view type with the components it requires. `Megaman` is registered as the only player, in slot 0. No call site outside the player package moves to the service yet. The game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes these parts of `ITEM.md`:

- From "Proposed scope": "`IPlayer` (...). `Megaman` implements it." and "`game.players` (`all`, `byIndex`, `nearestTo`, `primary`)". The decisions below replace both: there is no `IPlayer` interface on `Megaman` and no `primary`.
- Decisions (2026-10-23):
  - The target is 2 players, but the design must scale to 4 or more. No type, array or rule may assume exactly 2.
  - `029` keeps the current level-flow rules.
  - The players service has no public `primary` or `first()` in its query API. Each caller says which player it wants.
  - The players service is an interface plus a base class in the engine. The base class gives standard helpers out of the box, and a game can add its own helpers.
  - `029` adds a player slot prop to the player events. The `PLAYER_SPAWN` split is in `038`.
  - The engine makes no assumption about a player beyond its slot and its entity: not a body, not a position. Player identity follows the components direction of `048`: the game supplies a view type whose required components are checked once, at registration (choice A: `PlayerView` is an interface). The game passes a capacity of 1 for now.

## Current state

Scan on `504bcd015`. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` (`C/`) or `engine/src/main/kotlin/com/maverick/game/engine/` (`E/`). The full research is in the item `NOTES.md` (research round 2).

### The player and how it is reached

- `MegamanMaverickGame.megaman` is a `lateinit var` (`C/MegamanMaverickGame.kt:207`). `postCreate` makes it, calls `init()` by hand and adds it as a `GameState` listener (`:391-396`). It is not pooled.
- `IMegaGameEntity.megaman` is an extension that gives `game.megaman` (`C/entities/contracts/IMegaGameEntity.kt:21-22`). About 900 lines in about 240 files outside the player package use it. These stay as they are in this branch.
- `Megaman` extends `AbstractHealthEntity` and implements 16 interfaces (`C/entities/megaman/Megaman.kt:71-73`).

### Engine interfaces that a player contract can use

- `IBodyEntity` (`E/entities/contracts/IBodyEntity.kt`): `body`.
- `IFaceable` (`E/common/interfaces/IFaceable.kt`): `var facing`, `isFacing`, `swapFacing`. It has no read-only form.
- `IDirectional` (`E/common/interfaces/IDirectional.kt`): `var direction`. It has no read-only form.
- The engine has no player, slot or players concept.

### Global self-references inside the player package

These read the global `megaman` (that is, `game.megaman`) in place of `this`. With one player they give the same object. With two players they read the wrong one (review 1, section 4):

- `C/entities/megaman/Megaman.kt:629` (stun impulse), `:929` and `:933` (heart-tank max health), `:990` (`translateAmmo` default weapon).
- `C/entities/megaman/components/BodyComponent.kt:192-195` (head-bump sound velocity).
- `C/entities/megaman/components/SpritesComponent.kt:101` (hide the sprite while teleporting).
- `C/entities/megaman/components/BehaviorsComponent.kt:279` (air dash blocked when facing a block).
- `C/entities/megaman/sprites/MegamanAnimationKeySupplier.kt:42-43` (pipe portal) and `:48` (weapon key), inside `fun Megaman.getAnimationKey`.
- `C/entities/megaman/sprites/MegamanTrailSpriteV2.kt:50-71`: the trail sprite reads all of its data (animation key, weapon, position, sprite translation and rotation) from the global. The spawn site passes no owner.

### Player events

None of these events carries a player identity today. Submit sites:

| Event | Submitted at |
|---|---|
| `PLAYER_SPAWN` | `C/entities/megaman/Megaman.kt:507` |
| `PLAYER_JUST_DIED` | `C/entities/megaman/Megaman.kt:518` |
| `PLAYER_SPAWN_INIT` | `C/screens/levels/events/PlayerSpawnEventHandler.kt:113` |
| `PLAYER_READY` | `C/screens/levels/events/PlayerSpawnEventHandler.kt:196` |
| `PLAYER_DONE_DYIN` | `C/screens/levels/events/PlayerDeathEventHandler.kt:73` |
| `ADD_PLAYER_HEALTH` | `C/entities/items/HealthBulb.kt:51`, `C/screens/levels/stats/PlayerStatsHandler.kt:134` |
| `ADD_WEAPON_ENERGY` | `C/entities/items/WeaponEnergyBulb.kt:54` |
| `ADD_CURRENCY` | `C/entities/items/Screw.kt:50` |
| `ATTAIN_HEALTH_TANK` | `C/entities/items/HealthTank.kt:73` |
| `ATTAIN_HEART_TANK` | `C/entities/items/HeartTank.kt:148` |
| `STUN_PLAYER` | `C/entities/bosses/TimberWoman.kt:808` (a boss ground shake that stuns every player on the ground) |

The item sites run in `contactWithPlayer(megaman: Megaman)`, so the player in the contact is known there.

### Signatures typed `Megaman` that wait for `029`

- `Receivers.HIT_BY_PLAYER: (player: Megaman) -> Unit` (`C/world/body/Receivers.kt:42`), with the cast and `TODO` in `BodyPlayerRule` (`C/world/contacts/rules/ReceiverRules.kt:108-110`). Users: `SmallGrass`, `Togglee`, `SmallIceCube` (reads `canBeDamaged`).
- `ItemEntity.contactWithPlayer(megaman: Megaman)` (`C/entities/contracts/ItemEntity.kt:7`), with the `TODO` in `PlayerItemRule`. 7 items implement it. Only `Life` reads the player (`megaman.lives`), which is profile data.

These are not in this branch (see "Out of scope").

## In scope

1. **Engine player types** (a new package, for example `E/players/`):
   - `PlayerSlot`: a value type for the slot index. It does not limit the slot count.
   - `PlayerView`: an interface with `slot`, `entity` and `present` (default `entity.spawned`; a game can override it). Nothing else. No entity class implements it; the game writes a small view class.
   - `IGameEntity.require(type)`: gives the component or fails with a clear message (the entity and the component name). Views use it at registration.
   - `ComponentKey<T>` next to `PropertyKey<T>`, with a `get` operator on `IGameEntity` and `PlayerView`, for optional components (capability checks).
2. **Players service in the engine:**
   - `IPlayers<out V : PlayerView>`: `capacity` and `get(slot)`. Nothing else.
   - `Players<V>`: an open class with the storage, `register(slot, entity)` (makes the view through a game-supplied factory and checks that the slot is free and in range), `unregister(slot)` and `get(slot)`. The capacity comes from the game; the engine sets no limit.
   - Standard helpers as `inline` extension functions on `IPlayers`: `forEachPresent`, `any`, `all`, `firstOrNull`, `minByOrNull`. They iterate in slot order, a tie goes to the lower slot, and they do not allocate. Only a helper whose name says so (for example `forEachPresent`) filters by `present`; the others filter nothing, and the caller passes a predicate. This keeps today's behavior, because today's code reads `megaman` also while it is dead. No `primary`, no `first()`, no position.
   - A game adds its own helpers as extension functions in the same way.
3. **Core view and service:**
   - `MegaPlayer : PlayerView`: one non-null field for each required component, resolved with `require` at registration; accessors read through the component (for example `val body get() = bodyComponent.body`, because `BodyComponent.body` is a `var`). For this branch the required parts are only those that the core helpers below need (the body). Later branches add a field only when a moved call site needs a required part.
   - `MegaPlayers : Players<MegaPlayer>` with capacity 1.
   - Core helpers as extension functions, for example `nearestTo(point)` built on `minByOrNull`.
   - Rules: the view holds fields for required components only; every other helper is an extension. One view type for every character; no view subclass per character.
4. **`game.players`** in `MegamanMaverickGame`: a `MegaPlayers` with `Megaman` registered in slot 0, in `postCreate` after the current setup. `game.megaman` and the `megaman` extension stay unchanged. `Megaman` gets no new interface.
5. **Self-references:** replace the global `megaman` with `this` at the sites in "Current state". The trail sprite gets its player as an owner in its spawn props, and resets it on each spawn (it is pooled).
6. **Slot prop on the player events:** a new `ConstKeys` key (for example `SLOT`) with the `PlayerSlot` of the player, on the submit sites in the table above, except `STUN_PLAYER` (a broadcast to all players; it stays without a slot). Megaman's own events use its view's slot, read from `game.players` or kept on `Megaman` at registration. No listener reads the prop in this branch.
7. **Tests:**
   - Engine: `Players` with a capacity greater than 1 (register, unregister, a full or out-of-range slot, `get` of an empty slot), the helpers (slot order, `present` filter, `minByOrNull` tie to the lower slot, `any`, `all`, `firstOrNull`), a custom extension helper, `require` failure message, `ComponentKey` lookup.
   - Core: `MegaPlayer` registration fails without a `BodyComponent`; the slot prop is on the events where a test is practical with the relaxed game mock.

## Out of scope

- Moving any call site outside the player package to `game.players` (later `029` branches: enemies, bosses, world entities, level flow and UI).
- Removing or deprecating `game.megaman` or the `megaman` extension. A deprecation now would add about 900 compiler warnings. The last `029` branch removes them.
- More required parts on `MegaPlayer`, and capabilities such as `isDoing`, locks, `bounce`, gravity change, loadout and health. They come with the call sites that need them, as required fields or as optional components.
- Making facing, direction, `dead` or `ready` into components (`048`). Until then, a view accessor can read them from the entity in a later branch.
- The `HIT_BY_PLAYER` and `ItemEntity` retypes. They need `canBeDamaged` and `lives`, which are lock and profile concerns (later `029` branches).
- Listeners that read the slot prop, and the `PLAYER_SPAWN` split (`038`).
- Owner checks (`owner == megaman`) and exact-class damage sets (`030`).
- `PlayerProfile`, `CharacterDef`, input per player (`031`), level-flow rules (`047`).
- `LevelContext` (`016`): `game.players` moves there later.

## Affected areas and risks

- **Engine:** new files only. Risk: the `inline` helpers must stay in slot order and allocation-free; the unit tests check the order, and the plan step can measure allocation.
- **Required components and respawn:** `Megaman` keeps the same component objects across destroy and spawn (`reset()` runs on destroy), so a view made once at boot stays valid. Risk: a required component replaced with `addComponent` would make the cached field stale. Core does not do this on `Megaman`. A debug check (or a view rebuild on spawn) catches it.
- **Self-references:** with one player, `this` and `game.megaman` are the same object, so behavior does not change. Risk: the trail sprite owner. If the owner is not set or not reset on spawn, the trail is wrong or crashes. The spawn site is in `Megaman`.
- **Event props:** listeners read props by key, so a new key does not change them. Risk: a listener that copies or logs all props. The plan step checks this.
- **Boot:** `game.players` must exist before anything reads it, and `Megaman` must have its `BodyComponent` when it registers (after `init()`). Nothing reads `game.players` in this branch except the tests and the event submit sites.

## Overlap with other branches

- No branch is active. `001b` (host contract) is blocked; `019b` is planned.
- `001b` and `016` will decide where services live (host or `LevelContext`). `game.players` is a field for now; it moves with them.

## Open questions

1. Names: the engine package (`players`?), `PlayerSlot`, `PlayerView`, `IPlayers`, `Players`, `ComponentKey`, and the core `MegaPlayer` and `MegaPlayers`.
2. The slot prop key: a new `ConstKeys.SLOT`, or the existing `ConstKeys.PLAYER` (`"player"`)?
3. `STUN_PLAYER` without a slot means "all players": agree?
