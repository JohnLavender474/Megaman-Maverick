# Contract: mega-refactor-161a-random-call-lint

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them.

- Status: Claude's choices, pending owner confirmation
- Approved: no

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes (one fewer spec: `RandomSourceGuardTest` is deleted, `D2`).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain` passes on `engine`, `core` and `lwjgl3` (no forbidden random call in main code).
- [ ] `./gradlew core:verifyRandomRule` passes: the rule finds exactly the calls in
  `core/src/detektFixture/expected-findings.txt`. The fixture holds each case that the old text scan missed:
  `with(array) { shuffle() }`, `shuffle()` and `random()` in an `Array` subclass, `list.shuffle(rng)`, and the
  allowed calls through `GameRandom` give no finding.
- [ ] Proof by a planted call: a forbidden call put in `core/src/main` fails `core:detektMain`; removed, it passes.
  Done once in the branch (`NOTES.md`, "Proof").
- [ ] Proof that the fixture check fails on a rule list change: one entry removed from `detekt.yml` fails
  `core:verifyRandomRule` with the missing finding named. Done once in the branch (`NOTES.md`, "Proof").
- [ ] The review before the play test finds no problem in the diff.

## Manual playthrough

No play check. The branch changes no game behavior and no asset: only build files, a fixture that is not compiled,
a deleted test, docs, and one `@Suppress("ForbiddenMethodCall")` annotation on the `SeededRandom(...)` call in
`MegamanMaverickGame`, which the compiler ignores. The launched game uses the same classes as before. The JAR build (`lwjgl3:jar`) is the check
that the build change does not touch the game.

## Behaviors to check

None.

## Intended changes

None in gameplay. Build: `check` and CI run the detekt random-call rule.
