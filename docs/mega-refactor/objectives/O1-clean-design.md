# `O1`: Clean, composable, easy to extend

- Status: active

This objective supports all the others. An item goes under `O1` alone only if no other objective needs it.

## Problem

- `P1`: Logic is in deep inheritance layers and very large classes (`WilyFinalBoss` is 3393 lines). A
  developer must read many layers to change one behavior, and a second user of the logic needs a copy.
- `P2`: Global state and fragile order hide what depends on what. A small change can break a distant feature, and the
  developer cannot see this from the code.
- `P3`: Untyped keys and hand-written repeated patterns move errors from the compiler to play time.

## Goal

Clean, composable logic in the engine and the game that is easy to extend and to change. From the owner: "Ultimately,
all other objectives are affected to one degree or another by how well our systems are designed."

## Done means

- A new entity is a set of components and small functions, with no new base class.
- A reader can find the owners of a piece of state from its type, not by a search for string keys.
- A change to one system does not need a change to the order of events in another system.

## Rules

### `O1-R1`: No global session state

- Rule: entities and systems get session objects through `MegaServices` or narrower services, not through a global or
  a static.
- Problem: `P2`
- Check: search `core/src/main` for `MegamanMaverickGame` in entity packages, and for mutable top-level or companion
  state that holds session objects.
- Applies: `always-on`
- Active: after `096`

### `O1-R2`: Components, not new inheritance

- Rule: new shared entity logic is a component, a system or a function, not a new layer in an entity class line.
- Problem: `P1`
- Check: a branch that adds an `abstract class` under `entities/` must give the reason in `SCOPE.md`.
- Applies: `always-on`
- Active: `now`

### `O1-R3`: No new fragile order

- Rule: new logic does not depend on event order across frames or on listener registration order.
- Problem: `P2`
- Check: review the new listeners in the branch diff. An ordered sequence is one routine or one state machine.
- Applies: `always-on`
- Active: `now`

### `O1-R4`: Typed state

- Rule: new state uses a typed field, not a new property bag or `ConstKeys` key, where the value does not come from
  map data.
- Problem: `P3`
- Check: review the new `ConstKeys` values and the `putProperty` calls in the branch diff.
- Applies: `always-on`
- Active: `now`

### `O1-R5`: Code only with a caller

- Rule: a new API has a production caller in the branch that adds it.
- Problem: `P3`
- Check: `SCOPE.md` and `PLAN.md` list each new API with its first caller.
- Applies: `always-on`
- Active: `now`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. `084h` (`d796e00ba`) changes no count in this table. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O1-R1` | `partly` | Entity packages have 0 live `MegamanMaverickGame` lines, and `game.megaman` has 0 live lines. Statics still hold session objects: `MegaEntityFactory.kt:27` (306 lines in 185 files), `EntityFactories.kt:14-22` (73 lines in 52 files), `MegaGameEntities.kt:12-15` (62 lines in 45 files), `ToxicWater.kt:76,87`. Per-level state in companions: `Starkner.kt:74`, `AstroAssAssaulter.kt:61`, `SpiderWeb.kt:75`. Companion texture regions: 428 lines in 363 files. | `096`, `028`, `021` |
| `O1-R2` | `holds` for new code | 53 `abstract` and `open` classes at `5b4d76c5a` and 53 now. No new entity layer. Old layers remain: 9 abstract classes under `entities/`, 124 or 125 direct `AbstractEnemy`, 69 `AbstractProjectile` and 18 `AbstractBoss` subclasses (recount 2026-11-02). | `048`, `042` |
| `O1-R3` | `partly` | 0 new listeners since `5b4d76c5a`. 1 new `EventType` (`GAME_COMPLETE`). Old chains remain: 3 `submitEvent` lines in `MegaLevelScreen.onEvent` (9 before `118g`), and a submit during `events.run()` waits for the next frame (`EventsManager.kt:30`). The added `submitEvent` lines were not reviewed one by one, because the `016` rename hides them. Since `118a` (2026-11-02), `118b` (2026-11-03), `118c` (2026-11-04), `118d`, `118e` and `118f` (2026-11-05), all 25 live boss fights (`BigAssMaverickRobot`, and `TimberWoman`, `MoonMan`, `ReactorMan`, `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` at the end of their stages and in the middle of a Wily stage, `ElecDevil` and `GutsTank` at the end of their Wily stages, the mini bosses `MoonHead`, `MechaDragon` and `ReactorMonkey`, and `WilyFinalBoss`) are each one routine with no event for its order, including the resume after a mid-level fight; `SigmaRat` and `PenguinMiniBoss` also have a plan, but no live map spawns them. Since `118g` (2026-11-05), the old boss event chain, its 5 guards and the events `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN` are gone, and a boss room with no plan throws `MissingFightPlanException`. Since `118h` (2026-11-05), the fight runs the boss bar fill after its steps, so the screen's `when` chain has no bar branch. Since `118i` (2026-11-05), a fight step, not the `END_BOSS_SPAWN` event, starts the boss AI: the `BossComponent` phase replaces `ready` and `betweenReadyAndEndBossSpawnEvent`. Since `118j` (2026-11-06), 1 keyed `UNLOCK_GATE` event replaces `INTERMEDIATE_BOSS_DEAD` and `GAUNTLET_CLEARED`, and the fight plan reads the gate key from the map, so no boss field carries it. Since `038a` (2026-11-06), 1 `RespawnRoutine` runs the death and the respawn as an ordered list of steps, in place of a chain of 5 events between the screen and 2 handlers; `PLAYER_SPAWN_INIT` is gone, and the public player events stay. Since `038b` (2026-11-06), the routine sends `LEVEL_RESET` at the reset, and `PLAYER_SPAWN` is gone, so a level reset no longer rides on a player `onSpawn`. | `038`, `013`, `053`, `057` |
| `O1-R4` | `partly` | `ConstKeys` went from 409 to 395 keys. `putProperty` lines went from 339 to 286. New string-named keys from code: `BodyKeys.kt:7,9`, `Receivers.kt:55-61`, `HeartTank.kt:160`. | `098`, `057`, `024` |
| `O1-R5` | `partly` | 15 of 17 branch folders changed since 2026-10-30 have an API and caller table. 2 APIs have only test callers: `Players.kt:22` `unregister`, `LocksComponent.kt:37` `isLockedBy`. The scan misses properties and common names, and it does not check each table row. | `047` (`unregister`), `039` (`isLockedBy`) |

## Redo risk

- `016`: not a rewrite, but its goal is not complete. `MegaEntityFactory` and `EntityFactories` are still statics, and 306 and 73 call lines depend on that access.
- `029a`, `061a`: `Players.unregister` and `isLockedBy` need a production caller or removal (2 declarations). `047` is a possible caller for `unregister` (inference).
- `014`: `BodyKeys` and the string-named `Receivers` keys change only if the owner reads `O1-R4` as covering typed keys in a bag.
- No redo found in `051`, `062`, `063`, `064`, `084f`, `084g` or `084h`.

## Items

| Item | Status | Note |
|------|--------|------|
| `001` | active | [Maverick Engine](../items/001-maverick-engine/ITEM.md) (main) |
| `012` | planned | [Named, composable freezes with a reason table](../items/012-named-freezes/ITEM.md) (main) |
| `013` | planned | [Level flow state machine; pause as an overlay](../items/013-level-flow-fsm/ITEM.md) (main) |
| `014` | done | [Table-driven contact and collision rules](../items/014-contact-rules-table/ITEM.md) (main) |
| `015` | planned | [One entity state machine (enter/update/exit, state-owned timers)](../items/015-entity-fsm/ITEM.md) (main) |
| `016` | done | [Narrow services instead of `game` everywhere](../items/016-narrow-services/ITEM.md) (main) |
| `017` | planned | [Shared entity logic as systems and world-step hooks](../items/017-world-step-hooks/ITEM.md) (main) |
| `019` | done | [One spawn phase per tick with destroy notifications](../items/019-spawn-phase/ITEM.md) (main) |
| `023` | planned | [Damage profiles keyed by damage tags](../items/023-damage-profiles/ITEM.md) (main) |
| `027` | planned | [Non-null APIs instead of `!!`](../items/027-non-null-apis/ITEM.md) (main) |
| `028` | planned | [Game-owned texture and animation cache](../items/028-texture-cache/ITEM.md) (main) |
| `037` | planned | [Explicit respawn state and named `on_respawn` actions](../items/037-respawn-state/ITEM.md) (main) |
| `038` | done | [One ordered respawn routine instead of an event chain](../items/038-respawn-routine/ITEM.md) (main) |
| `039` | planned | [Retire unused assets and review dead code](../items/039-retire-dead-assets-code/ITEM.md) (main) |
| `042` | planned | [Split god classes and extract reusable motion and attack components](../items/042-reusable-components/ITEM.md) (main) |
| `043` | planned | [Lighter clone without a history rewrite](../items/043-lighter-clone/ITEM.md) (main) |
| `045` | planned | [Clear ownership rules and debug checks for `GameObjectPools`](../items/045-pool-ownership/ITEM.md) (main) |
| `048` | planned | [Replace entity inheritance with components](../items/048-components-not-inheritance/ITEM.md) (main) |
| `049` | idea | [Reduce repetitive object-oriented patterns](../items/049-reduce-repetitive-patterns/ITEM.md) (main) |
| `052` | idea | [Make values configurable](../items/052-configurable-values/ITEM.md) (main) |
| `053` | idea | [Clean up event-driven logic](../items/053-event-cleanup/ITEM.md) (main) |
| `056` | dropped | [Typed fixture receivers](../items/056-typed-fixture-receivers/ITEM.md) (main) |
| `057` | idea | [Typed event channels and payloads](../items/057-typed-event-channels/ITEM.md) (main) |
| `058` | idea | [Camera rotation as its own mechanism, separate from gravity](../items/058-camera-rotation-mechanism/ITEM.md) (main) |
| `062` | done | [One engine-owned entity existence state](../items/062-entity-existence-state/ITEM.md) (main) |
| `063` | done | [Entity contracts are entity types](../items/063-entity-contracts-as-entity-types/ITEM.md) (main) |
| `064` | done | [Entity instance identity: `id` and `group`](../items/064-entity-instance-identity/ITEM.md) (main) |
| `065` | planned | [Every `init()` calls `super.init()`](../items/065-init-calls-super/ITEM.md) (main) |
| `066` | idea | [`GameSystem.reset()` clears pending removes](../items/066-game-system-reset-pending-removes/ITEM.md) (main) |
| `071` | idea | [`getEntity()` without a cast to `MegaGameEntity`](../items/071-typed-get-entity/ITEM.md) (main) |
| `072` | planned | [Remove `IMegaGameEntity`](../items/072-remove-imegagameentity/ITEM.md) (main) |
| `083` | idea | [`PathfinderResult.targetReached` means what it says](../items/083-pathfinder-target-reached/ITEM.md) (main) |
| `085` | dropped | [Remove the dead `ConstKeys.FOCUS` override in `Megaman`](../items/085-dead-focus-override/ITEM.md) (main) |
| `086` | idea | [Player movement modifiers as a component](../items/086-player-movement-modifiers/ITEM.md) (main) |
| `087` | idea | [Reset order for components](../items/087-component-reset-order/ITEM.md) (main) |
| `092` | idea | [Remove old entity versions and the version suffixes](../items/092-old-entity-versions/ITEM.md) (main) |
| `093` | idea | [Use or remove `MegaRegionProcessor`](../items/093-mega-region-processor/ITEM.md) (main) |
| `103` | idea | [Remove the dead `ScreenController` path or give it a real use](../items/103-dead-screen-controller/ITEM.md) (main) |
| `104` | idea | [Remove the dead `LevelSelectScreen`](../items/104-dead-level-select-screen/ITEM.md) (main) |
| `018` | idea | [Event-driven system membership; component IDs when needed](../items/018-ecs-membership/ITEM.md) |
| `024` | planned | [One `WeaponDef` per weapon; loadout per character](../items/024-weapon-defs/ITEM.md) |
| `025` | idea | [Level manifest: traits, variants and rewards instead of level-identity checks](../items/025-level-manifest/ITEM.md) |
| `029` | done | [`IPlayer` contract, players service and `CharacterDef`](../items/029-player-contract/ITEM.md) |
| `032` | planned | [Platform-neutral `engine` module](../items/032-platform-neutral-engine/ITEM.md) |
| `046` | planned | [Backgrounds as pure functions of the camera, with visible-cell drawing](../items/046-backgrounds/ITEM.md) |
| `051` | done | [X-style camera bounds, auto-scroll and other camera behaviors](../items/051-camera-behaviors/ITEM.md) |
| `059` | idea | [Max health read through from the profile](../items/059-max-health-read-through/ITEM.md) |
| `061` | done | [Thin player view; locks and signals as components and functions](../items/061-thin-player/ITEM.md) |
| `084` | done | [Split `Megaman` into player components](../items/084-split-megaman/ITEM.md) |
| `096` | idea | [Session-owned entity factories and lists, in place of statics](../items/096-session-owned-entity-services/ITEM.md) (main) |
| `097` | idea | [Stable order for event keys in `EventsManager`](../items/097-stable-event-key-order/ITEM.md) |
| `098` | idea | [Typed fixture and body state in place of property bags](../items/098-typed-fixture-state/ITEM.md) (main) |
| `112` | planned | [Targeting as a policy that each entity owns](../items/112-targeting-policies/ITEM.md) (main) |
| `114` | idea | [Gravity from components](../items/114-gravity-components/ITEM.md) (main) |
| `115` | idea | [Partial sink and stuck behavior in sand](../items/115-partial-sink-stuck-in-sand/ITEM.md) |
| `116` | idea | [Body senses as typed state, then a sense history](../items/116-body-senses/ITEM.md) (main) |
| `118` | done | [Boss fights as routines, rules and leases](../items/118-boss-fight-routines/ITEM.md) (main) |
| `119` | planned | [`PLAYER_DESTROYED` and `LEVEL_FAILED`](../items/119-player-left-world-event/ITEM.md) |
| `126` | idea | [Movement and damage blocks as lock reasons](../items/126-movement-damage-lock-reasons/ITEM.md) (main) |
| `130` | planned | [`LevelCamera` and `SharedView`: the camera stack out of `MegaLevelScreen`](../items/130-level-camera/ITEM.md) (main) |
| `137` | idea | [One falling-piece entity in place of `DebrisPiece`, `BlockPiece` and `WoodCratePiece`](../items/137-falling-piece-entity/ITEM.md) (main) |
| `138` | idea | [Merge `Wreck` into `MockSpriteEntity`](../items/138-wreck-into-mock-sprite/ITEM.md) (main) |
| `139` | idea | [A shared simple enemy shot, starting with `JunkShot`](../items/139-shared-enemy-shot/ITEM.md) (main) |
| `140` | idea | [`TickFrames` as the engine `Animation`, with durations in seconds](../items/140-tick-frames-seconds/ITEM.md) (main) |
| `146` | idea | [A marker `IPlayer` interface, so a player has a type](../items/146-player-marker-interface/ITEM.md) (main) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

## Open questions
