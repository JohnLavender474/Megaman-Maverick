# Scope: mega-refactor-004a-entity-smoke-test

- Status: approved
- Approved: 2026-11-01, with the recommendation on each open question (see "Decisions" in `docs/NOTES.md`)

## Goal

Add a headless `smokeTest` suite that builds every concrete entity class and calls `init()` on it with the real
atlases. A missing region, a bad atlas name or a bad constructor then fails in CI. The branch changes no production
code.

## Item

`004`, Entity build-and-init smoke test. This branch takes all of the item. From `ITEM.md`:

> Build and `init()` every entity class with real atlas data in a headless test, so that a missing region or a bad
> constructor fails in CI.

Proposed scope in `ITEM.md`, with the change that this file asks for:

- A separate `core` JVM test suite `smokeTest` (`core/src/smokeTest/kotlin`, package `...smoketests`) with the headless
  backend and desktop natives. **Kept**, but with no FreeType natives (see "Current state").
- A shared headless harness with real atlas region data on stub textures. **Changed:** real textures, no stubs (open
  question 1).
- One test per concrete entity class: pool fetch plus `init()` with no exception. **Extended:** a silent region miss
  also fails (open question 2).
- A known-failures allowlist that may only shrink. **Kept.**

## Current state

Numbers below come from a throwaway spike on the branch base `f90895d1f` (run on JDK 17, not committed). The spike is
gone from the worktree.

### Results

- 432 concrete entity classes. `PushableBlock$InnerBlock` is left out because it takes a second constructor argument.
- 418 classes pass `init()`. 14 classes throw.
- All 66 `TextureAsset` atlases load.
- Time in the test: 1.5 s for the atlases, 2.1 s to 3.1 s for all `init()` calls. A run of `core:smokeTest` took 7 s
  to 8 s after the first compile.

### The 14 classes that throw

| Class | Cause | Live map |
|---|---|---|
| `JetMet` | `JetMet.kt:111` asks for `frozen`, and the atlas has no frozen region (only `JetMet/Jet`, `JetMet/Stand`, `JetMet/TakeOff`). `JetMet.kt:293` then throws. | `Gauntlet2.tmx` |
| `GapingFish` | No `GapingFish/frozen` region. Already filed: `docs/issues/2026-10-23-gaping-fish-missing-frozen-region.md`. | `Test1.tmx` |
| `ColtonJoe` | No `ColtonJoe/frozen` region. | `Test1.tmx` |
| `TreeStump` | `TreeStump.kt:38` passes `TextureAsset.ENVIRONS_1.name`, not `.source`. | none |
| `PenguinMiniBoss` | No region. | none |
| `SigmaRat`, `SigmaRatClaw`, `SigmaRatElectricBall`, `SigmaRatElectricBallExplosion` | All `SigmaRat/*` regions are missing. | none |
| `Merserker` | All five regions are missing. No code refers to the class. | none |
| `SniperJoe_OLD`, `SpikeBot_OLD` | Regions are missing. | none |
| `BombPenguinBot`, `CanonMet` | `NotImplementedError` (`TODO()`). No code refers to the class. | none |

### Silent misses

`findRegion` returns `null`. Many call sites do not throw on it: 235 lines put the result in a map, and 223 fields are
nullable regions. So "`init()` did not throw" does not prove that the regions exist.

The spike recorded each `findRegion` that returned `null`: 2351 misses in 20 classes.

- 2294 are deliberate. The engine extension `containsRegion` (`TextureAtlasExtensions.kt:5`) calls `findRegion`, and
  `Megaman`, `MegamanTrailSpriteV2`, `WilyFinalBoss` and `InfernoMan` use it as a guard.
- 57 are real unguarded misses in 16 classes. 12 of these classes are in the table above. The other 4 pass `init()` and
  only show up here:

| Class | Region | Live maps | Effect |
|---|---|---|---|
| `ConveyorBelt` | `ConveyorBelt/RightPart-MoveRight` | 6 | Not verified. `ConveyorBelt.kt:140` selects `rRight`. |
| `DesertMan` | `DesertMan/init` | 2 | Not verified. |
| `BulbBlaster` | `BulbBlaster/frozen` | 2 | Not verified. |
| `Coldier` | `Coldier/frozen` | 2 | Not verified. |

### Facts that differ from `NOTES.md`

- The spike needs a `GL20` mock. The headless backend leaves `Gdx.gl` null, so the note that it sets `MockGL20` is
  wrong.
- The PNGs in `assets/sprites/sprite_sheets/` are 2.5 MB on disk, not about 253 MB. Real decode and stub textures take
  the same time (1461 ms with stubs; 1524 ms and 1592 ms with real decode).
- No entity builds a font. The only use (`Togglee.kt:356`) is commented out, so FreeType natives are not needed. The
  spike passed without them.

### Traps for the suite

- In `smokeTest`, `core` is a jar. The directory scan in `EntityConstructorsTest.kt` finds 0 classes there, and the
  first spike passed with 0 classes. The suite needs a floor on the class count.
- `testServices()` is in `core/src/test`. The suite reads it through `sourceSets.test.output`.
- CI runs `engine:test core:test lwjgl3:jar` (`ci.yml:30`), so a new suite does not run until `ci.yml` names it.

## In scope

- `core/build.gradle`: the `smokeTest` JVM test suite, with its working directory at `assets/`. `check` depends on it.
- `.github/workflows/ci.yml`: add `core:smokeTest` to the Gradle job and its report folder to the upload step.
- New test code in `core/src/smokeTest/kotlin/.../smoketests/`:
  - `HeadlessHarness`: starts `HeadlessApplication`, sets the `GL20` mock, loads every `TextureAsset` with real
    textures, builds the services and calls `MegaEntityFactory.init()`.
  - `RecordingTextureAtlas`: a `TextureAtlas` that records each `findRegion` miss. A miss that comes through
    `containsRegion` is not recorded.
  - `EntityInitSmokeTest`: one test per class, a floor on the class count, and the known-failures check.
- `core/src/smokeTest/resources/known-entity-failures.txt`: one class per line with a short reason. It starts with the
  18 classes above. A listed class that now passes fails the test, so the list can only shrink.
- Class discovery in the suite reads a jar or a class folder (Gradle gives a jar; IntelliJ can give a folder).

New production APIs: none.

## Out of scope

- Fixes to the 18 classes. They go to the bug tracker at the merge (open question 3).
- `core/src/main` and `engine/src/main`: no changes.
- `onSpawn`, updates and any other run of the entity. Item `006` covers boot-and-run. Regions that an entity looks up
  after `init()` are not checked.
- Audio assets and fonts. `MegaAudioManager` stays a mock.
- Atlases on disk with no `TextureAsset` entry (84 `.txt` files, 66 entries). Item `040` is the home for atlas checks.
- The `smokeTest` parts for `005` and `006`. The harness holds only what `004` calls. Those items extend it.
- `-Pdecode=true` from the review. Real decode is the only mode.

## Affected areas and risks

- Build: `core/build.gradle`, `ci.yml`. A mistake here can break `core:test` or CI. The contract runs the three
  existing tasks.
- Game behavior: no risk from code, because `core/src/main` and `engine/src/main` do not change.
- A later branch that adds an entity with a missing region now fails CI. This is the purpose of the suite.
- A class-level list hides a new miss in a class that is already listed. Accepted while the list is short.
- The `containsRegion` filter depends on the name of that function. If the name changes, deliberate misses appear as
  new failures, so the break is loud and not silent.

## Overlap with other branches

No other branch is `active` in `ITEMS.md`. The worktrees `mega-refactor-016*`, `031d` and `084e` are of merged
branches. `compactor-level` is not a refactor branch and does not touch tests. Details are in `docs/NOTES.md`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | The test uses `MegaEntityFactory` as it is. No new global in production code. |
| `O1-R5` Code only with a production caller | no effect | Only test code is added, and no production API. |
| `O5-R1` No per-frame allocation | no effect | No production code. |
| `O6-R1` CI passes | helps | CI gains one more task. The rule text names three tasks and stays as it is. |
| `O6-R2` Named checks | helps | Each entity class gets a named check. |
| `O6-R3` Test tools with no window | helps | The suite uses the headless backend. The contract runs it with no `DISPLAY`. |
| `O6-R4` Content errors fail early | no effect | That rule is for map objects (`003`). |

The other always-on rules have no effect, because the branch changes no game code.

## Open questions

Each has a recommendation. The owner chooses at approval.

1. **Real textures or stubs?** Recommended: real. It costs no measured time, needs no stub code, and also catches a
   missing or corrupt PNG. This changes the proposed scope and closes the `ITEM.md` open question.
2. **Do silent misses fail the test?** Recommended: yes. Without this, `ConveyorBelt`, `DesertMan`, `BulbBlaster` and
   `Coldier` pass with a missing region, and the item goal is not met. It adds `RecordingTextureAtlas`.
3. **Fix the 18 classes here, or list them and file bugs?** Recommended: list them and file bugs at the merge. The
   branch then keeps all behavior and needs no play test. `JetMet` in `Gauntlet2.tmx` is the urgent one, and it can go
   in its own `fix/` branch.
