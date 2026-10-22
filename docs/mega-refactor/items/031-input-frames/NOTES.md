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
