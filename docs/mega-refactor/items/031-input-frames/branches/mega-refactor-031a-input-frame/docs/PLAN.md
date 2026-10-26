# Plan: mega-refactor-031a-input-frame

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-25

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from `build/test-results`.
Smoke launches are short (about 20 seconds); a clean exit, or the timeout with no exception, is a pass. The owner
plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass, and the game plays the same (the
poller stays the only source of input).

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| `InputFrame` (`C/input/`), with `getStatus`, `isPressed`, `isJustPressed`, `isJustReleased`, `isAnyJustReleased`, `areAllPressed` | the builder (step 4); the query methods: the moved readers (steps 5 to 7) |
| Building a frame from the poller | `MegamanMaverickGame.render` (step 4) |
| The per-slot frame store in `MegaPlayers` | the builder (write) and `MegaPlayer.input` (read), step 4 |
| `MegaPlayer.input` | Megaman's accessor (step 5), the special entities (step 6), pause (step 7) |
| Megaman's own-frame accessor (in `M/`) | the Megaman readers (step 5) |

Each query method gets its first caller in steps 5 to 7. If a method has no caller after step 7, it is removed.

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `031a` row in `ITEMS.md` to `active` with the start date,
  and item `031` to `active` with the start date (its first branch); mark `031a` active under "Branches" in
  `ITEM.md`; set the branch `process-log.md` state to `active`. Then
  `git worktree add -b mega-refactor-031a-input-frame .worktrees/mega-refactor-031a-input-frame master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 144 core) and the warning count (expected
  114) in the process log.
- Done: yes

### 3. Test map (subagent)

- Changes: a subagent builds `assets/tiled_maps/tmx/RefactorTest031.tmx` with the `level-builder` skill, and adds
  the `LevelDefinition` entry `REFACTOR_TEST_031` (type `TEST_LEVEL`, the same music, completion screen and
  loadout override as `REFACTOR_TEST_029`). Rooms, each with a player spawn, a floor and painted tiles on every
  block cell:
  - `spring_bouncer`: four `SpringBouncer`s (`direction` `up`, `down`, `left`, `right`), placed so that Mega can
    reach each one; props as in `Test1.tmx`.
  - `pipe_portal`: two linked `PipePortal` pairs as in `SMB3_1.tmx` (`direction`, `this_key`, `next_key`,
    `spawn_type=spawn_now`): an `up` pipe on the floor that leads to a `down` pipe in a lower area, and a way
    back.
  - `ground_slide_blocker`: a `GroundSlideBlocker` (in `blocks`) across a passage, with floor on both sides.
- Check: a short launch of each room (`--level REFACTOR_TEST_031 --room <name>`) on the baseline code shows no
  exception.
- Done: yes

### 4. `InputFrame`, the store and the builder

- Changes:
  - `C/input/InputFrame.kt`: a `@JvmInline value class` over one `Int`, 2 bits per `MegaControllerButton` holding
    its `ButtonStatus`. A default frame with every button `RELEASED`. The query methods with the poller's meaning
    (`isPressed` is `PRESSED` or `JUST_PRESSED`). A function that builds a frame from an `IControllerPoller`'s
    statuses (a missing status counts as `RELEASED`).
  - `MegaPlayers` keeps one frame per slot (the default frame at the start), with a write for the builder.
  - `MegaPlayer.input` reads its slot's frame from the store.
  - `MegamanMaverickGame.render`: right after `controllerPoller.run()`, write slot 0's frame from the poller.
  - Tests: `InputFrame` (each status of each button reads back the same; each query for each status; the default
    frame gives `false`); the equivalence suite (a real `ControllerPoller` with fake `IControllerButton`s and
    seeded scripts with presses, `on` toggles and `enabled` toggles; every query on the frame equals the poller's
    on every tick, for every button; named cases: hold then lock, unlock while held, press and lock in the same
    tick, the disabled-button flip); a player's `input` returns its slot's frame.
- Check: build and tests pass. No reader uses the frame yet, so the game is unchanged; a short launch of
  `REFACTOR_TEST_029 --room movement` shows no exception.
- Done: yes

### 5. Megaman's reads (group a)

- Changes: one accessor in `M/` that gives Megaman's own frame (through his player). Move the 44 reads, one for one:
  the 8 files in `M/behaviors/` (31), `M/components/ControllerComponent.kt` (8, from the actuator's `poller`
  parameter; the dispatch does not change), `M/components/BodyComponent.kt:82`, `M/components/AnimationsComponent.kt:93`,
  `M/sprites/MegamanAnimationKeySupplier.kt:118`, `M/handlers/MegamanWeaponsHandler.kt:674`, `M/Megaman.kt:833`.
  `JetpackBehavior`'s `allMatch(UP == PRESSED, A == JUST_PRESSED)` becomes `getStatus(UP) == PRESSED &&
  getStatus(A) == JUST_PRESSED`. `game.selectButtonAction` reads do not change.
- Check: build and tests pass. A script lists every changed read (file, method, button) before and after, and they
  match one for one. No `controllerPoller` and no `poller.` button read is left under `M/`. A short launch of
  `REFACTOR_TEST_029 --room movement` shows no exception.
- Done: yes

### 6. The special entities (group b)

- Changes: `PipePortal.kt:154-160` and `SpringBouncer.kt:209-216` read the contact player's frame (the `asPlayer`
  they already resolve). `SpiderWeb.kt:185` and `GroundSlideBlocker.kt:43-44` read `targetPlayer.input`.
- Check: build and tests pass; the one-for-one list matches; no `controllerPoller` under `C/entities/`,
  `C/world/` or `C/players/`. Short launches of `REFACTOR_TEST_031` `spring_bouncer`, `pipe_portal` and
  `ground_slide_blocker` show no exception.
- Done: yes

### 7. Pause (group c)

- Changes: `MegaLevelScreen.isPauseButtonRequested` (`:908-909`) reads the frame of the `player` it already holds,
  with the same methods and buttons.
- Check: build and tests pass; the one-for-one list matches. A grep shows that the remaining `controllerPoller`
  readers are only the frame builder, `ControllerSystem` and `onEvent` in `MegamanMaverickGame`, the menus and
  screens, the `controllers` package and the dead `ScreenController`. Every `InputFrame` query method has a
  caller. A short launch of `INTRO_STAGE --room room1` shows no exception.
- Done: yes

### 8. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build (tests, jar, warnings, the contact file, the
  greps, the one-for-one list, the single build point). Write the results in the process log. Then give the owner
  the manual checks as a list of commands, each with `--level` and `--room`, run from the worktree.
- Done: yes
