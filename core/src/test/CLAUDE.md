# Tests in `core`

## Run

- Run with a time limit: `timeout 110 ./gradlew core:test --console=plain`. An `OutOfMemoryError` can kill the Kotest thread, and the run then hangs with no output. A full run takes about 25 s.
- `--tests` filters find no Kotest tests here. Run the full task, and read the results in `core/build/test-results/test/*.xml`.

## Do not

- Do not call code that needs the LibGDX natives. They are not loaded, and the call throws `UnsatisfiedLinkError`. Example: `OrthographicCamera.update()` calls `Matrix4.prj`. Set `camera.position` and the viewport fields, and do not call `update()`: `CameraView` reads those fields.
- Do not make many calls to a MockK mock. MockK keeps each call with a full stack trace, and a mock held in a static field keeps them for the whole run. The test heap is 512 MB. Example: each spawn of `QuickSand` reads a mocked `Texture` for each 1-tile cell, and the regions are static, so keep test sand small.

## What the test setup does not do

- `testServices()` has a camera with no viewport. An `ABSTRACT` body with the default `doUpdate` updates only while it overlaps the camera, so it does not update in such a test. Give the test a `CameraView` with a viewport.
- An entity that was not spawned has a running damage timer, so `canBeDamagedBy` is false for it. A test that expects `false` can pass for the wrong reason. Check the case that expects `true` too.
- An entity with no `PlayPhaseComponent` counts as in play. With a player in play, `FeetSandRule` spawns a splash through `EntityFactories`, which a test does not start. Give a test player `PlayPhaseComponent(PlayPhase.PRE_PLAY)`.
- `MegaEntityFactory` is global. Call `MegaEntityFactory.init(services)` in `beforeEach` and `reset()` in `afterEach`.

## Check that a test can fail

When a new test passes on its first run, break the code it covers and confirm that it fails.
