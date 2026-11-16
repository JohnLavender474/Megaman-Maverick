# Scope: mega-refactor-011a-game-random

- Status: approved
- Approved: 2026-11-14

## Goal

Every random value in `engine/` and `core/` comes from one seeded source, `services.random`, so that a test or a
later replay can fix the seed or script the values.

## Item

Item `011` (`ITEM.md`, "Proposed scope"):

- Add a `GameRandom` interface in `engine` with seeded, scripted and recording implementations.
- Gameplay and cosmetic streams, seeded from `--randSeed` (`008`) or from the replay header.
- Delegate `getRandom*` to the gameplay stream. Delete `superRandom`. Fix `OrderedSet.random()`, which reorders the
  set. Move `.random()`, `.shuffle()` and `MathUtils.random` calls to the streams.
- A CI check that rejects new direct random calls outside the RNG package.
- Replace the wall-clock spawn-time ordering (`MegaGameEntity.kt:54`) with a counter.

Owner's input (2026-10-22): "a proxy should exist for all "random" calls so that we can intercept them with either
a fixed seed or an outright stubbed value".

## Current state

The full inventory is in `temp/mega-refactor-011a-game-random/scan.md` in the main checkout (not committed).

Four sources, all shared by gameplay and cosmetic code:

| Source | Where | Notes |
|---|---|---|
| `UtilMethods.random` | `engine/.../common/UtilMethods.kt:22` | `java.util.Random(currentTimeMillis)`. `getRandom*`, `getRandomBool`, `doIfRandomMatches` use it. Tests call `setSeed` on it. |
| gdx `MathUtils.random` | direct, and inside gdx `Array.shuffle()`, `Array.random()`, engine `OrderedSet.random()`, `WeightedRandomSelector` | static, never seeded |
| Kotlin `Random.Default` | `entries.random()` (12 sites), `IntRange.shuffled()` (`WilyFinalBoss.kt:1393`) | cannot be seeded |
| new `Random(currentTimeMillis)` per call | `Array.superRandom()` (`ArrayExtensions.kt:109-114`), `OrderedSet.superRandom()` (`ObjectSetExtensions.kt:74-83`) | two calls in one millisecond give the same index |

Counts: `core` 229 random lines in 79 files; `engine` 8 helper files. Per class (core files): `gameplay` 48,
`cosmetic` 17, `mixed` 7 (`WoodCrate`, `BigAssMaverickRobot`, `WilyFinalBoss`, `AbstractEnemy`, `PreciousGemCanon`,
`PreciousTron`, `Rain`), test entities 2. No screen uses randomness.

Special sites:

- The reorder is in `OrderedSet.superRandom()` (`ObjectSetExtensions.kt:79-80`), not in `OrderedSet.random()`. The
  only caller is `MechaDragon.kt:701`, which clears the set at `:704`. `Array.superRandom()` callers:
  `MechaDragon.kt:679`, `:680`.
- `MegaGameEntity.timeSpawned` (`MegaGameEntity.kt:71`, `currentTimeMillis`): only reader
  `MegamanWeaponsHandler.kt:338`, which sorts the Moon Scythes oldest first. Scythes of one shot share a millisecond,
  so the stable sort keeps the `getAllSpawned` order.
- Direct users of `UtilMethods.random`: `D1000Core.kt:681`, `D1000Orbs.kt:142-143`, `D1000DebrisWarning.kt:110`
  (passes it to `D1000CeilingDebris.roll(..., java.util.Random)`). `OrbAim` takes its source as a parameter
  (`BigAssMaverickRobot.kt:515`).
- Tests: 7 boss test files call `UtilMethods.random.setSeed`. `D1000Rig.kt:213-214` and `D1000EyeTest.kt:65-68`
  search for a seed with the `java.util.Random` algorithm, so they depend on the generator type and the draw order.
- 4 engine helpers have no caller: `doIfRandomMatches`, `randomVector2`, `RandomSelectorBehavior`,
  `MutableArray.shuffle()`/`random()`.
- Wall-clock logic that is not random: `AsyncPathfindingSystem` (`:47`, `:66`, a 500 ms wall-time timeout, and a path
  applied on the frame its future is done; only with `--pathfinding async`), `DarknessV2`/`DarknessV4` (debug log
  timings only; `DarknessV4` is not used).

## In scope

- `engine/.../common/random/`: `GameRandom` (interface), `SeededRandom` (on `java.util.Random`, so the D1000 seed
  searches stay valid). One stream (`D2`). API: `nextInt(min, maxInclusive)`, `nextFloat(min, max)`, `nextBoolean()`, `pick(array)`, `pick(vararg)`,
  `shuffle(array)`, and only the other functions that a call site needs.
- Access (`D1`): `MegaServices.random: GameRandom`, built in `postCreate()` from the session seed. Screens and other
  code that has the game read `game.services.random` (none use randomness today).
- Every site in the inventory moves to `services.random`. The gameplay/cosmetic classes in the scan stay as data for
  a later split.
- Engine helpers that draw (`getRandomPositionInBounds`, `getRandomElements`, `WeightedRandomSelector`, `Pendulum`)
  take a `GameRandom`. Delete the 4 helpers that have no caller, both `superRandom` functions, `UtilMethods.random` and
  `getRandom*`.
- `D1000CeilingDebris.roll` and `OrbAim` take a `GameRandom` in place of a `java.util.Random`.
- Session seed (`D3`): `--randSeed <long>` (launcher, `resolveLaunchOptions`), else the clock; the seed is written to the log at start.
- `timeSpawned` becomes a spawn order number that the engine gives each spawn (one counter in `GameEngine`).
- `ScriptedRandom` in the test sources (`O1-R5`: no production caller). The boss tests use it or a fixed seed
  in place of `UtilMethods.random.setSeed`.
- Guard (`D4`): a `core` test that scans `core/src/main` and `engine/src/main` for direct random calls, with the RNG package allowed.

## Out of scope

- `RecordingRandom` (call count and hash): no caller until `055`. Described here only.
- RNG state in save files: not stored (save files hold no simulation state).
- `AsyncPathfindingSystem` wall-clock timeout and apply frame: they belong to the fixed tick (`010`, `O2-R3`). A
  `TODO` at `AsyncPathfindingSystem.kt:66` names `010`.
- `DarknessV2`/`DarknessV4` log timings and the `DarknessV4` thread: not gameplay values; `DarknessV4` is dead code.
- A replay that repeats a run: a seed alone cannot repeat a run until the fixed tick (`010`), because the frame delta
  changes timer and physics results.

## Affected areas and risks

- About 80 files in `core` and 10 in `engine`; most changes are a one-line swap of the call.
- A wrong `min`/`max` in a swap changes a range by 1 (`getRandom(Int, Int)` is inclusive, `MathUtils.random(int, int)`
  is inclusive, Kotlin `nextInt(until)` is exclusive). The new API names the inclusive bound, and unit tests pin the
  ranges.
- The D1000 seed searches depend on the draw order of each fight. If a stream changes the order of draws in a D1000
  test, its seed search must change with it.
- `MegamanWeaponsHandler` is also changed by `024a`: a merge conflict on one line is possible.

## Overlap with other branches

| Branch | Overlap |
|---|---|
| `mega-refactor-024a-weapon-defs` | `MegamanWeaponsHandler.kt:338` (Moon Scythe sort). One line. |
| `mega-refactor-060d-protoman-slide-ladder` | None found; Proto Man code has no random call. |
| `fix/small-bugs-3` | To check at merge. |

## Objective check

| Rule | Effect | Note |
|---|---|---|
| `O1-R1` No global session state | helps | removes the `UtilMethods.random` global (`D1`) |
| `O1-R3` No new fragile order | no effect | |
| `O1-R5` Code only with a production caller | helps | `ScriptedRandom` in test sources; `RecordingRandom` out; 4 helpers with no caller deleted |
| `O2-R2` Seeded random | helps | the rule becomes active with this item |
| `O2-R3` One fixed tick | waiting | wall-clock pathfinding timeout stays (`010`) |
| `O2-R4` Stable iteration order | helps | `OrderedSet.superRandom` reorder removed |
| `O2-R7` Gameplay does not depend on rendering | no effect | one stream (`D2`): a cosmetic draw still shifts later gameplay values |
| `O5-R1` No per-frame allocation | helps | `superRandom` made a `Random` per call |
| `O6-R1` CI passes | helps | guard runs in CI |
| `O6-R2` Named checks | helps | range tests, guard test |

## Decisions (owner, 2026-11-14)

- `D1`: inject through `services.random`; delete `UtilMethods.random` and `getRandom*`.
- `D2`: one stream (not gameplay and cosmetic). A cosmetic draw still shifts the gameplay values after it; this is
  accepted. A split stays possible later (the scan classes each site).
- `D3`: a clock seed per run, written to the log, and `--randSeed <long>` to fix it.
- `D4`: a `core` source-scan test, with the RNG package allowed.

## Open questions

None.
