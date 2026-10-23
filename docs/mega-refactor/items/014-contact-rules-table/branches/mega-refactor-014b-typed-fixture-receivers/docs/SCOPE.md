# Scope: mega-refactor-014b-typed-fixture-receivers

What this branch changes and why. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

## Goal

Replace the hand-written receiver extension trios (`setX`/`hasX`/`getX` on string keys with
unchecked casts) with one generic, typed registration: typed keys, one `setReceiver` and one
`receiver` accessor. After this branch, a new receiver kind is one key declaration, and a wrong
lambda type does not compile. Gameplay does not change.

## Item

`014` (table-driven contact and collision rules). The owner moved the former item `056` into `014`
as this branch (2026-10-22). Its proposed scope, from `items/056-typed-fixture-receivers/ITEM.md`:

> - Add `ReceiverKey<R>`, `PhasedReceiverKey<R>` (one key per `ProcessState`) and generic
>   `setReceiver`/`receiver` accessors on `IFixture`, stored in the existing fixture `Properties`
>   with object keys. In `core` first (`world/body/FixtureReceivers.kt`). A genre-neutral
>   `TypedKey<T>` can move to the engine later.
> - Add a `Receivers` object with one `fun interface` and one key for each existing kind (about 17).
>   Keep each kind's current shape: phased stays phased, stateless stays stateless.
> - Turn each old trio into a one-line adapter, so that about 176 of the 183 registration sites
>   compile unchanged. Edit the 7 sites that pass a function-typed `val`.
> - Move `setExceptionForNoProjectileCollision` (`FixtureLabel.kt:44-53`) to the same API.
> - Delete what nothing uses: `hasConsumer`, and the dead `hitByLaser` kind (see
>   `docs/issues/2026-10-22-unused-contact-hooks.md`).
> - Later, optional: inline the adapters at the registration sites, then delete them.
> - Out of scope: giving every kind the same phase shape; `hitByPlayer(Megaman)`, which `029`
>   changes; typed keys for body properties and `spawnProps`.

The owner's input (2026-10-22): the extension style is "a code smell", and "a more generic
receiver registration setup would be better".

This draft changes three points of that proposal. See "Changes from the item proposal".

## Current state

Scan of `master` at `f8cab20dc`. `X` = `core/.../world/body/IFixtureExtensions.kt`.

**Storage.** `Fixture.properties` is a `Properties` (engine), which wraps `ObjectMap<Any, Any?>`.
`IPropertizable` (`engine/.../common/interfaces/IPropertizable.kt`) gives `putProperty(key: Any,
...)`, `getProperty(key: Any)`, `hasProperty`. Keys are already `Any`, so an object key needs no
engine change. `Fixture.copy()` makes a shallow copy of the properties, so receivers are copied
with the fixture.

**The receiver kinds** (registration counts from a grep of `core/src/main/kotlin`, outside `X`):

| Kind | Stored lambda | Key | Registrations | Read API |
|---|---|---|---|---|
| velocityAlteration | `(IFixture, Float, ProcessState) -> VelocityAlteration` | `VELOCITY_ALTERATION` | 10 | `getVelocityAlteration` (no `has`; a missing value throws) |
| filter | `(IFixture) -> Boolean` | `FILTER` | 41 | `hasFilter`, `getFilter` |
| runnable | `() -> Unit` | `RUNNABLE` | 6 | nullable `getRunnable` |
| consumer | `(ProcessState, IFixture) -> Unit` | `CONSUMER` | 38 | nullable `getConsumer`; `hasConsumer` unused |
| forceAlteration | `(VelocityAlteration) -> Unit` | one string per state | 2 | `applyForceAlteration` (falls back to `VelocityAlterator.alterate`) |
| hitByWater | `(IWater) -> Unit` | `HIT_WATER` | 3 | `hasHitByWaterByReceiver` (misspelled), `getHitByWater` |
| hitByExplosion | `(IBodyEntity) -> Unit` | `HIT_BY_EXPLOSION` | 4 | has/get |
| hitByBody | `(IBodyEntity, ProcessState) -> Unit` | `HIT_BY_BODY` | 8 | has/get |
| hitBySide | `(IFixture, ProcessState) -> Unit` | `HIT_BY_SIDE` | 2 | has/get |
| hitByBlock | `(Block, Float) -> Unit` | one string per state | 27 | has/get by state |
| hitByPlayer | `(Megaman) -> Unit` | `HIT_BY_PLAYER` | 4 | has/get |
| hitByProjectile | `(IProjectileEntity) -> Unit` | `HIT_BY_PROJECTILE` | 17 | has/get |
| hitByFeet | `(IFixture, Float) -> Unit` | one string per state | 9 | has/get by state |
| hitByDamageable | `(IDamageable, ProcessState) -> Unit` | `HIT_BY_DAMAGEABLE` | 2 | has/get |
| hitByLaser | `(IFixture, ProcessState) -> Unit` | `HIT_BY_LASER` | **0** | has/get (always false) |
| stickToBlock | `(ProcessState, IFixture) -> Boolean` | `STICK_TO_BLOCK` | 7 | `shouldStickToBlock` (default `true`) |
| hitByShield | `(ProcessState, IFixture) -> Unit` | `HIT_BY_SHIELD` | 2 | has/get (the rule never runs; see the issue) |
| noProjectileCollision exception | `(IProjectileEntity, IFixture) -> Boolean` | string template (`FixtureLabel.kt:44-53`) | 3 | `isExceptionForNoProjectileCollision` (default `false`) |

That is 182 registration sites, plus 3 for the exception.

**Readers.** All reads are in two files: `MegaContactListener.kt` (93 calls) and
`MegaContactFilter.kt` (3 calls: `hasFilter`, `getFilter`). No other code reads these keys. The
`ConstKeys.HIT_BY_BLOCK` uses in `GlacierMan.kt:609`, `MegamanWeaponsHandler.kt:817` and
`Coldier.kt:198` are `spawnProps` values, a different bag.

**Problems** (from the research in `items/056-typed-fixture-receivers/NOTES.md`):

- A typo in a key, or a wrong lambda type, fails only at run time (`ClassCastException`).
- Three styles for `ProcessState`, mixed return types, two lookup styles, getters named `getX` that
  run the receiver.
- The per-state kinds build a new key `String` twice per call (`has` and `get`), on each
  `FEET`-`BLOCK`, `HEAD`-`BLOCK` and `BLOCK`-`BODY` contact in each sub-step.
- The kinds cannot be found from the types. You must read `X`.

**A compile test** (Kotlin 2.3.20, the project's version, in a scratch folder) showed:

- A bare trailing lambda does **not** convert to a `fun interface` when the parameter type is a
  generic type parameter, also with an explicit type argument. So
  `setReceiver(KEY, state) { block, delta -> }` does not compile if the key's type is a
  `fun interface`. It compiles only with an explicit constructor: `HitByBlock { block, delta -> }`.
- With a **Kotlin function type** as the key's type (`PropertyKey<(Block, Float) -> Unit>`), the
  bare trailing lambda compiles, and `return@setReceiver` works.

## Changes from the item proposal

1. **Function types, not `fun interface`s, for the receiver types.** Because of the compile test,
   function types keep the registration syntax a bare trailing lambda. The cost: a receiver with a
   `Float` parameter still boxes `delta`, which is what the code does today, so this is no
   regression. The string building goes away in any case. A hot kind can become a `fun interface`
   later, with the explicit constructor at its registration sites.
2. **No adapters: migrate every registration site and delete the trios.** The adapters would keep
   one extension function for each kind, which is the smell the owner wants gone. The migration is a
   mechanical, repeated edit (a script), and the compiler checks every site.
3. **The generic key goes in the engine**, in line with the decision to put the `014c` table in the
   engine. It is genre-neutral: a typed key for any `IPropertizable`. The receiver kinds stay in
   `core`.

## In scope

**Engine (`common/objects/`, `common/interfaces/`):**

- `PropertyKey<T : Any>(val name: String)`: an object key, identity equality, `toString() = name`.
- `PhasedPropertyKey<T : Any>(name: String)`: one `PropertyKey<T>` for each `ProcessState`, built
  once, read with `get(state)`.
- Typed accessors on `IPropertizable`: `putProperty(key: PropertyKey<T>, value: T)`,
  `getProperty(key: PropertyKey<T>): T?`, `hasProperty(key)`, `removeProperty(key)`. The one
  unchecked cast is inside the getter.
- Engine tests for the key types and the accessors.

**Core:**

- `world/body/Receivers.kt`: one object with one key for each kind in the table above, except
  `hitByLaser`. The phased kinds (`hitByBlock`, `hitByFeet`, `forceAlteration`) use
  `PhasedPropertyKey`. The other kinds keep their current shape.
- `IFixture.setReceiver(key, receiver): IFixture`, `setReceiver(key, state, receiver)`,
  `receiver(key)`, `receiver(key, state)`: four short generic functions in the same file.
- Migrate the 185 registration sites to `setReceiver(Receivers.X, ...)`, including the labelled
  returns (`return@setHitByBlockReceiver` becomes `return@setReceiver`).
- Migrate the 96 reads in `MegaContactListener` and `MegaContactFilter`. A `hasX` + `getX` pair
  becomes one `receiver(...)?.invoke(...)`. Keep the special semantics:
  - velocityAlteration: still throws if missing (`!!`);
  - stickToBlock: still defaults to `true`;
  - forceAlteration: still falls back to `VelocityAlterator.alterate`;
  - noProjectileCollision exception: still defaults to `false`.
- Delete the trios in `X`, `hasConsumer`, the exception helpers in `FixtureLabel.kt`, and the dead
  `hitByLaser` kind: its two reads in the listener (`L:806`, `L:1248`) are always false, so removing
  them changes nothing. Close the `hitByLaser` part of `2026-10-22-unused-contact-hooks.md`.
- Keep in `X` the helpers that are not receivers: `getBody`, `setEntity`, `getEntity`,
  `depleteHealth`.

## Out of scope

- The `014c` rule table. This branch only changes how receivers are stored and read; the chain in
  `MegaContactListener` keeps its structure.
- Giving every kind the same phase shape (for example a state for `hitByExplosion`).
- `hitByPlayer(Megaman)`: `029` re-types it.
- The new receivers that `014c` adds (for example the feet-block contact receiver). `014c` adds
  them as keys.
- Typed keys for body properties (`IBodyExtensions.kt`) and `spawnProps`. The engine
  `PropertyKey` makes them possible later.
- `fun interface` receiver types (see "Changes from the item proposal", point 1).

## Affected areas and risks

- **Files:** 2 new engine files plus `IPropertizable.kt`, engine tests, core `IFixtureExtensions.kt`,
  `FixtureLabel.kt`, a new `Receivers.kt`, `MegaContactListener.kt`, `MegaContactFilter.kt`, and
  about 110 entity files with registration sites.
- **No behavior change by design.** Each receiver is stored and called at the same point, with the
  same arguments, in the same order. Only the key object and the lookup change.
- **Risk: a read that changes meaning.** A `has` + `get` pair becomes one nullable call. This is the
  same unless a receiver is stored as `null`, and no code does that. The four special semantics
  above are the places to review by hand.
- **Risk: a key read by its string elsewhere.** The scan found no reader outside `X`,
  `FixtureLabel.kt`, the listener and the filter.
- **Risk: the migration script misses an unusual site.** The trios are deleted, so a missed site does
  not compile.
- **Risk: identity.** A receiver's runtime class does not change with function types, and no code
  compares receivers.
- **Size:** about 110 files touched by a mechanical change. Review the script and a sample of sites,
  and review the listener, the filter and the special semantics in full.

## Overlap with other branches

- No `mega-refactor-` branch is active now.
- `014a` (fixture type) may run in parallel. It changes `Fixture`, `Body`, `IBody`, `BodyBuilder`,
  `WorldSystem`, `FixtureType.kt` and engine tests. This branch changes `IPropertizable` and
  `Properties` users. Shared files: maybe an import line in `MegaContactFilter.kt`, and both add
  engine tests (different files). The second branch to merge checks the merge.
- `014c` needs this branch.

## Open questions

None open.

- (2026-10-22) Owner's decisions: function types for the receiver types; migrate every site and
  delete the trios in this branch; the generic key (`PropertyKey`, `PhasedPropertyKey`) goes in the
  engine. All three match the draft.
