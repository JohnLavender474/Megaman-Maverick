# Process log: mega-refactor-031b-engine-input

## Status

- State: merged
- Goal: input frames as an engine mechanism (`InputButton`, `InputFrame`, `InputSource`); `ControllerSystem` dispatches from each entity's own input source
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `aa97e6c68` plus the notes commit).
- Scan: item `031` `NOTES.md`, "Who drives an entity: driver, slot view and side" and "Long-term shape", plus a read
  of `ControllerSystem`, `ControllerComponent`, `IButtonActuator`, `ButtonActuator`, `ControllerSystemTest`, the
  Megaman controller component, `InputFrame`, `PlayerInputs`, `InputSource`, `MegaPlayer(s)`, `MegamanInput.kt`,
  `MegaMenuScreen` and the frame builder in `MegamanMaverickGame.render`. Draft `docs/SCOPE.md` written.
- The owner answered the four open questions in the draft scope, all as proposed. `docs/SCOPE.md` updated (in scope
  item 6, out of scope, overlap, decisions); `docs/NOTES.md` has the decisions. The scope waits for the owner's
  approval.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written. No new test rooms: `REFACTOR_TEST_029`
  `movement`, `REFACTOR_TEST_031` `spring_bouncer` and `pipe_portal`, and `INTRO_STAGE` `room1` and `room12` (the room
  that holds the boss `Gate`, player spawn `6`).
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (7 steps: branch start, baseline, engine input
  types, the entity's source bound by the slot view, `ControllerSystem` on the source with actuators by button,
  contract checks, play test and merge).
- The owner approved `docs/PLAN.md`. Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree
  created.
- Step 2 (baseline, worktree on `fb4d41d89`): clean build passes; 470 engine and 157 core tests, 0 failures; 114
  warnings.
- Step 3: engine `input/` package: `InputButton` and `checkInputButtons`, `InputFrame` (value class over a `Long`, 2
  bits per button at `index * 2`, `MAX_BUTTONS = 32`, `RELEASED` on all 32 indexes, the four queries, `with`), and
  `InputSource` (moved from core). `MegaControllerButton` implements `InputButton` with `index` equal to the ordinal.
  Core keeps the poller builder as `InputFrame.Companion.of(poller)` in `input/InputFrames.kt` (so the call sites do not
  change) and `PlayerInputs` (now a `LongArray`). `MegamanMaverickGame.create()` calls
  `checkInputButtons(MegaControllerButton.entries)` before the poller is made. Plan detail (no approval needed): the
  frame's constructor and `bits` are public, because core's `PlayerInputs` stores the bits in another module. Tests:
  engine `InputFrameTest` (8: read-back at each index 0 to 31, the default frame, the queries, `with`, and 4 checks of
  the button set); core `MegaControllerButtonTest` (2); core `InputFrameTest` imports the engine frame. Build passes;
  478 engine and 159 core tests, 0 failures. Core `input/` holds only `InputFrames.kt` and `PlayerInputs.kt`. A launch
  of `REFACTOR_TEST_029 --room movement` ran with no exception until the timeout.
- Step 4: engine `ControllerComponent.input` (a nullable `InputSource`; `reset()` stays a no-op, so the binding
  survives Megaman's destroy and respawn). `MegaPlayer`'s `init` requires the entity's `ControllerComponent` and sets
  its `input` to the view's slot source; the `TODO` about `unregister` now also names the input source.
  `Megaman.input` reads `getComponent(ControllerComponent::class)!!.input!!.read()`, with no `asPlayer` (one map lookup
  per read, as `asPlayer`'s property lookup was). `ControllerSystem` still reads the poller. Tests: `MegaPlayersTest`
  gets 2 (the view binds the slot source, and `player.input` gives the same frame; registering an entity without a
  `ControllerComponent` fails). The player test entities in `MegaPlayersTest`, `MegamanStunTargetTest` and
  `PlayerItemRuleTest` get a `ControllerComponent`. Build passes; 478 engine and 161 core tests, 0 failures. A launch
  of `REFACTOR_TEST_029 --room movement` ran with no exception until the timeout.
- Step 5: `IButtonActuator` and `ButtonActuator` lose the `IControllerPoller` parameter. Plan detail (no approval
  needed): `ButtonActuator`'s `Consumer`/`BiConsumer` constructor had no caller, so it is removed, not changed
  (rule "write code only when production code calls it"). `ControllerComponent` is built from
  `GamePair<out InputButton, out IButtonActuator>` pairs and keeps the buttons and the actuators in two arrays sorted by
  `index`; the supplier, the `ObjectMap`, `putActuator` and `removeActuator` are gone. `ControllerSystem` takes no
  parameter: per entity it skips a missing source, reads the frame once (no nullable frame, so no boxing), and calls
  the one callback that matches each button's status, in index order. Megaman's four actuators change only their
  lambda headers (diff checked); the component is built from `MegaControllerButton` and actuator pairs.
  `createGameEngine()` builds `ControllerSystem()`. `ControllerSystemTest` rewritten (4 tests: the matching callback
  only, for each status; no source is skipped; index order with buttons added as 7, 2, 5, 3; one `read()` per update).
  Greps: no `IControllerPoller` in the four dispatch files; no `putActuator` or `removeActuator`. Build passes; 481
  engine and 161 core tests, 0 failures. Launches of `REFACTOR_TEST_029 --room movement` and `INTRO_STAGE --room room1`
  ran with no exception until the timeout.
- Step 6, fix found by the `javap` check: `Megaman.input` (step 4) called `getComponent(ControllerComponent::class)` on
  each read. Without `kotlin-reflect`, each `::class` makes a new `ClassReference` (`Reflection.getOrCreateKotlinClass`),
  so about 44 small objects per frame. `Megaman` now keeps `internal lateinit var controllerComponent`, set in
  `init()` where the component is added, and `Megaman.input` reads `controllerComponent.input!!.read()`.
  `ControllerSystem` still does one `getComponent(ControllerComponent::class)` per entity per update, as on `master`.
- Step 6, automated contract checks (clean build, on the step 5 commit plus the fix): build passes; 481 engine and
  161 core tests, 0 failures (`master`: 470 and 157); 114 warnings (same as `master`); `lwjgl3:jar` builds
  (`Megaman-Maverick-1.0.1.jar`); `contact-rule-selection.txt` unchanged; the engine `InputFrame` tests, the button-set
  tests, `MegaControllerButtonTest`, `ControllerSystemTest`, the core equivalence suite and `MegaPlayersTest` pass;
  greps: no `IControllerPoller` in `ControllerSystem`, `ControllerComponent`, `IButtonActuator`, `ButtonActuator`, no
  `putActuator` or `removeActuator`, no `asPlayer` in `MegamanInput.kt`, core `input/` holds only `InputFrames.kt` and
  `PlayerInputs.kt`; `javap`: no `InputFrame.box-impl` in `ControllerSystem`, `MegaPlayer` and `MegamanInputKt`
  (`MegaPlayer.getInput` and `Megaman.input` call `InputSource.read()` and return a `long`); the slot-0 frame is built
  and written only at `MegamanMaverickGame.kt:535`, right after `controllerPoller.run()`. A launch of
  `REFACTOR_TEST_029 --room movement` ran with no exception until the timeout. Waiting for the owner's play test.
- Step 7, play test: the owner played the manual checks in `docs/CONTRACT.md` and reported "Branch tests pass". The
  owner approved the merge ("Merge into `master`").
