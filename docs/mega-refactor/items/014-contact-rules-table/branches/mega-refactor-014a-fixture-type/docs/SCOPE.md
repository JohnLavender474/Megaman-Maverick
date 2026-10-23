# Scope: mega-refactor-014a-fixture-type

What this branch changes and why. This is a draft written ahead of time on `master`. The branch
does not exist yet, and the user has not approved this scope.

- Status: draft
- Approved: no

## Goal

Type fixtures in the engine as `IFixtureType` (`ordinal`, `name`) instead of `Any`. This gives the
`014c` rule table a dense index with no casts. Gameplay does not change: only compile-time types
change.

`FixtureTypeSet` (from the same decision) moves to `014c`. Only the rule table uses the ordinal, so
only the table needs the set and its `owns` check. See "Open questions" for the record of this
change.

## Item

`014` (table-driven contact and collision rules), the decision of 2026-10-22 in its `ITEM.md`:

> **Fixture type:** the engine types fixtures as an `IFixtureType` interface (`ordinal`, `name`),
> not `Any` and not a primitive ID. An engine `FixtureTypeSet` owns the set of types: it validates
> dense, ordered ordinals and unique names (`of`, `validated`), creates types for games without
> enums (`named`), and checks that a type belongs to the set (`owns`). Fixture types are single
> instances, with no value equality. This is the scope of `014a`.

Research: `items/014-contact-rules-table/NOTES.md`, "Research: narrow the engine fixture type" and
its follow-up "Fixture types that are not enums".

## Current state

Scan of `master` at `f8cab20dc`.

**Engine**

- `ITypable<T>` (`common/interfaces/ITypable.kt:3`) has 3 users. Only `IFixture : ITypable<Any>`
  (`world/body/IFixture.kt:7`) uses `Any`. `IMegaGameEntity` (`EntityType`) and `Fade`
  (`FadeType`) are already narrow.
- `Fixture` takes `type: Any` (`world/body/Fixture.kt:17`) and stores it (`:38`). `getType()`
  (`:67`), `toString()` (`:76`) and `copy()` (`:84`) use it.
- `Body.fixtures` is a public `OrderedMap<Any, OrderedSet<IFixture>>` (`world/body/Body.kt:30`).
  `addFixture`, `hasFixture`, `removeFixture` and `getFixtures` (`:62-92`) key it by `getType()`.
  `IBody.getFixtures(out, vararg params: Any)` (`IBody.kt:18`). `BodyBuilder.fixtures` uses the same
  map type (`BodyBuilder.kt:16`). No code assigns `body.fixtures = ...`.
- `WorldSystem.collectContacts` (`WorldSystem.kt:230-252`) visits every fixture of every body in
  each sub-step. The engine never reads the type there; only the client's `IContactFilter` does.
- `WorldSystem`'s private `DummyFixture` (`:52-66`) throws from `getType()`. Its return type is
  `Nothing`, so it compiles with any type.
- The 6 `Contact` helpers (`contacts/Contact.kt:14-51`) take `Any` or `ObjectSet<Any>`. `014c`
  deletes them.

**Core**

- `FixtureType` (`world/body/FixtureType.kt`) is a plain enum with 29 values. There are 1751
  `FixtureType.X` references in 321 files, and about 876 `Fixture(...)` calls.
- Every `Fixture(...)` call passes a `FixtureType`. Four calls pass a variable
  (`Starkner.kt:369`, `Cokeyro.kt` trunk and head, `Block.kt`), and each variable holds a
  `FixtureType`.
- Type reads: about 32 `getType() ==`/`!=`, 17 `when (getType())`, 17 `getType().equalsAny(...)`,
  23 `body.fixtures[...]` lookups, 2 `getFixtures(out, FixtureType...)` calls. All keep compiling,
  because `FixtureType` will be an `IFixtureType`.
- 4 `as FixtureType` casts: `MegaContactFilter.kt:96,143,144` and `FixtureTypeOverlapSpawn.kt:71`.
- Data paths: `FixtureType.valueOf(...)` from Tiled strings (`FixtureTypeOverlapSpawn.kt:56`) and
  `ConstKeys.FIXTURES` pairs (`Block.kt:173`). Both keep working.
- `MegaContactListener`'s private `DummyFixture` (`:1603`) throws from `getType()`, like the engine
  one.
- No core test builds a `Fixture`.

**Engine tests** use strings as fixture types in about 44 places: `FixtureTest`, `ContactTest`,
`BodyTest`, `WorldSystemTest` (an `ObjectMap<Any, ObjectSet<Any>>` filter of `"Type1"`/`"Type2"`),
`SimpleGridWorldContainerTest` and `LooseQuadtreeWorldContainerTest` (`getType() == "keep"`).

## In scope

**Engine (`world/body/`):**

- New `IFixtureType` interface: `val ordinal: Int`, `val name: String`. An enum implements it with no
  code.
- `IFixture : ITypable<IFixtureType>`; `Fixture(type: IFixtureType)`.
- `Body.fixtures: OrderedMap<IFixtureType, OrderedSet<IFixture>>`, the same in `BodyBuilder`, and
  `IBody.getFixtures(out, vararg params: IFixtureType)`.

**Core:**

- `enum class FixtureType : IFixtureType`. No other code in the enum.

**Tests:**

- Engine: an `internal enum class TestFixtureType : IFixtureType` replaces the string types and the
  `ObjectMap<Any, ...>` filter in `WorldSystemTest`.

## Out of scope

- The `014c` rule table, and the removal of the `Contact` helpers.
- `FixtureTypeSet`: it moves to `014c`, with the table that uses it.
- Any fixture-type check in `WorldSystem`: the world never reads the type.
- Removing the 4 `as FixtureType` casts. `014c` replaces the filter casts; they are harmless here.
- An `IFixtureType` overload of `equalsAny`.
- Changing `Body.fixtures` to an array indexed by ordinal. The insertion order of the map sets the
  order in which `collectContacts` visits fixtures, which sets the contact order, so this would
  change gameplay.
- Bitmask contact filters (Box2D-style category and mask).
- Other `Any`-typed engine keys: event keys, `Properties` keys, component map keys.
- `MegaContactListener`'s `DummyFixture`: it compiles unchanged.

## Affected areas and risks

- **Files:** engine `world/body/` (`IFixture`, `Fixture`, `Body`, `IBody`, `BodyBuilder`, 1 new
  file), about 6 engine test files, and core `FixtureType.kt`.
- **No behavior change by design.** Enum `equals` and `hashCode` stay the same, and the generic
  types are erased at run time. The map keeps its insertion order.
- **Engine tests:** a mechanical rewrite of the string types. A wrong rewrite shows up as a failing
  test, not as a silent change.
- **The 1751 core references and the 876 constructor calls do not change.**

## Overlap with other branches

- No `mega-refactor-` branch is active now.
- `014b` (typed fixture receivers) may run in parallel. It changes `IFixtureExtensions.kt`,
  `FixtureLabel.kt`, the receiver registration sites and the receiver calls in
  `MegaContactListener`/`MegaContactFilter`. This branch changes `Fixture`, `Body` and the type
  declaration. They share no files, except perhaps an import line in `MegaContactFilter.kt`.
- `014c` needs this branch.

## Open questions

1. Name of the index property: `ordinal` (an enum gets it free) or `index` (needs one line in the
   enum, but does not suggest that only enums fit)? The draft uses `ordinal`.

Resolved in the draft:

- (2026-10-22) The first draft passed an optional `FixtureTypeSet` to `WorldSystem` and checked
  `owns` there. The owner asked what that gives. Answer: nothing that matters. `WorldSystem` never
  reads the type. `Body.fixtures`, the filter maps and `==` compare by identity, so a stray type
  with a used ordinal does no harm there. The ordinal matters only in the `014c` table, so the set
  and its check belong to the table. `FixtureTypeSet` moves to `014c`, where it gets its first user.
