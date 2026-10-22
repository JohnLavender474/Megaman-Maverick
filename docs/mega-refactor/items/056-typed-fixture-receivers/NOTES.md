# Notes: 056 Typed fixture receivers

## Research: generic receiver registration (2026-10-22)

A read-only research agent did this research on `master` at `6fc353f43`.

- `X` = `core/src/main/kotlin/com/megaman/maverick/game/world/body/IFixtureExtensions.kt`.
- `L` = `core/src/main/kotlin/com/megaman/maverick/game/world/contacts/MegaContactListener.kt`.

**Verdict:** yes, a generic receiver registration is a good idea here, but keep it small: typed keys over the existing fixture `Properties` (design A below).

### What exists today

There are about 17 receiver kinds, written by hand as 47 functions, with about 183 registration sites.

| Kind | Stored lambda | Key | Registrations | Listener call sites |
|---|---|---|---|---|
| velocityAlteration | `(IFixture, Float, ProcessState) -> VelocityAlteration` | `VELOCITY_ALTERATION`, no `has` (a missing value throws), X:35-42 | 10 / 9 files | L:422, 531, 1117, 1517 |
| filter | `(IFixture) -> Boolean` | `FILTER`, X:44-51 | 41 / 27 | `MegaContactFilter`:131,139,140; L:306, 908, 1390 |
| runnable | `() -> Unit` | `RUNNABLE`, X:53-58 | 6 / 2 | L:425-426, 535, 1121, 1521 |
| consumer | `(ProcessState, IFixture) -> Unit` | `CONSUMER`, `hasConsumer` not used, X:60-67 | 38 / 25 | L:80, 839, 1334 |
| forceAlteration | `(VelocityAlteration) -> Unit` | one string key per state, X:69-84 | 2 / 1 | L:532, 1118, 1518 |
| hitByWater | `(IWater) -> Unit` | `HIT_WATER`, `hasHitByWaterByReceiver` (typo), X:86-96 | 3 | L:476 |
| hitByExplosion | `(IBodyEntity) -> Unit`, no state | `HIT_BY_EXPLOSION`, X:98-108 | 4 | L:192, 206, 1131, 1144 (runs in begin and continue, but cannot tell them apart) |
| hitByBody | `(IBodyEntity, ProcessState) -> Unit` | `HIT_BY_BODY`, X:110-120 | 8 / 6 | 7 sites |
| hitBySide | `(IFixture, ProcessState) -> Unit` | `HIT_BY_SIDE`, X:122-132 | 2 / 1 | 6 sites |
| hitByBlock | `(Block, Float) -> Unit` | one string key per state, X:134-143 | 27 / 15 | 10 sites |
| hitByPlayer | `(Megaman) -> Unit` | `HIT_BY_PLAYER`, X:145-153 | 4 / 3 | L:615 |
| hitByProjectile | `(IProjectileEntity) -> Unit` | `HIT_BY_PROJECTILE`, X:155-163 | 17 / 12 | L:655 |
| hitByFeet | `(IFixture, Float) -> Unit` | one string key per state, X:165-173 | 9 / 7 | 9 sites |
| hitByDamageable | `(IDamageable, ProcessState) -> Unit` | `HIT_BY_DAMAGEABLE`, X:175-183 | 2 / 1 | L:95, 852 |
| hitByLaser | `(IFixture, ProcessState) -> Unit` | `HIT_BY_LASER`, X:185-193 | 0 (dead) | L:806, 1248 |
| stickToBlock | `(ProcessState, IFixture) -> Boolean`, default `true` | `STICK_TO_BLOCK`, X:195-210 | 8 / 5 | L:319, 921, 1300 |
| hitByShield | `(ProcessState, IFixture) -> Unit` | `HIT_BY_SHIELD`, X:212-221 | 2 / 1 | L:240 (a dead rule) |
| noProjectileCollision exception | `(IProjectileEntity, IFixture) -> Boolean` | string template key, `FixtureLabel.kt:44-53` | n/a | L:651 |

**Inconsistencies:**

- Three ways to handle `ProcessState`:
  - one key per state;
  - the state as a lambda argument;
  - no state at all (even for `hitByExplosion`, which runs in two phases).
- Mixed return types: `IFixture`, `Unit`, or the result of `invoke`.
- Two lookup styles: has + get (2 lookups), or `?.invoke` on a nullable value (1 lookup).
- The getters are named `getX`, but they run the receiver.
- `setHitByShield` and `setShouldStickToBlock` do not return `IFixture`.

**Failure modes:**

- A typo in a key, or a hand-built key string, is not caught.
- A wrong lambda type fails only at run time, with a `ClassCastException`, because the storage is `ObjectMap<Any, Any?>`.
- The kinds cannot be found from the type. You must read `X`.
- Nothing shows which rule calls which receiver.

**Hot-path cost:**

- The per-state kinds build a new key `String` twice per call (`has` and `get`), on each `FEET`-`BLOCK`, `HEAD`-`BLOCK` and `BLOCK`-`BODY` contact, in each sub-step.
- `(Block, Float) -> Unit` and `(IFixture, Float) -> Unit` box `delta` on each call.

**Bodies:** `IBodyExtensions.kt` has the same smell, but smaller:

- `BLOCK_FILTERS` is an untyped array of lambdas (78-88).
- `FEET_BLOCKS` and `CONTACT_WATER` are lazily created sets read with unchecked casts (90-120).
- `BODY_OVER_GROUND` is a `() -> Boolean` cast.

About 20 more unchecked function casts read `spawnProps`. The same typed-key primitive could cover them later. They are not in scope.

**Storage:**

- `Fixture.properties` is a `Properties`, which wraps `ObjectMap<Any, Any?>`.
- Its keys are already `Any`, so an object key works with no engine change. An object key hashes by identity.

### Alternatives

**A. Typed keys over `Properties` (recommended)**

```kotlin
class ReceiverKey<R : Any>(val name: String) {
    override fun toString() = name
}

class PhasedReceiverKey<R : Any>(name: String) {
    private val keys = Array(ProcessState.entries.size) { ReceiverKey<R>("${name}_${ProcessState.entries[it].name}") }
    operator fun get(state: ProcessState) = keys[state.ordinal]
}

fun <R : Any> IFixture.setReceiver(key: ReceiverKey<R>, receiver: R): IFixture {
    putProperty(key, receiver)
    return this
}

@Suppress("UNCHECKED_CAST")
fun <R : Any> IFixture.receiver(key: ReceiverKey<R>): R? = getProperty(key) as R?

fun <R : Any> IFixture.setReceiver(key: PhasedReceiverKey<R>, state: ProcessState, receiver: R) =
    setReceiver(key[state], receiver)

fun <R : Any> IFixture.receiver(key: PhasedReceiverKey<R>, state: ProcessState) = receiver(key[state])

object Receivers {
    fun interface HitByBlock { fun onHit(block: Block, delta: Float) }
    val HIT_BY_BLOCK = PhasedReceiverKey<HitByBlock>("hit_by_block")

    fun interface FeetBlockContact { fun onContact(state: ProcessState, block: Block) }
    val FEET_BLOCK_CONTACT = ReceiverKey<FeetBlockContact>("feet_block_contact")
}

fun IFixture.setHitByBlockReceiver(state: ProcessState, receiver: Receivers.HitByBlock) =
    setReceiver(Receivers.HIT_BY_BLOCK, state, receiver)

feetFixture.receiver(Receivers.HIT_BY_BLOCK, state)?.onHit(block, delta)
```

- **Type safety:** the one unchecked cast is in one generic function, and the key type binds it.
- **Performance:** one identity-hash lookup instead of two string lookups. No string building. No `Float` boxing, because a `fun interface` keeps a primitive `Float` parameter.
- **Granularity:** stays per fixture. This matters, because many entities register different reactions on fixtures of the same type:
  - `BikerKibbo.kt:196/204/212`;
  - `SpikeCopter.kt:205/213/220`;
  - `BouncingPebble.kt:142-167`;
  - `ReactorMonkeyBall.kt:145-167`;
  - `CartV2.kt:148-167`;
  - `Jetto.kt:275/290`.
- **Discoverability:** IDE completion on `Receivers.` lists every kind. "Find usages" on a key shows the rules that call it.
- **A new kind:** two lines.
- **Migration:** the old setters become adapters, and Kotlin turns a trailing lambda into the `fun interface`. So about 176 of 183 registration sites compile unchanged, including labelled `return@setHitByBlockReceiver`. The 7 sites that pass a function-typed `val` need a small edit:
  - `megaman/components/BodyComponent.kt:156,165,178`;
  - `CartinJoe.kt:201,209`;
  - `FixtureTypeOverlapSpawn.kt:73`;
  - the `setShouldStickToBlock(Boolean)` overload.

**A2. An array slot store on the engine `Fixture`** (indexed by a key ID)

- Faster still: no hashing.
- But it changes the engine `Fixture`, its `copy()` and its `reset()`, and it needs an ID registry.
- Only if a profile shows a need.

**B. Per-rule receiver interfaces** (each rule owns its contract)

- This does not match the data. Several rules share each kind:
  - `hitByBlock`: 4 rules;
  - `hitByFeet`: 3 rules;
  - `hitByBody`: 3 rules.
- An entity would register the same reaction 3-4 times.
- Keep it as a convention only: a rule may declare a key next to itself when only that rule uses it (`FEET_BLOCK_CONTACT`).

**C. One generic `onContact(state, other, delta)` per fixture** (Godot/Unity style)

- Loses the payload types (`Block`, `IWater`, `IProjectileEntity`, `IDamageable`).
- Loses the call point inside a rule's sequence: `FeetBlockContact` must run before `FEET_ON_GROUND` and `block.hitByFeet`, and `hitByBlock` after them.
- Repeats the phase asymmetries in each entity.
- Rejected. Only `CONSUMER` already has this shape, and it is right for that wildcard.

**D. Capability interfaces on the entity**, checked with `is`

- These are per entity, but per-fixture reactions are common (see the list under A). With D, entities would need `if (fixture === leftFixture)`.
- Use D only for yes/no questions with no payload, for example `IGateTrigger`. `029` can fold those into `IPlayer`.

### Order, migration, risks

**Order:** before the `014` rule files, or as their first commits. Doing it after `014` means writing about 70 rule call sites twice.

**Steps:**

1. Add the new API and the adapters in one commit.
2. Point the rules at `receiver(...)`.
3. Migrate the registration sites one at a time, or never.

**Keep the behavior:**

- Keep each call point and the call order.
- Keep the per-state split.
- Keep the "missing means default" rules: `stickToBlock` defaults to `true`, and `forceAlteration` falls back to `VelocityAlterator`.
- Keep `velocityAlteration` non-null.

**Risks:**

- The key changes from a string to an object. Any code that reads the string key breaks. A grep found none, because only `X` uses these fixture keys. `ConstKeys.HIT_BY_BLOCK` in `spawnProps` (`GlacierMan`:609, `SmallIceCube`:140) is a different bag.
- A `fun interface` lambda has a different runtime class. This matters only for identity comparisons of receivers, and no code does that.

### Side findings

- `BouncingPebble.kt:167`: the right `SIDE` fixture calls `bounce(Direction.RIGHT)`, the same as the left fixture at `:160`. `ReactorMonkeyBall.kt:167` uses `LEFT` there. This is probably a copy-and-paste bug. Not filed yet.
- `hitByExplosion` has no `ProcessState`, but runs in begin and in every continue sub-step.
- `AbstractItem.kt:168` calls `setShouldStickToBlock(...)` again at run time, which allocates a lambda on each call.
- `hasHitByWaterByReceiver` is misspelled (X:91), and `hasConsumer` is not used.
