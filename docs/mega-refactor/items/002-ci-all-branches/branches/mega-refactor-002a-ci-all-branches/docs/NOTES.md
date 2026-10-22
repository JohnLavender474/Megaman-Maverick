# Notes: mega-refactor-002a-ci-all-branches

## Decisions

- (2026-10-22) Skip JDK 25 (owner). Gradle 8.10.1 cannot run on JDK 25: with the build script
  changed, it fails with "Unsupported class file major version 69". A test with
  `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=17`) made JDK 25 work, but that file is
  git-ignored on purpose (`.gitignore:4`) and outside the scope, so it is not added.
- (2026-10-22) `SpriteSystemTest` failed after the Kotest upgrade. Cause: its three sprites have
  equal draw priority, so its `TreeSet` kept only one; Kotest 4's `shouldContain` used the set's
  comparator-based `contains()` and passed anyway, while Kotest 5 checks object equality. The test
  now collects the sprites in an `ArrayList`. No game code is affected.

## Baseline (2026-10-22)

Taken once for all wave 1 branches, in the `002a` worktree, when the code of all three branches
was the same as `master` at `612edecf8` (`git diff master <branch> -- . ':!docs'` was empty for
each).

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

| Check | Result |
|-------|--------|
| Build (JDK 17) | succeeds |
| `engine:test` | 50 spec files, 427 tests, 0 failures |
| `core:test` | 4 spec files, 49 tests, 0 failures |
| `lwjgl3:jar` | `Megaman-Maverick-1.0.1.jar`, 35,895,548 bytes |
| Kotlin compiler warnings | 133 |
| Build with the default JDK 25 (`./gradlew clean engine:test`) | fails at `:engine:compileTestKotlin` ("Inconsistent JVM Target Compatibility", Java 17, Kotlin 25) |

## Overlap with other branches
