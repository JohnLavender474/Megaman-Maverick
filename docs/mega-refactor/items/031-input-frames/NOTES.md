# Notes: 031 Input intents per player (`InputFrame`), sources and router

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 30: INPUT INTENTS PER PLAYER (`InputFrame`)".

````text
1. Current state (the flow)
- Bindings: `MegaControllerButton` is 8 buttons with default keys (`C/controllers/MegaControllerButton.kt`). `ControllerButtons` maps each to a `ControllerButton(keyboardCode, controllerCode, alternateActuators)`.
- `ControllerButton.isPressed()` ORs keyboard, controller index 0 and touch (`E/controller/buttons/ControllerButton.kt:21`). Touch comes from `ScreenController`, which puts an alternate actuator for each button (`C/controllers/ScreenController.kt:147`).
- `MegaControllerPoller.run()` maps controller codes on connect from prefs or defaults (`C/controllers/MegaControllerPoller.kt`). Prefs are keyed by controller name (`ControllerUtilsExtensions.kt:5`).
- `ControllerPoller.run()` builds one status map (RELEASED / JUST_PRESSED / PRESSED / JUST_RELEASED) once per render frame, in `MegamanMaverickGame.kt:485`, before `screen.render`.
- When `on = false`, held buttons go to JUST_RELEASED for one frame, then RELEASED (`ControllerPoller.kt:42`).
- `TURN_CONTROLLER_OFF/ON` toggles the poller and `ControllerSystem` (`MegamanMaverickGame.kt:593-610`).
- `ControllerSystem` calls actuators per status (`E/controller/ControllerSystem.kt`).
- 63 direct reads (see "Number checks"). They are in movement behaviors, screens and menus (`MegaMenuScreen.kt:21, 31`), special entities (`PipePortal.kt:153-159`, `SpringBouncer.kt:209-216`, `SpiderWeb`, `GroundSlideBlocker`), and the physics collision handler (`MegaCollisionHandler.kt:46`, inside world substeps).
- There are no axes and no per-player separation. All sources merge into one player.

2. Options
A. Add an index to the poller (`game.pollerFor(i)`). Pro: small change. Con: no replay, and buttons stay polled everywhere.
B. One immutable `InputFrame` per player per tick, made by input sources. The poller becomes an adapter.
C. An action/event queue (commands). Pro: suits networking. Con: changes just-pressed semantics, and a large rewrite.

3. Recommendation
Option B, adapter first.
- `InputFrame`: `held`, `pressed` (edge), `released` (edge) as bitsets over `PlayerAction`. At first `PlayerAction` = the 8 buttons, renamed later to intents (JUMP, SHOOT, DASH, PAUSE, SWITCH_WEAPON). Plus `moveX`, `moveY` in [-1, 1]. Today the d-pad gives ±1.
- Sources: `KeyboardSource(bindings)`, `GamepadSource(index, bindings)`, `TouchSource(ScreenController)`, `ReplaySource(file)`, `AiSource(script)`.
- `InputRouter` assigns sources to player slots. In single-player, keyboard + pad 0 + touch merge into slot 0, the same OR as today.
- Entities read `player.input` (the owner's frame). Objects that react to a player (PipePortal, SpringBouncer, `MegaCollisionHandler`) read the frame of the player in the contact, not a global one.
- Menus use a separate `UiInput` (navigate / confirm / back) from any source.
- Control lock: a per-player gate that produces the same "held -> one-tick release -> released" sequence as `ControllerPoller.kt:42`.
- Co-op: a join screen. "Press START" on an unassigned source claims the next slot. Bindings are saved per device, using the per-controller-name prefs that exist.
- Replay: record `{tick, slot, frame}`, only on change, plus seed (item 10), fixed step (item 9) and start level/room (item 7). Replaying feeds `ReplaySource`.
- Just-pressed timing stays the same: the adapter builds the frame at the same place (`MegamanMaverickGame.kt:485`), once per frame, and all readers in that frame see the same frame. This includes the physics substeps (`MegaCollisionHandler.kt:46`), which see one frame across all substeps today. If item 9 later runs several ticks per frame, an edge must go to exactly one tick (the first). That is a deliberate change and needs a test.

Migration:
1. `InputFrame` and `game.input.frame(0)` built from today's poller. No behavior change.
2. Move call sites: Megaman behaviors and actuators first (38), then special entities and the collision handler, then screens to `UiInput`.
3. `ControllerSystem` takes `InputFrame`.
4. Real sources and the router, gamepad index N, axes.
5. Record and replay.

4. Open questions for the owner
- Rename A/B to intents (JUMP/SHOOT) now, or keep button names?
- Analog stick: digital threshold only, or real analog movement for some characters?
- Must touch and keyboard still merge for player 1 on desktop?
- How many players at most (2 or 4)?

5. Backlog wording
Replace "Support multiple controllers and co-op" with: "Input intents per player (`InputFrame`), sources and router"
- `InputFrame` (held / pressed / released per action, plus axes), built once per tick at today's poll point.
- Sources (keyboard, pad N, touch, replay, AI). Router assigns them to slots. Join screen for co-op.
- Entities read the owner's frame. Menus use `UiInput`. The collision handler reads no input.
- Control lock per player with today's release sequence.
- Record and replay of frames (with items 9/10).

6. Dependencies
Item 28 (players), 11 (locks), 9/10 (replay), 7 (start args for replay), 5 (headless replay), 33 (persisted bindings), 31 (input sources are platform-specific; keep them out of `engine` core).
````

## Input assignment: cases and options (2026-10-25)

Based on `master` at `95deef9b2`. This is a first pass for discussion, not a full scan.

### Today

- `ControllerButton.isPressed()` ORs the keyboard, gamepad 0 and the touch overlay (`ScreenController`) into one
  set of buttons. There is one player.
- `MegaControllerPoller.run()` (`C/controllers/MegaControllerPoller.kt`) checks each frame if a gamepad is
  connected. On connect it maps the gamepad codes (from prefs keyed by the gamepad name, or the default mapping). On
  disconnect it clears the gamepad codes. The game does not pause. The keyboard still works, so in single player
  the keyboard is already a silent fallback.
- Only one gamepad is used (`ControllerUtils.getController()`). The library is `gdx-controllers` `2.2.3`, which has
  connect and disconnect events (`ControllerListener`) and `Controller.getUniqueId()`. The game does not use
  either.
- The mouse is not used, except that the touch overlay can be clicked.

### Cases the design must handle

1. Single player, keyboard only.
2. Single player, keyboard and a gamepad, both used (today's merge). The player picks up whichever is closer.
3. Single player, the gamepad is unplugged.
4. Single player, the gamepad stops responding but stays connected. The game cannot detect this. Only the player
   knows.
5. Single player, a gamepad is plugged in during play.
6. Co-op, player 1 on a gamepad and player 2 on the keyboard. They must not merge.
7. Co-op, two gamepads (maybe two of the same model, with the same name).
8. Co-op, one player's gamepad is unplugged or stops responding. The other player keeps a working device.
9. Co-op, the unplugged gamepad comes back, or the player wants to switch to another free device.
10. A player joins or leaves co-op during a session.
11. Menus and pause: who controls them in co-op.
12. Later: 4 slots for split screen and online. Online slots get frames from the network, not from local devices.

### The parts of the design

- **Devices:** the keyboard, each gamepad (by its unique id while connected), the touch overlay, and the mouse.
- **Slots:** player 1 to player 4. Each slot claims a set of devices. Its `InputFrame` is the OR of its devices.
- **The mouse is never a player device.** It can only drive menus, so it belongs to nobody.
- **System keys:** a few keyboard keys (for example `Esc` for pause and the input menu) that work even when the
  keyboard belongs to a player. The rest of the keyboard belongs to its slot.

### Option A: keep today's merge for player 1; co-op takes only free devices

- Pros: no change for single player.
- Cons: fails case 6. Player 1's merge takes the keyboard, so player 2 cannot have it. Rejected.

### Option B: "auto" in single player, explicit claims in co-op (recommended)

- Single player: slot 1 is in auto mode. It claims every device, including any gamepad that is plugged in later.
  This is today's merge, so cases 1, 2, 3 and 5 behave as today.
- Co-op: a join step ("press `START`") claims one device for each slot. A device that a slot claimed is not part of
  any other slot. Case 6 and case 7 work. Two gamepads of the same model are told apart by the unique id, not by
  the name. Bindings stay keyed by the gamepad name, so identical pads share bindings.
- Disconnect in co-op (case 8): the game pauses and shows "Player 2: press `START` on a device". Any free device,
  or the same gamepad when it comes back, claims the slot (case 9).
- Unresponsive gamepad (case 4 and case 8): nothing can detect it, so the player needs a way out that does not use
  that gamepad. The system keys and the mouse open the pause menu and the input menu from any state. There, the
  player gives the slot another device.
- Pros: single player stays the same, so the contract is easy. Co-op gets clear ownership. One model covers all 12
  cases.
- Cons: two modes (auto and claimed) to test. The switch from single player to co-op must move slot 1 from auto to
  claimed.

### Option C: explicit claims always

Each session starts with "press `START`", also in single player. Slot 1 claims only that device.

- Pros: one mode only. The same code path for single player and co-op.
- Cons: a change in gameplay for single player. The player can no longer use the keyboard and the gamepad at the
  same time, and an unplugged gamepad stops the game where today the keyboard takes over. Slot 1 can claim several
  devices to fix this, but then it is Option B's auto mode again.

### Sub-choices (for Option B or C)

Disconnect in single player:
- (i) Keep today's behavior: no pause, the keyboard keeps working. No contract change.
- (ii) Pause and show the reconnect prompt, as in co-op. This is clearer for the player, but it changes gameplay.

Reconnect in co-op:
- (i) "Press `START` on any free device" claims the slot. Simple and robust, also for a different device.
- (ii) Match the same gamepad by unique id and give it back to its slot automatically. More convenient, but the id
  is valid only while the game runs, and a new device still needs (i).
- Recommended: (i), with (ii) only if the same id comes back while the game runs.

The way out for an unresponsive gamepad:
- (i) System keys on the keyboard (for example `Esc`). The keyboard is always connected on desktop, so it is the
  most trusted device. In co-op, a key press by the keyboard player then pauses for both players. That is normal in
  shared-screen co-op.
- (ii) The mouse drives the pause menu and the input menu. It belongs to nobody, so it never conflicts. But it is a
  new input path for menus (today the menus use only buttons), and some setups have no mouse (a handheld PC).
- (iii) Both. Recommended: the keyboard first (no new UI input path), the mouse as an addition in the input menu.

The input menu:
- It shows each slot and its devices, and lets a player claim or free a device ("press a button on the device to
  give it to player 2").
- It is reachable from pause, from the system keys and from the mouse.
- It also handles keyboard-only co-op later (two players on one keyboard with two key sets), if wanted. That is
  out of scope for now.

### Effect on the `031` scope

- The adapter step (an `InputFrame` for slot 1 from today's poller) does not depend on this choice.
- Devices, slots, the router and the join step belong to a later branch of `031`, or to `047` (co-op), which needs
  `MegaPlayers` with a capacity above 1.
- The input menu is a new screen. It belongs with the router branch, not with the adapter.

## Custom key bindings in the proposed setup (2026-10-25)

Based on `master` at `88177c11b`. Line numbers refer to that commit. `C/` is
`core/src/main/kotlin/com/megaman/maverick/game/`, `E/` is `engine/src/main/kotlin/com/maverick/game/engine/`. The
library facts come from the sources of `gdx-controllers` `2.2.3` and `jamepad` `2.0.20.0` in the Gradle cache. I
read code only. I did not run the game, so the bugs below are found by reading and are not reproduced.

### Today

Data model:

- `MegaControllerButton` has 8 buttons, each with one default keyboard key: `W`, `S`, `A`, `D`, `K` (`A`), `J`
  (`B`), `ENTER` (`START`), `L` (`SELECT`) (`C/controllers/MegaControllerButton.kt:6-14`).
- Each button has one `ControllerButton` with one `keyboardCode`, one nullable `controllerCode` and a map of
  `alternateActuators` (`E/controller/buttons/ControllerButton.kt:7-11`). So a button has exactly one key and one
  gamepad button. There is no second key for a button (for example the arrow keys and `WASD` together).
- `isPressed()` ORs the keyboard key, gamepad 0 and the alternate actuators (`ControllerButton.kt:21`). The
  gamepad is always index 0 (`E/controller/ControllerUtils.kt:11`, `:23`).
- The binding lives inside the button object. So the binding and the device read are one thing today.

Defaults for the gamepad:

- The default gamepad codes come from `controller.mapping` through `getMapping()`
  (`C/controllers/ControllerMappingExtensions.kt:5-14`): `A` is `buttonB` (right face button), `B` is `buttonY`
  (top face button), `SELECT` is `buttonA` (bottom face button), `START` is `buttonStart`, and the d-pad is the
  d-pad.
- On desktop, `getMapping()` returns one shared `JamepadMapping` for every gamepad (`JamepadMapping.java:10-28`,
  `JamepadController.java:277-279`). It is the SDL game controller layout, so SDL already puts the buttons of a
  known gamepad model in the standard places. Inference: custom gamepad bindings are mostly a player preference
  (for example jump on the bottom face button), or a fix for a gamepad that SDL does not know.
- The analog sticks do nothing. No code reads an axis (no `getAxis` call in `core`, `engine` or `lwjgl3`). The
  triggers `L2` and `R2` are axes in SDL and are `UNDEFINED` in the mapping (`JamepadMapping.java:16-17`), so they
  cannot be bound.

Touch overlay:

- `ScreenController` puts one alternate actuator per button under the key `ConstKeys.SCREEN`
  (`C/controllers/ScreenController.kt:147`). The on-screen button decides the game button. There is nothing to
  rebind. The overlay also sets `Gdx.input.inputProcessor` to its stage (`ScreenController.kt:96`).

Load at boot and on connect:

- Boot builds the buttons from the default keys and the default gamepad mapping only
  (`C/MegamanMaverickGame.kt:287`, `C/controllers/ControllerUtilsExtensions.kt:17-35`). Boot sets
  `selectButtonAction` to `NEXT_WEAPON` (`MegamanMaverickGame.kt:289`).
- `MegaControllerPoller.run()` sets the gamepad codes when gamepad 0 connects: from prefs if
  `loadPrefsOnConnect` is `true`, else from the default mapping (`C/controllers/MegaControllerPoller.kt:22-35`). On
  disconnect it sets all gamepad codes to `null` (`:37-43`). `loadPrefsOnConnect` starts `false` (`:15`).
- The poll runs once per render frame, before the screen render (`MegamanMaverickGame.kt:531`).

The settings screens:

- Two instances of `ControllerSettingsScreen` share the one `ControllerButtons`: one for the keyboard and one for
  the gamepad (`MegamanMaverickGame.kt:420-421`). Both open from `GameSettingsPanel` (main menu and level pause).
  The gamepad screen refuses to open if no gamepad is connected (`C/screens/utils/GameSettingsPanel.kt:343-348`).
- Keyboard rebind: the screen replaces `Gdx.input.inputProcessor` with a key listener
  (`C/screens/menus/ControllerSettingsScreen.kt:290-292`). The next key goes to the selected button. Any key is
  accepted; no key is reserved.
- Gamepad rebind: the screen adds a `ControllerAdapter` to gamepad 0 (`:295`). The next button goes to the selected
  button.
- Duplicates: both paths swap. If another button has the new code, it gets the old code of the selected button
  (`:80-85` keyboard, `:117-122` gamepad). So each device always has 8 different codes, and no button is ever
  unbound.
- Each rebind saves all 8 codes of that device at once (`:88-92`, `:124-129`).
- "LOAD SAVED SETTINGS" reads the saved codes into the buttons (`:181-232`). On the gamepad screen it also sets
  `loadPrefsOnConnect = true` (`:224`). It also loads the saved `selectButtonAction` (`:227`).
- "RESET TO DEFAULTS" sets the default codes and `NEXT_WEAPON` (`:251-258`).
- The screen shows the raw integer code, for example `A: 39` (`:271-275`), not a key name.
- When the screen opens from the level pause, its back action calls `game.startLevel()`
  (`GameSettingsPanel.kt:313-325`). Inference: this reloads the level from its start (`MegamanMaverickGame.kt:955-963`),
  so a player who rebinds during a level loses the progress in that level.

Persistence:

- Keyboard: one prefs file `Megaman Maverick - Keyboard Preferences`, one integer per button name
  (`C/Constants.kt:8`, `ControllerUtilsExtensions.kt:11-12`, `:37-44`).
- Gamepad: one prefs file per gamepad name, `Megaman Maverick - Controller Preferences - <name>`
  (`Constants.kt:7`, `ControllerUtilsExtensions.kt:14-15`, `:45-53`). So two gamepads of the same model share one
  file.
- `selectButtonAction` is saved under `SELECT_BUTTON_ACTION` in the keyboard file or in the gamepad file, by the
  screen that changed it (`ControllerUtilsExtensions.kt:78-89`, `ControllerSettingsScreen.kt:347-357`). But the game
  has only one value (`MegamanMaverickGame.kt:204`). Its readers are `MegaLevelScreen.kt:908-909` (pause),
  `entities/megaman/components/ControllerComponent.kt:98` and `entities/megaman/behaviors/AirDashBehavior.kt:30-35`.

System keys today:

- No key is reserved. There is no `Esc` action. The only direct key read is the screenshot key `P`
  (`MegamanMaverickGame.kt:164`, `:560`), and only when `allowScreenshots` is on. A player can bind `P` to a game
  button; then a press does both.

### Bugs and smells found

1. Saved keyboard keys and the saved gamepad codes do not apply at boot (issue
   `2026-10-22-saved-key-bindings-not-applied.md`; the line numbers in that issue moved: boot is now
   `MegamanMaverickGame.kt:287`). Still true.
2. `selectButtonAction` resets at boot (issue `2026-10-22-select-button-action-resets.md`; now
   `MegamanMaverickGame.kt:289`). Still true.
3. New, not filed: `getPreferredControllerCode()` can never return `null` (`ControllerUtilsExtensions.kt:72-76`).
   `Preferences.getInteger(key)` returns `0` when the key is missing. So after "LOAD SAVED SETTINGS" on the gamepad
   screen (`loadPrefsOnConnect = true`), a gamepad model with no saved file gets code `0` for all 8 buttons on
   connect. The `null` fallback at `MegaControllerPoller.kt:31` never runs. Code `0` is the bottom face button
   (`SELECT` by default), so that one button presses all 8 game buttons. `MegaControllerPollerTest.kt:110` mocks the
   function to return `null`, so the test hides the bug. The bug can hit when the player plugs in another gamepad
   model, with no saved file, later in the same session.
4. "RESET TO DEFAULTS" does not save (`ControllerSettingsScreen.kt:254`), but each rebind saves at once. So after a
   reset, "LOAD SAVED SETTINGS" brings back the old codes. The reset of `selectButtonAction` is also not saved.
5. `selectButtonAction` is one game setting, but it is saved per device file. Which value loads depends on the
   screen where the player selected "LOAD SAVED SETTINGS".
6. Today the restart is the escape hatch for a bad binding: boot always uses the defaults (bug 1). When `034`
   applies saved bindings at boot, this escape goes away. A real escape hatch must exist before or with that fix.
7. Correction to "Input assignment: cases and options", sub-choice "Reconnect in co-op (ii)": on desktop,
   `getUniqueId()` is a new random `UUID` for each `JamepadController` (`JamepadController.java:45`, `:202-203`). A
   reconnect makes a new `JamepadController` (`JamepadControllerMonitor.java:38-43`, `:70-73`). So the same id never
   comes back, also while the game runs. Matching a returned gamepad is possible only by name, and the name is the
   same for identical models.

### What `gdx-controllers` `2.2.3` gives

- `getName()`: the model name from SDL. It is stable across runs. Identical models have the same name. Inference:
  the name can change with the OS or the driver, so the same gamepad on another computer can get a new entry.
- `getUniqueId()`: tells apart two connected gamepads of the same model. It is valid for one connection only (see
  item 7 above). The Javadoc says: "Use this to map a controller to a player, but do not use it to save a button
  mapping" (`Controller.java:41-45`).
- `getMapping()`: one standard layout for all gamepads on desktop (see "Today").
- No GUID, vendor id, product id or serial number in the API (`jamepad` `ControllerIndex` has none of them).
- `getAxis()` and the mapping axis codes (`axisLeftX`, `axisLeftY`) exist, so the stick-to-d-pad threshold is
  possible.

So the only stable key for a saved gamepad binding is the name. The unique id is for the assignment of a device to
a slot (the router), not for bindings.

### Cases

1. Single player, custom keyboard keys. Must apply at boot (after `034`).
2. Single player, custom gamepad codes for one model.
3. Co-op, two gamepads of the same model, and the two players want different maps.
4. A gamepad disconnects and connects again (new unique id, same name).
5. A gamepad model that the game has never seen: it gets the defaults.
6. The same gamepad on another computer or OS (maybe another name).
7. Later: keyboard-only co-op with two key sets on one keyboard.
8. System keys (for example `Esc`) and player keys on the same keyboard.
9. The stick threshold (and a dead zone) for stick-to-d-pad.
10. Per-player settings, for example `selectButtonAction`.
11. The touch overlay: fixed buttons, no bindings.
12. Rebinding a device while another player plays.
13. A bad binding makes the game hard or impossible to control.
14. Replay (`055`) and online play: bindings must not change the recorded or sent input.

### Options: who owns a binding

#### Option 1: the device model (keyboard; gamepad by name), shared by all slots

This is today's model, made explicit. One binding table for the keyboard and one per gamepad name. A slot that
claims a device uses that device's table.

- Pros: smallest change. Migration is one to one. No profile UI. Cases 1, 2, 4, 5, 11, 14 work. Case 4 needs no
  extra code: the name finds the same table.
- Cons: case 3 fails: two identical gamepads share one table. Case 10 does not fit: a model is not a person.
  Case 6 gives a new table.

#### Option 2: the device model, plus an override per slot

Option 1, plus "slot 2 uses its own table for gamepad model X".

- Pros: case 3 works.
- Cons: two layers to explain and to show in the UI. A slot is a seat, not a person: when two players change seats,
  the overrides do not follow them.

#### Option 3: a player profile

A named profile ("player name") holds one table per device kind (keyboard, gamepad by name or one standard gamepad
table) plus player settings (`selectButtonAction`). A player picks a profile when they join.

- Pros: cases 3 and 10 work. The bindings follow the person, also to another seat. Fits online later (each machine
  has its own profiles).
- Cons: the most work: create, name, pick and delete profiles. Nobody asked for profiles yet. The single player
  path needs a default profile, so it adds a step or a hidden default.

#### Option 4: the slot

Slot 1 has a keyboard table and a gamepad table, slot 2 has its own, and so on.

- Pros: case 7 is natural (slot 1 key set, slot 2 key set). Case 3 works.
- Cons: a person who joins as player 2 gets another map than in single player. A gamepad model's table is copied in
  each slot. Same seat problem as option 2.

#### Recommendation: Option 1, with a key that can grow

- Use Option 1 now. Key each table by a binding id. In the first version the id is the device model: `keyboard`,
  or `pad:<name>`.
- Case 7 later: a second key set is a second keyboard "device" with its own id (for example `keyboard-2`). The
  router can let a slot claim it like any other device. This fits Option B of the assignment notes, and needs no
  slot-owned bindings.
- Case 3: accept the limit for now (both identical pads use one table). If the owner wants case 3, add Option 3
  later. The binding id makes this a lookup change (profile id instead of model id), not a rewrite.
- Case 10: `selectButtonAction` is a player setting, not a binding. Keep it as one game setting until co-op. In
  `047` it moves to the slot (or to a profile, if Option 3 comes). Do not keep it in the device files. Inference:
  storing it per device today is an accident of the two screens, not a design.
- Case 9: one stick threshold for all devices, as a constant at first (owner decision: threshold only). Make it a
  per-model value in the same table only if a real gamepad needs it. Inference: today the stick does nothing, so
  stick-to-d-pad is a new input in single player. It is small, but the branch that adds it must name it under
  "Intended changes" in its contract.
- Case 11: the touch overlay has no table. Its adapter gives the 8 buttons directly.

### Rules for one binding table

- Each of the 8 buttons has exactly one code per table. Keep today's swap rule, so no button is unbound and no code
  drives two buttons.
- Reserved keys: a small fixed set of system keys (proposal: `Esc` for pause and the input menu). The bindings
  screen refuses a reserved key and says why. Gamepads have no reserved button; `START` is the pause button, as
  today.
- The screenshot key `P` is a dev key. Either reserve it while `allowScreenshots` is on, or move it to a key that
  players do not use (for example `F12`). Open question.
- A second keyboard key set (case 7, later) must not use a key of the first set or a reserved key. The swap rule
  then works across the two keyboard tables.
- More than one key per button (for example arrows and `WASD`) is not possible today. It is a new feature, not
  part of `031`. Open question.

### Place in the frame pipeline

```text
device raw state ──(binding table, stick threshold)──> held mask per device (8 bits)
held masks of the devices that a slot claims ──OR──> held mask per slot
held mask per slot + control lock (today's on=false sequence) + enabled flags
held mask per slot vs. previous tick ──> pressed / released edges ──> InputFrame per slot
```

- Bindings resolve in the device adapter. The output of the adapter is a button mask. Nothing after the adapter
  knows about key codes or gamepad codes.
- Edges are computed per slot, after the OR. This is today's behavior: `ControllerButton.isPressed()` ORs the
  devices first (`ControllerButton.kt:21`), and `ControllerPoller.run()` computes the edge once from that result
  (`E/controller/polling/ControllerPoller.kt:30-44`). Keep it. Example: the keyboard holds `A` and the gamepad then
  presses `A`. With the OR first, there is no second "just pressed". With edges per device and then an OR, the slot
  would get a "just pressed" while `A` is already held, and a release of one device would not release the slot.
  That would change the jump and shoot feel.
- The control lock and the `enabled` flag of a button also apply per slot, after the OR, as today
  (`ControllerPoller.kt:31-43`).
- The bindings screen reads raw device events (one key or one button), not frames. It must, because the new code
  may have no button yet.
- Menus read `UiInput` built from frames (or from raw devices for the input menu). The mouse goes only to menus.
- Module: the binding tables use `MegaControllerButton`, so they are game content and go in `core`. The device
  adapters read LibGDX input, so they stay out of the engine core (see `032`).

### The input menu and the bindings screen

- Two screens, linked. The input menu (assignment: which slot has which device) and a bindings screen for one
  device (binding id). The input menu opens the bindings screen for the device that the player selects, or for "the
  device on which you press a button now".
- Rebinding captures from that one device only (by its unique id while it is connected). Today the capture listens
  to gamepad 0 only (`ControllerSettingsScreen.kt:295`). With several gamepads, a press on another player's gamepad
  must not change the binding.
- Who can open it: in shared-screen co-op, any player opens pause, and pause stops the game for both players. So
  "rebind while another player plays" does not happen on a shared screen. Inference: in split screen it is the same
  (one game, one pause). In online play, a local pause overlay is needed, and bindings are local, so a player can
  rebind without stopping the other machines. That design belongs to the online item, not to `031`.
- The bindings screen must be usable without the device that it changes: the mouse and the system keys can always
  move in it, select "reset to defaults" and leave.
- Back from the bindings screen during a level: today it restarts the level (see "Today"). Recommendation: go back
  to the pause menu without a restart. That is a behavior change, so it needs an owner decision and an "Intended
  changes" entry.

### The escape hatch

A bad binding must never lock the player out. Layers, from most to least common:

1. The rules above already block the worst cases: every button keeps one code, no code drives two buttons, and
   system keys cannot be bound.
2. In single player (auto mode), the keyboard and the gamepad both work, so a bad map on one device can be fixed
   with the other.
3. `Esc` and the mouse always reach pause, the input menu and the bindings screen, also when the player's own
   device is badly bound.
4. "Reset to defaults" in the bindings screen resets that one device and saves at once (fixes bug 4).
5. Last resort outside the game: a launcher flag such as `--resetBindings` (same pattern as the dev start options
   in `DesktopLauncher`), or delete the settings file. Inference: a flag is cheap and helps players on a handheld PC
   with no mouse.
6. Optional: after a change, "Keep these bindings?" with a revert after a few seconds, as in display settings.

Layers 3 and 4 must exist before `034` applies saved bindings at boot (bug 6).

### Persistence and migration

Proposal for the bindings part of the `034` settings file (`settings.json`):

```json
{
  "version": 1,
  "bindings": {
    "keyboard": { "UP": "W", "DOWN": "S", "LEFT": "A", "RIGHT": "D", "A": "K", "B": "J", "START": "Enter", "SELECT": "L" },
    "pads": {
      "<gamepad name>": { "UP": 11, "DOWN": 12, "LEFT": 13, "RIGHT": 14, "A": 1, "B": 3, "START": 6, "SELECT": 0 }
    }
  },
  "stickThreshold": 0.5,
  "selectButtonAction": "NEXT_WEAPON"
}
```

- Keyboard keys as names (`Input.Keys.toString` / `Input.Keys.valueOf`), so the file is easy to read and to fix by
  hand. Inference: the LibGDX key codes are also stable, so integers would work too.
- Gamepad codes as SDL button indexes (the standard layout), keyed by `getName()`. Never by `getUniqueId()`.
- The numbers in the example are an illustration. Take the real defaults from `controller.mapping`.
- Load rule per table: a missing or unknown button gets its default. If the table has a duplicate or a reserved key,
  use the defaults for that whole table and log an error.
- Migration from the old prefs, once, and leave the old files unchanged (`034` scope):
  - Keyboard: read `Megaman Maverick - Keyboard Preferences` at the first boot with no `bindings.keyboard`.
  - Gamepads: the file names hold the gamepad name, and the game cannot list them in a portable way. So import on
    connect: when a gamepad connects and the settings file has no table for its name, read
    `Megaman Maverick - Controller Preferences - <name>` if it exists. Inference: this is simpler than a scan of the
    prefs folder.
  - `SELECT_BUTTON_ACTION` is in both kinds of files. Pick one source (proposal: the keyboard file, else the first
    gamepad file found). Open question.
  - Check each imported table with the load rule above. A file can hold a bad table (for example after bug 3).
- Replay (`055`): the replay records `InputFrame`s, which come after the bindings. So bindings do not change a
  replay, and a replay plays the same on a machine with other bindings. Inference: `055` has no design yet; this
  holds if it records frames, as the review proposes.
- Online: each machine resolves its own bindings and sends frames. Bindings are local only and are never sent.

### What belongs where

- First `031` branch (adapter): no binding change. Slot 1's `InputFrame` comes from today's poller and today's
  `ControllerButtons`. The bindings screens, the prefs and their bugs stay as they are. The contract is "plays the
  same".
- Later `031` branch (devices and router): move `keyboardCode` and `controllerCode` out of `ControllerButton` into
  binding tables keyed by binding id; one adapter per device that uses its table; stick-to-d-pad with a threshold
  (an intended change); load a table per gamepad name on connect, for each gamepad, not only gamepad 0; the bindings
  screen captures from one device; reserved system keys. Fix bug 3 here, because the connect code changes anyway, or
  before as an issue fix.
- Later `031` branch (input menu): the input menu, its link to the bindings screen, mouse input in these two
  screens, the escape hatch layers 3 and 4, and key names instead of raw codes on screen.
- `034`: the settings file, apply the bindings and `selectButtonAction` at boot (fixes bugs 1 and 2), the migration,
  and the `--resetBindings` flag if wanted. Needs the escape hatch first.
- `047`: `selectButtonAction` per slot, the second keyboard key set, and profiles (Option 3) if the owner wants case
  3.
- `055`: nothing on bindings; it records frames.
- `docs/issues/`: file bug 3. Bug 4 can go in the same file or in its own.

### Open questions for the owner

1. Is Option 1 (bindings per device model, shared by all slots) enough? Must two players with the same gamepad
   model have different maps (case 3)? If yes, is a player profile (Option 3) wanted?
2. Is `selectButtonAction` one setting for the game, or a setting per player in co-op?
3. Which keys are system keys? Only `Esc`? What happens to the screenshot key `P`?
4. Must a button accept more than one key (for example arrows and `WASD`)?
5. The stick threshold: a fixed constant, or a setting the player can change? The left stick only, or also the
   right stick?
6. When a player leaves the bindings screen during a level, must the level still restart, or go back to pause?
7. Is a "keep these bindings?" revert timer wanted, and a `--resetBindings` launcher flag?
8. Must the triggers (`L2`, `R2`) be bindable? They are axes, so this needs a threshold like the stick.
9. The touch overlay: no rebinding, only its fixed layout. Confirm.
10. For the migration of `selectButtonAction`, which old file wins: the keyboard file or the gamepad file?

### Owner's answers on bindings (2026-10-25)

Answers to the open questions above (numbers as in that list). Questions 5 and 7 to 10 are still open.

1. **Yes: two players with the same gamepad model must be able to have different maps.** So Option 1 alone is not
   enough. The binding must belong to the player, not only to the device model. See "Recommendation after the
   answers" below.
2. **`selectButtonAction` is one setting per player.** It moves with the player's bindings, not with a device file.
3. **The screenshot key.** The screenshot logic takes a generic trigger (a lambda), not the `P` key. For now the
   launcher gives the trigger, with a `TODO` to update it later (to a system key or a binding). Done as a direct
   change to `master` on 2026-10-25; see `process-log.md`. Which other keys are system keys (`Esc`?) is still open.
4. **One key per button (1:1).** The usual reasons for more than one key per button do not apply here:
   - Arrows and `WASD` at the same time is a comfort feature for keyboard players. Each player can bind the set
     they like, so one set per player is enough.
   - The gamepad d-pad and the left stick both give the d-pad buttons, but the stick goes through the fixed
     digital threshold in the device adapter, not through a binding. So the 1:1 rule stays true for bindings.
   - Menu keys (for example `Enter` to confirm) are part of `UiInput` and the system keys, not of the player's
     bindings.
   - Keyboard-only co-op later uses two key sets, one per player. Each set is still 1:1.
6. **Back from the bindings screen during a level goes back to pause, without a restart.** The restart today is a
   gap in the flow, not a design. Filed as `docs/issues/2026-10-25-bindings-screen-restarts-level.md`.

### Recommendation after the answers

Answers 1 and 2 put the bindings and `selectButtonAction` on the player. Two ways fit:

- **Option 4 (the slot), recommended for now.** Each slot (player 1 to 4) keeps its own settings: one keyboard
  table, one table per gamepad name, and `selectButtonAction`. Slot 1's settings are today's settings (migration
  is one to one). A new slot starts from the defaults.
  - Pros: no profile UI. It matches a couch game: "player 2" is the person on the right. The router already thinks
    in slots.
  - Cons: if two people change seats, they also change maps. They must rebind, or change seats back.
- **Option 3 (profiles), later if wanted.** A profile is a named, movable copy of one slot's settings. A player picks
  a profile when they join, and the profile is loaded into the slot.
  - Pros: the map follows the person to any seat. It also fits online play.
  - Cons: create, name, pick and delete profiles; more UI.
  - The step from Option 4 to Option 3 is small if the settings are one object per slot from the start. A profile
    is then that object with a name.

The binding id from the earlier recommendation stays: inside a slot's settings, each table is still keyed by
`keyboard` or `pad:<name>`. The table lookup becomes (slot, binding id) instead of (binding id).

A decision for the owner: Option 4 now (with Option 3 possible later), or profiles from the start.

### More owner answers on bindings (2026-10-25)

- **Bindings per slot now (Option 4), profiles later.** The owner agrees: per-slot settings move in the direction of
  profiles. Keep each slot's settings in one object from the start, so a profile is later that object with a name.
  Profiles wait for a feature branch during or after co-op (`047`).
- **`Esc` is a reserved system key.** It can never be bound. It opens pause and, from there, the input menu and the
  bindings screen, from any device state.
- **`Enter`: proposed as reserved; pushback, not decided.** `Enter` is today's default keyboard key for `START`
  (`MegaControllerButton.kt:13`). If `Enter` is fully reserved:
  - `START` on the keyboard is either locked to `Enter` (a player cannot move it) or must move to another default
    key, which changes the default controls;
  - keyboard-only co-op later has one `Enter` for two players, so only one of them can have it as `START`.
  The proposal instead: `Enter` stays a normal key in gameplay (default `START`, bindable), but in the system screens
  (pause, input menu, bindings screen) it always means "confirm". When the bindings screen waits for a new key, a
  press of `Enter` binds `Enter`, as with any other key. With `Esc` (back) and `Enter` (confirm) always working in
  the system screens, a player with a broken map can still move out and reset.
- **Stick threshold: one constant, the minimal common value.** Proposal: a stick axis presses its d-pad direction
  when its value is at least `0.5` (of the range `-1` to `1`), checked for each axis on its own. A full diagonal
  gives about `0.71` on both axes, so it presses both directions. Values used elsewhere: RetroArch converts an
  analog axis to a digital press at `input_axis_threshold = 0.5` by default; XInput's suggested dead zones are
  about `0.24` (left stick, `7849` of `32767`) and about `0.12` for the triggers (`30` of `255`). A dead zone of
  `0.24` stops drift in analog games. For a digital press it is too low: a light touch would move Mega. Inference:
  `0.5` is the usual value for a digital press. A release threshold a little lower than the press threshold (for
  example `0.4`) stops flicker at the edge. It is optional; add it only if play tests show flicker.
- **Revert timer: not needed.** It is a "Keep these bindings?" prompt after a change that reverts the change if
  nobody confirms within a few seconds, as in display settings. It protects a player who has made confirm
  impossible to press. Here `Esc`, `Enter` in the system screens and the mouse already give a way out, and every
  button always keeps one code. So the timer adds UI for a case that the other layers already cover.

### Reserved keyboard keys and one device per player (2026-10-25)

Owner's decisions, after a discussion of the `Enter` question above. They replace the `Enter` proposal and the
"second keyboard key set" plan.

- **Reserved keys on the keyboard: `Esc`, `Enter` (and numpad `Enter`), and the four arrow keys.** The bindings
  screen refuses all of them and says why. The aim: back, `START` and the directions are always available on the
  keyboard, whatever the bindings are.
- **`Enter` and the arrows are fixed aliases, not bindings.** The bindings stay 1:1. `Enter` and numpad `Enter`
  (LibGDX `Keys.ENTER` and `Keys.NUMPAD_ENTER`) always give `START`. The arrow keys always give the d-pad. The
  default bindings stay `WASD` for the d-pad and `Enter` for `START`. If a player moves `START` to another key,
  `Enter` still gives `START` too.
- **Where the aliases are built: in the keyboard device adapter.** The adapter's held mask is the bound keys OR the
  fixed aliases. This happens before the OR per slot and before the press and release edges. So every reader sees
  one plain `START` or `UP` in the frame, and no gameplay or menu code reads `Enter` or the arrows directly. Example:
  the player holds `W` and then presses the up arrow: no second "just pressed".
- **`Esc` is a system key, not a game button.** In menus it means back. In gameplay it opens the pause screen. The
  code that adds this gets a `TODO` to consider another action for `Esc` in gameplay later. In co-op it pauses for
  both players (shared screen).
- **One device per player in co-op.** Two players never share a keyboard or a gamepad. This drops keyboard-only
  co-op and the "second keyboard key set" (`keyboard-2`) from the plans. The rule applies to co-op only. In single
  player, slot 1 keeps every device (Option B's auto mode): the keyboard and any number of gamepads work at the same
  time, and the keyboard stays the fallback when a gamepad is unplugged.
- **Gamepads get no fixed aliases.** They keep today's model: the default codes come from the library's mapping
  (`controller.mapping`) on connect, and the player changes them in the bindings screen. Reason: a fixed alias for
  the d-pad or `START` would come from the same mapping. For a gamepad that the mapping gets wrong, the alias would
  also be wrong, and the player could not fix it, because a fixed alias cannot be rebound. A binding that the player
  can change is safer. The stick threshold also reads the stick axes from that mapping, so the stick has the same
  limit.

### More binding answers (2026-10-25)

- The screenshot trigger stays `P` for now (owner). Filed: `docs/issues/2026-10-25-p-key-binding-takes-screenshots.md`.
- A `--resetBindings` launcher flag is wanted (layer 5 of the escape hatch).
- Gamepad triggers (`L2`, `R2`) can be bound, like any other button. They are axes, so the gamepad adapter turns
  them into a press with a threshold. Proposal: the same `0.5` as the stick (inference: XInput's suggested trigger
  threshold is about `0.12`, but a digital press wants a firmer pull).
- `selectButtonAction` defaults to `NEXT_WEAPON` for a slot (or later a profile) with no saved value. This is today's
  default.

### Last binding answers (2026-10-25)

- The touch overlay is `ScreenController`, the on-screen controller meant for a mobile build. It keeps a fixed
  layout. No mobile build exists, so it is skipped for now; the branch that builds the touch device adapter adds a
  `TODO` there.
- No migration (owner): the plan in "Persistence and migration" to import the old prefs is dropped. The new settings
  start from the defaults, and players set their bindings again once. Open question 10 (which old file's
  `selectButtonAction` wins) no longer applies. Item `034` `ITEM.md` is updated to match.

## Scan: the size of the work (2026-10-25)

Based on `master` at `48c602ed4`. Four read-only scans: the input pipeline and its timing, the gameplay readers,
screens and settings, and devices with tests and overlaps. Paths: `E` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C` = `core/src/main/kotlin/com/megaman/maverick/game/`.

Corrections to the review above:
- The poll is at `C/MegamanMaverickGame.kt:529` (not `:485`).
- `MegaCollisionHandler` does not read input itself. The read inside the collision step is the
  `FALL_THROUGH_PRESS_UP` lambda in `C/entities/megaman/components/BodyComponent.kt:81-83`, which
  `MegaCollisionHandler.kt:38-39` calls.
- "Slot 1" in these docs is `PlayerSlot(0)` in code (slots are 0-based).

### Summary

- **One poll per rendered frame.** `controllerPoller.run()` runs once per frame at `MegamanMaverickGame.kt:529`,
  before `screen.render` (so before every engine system and every physics sub-step) and before `eventsMan.run()`.
  There is no poll in the asset-loading frames. Every reader runs on the main thread between two polls. So an
  `InputFrame` built right after `run()` from the poller's statuses gives every reader the same data as today.
- **The frame must copy the poller's 4 states, not the raw devices.** The control lock (`on = false`) forces
  `JUST_RELEASED` and then `RELEASED` while a key is still down, and the unlock gives a new `JUST_PRESSED` for a key
  that is still held. Only the poller knows this.
- **Readers:** 56 explicit gameplay reads, plus 4 implicit ones (`ControllerSystem` dispatch for B, LEFT, RIGHT,
  SELECT) and 4 lines that read `selectButtonAction`. Plus one menu choke point (`MegaMenuScreen`) and 7 direct reads
  in other screens.
- **Risk is low for a first branch that keeps the poller as the source.** Most reads are in Megaman's code and only
  change where they read from.

### The pipeline today

- `ControllerButton.isPressed()` (`E/controller/buttons/ControllerButton.kt:21`) ORs the keyboard key, gamepad 0
  and the alternate actuators (touch). The OR happens before any edge.
- `ControllerPoller.run()` (`E/controller/polling/ControllerPoller.kt:20-47`), per button in enum order:

  | Case | Old status | New status |
  |---|---|---|
  | `on`, enabled | `RELEASED` or `JUST_RELEASED` | `JUST_PRESSED` if pressed, else `RELEASED` |
  | `on`, enabled | `PRESSED` or `JUST_PRESSED` | `PRESSED` if pressed, else `JUST_RELEASED` |
  | `on`, disabled | `JUST_RELEASED` | `RELEASED` |
  | `on`, disabled | any other | `JUST_RELEASED` |
  | `on = false` | `PRESSED` or `JUST_PRESSED` | `JUST_RELEASED` |
  | `on = false` | `RELEASED` or `JUST_RELEASED` | `RELEASED` |

  - A `JUST_*` state lasts exactly one frame. A tap that starts and ends between two polls is lost.
  - Quirk: a disabled button flips `RELEASED` and `JUST_RELEASED` every frame. No production code sets
    `enabled = false` (only `MegaControllerPollerTest`), so this is latent.
  - Before the first poll, `getStatus` returns `null`: readers see `false`, and `ControllerSystem` skips the key.
- `MegaControllerPoller.run()` (`C/controllers/MegaControllerPoller.kt:19-46`) checks gamepad 0 for connect and
  disconnect first, sets the gamepad codes, then calls `super.run()`. It logs a debug line every frame.
- **The control lock.** `TURN_CONTROLLER_OFF/ON` is handled in `MegamanMaverickGame.onEvent` (`:633-651`): it sets
  `controllerPoller.on` and, unless the event says otherwise, `ControllerSystem.on`.
  - 13 submitters: `MegaLevelScreen` `:286` (room transition), `:367`, `:619` (`PLAYER_READY`), `:788`
    (`END_BOSS_SPAWN`), `:806` (`BOSS_DEFEATED`: poller off, system kept on), `:836`, `:864`, `:1126` (`reset`);
    `PlayerSpawnEventHandler:124, 205`; `PlayerDeathEventHandler:54`; `EndLevelEventHandler:112`;
    `WilyFinalBoss:302`.
  - An event sent inside `eventsMan.run()` is delivered one frame later (`E/events/EventsManager.kt:29-31, 58-81`).
    So a lock change shows at the next poll or one poll later.
  - With the system kept on (`:806`), Megaman's actuators get `onJustReleased` for a held button (for example a
    held B fires the charged shot), then `onReleaseContinued`.
  - `LevelStateHandler.pause/resume` and `engine.systems.forEach { it.on = ... }` (`MegaLevelScreen.kt:428, 591,
    781, 790`) also switch `ControllerSystem`. After `END_BOSS_SPAWN`, the system runs one frame with every button
    `RELEASED` before the poller unlocks.
- **`ControllerSystem`** (`E/controller/ControllerSystem.kt:13-37`) calls exactly one actuator callback per key per
  frame, from the global poller, and passes the poller into each callback (`IButtonActuator` takes an
  `IControllerPoller`). Only Megaman has a `ControllerComponent` (`C/entities/megaman/components/ControllerComponent.kt:27-125`,
  actuators for B, LEFT, RIGHT, SELECT). The actuator map is an `ObjectMap` keyed by enum values, whose hash is the
  identity hash, so **the call order of the four actuators can differ between JVM runs**.

### The gameplay readers

(a) **The player's own input (Megaman): 44 explicit reads.**
- `ControllerComponent` 8 (all held, through the actuator's `poller` parameter), plus `selectButtonAction` at `:98`.
- The 8 behavior files: 31 reads (`ClimbBehavior` 11, `AirDashBehavior` 5 plus 2 `selectButtonAction`,
  `GroundSlideBehavior` 4, `JumpBehavior` 3, `JetpackBehavior` 3, `CrouchBehavior` 2, `WallSlideBehavior` 2,
  `SwimBehavior` 1). All in the Behaviors system, once per frame.
- `JetpackBehavior.kt:29-32` uses `allMatch(UP == PRESSED, A == JUST_PRESSED)`: UP must be held from an earlier frame.
  A frame with only held, pressed and released sets must express this as `held(UP) && !pressed(UP)`.
- `BodyComponent.kt:82`: UP **just pressed**, read inside the collision handler, so on every physics sub-step and
  for every `PRESS_UP_FALL_THRU` block.
- `AnimationsComponent.kt:93`, `MegamanAnimationKeySupplier.kt:118` (Animations system), `MegamanWeaponsHandler.kt:674`
  (from the attack actuator), `Megaman.kt:833` (B just released, Updatables system).

(b) **Other entities, about a player: 10 reads.**
- `PipePortal.kt:154-160` (held, in `TeleporterRule`, a contact rule) and `SpringBouncer.kt:209-216` (held, in
  `BouncerRule`). Both already resolve the contact player with `asPlayer`.
- `SpiderWeb.kt:185` (any of 6 buttons just released, Updatables) and `GroundSlideBlocker.kt:43-44` (DOWN held, in
  body `preProcess`). Both use `targetPlayer` (always slot 0). `GroundSlideBlocker` turns collision on or off for
  all bodies at once, so it cannot serve two players in co-op (a `047` problem, not a `031` one).

(c) **Global flow.**
- Pause (`MegaLevelScreen.kt:908-909`): START just released, or SELECT just pressed when `selectButtonAction ==
  START`. Uses `PlayerSlot(0)`.

Reads inside physics sub-steps (they see the same frame on every sub-step): `BodyComponent:82` (the only edge),
`GroundSlideBlocker:44`, `PipePortal:154-160`, `SpringBouncer:211-216`. If `010` later runs several ticks per frame,
an edge must go to one tick only; `010` owns that change.

Tests: no test checks how a behavior reacts to a button. Six core tests build `Megaman` with a relaxed mock game, so
`controllerPoller` and `selectButtonAction` return mock defaults and hide the input reads.

### Screens and settings

- **Menus:** all 9 live menu instances read input through `MegaMenuScreen` (`C/screens/menus/MegaMenuScreen.kt:19-31`):
  navigation on `isJustReleased` of the d-pad, confirm on START or A just released. There is no back input: every
  "BACK" is a row to confirm. `LevelSelectScreenV2` has no way back at all.
- **Pause menu** (`LevelPauseScreen`): confirming a weapon resumes the game; there is no dedicated "START unpauses".
- **Other screens:** 7 direct reads (`AbstractStoryScreen:110`, `CreditsScreen:89` (on press), `LogoScreen:129`,
  `GetWeaponScreen:244`, `SimpleInitGameScreen:59`, `GameOverScreen:85`, plus pause above).
- **No screen reads raw keys.** `Esc` is used nowhere. The arrow keys and numpad `Enter` do nothing today.
- **Default gamepad mapping** (`C/controllers/ControllerMappingExtensions.kt:6-13`): A = `buttonB`, B = `buttonY`,
  START = `buttonStart`, SELECT = `buttonA` (the south face button is SELECT, not confirm).
- **The bindings screen** (`ControllerSettingsScreen`, one class for keyboard and gamepad): capture swaps
  `Gdx.input.inputProcessor` (keyboard) or adds a listener on gamepad 0. Any key is accepted (no reserved keys,
  no cancel). A duplicate is swapped. Each rebind saves at once, "RESET TO DEFAULTS" does not save. No prefs are read
  at boot.
- **The touch overlay is dead code.** `screenController` (`MegamanMaverickGame.kt:218`) is never assigned, and
  `--showScreenController` sets a parameter that nothing reads. Its construction was removed in commit `274641680`.
- **What `UiInput` must cover:** navigate (4 directions, edge), adjust left/right, confirm, back (new), skip/advance,
  and the pause entry. The bindings screen needs raw device events, not frames.

### Devices and the library (`gdx-controllers` `2.2.3`, desktop Jamepad backend)

- About 13 places in 6 files assume gamepad index 0 (`ControllerUtils`, `ControllerButton`, `MegaControllerPoller`,
  `ControllerUtilsExtensions`, `ControllerSettingsScreen`, `GameSettingsPanel`).
- `Controllers.getControllers()` is the live list in connect order, up to 4 on desktop. Listeners run on the render
  thread. **Index shift:** if pad 0 is unplugged while pad 1 stays, pad 1 becomes index 0, and
  `MegaControllerPoller` never sees a disconnect, so it keeps pad 0's codes.
- `getUniqueId()` is a new random UUID for each connect. `getName()` is the only stable key; identical models share
  it. No GUID or vendor id.
- `getMapping()` is one shared mapping for every pad. It has no trigger fields. On desktop the triggers are axes 4
  and 5 (values 0 to 1). Sticks are axes 0 to 3 (about -1 to 1; the Y sign needs a check on real hardware).
- `gradle.properties` says `jamepadVersion=2.0.14.2`, but Gradle resolves `2.0.20.0` (inference, from the cache).

### Module placement

- Engine today: `ControllerSystem`, `ControllerComponent`, `ControllerPoller`, `IControllerPoller`, `ButtonStatus`,
  `ButtonActuator`, `ControllerButton(s)`, `ControllerUtils`. Core: `MegaControllerButton`, `SelectButtonAction`,
  `MegaControllerPoller`, prefs helpers, `ScreenController`, the settings screens.
- `InputFrame` is keyed by `MegaControllerButton`, so it goes in core (a new `input/` package), stored as plain
  `Int` masks (8 bits each; immutable, no allocation). An engine form waits for an engine caller (as in `014b`).
- `032` keeps `gdx-controllers-core` in the engine and removes `gdx-controllers-desktop`. The device adapters go in
  core and get the trigger axis codes injected, so they add no desktop dependency to the engine.

### Players API

`MegaPlayer` (`C/players/`) is a view. `input` can be a read-through property: `MegaPlayer.input` returns the frame
of its slot from a per-slot frame store owned by an input service. `MegaPlayers` builds views with `::MegaPlayer`,
so its factory must capture that service. Item `029` decided that input belongs to the slot, not to the character.

### Overlaps

- `012` (named freezes) replaces `TURN_CONTROLLER_*` with freeze reasons, and edits `MegamanMaverickGame.onEvent`
  like `031b` would. Do not run them in parallel.
- `013` (pause overlay) owns the pause flow later. `031` does the small fixes (the `Esc` pause entry, back to pause
  from the bindings screen).
- `034` persists the per-slot settings. The escape hatch must exist before `034` applies bindings at boot.
- `010` latches input per tick later. An immutable frame makes this simple.
- `016` (narrow services): expose input as a service, not as more `game` fields.
- `032`: the adapters must not add desktop dependencies to the engine. Deleting the engine's `ControllerButton` and
  `ControllerUtils` reads later helps `032`.
- `047` and `055` need `031a` first. `060` reads the same button frame.
- No branch is active in a worktree.

### Bug candidates (not filed yet)

1. The index shift above: after pad 0 is unplugged while pad 1 stays, the game keeps pad 0's codes for pad 1
   (from the library source; not seen in the game).
2. "RESET TO DEFAULTS" in the bindings screen is not saved, so "LOAD SAVED SETTINGS" brings the old codes back
   (`ControllerSettingsScreen.kt:251-258`, `ControllerUtilsExtensions.kt:57-70`).
3. If the gamepad disconnects during a capture, the screen leaves with the listener still attached and the capture
   state still set; the next visit shows the hint text (`ControllerSettingsScreen.kt:387-392`).
4. Inference, not run: after a rebind, the menu unlocks 0.25 s later. If the player binds START or A and holds the
   key longer, its release confirms the same row again and starts a new capture.
5. Inference, not run: if a focus-loss pause (`DesktopLauncher.kt:160-173`) happens while the controller is locked
   (for example during a room transition), the pause menu cannot be used, because it reads the same locked poller.
6. `jamepadVersion` in `gradle.properties` does not match the resolved jar (build).

Filed on 2026-10-25 (owner): 1 as `2026-10-25-gamepad-index-shift-keeps-old-codes.md`, 2 as
`2026-10-25-reset-to-defaults-not-saved.md`, 3 as `2026-10-25-capture-state-left-after-gamepad-disconnect.md`, 6 as
`2026-10-25-jamepad-version-mismatch.md` (the resolved version is confirmed: `2.0.14.2 -> 2.0.20.0`). 4 and 5 need a
check in the game first.

## Options for the first branch (2026-10-25)

All options keep the poller as the only source in the first branch, so the contract is "plays the same". The
devices (keyboard aliases, all gamepads, stick, triggers) and the bindings come in later branches.

### Option 1: frame and Megaman's reads only

Build `InputFrame` for slot 0 after `controllerPoller.run()`. Add `MegaPlayer.input` (first caller: the Megaman
behaviors). Move group (a) only.

- Pros: the smallest diff (about 14 files, all in `entities/megaman/`). Lowest risk.
- Cons: the other entities, pause and `ControllerSystem` still read the global poller, so the global stays in
  gameplay code. A second branch must still touch the same concepts.

### Option 2: frame and all gameplay reads (recommended)

Option 1, plus group (b) (`PipePortal` and `SpringBouncer` read the contact player's frame; `SpiderWeb` and
`GroundSlideBlocker` read `targetPlayer`'s frame, as today) and group (c) (pause reads the slot-0 frame). Menus,
skip screens and `ControllerSystem` stay on the poller.

- Pros: after the branch, no gameplay code reads `game.controllerPoller`; only the frame builder, `ControllerSystem`
  and the screens do. That is a clear line for the next branch. Every move is mechanical. The parity suite covers
  all of it.
- Cons: about 20 files. `selectButtonAction` still lives on `game` (it moves with the per-slot settings later).

### Option 3: Option 2 plus `ControllerSystem` and the lock

Also make `ControllerSystem` give each entity its owner's frame (an engine API change in `IButtonActuator`), and
turn `on` into a per-slot lock with the same release sequence.

- Pros: the poller is used only by the frame builder and the screens.
- Cons: it changes engine API and `ControllerSystemTest`, and the lock edits `MegamanMaverickGame.onEvent`, which
  `012` also changes. The actuator order is hash-ordered, which makes parity harder. A larger branch with more risk.

### Later branches (for all options)

- `031b`: `ControllerSystem` on frames, the per-slot lock (in step with `012`), menus and skip screens on `UiInput`
  with `Esc` as back and the pause entry.
- `031c`: device adapters in auto mode: the keyboard with the reserved aliases, every connected gamepad (fixing the
  index shift and the code-0 bug), the stick at `0.5`, the triggers on axes 4 and 5, touch with a `TODO`.
  Intended changes: the aliases, the stick and the bug fixes.
- `031d`: per-slot bindings for slot 0 (one settings object: tables by `keyboard` or pad name, plus
  `selectButtonAction`), the bindings screen (capture from one device, reserved keys refused, back to pause with no
  restart, key names on screen), and `--resetBindings`. In step with `034`.

### Verification for the first branch

- **Temporary suite** (deleted before the merge): a frozen copy of `ControllerPoller` and `MegaControllerPoller.run`
  in the test sources, and fake `IControllerButton`s driven by seeded scripts (held bits from 3 devices, `on`
  toggles, `enabled` toggles, connect and disconnect). Each tick, the status of each button from the frozen poller
  must equal the status read back from the new frame. Named cases: hold then lock, lock with the system kept on,
  unlock while held, press and lock in one tick, keyboard holds A and the pad presses A, the disabled-button flip.
- **Permanent suite:** frame edges and the `held && !pressed` case (`JetpackBehavior`); the frame is the same for all
  readers in a frame; a frame per slot is addressable.
- **Manual checks** in the game: jump, slide, climb, wall slide, air dash, jetpack, swim, the charged shot through a
  boss intro, pipe portals, spring bouncers, spider webs, ground-slide blockers, pause with START and with SELECT
  set to START.

### Recommendation

Option 2 for `031a`. Then `031b`, `031c` and `031d` as above. File the bug candidates 1, 2, 3 and 6 now; check 4 and
5 in the game first.

## Who drives an entity: driver, slot view and side (2026-10-26)

Proposal from the `031b` assessment. Not decided. Based on `master` at `95d1fb48c`.

### The owner's case

A boss fights Mega Man. Wily captures Mega Man. The boss changes sides and becomes playable. So the thing that drives
an entity can change: deterministic code (AI) or a player's input. The owner's model: the input driver changes from AI to
the player, and the boss gets a player view and becomes the entity of the slot.

### What the code does today

- `asPlayer` (`players/PlayerExtensions.kt`) means "this entity has a `MegaPlayer` view in its properties". It has 154
  uses in 65 files, with three meanings:
  - Side: "is this shot from the player's side?" (`AbstractEnemy.kt:246,259`, `AbstractBoss.kt:198`, `Water.kt:151`,
    `IceBomb`, `Snowhead`, `ReactorManProjectile`, ...). This is item `030`.
  - Player-body rules: world gravity (`MegaContactRules.kt:53`, `PortalHopper`, `CapsuleTeleporter`), the camera
    rotation check (`IBodyExtensions.kt:20`), items (`PlayerItemRule`).
  - Input and player state: `SpringBouncer`, `PipePortal`, `Rain` (`ready`), `PreciousGem` (`teleporting`).
  If a boss gets a view, all 154 checks change for it at the same time.
- `MegaPlayer` requires about 11 parts (`MegaPlayer.kt:30-44`). Some only `Megaman` has: `PlayerStateComponent`, the
  gravity-device switch, the light receiver, `IFocusable`.
- `Players.register` throws if the slot has a player. `unregister` does not remove the view property from the entity
  (`TODO` at `MegaPlayer.kt:47`).
- Only `Megaman` has a `ControllerComponent`.

### Common practice

Unreal: a `PlayerController` or an `AIController` possesses the same `Pawn`; "controlled by a human" is not a property
of the body. The slot lives longer than the body. See `029` `NOTES.md`, review 5 (`:850`, `:925-928`) and review 4
(`:734`).

### Proposed split into three concerns

1. **Driver: input that belongs to the entity (`031`).** A controlled entity holds an `InputSource`
   (`input/InputSource.kt`, a `fun interface` with a non-generic `read()`, so it does not box `InputFrame`). Readers
   ask the entity, not `asPlayer`. `ControllerSystem` asks the entity's component for the status of each key; it does
   not know about players. The engine needs only a status lookup by key that returns the enum. A slot source reads
   `players.inputs[slot]`.
2. **Slot binding: a thin view (`029`, `047`).** Possession is: unregister the old view (and clear its property),
   register a view on the new entity. The view holds only what every slot needs (`029` review 4: slot, targeting
   bounds, facing, health, focus), with capabilities for the optional parts. Today's `MegaPlayer` has Megaman's shape.
3. **Side: faction (`030`).** "On the player's side" becomes a faction. The side checks among the `asPlayer` uses
   move to faction; the player-body checks become capability checks.

By the rule "write code only when production code calls it", `031b` builds only the slot source. An AI source and
the possession API wait for the first entity that needs them.

### Can a boss get `PlayerStateComponent` after it spawns, and lose it later?

Mechanically yes; in meaning no.

- Mechanically: `GameEntity.components` is an `OrderedMap` keyed by class, and `addComponent` and `removeComponent`
  are map writes. No system mask contains `PlayerStateComponent`, so no `updateSystemMembershipsFor` call is needed.
  The component must be on the entity before `register`, because `MegaPlayer` reads it once in its constructor
  (`entity.require`). The view keeps that reference, so remove the component only after `unregister`.
- In meaning it does not work. The component is passive data. Only `Megaman`'s code honors it:
  - Its constructor takes `Megaman`'s `damageTimer` and `shootAnimTimer`. A boss needs its own timers that its own code
    updates, or `damaged` and `shooting` give wrong values.
  - The handlers write `ready`, `canBeDamaged`, `teleporting` and `movementAllowed` through the view. The boss's damage,
    movement and spawn code does not read them, so these locks have no effect on the boss. No error shows.
  - `damageListeners` fire only from `Megaman.kt:723`. `AbstractBoss.kt:135` adds each boss as a listener on the
    players.
  - The component has no `reset()` (the `IGameComponent` default does nothing), and `init()` runs only once. A pooled
    boss that is destroyed with the component still on it keeps the stale state at its next spawn. Remove it in the
    possession release or in `onDestroy`.
- So the type check passes but the contract is broken in silence. The work is in the boss code that honors the fields,
  not in the component.

### A boss with two modes: state machine as an enemy, behaviors when playable

The owner's variant: as an enemy, the boss runs today's state machine. When playable, it uses separate movement and
behaviors (sharing private helpers with the state machine).

- This is a mode switch on the boss, not a change of input source. In enemy mode the boss reads no input: the state
  machine writes the body directly and the input source is empty (`ControllerSystem` skips the entity). In playable
  mode the behaviors and actuators read the slot source. An AI source is needed only for a third mode: a CPU that
  drives the playable behaviors (for example a CPU partner).
- Two ways to switch:
  - Build both sets in `init()` and gate them by the mode: the state machine updatable returns early, and
    `setAllBehaviorsAllowed` and the actuators check the mode. No system membership changes. This is the safer way.
  - Add and remove `ControllerComponent` and `BehaviorsComponent` at the switch. These are in system masks, so each
    change must call `engine.updateSystemMembershipsFor(entity)`. Today only `GameEngine.spawnNow` (`:79`) calls it, so
    this is easy to forget.
- Shared private helpers (for example `fireShot()` and `jump()`), called both by state-machine states and by behaviors,
  keep the moves the same.
- On the switch, also: the faction (`030`), the slot view (thin, see above), the boss's health bar and the boss
  defeat flow (`AbstractBoss` must not run its defeat sequence for a playable boss).
