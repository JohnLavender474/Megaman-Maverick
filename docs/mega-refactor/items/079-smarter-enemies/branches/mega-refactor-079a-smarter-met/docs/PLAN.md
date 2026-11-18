# Plan: mega-refactor-079a-smarter-met

Steps 1, 2 and 3 are independent and run in parallel (subagents). Only one agent at a time runs Gradle
(`feedback_gradle_memory_pressure`). No step launches the game.

1. **Test map.** `assets/tiled_maps/tmx/RefactorTest079.tmx` with the `level-builder` skill, the `REFACTOR_TEST_079`
   entry in `LevelDefinition` (and `LevelCompletionScreensTest` data), rooms `pit_edge`, `safe_drop`, `gap_jump`,
   `step_jump`, `platform_jump`, `charged_stagger`, `helmet_break`, `turned_met`, `frozen`, painted `tiles`.
   Check: a script lists each room with a player spawn, the `Met`s and the `Death` areas in it.
2. **Art drafts** in `temp/079a-met/`: no-helmet run and shoot, helmet popped up (small, large), 3-4 helmet pieces,
   jump; `SnowMet` palette copies; a contact sheet next to the old frames. Check: the owner approves the sheet.
3. **`GroundProbe` and `JumpPlanner`** in `entities/utils/`, with unit tests (local frame for 4 directions; solid,
   one-way and `DEATH` cases; arc clear, blocked, unsafe landing). Check: `core:test` for the new specs.
4. **`Met` states** on `EnumStateMachineBuilder`; pit avoidance and safe drop with `GroundProbe`; jumps with
   `JumpPlanner` and the jump rules. `run_only` keeps the old path. Uses existing regions until step 7. Check:
   `core:test`, `core:smokeTest`.
5. **Charged hits:** `HIT_BY_PROJECTILE` on the shield, the `ChargedShot` path on the body, stagger (half, full),
   helmet hit count reset in `onSpawn`, helmet break with `FallingPiece`, desperate run and pause. Check: compile.
6. **Hard mode:** seek (face the player before a run, longer run, jump toward the player's height) and dodge (1 in 3
   from `services.random`; helmet down or a safe hop). Check: `detektMain core:verifyRandomRule`.
7. **Pack the art** into `Enemies1` (`Basic` only), wire the new regions, `FallingPiece.load(..., ENEMIES_1)`,
   README credits. Check: region pixel-compare script, `core:smokeTest`.
8. Owner play-test rounds (`docs/ways-of-working/gameplay.md`): no Gradle, no new tests per round; spec lines in
   `docs/TEST-DEBT.md`.
9. **Final pass** when the owner calls the `Met` final: `Met` reaction tests with `testServices()`, mutation checks,
   the full automated checks, the review before the play test, Diffity.
