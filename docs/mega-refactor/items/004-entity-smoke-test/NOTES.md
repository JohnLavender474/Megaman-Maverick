# Notes: 004 Entity build-and-init smoke test

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Testing, CI and bug tracking (2026-10-22)

Copied as written from the review section "ITEM 3. Entity build-and-init smoke test (folder, environment)".

````text
(1) Current state
- `MegamanHealthTankTest.kt:21-29` builds `Megaman(game)` with a relaxed MockK `MegamanMaverickGame` and never calls `init()`. That is a unit test. It works only because the tested method does not touch textures.
- Entity `init()` loads atlases. Example: `Met.kt:119` calls `game.assMan.getTextureAtlas(...)`, which is `get(...)!!` (`engine/.../AssetManagerExtensions.kt:8`). Then `Animation(atlas!!.findRegion("Met/Run"), ...)` (`Met.kt:346-351`). A missing region makes `findRegion` return null into a non-null Kotlin parameter, so `init()` throws at once. That is the regression this test can catch. The codebase has 561 `findRegion` calls (grep).
- Some entities create fonts through `BitmapFontHandle.loadFont`, which uses `FreeTypeFontGenerator` (`engine/.../BitmapFontHandle.kt:37`). That needs FreeType natives.

(2) Unit, integration and e2e for this case
- Unit: one class, with its collaborators mocked. A relaxed mocked game returns mock atlases and mock regions, so a missing region is never found. It only proves that constructors and `init()` do not throw on mocks. Low value for this item.
- Integration: several real parts together, with no window. For item 3 that means real `.atlas` region data, the real entity code, real Kotlin reflection, and the LibGDX headless backend (`gdx-backend-headless`). The headless backend sets `Gdx.files`, `Gdx.app`, `Gdx.audio` (mock audio) and `Gdx.gl` (`MockGL20`, where GL calls do nothing). So `Texture` and `TextureAtlas` can be built with no display. Known limit: `SpriteBatch` and `ShapeRenderer` cannot be built on `MockGL20`, because the shader compile status reads as false and the constructor throws. Entities do not build these. The game class does (see item 5). Item 3 is an integration (component smoke) test.
- E2E: the real launcher (`lwjgl3`) with a window, as a player runs it. It needs a display and a GPU (`xvfb` at least). It is fragile in CI and not needed for item 3.
- A cheaper middle path for atlases: parse each `.atlas` with `TextureAtlas.TextureAtlasData` and build regions on one stub `Texture`. This checks region names without decoding about 253 MB of PNGs, so the test stays fast.

(3) Recommendation
- Do not use `src/test/.../smoke-tests/`. A hyphen is not legal in a Kotlin or Java package name (only with backticks, and that is bad practice), and the folder must match the package. Use package `com.megaman.maverick.game.smoketests`.
- Use a separate Gradle JVM test suite in `core` (`testing { suites { smokeTest(JvmTestSuite) } }`), with source in `core/src/smokeTest/kotlin` and its own task `core:smokeTest`. Make `check` depend on it and add it to the CI job. Reasons:
  - it has its own dependencies (`gdx-backend-headless`, `gdx-platform:natives-desktop`, `gdx-freetype-platform:natives-desktop`)
  - it needs `workingDir = assets/`, like `lwjgl3/build.gradle` `run {}`
  - it is slower, so `core:test` stays fast for TDD
  - items 4 (if it uses `init()`) and 5 share the same harness.
- The alternative is Kotest tags (`@Tags("smoke")`) inside `src/test`, filtered by `-Dkotest.tags`. It is simpler, but it mixes native and backend dependencies into the unit classpath. It is acceptable if the owner wants one source set.
- Test shape:
  - A shared `HeadlessHarness`: load natives, set the `Gdx.*` globals, use an `AssetManager` with real atlas data (stub textures), use a relaxed game mock (or a real `GameEngine`), and call `MegaEntityFactory.init(game)`.
  - Discover classes: list concrete `MegaGameEntity` subclasses under `com.megaman.maverick.game.entities.*` by scanning the compiled class dir (or ClassGraph). After item 20, read the registry instead.
  - One dynamic test per class: `fetch(clazz)` and then `init()`.
  - A known-failures allowlist file. The list may only shrink.

(4) Open questions
- One `src/test` with tags, or a separate `smokeTest` suite? I recommend the separate suite.
- Stub textures (fast, checks names) or real PNG decode (slower, also catches corrupt or missing PNG pages)? I suggest stubs in CI and an optional `-Pdecode=true`.
- Are some entity classes known to be retired? Those go on the allowlist, which ties into item 38.

(5) Backlog wording
Title: Entity build-and-init smoke test (headless integration suite)
- New `core` JVM test suite `smokeTest` (`core/src/smokeTest/kotlin`, package `...smoketests`) with the headless backend and desktop natives. It runs in CI.
- A shared headless harness with real atlas region data. The harness is reused by items 4 and 5.
- One test per concrete entity class: pool fetch plus `init()` with no exception, so every `findRegion` resolves.
- A known-failures allowlist that may only shrink.

(6) Dependencies: item 1. It helps 20 (the registry replaces scanning), 27 (texture cache) and 38 (finds dead classes). Items 4 and 5 build on its harness.
````
