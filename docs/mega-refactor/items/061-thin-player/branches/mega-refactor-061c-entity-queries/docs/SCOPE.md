# Scope: mega-refactor-061c-entity-queries

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and `PLAN.md`. The user
approves it. If the scope changes later, update this file and get approval again.

- Status: draft
- Approved: no

## Goal

Callers ask the entity, not the player view (item `061` decision of 2026-10-26: "no player wrappers for entity
questions"; a player view only finds the entity). The functions in `players/MegaPlayerAccess.kt` become entity-level
functions that work for any entity, targeting gives the target's entity, and `MegaPlayerAccess.kt` goes. The player
view keeps `slot`, `entity` and `input`. The game plays the same.

## Item

`061`, "next: a branch that moves the `MegaPlayerAccess.kt` callers to entity-level functions" (`ITEM.md`).

## Current state

Based on `master` at `10927055f`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`.

- `C/players/MegaPlayerAccess.kt` has 22 public functions on `MegaPlayer`, each reading `entity`: `body`, `hasBody`,
  `direction`, `facing`, `focus`, `dead`, `invincible`, `damaged`, the five health functions, `lightOrNull`,
  `onGravityDeviceSwitch`, `isBehaviorActive`, `isAnyBehaviorActive`, and six fixture getters. Needed parts fail at
  the read; optional parts give an empty value. `C/players/PlayerExtensions.kt` has `megaman` (a downcast, items
  `024`/`060`) and `shooting`.
- How callers reach a player:
  - `targetPlayer` (`C/entities/contracts/IMegaGameEntity.kt`: `game.players.targetFor(this)`, the first player with
    a body): 147 files; 423 `body` reads, 23 `direction`, 15 `dead`, about 10 others. Only 6 uses need the view
    itself: `slot` (`AbstractEnemy.kt:103,332`, life drops), `input` (`SpiderWeb.kt:191`,
    `GroundSlideBlocker.kt:46`), `present` (`LaserBeamer.kt:248`), `entity` (`DarknessV4.kt:564`).
  - The screens and handlers' `player` (about 106 uses: HUD health, the camera focus, the spawn and death sequences)
    and `players` loops (about 77 uses).
- `MegaPlayer.entity` is typed `IGameEntity` (the engine `PlayerView`), so `dead` (a `MegaGameEntity` member) needs a
  cast today; every player entity is a `MegaGameEntity`.
- Names: `target` is declared in 36 files, so an extension named `target` would be shadowed by those members;
  `targetEntity` is declared nowhere. The members `body`, `facing` and `direction` are declared in 354, 148 and 108
  files; an extension on `IGameEntity` with the same name is used only where the receiver has no member (for example
  the target entity), so it does not change any existing read.

## In scope

1. **Entity-level query functions**, the same rules as today (needed parts fail at the read, optional parts give an
   empty value), on `IGameEntity`:
   - Engine (they use only engine types; genre concepts): `body` (requires `BodyComponent`), `hasBody`, `facing`
     (`IFaceable`), `direction` (`IDirectional`), `isBehaviorActive`, `isAnyBehaviorActive` (`BehaviorsComponent`,
     `false` if absent). Proposed file: `E/entities/EntityQueries.kt`.
   - Core (game types): `focus` (`IFocusable`), `invincible` (`IDamageable`, `true` if absent), `damaged`
     (`AbstractHealthEntity`, `false` if absent), the health functions (`IHealthEntity`), `lightOrNull`,
     `onGravityDeviceSwitch`, the six fixture getters, and `shooting` (Megaman-only, with the `024` `TODO`). Proposed
     file: `C/entities/EntityQueries.kt` (the light and gravity device next to their components in `C/players/`).
2. **Targeting gives the entity:** `IMegaGameEntity.targetEntity` (the targeted player's entity) replaces
   `targetPlayer` at the call sites that ask about the entity (`targetPlayer.body` → `targetEntity.body`, and so on).
   The 6 uses that need the view use `players.targetFor(this)` directly. `targetPlayer` goes.
3. **The other callers** (`player.X`, `it.X` in `players` loops) read `player.entity.X`.
4. **The view:** `MegaPlayer.entity` narrows to `MegaGameEntity` (see "Open questions"); `MegaPlayerAccess.kt` and
   the `MegaPlayer.shooting` wrapper go. `megaman` stays (items `024`/`060`).
5. **Tests:** the entity query functions (needed parts fail, optional parts empty, on a plain entity and on a test
   player entity); `MegaPlayersTest` keeps only the view's own tests (registration, `asPlayer`, input, `targetFor`,
   the damage signal).

New APIs and their first callers:

| API | First callers |
|---|---|
| Engine `body`, `hasBody`, `facing`, `direction`, `isBehaviorActive`, `isAnyBehaviorActive` on `IGameEntity` | the `targetEntity` and `player.entity` call sites; `MegaPlayers.targetFor` (`hasBody`) |
| Core `focus`, `invincible`, `damaged`, health, `lightOrNull`, `onGravityDeviceSwitch`, fixtures, `shooting` on `IGameEntity` | the same call sites (screens, hazards, blocks, `Darkness*`, `GravitySwitcharoo`, `AbstractEnemy`) |
| `IMegaGameEntity.targetEntity` | the 147 `targetPlayer` files |

## Out of scope

- `dead` and `spawned` (item `062`); this branch reads `dead` as the `MegaGameEntity` member.
- Facing and direction as components (item `048`): the functions read the interfaces; only their bodies change later.
- Targeting rules (`targetFor` still gives the first player with a body; item `047`).
- `megaman` and its downcast readers (items `024`, `060`).

## Intended changes

None. Every read is the same field or call as today.

## Affected areas and risks

- About 200 files, mostly `targetPlayer.` → `targetEntity.` and `player.X` → `player.entity.X`, plus imports. A script
  makes the mechanical edits; the compiler checks every receiver type.
- **Shadowing:** an entity-level extension must not replace a read of another object's member. Members always win in
  Kotlin, so a read that compiled against a member before keeps it; the risk is only a file where the old receiver
  was a `MegaPlayer` and the new one is an entity whose class has its own member with another meaning. The one-for-one
  check compares each changed read.
- **Performance:** the same lookups as today (`MegaPlayerAccess.kt` already read the entity per call).

## Overlap with other branches

No branch is active in a worktree. Item `062` will change `dead`; this branch only moves where `dead` is read from.

## Open questions

1. **The name `targetEntity`** for the targeted player's entity (`target` is taken in 36 files). Other choices:
   `targetPlayerEntity` (longer, says it is a player), or keep `targetPlayer` but make it return the entity (shorter
   diff, but the name says "player" for an entity).
2. **Narrow `MegaPlayer.entity` to `MegaGameEntity`?** Every player entity is a `MegaGameEntity` (the base of all game
   entities), and the narrow type lets callers read `dead` and other core members without a cast. The cost: the view
   requires that one type at registration (an `IllegalArgumentException` otherwise), which is a part every core
   entity has.
3. **Engine or core** for `body`, `hasBody`, `facing`, `direction` and the behavior queries. Proposal: the engine, since
   they use only engine types.
4. **One branch or two:** all callers in one branch (one large mechanical diff), or `targetPlayer` callers first and
   the `player`/`it` callers in a second branch. Proposal: one branch, in batches of commits by package.
