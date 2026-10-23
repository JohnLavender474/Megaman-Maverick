# Notes: mega-refactor-029a-player-seam

## Decisions

## Overlap with other branches

## Research: typed player view for required components (2026-10-23)

The owner's question: if a component is required, can its accessors (for example `player.body`) appear on the player view automatically, with no `!!`? A research agent compared seven options at `35f7aa0f0`. It compiled the sketches in a throwaway Gradle project (Kotlin `2.3.20`, JDK 17) outside the repo.

### Facts from the repo

- Kotlin is `2.3.20`. Context parameters are still experimental there (`-Xcontext-parameters`).
- `Megaman` is one object. It is destroyed (`Megaman.kt:740`) and spawned again (`MegaLevelScreen.kt:593`). Its components stay the same objects between spawns (`reset()` runs on destroy). A view bound to the entity stays valid across a respawn. Today `megaman.body` can be read while Megaman is dead; a bound view keeps that behavior.
- `BodyComponent.body` is a `var`. An accessor must read through the cached component (`get() = bodyComponent.body`), not cache the `Body`.
- `GameEntity.addComponent` replaces by class, and `removeComponent` exists. A cached component field becomes stale if a player's required component is replaced. Core does not do this on `Megaman`.
- `engine` has `kotlin-reflect` as `api`, so `X::class` does not allocate.

### Options

| Option | Result | Verdict |
|---|---|---|
| 1. Core extension properties with `!!` after a registration check | Compiles. No compile-time safety; the required set and the extensions can drift apart; NPE with no message when the entity is absent | Baseline only |
| 2. Game view type: `Players<V : PlayerView>` with a view factory; the view resolves required components once into non-null fields | Compiles and runs. Real null safety; a missing component fails at registration with a clear message; no allocation per call; engine code takes covariant `IPlayers<PlayerView>` | **Recommended** |
| 3. Phantom capability types (`PlayerView<out C : Cap>`, extensions on `PlayerView<HasBody>`) | Partly compiles. A `requires(Body).requires(Facing)` builder that returns an intersection type fails (*"Type parameter cannot have any other bounds if it's bounded by another type parameter"*); `PlayerView<HasBody & HasFacing>` fails (*"Intersection types are supported only for definitely non-nullable types"*); completeness is not checked; each new component touches 5 places | Reject |
| 4. Context parameters (`context(_: Proof<BodyComponent>)`) | Compiles only with the experimental flag. About 900 call sites need a proof in scope; does not remove the `!!` or solve absence | Reject for `029` |
| 5. Delegated properties and `ComponentKey<T>` | Compiles. Only a style of writing option 2; a lazy or resettable delegate could fix stale components. `ComponentKey<T>` (like `PropertyKey<T>`) gives a short typed form for optional components: `player[Keys.SHIELD]?.let { ... }` | Add `ComponentKey<T>` for optional components |
| 6. Code generation (KSP) | Needs a new `processor` module, the KSP plugin in the root `buildscript`, a KSP version that supports Kotlin `2.3.20`, CI changes. Saves about one line per component | Only if many view types appear later |
| 7. Prior art | Unity `RequireComponent` + cached `GetComponent` in `Awake`, and Godot typed `@onready` references, are option 2. Fleks (Kotlin) `entity[Position]` with `ComponentType<T>` is option 5. Bevy and EnTT need variadic generics, which Kotlin lacks | Kotlin frameworks check membership at a boundary and give non-null access inside it |

Allocation test: 5,000,000 calls after a warm-up allocate nothing per call for options 1, 2, 3 and 5.

### Recommended shape (option 2 plus `ComponentKey<T>`)

```kotlin
// engine
interface PlayerView {
    val slot: PlayerSlot
    val entity: IGameEntity
    val present: Boolean get() = entity.spawned
    fun <C : IGameComponent> get(type: KClass<C>): C? = entity.getComponent(type)
}

fun <C : IGameComponent> IGameEntity.require(type: KClass<C>): C =
    getComponent(type) ?: throw IllegalStateException("Player entity $this has no ${type.simpleName}")

interface IPlayers<out V : PlayerView> {
    fun get(slot: PlayerSlot): V?
    fun nearestTo(point: Vector2): V?
    fun any(predicate: (V) -> Boolean): Boolean
}

open class Players<V : PlayerView>(
    private val createView: (PlayerSlot, IGameEntity) -> V,
    private val position: (V) -> Vector2?,
    val maxPlayers: Int = 4
) : IPlayers<V> { /* register, unregister, get, forEachPresent, nearestTo, any, all; slot order */ }

// core
class MegaPlayer(override val slot: PlayerSlot, override val entity: IGameEntity) : PlayerView {
    private val bodyComponent = entity.require(BodyComponent::class)
    val body: Body get() = bodyComponent.body
}

class MegaPlayers : Players<MegaPlayer>(::MegaPlayer, { it.body.getCenter() })

fun MegaPlayers.lowestHealth(): MegaPlayer? = TODO()
```

- **Bound and present.** A view exists while an entity is bound to the slot, so its required fields are non-null. `get(slot)` is null only when no entity is bound. `present` (default `entity.spawned`; the game can add `ready`) filters the helpers. During a respawn wait the view stays bound: `player.body` gives the same stale-but-valid object as today, and target searches skip the player.
- **No separate `required` set.** The view constructor is the registration check.
- **Rules:** the view has fields only for required components; helpers go in extension functions. One view type for every character (no view subclass per character). Required components are fixed while the entity is bound; a debug assertion or a view rebuild on spawn catches a replaced component.
- **Trade-offs:** one generic parameter at the service boundary; the view class must stay thin; code handles `V?` from `get`, or uses a `require(slot)` where slot 0 is known to exist.
