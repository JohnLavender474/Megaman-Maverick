# Plan: mega-refactor-081b-x-run-jump-shoot

Each step has its own check. All checks use Gradle; no game launch.

1. **Test map `REFACTOR_TEST_081`.** A subagent builds `RefactorTest081.tmx` with the `level-builder` skill: rooms
   `flat`, `slope` (a `GroundSlope`) and `targets` (breakable or damageable targets for the buster), with painted
   tiles and a player spawn in each room. Add the `LevelDefinition` entry `REFACTOR_TEST_081` (`TEST_LEVEL`,
   `characters = setOf(X)` comes in step 2). Check: `LevelCompletionScreensTest` updated and passes; the
   `level-analyzer` reads the map back.
   - Done: yes
2. **Characters and the swap.** The character enum, `LevelDefinition.characters`, `X6_INTRO` and `REFACTOR_TEST_081`
   set X. The roster in the game; the swap in `startLevel`; the fail-fast checks; `resetPlayerProgress` through the
   roster; `setFocus` at the top of `MegaLevelScreen.start()`. X is a stub class in this step only if the swap test
   needs one; else this step follows step 3. Check: swap tests and `resetPlayerProgress` test.
   - Done: yes
3. **`MegamanX` class.** `TextureAsset.MEGAMAN_X`, `MegamanXValues`, body (X6 box, one gravity, fall limit, no air
   drag), sprites and animations (stand, run start, run, rise, fall, land, the shoot versions, damaged, death), the
   `081a` shared functions, `PlayPhaseComponent` and the sprite hide rule, max health from the game state (`TODO`).
   Check: `core:smokeTest` (region names), `EntityConstructorsTest`, `PlayerRulesTest`-style tests on X.
   - Done: yes
4. **Run.** Walk start, then walk, from the controller. Check: speed tests with fixed input frames.
   - Done: yes
5. **Jump.** A jump-height test on Megaman first, on the old code. Then `jumpBehavior` with a generic receiver and
   parameters; Megaman's behaviors pass Megaman's rules; X's behaviors pass X's. Check: the Megaman jump test gives the
   same result as before; `MegamanBehaviorsTest`; X jump speed, release cut and fall limit tests.
   - Done: yes
6. **Buster.** `WeaponChargeComponent` with the X6 times, `Bullet` and `ChargedShot` spawns, shot limits, the tint
   flash (D2). Check: buster tests.
   - Done: yes
7. **Ladder top (D1).** X passes a `COLLIDE_DOWN_ONLY` top while `DOWN` is held, with a `TODO`. Check: a collision
   test for X with and without `DOWN`; Megaman's ladder rule is unchanged.
   - Done: yes
8. **Screen casts.** `PlayerStatsHandler.kt:106,115`, `MegaLevelScreen.kt:564`, `EndLevelEventHandler.kt:173`,
   `LevelPauseScreen.kt:286,569`: `is Megaman` workarounds with `TODO`s for `024`. Check: tests that run each site
   with X in slot 0 where `testServices()` can reach it.
   - Done: yes
9. **Docs and README.** `players/README.md` names X as the second caller. A README for the new X package if its
   design is not clear from one file. Branch notes for the `024` and `029` workarounds (for the merge).
   - Done: yes
10. **Checks.** The contract automated checks, the mutation checks, then the review before the play test (subagents;
   every manual check). Fix the findings. Then the owner's play test.
   - Done: yes
