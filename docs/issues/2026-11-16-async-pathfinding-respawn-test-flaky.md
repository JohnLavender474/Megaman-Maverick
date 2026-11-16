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
