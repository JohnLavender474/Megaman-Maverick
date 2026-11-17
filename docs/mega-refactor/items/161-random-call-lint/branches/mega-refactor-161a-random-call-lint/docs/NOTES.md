# Notes: mega-refactor-161a-random-call-lint

## Decisions

The owner asked for the branch end to end with no stops (2026-11-16). Each decision is Claude's choice, pending owner
confirmation. `SCOPE.md` has the short form.

- `D1` Tool: detekt `2.0.0-alpha.3`, rule `ForbiddenMethodCall`, with type resolution (`detektMain`).
- `D2` `RandomSourceGuardTest` is deleted.
- `D3` A permanent fixture check, `core:verifyRandomRule`.
- `D4` Only main sources are checked.
- `D5` `SeededRandom(...)` is forbidden outside the random package too; the game's one call is suppressed by an
  annotation (from the review before the play test).

## Tool comparison (`D1`)

Facts from Maven Central and the plugin sources (2026-11-16):

| Option | Kotlin inside | Sees types | Fit |
|---|---|---|---|
| detekt `1.23.8` (`io.gitlab.arturbosch.detekt`) | `2.0.21` (K1 compiler) | yes, K1 | no: a K1 `2.0.21` compiler cannot read the `2.3` metadata of the stdlib and libraries the project compiles against |
| detekt `2.0.0-alpha.3` (`dev.detekt`) | `2.3.21` (Analysis API) | yes | yes: the nearest Kotlin to the project's `2.3.20`; Gradle 8.2+ |
| detekt `2.0.0-alpha.4` to `alpha.6` | `2.4.0` to `2.4.10` | yes | possible, but a newer compiler than the project; no gain for this rule |
| Konsist `1.0.0-beta.1` | PSI only | no | no: it reads the syntax tree, so `with(array) { shuffle() }` has no known receiver type |
| A Kotlin compiler plugin of our own | project compiler | yes | no: much more code to own than one config file |
| Text scan (`RandomSourceGuardTest`) | none | no | the gap that item `161` names |

detekt `2.0.0` is not released yet. Only `alpha` versions exist (`alpha.0` to `alpha.6`). The `alpha.3` Gradle plugin
reads `KOTLIN_IMPLEMENTATION_VERSION = "2.3.21"` (`BuildConfig.kt`), and needs Gradle 8.2 or later
(`GradleCompat.kt`). The rule `ForbiddenMethodCall` resolves each call with the Analysis API
(`resolveToCall`) and also matches each symbol that the called symbol overrides (`allOverriddenSymbols`). So:

- a call on a subclass matches the base entry: `SnapshotArray.shuffle()` matches `com.badlogic.gdx.utils.Array.shuffle`,
  and `RandomXS128.nextInt()` matches `java.util.random.RandomGenerator.nextInt`;
- a name with no parameter list matches all overloads, also the ones with a `Random` argument
  (`kotlin.collections.shuffle` matches `list.shuffle(rng)`);
- a Java static field matches by its name: `com.badlogic.gdx.math.MathUtils.random` matches the field access
  `MathUtils.random.nextInt()` and each `MathUtils.random(...)` method.

## Rule list

`config/detekt/detekt.yml`. `buildUponDefaultConfig = false`, and a rule that the file does not name is off
(`RuleDescriptor.kt`: `ruleConfig.isActiveOrDefault(false)`). So only `ForbiddenMethodCall` runs, and the existing
code needs no baseline.

The list covers: `MathUtils.random*`; `RandomXS128()`; `Vector.setToRandomDirection` (it draws from `MathUtils.random`);
the `random()` and `shuffle()` members of the LibGDX arrays (`Array`, `ArrayMap`, `IntArray`, `FloatArray`,
`LongArray`, `ShortArray`, `ByteArray`, `CharArray`, `BooleanArray`; found with `javap` on `gdx-1.14.2.jar`); the
Kotlin `random`, `randomOrNull`, `shuffle`, `shuffled` extensions (`kotlin.collections`, `kotlin.ranges`,
`kotlin.text`, `kotlin.sequences`); `kotlin.random.Random` (the factory, its members and its extensions);
`java.util.Random()`, `SecureRandom`, `SplittableRandom`, `ThreadLocalRandom.current`, `RandomGenerator` (its
members cover each JDK generator), `Math.random`, `StrictMath.random`, `Collections.shuffle`.

`SeededRandom.<init>` is also on the list (`D5`), so code outside the random package cannot make a second, unseeded
stream. The one real call, in `MegamanMaverickGame` (the session source), has `@Suppress("ForbiddenMethodCall")`.

Limits: detekt reads only `.kt` files, so a call in Java main code is not checked (2 Java files in main today, with no
random call; the old text scan also read only `.kt`). A call that does not resolve is skipped with no error
(`successfulCallOrNull` in `ForbiddenMethodCall.kt`), so a library that is missing from the classpath of
`detektMain` would hide its calls.

Excluded paths: `**/engine/common/random/**` (the seeded source) and the test source sets.

Not in the list: `java.util.UUID.randomUUID()`. It makes keys for `OrderedMap`s (`UpdatablesComponent`,
`AnimationsComponent`, `MegaUtilMethods`, `Water`). The maps keep insertion order, so the key value does not change an
order or a gameplay value.

## Build wiring

- `build.gradle`: the plugin on the buildscript classpath, `apply plugin: 'dev.detekt'` in `configure(subprojects)`,
  the config, `basePath` = the repo root. `check` depends on `detektMain`. The plain `detekt` task is off: it has no
  classpath, so a rule that needs types cannot run in it.
- `detektMain` uses the classes of its compilation, so it runs after `compileKotlin` of its module.
- `core/build.gradle`: `detektRandomRuleFixture` (a `Detekt` task on `core/src/detektFixture/kotlin`, with the `core`
  main compile classpath, `ignoreFailures = true`, console report off through `detekt-fixture.yml`) and
  `verifyRandomRule`, which reads its checkstyle XML and compares `file:line:column method`
  with `expected-findings.txt`. `-PupdateRandomRuleFixture` writes the file. This follows the golden-file pattern of
  `ContactRuleSelectionTest`.
- The fixture is not in a source set, so Gradle does not compile it and it is not in the JAR.

## Proof

All runs with Gradle only, no game launch.

1. `detektMain` on `engine`, `core`, `lwjgl3` with the current code: 0 issues (`core/build/reports/detekt/main.md`:
   "Issues (0)").
2. Planted call file `core/src/main/kotlin/com/megaman/maverick/game/lintprobe/RandomLintProbe.kt` with 23
   forbidden calls, an `Array` subclass with `shuffle()` and `random()`, and 5 allowed calls: `core:detektMain`
   failed with "Analysis failed with 31 issues". Each forbidden line had at least one finding, no allowed line had
   one. The cases that the text scan missed were all found: `with(gdx) { shuffle() }`, `shuffle()` and `random()` in
   the subclass, `list.shuffle(rng)`, `with(list) { random() }`, `MathUtils.random.nextInt()`, `snapshot.shuffle()`,
   `Vector2().setToRandomDirection()`. The file then moved to the fixture folder (out of `src/main`).
3. Fixture: `core:verifyRandomRule` passes with 33 findings on 30 forbidden lines (after the review fixes); the `allowed` function has none.
   The fixture also holds the "bad" cases of the old guard self-test (import alias `MathUtils as MU`,
   `entries.random()`, `Random(42L)`, `Math.random()`, `Collections.shuffle`).
4. With the entry `kotlin.collections.shuffle` removed from `detekt.yml`, `core:verifyRandomRule` failed:
   "Missing: [47:14 kotlin.collections.shuffle]". The entry was put back.

## Measurements

See the process log entries for the full runs. The times are from this machine (other Gradle builds ran in
parallel in other worktrees), Gradle `--profile`:

Run of 2026-11-16 (`--profile`, `detektMain` forced with `--rerun`, one Gradle process, `--no-daemon`):

| Task | Time |
|---|---|
| `core:detektMain` | 1m27.07s |
| `engine:detektMain` | 32.07s |
| `lwjgl3:detektMain` | 0.81s |
| `core:detektRandomRuleFixture` | 0.58s |
| `core:verifyRandomRule` | 0.31s |
| for scale: `core:test` | 4m35.14s |
| for scale: `core:smokeTest` | 8.62s |
| for scale: `engine:test` + `allocationTest` | 16.78s + 24.24s |

- The detekt tasks add about 2 minutes to the full checks on this machine (total build 7m49.70s). They run after
  `compileKotlin` and need no extra compile.
- No baseline: 0 findings on the current code, because only the one rule is on.
- Memory: detekt runs in the Gradle daemon (`org.gradle.jvmargs=-Xmx4g`); no out-of-memory error in the runs.
- Second full run (after the review fixes): `engine:detektMain` 21.54s, `lwjgl3:detektMain` 0.92s,
  `core:detektMain` UP-TO-DATE (its inputs did not change since the last run). detekt tasks are skipped when the
  sources, the classpath and the config do not change, so a build with no Kotlin change pays almost nothing.
- The `detektRandomRuleFixture` time is short because the analysis classes are already loaded by the `detektMain`
  tasks in the same build.

## Overlap with other branches

Read on 2026-11-16: `compactor-level`, `fix/moon-scythes`, `fix/projectiles-ignore-death`, `fix/small-bugs-3`,
`mega-refactor-024b-character-loadouts`, `mega-refactor-045a-pool-ownership`, `mega-refactor-077b-desert-man`. None
changes `build.gradle`, `gradle.properties`, `ci.yml` or the random package. A branch that adds a forbidden random
call fails `detektMain` after this branch merges and it takes in `master`; the old guard test already failed on most
such calls.
