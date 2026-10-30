# Plan: mega-refactor-051a-camera-read-interface

How to do the work in `SCOPE.md` (approved 2026-10-30), checked by `CONTRACT.md` (approved 2026-10-30).
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`.

- Status: approved
- Approved: 2026-10-30

## New API and its first callers

| API | First production caller |
|---|---|
| `E/camera/ICameraView` (`direction`, `getRotatedBounds(out)`, `toGameRectangle(out)`, `overlaps(bounds, out)`) | `MegaServices.gameCamera` (type), `getGameCameraCullingLogic` |
| `ICameraView.getRotatedBounds()` (core, pooled) | about 40 entity lines, `GameEntityUtils.kt:60,66,83` |
| `ICameraView.toGameRectangle()` (core, pooled) | `Disintegration.kt:72`, `RisingLavaRiver.kt:343`, `DisappearingBlocks.kt:137` |
| `ICameraView.overlaps(bounds)` (core, pooled) | `TubeBeamerV2.kt:271` and the other 1-argument `overlaps` callers |
| `MegaServices.gameCamera` | the 76 entity reads |
| `MegamanMaverickGame.gameCamera` | `MMG` (holder build, pixel-perfect toggle), `MegaLevelScreen.kt:236` |

## Overload facts (from the scan)

- The 9 `overlaps` callers use 2 forms: 1 argument (the `core` `Camera.overlaps(bounds)`, pooled box) and 2 arguments
  (the `engine` `Camera.overlaps(bounds, out)`: `DragonFly.kt:216,227,238`, `IceCubeMaker.kt:122`). On an
  `ICameraView` receiver, the 2-argument call resolves to the interface member, and the 1-argument call to the new
  `core` extension. Both run the same frustum test as now.
- The new `core` extensions go in the package `com.megaman.maverick.game.utils.extensions`, with the same names as
  the `Camera` extensions there (`overlaps`, `toGameRectangle`). A file that imports `...utils.extensions.overlaps` or
  `...toGameRectangle` (or `.*`) then sees the new overload with no import change. `getRotatedBounds` is a member now,
  so its callers need a new import (`...utils.extensions.getRotatedBounds`), unless they import `.*`.
- In `RotatableCamera`, the members `overlaps(bounds, out)` and `toGameRectangle(out)` must not call the `Camera`
  extension of the same signature by name: inside the class the member wins, which recurses. Write the body
  (`frustum.boundsInFrustum(bounds.toBoundingBox(out))`; `out.setSize(...)`, `out.setCenter(...)`).
- A call with a static `RotatableCamera` receiver and 1 argument (`MegaLevelScreen`, `CameraManagerForRooms`) can
  match both the `Camera` and the `ICameraView` extension and fail to build as ambiguous. Fix any such site by hand
  (step 3), with the form that it uses now.

## Steps

### 1. Baseline

- Changes: none.
- Check: `./gradlew engine:test core:test lwjgl3:jar` on the branch. Write in the process log the test counts and the
  number of unique compiler warnings.
- Done: yes

### 2. Temporary equivalence suite on the old code

- Changes: core test sources: `Temp051aFrozenCameraReads` (copies of the old `RotatableCamera.getRotatedBounds()`
  body, the old `core` `Camera.toGameRectangle(out)` and `Camera.overlaps(bounds)`) and
  `Temp051aCameraReadEquivalenceTest` (as in `CONTRACT.md`: seeded positions, the 4 directions through
  `immediateRotation`, rectangles inside, outside and on the edge, `camera.update()` before each frustum test). The
  "new" side calls the current API for now.
- Check: `./gradlew core:test --tests '*Temp051a*'` passes on the old code (the suite and the frozen copy agree).
- Done: yes

### 3. The interface, the camera, the holder and the host (hand edits)

- Changes:
  - `E/camera/ICameraView.kt`: the interface (4 members, `val direction`).
  - `RotatableCamera`: implements `ICameraView`; `getRotatedBounds(out)` in place of `getRotatedBounds()`;
    `toGameRectangle(out)` and `overlaps(bounds, out)` members (bodies written out, see "Overload facts"); its 2 debug
    logs use the pooled form.
  - `C/utils/extensions/CameraExtensions.kt`: the 3 pooled `ICameraView` extensions.
  - `MegaServices`: the private field becomes `ICameraView`; `fun getGameCamera()` stays for this commit, so that the
    commit builds. A public `val gameCamera` next to `fun getGameCamera()` clashes on the JVM (the same getter
    signature), so step 4 makes the field public and removes the function.
  - The `getRotatedBounds` import in each file that needs it (moved here from step 4: it is needed as soon as
    `getGameCamera()` returns `ICameraView`).
  - The temporary suite reads through an `ICameraView` reference (moved here from step 5: on a `RotatableCamera`
    receiver the 1-argument `overlaps` and `toGameRectangle()` are ambiguous, as "Overload facts" predicted).
  - `MegamanMaverickGame`: `val gameCamera: RotatableCamera` as a field, made with the game; `create()` keeps
    `setToDefaultPosition()` and the viewport set-up; the `onJustFinishedRotating` lambda stays the same (it reads
    `camera` and `events` only when it runs, after `create()`). The holder build (`MMG:417`) and the pixel-perfect
    toggle (`MMG:691`) read the field. `getGameCamera()` goes. `MegaLevelScreen.kt:236` reads `game.gameCamera`.
  - `GameEntityUtils.kt:56-60`: `getGameCameraCullingLogic(camera: ICameraView, ...)`; the entity overload and the 2
    predicates read `services.gameCamera`.
  - `TestServices`: parameter `gameCamera: ICameraView = RotatableCamera()`.
  - Any ambiguous call from "Overload facts".
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes, with the temporary suite. Commit.
- Done: yes

### 4. Mechanical rename (one commit)

- Changes:
  - Script (scratch): in `core/src/main` and `core/src/test`, `services.getGameCamera()` → `services.gameCamera`
    (76 lines). The commented-out line in `GravityCapsule.kt` stays.
  - Hand: in `MegaServices`, `private val gameCamera` → `val gameCamera`, and remove `fun getGameCamera()`.
- Check:
  - The contract check script: each changed line is the same line with `getGameCamera()` → `gameCamera`, or one of
    the `MegaServices` hand edit.
  - `grep -rn "getGameCamera" core lwjgl3 --include=*.kt` finds only `GravityCapsule.kt:166`.
  - `./gradlew engine:test core:test lwjgl3:jar` passes. Compare the unique warnings with step 1. If an `engine`
    `overlaps` import is now unused (`DragonFly`, `IceCubeMaker`), remove it in step 5.
  - Commit. Add the commit SHA to `.git-blame-ignore-revs` in a separate commit, with a comment line.
- Done: yes

### 5. Tests on the new code, import cleanup

- Changes:
  - Permanent test (core test sources, next to `GameCameraStateTest`): the host's `gameCamera` exists when the game
    is made (before `create()`), it is the same instance on each read, and its `direction` is `UP`. The holder build
    needs a running game, so the build of `postCreate()` (which passes the field) and manual check 8 cover the
    holder side.
  - Remove the imports that step 4 left unused.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes; no new unique warning compared with step 1. Commit.
- Done: yes

### 6. Docs

- Changes: the root `CLAUDE.md` (the `MegaServices` member list, the `camera` line, the entity base classes text:
  `getGameCamera()` → `gameCamera`, and that the holder type is the engine `ICameraView`); the branch process log.
- Check: `grep -rn "getGameCamera" CLAUDE.md` finds nothing. Commit.
- Done: yes

### 7. Automated contract checks

- Changes: none.
- Check: all items under "Automated checks" in `CONTRACT.md`. Write the results in the process log.
- Done: yes

### 8. Owner play test

- Changes: none.
- Check: the owner runs the manual checks in `CONTRACT.md`. Write the result in the process log.
- Done: yes

### 9. Remove the temporary suite

- Changes: delete `Temp051aCameraReadEquivalenceTest` and `Temp051aFrozenCameraReads`, in the last commit before the
  merge.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Write the counts in the process log.
- Done: yes
