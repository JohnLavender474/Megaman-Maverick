# Notes: 011 One seedable, interceptable random source with named streams

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 10. One seedable random source".

````text
(1) Current state
- `UtilMethods.random = java.util.Random(System.currentTimeMillis())` (`engine/.../common/UtilMethods.kt:22`). `getRandom*` and `getRandomBool` use it. The seed is the time, so it cannot be reproduced.
- `Array.superRandom()` (`ArrayExtensions.kt:109-113`) and `OrderedSet.superRandom()` (`ObjectSetExtensions.kt:81-89`) make a new `Random(currentTimeMillis)` on each call. Two calls in the same millisecond give the same index.
- `OrderedSet.random()` (`ObjectSetExtensions.kt:68-79`) uses `MathUtils.random` and also removes the chosen item and adds it again. So each read changes the iteration order of the set. This hides state changes and breaks determinism.
- `Array.getRandomElements` calls gdx `shuffle` (MathUtils).
- Counts in `core/src/main/kotlin` (mine): `getRandom` 188, `MathUtils.random` 19, `.random()` 23, `.shuffle`/`.shuffled` 7, `superRandom` 3. The engine also has 16 `getRandom` references.
- Kotlin `.random()` and `.shuffled()` use `Random.Default`, which cannot be seeded.
- X6 for comparison (`engine.md:45`, `:216`, `:519`): one global RNG. Seed 3383 at boot. One step for each play frame. Values 0-255.

(2) Options for the API and how code reaches it

API in the engine (`com.mega.game.engine.common.random`). Interface `GameRandom` with: `nextInt(bound)`, `nextInt(min, maxInclusive)`, `nextFloat()`, `nextFloat(min, max)`, `nextBoolean()`, `chance(p)`, `pick(array)`, `pickIndex(n)`, `shuffle(array)`.

Implementations:
- `SeededRandom`, on `RandomXS128`. Its state can be saved and restored.
- `ScriptedRandom`: returns queued values, and fails or logs when the queue is empty. Use it to force a boss pattern or a drop in tests.
- `RecordingRandom`: a decorator that counts calls and hashes the values (optionally with call-site tags) for divergence checks.

Named streams:
- `gameplay`: AI choices, attack patterns, drops, spawn variants.
- `cosmetic`: particles, debris, leaves, explosion variants, shake.
- Optionally `audio`.
- With separate streams, a particle burst, or an FPS guard that skips debris, does not shift the boss RNG.

How code reaches it:
- (a) Pure injection through `LevelContext` (item 15). Pros: clean, testable. Cons: about 240 call sites to change, and they must wait for item 15.
- (b) A global facade (`GameRng.gameplay`, `GameRng.cosmetic`) whose backing objects are swapped at level start or at replay start. Pros: one edit makes the 188 `getRandom` calls seedable (change `UtilMethods.getRandom*` to delegate). It matches X6. Cons: global state; tests must reset it.
- (c) Both: (b) now, and the facade becomes a field of `LevelContext` when item 15 lands.

Prevention of new direct uses:
- There is no detekt or ktlint in the build. CI has `engine-tests.yml`, `game-tests.yml` and `utils-tests.yml`.
- Option 1: add detekt with `ForbiddenMethodCall` for `kotlin.random.Random.*`, `kotlin.collections.random`, `shuffled`, `shuffle`, `MathUtils.random*`, `java.util.Random.<init>` and `Math.random`.
- Option 2: a small grep check in `utils/` that CI runs, with an allow-list (the RNG package only).
- The grep check is cheaper to start with.
- Backstop: in replay mode, also seed `MathUtils.random`, because LibGDX itself uses it (for example in gdx `Array.shuffle` and particles).

(3) Recommendation
Use (c):
- Add `GameRandom` and the streams in the engine.
- Make `UtilMethods.getRandom*` delegate to `GameRng.gameplay`.
- Delete both `superRandom` functions.
- Fix `OrderedSet.random()` so that it does not reorder the set.
- Move the cosmetic sites to the `cosmetic` stream.
- Add the grep check in CI.
- Replace the wall-clock ordering at `MegaGameEntity.kt:54` with a tick or spawn counter.

(4) Open questions for the owner
- Do you want two streams (gameplay/cosmetic) or more (for example one for each boss)?
- In normal play, do you want a random seed for each run (logged, so that a bug can be reproduced) or a fixed seed?
- Must the RNG state go into save files (item 6)? (Probably not.)

(5) Proposed backlog wording

Title: "One seedable, interceptable random source with named streams"
- Add the `GameRandom` interface (seeded, scripted and recording implementations) in `engine`.
- Add gameplay and cosmetic streams. Seed them from `--seed` (item 7) or from the replay header.
- Delegate `getRandom*` to the gameplay stream. Delete `superRandom`. Fix the side effect of `OrderedSet.random()`. Move `.random()`, `.shuffle()` and `MathUtils.random` calls to the streams.
- Add a CI check that rejects new direct random calls outside the RNG package.

(6) Dependencies
- Items 9, 5 and 7 use it.
- Item 15 (context) will hold it later.
- The replay backlog item needs it.
````
