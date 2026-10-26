# Scope: mega-refactor-031a-input-frame

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-25

## Goal

Each player slot gets one `InputFrame` per frame: the status of each of the 8 buttons, copied from today's poller
right after it polls. Every gameplay input read moves from the global `game.controllerPoller` to the frame of the
right player: Megaman reads his own frame, entities that react to a player read that player's frame, and pause reads
slot 0's frame. The poller stays the only source of input, so every reader sees the same data as today. The game
plays the same.

After the branch, no gameplay code reads `game.controllerPoller`. Only the frame builder, `ControllerSystem` and the
screens read it. The later branches (`031b` to `031d`) replace the poller behind the frame without touching the
readers again.

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes:

- From "Proposed scope": "`InputFrame` (held, pressed and released per action, plus axes), built once per tick at
  today's poll point" and "Entities read their owner's frame". The axes are dropped by the 2026-10-25 decision
  (analog sticks use a digital threshold only).
- Decision 2026-10-25: keep the button names; the frame is keyed by button, not by intent.
- Decision 2026-10-25: the frames support 4 slots as a design constraint. Only slot 0 has a frame writer and a
  player today (`MegaPlayers` capacity 1).
- From `NOTES.md`, "Options for the first branch": Option 2 (owner, 2026-10-25).

## Current state

The full scan is in the item `NOTES.md`, "Scan: the size of the work (2026-10-25)", on `48c602ed4`. Summary
(`C` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E` = `engine/src/main/kotlin/com/maverick/game/engine/`):

- `controllerPoller.run()` runs once per rendered frame at `C/MegamanMaverickGame.kt:529`, before `screen.render`
  (so before every engine system and every physics sub-step) and before `eventsMan.run()`. There is no poll in the
  asset-loading frames. All readers run on the main thread between two polls.
- `ControllerPoller.run()` (`E/controller/polling/ControllerPoller.kt:20-47`) keeps one `ButtonStatus` per button
  (`PRESSED`, `RELEASED`, `JUST_PRESSED`, `JUST_RELEASED`). The control lock (`on = false`) forces `JUST_RELEASED`
  and then `RELEASED` on held buttons; the unlock gives a new `JUST_PRESSED`. Only the poller knows this, so the
  frame copies the poller's statuses, not the raw devices.
- The readers query the poller with `isPressed` (`PRESSED` or `JUST_PRESSED`), `isJustPressed`, `isJustReleased`,
  `isAnyJustReleased`, `areAllPressed` and `allMatch` (`E/controller/polling/IControllerPoller.kt`).
- Gameplay readers (the scan's groups):
  - (a) Megaman, 44 explicit reads: `ControllerComponent` 8 (through the actuator's `poller` parameter), the 8
    behavior files 31, `BodyComponent.kt:82` (UP just pressed, inside the collision handler on every physics
    sub-step), `AnimationsComponent.kt:93`, `MegamanAnimationKeySupplier.kt:118`, `MegamanWeaponsHandler.kt:674`,
    `Megaman.kt:833`.
  - (b) Other entities, 10 reads: `PipePortal.kt:154-160` and `SpringBouncer.kt:209-216` (contact rules; both
    already resolve the contact player with `asPlayer`), `SpiderWeb.kt:185` and `GroundSlideBlocker.kt:43-44`
    (both use `targetPlayer`, which is always slot 0).
  - (c) Pause, 2 reads: `MegaLevelScreen.kt:908-909`.
- `JetpackBehavior.kt:29-32` uses `allMatch(UP == PRESSED, A == JUST_PRESSED)`: UP must be held from an earlier
  frame. The frame must give the exact status, not only "held".
- `MegaPlayer` (`C/players/MegaPlayer.kt`) is a view over the player entity. `MegaPlayers` builds the views with
  `::MegaPlayer` (`C/players/MegaPlayers.kt:7`). No input lives on a player today.
- No test checks how a behavior reacts to a button. Six core tests build `Megaman` with a relaxed mock game, so the
  input reads return mock defaults.

## In scope

1. **`InputFrame`** in a new core package `input/`: an immutable value that holds one `ButtonStatus` per
   `MegaControllerButton`. Proposed form: a `@JvmInline value class` over one `Int` (2 bits per button, 16 bits),
   so a frame is never allocated. The default frame has every button `RELEASED` (note: `RELEASED` is not ordinal 0
   in `ButtonStatus`). Its query methods are the ones the moved readers use, with the poller's meaning:
   `getStatus`, `isPressed`, `isJustPressed`, `isJustReleased`, `isAnyJustReleased`, `areAllPressed`.
2. **The frame store and the builder.** The players service keeps one frame per slot. Right after
   `controllerPoller.run()` at `MegamanMaverickGame.kt:529`, the game copies the poller's statuses into slot 0's
   frame, on every frame where the poller runs (also while paused). On a frame with no poll, the frame keeps its
   last value, as the poller keeps its statuses.
3. **`MegaPlayer.input`**: reads the frame of its own slot from the store. The view holds no input state itself.
4. **The readers move**, one for one (the same method and the same button, on the frame in place of the poller):
   - (a) Megaman's 44 reads read Megaman's own frame, through one accessor in the player package.
     `JetpackBehavior`'s `allMatch` becomes two `getStatus` checks with the same statuses.
   - (b) `PipePortal` and `SpringBouncer` read the contact player's frame (the `asPlayer` they already resolve).
     `SpiderWeb` and `GroundSlideBlocker` read `targetPlayer.input` (the same player as today: slot 0).
   - (c) Pause reads the frame of the `player` that `MegaLevelScreen` already holds (slot 0).
5. **Tests:**
   - `InputFrame`: each status of each button reads back the same; each query method gives the poller's answer for
     each status.
   - Equivalence with the poller: a real `ControllerPoller` driven by fake `IControllerButton`s and seeded scripts
     (presses from several buttons, `on` toggles, `enabled` toggles). On every tick, for every button, each query
     on the built frame equals the same query on the poller. Named cases: hold then lock, unlock while held, press
     and lock in the same tick, the disabled-button flip.
   - A player's `input` returns the frame of its slot.

New APIs and their first callers:

| API | First callers |
|---|---|
| `InputFrame` and its query methods | the moved readers (each method has at least one caller: `getStatus` in `JetpackBehavior`, `isAnyJustReleased` in `Megaman.kt` and `SpiderWeb`, `areAllPressed` in `JetpackBehavior`, the others in the behaviors and pause) |
| Building a frame from the poller | `MegamanMaverickGame.render` |
| The per-slot frame store in the players service | the builder (write), `MegaPlayer.input` (read) |
| `MegaPlayer.input` | `PipePortal`, `SpringBouncer`, `SpiderWeb`, `GroundSlideBlocker`, pause, Megaman's accessor |
| Megaman's own-frame accessor (player package) | the 8 behaviors, `ControllerComponent`, `BodyComponent`, `AnimationsComponent`, `MegamanAnimationKeySupplier`, `MegamanWeaponsHandler`, `Megaman.kt` |

## Out of scope

- `ControllerSystem` and the actuator dispatch: it still calls Megaman's actuators from the poller's statuses
  (`031b`). The 8 explicit reads inside the actuators move to the frame; the dispatch does not.
- The control lock (`TURN_CONTROLLER_OFF/ON`, `controllerPoller.on`) and `enabled` (`031b`, in step with `012`).
- Menus, the pause menu's navigation, the skip screens, `UiInput`, `Esc` (`031b`).
- Devices: keyboard aliases, every gamepad, the stick, the triggers, touch (`031c`). The dead `ScreenController`
  keeps reading the poller to draw itself.
- Bindings, the bindings screen, `--resetBindings`, `selectButtonAction` per slot (`031d`).
  `game.selectButtonAction` stays on `game`.
- `GroundSlideBlocker`'s global collision toggle, which cannot serve two players (`047`).
- The open input issues (they belong to `031c`, `031d` or `034`).
- Slots 2 to 4 writers, a router, the join step (`047`).

## Affected areas and risks

- About 20 files: the player package (8 behaviors, `ControllerComponent`, `BodyComponent`, `AnimationsComponent`,
  `MegamanAnimationKeySupplier`, `MegamanWeaponsHandler`, `Megaman.kt`), `PipePortal`, `SpringBouncer`,
  `SpiderWeb`, `GroundSlideBlocker`, `MegaLevelScreen`, `MegamanMaverickGame`, the players package, and the new
  `input/` package.
- **A wrong move** (another method or another button) changes a control. The moves are one for one, so a review of
  the diff call by call checks them; the plan gives each group its own step and check.
- **`isPressed` includes `JUST_PRESSED`.** The frame's `isPressed` must keep that meaning.
- **`JetpackBehavior`** needs the exact `PRESSED` status for UP.
- **Timing:** the frame must be built after the poll and before `screen.render`, and only there. Building it again
  later in the frame (for example after `eventsMan.run()`) would let a lock change act in the same frame.
- **The default frame:** before the first poll the poller has no statuses (readers see `false`). The default frame
  must also give `false` for every query (every button `RELEASED`).
- **The contact readers** run inside physics sub-steps. They keep seeing one frame for the whole frame, as today.
- **Tests that build `Megaman`** with a relaxed mock game do not call the readers, so they keep passing. They do not
  prove the moves; the equivalence suite and the play test do.
- **Contract:** the manual checks need rooms for the special entities (`PipePortal`, `SpringBouncer`, `SpiderWeb`,
  `GroundSlideBlocker`) and for the movements (jump, slide, crouch, climb, wall slide, air dash, jetpack, swim,
  fall-through blocks), plus pause with START and with SELECT set to START. The contract step decides which rooms
  go in a new `RefactorTest031.tmx` and which existing rooms (for example `RefactorTest029` `movement`) are reused.

## Overlap with other branches

No branch is active in a worktree. Items that touch the same code later: `012` (the lock in
`MegamanMaverickGame.onEvent`; not touched here), `013` (pause; only the pause read moves here), `016` (the frame
store lives in the players service, not as a new `game` field), `010` (an immutable frame makes a later per-tick
latch simple).

## Decisions on the open questions

Approved with the scope (owner, 2026-10-25), as proposed:

1. `InputFrame` is a `@JvmInline value class` over one `Int` (2 bits per button). No allocation.
2. The frame store lives in `MegaPlayers` (input belongs to the slot). `MegaPlayer.input` reads its slot's frame.
3. No frozen copy and no temporary suite in `031a`. The equivalence suite compares the frame with the live poller
   and stays while the poller exists. The temporary suite with a frozen copy comes in `031c`.
