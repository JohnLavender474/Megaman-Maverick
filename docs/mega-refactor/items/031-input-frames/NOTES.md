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
