# Plan: mega-refactor-011a-game-random

Decisions: `SCOPE.md`, "Decisions" (`D1` inject, `D2` one stream, `D3` clock seed + `--randSeed`, `D4` source-scan test).

## New API and first callers

| API | First production caller |
|---|---|
| `GameRandom` (engine, `common/random/`) | `MegaServices.random` |
| `SeededRandom(source: java.util.Random)` | `MegamanMaverickGame.postCreate()` |
| `GameRandom.nextInt(min, maxInclusive)`, `nextFloat(min, max)`, `nextBoolean()` | replace `getRandom(Int, Int)`, `getRandom(Float, Float)`, `getRandomBool()` |
| `GameRandom.pick(array)`, `pick(vararg)` | replace gdx `Array.random()`, Kotlin `entries.random()`, `getRandom(vararg)`, `superRandom` |
| `GameRandom.shuffle(array)` | replace gdx `Array.shuffle()`, `IntRange.shuffled()` |
| `MegaServices.random` | every entity site |
| `LaunchOptions.randSeed` / `--randSeed` | `DesktopLauncher` |
| `GameEngine` spawn order number | `MegamanWeaponsHandler` Moon Scythe sort |

`SeededRandom` takes a `java.util.Random`, so a test keeps its own `java.util.Random` and calls `setSeed` on it (no
seed setter only for tests). `ScriptedRandom` goes in the test sources only if a test needs it. Add other functions
only when a site needs them.

## Steps

### 1. Engine source

- Changes: `GameRandom`, `SeededRandom` in `engine/.../common/random/`. Engine tests for ranges, `pick`, `shuffle`,
  same seed same values.
- Check: `./gradlew engine:test`.
- Done: yes

### 2. Engine helpers take a `GameRandom`

- Changes: `getRandomPositionInBounds` (both), `getRandomElements`, `WeightedRandomSelector`, `Pendulum`. Delete
  `superRandom` (both), `doIfRandomMatches`, `randomVector2`, `RandomSelectorBehavior`, `MutableArray.shuffle()` and
  `random()`, `OrderedSet.random()` if its callers move to `pick`. Keep `UtilMethods.random` and `getRandom*` until
  step 5.
- Check: `./gradlew engine:test` (fix `ObjectSetExtensionsTest`, `GameLineTest`, `RotatingLineTest`).
- Done: yes

### 3. Services, seed, launch option

- Changes: `MegaServices.random`; `postCreate()` builds `SeededRandom` from `--randSeed` or the clock and logs the seed;
  `--randSeed` in `DesktopAppArgs`, `LaunchOptions` and `resolveLaunchOptions` (a bad value is an error, exit code 2);
  `testServices(random = ...)`. Launch option test.
- Check: `./gradlew core:test`.
- Done: yes

### 4. Move the core sites

- Changes: every site in `temp/mega-refactor-011a-game-random/scan.md` (main checkout) to `services.random`.
  `getRandom(a, b)` keeps its inclusive bounds; `MathUtils.random(int, int)` is inclusive; `MathUtils.random(float)`
  is `[0, x)`; Kotlin `nextInt(until)` is exclusive. `D1000CeilingDebris.roll` and `OrbAim` take a `GameRandom`.
  The pattern is the same in many files, so a script may do the plain `getRandom` swaps; each other site is a hand
  edit. Boss tests pass a `SeededRandom(java.util.Random(seed))` and call `setSeed` on their own source.
- Check: `./gradlew core:test core:smokeTest`.
- Done: yes

### 5. Delete the old source

- Changes: delete `UtilMethods.random`, `getRandom*`, `getRandomBool`. `timeSpawned` becomes the engine spawn order
  number. `TODO` at `AsyncPathfindingSystem.kt:66` for `010`.
- Check: `./gradlew engine:test core:test`.
- Done: yes

### 6. Guard test

- Changes: a `core` test that scans `core/src/main` and `engine/src/main` for direct random calls; the RNG package is
  allowed. Prove it fails on a planted call (then remove the call).
- Check: `./gradlew core:test`.
- Done: yes

### 7. Package README and full checks

- Changes: `engine/.../common/random/README.md` (purpose, the seed path, the rule for new code, the link to `011`).
- Check: all automated checks in `CONTRACT.md`, then the review before the play test, then the play test and the
  Diffity review.
- Done: yes
