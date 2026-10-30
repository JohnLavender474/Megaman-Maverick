# Scope: mega-refactor-031d-devices

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-30

## Goal

Make the device adapters of slot 0 read every device that single player can use, in auto mode:

- The keyboard adapter adds fixed aliases: `Enter` and numpad `Enter` always give `START`, and the four arrow keys
  always give the d-pad. The bound keys still work as today.
- The gamepad adapter reads every connected gamepad, not only gamepad 0. It tracks the gamepad that owns the codes
  by its controller object, not by its index (index-shift fix). A gamepad model with no saved file gets the library
  mapping, not code `0` (code-0 fix).
- On each gamepad, the left stick gives the d-pad at a constant `0.5` per axis.
- On each gamepad, the two triggers can be bound and act as buttons through a threshold.

All devices are OR'ed into slot 0, before the edges, as today. This branch changes behavior on purpose. Each change
is listed in "Intended changes". The engine does not change.

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes:

- From "Proposed scope": "Sources (keyboard, pad N, touch, replay, AI)", for the keyboard and every gamepad (pad N),
  in single player only.
- Decision 2026-10-26: "`031d`: devices (the keyboard aliases, every connected gamepad with the index-shift and code-0
  fixes, the stick at `0.5`, the triggers)."
- Decision 2026-10-25 (bindings): "reserved keyboard keys: `Esc`, `Enter`, numpad `Enter` and the four arrow keys.
  [...] `Enter` and numpad `Enter` always give `START`, and the arrows always give the d-pad, as fixed aliases built in
  the keyboard device adapter (the bindings stay 1:1; defaults stay `WASD` and `Enter`)." The refusal on the bindings
  screen is `031e` (see question 2).
- Decision 2026-10-25 (bindings): "the stick uses one constant threshold (proposal `0.5` per axis [...]). No revert
  timer." And: "One key per button (1:1). The stick reaches the d-pad through the fixed threshold, not through a
  binding."
- Decision 2026-10-25 (bindings): "gamepad triggers can be bound, and act like any other button (through a threshold,
  because they are axes)."
- Decision 2026-10-25: "In single player, the player can use any number of devices at the same time (auto mode)."
- Decision 2026-10-25: "in single player, an unplugged gamepad keeps today's behavior: no pause, and the keyboard (and
  any other device) keeps working."
- Decision 2026-10-25 (bindings): "gamepads get no fixed aliases. Default codes come from the library mapping on
  connect, and the player customizes them, as today."
- From `NOTES.md`, "Reserved keyboard keys and one device per player": the aliases are OR'ed in the keyboard adapter's
  held mask, before the OR per slot and before the edges, so "the player holds `W` and then presses the up arrow: no
  second 'just pressed'".
- Issues: `2026-10-25-gamepad-index-shift-keeps-old-codes.md` and `2026-10-25-preferred-controller-code-zero.md`.

## Current state

Based on `master` at `05fc5ed12`. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- **Pipeline** (from `031c`): `MegamanMaverickGame.render` builds slot 0's frame at `C/MegamanMaverickGame.kt:559`:
  `slot0FrameBuilder.build(keyboardInput.held() or gamepadInput.held())`, inside the diagnostics entry
  `"controllerPoller"` (`:558-560`). The adapters are made in `create()` (`:295-298`) from one `ControllerButtons`
  (`game.buttons`, `:208`). The lock is `slot0FrameBuilder.locked`, set by `TURN_CONTROLLER_OFF/ON` in `onEvent`
  (`:664-680`). The edge rule is in `E/input/InputFrameBuilder.kt:14-34`. Nothing in the engine reads a device.
- **Keyboard adapter:** `C/input/KeyboardInput.kt:9-18` sets a button's bit if `Gdx.input.isKeyPressed` of its
  `keyboardCode` is true. No aliases. `Enter` gives `START` only because it is the default binding
  (`C/controllers/MegaControllerButton.kt:14`). Numpad `Enter` and the arrow keys give nothing unless bound. No code
  in `core` or `lwjgl3` reads `Esc` today.
- **Gamepad adapter:** `C/input/GamepadInput.kt:15-45`.
  - Connect (`:16-29`): if `connected` is false and a gamepad is at index 0, it sets each button's `controllerCode`
    from `getPreferredControllerCode` (if `loadPrefsOnConnect`) or else from `controller.mapping`.
  - Disconnect (`:31-34`): if `connected` is true and no gamepad is at index 0, it sets every `controllerCode` to
    `null`.
  - Read (`:36-44`): a button is held if a gamepad is at index 0 and `ControllerUtils.isControllerKeyPressed(code)` is
    true. That reads index 0 only (`C/controllers/ControllerUtils.kt:11`, `:23`).
  - No stick and no trigger read.
- **Index shift (issue confirmed by reading):** the library keeps `Controllers.getControllers()` in connect order and
  removes an unplugged gamepad from it (`JamepadControllerManager`, `disconnected` calls `removeValue`). If gamepads A
  (index 0) and B (index 1) are connected and A is unplugged, B becomes index 0. `GamepadInput` checks only "is
  something at index 0" (`GamepadInput.kt:16`, `:31`), so it runs neither branch, and B keeps A's codes. The issue's
  "Where" names `MegaControllerPoller`; `031c` moved that logic to `GamepadInput.kt:16-34` unchanged.
- **Code 0 (issue confirmed by reading):** `getPreferredControllerCode` (`C/controllers/ControllerUtilsExtensions.kt:71-75`)
  returns `Preferences.getInteger(button.name)`, which is `0` for a missing key. So the `null` fallback at
  `GamepadInput.kt:25` never runs. With `loadPrefsOnConnect`, a model with no saved file gets code `0` (the bottom face
  button) for all 8 buttons. `GamepadInputTest.kt:103-122` mocks the function to return `null`, so the tests miss it.
- **One code set:** `ControllerButton` (`E/controller/buttons/ControllerButton.kt:3`) holds one `keyboardCode` and one
  `controllerCode` per button. The gamepad bindings screen edits that one set and saves it under the name of the
  gamepad at index 0.
- **Every place that assumes gamepad index 0** (13 call sites in 5 files):
  - `C/controllers/ControllerUtils.kt:11` (`isControllerKeyPressed(key)`), `:23` (`getController()`), `:25`
    (`isControllerConnected()`).
  - `C/input/GamepadInput.kt:16`, `:17`, `:31`, `:41` (the adapter).
  - `C/controllers/ControllerUtilsExtensions.kt:24` (boot codes from the mapping), `:45` (save prefs under the pad
    name), `:62` (reset to the mapping), `:81` (`selectButtonAction` prefs file).
  - `C/screens/menus/ControllerSettingsScreen.kt:49-50` (`controller` = index 0; used at `:195-205` load,
    `:287-288` capture start, `:380` back out when no pad).
  - `C/screens/utils/GameSettingsPanel.kt:346` (the gamepad settings entry needs a connected pad).
- **Library facts** (`gdx-controllers` `2.2.3`, Jamepad backend; read from the sources jar):
  - `getMapping()` is one shared `JamepadMapping` for every pad. `axisLeftX` = `0`, `axisLeftY` = `1`. `buttonL2` and
    `buttonR2` are `UNDEFINED`: the triggers are not buttons. They are axes `4` and `5` (`ControllerAxis.TRIGGERLEFT`,
    `TRIGGERRIGHT`), and `getButton` on any code returns `false` for them.
  - SDL gives the Y axis positive when the stick is pushed down. Jamepad does not flip it (inference from the source;
    needs a check on real hardware).
  - Each `getButton` and `getAxis` call is a native call (`isButtonPressed` updates the button; `nativeCheckAxis`
    calls `SDL_GameControllerUpdate`).
  - Each connect makes a new `JamepadController` object with a new random `getUniqueId()`, so object identity tells
    one connection from the next.
  - Up to 4 pads on desktop (`maxNumControllers`).
- **Gamepad bindings screen capture:** `ControllerSettingsScreen.kt:101-135` captures only `buttonDown`, on the index-0
  pad. A trigger never fires `buttonDown`, so no player can bind a trigger today.
- **Touch:** `ScreenController` is dead (`screenController` is never assigned, `MegamanMaverickGame.kt:222`). It has a
  `TODO` for a touch adapter in the mobile build (`C/controllers/ScreenController.kt:145`).
- **Readers of the frame:** gameplay reads `players.inputs[PlayerSlot(0)]`. The menus and skip screens read the same
  frame through `FrameControllerPoller` (`C/input/FrameControllerPoller.kt`). So every new input in this branch also
  reaches the menus.
- **Tests:** `KeyboardInputTest` (3), `GamepadInputTest` (8; mocks `ControllerUtils.getController()` and
  `isControllerKeyPressed`), `FrameControllerPollerTest`, `PlayerInputsTest`; engine `InputFrameBuilderTest` (8) and
  `InputFrameTest`.

## In scope

1. **Keyboard aliases** in `KeyboardInput.held()`: after the bound keys, OR `START` if `Keys.ENTER` or
   `Keys.NUMPAD_ENTER` is down, and `UP`, `DOWN`, `LEFT`, `RIGHT` if `Keys.UP`, `Keys.DOWN`, `Keys.LEFT`,
   `Keys.RIGHT` is down. A fixed table in the adapter, not in `ControllerButtons`, so the bindings screen does not
   show or change it. `Esc` gives no button.
2. **Every connected gamepad** in `GamepadInput.held()`: a loop over `Controllers.getControllers()` (index loop, no
   allocation). Each pad's bound buttons, stick and triggers OR into the one held mask. An unplugged pad leaves the
   list and gives nothing; the keyboard and the other pads keep working, with no pause.
3. **Index-shift fix:** the adapter remembers the "primary" pad (the first pad in the live list) by its controller
   object, in place of the `connected` flag. When the primary object changes (first connect, the primary unplugged
   while another pad stays, or a reconnect, which makes a new object), it runs today's connect logic for the new
   primary: its prefs if `loadPrefsOnConnect`, else the mapping. When the list becomes empty, every `controllerCode`
   goes to `null`, as today. The check stays before the read, in the same tick.
4. **One code set for every pad** (question 1): every connected pad reads the codes in `ControllerButtons`, which come
   from the primary pad. The bindings screen keeps editing that set for the primary pad (index 0 is the primary, so
   the 8 primary-pad call sites in `ControllerUtilsExtensions`, `ControllerSettingsScreen` and `GameSettingsPanel` do
   not change).
5. **Code-0 fix:** `getPreferredControllerCode` returns `null` when the prefs file does not contain the button's key
   (`contains` before `getInteger`).
6. **Stick:** on each pad, `getAxis(mapping.axisLeftX)` at or below `-0.5` gives `LEFT` and at or above `0.5` gives
   `RIGHT`; `getAxis(mapping.axisLeftY)` at or below `-0.5` gives `UP` and at or above `0.5` gives `DOWN`. Each axis
   is checked alone, so a diagonal can give two directions, as a d-pad diagonal does. A fixed constant, no binding,
   no revert timer.
7. **Triggers** (question 3): two fixed "trigger codes" that cannot collide with a button code. A button whose
   `controllerCode` is a trigger code is held when that trigger axis (`4` or `5`) is at or above the trigger
   threshold. The gamepad bindings screen's capture listener also binds a trigger when its axis passes the threshold
   (an `axisMoved` override next to `buttonDown`, `ControllerSettingsScreen.kt:101-135`). The code is stored and saved
   as any other `controllerCode`.
8. **Removal:** `ControllerUtils.isControllerKeyPressed(key)` (the index-0 overload, `ControllerUtils.kt:11`). Its only
   caller is `GamepadInput.kt:41`.
9. **Tests:** see "Tests".

New APIs and their first callers:

| API | First callers |
|---|---|
| Alias table in `KeyboardInput` (private) | `KeyboardInput.held()` |
| Primary-pad tracking in `GamepadInput` (private field, replaces `connected`) | `GamepadInput.held()` |
| Stick threshold constant `0.5` (private in `GamepadInput`) | `GamepadInput.held()` |
| Trigger codes and trigger threshold (in `GamepadInput`'s companion) | `GamepadInput.held()`; the capture listener in `ControllerSettingsScreen` |
| A seam for the pad list, for example `ControllerUtils.getControllers()` (the plan decides) | `GamepadInput.held()` (the tests mock it, as they mock `getController()` today) |

No new type. No engine API. No reserved-key set in code unless question 2 takes the refusal into this branch: no
production code would read it.

## Out of scope

- `031e`: the per-slot bindings storage that replaces `ControllerButtons`; the bindings screen behavior; refusing the
  reserved keys on the bindings screen (question 2); `--resetBindings`; `selectButtonAction` per slot; issues
  `2026-10-22-saved-key-bindings-not-applied.md`, `2026-10-25-bindings-screen-restarts-level.md` and
  `2026-10-25-p-key-binding-takes-screenshots.md`.
- Issue `2026-10-25-capture-state-left-after-gamepad-disconnect.md`: the bug is still in the code
  (`ControllerSettingsScreen.kt:280-292` capture start, `:379-385` back out with no cleanup, `:424-430` `reset()`
  clears neither the listener nor `selectedMegaButton`). It is a bindings screen bug, not a device bug, so it goes to
  `031e` (question 5).
- `031f`: `Esc` as back and pause, the menus and skip screens on frames, the removal of `controllerPoller` and
  `FrameControllerPoller`. `Esc` needs no code in this branch: as a reserved key it only matters on the bindings
  screen, and it gives no button.
- `047`: co-op device flows (join, claims, one device per player, the unplugged-pad prompt, reconnect). The adapter
  keeps one read per pad inside its loop, so `047` can split the pads by claim later. No API is added for it.
- A touch adapter: `ScreenController` keeps its `TODO`; this branch does not touch it.
- Fixed gamepad aliases (decision: none).
- The right stick (question 4).
- Per-pad code sets for two different models (question 1).
- `012`: lock reasons in place of `TURN_CONTROLLER_*`.

## Intended changes

Each item below is a gameplay change that `CONTRACT.md` names under "Intended changes":

1. `Enter` gives `START` on the keyboard whatever key `START` is bound to. Today it gives `START` only while `START` is
   bound to `Enter`.
2. Numpad `Enter` gives `START`. Today it gives nothing unless bound.
3. The four arrow keys give `UP`, `DOWN`, `LEFT`, `RIGHT`. Today they give nothing unless bound.
4. Every connected gamepad gives input to slot 0. Today only the gamepad at index 0 does.
5. Index shift (issue `2026-10-25-gamepad-index-shift-keeps-old-codes.md`): when the first pad is unplugged and another
   stays, the remaining pad gets its own codes (its saved prefs if `loadPrefsOnConnect`, else the mapping). Today it
   keeps the old pad's codes.
6. Code 0 (issue `2026-10-25-preferred-controller-code-zero.md`): with `loadPrefsOnConnect`, a pad model with no saved
   file gets the mapping. Today it gets code `0` on all 8 buttons.
7. The left stick of each pad gives the d-pad at `0.5` per axis. Today the stick does nothing.
8. A trigger can be bound on the gamepad bindings screen and then acts as a button (if question 3 keeps it here).

Because the menus and skip screens read slot 0's frame, changes 1 to 4, 7 and 8 also work there (for example, the
arrow keys and the stick move the menu cursor).

## What must stay the same

- The edge rule and the lock: `InputFrameBuilder` does not change; all devices are OR'ed before the edges, so a button
  held on two devices (for example `W` and the up arrow, or two pads) gives one `JUST_PRESSED`.
- The bound keyboard keys and their defaults (`WASD`, `K`, `J`, `Enter`, `L`).
- One gamepad alone: the same codes on connect (prefs or mapping), the same `null` codes on disconnect, the same
  reads of the bound buttons.
- An unplugged gamepad does not pause the game; the keyboard keeps working.
- `loadPrefsOnConnect` and "LOAD SAVED SETTINGS"; the rest of the bindings screen; `selectButtonAction`.
- The slot-0 frame is built only at `MegamanMaverickGame.kt:559`; the diagnostics entry keeps its name.
- `ControllerSystem`, the `TURN_CONTROLLER_*` submitters, the touch overlay.

## Tests

The engine does not change. Its edge rule and the OR-before-edges rule stay proved by `InputFrameBuilderTest` (8).
The device adapters are core and are tested with mocks (`Gdx.input`, `Controller`, `ControllerMapping`, the pad list).

- `KeyboardInputTest` (keep the 3 cases; add): `Enter` gives `START`; numpad `Enter` gives `START`; `START` rebound
  to another key and `Enter` still gives `START`; each arrow key gives its d-pad button; `W` and the up arrow together
  give one bit; `Esc` gives no bit.
- One core test that drives `KeyboardInput` into a real `InputFrameBuilder`: hold `W`, then press the up arrow: `UP`
  stays `PRESSED`, with no second `JUST_PRESSED`.
- `GamepadInputTest` (keep the 8 cases, moved to the pad-list seam; add):
  - two pads: a button on the second pad sets its bit; buttons on both pads OR;
  - index shift: list `[A, B]` then `[B]`: the codes reload for B (from B's prefs, and from the mapping when
    `loadPrefsOnConnect` is false);
  - reconnect: list `[A]`, `[]`, `[A']` (a new object): the codes go to `null`, then reload;
  - the primary stays the same object: no reload (today's "does not re-apply the mapping" case);
  - stick: `0.49` gives nothing, `0.5` gives `RIGHT`, `-0.5` gives `LEFT`, Y `-0.5` gives `UP`, Y `0.5` gives
    `DOWN`, a diagonal gives two bits, the stick ORs with a bound d-pad button;
  - triggers: a button bound to a trigger code is held when the axis is at the threshold and not below it; a trigger
    code never reaches `getButton`.
- `getPreferredControllerCode`: a missing key gives `null`; a saved key gives its value (mock `Gdx.app` prefs).
- The bindings screen has no tests. The trigger capture is checked in the manual play test.
- No temporary contract suite: the parts that must stay the same are the existing `KeyboardInputTest` and
  `GamepadInputTest` cases, which stay unchanged in meaning.

## Affected areas and risks

Files (measured from the scan): `C/input/KeyboardInput.kt`, `C/input/GamepadInput.kt`,
`C/controllers/ControllerUtils.kt`, `C/controllers/ControllerUtilsExtensions.kt` (one function),
`C/screens/menus/ControllerSettingsScreen.kt` (one listener method, only with question 3 option a); tests
`KeyboardInputTest`, `GamepadInputTest`, one new test file or case for `getPreferredControllerCode`. Of the 13
index-0 call sites, 5 change (`ControllerUtils.kt:11` and the 4 in `GamepadInput`); the 8 primary-pad sites stay.
`MegamanMaverickGame` does not change (the adapters keep their constructors). The engine does not change.

- **Native calls per tick:** each `getButton` and `getAxis` is a native call. Today: up to 8 per tick (one pad). After:
  per connected pad, up to 8 buttons plus 2 stick axes plus up to 2 trigger axes. Measure the `"controllerPoller"`
  diagnostics entry with `--diagnostics` before and after, with one and with two pads.
- **Stick drift:** a worn stick that rests past `0.5` holds a direction. `0.5` is high; the owner's pads are the check.
- **Y sign:** the SDL Y axis is positive down (inference). A wrong sign swaps `UP` and `DOWN` on the stick. Check on
  real hardware in the play test.
- **Extra devices:** any device that SDL lists as a game controller now gives input (for example a second pad left on
  the desk). This is the intended auto mode.
- **Two different models with `loadPrefsOnConnect`:** every pad reads the primary pad's codes. A second pad of another
  model uses codes saved for the first model. `031e` replaces the codes with per-slot bindings (the binding belongs to
  the player), so this limit ends there.
- **Old saved binding on a reserved key:** a player who bound, for example, `A` to the up arrow before this branch (or
  between `031d` and `031e`) gets `A` and `UP` from that key. The branch puts a `TODO` at the alias table that points
  to the refusal in `031e` (question 2).
- **Menus:** the arrow keys and the stick now move the menu cursor, and `Enter` confirms in every menu that reads
  `START`. On the keyboard bindings screen, a capture still takes an arrow key or `Enter` as the new key (until
  `031e` refuses them).
- **Capture after an index shift:** the gamepad bindings screen's listener stays on the old pad object (existing bug,
  issue `2026-10-25-capture-state-left-after-gamepad-disconnect.md`; `031e`).

## Overlap with other branches

None active. See `docs/NOTES.md`.

## Open questions

1. **Codes when several pads are connected.**
   - a) One code set (from the primary pad: its prefs or the mapping), read by every pad. The bindings screen and the
     prefs helpers keep working on the primary pad, with no change. This matches `031e`, where a slot has one gamepad
     code set for all its pads.
   - b) One code set per pad, loaded on each pad's connect from its own prefs. The bindings screen still edits one
     set, so the branch must sync it with the primary pad's set; `031e` then throws this away.
   - Recommendation: a).
   - Answer (owner, 2026-10-30, verbatim): "I agree" (option a).
2. **Refusing the reserved keys on the keyboard bindings screen.**
   - a) This branch adds the refusal (one check in `keyboardListener.keyDown`, `ControllerSettingsScreen.kt:70-98`,
     plus a reserved-key set that it reads).
   - b) `031e` adds it, as the item split says. Until then, a key that is bound and is also an alias gives both
     buttons (a `TODO` at the alias table names `031e`).
   - Recommendation: b). It keeps the bindings screen in `031e`, and the edge case needs a player to bind a reserved
     key on purpose. `Esc` needs no code in either case.
   - Answer (owner, 2026-10-30, verbatim): "approved" (option b).
3. **Where the trigger capture goes.** Without a capture on the bindings screen, no `controllerCode` can ever be a
   trigger code, so the trigger read has no production use.
   - a) This branch adds an `axisMoved` override to the existing capture listener (`ControllerSettingsScreen.kt:101-135`)
     for the two trigger axes.
   - b) Move the triggers to `031e`, with the bindings screen.
   - Recommendation: a). It keeps the item split of 2026-10-26, touches one listener, and stores the code in today's
     `controllerCode` and prefs, which `031e` carries over.
   - Answer (owner, 2026-10-30, verbatim): "approved" (option a).
4. **Which stick.**
   - a) The left stick only.
   - b) Both sticks.
   - Recommendation: a). The right stick in a Mega Man game is easy to touch by accident, and the decision names "the
     analog stick".
   - Answer (owner, 2026-10-30, verbatim): "approved" (option a).
5. **Issue `2026-10-25-capture-state-left-after-gamepad-disconnect.md`.**
   - a) `031e` fixes it with the rest of the bindings screen.
   - b) This branch fixes it, because the index shift is a device concern.
   - Recommendation: a). The fix is in the screen's `reset()` and back-out path, which `031e` changes.
   - Answer (owner, 2026-10-30, verbatim): "approved" (option a).
6. **Trigger threshold.**
   - a) `0.5`, the same value as the stick (proposal in the item `NOTES.md`).
   - b) A lower value, for example `0.12` (XInput's suggested value, inference).
   - Recommendation: a). A digital press wants a firm pull, and one value is easy to explain.
   - Answer (owner, 2026-10-30, verbatim): "approved" (option a).
