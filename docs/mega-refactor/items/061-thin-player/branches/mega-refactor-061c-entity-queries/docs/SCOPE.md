# Scope: mega-refactor-061c-entity-queries

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and `PLAN.md`. The user
approves it. If the scope changes later, update this file and get approval again.

- Status: approved
- Approved: 2026-10-26

## Goal

A player is an entity with a player component, not a view object. The `MegaPlayer` view class goes: the slot lives in
a `PlayerComponent(slot)` on the entity, the players registry maps each slot to its entity, and the input stays bound
to the entity's `ControllerComponent`. Callers ask the entity through entity-level query functions that work for any
entity, and `MegaPlayerAccess.kt` goes. The game plays the same.

## Item

`061`: "next: `061c`" in `ITEM.md`; the decision of 2026-10-26 "no player wrappers for entity questions; a player
view only finds the entity"; and the owner's question of 2026-10-26 ("What if we used a component instead of the view
class?"), answered by the research in item `029` `NOTES.md` (Bevy `Player` component and queries, Unity `PlayerInput`
component with `playerIndex`, Godot groups; Unreal's separate `PlayerController` exists because it outlives the body,
which here the registry and the slot-keyed stores already do).

## Current state

Based on `master` at `5c7c60b53`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`.

- **Engine players:** `E/players/PlayerSlot.kt`, `PlayerView.kt` (`slot`, `entity`, `present` = `entity.spawned`),
  `IPlayers.kt` (`IPlayers<V : PlayerView>` with `forEach`, `any`, `all`, `viewOf`, `slotOf`, `registeredSlots`),
  `Players.kt` (a view per slot, built by a factory; `register`, `unregister`). Test: `PlayersTest`.
- **Core view:** `C/players/MegaPlayer.kt` (`slot`, `entity`, `input`; at construction it binds its input source to
  the entity's `ControllerComponent` and adds `PlayerComponent(this)`), `MegaPlayers.kt` (the registry, `inputs`,
  `targetFor`, `playerDamaged`), `PlayerComponent.kt` (holds the view), `PlayerExtensions.kt` (`asPlayer`: the view
  from the component; `megaman`: a downcast; `shooting`), `MegaPlayerAccess.kt` (22 forwarding functions),
  `PlayerDamaged.kt` (signal payload with the view). The view type appears in 25 core files (the items'
  `contactWithPlayer`, the `HIT_BY_PLAYER` receiver, the boss damage listener, the screens and handlers).
- **Who reads a view:**
  - `targetPlayer` (`C/entities/contracts/IMegaGameEntity.kt`, `players.targetFor(this)`, the first player with a
    body): 147 files; 423 `body`, 23 `direction`, 15 `dead`, about 10 others; view-only uses: `slot`
    (`AbstractEnemy.kt:103,332`), `input` (`SpiderWeb.kt:191`, `GroundSlideBlocker.kt:46`), `present`
    (`LaserBeamer.kt:248`), `entity` (`DarknessV4.kt:564`).
  - `asPlayer`: 150 uses in 64 files (about half `!= null` checks).
  - `players` loops: `any` 70, `forEach` 28, `all` 11; `get(PlayerSlot(0))` in the screens; the screens' and
    handlers' `player` (about 106 member uses).
- The per-player data that outlives the body is already outside the view: `PlayerInputs` (frames by slot) in
  `MegaPlayers`, lives by slot in `GameState`. Megaman is never pooled; he respawns as the same object.
- **Names:** `target` is declared in 36 files (rejected); the members `body`, `facing`, `direction` are declared in
  354, 148 and 108 files, and an extension on `IGameEntity` with the same name applies only where the receiver has no
  member.

## In scope

1. **Engine players as components:**
   - `PlayerComponent(slot: PlayerSlot)` moves to the engine (`E/players/`); slots are a genre concept.
   - `Players` becomes a registry of slot → `IGameEntity` (no view, no factory): `register(slot, entity)` adds the
     `PlayerComponent`, `unregister(slot)` removes it (this also closes the old `TODO` that the view stayed on the
     entity), `get(slot)`, and the loops over entities in slot order (`forEach`, `any`, `all`, `registeredSlots`).
     Free functions: `IGameEntity.playerSlot` (`PlayerSlot?` from the component) and `isPlayer`.
   - `PlayerView` and `viewOf` go; `present` becomes `spawned` at its one caller.
2. **Core registry:** `MegaPlayers` registers an entity and binds its input source to the entity's
   `ControllerComponent` (the work that `MegaPlayer`'s constructor did). `targetFor` returns the entity. The damage
   signal's payload carries the entity. `MegaPlayer` and `MegaPlayerAccess.kt` go.
3. **Entity-level query functions** on `IGameEntity`, the same rules as today (needed parts fail at the read, optional
   parts give an empty value):
   - Engine (engine types only): `body`, `hasBody`, `facing`, `direction`, `isBehaviorActive`, `isAnyBehaviorActive`,
     and `input` (the entity's `ControllerComponent` source; `RELEASED` without one).
   - Core (game types): `focus`, `invincible`, `damaged`, the health functions, `lightOrNull`, `onGravityDeviceSwitch`,
     the six fixture getters, `shooting` (Megaman-only, with the `024` `TODO`).
4. **Callers:** `targetPlayer` and `asPlayer` return the player's entity (see "Open questions" for the names), so
   `targetPlayer.body`, `x.asPlayer?.body` and `players.any { it.body ... }` read the entity directly. The screens and
   handlers hold the player entity. The view-only uses read `playerSlot`, `input`, `spawned`. The `megaman` accessor's
   19 uses become explicit `as Megaman` casts (see "Open questions"). The items' `contactWithPlayer`, the
   `HIT_BY_PLAYER` receiver and `AbstractBoss.onPlayerDamaged` take the entity.
5. **Tests:** the engine `PlayersTest` (registry, component on register and off on unregister, slot order); the entity
   query functions (needed parts fail, optional parts empty); `MegaPlayersTest` (input binding, `targetFor`, the damage
   signal); the tests that build `Megaman` and register him.

New APIs and their first callers:

| API | First callers |
|---|---|
| Engine `PlayerComponent(slot)`, `playerSlot`, `isPlayer`, the entity registry | `MegaPlayers`, `asPlayer`, `AbstractEnemy` (life drops), `PlayerSpawnEventHandler` and the other slot readers |
| Engine `body`, `hasBody`, `facing`, `direction`, behavior queries, `input` | the `targetPlayer`, `asPlayer` and `player` call sites; `targetFor` (`hasBody`) |
| Core `focus`, `invincible`, `damaged`, health, light, gravity device, fixtures, `shooting` | the same call sites (screens, hazards, blocks, `Darkness*`, `GravitySwitcharoo`, `AbstractEnemy`) |

## Out of scope

- `dead` and `spawned` (item `062`); this branch reads `dead` as today.
- Facing and direction as components (item `048`); the functions read the interfaces.
- Targeting rules (`047`), faction for the side checks of `asPlayer` (`030`), removing the `megaman` downcasts (`024`,
  `060`).

## Intended changes

None. Every read is the same field or call as today.

## Affected areas and risks

- About 200 files, mostly mechanical: imports, `player.X` reads, the 25 files that name the view type, the engine
  players package and its test. A script makes the repeated edits; the compiler checks each receiver type.
- **Shadowing:** members always win in Kotlin, so a read that compiled against a member keeps it. The risk is a call
  site where the receiver changes from a view to an entity whose class has its own member with another meaning
  (only possible where the entity's static type is a concrete class). The one-for-one check compares each changed
  read.
- **Registration order:** `PlayerComponent` is added at `register` (as the view did), after `Megaman.init()`; the
  input binding stays at the same moment.
- **Performance:** the same lookups as today; `input` must not box the frame (the free function returns
  `InputFrame` with no nullable step).

## Overlap with other branches

No branch is active in a worktree. Item `062` will change `dead` and `spawned`; this branch only moves where they are
read from.

## Open questions

1. **Names.** Proposal: keep `targetPlayer` and `asPlayer`, now returning the player's entity (`asPlayer`: "this
   entity as a player, or `null`"). That keeps the call sites short and the diff small. Other choice: `targetEntity`
   and `playerEntityOrNull`.
2. **The `megaman` downcast.** Proposal: replace the accessor with explicit `as Megaman` casts at its 19 uses, so the
   downcasts stay visible until items `024`/`060` remove them. Other choice: keep a `megaman` extension on
   `IGameEntity`.
3. **Engine or core** for `PlayerComponent`, the registry and `body`/`facing`/`direction`/behaviors/`input`.
   Proposal: the engine.
4. **One branch.** Removing the view and moving the callers cannot be split: the forwarding functions are extensions
   on the view, so they go with it. Proposal: one branch, with commits in batches.

## Decisions

- 2026-10-26 (owner, "Scope approved; go with your suggestions for all 4 questions"): `targetPlayer` and `asPlayer`
  keep their names and return the player's entity; the `megaman` accessor becomes explicit `as Megaman` casts at its
  uses; `PlayerComponent`, the registry and the engine-type queries go in the engine; one branch, commits in batches.
