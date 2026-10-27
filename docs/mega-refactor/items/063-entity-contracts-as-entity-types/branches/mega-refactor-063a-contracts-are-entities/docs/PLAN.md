# Plan: mega-refactor-063a-contracts-are-entities

- Status: approved
- Approved: 2026-10-27

New API: none. The branch adds supertypes and a setter to existing types and removes code. The public setter of
`existenceState` has its first callers in `GameEngine` (step 3).

Temporary test code: `Temp063aExplodeAndDieTest` (core test sources) with its frozen copy of the old destroy step. It is
deleted in step 8.

JDK: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13` (the default JDK 25 does not run Gradle 8.10.1).

## Steps

1. **Baseline.** On the branch before any code change: `./gradlew clean engine:test core:test lwjgl3:jar`. Write the
   test counts in the process log and save the unique warning lines (`^w:`, without line and column) to the
   scratchpad.
   - Check: the build passes; the counts and the warning file exist.

2. **Temporary suite.** Add `Temp063aExplodeAndDieTest`: a frozen copy of the old step (`game.engine.destroy(this)`)
   against `destroy()` on two `Cokonut` instances in each of the 5 existence states; equal return values, and equal
   `existenceState` after the call and after one `engine.update()`. It runs on the old code here and stays green
   through steps 3 to 7.
   - Check: `core:test` passes, including the new suite.

3. **Engine.** `IGameEntity : ..., IDestroyable`; `existenceState` becomes `var` on `IGameEntity`; `GameEntity` loses the
   `internal set` and the redundant `IDestroyable`; delete `GameEngine.setExistenceState` and write
   `entity.existenceState = ...` at its call sites; `IDamageable : IGameEntity`, `IDamager : IGameEntity`. Add the
   permanent engine tests: an `IGameEntity` that is not a `GameEntity` through spawn, destroy, respawn and `reset()`
   (same states as a `GameEntity`), and `destroy()` through an `IGameEntity` reference (the `062b` results and a
   subclass override).
   - Check: `engine:test core:test` pass; no `setExistenceState` in `engine/`.

4. **Core contracts.** `: IMegaGameEntity` on `IFreezableEntity`, `IFreezerEntity`, `IFireEntity`, `IFireableEntity`,
   `IScalableGravityEntity`, `ILightSource`, `IOwnable`, `ITeleporterEntity`, `ItemEntity`, `IGateTrigger`;
   `IBossListener` changes `: IDestroyable` to `: IMegaGameEntity`; delete `IUpsideDownable`. Add `EntityContractsTest`.
   - Check: `engine:test core:test` pass; no `IUpsideDownable`.

5. **Casts that need the new types** (9): `MegamanMaverickGame.kt:900`, `RotationAnchor.kt:102`, `Togglee.kt:193`,
   `:215`, `IProjectileEntity.kt:69-70` (to `destroy()`, comment removed), `FreezableEntityHandler.kt:53`, `:93`,
   `ShieldEntity.kt:68` (null check kept), `LightSourceUtils.kt:43` with the parameter at `:51`. Remove imports that
   become unused.
   - Check: `engine:test core:test` pass, including the temporary suite.

6. **Redundant casts** (22, listed in `SCAN.md` 3c): the 11 `destroy()` casts on typed `children`,
   `TimberWoman.kt:931`, `PetitDevil.kt:147`, and the 9 iterator casts. Remove imports that become unused.
   - Check: `engine:test core:test` pass; no `as GameEntity).destroy()` or `as MegaGameEntity).destroy()` in `core/`.

7. **Docs.** Root `CLAUDE.md`, the gotcha about `existenceState`: only the engine should write it; the setter is public.
   - Check: the line reads correctly.

8. **Contract checks.** Clean build, all tests, warnings compared with step 1 (each new warning listed), the greps and
   the review items in `CONTRACT.md`. Then ask for the owner's play test with the manual checks. After the play test,
   delete `Temp063aExplodeAndDieTest` in its own commit and run the automated checks again.
   - Check: every box in `CONTRACT.md` ticked and logged.
