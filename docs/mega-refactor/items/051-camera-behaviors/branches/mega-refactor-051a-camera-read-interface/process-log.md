# Process log: mega-refactor-051a-camera-read-interface

## Status

- State: merged
- Goal: A read-only camera interface in `engine`; entities read the camera through it; `getGameCamera()` becomes a
  property on the holder and on the host.
- Done: all plan steps; squash-merged into `master` as `7370943a0` (2026-10-30).
- Next: none.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- Branch start. The owner approved the merge target (directly into `master`) and the 051 branch split (see the
  051 `ITEM.md`, "Decisions"). Created the branch and its worktree `.worktrees/mega-refactor-051a-camera-read-interface/`
  from `master`.
- Scan done. Wrote the draft `docs/SCOPE.md` (3 open questions) and the overlaps in `docs/NOTES.md`.
- Owner approved `docs/SCOPE.md` with the 3 recommended answers (see `docs/NOTES.md`). Room scan of the live TMX
  files for the manual checks. Wrote the draft `docs/CONTRACT.md`.
- Owner approved `docs/CONTRACT.md`. Wrote the draft `docs/PLAN.md`. While writing it, 2 contract items changed
  (to confirm with the owner with the plan): the import check (the new `core` extensions share the names and the
  package of the `Camera` extensions, so the imports stay; the check is the static type `ICameraView` at each call),
  and the permanent test (the holder build needs a running game, so the test covers the host field only).
- Owner approved `docs/PLAN.md` and the 2 contract changes.
- Step 1 (baseline, at `1f28f5f3f`): `./gradlew engine:test core:test lwjgl3:jar` passes. engine 575 tests, core 272,
  0 failures, 0 skipped. 103 unique compiler warnings (`w:` lines).
- Step 2: wrote `Temp051aFrozenCameraReads` and `Temp051aCameraReadEquivalenceTest` (50 cases). The test classpath
  has no libGDX natives, and `camera.update()` calls the native `Matrix4.prj` for the frustum. The frozen file has a
  test-only `updateWithoutNatives(camera)`: `update(false)`, then the same frustum planes as `Frustum.update` (libGDX
  1.14.2), with the Java `Vector3.prj`. Both sides read the same frustum, so the comparison holds. Sanity asserts: a
  rectangle at the camera center overlaps, a far one does not. 50 of 50 pass on the old code.
- Step 3: `ICameraView` (engine), `RotatableCamera` implements it, the 3 pooled `core` extensions, the host field
  `gameCamera`, the holder field typed `ICameraView`, `getGameCameraCullingLogic(camera: ICameraView, ...)`,
  `TestServices`. Plan changes (reasons in `docs/PLAN.md`, step 3): the holder keeps `getGameCamera()` until step 4
  (JVM clash with a public `gameCamera`); the `getRotatedBounds` imports (24 entity files, `MegaLevelScreen`,
  `GameEntityUtils`, `RotatableCamera`) came here from step 4; the temporary suite reads through `ICameraView` here,
  not in step 5. No production call was ambiguous. Checks: `./gradlew engine:test core:test lwjgl3:jar` passes,
  engine 575, core 322 (272 + 50 temporary), 0 failures. After a forced recompile: 103 unique warnings, the same set
  as step 1.
- Step 4: mechanical commit `eebd95fc3` (76 lines `services.getGameCamera()` → `services.gameCamera`, plus the
  `MegaServices` hand edit: the field becomes public, `getGameCamera()` goes). Check script: 78 changed `+` lines;
  every line except the `MegaServices` hand edit is the token rename. `grep -rn "getGameCamera()"` finds only the
  commented-out line in `GravityCapsule.kt`. (The contract grep `getGameCamera` also matches
  `getGameCameraCullingLogic`; the check uses `getGameCamera()`.) Build and tests pass (engine 575, core 322).
  `ba9c541ab` lists `eebd95fc3` in `.git-blame-ignore-revs`.
- Step 5: permanent `GameCameraFieldTest` (2 tests: the host field exists at construction and is the same instance;
  direction `UP`). Removed the `engine` `overlaps` import from `DragonFly` and `IceCubeMaker` (their only `overlaps`
  calls now go to the `ICameraView` member). Build and tests pass: engine 575, core 324 (272 + 50 temporary + 2).
- Step 6: root `CLAUDE.md` names `gameCamera` (host and holder) and `ICameraView`; no `getGameCamera` left in it.
- Step 7 (automated contract checks, at `b133266bd`, after `./gradlew clean`): `engine:test core:test lwjgl3:jar`
  passes. engine 575, core 324 (272 + 50 in `Temp051aCameraReadEquivalenceTest` + 2 in `GameCameraFieldTest`),
  0 failures, 0 skipped. 103 unique warnings, the same set as step 1 (diff empty). `grep -rn "getGameCamera()"` finds
  only `GravityCapsule.kt:166` (commented out). Mechanical check: done in step 4. Every camera read goes through
  `services.gameCamera` or a local from it (`FlipperPlatform.kt:214`, `DisappearingBlocks.kt:137`), static type
  `ICameraView`.
- Step 8: owner play test (verbatim): "All tests pass". All manual checks in `docs/CONTRACT.md` pass.
- Step 9: deleted `Temp051aCameraReadEquivalenceTest` and `Temp051aFrozenCameraReads`. After `./gradlew clean`:
  `engine:test core:test lwjgl3:jar` passes; engine 575, core 274 (272 + 2 in `GameCameraFieldTest`), 0 failures;
  103 unique warnings, the same set as step 1. All plan steps are done.
- `170b37e05` merged `master` (at `1eaef3415`: the `--loadKeyboardPrefs` flag in `MegamanMaverickGame`,
  `DesktopLauncher`, the run scripts and `CLAUDE.md`; the `031d` and `084a` starts) into the branch, with no conflict.
  The step 9 checks ran after it. Overlap check with the active branches before the merge: see `docs/NOTES.md`.
- Owner approved the merge. Squash-merged into `master` as `7370943a0`; `1d50a659e` puts the squash SHA in
  `.git-blame-ignore-revs`.
