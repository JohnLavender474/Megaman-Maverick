---
status: open
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor-001a-package-rename (2026-10-22)
related-items: [002]
---

# The build fails when Gradle runs on JDK 25

## Where

`build.gradle:61`

## What happens

With JDK 25 as the Gradle JVM (the default `java` on the owner's machine), `:engine:compileTestKotlin`
fails:

```
Inconsistent JVM-target compatibility detected for tasks 'compileTestJava' (17) and 'compileTestKotlin' (25).
```

`build.gradle:61` sets `jvmTarget` to 17 only for `compileKotlin`. `compileTestKotlin` takes the
JDK version, so the Java and Kotlin test targets differ.

## What should happen

The build works with any recent JDK, for example through a JVM toolchain set to 17, or a
`jvmTarget` for all Kotlin compile tasks.

## How to reproduce

```
./gradlew engine:test
```

with JDK 25 on the `PATH` and no `JAVA_HOME` set to a JDK 17.

## Notes

CI uses JDK 17, so it does not see this. Workaround: run Gradle with a full JDK 17, for example
`JAVA_HOME=~/.jdks/corretto-17.0.13`. `/usr/lib/jvm/java-17-openjdk-amd64` on the owner's machine
is a runtime only, with no compiler, so it fails at `:lwjgl3:compileJava`. A fix belongs with
item `002`.

## Progress

- 2026-10-22: `mega-refactor-002a` (squash `52dde921f`) sets the JVM target to 17 for every compile
  task, so the build now works when Gradle runs on JDK 21. JDK 25 still fails: Gradle 8.10.1 itself
  cannot run on it ("Unsupported class file major version 69"). The owner chose to skip JDK 25 for
  now. A fix needs a newer Gradle, or a committed `gradle/gradle-daemon-jvm.properties` (git-ignored
  today, `.gitignore:4`).
