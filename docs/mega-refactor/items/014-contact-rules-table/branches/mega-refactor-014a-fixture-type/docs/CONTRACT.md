# Contract: mega-refactor-014a-fixture-type

The gameplay that this branch must not change, and how to check it. This is a draft written ahead
of time on `master`. The user has not approved it yet.

- Status: draft
- Approved: no

This branch changes only compile-time types: `IFixture.getType()` returns `IFixtureType` instead of
`Any`, and `FixtureType` implements `IFixtureType`. At run time, the same enum objects are stored,
compared and hashed as before, and `Body.fixtures` keeps its insertion order. So no gameplay can
change. The checks below confirm that the build and the tests agree, and a short smoke run confirms
that the game still starts and plays.

## Automated checks

- [ ] `./gradlew engine:test` passes. The test count is the same as on `master` (427), plus any new
      tests.
- [ ] `./gradlew core:test` passes (77 on `master`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] No fixture type is typed `Any` in `engine/src/main/kotlin/com/maverick/game/engine/world/body/`:
      `grep -rn "Any" engine/src/main/kotlin/com/maverick/game/engine/world/body/` shows no fixture
      type. `preProcess`, `postProcess` and `onReset` keys stay `Any`, which is out of scope.
- [ ] No engine test passes a `String` as a fixture type (the compiler enforces this).
- [ ] The diff does not change `Body.fixtures` from an `OrderedMap`, and does not change the
      order of `addFixture`, `forEachFixture` or `getFixtures`.

## Manual playthrough

A smoke run. A short run is enough, because nothing changes at run time.

| Level | What to check |
|-------|---------------|
| `TEST_1` (`./run-game-test.sh`) | The level loads. Megaman lands on blocks, walks, jumps, slides into walls, climbs a ladder, takes damage from an enemy and kills one. No exception in the log. |
| Any one Robot Master level, first room (for example `--level DESERT_MAN`) | The level loads and plays for about a minute with no exception. |

## Behaviors to check

None beyond the smoke run. No save data, menu, event or timing code changes.

## Intended changes

None.
