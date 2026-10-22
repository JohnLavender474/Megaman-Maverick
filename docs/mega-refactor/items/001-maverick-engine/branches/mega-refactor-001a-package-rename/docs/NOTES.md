# Notes: mega-refactor-001a-package-rename

## Decisions

## Baseline before the rename (2026-10-22)

Command, on the branch before any code change (same code as `master` at `f595ec73a`):

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

| Check | Result |
|-------|--------|
| Build | `BUILD SUCCESSFUL`, 15 tasks |
| `engine:test` | 50 spec files, 427 tests, 0 failures, 0 errors, 0 skipped |
| `core:test` | 4 spec files, 49 tests, 0 failures, 0 errors, 0 skipped |
| `lwjgl3:jar` | `lwjgl3/build/libs/Megaman-Maverick-1.0.1.jar`, 35,886,830 bytes |
| Kotlin compiler warnings (`w:` lines) | 133 |

The counts come from the JUnit XML files in `*/build/test-results/test/`.

JDK notes:

- With the default JDK 25, `:engine:compileTestKotlin` fails ("Inconsistent JVM Target
  Compatibility"). See the bug candidate in `TODO.md`.
- `/usr/lib/jvm/java-17-openjdk-amd64` is a runtime only, with no compiler, so
  `:lwjgl3:compileJava` fails with it. Use a full JDK 17, for example Corretto in `~/.jdks/`.

## Overlap with other branches

None. No other `mega-refactor-` branch is active (2026-10-22).
