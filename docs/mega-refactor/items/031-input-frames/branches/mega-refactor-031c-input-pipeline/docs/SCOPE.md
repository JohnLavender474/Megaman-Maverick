# Scope: mega-refactor-031c-input-pipeline

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Replace the poller with an input pipeline that gives the same frames. Each device adapter (keyboard, gamepad 0) gives
a held mask: one bit per button. Slot 0 ORs the masks. An engine edge builder turns the held mask into the slot's
`InputFrame`, with the four statuses and today's lock sequence. The poller, the device reads in the engine and the
per-button `enabled` flag go. The game plays the same.

After the branch, the engine reads no input device, and the later branches change only the adapters (`031d`: aliases,
every gamepad, stick, triggers) and the code storage (`031e`: bindings).

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes:

- From "Proposed scope": "Sources (keyboard, pad N, touch, replay, AI)", for the keyboard and gamepad 0 only, and "A
  control lock per player with today's release sequence", for slot 0.
- From `NOTES.md`, "Place in the frame pipeline": device raw state → held mask per device → OR per slot → lock →
  edges → `InputFrame`. Edges are computed after the OR, as today.
- From `NOTES.md`, "Long-term shape", design point 1: one edge machine for all sources, which holds the lock.
- Decisions of 2026-10-26 in `ITEM.md`: the split (`031c` parity, `031d` devices, `031e` bindings) and the five
  choices for `031c` (see "Decisions").

## Current state

Based on `master` at `1df22a05c`. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- **Device read** (engine): `ControllerButton.isPressed()` (`E/controller/buttons/ControllerButton.kt:21`) ORs the
  keyboard key (`Gdx.input.isKeyPressed(keyboardCode)`), gamepad 0 (`ControllerUtils.isControllerKeyPressed`, index 0
  hard-coded in `E/controller/ControllerUtils.kt:11`) and `alternateActuators`. `ControllerButton` also holds the codes
  that the bindings screen writes.
- **Connect logic** (core): `MegaControllerPoller.run()` (`C/controllers/MegaControllerPoller.kt:19-46`) checks gamepad
  0 each frame before it polls. On connect it sets each button's `controllerCode` from prefs (if
  `loadPrefsOnConnect`) or from `controller.mapping`. On disconnect it sets them to `null`. It logs a debug line each
  frame.
- **Edges and lock** (engine): `ControllerPoller.run()` (`E/controller/polling/ControllerPoller.kt:20-47`) gives each
  button one of the four statuses from `isPressed()`, with `on = false` forcing `JUST_RELEASED` then `RELEASED`, and
  the `enabled` flag (no production code sets it to `false`). Before the first poll, `getStatus` returns `null`.
- **Frame:** `MegamanMaverickGame.render` (`C/MegamanMaverickGame.kt:533-535`) calls `controllerPoller.run()` and then
  `players.inputs[PlayerSlot(0)] = InputFrame.of(controllerPoller)`. The engine updates later in the same frame.
- **Lock:** `MegamanMaverickGame.onEvent` (`:640-656`) sets `controllerPoller.on` for `TURN_CONTROLLER_OFF/ON`, and
  `ControllerSystem.on` unless the event says otherwise.
- **Other readers of `game.controllerPoller`:** `MegaMenuScreen` (`:21-31`; 8 menu classes), six skip screens
  (`GameOverScreen:85`, `SimpleInitGameScreen:59`, `GetWeaponScreen:244`, `AbstractStoryScreen:110`, `LogoScreen:129`,
  `CreditsScreen:89`), `ControllerSettingsScreen:224` (sets `loadPrefsOnConnect` through a cast to
  `MegaControllerPoller`), `MegaLevelScreen:166-167` (a property with no reader), and the dead `ScreenController:194`.
  They use only `isPressed`, `isJustPressed`, `isJustReleased`, `isAnyJustPressed` and `isAnyJustReleased`. No caller
  uses `isReleased` or `getStatus`, so a `RELEASED` default and today's `null` give the same answers.
- **Other users of the device helpers:** `ControllerUtils` (engine object) is the receiver of the core extensions in
  `C/controllers/ControllerUtilsExtensions.kt` (load, save, reset of codes; prefs) and is used by
  `ControllerSettingsScreen` and `GameSettingsPanel:345`.
- **Touch:** `ScreenController` (dead: `screenController` is never assigned, `MegamanMaverickGame.kt:222`) registers an
  alternate actuator per button (`ScreenController.kt:147`).
- **Tests:** `MegaControllerPollerTest` (core; connect and disconnect, prefs, mapping; mocks `ControllerUtils`),
  `InputFrameTest` (core; the frame against the live poller), `MegaControllerButtonTest`, the engine `InputFrameTest`.

## In scope

1. **Engine edge builder** (`E/input/`): keeps the slot's last frame and a lock flag. Each tick it takes a held mask
   and returns the new `InputFrame` with today's rule: unlocked, from `RELEASED`/`JUST_RELEASED` to `JUST_PRESSED` if
   held, else `RELEASED`; from `PRESSED`/`JUST_PRESSED` to `PRESSED` if held, else `JUST_RELEASED`; locked, from
   `PRESSED`/`JUST_PRESSED` to `JUST_RELEASED`, else `RELEASED`. No `enabled` flag. The held mask has one bit per button
   index. No allocation per tick.
2. **Core device adapters** (`C/input/` or `C/controllers/`, the plan decides):
   - Keyboard: a button is held if `Gdx.input.isKeyPressed` of its `keyboardCode` is true.
   - Gamepad 0: the connect and disconnect logic of `MegaControllerPoller` (same order: before the read, in the same
     frame), including `loadPrefsOnConnect`; a button is held if gamepad 0 is connected and its `controllerCode`
     button is down.
   - Both read the codes from `ControllerButtons` (decision 2).
3. **Slot 0 pipeline** at the same point in `render`: the keyboard mask OR the gamepad mask goes into slot 0's edge
   builder, and the result into `players.inputs[PlayerSlot(0)]`.
4. **The lock** in `onEvent` sets slot 0's edge builder in place of `controllerPoller.on`. `ControllerSystem.on` does
   not change.
5. **The facade** (decision 3): `game.controllerPoller` becomes an `IControllerPoller` that reads slot 0's frame, with
   a `TODO` that points to the menu-input branch. Its `run()` does nothing. `ControllerSettingsScreen:224` sets
   `loadPrefsOnConnect` on the gamepad adapter.
6. **Removals:** `ControllerPoller`, `MegaControllerPoller`, `IControllerButton`, the device reads and `enabled` in
   `ControllerButton` (it keeps `keyboardCode` and `controllerCode`), `alternateActuators` (with a `TODO` in
   `ScreenController` for a touch adapter in the mobile build), and the unused `MegaLevelScreen.controllerPoller`.
   `ControllerUtils` moves to core unchanged (the core extensions and the screens keep using it), so the engine no
   longer depends on `gdx-controllers` for input.
7. **Tests:**
   - Temporary contract suite (deleted in the last commit before the merge): frozen copies of `ControllerPoller`,
     `MegaControllerPoller` and `ControllerButton.isPressed` in the test sources, against the new pipeline, both driven
     by fake devices (keys down, gamepad connected or not, gamepad buttons down, `loadPrefsOnConnect`) and lock
     toggles from seeded scripts. Each tick, the slot-0 frame from the new pipeline equals the frame built from the
     frozen poller. Named cases: hold then lock, unlock while held, press and lock in one tick, keyboard holds A and
     the gamepad then presses A, gamepad connect and disconnect while a button is held.
   - Permanent: the edge builder (each transition, the lock cases, OR before the edges); the adapters (the keyboard
     codes; the connect, disconnect and prefs cases from `MegaControllerPollerTest`, which moves to the gamepad
     adapter); the facade (each query reads the slot-0 frame).
   - `InputFrameTest`'s comparison with the live poller goes (the poller goes); its frame checks stay.

New APIs and their first callers:

| API | First callers |
|---|---|
| Engine edge builder (held mask → frame, lock) | slot 0 in `render`; the lock in `onEvent` |
| Keyboard adapter (held mask) | slot 0 in `render` |
| Gamepad-0 adapter (held mask, connect logic, `loadPrefsOnConnect`) | slot 0 in `render`; `ControllerSettingsScreen` |
| `IControllerPoller` facade over the slot-0 frame | `MegaMenuScreen`, the six skip screens, `ScreenController` |

## Out of scope

- `031d`: the `Enter`, numpad `Enter` and arrow aliases; every connected gamepad; the index-shift fix
  (`2026-10-25-gamepad-index-shift-keeps-old-codes.md`); the code-0 fix (`2026-10-25-preferred-controller-code-zero.md`);
  the stick; the triggers.
- `031e`: the code storage, the bindings screen and its issues, `selectButtonAction` per slot, `--resetBindings`.
- The menu-input branch: the menus and skip screens on frames, `Esc`, the back action.
- A touch adapter (only a `TODO`).
- `012`: lock reasons in place of `TURN_CONTROLLER_*`.

## Intended changes

None. The `enabled` flag goes, but no production code sets it to `false`.

## Affected areas and risks

- Engine: `input/` (the edge builder), `controller/` (removals). Core: `MegamanMaverickGame` (create, render, onEvent),
  the new adapters, `controllers/` (removals, `ControllerUtils` moves in), `ControllerSettingsScreen`,
  `GameSettingsPanel` (imports), `ScreenController`, `MegaLevelScreen`, the tests.
- **Edge rule:** a mistake changes the feel of every button. The temporary suite compares every tick with the frozen
  poller.
- **OR before the edges:** the edges must be computed after the OR of the devices, as today. The named case covers it.
- **Connect timing:** today the connect check runs before the read in the same `run()`. The gamepad adapter keeps this
  order.
- **Before the first tick:** the frame is `RELEASED`; the poller gave `null`. No reader tells them apart (see "Current
  state").
- **The facade** must keep the meaning of each query it serves, including the lock (it reads the locked frame, as the
  menus read the locked poller today).
- **Performance:** no allocation per tick; one pass over the 8 buttons per device.

## Overlap with other branches

No branch is active in a worktree. `012` is planned, not active.

## Decisions

Owner, 2026-10-26 (in `ITEM.md`), as recommended:

1. `031c` is the parity branch; `031d` devices; `031e` bindings.
2. `ControllerButtons` stays as code storage only.
3. The menus and skip screens read a thin `IControllerPoller` facade over the slot-0 frame.
4. The edge builder drops `enabled`.
5. The lock moves to the edge builder; `TURN_CONTROLLER_*` still switches it.
6. The edge builder goes in the engine; the device adapters in core.

## Open questions

None.
