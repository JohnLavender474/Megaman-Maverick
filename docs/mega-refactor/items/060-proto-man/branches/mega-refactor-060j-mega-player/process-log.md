# Process log: mega-refactor-060j-mega-player

## Status

- State: active
- Goal: Refactor only: an abstract `MegaPlayer` base class that owns the shared player wiring and is the receiver of the shared `players/` functions; `Megaman` and X move onto it and play the same
- Done: planning; brief approved; steps 1 to 6 (with owner scope changes A, B, C)
- Brief: https://claude.ai/artifact/JEMmaKF3ihS4fgGABcNbeq
- Next: owner play test and Diffity review
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-10

- Branch start. Owner decision after the `IPlayer` review (`temp/060-iplayer-review/report.md`): a `MegaPlayer`
  base class, built in a refactor-only branch before `060b` goes on. `060b` pauses after its planning scan.
- Planning scan (1 read-only agent; `temp/mega-refactor-060j-mega-player/scan.md`), with the owner input that the
  base class must fit future characters (Bass, Axl, Zero). Draft `SCOPE.md`, `CONTRACT.md` and the brief.
- Brief approved (2026-11-10): D1 every class sets the heart-tank max health at spawn; D2 `MegamanValues` with a
  concise `TODO`; D3 move the part bodies into the classes (confirmed after the size check: `Megaman.kt` about 700
  lines, `MegamanX.kt` about 550); D4 no component-order test. `SCOPE.md` and `CONTRACT.md` approved. `PLAN.md` written.

## 2026-11-10 (steps 1 to 6)

Implementation by a subagent. Commits in this order (step 5 went before step 4, so that each commit compiles: the
step 4 receivers need the test doubles on `MegaPlayer`).

- Steps 1 to 3 (`97ae6a699`): `players/MegaPlayer.kt`; `Megaman` and `MegamanX` extend it. D3: the bodies of the
  `define...Component()` extensions move into the classes as the overrides of the `internal` abstract members; the
  emptied files go (Megaman: `BehaviorsComponent.kt`, `CameraSubjectComponent.kt`, `DamageReactionComponents.kt`,
  `MovementComponents.kt`, `PlayerProgressComponent.kt`; X: `MegamanXBody.kt`). Private helpers that the moved bodies
  call become `internal`. D1: `setMaxHealthFromHeartTanks()` first in the final `onSpawn`. D2: one `TODO` at the
  `MegamanValues` reads.
  - Line moves (scan Q2): Megaman `damageTimer.setToEnd()` (before the class lines; `AbstractHealthEntity.onSpawn`
    already ends the timer, and no class line reads it); Megaman `airDashConsumed = false` (before the momentum reset,
    which does not read it); X `sabre.reset()` (after the shared destroy, which only removes the listener and queues
    an event).
  - Component order (scan Q7): Megaman sand, gravity table, weapon charge, bubble, trail go after the shared
    components (no `reset()`, or a `reset()` of its own timer only); X weapon charge goes last (its `reset()` resets
    only its timer; the scan listed this move only for Megaman, the same proof holds); X `Audio` and `Animations`
    swap (each resets only its own data). Every shared component keeps its relative order.
  - Logs only: `MegaPlayer` logs with the class name; the `healthDiff` debug log now also runs for X.
- Step 5 (`e7311ea9c`): `TestMegaPlayer`; `TestHealthPlayer` and `RunPlayer` extend it. The scan said that the doubles
  never call `init()`; `KoopaShellKickerTest` and `CartV2DeadPlayerTest` spawn the double through a real
  `GameEngine`. They use the old double, unchanged, as `TestBodyPlayer` (test only).
- Step 4, scope change A (`1e1a732ae`): the 7 lifecycle functions are private members of `MegaPlayer` (MegaPlayer was
  the only production caller of each). Tests drive `onSpawn`, `onDestroy`, `onEvent`, `invincible` and
  `takeDamageFrom`. The case "throws for an event that is not a room transition" cannot be reached through `onEvent`;
  it became "leaves the body as it is". The obsolete `TODO` on the `playerInvincible` receiver went.
- Step 4, scope change B (`24d0ee7a4`): `entities/movement/Run.kt` (`RunParams`, `runStep`, `maxRunSpeed`) and
  `playerRunButton(player, side, params, blocked, onBlocked)`. New `RunTest` (5 cases); `PlayerRunTest` keeps its 4.
- Step 4, scope change C (`a6c7e1ea8`): `playerBody`, the 3 move behaviors and the 2 air-dash inputs take
  `player: MegaPlayer`, each with a `TODO` for item `121`. The 2 `playerSlot!!` reads are `slot` (item `148`).
  `playerCanMove` and `killPlayerIfOutsideLevel` take a `MegaPlayer` receiver.
- Step 6: `players/README.md` (base-class rule, parts diagram, spawn order, the 3 new rules), `docs/NOTES.md`
  (decisions, "Fit for future characters").
- Tests: after steps 1 to 3, `core:test` 1332 and `core:smokeTest` 451 (the 1783 of `master`), all pass. After each
  later step, `core:test` passes (1332, then 1337 with `RunTest`). No game launch.
- Final check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`: `engine:test` 675, `core:test` 1337,
  `core:smokeTest` 451, all pass (1788 core tests in total, 5 more than the 1783 of `master`); the JAR builds.
- Step 7, review before the play test (2 read-only agents; reports in `temp/mega-refactor-060j-mega-player/`):
  - Diff (`review-diff.md`): no behavior difference outside the allowed list; each allowed difference holds (the 3
    line moves, D1, the component order moves including X's `WeaponCharge`, log tags). 2 low findings: the stale
    `TODO` at `MegamanMaverickGame.kt:451` (late join keeps the base max health; D1 fixed it) is removed; the
    unreachable `else -> throw` in `MegaPlayer`'s room-transition function stays as a fail-fast guard.
  - Play checks (`review-checks.md`): all 13 commands valid; each room holds the setup of its check.
  - Test check (main session): 3 breaks in `runStep` (ice, ramp, gravity sign) fail `RunTest`, `PlayerRunTest` and
    `MegamanRunTest`; restored.
