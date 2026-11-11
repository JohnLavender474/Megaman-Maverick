---
status: open
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor-060c-protoman-buster (2026-11-11)
related-items: [060]
---

# `core:test` keeps the services and mocks of finished tests alive and fills its 1 GB heap

## Where

- The `core` test suite (`core/src/test/`), which runs in one test JVM with `maxHeapSize = '1g'` (`core/build.gradle`).
- Not known yet: the object that keeps the old services alive. Candidates are in "Notes".

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

## Short contract

Branch: `fix/core-test-heap-leak`, off `master`, in `.worktrees/fix/core-test-heap-leak`.

Intended changes:

- Find the object that keeps the services of finished tests alive (a heap dump or a reference path, not a guess), and
  remove the hold: in test setup and teardown, or in the production code if a static there is the cause.
- No change to gameplay. A production change is limited to the static that holds the reference.

Changed files: decided by the investigation; written here before the code changes.

Not changed:

- The test heap size (`core` keeps `maxHeapSize = '1g'`), the tests' assertions, gameplay.

Checks:

- The per-spec retained-heap log shows no spec that keeps growth after its end (each spec's growth under 10 MB), and
  the live `MegaServices` count after the suite stays under 5.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch.
- After the merge, `mega-refactor-060c-protoman-buster` takes in `master`, and its `core:test` passes.
