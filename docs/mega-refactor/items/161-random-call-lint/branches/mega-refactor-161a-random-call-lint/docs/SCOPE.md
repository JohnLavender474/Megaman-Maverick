# Scope: mega-refactor-161a-random-call-lint

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: Claude's choices, pending owner confirmation (the owner asked for the work end to end with no stops,
  2026-11-16)
- Approved: no

## Goal

Keep rule `O2-R2` (random values only from the seeded source) true with a check that resolves types, not only text.

## Item

`161` (`ITEM.md`), proposed scope, copied in full:

- `RandomSourceGuardTest` (`core/src/test`, from `011a`) scans the source text. It does not catch a call with no
  receiver (`shuffle()` or `random()` inside `with(array)` or in an `Array` subclass) or a call with an argument
  (`list.shuffle(rng)`).
- Option: a detekt `ForbiddenMethodCall` rule (with type resolution) for `MathUtils.random*`, `kotlin.random.Random`,
  `java.util.Random` and the collection `random()`/`shuffle()`/`shuffled()` functions, allowed only in
  `engine/.../common/random/`. Then the source-scan test can go, or stay as a fast first check.

Open question of the item: "Does the project want detekt in the build (CI time, a baseline for the existing code)?"
Answer with measured facts: see `NOTES.md`, "Measurements".

## Current state

- `core/src/test/kotlin/com/megaman/maverick/game/RandomSourceGuardTest.kt` (on `master`): 13 regexes over the text
  of `core`, `engine` and `lwjgl3` main sources. It misses `with(array) { shuffle() }`, `shuffle()` in an `Array`
  subclass, `list.shuffle(rng)`, `ThreadLocalRandom.current()` written with an import alias, and any call whose text
  does not match (for example `Vector2.setToRandomDirection()`, which draws from `MathUtils.random`).
- `engine/src/main/kotlin/com/maverick/game/engine/common/random/`: `GameRandom`, `SeededRandom` (the only user of
  `java.util.Random`), `README.md` ("Rules for a change" names the guard test).
- The build has no static analysis. `build.gradle` applies `kotlin` to all subprojects in `configure(subprojects)`.
  Kotlin `2.3.20`, Gradle `8.10.1`, JDK 17.
- Main code has no forbidden call today (the guard test passes on `master`; the new check also passes, see
  `NOTES.md`). `UUID.randomUUID()` is used for map keys in `UpdatablesComponent`, `AnimationsComponent`,
  `MegaUtilMethods` and `Water`; the maps are `OrderedMap`, so the keys do not change an order. It is not a gameplay
  value, and the rule does not forbid it.

## In scope

- Add the detekt Gradle plugin `dev.detekt` `2.0.0-alpha.3` (Kotlin `2.3.21` inside) to all subprojects, with one
  config `config/detekt/detekt.yml` that turns on only `style>ForbiddenMethodCall`. No other rule, so no baseline.
- `check` depends on `detektMain` (type resolution). The plain `detekt` task (no types) is off.
- A permanent fixture check: `core/src/detektFixture/kotlin/.../RandomRuleFixture.kt` (forbidden and allowed calls,
  not compiled) and `core:verifyRandomRule`, which runs the rule on it and compares the findings with
  `core/src/detektFixture/expected-findings.txt`. `check` depends on it.
- CI runs `detektMain core:verifyRandomRule` and uploads the detekt reports on a failure.
- Delete `RandomSourceGuardTest` (decision `D2`).
- Forbid `SeededRandom(...)` outside the random package (`D5`), with `@Suppress("ForbiddenMethodCall")` on its one
  call in `MegamanMaverickGame`. The annotation changes no behavior.
- Update `engine/.../common/random/README.md` ("Rules for a change") and the repo `CLAUDE.md` ("Build, run, test").

## Out of scope

- Other detekt rules, style findings or a baseline.
- `UUID.randomUUID()` (identity keys, not a gameplay value).
- Random calls inside libraries (for example LibGDX particle effects): the rule sees only calls in this repo's code.
- Java sources: detekt reads only `.kt` files (2 Java files in main, with no random call).
- Test sources: tests make their own `java.util.Random` on purpose (`SeededRandom(java.util.Random)`).
- `objectives/O2-multiplayer.md` (`O2-R2` check text and current state) and `O6`: `master` only, at the merge.

## Decisions

All are Claude's choice, pending owner confirmation.

- `D1` Tool: detekt `2.0.0-alpha.3` `ForbiddenMethodCall` with type resolution (`detektMain`). It is the only detekt
  line that reads Kotlin 2.3 code (1.23.8 has Kotlin 2.0.21 inside). An alpha version is a risk; the comparison is in
  `NOTES.md`.
- `D2` `RandomSourceGuardTest` goes. The detekt rule finds every case of its self-test, and more. Two lists would
  drift. The fixture check replaces its self-test.
- `D3` A permanent fixture check (`core:verifyRandomRule`), not only a one-time planted call. A wrong name in the
  config gives no error in detekt; the fixture shows it.
- `D4` Only main sources are checked (`detektMain` in each module). `detektTest` is not in `check`; the config also
  excludes test folders, in case someone runs it.
- `D5` `SeededRandom(...)` is on the list too, so no code outside the random package makes a second stream. The
  game's one call has a `@Suppress` annotation. Option: allow it, and trust the review.

## Affected areas and risks

- `build.gradle`, `core/build.gradle`, `gradle.properties`, `config/detekt/detekt.yml`, `.github/workflows/ci.yml`.
- Build time and memory: detekt with the Kotlin Analysis API runs in the Gradle daemon. Measured times in `NOTES.md`.
- An alpha plugin can change its config keys or task names in a later version. The fixture check fails if the rule
  stops finding calls.
- No game code changes, so no gameplay can change.

## Overlap with other branches

See `NOTES.md`, "Overlap with other branches".

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O2-R2` Seeded random | helps | the check sees types; it closes the gaps of the text scan |
| `O6-R1` CI passes | helps | CI also runs `detektMain` and `verifyRandomRule`; the 4 named tasks stay |
| `O6-R2` Named checks | helps | `CONTRACT.md` names the new checks |
| `O1-R5` Code only with a production caller | no effect | build config and a fixture; no new API |
| `O5-R1` No per-frame allocation | no effect | no game code |

## Open questions

None for the owner beyond the pending decisions above.
