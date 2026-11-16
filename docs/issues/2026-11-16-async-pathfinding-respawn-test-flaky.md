---
status: open
verified: yes
area: build
gameplay-change: no
found-by: `mega-refactor-045a-pool-ownership` merge (2026-11-16)
related-items: []
---

# `PathfindingRespawnTest` "drops the search of an entity that leaves" fails in some full runs

## Where

`engine/src/test/kotlin/com/maverick/game/engine/pathfinding/PathfindingRespawnTest.kt:107`:
`finished.await(5, TimeUnit.SECONDS) shouldBe true` (from `fix/small-bugs-3`).

## What happens

On `master` at `b35f8e9a5`, the test failed in 2 runs of `engine:test core:test core:smokeTest lwjgl3:jar` in a row:
the pathfinder's `call()` did not finish within 5 s. The same code passed in the `045a` worktree (full run), in the
`045b` worktree (`engine:test` alone) and in the main checkout (`engine:test --rerun` alone). No engine pathfinding
code changed between the passes and the fails.

## What should happen

The test passes in every run.

## How to reproduce

Run `JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew engine:test core:test core:smokeTest lwjgl3:jar --console=plain`
in the main checkout a few times. Not found: what makes the search not start or not finish; one guess (not checked)
is that the first `engine.update(1f)` does not submit a search in some runs, so `release` frees nothing and the
latch waits 5 s.

## Notes

Cause (found 2026-11-16): `engine.update(1f)` submits the search to the cached thread pool of
`AsyncPathfindingSystem`. The destroy in the next `engine.update(0f)` calls `future.cancel(true)` in `onEntityLeft`
(`AsyncPathfindingSystem.kt:42`) some microseconds later. When the new pool thread has not yet entered
`FutureTask.run()`, the task is cancelled before `call()` runs, so `finished` never counts down and the await gives
`false`. A warm-JVM harness (`temp/flaky-pathfinding-test/`) lost the race in 295 of 300 loops; cold JVMs in 5 of 10.
The guess in "How to reproduce" is wrong: the search is submitted, and it is dropped before it starts.

The production code is correct: a search that is dropped before it starts is the intended result of `fix/small-bugs-3`
(`c52ce18f6`). Only the test has the race.

## Short contract

Branch: `fix/async-pathfinding-test-race`, off `master`, in `.worktrees/fix/async-pathfinding-test-race`.

Intended changes (test only):

- `PathfindingRespawnTest`, case "drops the search of an entity that leaves, so its next spawn gets no old path": the
  test pathfinder counts down a new `started` latch at the start of `call()`. After `engine.update(1f)` and before
  `engine.destroy(entity)`, the test waits for it (`started.await(5, TimeUnit.SECONDS) shouldBe true`). So the
  destroy always cancels a search that runs, which is the path that the case checks. `Thread.sleep(20)` stays.

Not changed:

- No production code. `AsyncPathfindingSystem` gets no injected executor (no API with only a test caller, `O1-R5`).
- The other cases of `PathfindingRespawnTest` and all other tests.

Checks:

- `engine:test --rerun --tests 'com.maverick.game.engine.pathfinding.PathfindingRespawnTest'` passes in 10 runs in a
  row, and the harness on the new order (with the `started` wait) gives no fail in a warm loop.
- With the `futureMap.remove(component)?.cancel(true)` line of `AsyncPathfindingSystem.onEntityLeft` removed for one
  run, the case fails (`consumed` is not `0`). The line is then restored, and `git diff` shows no production change.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

No play check: the change is in a test only, and the game code does not change.
