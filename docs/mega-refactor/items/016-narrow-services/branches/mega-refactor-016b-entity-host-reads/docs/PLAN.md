# Plan: mega-refactor-016b-entity-host-reads

- Status: approved
- Approved: 2026-10-28 (owner, "approved to all")

How to do the work in `SCOPE.md` (approved 2026-10-28) so that the checks in `CONTRACT.md` (approved 2026-10-28)
pass. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

Rules for every step:

- One commit per step. Each commit builds and passes all tests.
- The build command is `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
  lwjgl3:jar`. "Build green" below means: it passes, engine 575 tests (after the merge `7291362c8`), core 225 plus the new tests, 0 failures, no
  new unique `^w:` line against the 81 of the baseline.
- Each changed site keeps its place in the code and its expression shape (a read stays a read at the same line, a
  write stays a write). No new reset, no reordering.
- No code comments. Small edits with the Edit tool; no script (no change repeats more than a few times per file).
- Narrow edits: no change to `Bat.kt`, `SubmarineJoe.kt`, `MegaPathfinderResultConsumer.kt`,
  `AbstractPathfindingSystem`, or to `createGameEngine()` outside the 3 drawables lines. A planned `fix/` branch from
  `master` changes those parts (`docs/NOTES.md`, "Overlap with other branches").
- No test map: the manual checks use real rooms (`SCOPE.md` open question 9).

Checked during planning (2026-10-28, by reading the code; no build):

- `SpritesComponent.preProcess(delta)` (engine) runs the pre-process functions of an entity, so a test can run the
  `Starkner` sprite `preProcess` with no `SpritesSystem`. `GameSprite` builds in a plain unit test
  (`StarknerAliveTest` already builds a `Starkner` with its sprites).
- `DrWily.update(delta)` is public (`Updatable`), so a test can drive it with no engine systems. Its private `state`
  and `position` are read through reflection in the temporary suite only.
- `Starkner` at spawn has `rawRotation = 0f` and `moving = false`. With a body direction that is not `UP`, the
  unpaused `preProcess` sets the sprite rotation to `direction.rotation` (not `0`), and the paused `preProcess` keeps
  `0`. So the test can see the pause gate with no need to make the `Starkner` move.

## Steps

### 1. `GameClock` and the pause flag

- Changes:
  - New `C/state/GameClock.kt`: `class GameClock { var paused = false }`.
  - `MMG`: `val clock = GameClock()` next to `levelState`; remove `var paused ... private set`. In `pause()` and
    `resume()` the early-return checks and the writes use `clock.paused` at the same lines; the log line and the
    screen call stay. `setCurrentScreen` reads `clock.paused`.
  - Every other `game.paused` read becomes `game.clock.paused`: `Starkner.kt:414`; `MLS:643`, `:930`, `:936`, `:940`,
    `:966`, `:1005`, `:1058`; `RobotMasterIntroScreen:226`, `RobotMasterIntroScreenV2:240`,
    `temp_BossIntroScreen:143`, `MainMenuScreen:249`, `LevelSelectScreen:379`, `LevelSelectScreenV2:245`;
    `DesktopLauncher.kt:165`, `:169`, `:173`.
  - Permanent tests: `T/state/GameClockTest.kt`: `GameClock().paused` is `false`; on a real `MegamanMaverickGame()`
    (no current screen): `clock.paused` is `false`, `pause()` sets it, a second `pause()` keeps it, `resume()` clears
    it, a second `resume()` keeps it cleared. `T/entities/enemies/StarknerPausedTest.kt` (same setup as
    `StarknerAliveTest`, with a real `GameClock` stubbed on `game.clock`): spawn a `Starkner`, set its body direction
    to a value that is not `UP`; with `clock.paused = true` the `preProcess` leaves the sprite rotation at `0`; with
    `clock.paused = false` it sets the rotation to `direction.rotation`.
- New API and first caller: `GameClock`, `GameClock.paused`, `MMG.clock` (`MMG.pause`, `Starkner`).
- Check: build green. `MMG` has no `paused` member (the build proves that no caller is left);
  `grep -rnE "game\.paused\b" core/src lwjgl3/src` finds 0 lines.
- Done: yes (2026-10-28)

### 2. `selectButtonAction` in `MegaPlayers`

- Changes:
  - `C/players/MegaPlayers.kt`: `var selectButtonAction = SelectButtonAction.NEXT_WEAPON`.
  - `MMG`: remove `lateinit var selectButtonAction` (`:216`) and its write in `create()` (`:305`).
  - Every caller uses `game.players.selectButtonAction`: `ControllerComponent.kt:99`, `AirDashBehavior.kt:33`, `:38`,
    `ControllerSettingsScreen.kt:220`, `:248`, `:319`, `:341`, `:342`, `:347`, `:348`, `MLS:918`.
  - Permanent test: extend `T/players/MegaPlayersTest.kt`: `selectButtonAction` starts `NEXT_WEAPON` and keeps a value
    that is set.
- New API and first caller: `MegaPlayers.selectButtonAction` (`ControllerComponent.select`).
- Check: build green. `grep -rnE "game\.selectButtonAction\b" core/src lwjgl3/src` finds 0 lines; `MMG` has no
  `selectButtonAction`.
- Done: yes (2026-10-28)

### 3. Temporary `DrWily` suite on the old code

- Changes (test sources only):
  - `T/entities/special/Temp016bFrozenDrWily.kt`: a copy of `DrWily` from the base `07307f156`, renamed, with its
    `game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name)` call. Its own `TAG` value is the same `"DrWily"`, so it
    loads the same atlas regions.
  - `T/entities/special/Temp016bDrWilyEquivalenceTest.kt`: two relaxed game mocks (one per side) with a real
    `GameEngine` and `GameState`, an atlas stub, and a relaxed `EventsManager` mock per side whose `submitEvent` calls
    are captured, as in `StarknerAliveTest`; one shared target player mock with a body and an `existenceState` whose
    `liveAfterUpdate` the test sets. The old side records `game.setCurrentScreen(...)` calls. The new side records
    both `game.setCurrentScreen(...)` calls and submitted events with the key `GAME_COMPLETE` (looked up by name, so
    the test compiles before the key exists), so the suite passes on the old code and keeps its meaning after step 4.
    Seeded sequences (fixed seeds, for example 200 sequences of up to 2000 updates): random spawn position, target,
    `maxY` and facing; random deltas from `0.001` to `0.05`; the target alive, or dead from a random update on. After
    each update: the same `state` and `position` (reflection), the same `facing`, and the same list of requests
    (update index); each old request names `ENDING_SCREEN`. The suite also asserts that many sequences reach a request
    (target alive at the end of the still timer) and some do not (target dead), so it cannot pass by never reaching
    `LAND`.
- Check: build green on the old production code (core 225 plus the tests of steps 1 and 2 plus this suite).
- Done: yes (2026-10-28)

### 4. The game ending as an event, and the dead `endGame` flag

- Changes:
  - `C/events/EventType.kt`: new key `GAME_COMPLETE` after `END_LEVEL`.
  - `DrWily.kt:152`: `game.eventsMan.submitEvent(Event(EventType.GAME_COMPLETE))` in place of the
    `setCurrentScreen` call, same condition; remove the `ScreenEnum` import if it has no other use.
  - `MLS`: `EventType.GAME_COMPLETE` in `eventKeyMask` after `END_LEVEL`; in `onEvent`, after the `END_LEVEL` branch, a
    `GAME_COMPLETE` branch with a debug log (same form as its neighbours) and
    `game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name)`.
  - `MLS`, `VICTORY_EVENT` branch: remove the `endGame` read; `endLevelEventHandler.init()` with no condition.
  - (Done note) The `VICTORY_EVENT` unit case is dropped: `MLS.init()` needs LibGDX in a unit test.
  - Permanent tests: new `T/entities/special/DrWilyTest.kt`: drive one `DrWily` from spawn to the end of the still
    timer; `GAME_COMPLETE` is submitted once, at the update where the still timer finishes; with the target dead at
    that update it is never submitted. New `T/screens/levels/MegaLevelScreenEventsTest.kt` (a `MegaLevelScreen` built
    with a relaxed game mock; checked practical on 2026-10-28 with a scratch test, deleted): the mask contains
    `GAME_COMPLETE`; `onEvent(GAME_COMPLETE)` calls `game.setCurrentScreen(ENDING_SCREEN.name, any())` once;
    `onEvent(VICTORY_EVENT)` with and without `ConstKeys.END = true` gives the same result (the end-level handler
    starts). The `VICTORY_EVENT` case needs `MLS.init` (it builds the end-level handler, fonts and cameras); if
    `MLS.init` cannot run in a unit test (no LibGDX context), this case is dropped, and the play check 7 and the review
    of the 2-line change cover it. The process log says which.
- New API and first caller: `EventType.GAME_COMPLETE` (`DrWily.update` submits it; `MLS.onEvent` handles it).
- Check: build green; the temporary suite passes unchanged.
  `grep -rn "setCurrentScreen" C/entities` and `grep -n "endGame" MLS` find 0 lines.
- Done: yes (2026-10-28)

### 5. Drawables map type

- Changes: `ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>` in the field
  `MMG:194`, the local map in `createGameEngine` (`MMG:881`; the next 2 lines do not change), an explicit return type
  on `getDrawables()` (`MMG:991`), and the field `MLS:195` (new import of `InsertionOrderPriorityQueue`). Remove the
  `java.util.*` import of `MLS` only if the build shows no other use of it.
- Check: build green. `grep -n "java.util.Queue" C/MegamanMaverickGame.kt` finds 0 lines; no cast (`as
  InsertionOrderPriorityQueue`) in `core/src`.
- Deviation (2026-10-28): `InsertionOrderPriorityQueue.poll()` is declared `T?` in Kotlin, where `java.util.Queue.poll()`
  gave a platform type. The 3 draw loops in `MLS` now use `poll()!!`. Each loop polls only after `!isEmpty()`, so the
  value is never `null`; if it were, both the old and the new code throw a `NullPointerException` at the same line.
- Done: yes (2026-10-28)

### 6. Root `CLAUDE.md`

- Changes: "Game object": add `clock: GameClock` (the pause flag `clock.paused`; `pause()` and `resume()` change it);
  `selectButtonAction` lives in `players` (`MegaPlayers`). In "Events" (or "Screens"): the game ending is the event
  `GAME_COMPLETE`; `DrWily` submits it and the level screen switches to `ENDING_SCREEN`, as it does for `END_LEVEL`.
- Check: the lines name only members that exist.
- Done: yes (2026-10-28)

### 7. Automated contract checks and smoke launches

- Run every automated check in `CONTRACT.md`. Smoke launches (about 15 to 20 seconds each, with `timeout 20`):
  `./run-game-alpha.sh --level MOON_MAN --room room12` and `./run-game-alpha.sh --level INTRO_STAGE --room room1`;
  a clean exit or the timeout with no exception in the log is a pass.
- Write the results in `CONTRACT.md` "Results" and the process log. Then STOP and report the manual checks (each as a
  `cd` to the absolute worktree path, then the command) for the owner's play test.
- Done: yes (2026-10-28)

### 8. After the play test

- Delete `Temp016bDrWilyEquivalenceTest` and `Temp016bFrozenDrWily` in their own commit.
- Run the automated checks again (core: 225 plus the permanent tests); `grep -rn "Temp016b" core/src` finds 0 lines.
- Update `CONTRACT.md` "Results", the status block and the process log. STOP and report.
- Done: yes (2026-10-29)
