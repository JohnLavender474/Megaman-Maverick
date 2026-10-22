# TODO: mega-refactor-001a-package-rename

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- The build fails when Gradle runs on JDK 25 (the default `java` on the owner's machine):
  `:engine:compileTestKotlin` reports "Inconsistent JVM Target Compatibility" (Java 17, Kotlin
  25). `build.gradle:61` sets `jvmTarget` only for `compileKotlin`, not for `compileTestKotlin`.
  CI uses JDK 17, so it does not see this. Workaround: run Gradle with
  `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`. A fix (for example a JVM toolchain) belongs
  with item `002`.
