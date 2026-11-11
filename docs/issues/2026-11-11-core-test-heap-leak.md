---
status: closed
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor-060c-protoman-buster (2026-11-11)
related-items: [060]
---

# `core:test` keeps the services and mocks of finished tests alive and fills its 1 GB heap

## Where

- The `core` test suite (`core/src/test/`), which runs in one test JVM with `maxHeapSize = '1g'` (`core/build.gradle`).
- The holder (found 2026-11-11, see "Measurements of the remedies"): MockK `1.14.11` keeps every mock alive for the
  whole test JVM, with every call it recorded and a stack trace for each call. The recorded calls hold the old
  entities and services as arguments.

## What happens

Each entity test builds its own `MegaServices` with `testServices()`, with relaxed MockK mocks for the engine, the assets
and the audio. A relaxed mock records every call with a captured stack trace. After a test class ends, its services
and their mocks stay reachable, so the heap keeps growing over the suite.

Measured on `mega-refactor-060c-protoman-buster` after it took in `master` at `63c04a58d` (class histograms of the
branch's own test JVM, every 8 s):

| Time | Live heap | `MegaServices` alive | `io.mockk.Invocation` alive |
|---|---|---|---|
| 8 s | 147 MB | 29 | 18,897 |
| 16 s | 885 MB | 115 | 140,384 |
| 32 s | 1,012 MB | 187 | 159,349 |
| 72 s | 1,019 MB | 188 | 163,023 |

Heap still in use after a full GC at the end of each spec (the growth over that spec):

| Spec | Retained growth |
|---|---|
| `PushableBlockTest` | 231 MB |
| `MegamanXSabreTest` | 158 MB |
| `MegamanXTest` | 126 MB |
| `ProtomanTest` | 102 MB |
| `QuickSandFloorsTest` | 97 MB |
| `HeartTankSandTest` | 93 MB |

`master` passes, close to the limit (1364 tests). `060c` adds 2 tests to `ProtomanTest` and goes over it: 4 of 4 full
runs hit `OutOfMemoryError`. Since `fix/test-jvm-oom-hang`, the test JVM then exits at once, so the rest of the suite
does not run.

## What should happen

A finished test class leaves nothing reachable, and the suite runs well under its heap limit, so that new tests do
not push it over.

## How to reproduce

Run `./gradlew core:test` on `mega-refactor-060c-protoman-buster` (`cb2539260` or later): the test JVM ends with
"Terminating due to java.lang.OutOfMemoryError". To watch the growth, take `jcmd <pid> GC.class_histogram` of the test
JVM whose working folder is the worktree's `core/` (select it by PID and folder, not by name).

## Notes

- Ruled out: `MegaGameEntities` (the static entity registry). With the registry cleared after each spec, 128
  `MegaServices` stayed alive.
- The 6 specs above all call `MegaEntityFactory.init(services)`, which builds and initializes 25 `Bullet` and 25
  `Block` each time (`ENTITIES_TO_PRELOAD`). `MegaEntityFactory.reset()` clears the pools but keeps `services`. Not
  checked yet: static state that `init()` of these entities fills (for example companion region maps from the mocked
  atlas), and the pools of `GameObjectPools`.
- Work files: `temp/mega-refactor-060c-protoman-buster/heap/` (histograms `h_*.txt`, `x_*.txt`; per-spec log
  `perspec.txt`).

## Research (2026-11-11)

Web research at the start of `fix/core-test-heap-leak`. This repo uses MockK `1.14.11` and Kotest `5.9.1`
(`gradle.properties`).

- **A known MockK bug in exactly `1.14.11`:** "Regular mocks are never garbage collected"
  ([mockk#1566](https://github.com/mockk/mockk/issues/1566), closed). In `JvmMockKWeakMap` (inside
  `WeakMockHandlersMap`) the key (the mock) is weak, but the value strongly references the key: handler → `MockKStub`
  → dispose routine → proxy → the mock. So every `mockk<T>()` stays alive for the whole test JVM, with all the calls
  that it recorded. The reporter: about 5k tests, a 512 MB heap, `OutOfMemoryError`; about 90k `MockKStub` objects, about
  50% of the heap. Reproduction with 2000 mocks: no cleanup, `clearAllMocks()`, `unmockkAll()` and
  `clearAllStubsFromMemory()` each collect 0 of 2000; only an internal stub dispose collects all 2000.
- **Follow-up** ([mockk#1575](https://github.com/mockk/mockk/issues/1575), closed by #1576): the JUnit `MockKExtension`
  and `unmockkAll()` leave every regular mock reachable until the JVM exits. Real suite: 5686 tests, 512 MB, live
  `MockKStub` counts of 23k to 83k, OOM in 2 of 3 runs. The root is `JvmMockKDispatcher.DISPATCHER_MAP`.
- **Fix released in MockK `1.14.12`** (2026-11-07, the newest release):
  - [mockk#1567](https://github.com/mockk/mockk/pull/1567): `clearAllStubsFromMemory()` now disposes the stubs it
    removes, so the mocks can be collected. It does not help if nothing calls a cleanup.
  - #1576: an opt-in "release mocks after each test" for the JUnit 5 `MockKExtension` (not used by Kotest). A
    released mock cannot be used again.
- **Workarounds reported for `1.14.11`:** `forkEvery` on the Gradle `Test` task (#1566); or after each test class,
  `unmockkAll()` and then `dispose()` on every `MockKStub` through the internal `io.mockk.impl` API (#1575; fragile).
- **Older reports of the same symptom:** [mockk#596](https://github.com/mockk/mockk/issues/596) (1.10 to 1.13, mocks not
  freed after their test class; closed without a diagnosis). A Kotest maintainer called a similar heap problem a MockK
  issue ("mockk keeping an internal list") and suggested a cleanup in `prepareSpec`/`finalizeSpec`
  ([Kotlin Slack](https://slack-chats.kotlinlang.org/t/503418/i-m-running-into-a-problem-with-mockk-on-machines-with-small)).
- **The same pattern in Mockito:** recorded invocations with full stack traces can fill the heap; Mockito has
  `withSettings().stubOnly()`, which records nothing ([mockito#86](https://github.com/mockito/mockito/issues/86)). The
  search found no MockK equivalent; `excludeRecords` (a user report in #596) narrows what a mock records.
- **General practice** (Gradle forum, `forkEvery` threads): fix the leaking state; `forkEvery` and a bigger
  `maxHeapSize` are stopgaps that slow the run or hide the leak.

Fit with the measurements above: 163k retained `io.mockk.Invocation` objects with captured stack traces, and the
retained `MegaServices` are reachable from mocks (an entity or the services given as an argument to a relaxed
engine mock). Not yet confirmed with a heap dump of this suite.

## Measurements of the remedies (2026-11-11)

Same suite (`master` at `738de7b23`, 1364 tests, 140 specs), measured with a temporary Kotest listener (a full GC and
the heap in use after each spec; a class histogram at the end of the suite). Work files:
`temp/fix-core-test-heap-leak/`.

| Measure at the end of the suite | Baseline `1.14.11` | `1.14.12` + `clearAllStubsFromMemory()` after each spec | `1.14.12` + clear recorded calls | `1.14.11` + clear recorded calls |
|---|---|---|---|---|
| Heap in use | 1009 MB | 75 MB | 110 MB | 110 MB |
| Largest growth over one spec | 230 MB | 4 MB | 20 MB | 20 MB |
| `io.mockk.Invocation` alive | 157,065 | not in the top list | not in the top list | not in the top list |
| `MockKStub` alive | 6,898 | 2,159 | 6,898 | 6,898 |
| `MegaServices` alive | 181 | 63 | 146 | 146 |
| Failed tests | 0 | 220 | 0 | 0 |

- The recorded calls (with their stack traces) are about 900 MB of the 1 GB. Clearing only them after each spec is
  enough, also on MockK `1.14.11`.
- Releasing the mocks (`clearAllStubsFromMemory()`, which frees them only from `1.14.12`) fails 218 tests: about 320
  entity classes cache their texture regions in a companion map, filled once from the first atlas they see. In tests
  that atlas is a relaxed mock, so the cached regions are mocks of the first spec; once they are released, later specs
  reach the real `Texture` code (`TextureData` is null). The 2 other failures: the call throws in a spec that runs
  before any mock exists. A full release waits for item `028` (a game-owned texture cache in place of the companion
  maps).

## Short contract

Branch: `fix/core-test-heap-leak`, off `master`, in `.worktrees/fix/core-test-heap-leak`.

Intended changes:

- Changed 2026-11-11 (owner), after the research and the measurements above: the holder is MockK's mock registry, so
  no heap dump is needed. A Kotest project config clears the recorded calls of every mock after each spec
  (`clearAllMocks` with `recordedCalls = true` and `answers = false`, `childMocks = false`), so the mocks stay usable
  for the static region caches. MockK stays at `1.14.11`. A `TODO` at the config names item `028`.
- No change to gameplay or production code.

Changed files: `core/src/test/kotlin/io/kotest/provided/ProjectConfig.kt` (new).

Not changed:

- The test heap size (`core` keeps `maxHeapSize = '1g'`), the tests' assertions, gameplay.

Checks:

- Changed 2026-11-11 (owner): the heap in use at the end of the suite is under 200 MB, and `io.mockk.Invocation` is
  not in the top list of the class histogram. (The old checks, each spec's growth under 10 MB and under 5 live
  `MegaServices`, need the mocks released, which waits for `028`.)
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch.
- After the merge, `mega-refactor-060c-protoman-buster` takes in `master`, and its `core:test` passes.

## Resolution

Fixed in `fix/core-test-heap-leak`, squash commit `8a25a5a6b` (2026-11-11): a Kotest project config
(`core/src/test/kotlin/io/kotest/provided/ProjectConfig.kt`) clears the recorded calls of every mock after each spec.
MockK stays at `1.14.11`. On the branch: `engine:test` 675, `allocationTest` 2, `core:test` 1364, `smokeTest` 452,
JAR built; sampled peak heap of `core:test` 486 MB (before: the full 1 GB by 16 s). `mega-refactor-060c-protoman-buster`
with the config: 1380 tests, 0 failures. A full release of mocks (and a heap that does not grow per spec) waits for
item `028`.
