# Plan: mega-refactor-081c-x-wall-slide-air-dash

Each step has its own check. All checks use Gradle; no game launch.

1. **Test rooms.** A subagent adds rooms `walls` (a 10-tile wall, a 2.5-tile step, a 4-wide chimney, a
   `no_side_touchie` wall, a 2-tile opening 4 tiles up) and `dash` (pits of 3, 4 and 5 tiles, a 2-tile tunnel) to
   `RefactorTest081.tmx`, each with a spawn and painted tiles. Check: the map parses; the level analyzer reads it back.
   - Done: yes
2. **Megaman baseline tests.** Tests on the old `wallSlideBehavior`, `airDashBehavior` and the `aButtonTask` changes
   (start, act, end; water, Rush Jet, claws, `SELECT` as air dash, the end bump, 1 dash per air). Check: they pass on
   the old code; commit before the change.
   - Done: yes
3. **Shared wall slide.** `players/PlayerWallSlide.kt`, `isRunSidePressed` to `players/`, `JumpParams.wallJumpAllowed`.
   Megaman passes his rules. Check: the baseline tests and `MegamanJumpTest` pass unchanged.
   - Done: yes
4. **Shared air dash.** `players/PlayerAirDash.kt` with `startDelay`. Megaman passes his rules. Check: the baseline
   tests pass unchanged.
   - Done: yes
5. **X wall moves.** X6 values; the grab, slide, push-off, kick and kick lock; the body fix (no side-speed reset during
   a kick, a dash or a push-off) and the controller fix (no run input during the slide and the lock); animation keys
   `wallslide`, `walljump` (with `_shoot`). Check: X tests.
   - Done: yes
6. **X air dash and the `DASH` button.** The X6 air dash; 1 per air; starts on `A` in the air, `SELECT` as air dash,
   and `DASH`. `MegaControllerButton.DASH` (index 8, keyboard `U`, pad `R1`), the pad default, the controls screen
   layout, `MegaControllerButtonTest`; the `dash` and `dash_shoot` keys. Check: X tests; button tests.
   - Done: yes
7. **Docs.** `players/README.md`; branch notes.
   - Done: yes
8. **Checks.** The automated checks, mutation checks, the review before the play test. Then the owner's play test.
   - Done: no
