# Plan: mega-refactor-031b-engine-input

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-26

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from `build/test-results`.
Smoke launches are short (about 20 seconds); a clean exit, or the timeout with no exception, is a pass. The owner
plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass, and the game plays the same (the
poller stays the only source of input). The actuator order changes only in step 5.

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`M/` = `C/entities/megaman/`.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| `InputButton` (`E/input/`) | `MegaControllerButton` (step 3); the `InputFrame` queries |
| Engine `InputFrame` (`Long`), its four queries and `RELEASED` | the existing readers (step 3, imports only), `ControllerSystem` (step 5) |
| `InputFrame.with(button, status)` (a new frame with one status changed) | the slot-0 builder in core (step 3) |
| The button-set check | `MegamanMaverickGame` at startup (step 3) |
| `InputSource` (moves to `E/input/`) | `MegaPlayer`, `ControllerComponent` |
| `ControllerComponent.input` | `MegaPlayer` binds it (step 4); `ControllerSystem` reads it (step 5) |
| Actuators by button on `ControllerComponent` | `Megaman.defineControllerComponent` (step 5) |

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `031b` row in `ITEMS.md` to `active` with the start date;
  list `031b` as active under "Branches" in `ITEM.md`; set the branch `process-log.md` state to `active`. Then
  `git worktree add -b mega-refactor-031b-engine-input .worktrees/mega-refactor-031b-engine-input master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 157 core) and the warning count in the process
  log.
- Done: no

### 3. Engine input types; core moves to them

- Changes:
  - `E/input/InputButton.kt`: `interface InputButton { val index: Int }`.
  - `E/input/InputFrame.kt`: a `@JvmInline value class` over a `Long`, 2 bits per button at `index * 2`, with `Long`
    shifts and masks. `RELEASED` (every index from 0 to 31 set to `RELEASED`; `RELEASED` is not ordinal 0 in
    `ButtonStatus`). `getStatus`, `isPressed`, `isJustPressed`, `isJustReleased`, taking `InputButton`, with
    today's meaning. `with(button, status)` returns a new frame (no allocation). A constant for the limit (32).
  - `E/input/InputSource.kt`: moved from core, unchanged.
  - `E/input/`: the button-set check (each `index` unique and in `0 until 32`; an `IllegalArgumentException` that
    names the bad button).
  - `C/controllers/MegaControllerButton.kt`: implements `InputButton`, with an explicit `index` equal to the
    `ordinal`.
  - `C/input/InputFrame.kt` and `C/input/InputSource.kt` go. The slot-0 builder (from the poller, a missing status
    counts as `RELEASED`) stays in core, built with `RELEASED` and `with` over `MegaControllerButton.entries`.
    `PlayerInputs` stores `Long` frames (`LongArray`).
  - `MegamanMaverickGame`: the button-set check on `MegaControllerButton.entries`, once at startup.
  - The readers change their imports only.
  - Tests: engine `InputFrameTest` (read-back at each index from 0 to 31 with the others set otherwise; the default
    frame; the queries; `with`); engine tests of the button-set check (duplicate, `-1`, `32`, a valid set); core:
    `MegaControllerButton` indexes equal the ordinals and pass the check; the core equivalence suite moves to the
    core builder (same scripts and named cases).
- Check: build and tests pass. A grep shows no `InputFrame` or `InputSource` left in `C/input/`. A short launch of
  `REFACTOR_TEST_029 --room movement` shows no exception.
- Done: no

### 4. The entity's source, bound by the slot view

- Changes:
  - `E/controller/ControllerComponent.kt`: a nullable `input: InputSource` (empty means "no driver"). The actuators
    do not change yet.
  - `C/players/MegaPlayer.kt`: in `init`, set the entity's `ControllerComponent.input` to the view's slot source.
    `MegaPlayer.input` reads that same source.
  - `M/MegamanInput.kt`: `Megaman.input` reads the entity's own source (`ControllerComponent.input`), not
    `asPlayer`.
  - `ControllerSystem` still reads the poller.
  - Tests: `MegaPlayersTest`: building the view sets the entity's source; `MegaPlayer.input` and the entity's source
    give the same frame.
- Check: build and tests pass. `MegamanInput.kt` has no `asPlayer`. A short launch of
  `REFACTOR_TEST_029 --room movement` shows no exception.
- Done: no

### 5. `ControllerSystem` on the entity's source; actuators by button

- Changes:
  - `E/controller/buttons/IButtonActuator.kt` and `ButtonActuator.kt`: the callbacks lose the `IControllerPoller`
    parameter (`onJustPressed()`, `onPressContinued(delta)`, `onJustReleased()`, `onReleaseContinued(delta)`). The
    `Consumer`/`BiConsumer` constructor changes to match.
  - `E/controller/ControllerComponent.kt`: built from pairs of `InputButton` and `IButtonActuator` (no supplier).
    It keeps the buttons and the actuators in two arrays sorted by `index`. `putActuator` and `removeActuator` go.
  - `E/controller/ControllerSystem.kt`: no constructor parameter. For each entity with a source: read the frame
    once, then for each button in index order, call the one callback that matches the status, as today.
  - `M/components/ControllerComponent.kt`: the four actuators lose the `_` parameter; the component is built from
    `MegaControllerButton` and actuator pairs.
  - `MegamanMaverickGame.createGameEngine()`: `ControllerSystem()`.
  - Tests: `ControllerSystemTest` rewritten on a fake source (each status calls the matching callback only, with
    `delta` for the two "continued" ones; no source means skipped; index order with buttons added out of order;
    one `read()` per entity per update).
- Check: build and tests pass. A grep shows no `IControllerPoller` in `ControllerSystem`, `ControllerComponent`,
  `IButtonActuator` and `ButtonActuator`, and no `putActuator` or `removeActuator`. A review of the diff shows that
  the four actuator bodies are the same. A short launch of `REFACTOR_TEST_029 --room movement` and
  `INTRO_STAGE --room room1` shows no exception.
- Done: no

### 6. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build (tests, jar, warnings, the contact file, the
  greps, `javap` for boxing, the single build point). Write the results in the process log. Then give the owner the
  manual checks as a list of commands, each with `--level` and `--room`, run from the worktree.
- Done: no

### 7. Owner's play test and merge

- Changes: none, unless the play test finds a problem.
- Check: the owner plays the manual checks in `docs/CONTRACT.md` and approves the merge. Then follow "Branch merge"
  in `docs/mega-refactor/CLAUDE.md`.
- Done: no
