# Scope: mega-refactor-084e-class-line

- Status: approved
- Approved: 2026-10-31

## Goal

Remove 5 of the 15 interfaces on `Megaman`, and the dead player code, so that a second playable entity gets the gate, cart and sprite paths through `isPlayer` and component queries. Mega plays the same.

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Interfaces:** remove each interface on `Megaman` that has no reason left: `084e` (15 to 10) and `084f` (10 to 8).

Owner decisions that apply (2026-10-31, 084 `ITEM.md`): `isPlayer` replaces `IGateTrigger` and `ICartRider`. The commented `regionProcessor` block goes; its idea is in the `024` `NOTES.md`.

## Current state

Scan on 2026-10-31, `master` at `405cf0171`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`. Line numbers
are in `M/Megaman.kt` unless the line names a file.

**`IPointsEntity`.** `IHealthEntity` extends it (`C/entities/contracts/IHealthEntity.kt:7`), so the entry on the class line changes nothing.

**`IAnimatedEntity`.** 2 uses on `Megaman`: `forEachAnimator` at `:250`, and `getAnimator` at `M/components/SpritesComponent.kt:189`. The interface stays for other entities.

**`IBehaviorsEntity`.** `Megaman` is the only implementer, and it is in `engine/`. The engine has `isBehaviorActive` and `isAnyBehaviorActive` as extensions on `IGameEntity` (`E/entities/EntityQueries.kt`). 82 calls in 17 files in `M/` have no receiver, and they resolve to the member
today. 2 handlers call them on `megaman`. Only 2 other members are in use: `resetBehavior` (`:179`, `:287`) and `getBehavior` (`M/components/BodyComponent.kt:105`). `BodyComponent.kt:104-106` checks `isBehaviorActive`, then `behavior?.isActive()`; these are the same check (`E/behaviors/BehaviorsComponent.kt:70`).

**`IGateTrigger`, `ICartRider`.** Empty markers with 1 implementer. `GateRule.kt:15` and `FeetCartRule.kt:21,33` check them. `MegaContactFilter.kt:110-116` also drops gate and cart contacts unless one entity has `EntityType.MEGAMAN`. `isPlayer` (`E/players/PlayerComponent.kt:11`) reads a
component. `MegamanMaverickGame.kt:448` registers the player before the first spawn. `Cart` is in one live map, `Test1.tmx`, and no room there with a cart has a player spawn. `BodySense.TOUCHING_CART` and `ConstKeys.CART` have writers only.

**`UnderwaterFan`.** `ENTITIES_TO_BLOW` (`C/entities/hazards/UnderwaterFan.kt:84`) is a set of one class, checked at `:224`. It is an exact class match, so a second player would not get blown. Fans are in `GLACIER_MAN` (`room8`), `WILY_STAGE_2` (`path1_room1`) and `Test1`; none of these rooms has a player spawn.

**`input`.** `val Megaman.input` (`M/MegamanInput.kt`, 6 lines) reads the cached field `controllerComponent` (`:122`, `:311-312`), and it uses `!!`. The engine has `IGameEntity.input`, with a `RELEASED` default. 14 files import the Megaman version, and 45 lines in `M/` read it.

**Fixture accessors.** 7 `val Megaman.*Fixture` at `M/components/BodyComponent.kt:57-76`. 6 copy `C/entities/EntityQueries.kt`. `bodyFixture` has no copy.

**Sprite helpers.** `getSpritePriority`, `shouldFlipSpriteX` and `shouldFlipSpriteY` (`M/components/SpritesComponent.kt:50-52,95`) read no class-only member. 3 lines hold 4 casts only to reach them: `EndLevelEventHandler.kt:104,179`, `PlayerSpawnEventHandler.kt:98`.

**Dead code.**
- `spawningTimer` (`:272`, `:444`, `:605`) has no reader. `MegamanValues.SPAWNING_DUR` goes with it.
- `wallSlideGravity` (`:193`, `:208`, `:262`) has no reader. `MegamanValues.WALL_SLIDE_GRAVITY` goes with it. `TimberWoman` and `InfernoMan` have their own private constants of that name.
- `M/components/PointsComponent.kt` (21 lines): the member `definePointsComponent` at `:568` shadows it.
- The commented block at `:316-372`, and the comment inside the call at `:373`.

### Frame-order constraints

No line moves in a frame. `isPlayer` replaces 3 checks at the same places, and the dead `spawningTimer.update` at `:605` goes from the lambda.

## In scope

1. **Class line:** remove `IPointsEntity`, `IAnimatedEntity`, `IBehaviorsEntity`, `IGateTrigger` and `ICartRider`. `IBodyEntity` stays. Delete the files `IBehaviorsEntity.kt`, `IGateTrigger.kt` and `ICartRider.kt`.
2. **Behavior calls:** the 17 files import the engine extensions. `IGameEntity.resetBehavior(key)` goes in `E/entities/EntityQueries.kt`. `BodyComponent.kt:104-106` becomes `if (isBehaviorActive(behaviorType)) resetBehavior(behaviorType)`.
3. **Animators:** the 2 uses call `require(AnimationsComponent::class)`.
4. **`isPlayer`:** `GateRule`, `FeetCartRule`, `MegaContactFilter` and `UnderwaterFan` use it. `ENTITIES_TO_BLOW` goes.
5. **`input`:** delete `M/MegamanInput.kt` and the field `controllerComponent`. The 14 files import the engine `input`.
6. **Fixtures:** delete the 7 accessors. Changed during step 7: no `IGameEntity.bodyFixture` is added, because `Megaman.bodyFixture` had no readers (each `bodyFixture` in `M/` is a local). 13 files, most of them outside `M/`, import the `C/entities/EntityQueries.kt` versions in place of the deleted ones.
7. **Sprite helpers:** changed on 2026-10-31, approved by the owner: these functions apply only to Mega Man, so they do not go on `IGameEntity`. `getSpritePriority` becomes `getMegamanSpritePriority(out)`, with no receiver. `shouldFlipSpriteX` and `shouldFlipSpriteY` go, and the 3 callers read `facing == Facing.LEFT` and pass `false`, as other lines in the handlers do. 4 casts go, so 16 remain.
8. **Dead code:** all of the list above.
9. **Tests:** `GateRuleTest` and `EntityContractsTest` change. New tests: a player and a non-player for the gate rule and the cart rule, and for the 2 contact filter checks; `resetBehavior` on an active, an inactive and a missing behavior; the `UnderwaterFan` blow check, if it can run with no assets.
10. **Test room:** a new room `cart` in `RefactorTest084.tmx`, with a `CartV2` and a player spawn, built the same as the carts in `DesertMan_16x14_v4.tmx`. Changed on 2026-10-31 at the owner's request: the room first held the old `Cart`, which no stage uses. `Cart` has no play effect (see `docs/NOTES.md`), so play cannot show the `FeetCartRule` and `CART` filter paths; only tests cover them.

No temporary suite. The changes are removals and call swaps, with one rule replaced in each of 4 places; tests state each rule.

### New API and its first caller

| API | First caller |
|---|---|
| `IGameEntity.resetBehavior(key)` | `Megaman.kt:179,287`, `BodyComponent.kt:105` |

## Out of scope

- The other 10 interfaces (`084f`, `048`, and the owners in the 084 `NOTES.md`).
- `isAnyBehaviorActive(Iterable)` and the other unused members of `IBehaviorsEntity` go with the file; no engine extension replaces them.
- `MegaRegionProcessor` and the `regionProcessor` parameter of `MegamanAnimations`: they have no implementer after this branch, and today too. See `docs/TODO.md`.
- `BodySense.TOUCHING_CART` and `ConstKeys.CART`, which have no readers.
- `EntityType.MEGAMAN` on `Megaman.getType()`.
- The other `Megaman::class` keys (`023`, `030`).

## Affected areas and risks

Files: about 30 production files, because of the imports: the 14 `input` files and the 17 behavior files overlap. 5 production files are deleted.

Risks:

- **`isPlayer` against the old checks.** With one player the results are the same, but only for a registered player. A test that makes a `Megaman` with no registration gets `false`. `GateRuleTest` must register it.
- **`input` default.** The old `!!` threw on a player with no input source; the engine query gives `RELEASED`. `MegaPlayers` sets the source, so this path does not run in play, and a missing source would now be silent.
- **Performance:** each `input` read adds 1 component lookup, in place of a field read. 45 lines in `M/` read it, several in each frame for one player. `--diagnostics` can measure it. `MegaContactFilter` reads `isPlayer` only for a gate or cart fixture.
- **`movementScalar`** (`:249`) runs `forEachAnimator` in a setter. `require` throws as the old `!!` did, if the component is missing.
- **A class member hides an extension.** After the interface goes, a name in `M/` can resolve to another extension. The compiler finds missing imports, not a wrong overload; check that only the engine extensions are imported.

## Overlap with other branches

See `docs/NOTES.md`. `031e` changes `AirDashBehavior.kt` and `ControllerComponent.kt`, which this branch changes in the imports only. It also changes `MegaPlayers.kt` and `MegamanMaverickGame.kt`, which this branch does not change.

## Open questions

None.
