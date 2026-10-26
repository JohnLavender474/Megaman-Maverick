---
status: open
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor 031 scan (2026-10-25)
related-items: [032]
---

# `jamepadVersion` in `gradle.properties` does not match the Jamepad version that the build uses

## Where

`gradle.properties:8` (`jamepadVersion=2.0.14.2`), `lwjgl3/build.gradle:41`, `master` `9a74fb08a`

## What happens

`lwjgl3` asks for `com.badlogicgames.jamepad:jamepad:2.0.14.2`, but `gdx-controllers-desktop` `2.2.3` needs
`2.0.20.0`, and Gradle picks the newer one:

```
+--- com.badlogicgames.jamepad:jamepad:2.0.14.2 -> 2.0.20.0 (*)
```

So the property names a version that the game does not use.

## What should happen

Set `jamepadVersion=2.0.20.0`, or remove the direct dependency and let `gdx-controllers-desktop` bring Jamepad.

## How to reproduce

`./gradlew -q lwjgl3:dependencies --configuration runtimeClasspath | grep jamepad`
