# Notes: 032 Platform-neutral `engine` module

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEM 31: Platform-neutral `engine` module".

````text
1. Current state (complete list of what is not neutral in `engine/src/main`)

Dependencies in `engine/build.gradle`:
- `gdx-controllers-desktop` (desktop natives). `lwjgl3/build.gradle:37` already declares it, so remove it from `engine`.
- `gdx-tools:1.6.3`: very old and desktop-only. No `com.badlogic.gdx.tools` import exists in `engine`, `core` or `lwjgl3`. Remove it.
- `gdx-freetype:1.11.0` is pinned, but `gdxVersion` is 1.14.2 and `core` uses `$gdxVersion`. This is a version mismatch on the classpath. Use `$gdxVersion`.
- `kotlin-reflect` (as `api`). `engine` main code uses only stdlib reflection (`KClass.cast`, `isInstance`). Full reflection is used only in `core`: `MegaEntityFactory.kt:20` (`primaryConstructor`) and `GameObjectPools.kt:19` (`kotlin.reflect.full.cast`). The library is large, and TeaVM supports it poorly.

Threads and concurrency:
- `AsyncPathfindingSystem.kt:7-9, 28` (`Executors.newCachedThreadPool`, `Future`, timeouts). `SimplePathfindingSystem` already exists, and `MegamanMaverickGame.kt:842-843` selects "async" or "sync".
- `RuntimeDiagnostics.kt:4-8, 175` (writer `Thread`, `java.io.File`/`FileWriter`, `ArrayBlockingQueue`).

Other JVM-only APIs:
- `GameLogger.kt:7-8, 76` (`java.time.LocalTime`, `DateTimeFormatter`). TeaVM support for these is partial.
- `System.currentTimeMillis()` seeds `Random` in `UtilMethods.kt:22`, `ArrayExtensions.kt:112` and `ObjectSetExtensions.kt:84` (also item 10).
- `System.nanoTime()` in `RuntimeDiagnostics`. This works on all backends, but use a clock service.
- Java reflection: `E::class.java.enumConstants` in `EnumStateMachineBuilder.kt:11`. TeaVM support must be checked.
- Latent bug: `FloatExtensions.kt:5` `"%.${decimals}f".format(this).toFloat()` throws `NumberFormatException` in locales that use a comma decimal separator (common on Android phones). I found no callers today. Delete it or fix it.

Fine as is: `Controllers` (`ControllerUtils.kt`) comes from `gdx-controllers-core`, which has Android and web backends.

In `core` (not the engine, but these block web and Android): `AsyncFileWriter.kt` (threads, `java.io`) and `MegaEntityFactory` (reflection construction).

TeaVM vs GWT: GWT compiles Java source, so it cannot compile Kotlin. The LibGDX web path for Kotlin is `gdx-teavm`, which compiles JVM bytecode. TeaVM has limited reflection and no real parallel threads, so the reflection-built factory and the thread code must have alternatives. The Graal native option (`enableGraalNative` in `core/build.gradle:13`) also gains from removing reflection.

2. Options

A. Remove the problems in place: delete the async pathfinder and file output, and use only LibGDX APIs. Simple, but it loses useful desktop tools.
B. A `PlatformServices` interface in `engine`. Each launcher module supplies the implementation.
C. Split `engine` into `engine-core` (neutral) and `engine-jvm` (threads, files). This is more modules for little gain over B.

3. Recommendation

Use B.
- `interface PlatformServices { val clock: Clock; val taskRunner: TaskRunner; fun logSink(name: String): LogSink?; val supportsThreads: Boolean; val supportsFileOutput: Boolean }`.
- `lwjgl3` supplies: a thread pool, `FileWriter` sinks, `System.nanoTime`.
- A future `android` module supplies: a thread pool, sinks in internal storage.
- A future `teavm` module supplies: a synchronous runner, a console sink, no files.
- `AsyncPathfindingSystem` takes a `TaskRunner`. On web it falls back to sync. `RuntimeDiagnostics` writes to a `LogSink`. `GameLogger` formats time from the clock (millis since start) with no `java.time`.
- `engine` dependencies become only `gdx`, `gdx-controllers-core`, `gdx-freetype` (`$gdxVersion`) and `kotlin-stdlib`.
- Replace `MegaEntityFactory` reflection with the explicit registry (item 20, constructor lambdas).
- Add a CI guard (item 1): either a `teavm` compile-only target, or a dependency check that fails if `engine` pulls a `*-desktop`, `lwjgl` or `gdx-tools` artifact.

4. Open questions for the owner

- Is web a real target or a nice-to-have? It decides whether the TeaVM compile check goes in CI now.
- Can desktop-only dev tools (diagnostics file output, async pathfinding) be absent on web and Android builds?

5. Proposed backlog wording

Title: Platform-neutral `engine` module
- Remove `gdx-tools` and `gdx-controllers-desktop` from `engine`. Align `gdx-freetype` with `$gdxVersion`. Move `kotlin-reflect` out (after item 20).
- Add a `PlatformServices` interface (clock, task runner, log sink, capability flags). `lwjgl3` supplies the JVM implementation.
- Route `AsyncPathfindingSystem`, `RuntimeDiagnostics`, `GameLogger` time and `Random` seeding through it.
- Delete or fix `Float.round` (it breaks in comma-decimal locales).
- CI guard: `engine` has no desktop or JVM-only dependencies (optional TeaVM compile check).

6. Dependencies

20 (reflection-free registry), 10 (random source), 43 (logging), 8 (diagnostics sink), 1 (CI), 32 and 34 (they use this for the web and Android targets).
````
