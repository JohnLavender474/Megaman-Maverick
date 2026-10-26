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
