# Notes: mega-refactor-008a-dev-launch-options

## Decisions

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
