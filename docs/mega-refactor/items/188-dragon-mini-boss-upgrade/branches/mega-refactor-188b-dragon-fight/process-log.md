# Process log: mega-refactor-188b-dragon-fight

## Status

- State: active
- Goal: MechaDragon fight code: legs, standing sweeping flame breath, stomp landing and takeoff, chin chomp, aimed volley, high-lane charge, panic return sweep; atlas and credits for the 188a art; tests.
- Done: implementation (atlas, map points, `MechaDragon` moves, `MechaDragonFlame`, damage entries, tests, credits); branch folder created inside this branch (stacked on `mega-refactor-188a-dragon-moves-art`, which holds the art); `ITEMS.md` row and the item `ITEM.md` branch list are added on `master` at the merge.
- Next: commit and draft PR (main session); CI is the first compile; play checks 1 to 3 (`temp/play/`).
- Blocked: Gradle cannot run in the cloud (`services.gradle.org` blocked by the proxy, 2026-11-26).

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-26

- Started from `mega-refactor-188a-dragon-moves-art` (PR #36) because the code needs its frames. Deviation from the process: the branch folder is created on this branch, not on `master`, because `master` is not pushed from this cloud session. 
- Implementation (agent): `SCOPE.md`, `PLAN.md`, `NOTES.md` written; all `PLAN.md` steps done. New atlas `MechaDragon`
  by script (`temp/dragon-code/make_atlas.py`, checked by `check_atlas.py`: 15 regions, pixel-equal, deterministic);
  frames moved to `frames/MechaDragon/`; `Bosses1.tps` entries removed. Map points 943/944 in
  `InfernoMan_16x14_v3.tmx`. Code: `mechadragon/` package (art, director, tuning, README), `MechaDragonFlame`,
  `MechaDragon` rewritten (land, breath, chomp, takeoff, aim mix, high lane, charge tell, panic, hard rows).
  `spawn-allocation-baseline.txt`: `MechaDragon.kt` removed (0 allocations in `onSpawn` now). Tests:
  `MechaDragonDirectorTest`, `MechaDragonArtTest`, `MechaDragonTuningTest`, `MechaDragonAtlasTest`,
  `MechaDragonFlameTest`. README credits updated.
- Gradle: one try of `./gradlew --daemon core:compileKotlin`; the wrapper download from `services.gradle.org` got a
  403 from the proxy. Not retried (git.md). Nothing compiled or ran here; CI is the check. No mutation checks run.
