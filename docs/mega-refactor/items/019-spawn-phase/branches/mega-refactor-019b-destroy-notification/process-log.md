# Process log: mega-refactor-019b-destroy-notification

## Status

- State: merged
- Goal: tell a spawner at once when its entity is destroyed; one spawn phase per tick
- Done: planning (scope, contract, plan approved); steps 1 to 7
- Next: none (merged 2026-10-27)
- Blocked: none (`062b` merged).

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master` at `69b2297d6`, in parallel with `062b` (owner's choice). Worktree `.worktrees/mega-refactor-019b-destroy-notification`. 
- Scan: findings and the `062b` overlap in `docs/NOTES.md`.
- The owner said to plan now while `062b` runs in its own session; the engine code waits for `062b` to merge.
- Wrote the draft `docs/SCOPE.md`. Queue merge moved out (backlog `019c`).
- `062b` merged; `master` merged into this branch (`1a2ebeae2`). Draft scope updated for the new engine: the hook goes in `destroy()` (the `onDestroyQueued` engine function is gone) and also covers a cancelled respawn; open question 1 now has the `062b` evidence; overlap section updated.
- Scope approved (owner, 2026-10-27: "yes"; with `onSpawnQueued` added by the owner's decision). Draft `docs/CONTRACT.md`: the temporary old-versus-new engine suite, hook tests, baseline-first spawner tests for the two `TODO` cases and the pooled-reuse case; six manual checks of camera and room-entry respawns (rooms found in the live TMX files: `spawn_room` entities in `WILY_STAGE_1` `room8` and `DESERT_MAN` `room9`/`room14`).
- Contract approved (owner, 2026-10-27: "yes"). Draft `docs/PLAN.md` (7 steps). Design detail: `SpawnerForBoundsEntered` sets `isEntered` from the overlap at the moment of the destroy call, so an off-screen death respawns when the area enters and an on-screen death still needs the area to leave and enter.
- Plan approved (owner, 2026-10-27: "yes").
- Step 1 (baseline, after the `master` merge): clean build passes; 527 engine and 163 core tests, 0 failures; 110
  unique warnings (scratchpad `019b-warnings-baseline.txt`).
- Step 2 (core spawner tests on the baseline, not committed yet): `CT/spawns/SpawnerReleaseTest.kt` runs frames in
  the level screen's order (spawn pass, engine spawns, `engine.update`, events). On the baseline: `TODO` case 1 fails
  (2 entities expected, 1 made: the frame's event was dropped), pooled reuse fails (the spawner still holds the
  reused entity), `TODO` case 2 fails (2 expected, 1 made after an off-screen death); "ignores events while its
  entity lives", "free again after a rejected spawn" and "an on-screen death respawns only after the area leaves and
  enters" pass. All as predicted.
- Step 3 (temporary suite on the baseline): `ET/TempOldGameEngine.kt` (today's engine, renamed) and
  `ET/TempOldEngineContractTest.kt` (10,000 seeded sequences with nested calls, the `062b` call mix): passes in about
  2 s. With `destroy()` of a live entity returning `false` in the frozen copy, it fails at once (sequence 0, step 12);
  undone.
- Step 4 (engine hooks): `IGameEntity.onSpawnQueued()` and `onDestroyQueued()`, called in `GameEngine.spawn`/`destroy`
  in the accepting cases of scope items 1 and 7. `ET/entities/EntityExistenceHooksTest.kt` (12 tests): each case with
  its state, the refused/repeated/reset cases, and on 2,000 random sequences with nested calls: `onSpawnQueued` runs
  once for each accepted spawn, `onDestroyQueued` once for each accepted destroy of an entity that was not `dead`, and
  no hook runs outside such a call (updates, resets). Plan detail: in the one case where a destroy comes inside the
  entity's own respawn `onSpawn`, the state stays `RESPAWN_QUEUED` until that `onSpawn` ends (the `062b` engine), so
  the test checks that hook by count, not by state. The temporary suite passes (no change to actions, return values
  or states). Engine tests: 540, 0 failures.
- Step 5 (the spawners): `MegaGameEntity.runnablesOnDestroyQueued` and its `onDestroyQueued()` override (copies the
  runnables into a reusable array first, so a runnable can remove itself); `Spawner` puts a release runnable under
  `ConstKeys.SPAWNER` on each entity it spawns, `release()` removes it, `onSpawnRejected()` and `reset()` release, the
  `dead` poll goes; `SpawnerForBoundsEntered.onReleased()` sets `isEntered` from the overlap at the destroy call;
  `MegaLevelScreen` sets one shared `spawnCameraBounds` before the spawn pass and passes it through
  `MegaMapLayerBuildersParams` to `SpawnerFactory.spawnerForWhenInCamera`; the `019a` `TODO` is gone. Plan detail:
  `SpawnsManager.reset()` now resets each spawner before it clears the list (no spawner sets `onCull`), so a level
  teardown, where the engine runs no hook, leaves no release runnable on pooled entities. Core tests: the three step 2
  tests pass now; added: `reset()` frees the spawner and leaves no runnable; a non-respawnable spawner leaves no
  runnable; a pooled entity reused by another owner carries no runnable. Tests: 540 engine, 172 core, 0 failures.
  Greps: no `dead` poll in `spawns/`, no `getRotatedBounds` in `spawns/`, no `TODO` at the spawn pass.
- Step 6 (contract checks, clean build): build and jar pass; 540 engine and 172 core tests, 0 failures; the 110
  warnings are the same set as the baseline (7 lines in `MegaLevelScreen.kt` moved down by 3);
  `contact-rule-selection.txt` unchanged; the greps pass. Launches of the six contract rooms: no exception until the
  timeout. Map errors at load, not from this branch: `DESERT_MAN` (the five filed in
  `docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`) and one in `WILY_STAGE_1` (`DuoBallCanon` on the
  `blocks` layer; bug candidate in `docs/TODO.md`). Waiting for the owner's play test.
- Step 7: the owner's play test passes (2026-10-27, "all tests pass"; six checks). Waiting for the merge approval.
- The owner approved the merge (2026-10-27, "approved"). The temporary suite is deleted in this last commit; clean build passes; 539 engine and 172 core tests, 0 failures.
