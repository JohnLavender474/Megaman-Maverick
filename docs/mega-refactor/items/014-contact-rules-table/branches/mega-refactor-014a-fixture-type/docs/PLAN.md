# Plan: mega-refactor-014a-fixture-type

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`.

## Steps

### 1. Branch start

- Changes:
  - On `master`, in a docs-only commit: set the `014a` row in `ITEMS.md` to `active` with the start
    date, set item `014` to `active` with the start date, and list the branch under "Branches" in
    `ITEM.md`. Set the branch `process-log.md` state to `active`.
  - `git worktree add -b mega-refactor-014a-fixture-type .worktrees/mega-refactor-014a-fixture-type master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 427 engine, 77 core) in the process log.
- Done: no

### 3. Add `IFixtureType` and narrow the engine types

- Changes (engine `world/body/`):
  - New `IFixtureType.kt`: `interface IFixtureType { val index: Int }`.
  - `IFixture : ITypable<IFixtureType>`.
  - `Fixture(type: IFixtureType)`; the private `fixtureType` field follows.
  - `Body.fixtures: OrderedMap<IFixtureType, OrderedSet<IFixture>>`.
  - `BodyBuilder.fixtures`: the same type.
  - `IBody.getFixtures(out, vararg params: IFixtureType)` and the `Body` override.
  - Keep the map an `OrderedMap`, and keep the code of `addFixture`, `hasFixture`, `removeFixture`,
    `getFixtures` and `forEachFixture` the same apart from the types.
- Check: `./gradlew engine:compileKotlin` passes. The engine tests do not compile yet; step 4 fixes
  them.
- Done: no

### 4. Engine tests: a test enum instead of strings

- Changes:
  - New `engine/src/test/kotlin/com/maverick/game/engine/world/body/TestFixtureType.kt`:
    `internal enum class TestFixtureType : IFixtureType` (with `override val index get() = ordinal`)
    with one value for each string the tests
    use today (for example `TYPE`, `OTHER_TYPE`, `TYPE1`, `TYPE2`, `F`, `F1`, `F2`, `KEEP`, `SKIP`,
    `FIXTURE1`, ...). Collect the full list with a grep of `Fixture(..., "...")` and
    `getType() == "..."` in `engine/src/test`.
  - Replace each string fixture type with its enum value: `FixtureTest`, `ContactTest`, `BodyTest`,
    `WorldSystemTest` (also its `ObjectMap<Any, ObjectSet<Any>>` filter map becomes
    `ObjectMap<IFixtureType, ObjectSet<IFixtureType>>`), `SimpleGridWorldContainerTest`,
    `LooseQuadtreeWorldContainerTest`. This is a repeated pattern in about 44 places, so a script is
    fine. Leave strings that are not fixture types alone (property keys, event keys).
  - Fix any test `DummyFixture` that overrides `getType()`.
- Check: `./gradlew engine:test` passes, with the same test count as the baseline.
- Done: no

### 5. Core: `FixtureType` implements `IFixtureType`

- Changes: `enum class FixtureType : IFixtureType` in `core/.../world/body/FixtureType.kt`, with
  `override val index get() = ordinal`. Fix any core compile error that the narrower types show. The scan expects none.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the same core test count as the baseline.
- Done: no

### 6. Contract checks

- Changes: none, unless a check fails.
- Check:
  - All automated checks in `CONTRACT.md`, including the grep in `engine/.../world/body/`.
  - Review the diff of `Body.kt` and `BodyBuilder.kt`: only types change.
  - The owner runs the smoke run in `CONTRACT.md` (`TEST_1`, and one minute of a Robot Master
    level).
  - Write the results in the process log.
- Done: no

### 7. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval. If `014b` merged first, compare with `git diff --stat <branch>...master` and
  merge `master` into this branch first if they overlap (`MegaContactFilter.kt` imports, engine
  tests).
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no
