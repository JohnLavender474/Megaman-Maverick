# Plan: mega-refactor-062d-queued-spawn-query

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-27

## New API and first caller

| API | First caller |
|---|---|
| `GameEngine.hasClaimOn(entity: IGameEntity): Boolean` | the pool return in `MegamanMaverickGame.kt` `onSpawnRejected` (step 3) |

`GameEngine.contains` loses its parameter and becomes `internal` (less API). The temporary suite is test code.

## Design

```kotlin
fun hasClaimOn(entity: IGameEntity) =
    entity.existenceState.liveNow || entities.contains(entity) || entitiesToSpawn.contains(entity)
```

The three parts are the old condition, negated, in the same order. No allocation; at most two set lookups.

## Steps

### 1. Baseline

- Changes: none. A clean build (`./gradlew clean engine:test core:test lwjgl3:jar`); write the test counts and the
  set of unique compiler warnings to scratchpad `062d-warnings-baseline.txt`.
- Check: the build passes; the counts and the warnings are in the process log.
- Done: yes

### 2. `hasClaimOn` and its tests

- Changes: `GameEngine.hasClaimOn`. Permanent tests for each path (`docs/CONTRACT.md`), in
  `EntityExistenceStateTest` or a new `EngineClaimTest`. The temporary suite `TempHasClaimOnContractTest`: seeded
  random sequences (the generator of the `062b` permanent invariant test, with nested calls, rejected spawns and
  `reset()`), and before every call, `hasClaimOn(e) == e.existenceState.liveNow || engine.contains(e, true)` for every
  entity; counters for the three deciding parts, each at least once; at least 10,000 sequences.
- Check: `./gradlew engine:test` passes; the counters are in the process log.
- Done: yes

### 3. The pool return asks `hasClaimOn`

- Changes: `MegamanMaverickGame.kt` `onSpawnRejected`: `if (!entity.engine.hasClaimOn(entity))
  entity.runnablesOnDestroy[ConstKeys.POOL]?.invoke()`; the comment above it stays as it is.
- Check: `./gradlew engine:test core:test` passes.
- Done: yes

### 4. Remove the old parameter and the temporary suite

- Changes: `GameEngine.contains(entity)` without `containedIfQueuedToSpawn`, `internal`; delete
  `TempHasClaimOnContractTest`. This is the last code commit; the play test runs on it.
- Check: the greps in `docs/CONTRACT.md`; `./gradlew engine:test core:test` passes.
- Done: yes

### 5. Contract checks and the owner's play test

- Changes: none.
- Check: all automated checks in `docs/CONTRACT.md` on a clean build (warnings compared with the baseline); then the
  owner plays the manual checks. The results go in the process log.
- Done: yes
