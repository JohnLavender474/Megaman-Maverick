# Plan: mega-refactor-051e-camera-rotation

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-01

Each step builds and passes `./gradlew engine:test core:test`, and is one commit. Stage files by path.

## Steps

### 1. Take in `master`

- Changes: in the parent worktree, `git merge master` (`084f`, and `031e` if it merged). Run the checks there. Then merge
  the parent into this branch.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes on the parent and on this branch. `docs/NOTES.md` is updated
  with the overlap at that commit.
- Done: yes

### 2. Rotation classes and the temporary suite

- Changes: add `CameraRotation`, `CameraOrientation` and `CameraView` in `engine/.../camera/`, with no game wiring. Move the
  turn logic from `RotatableCamera` with its details: `accumulator` is not cleared at a new turn, the angle is cumulative,
  and `direction` changes at the start. Add `CameraRotationTest` and `CameraViewTest` in `engine`. Add
  `FrozenRotatableCamera` and `RotationEquivalenceTest` in `core/src/test/`.
- Check: the temporary suite passes for each assertion in `docs/CONTRACT.md`. The new classes have callers in steps 3 to 5.
- Done: yes

### 3. The game camera becomes a plain camera

- Changes: `CameraController` finds the `CameraRotation` in `addEffect`, writes the orientation after the parts update, and
  gets `applyOrientation()`. `MegamanMaverickGame`: `gameCamera` is an `OrthographicCamera`, with new `cameraRotation` (it
  holds the end callback) and `cameraView`. `MegaServices.gameCamera` is the `cameraView`. `MegaLevelScreen`: add the
  rotation as the first effect in the place of `CameraRotationStep`, start it at `:849`, snap it and apply at `:1077`, and
  read the view at `:885` and `:1036`. `CameraLimitStage`, `CameraRoomClamp` and `CameraScroll` take a `CameraView`.
  `ICameraPart` and `CameraController` keep their type parameter in this step, with `C` as `OrthographicCamera`.
  Delete `CameraRotationStep.kt`. `GameCameraState` stays, with its writes.
- Check: the tests pass, with the 5 test rigs changed. Add the controller tests from `docs/CONTRACT.md`. One short launch of
  `REFACTOR_TEST_051 --room scroll_gravity`.
- Done: yes

### 4. `rotating` on `ICameraView`, and `GameCameraState` goes

- Changes: add `ICameraView.rotating`. Change the 40 reads with one script, because it is the same change on each line:
  `services.camera.rotating` to `services.gameCamera.rotating`, and `game.camera.rotating` to `game.cameraView.rotating`
  in `cameraHold`. Remove the 3 writes, including `:372`. Remove `camera` from the game, `MegaServices` and `TestServices`.
  Delete `GameCameraState.kt` and `GameCameraStateTest`.
- Check: `grep` finds no `camera.rotating` on a `GameCameraState`. The tests pass.
- Done: yes

### 5. Backgrounds

- Changes: `Background` gets a plain `OrthographicCamera`, a nullable `CameraRotation` from `rotatable`, and a
  `CameraOrientation`. `update` steps the rotation and writes. The snap at `MegaLevelScreen.kt:534` stays. The 2 rotation
  functions return `Unit`.
- Check: the tests pass. One short launch of `MOON_MAN --room room6`.
- Done: yes

### 6. Remove the type parameter and the old classes

- Changes: `ICameraPart` and `CameraController` lose their type parameter (20 and 6 lines). The 7 parts take `Camera`.
  `CameraController<*>` in `CameraExtensions.kt` follows. Delete `RotatableCamera.kt`, `RotatableCameraTest` and
  `utils/CameraRotator.kt`. `GameCameraFieldTest` follows the new field.
- Check: `grep` finds no reference from the checks in `docs/CONTRACT.md`, other than the temporary suite. The tests pass.
- Done: yes

### 7. Automated contract checks

- Changes: none in the code, unless a check fails.
- Check: `./gradlew clean engine:test core:test lwjgl3:jar` passes, and the warning count is the baseline of 96. One short
  launch of each room in the contract, with `timeout 12` after the build. A clean exit passes.
- Done: yes

### 8. The owner's play test

- Changes: fixes only. After a fix to an experimental check, the control checks run again.
- Check: the 10 manual checks in `docs/CONTRACT.md`. The result goes in the process log.
- Done: yes

### 9. Before the merge

- Changes: delete `FrozenRotatableCamera` and `RotationEquivalenceTest` in the last commit. Check the file list. Prepare the
  root `CLAUDE.md` text for `master`.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. The owner gives merge approval. The merge goes to
  `mega-refactor-051-master` with a squash.
- Done: yes
