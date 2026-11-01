# Scope: mega-refactor-084f-focus-progress

- Status: approved
- Approved: 2026-11-01

## Goal

Remove 2 of the 10 interfaces on `Megaman`, `IFocusable` and `IGameStateListener`, and the dead `ConstKeys.FOCUS` reads. Components hold the 2 roles, so a second playable entity gets them with its own hooks. Mega plays the same.

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Interfaces:** remove each interface on `Megaman` that has no reason left: `084e` (15 to 10) and `084f` (10 to 8).

> **Further branches (planned):** `084f-focus-progress`, `084g-movement-state` (after `031e` merges), `084h-charge`.

> Still in the bag: 15 lines in `entities/megaman/`. 4 go to `084g`, 2 (`ConstKeys.FOCUS`) go to `084f`, and 9 are weapon lines that go to `024`.

From the 084 `NOTES.md`, "Proposed branches":

> `084f-focus-progress`: 10 to 8 interfaces. `FocusComponent` and `PlayerProgressComponent`. The dead `ConstKeys.FOCUS` reads go, which closes `085`.

Owner decision that applies (2026-10-31): `085` folds into `084f`.

## Current state

Scan on 2026-11-01, `master` at `6eb3884bd`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`. Line numbers are in `M/Megaman.kt` unless the line names a file.

**`IFocusable`.** `Megaman` is the only production implementer (`:75`). It has 2 members: `getFocusBounds()` (`:550`) and `getFocusPosition()` (`:552-564`). The position is the inverted direction point of the body, moved half a body height along the direction, so it uses `MEGAMAN_BODY_HEIGHT`.

3 lines reach the role:

- `C/entities/EntityQueries.kt:15-16`: `IGameEntity.focus` casts the entity to `IFocusable`, and it throws if the cast fails.
- `C/screens/levels/MegaLevelScreen.kt:336`: `cameraFollow.setFocus(gameCamera, player.focus)`. The camera parts keep the `IFocusable` reference.
- `M/handlers/MegamanWeaponsHandler.kt:383`: `megaman.getFocusPosition()`, each frame while a `PRECIOUS_GUARD` cluster is live.

The test entities that implement `IFocusable` pass themselves to the camera parts, so they do not change. `EntityQueriesTest` reads `entity.focus` on an entity that implements it.

**`ConstKeys.FOCUS`.** 3 lines in `core/src`: the constant (`C/Constants.kt:88`), `removeProperty` (`:389`) and the read (`:553`). Nothing puts the property, so the read is always false.

**`IGameStateListener`.** `Megaman` is the only implementer (`:74`). `C/MegamanMaverickGame.kt:444` adds it to `GameState`, before `players.register`. It overrides 4 of the 12 members:

- `onAddLevelDefeated` (`:566`): sets `levelState.weaponsAttained`.
- `onAddHeartTank`, `onRemoveHeartTank` (`:570,572`): call the private `setMaxHealthFromHeartTanks()` (`:577`), which holds a `TODO` for `059`.
- `onAddCurrency` (`:582`): plays the pickup sound if the currency is below the max.

`GameState.reset()` and `fromString()` call the same listeners. `MegamanHeartTankTest` adds the `Megaman` as the listener, and it does not call `init()`.

### Frame-order constraints

No line moves in a frame. `GameState` calls one listener, as today, at the same points. The camera parts read the focus at the same points. The new listener stays registered for the session, as `Megaman` does, because a component is not removed on destroy.

## In scope

1. **`FocusComponent`:** new `C/entities/state/FocusComponent.kt`, an `IGameComponent` that implements `IFocusable`, with 2 hooks: `bounds` and `position`. It holds no entity reference.
2. **`defineFocusComponent()`:** new, in `M/components/`. The position rule moves there with no change, less the `ConstKeys.FOCUS` line.
3. **Readers:** `IGameEntity.focus` reads the component, and it still throws if the component is missing. `MegamanWeaponsHandler.kt:383` reads `megaman.focus.getFocusPosition()`.
4. **`PlayerProgressComponent`:** new `C/entities/state/PlayerProgressComponent.kt`, an `IGameComponent` that implements `IGameStateListener`, with 3 hooks: a level is defeated, the heart tanks changed, and currency is added.
5. **`definePlayerProgressComponent()`:** new, in `M/components/`. The 3 rule bodies and `setMaxHealthFromHeartTanks()` with its `TODO` move there.
6. **Registration:** `MegamanMaverickGame.kt:444` adds the component to `GameState`, at the same line.
7. **Class line:** remove `IFocusable` and `IGameStateListener`. 8 interfaces remain.
8. **Dead code:** delete the 2 `ConstKeys.FOCUS` lines in `Megaman.kt` and the constant.
9. **Tests:** `EntityQueriesTest` and `MegamanHeartTankTest` change. New `FocusComponentTest` and `PlayerProgressComponentTest`. The position rule gets a test for each of the 4 directions.

`085` stays `dropped`; its work is done here.

No temporary suite and no new test room. `RefactorTest029` has the rooms `heart`, `pickups` and `gravity_switch`.

### New API and its first caller

| API | First caller |
|---|---|
| `FocusComponent` | `IGameEntity.focus`, which `MegaLevelScreen.kt:336` and `MegamanWeaponsHandler.kt:383` read |
| `defineFocusComponent()` | `Megaman.init()` |
| `PlayerProgressComponent` | `MegamanMaverickGame.kt:444` |
| `definePlayerProgressComponent()` | `Megaman.init()` |

## Out of scope

- The other 8 interfaces (`048` and the owners in the 084 `NOTES.md`).
- Registration in `MegaPlayers.register`. `MegaPlayers` is made at `MegamanMaverickGame.kt:234`, before `gameState` at `:325`, so it has no `GameState` to use, and 17 lines construct it. A second player adds 1 line at the same place (`060`). `031e` also changes `MegaPlayers.kt`.
- The move of `weaponsAttained` out of the player. It is a session rule, not a player rule, but the only signal for a newly defeated level is the listener. With 2 players the hook runs twice with the same value. With capacity 1 this does not happen.
- The currency sound with 2 players (`047`). With capacity 1 this does not happen.
- The read-through max health (`059`).
- A shared position rule for other characters (`060`). `MEGAMAN_BODY_HEIGHT` stays in the Mega hook.

## Affected areas and risks

Production files: 2 new, 2 or 3 new or changed in `M/components/`, and `Megaman.kt`, `EntityQueries.kt`, `MegamanWeaponsHandler.kt`, `MegamanMaverickGame.kt`, `Constants.kt`.

Risks:

- **Focus before `init()`.** `IGameEntity.focus` throws if the component is missing. `Megaman.init()` runs in `postCreate()`, before the level screen reads the focus. A test that makes a `Megaman` with no `init()` and reads the focus fails at the read.
- **Listener identity.** `gameState.addListener` needs the same component instance that the entity holds. `init()` runs once, so one instance exists.
- **`setMaxHealth` order.** The hook calls the same public functions, with no new order. `MegamanHeartTankTest` covers it.
- **Performance:** `MegamanWeaponsHandler.kt:383` adds 1 component lookup each frame while a cluster is live. The camera parts keep the reference, so they add none. A hook call is added to each focus read.

## Overlap with other branches

See `docs/NOTES.md`. `031e` changes `MegamanMaverickGame.kt` and `Constants.kt`, at other lines.

## Open questions

None.
