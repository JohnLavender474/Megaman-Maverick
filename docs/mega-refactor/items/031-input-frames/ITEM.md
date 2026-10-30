# 031: Input frames per player (`InputFrame`), devices and bindings

- Status: active
- Type: refactor
- Depends on: `029`
- Related: `006`, `008`, `010`, `011`, `012`, `032`, `034`, `047`, `055`, `082`
- Branches: `mega-refactor-031a-input-frame` (merged), `mega-refactor-031b-engine-input` (merged), `mega-refactor-031c-input-pipeline` (merged); `mega-refactor-031d-devices` (planned), `mega-refactor-031f-menu-input` (blocked by `031d` and `031e`); next: `031d` (devices), `031e` (per-slot bindings), then `031f` (the menu-input branch)

## Goal

Each player reads one immutable input frame per tick, built from input sources, instead of polling a global controller.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `InputFrame` (held, pressed and released per action, plus axes), built once per tick at today's poll point.
- Sources (keyboard, pad N, touch, replay, AI). A router assigns them to player slots. A join screen for co-op.
- Entities read their owner's frame. Menus use `UiInput`. The collision handler reads no input.
- A control lock per player with today's release sequence.
- Record and replay of frames (with `010` and `011`).

## Owner's input

Answer to review item 30 (2026-10-22, verbatim):

> yes, let's discuss further

## Decisions

- H (2026-10-22): co-op comes in steps from easiest to hardest: shared screen, then split screen, then online. The input design must not block split screen or online lockstep.
- 2026-10-25: keep the button names (`A`, `B`, `START`, `SELECT`, the d-pad). The controls mirror the NES
  controller, and the names make it easy to know which button is which. The frame is keyed by button, not by
  intent, so the controller is not coupled to intents. What a button does stays the job of the code that reads it.
- 2026-10-25: analog sticks use a digital threshold only. No character moves faster or slower than with the d-pad.
  So `moveX` and `moveY` are not needed; the frame holds the four d-pad buttons like the other buttons.
- 2026-10-25: shared-screen co-op has at most 2 players. Split screen and online may need 4. The input router and
  the frames must support 4 slots, even if the first co-op step uses 2.
- 2026-10-25 (bindings): two players with the same gamepad model must be able to have different maps, so a binding
  belongs to the player, not only to the device model.
- 2026-10-25 (bindings): `selectButtonAction` is one setting per player.
- 2026-10-25 (bindings): one key per button (1:1). The stick reaches the d-pad through the fixed threshold, not
  through a binding.
- 2026-10-25 (bindings): leaving the bindings screen during a level goes back to pause, with no restart (issue
  `2026-10-25-bindings-screen-restarts-level.md`).
- 2026-10-25: the screenshot logic takes a generic trigger, not the `P` key; the launcher gives the trigger for now,
  with a `TODO` to update it later.
- 2026-10-25 (bindings): bindings and `selectButtonAction` are per slot now, kept in one settings object per slot.
  Profiles (a named copy of that object) wait for a feature branch during or after co-op (`047`).
- 2026-10-25 (bindings): `Esc` is a reserved system key and can never be bound.
- 2026-10-25 (bindings): the stick uses one constant threshold (proposal `0.5` per axis; see `NOTES.md`). No
  revert timer.
- 2026-10-25 (bindings): reserved keyboard keys: `Esc`, `Enter`, numpad `Enter` and the four arrow keys. The bindings
  screen refuses them. `Enter` and numpad `Enter` always give `START`, and the arrows always give the d-pad, as fixed
  aliases built in the keyboard device adapter (the bindings stay 1:1; defaults stay `WASD` and `Enter`). `Esc` is a
  system key: back in menus, pause in gameplay (with a `TODO` to consider another action later).
- 2026-10-25: one device per player in co-op; two players never share a keyboard or a gamepad. No keyboard-only
  co-op. In single player, the player can use any number of devices at the same time (auto mode).
- 2026-10-25 (bindings): gamepads get no fixed aliases. Default codes come from the library mapping on connect, and
  the player customizes them, as today.
- 2026-10-25: in single player, an unplugged gamepad keeps today's behavior: no pause, and the keyboard (and any
  other device) keeps working. Co-op differs because there it is not safe to guess which device belongs to which
  player as a fallback.
- 2026-10-25: the co-op input flows are not built in `031`. They move to `047` (the shared-screen co-op branch):
  the join step ("press `START`" claims a device), one claimed device per player, the pause and "press `START` on any
  free device" prompt when a player's gamepad is unplugged, and reconnect. Reason: `MegaPlayers` has capacity 1
  until `047`, so this code would have no production caller in `031` (rule "write code only when production code
  calls it"). `031` builds what single player uses: `InputFrame` for slot 1, the device adapters (keyboard with the
  reserved aliases, each gamepad, touch) in auto mode, and per-slot bindings for slot 1. Its design must not block the
  `047` flows.
- 2026-10-25: `P` stays the screenshot trigger for now (with `--allowScreenshots`). It can be changed or removed
  later. The conflict with a player binding on `P` is filed as `docs/issues/2026-10-25-p-key-binding-takes-screenshots.md`.
- 2026-10-25 (bindings): add a `--resetBindings` launcher flag (the last-resort escape hatch).
- 2026-10-25 (bindings): gamepad triggers can be bound, and act like any other button (through a threshold, because
  they are axes; see `NOTES.md`).
- 2026-10-25 (bindings): `selectButtonAction` defaults to `NEXT_WEAPON` when a slot (or later a profile) has no
  saved value. This is today's default (`MegamanMaverickGame.kt:287`).
- 2026-10-25 (bindings): the touch overlay (`ScreenController`, the on-screen controller meant for mobile) keeps a
  fixed layout, with no rebinding. No mobile build exists yet, so it is out of scope: the branch that builds the
  touch device adapter adds a `TODO` there for the mobile work.
- 2026-10-25 (bindings): no migration of the old binding prefs. The new settings start from the defaults, and
  players set their bindings again once. The old prefs files are left on disk, unused.
- 2026-10-26: the rest of `031` splits into a parity branch and two branches with intended changes. `031c`: the new
  input pipeline with the same behavior (device adapters for the keyboard and gamepad 0, one held mask per device, an
  engine edge builder that holds the lock, the poller removed; proved by a temporary contract suite against a frozen
  copy of the poller). `031d`: devices (the keyboard aliases, every connected gamepad with the index-shift and code-0
  fixes, the stick at `0.5`, the triggers). `031e`: bindings (the content planned before as `031d`). A later branch
  moves the menus and skip screens to frames, with `Esc` and the back action.
- 2026-10-26 (for `031c`): `ControllerButtons` stays as the storage of the key and gamepad codes only, so the bindings
  screen does not change (`031e` replaces the storage); the menus and skip screens read a thin `IControllerPoller`
  facade over the slot-0 frame until the menu-input branch; the edge builder drops the `enabled` flag (no production
  code sets it); the lock moves to the edge builder and is still switched by `TURN_CONTROLLER_*` (`012` replaces the
  events later); the edge builder goes in the engine and the device adapters in core, so the engine reads no device.

## Open questions

- None of the binding questions in `NOTES.md` is still open (see "Decisions").

## Sources

- `NOTES.md` in this folder.
