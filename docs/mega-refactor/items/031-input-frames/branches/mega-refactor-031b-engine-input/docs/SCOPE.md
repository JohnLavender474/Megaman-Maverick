# Scope: mega-refactor-031b-engine-input

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Make input frames an engine mechanism. The engine owns the frame, the input source and the button contract. The game
decides which buttons exist, their names and their mapping. Each controlled entity holds its own input source, and
`ControllerSystem` dispatches the actuators from that source, not from the global poller. The poller stays the only
source of input, so the game plays the same. The one exception is the actuator order (see "Intended changes").

After the branch, the input that drives an entity can be changed by giving the entity another `InputSource`. This is
the base for the later sources (replay, AI, network) and for possession (item `031` `NOTES.md`, "Who drives an
entity").

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes:

- From "Proposed scope": "Entities read their owner's frame."
- From `NOTES.md`, "Later branches": "`031b`: `ControllerSystem` on frames". The per-slot lock and `Esc` are not taken
  (see "Out of scope").
- From `NOTES.md`, "Long-term shape", design point 4 (owner, 2026-10-26): `InputFrame` and `InputSource` are engine
  mechanism; the engine adds `InputButton { val index: Int }`; `MegaControllerButton` implements it with an explicit
  `index`; the frame is a value class over a `Long` (2 bits per button, 32 buttons).
- Decision 2026-10-25: keep the button names; the frame is keyed by button, not by intent.

## Current state

Based on `master` at `aa97e6c68`. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- **The frame** (`C/input/InputFrame.kt`): a `@JvmInline value class` over an `Int`, 2 bits per
  `MegaControllerButton`, at `button.ordinal * 2`. Queries: `getStatus`, `isPressed`, `isJustPressed`,
  `isJustReleased`. `RELEASED` is the default. `of(poller)` builds a frame from the poller's statuses (`null` reads as
  `RELEASED`). It is in core because it is keyed by the core enum.
- **The store** (`C/input/PlayerInputs.kt`): one frame per slot in an `IntArray`, owned by `MegaPlayers`.
  `MegamanMaverickGame.kt:530-531` writes slot 0 right after `controllerPoller.run()`, on every frame where the
  poller runs. The same `if` block calls `screen.render` after it, so the engine never updates before the first poll.
- **`InputSource`** (`C/input/InputSource.kt`): a `fun interface` with `read(): InputFrame`. One caller:
  `MegaPlayer.input`, through `InputSource { inputs[slot] }` in `MegaPlayers`.
- **Megaman's reads** (44) go through `C/entities/megaman/MegamanInput.kt`: `Megaman.input = asPlayer!!.input`.
  Other entities (`PipePortal`, `SpringBouncer`, `SpiderWeb`, `GroundSlideBlocker`) and pause read `MegaPlayer.input`.
- **`ControllerSystem`** (`E/controller/ControllerSystem.kt`) takes the global `IControllerPoller`. For each entity,
  for each entry of the actuator map, it reads `poller.getStatus(key)` (skips `null`) and calls exactly one of
  `onJustPressed`, `onPressContinued`, `onJustReleased`, `onReleaseContinued`, passing the poller.
- **`ControllerComponent`** (`E/controller/ControllerComponent.kt`): `ObjectMap<Any, () -> IButtonActuator?>`. Only
  `Megaman` has one (`C/entities/megaman/components/ControllerComponent.kt:120-125`: B, LEFT, RIGHT, SELECT, each a
  constant supplier). The map keys are enum values, whose hash is the identity hash, so **the call order of the four
  actuators can differ between JVM runs**. `putActuator` and `removeActuator` have no callers.
- **`IButtonActuator` and `ButtonActuator`** (`E/controller/buttons/`) pass `IControllerPoller` to each callback.
  Since `031a`, Megaman's actuators ignore it (`{ _ -> ... }`, `{ _, delta -> ... }`). No other implementation exists.
- **Where the order can matter.** `runLeft`/`runRight` run on `onPressContinued` (LEFT or RIGHT held from an earlier
  frame; `onJustPressed` only logs) and set `facing` on each such frame. The B release calls `shoot()`. If other code
  changed `facing` after the controller ran in the previous frame (for example a behavior, or the `RODENT_CLAWS` rule
  in `runLeft`/`runRight`), the shot can use the value from before or after `runLeft`/`runRight`, depending on the
  order. Inference from the code; not seen in the game.
- **Menus and skip screens** still read `game.controllerPoller`: `MegaMenuScreen.kt:21-31` (8 menu classes),
  `GameOverScreen.kt:85`, `SimpleInitGameScreen.kt:59`, `GetWeaponScreen.kt:244`, `AbstractStoryScreen.kt:110`,
  `LogoScreen.kt:129`, `CreditsScreen.kt:89` (on press). The array queries (`isAnyJustReleased`,
  `isAnyJustPressed`) come from `IControllerPoller`.
- **Tests:** `ControllerSystemTest` (engine, 1 test, mocks the poller and checks the poller is passed),
  `InputFrameTest` (core, 12 tests, includes the equivalence with a real `ControllerPoller`), `MegaPlayersTest`.

## In scope

1. **Engine input package** (`E/input/`, name to confirm in the plan):
   - `interface InputButton { val index: Int }`.
   - `InputFrame`: a `@JvmInline value class` over a `Long`, 2 bits per button at `index * 2`, 32 buttons. The same
     four queries, taking `InputButton`. `RELEASED` default. A way to build a frame from a status per button (the
     plan decides the exact form; it must not allocate).
   - `InputSource` (moves from core, unchanged).
   - A check of a button set (each `index` unique and in `0 until 32`), called once by core at startup.
2. **Core buttons:** `MegaControllerButton` implements `InputButton` with an explicit `index` equal to today's
   `ordinal` (UP 0, DOWN 1, LEFT 2, RIGHT 3, A 4, B 5, START 6, SELECT 7). So the bit positions do not change.
3. **Core frame store:** `PlayerInputs` keeps `Long` frames. The builder in `render` builds the slot-0 frame from the
   poller, as today.
4. **The entity's own source:** `ControllerComponent` holds the entity's `InputSource` (empty means "no driver"; the
   system skips the entity). The slot view binds it: when `MegaPlayer` is built for an entity, it sets the entity's
   source to its slot source, and `MegaPlayer.input` reads that same source.
   `Megaman.input` (`MegamanInput.kt`) reads the entity's own source, not `asPlayer`.
5. **`ControllerSystem`** takes no poller. Per entity it reads the source once per update, then dispatches per button
   with the same status-to-callback rule.
6. **Actuators indexed by button:** `ControllerComponent` holds its actuators in an array indexed by
   `InputButton.index`, and the system calls them in index order. The callbacks lose the `IControllerPoller`
   parameter (`IButtonActuator`, `ButtonActuator`, Megaman's four actuators).
7. **Tests:** `InputFrame` read-back for each status at each index including 31 (the high bits of the `Long`), the
   default frame, the button-set check; `ControllerSystemTest` on a source (each status calls the right callback; an
   entity with no source is skipped; the call order is the index order); the core equivalence with the poller stays
   (`InputFrameTest`, adapted); `MegaPlayersTest` (the view binds the entity's source).

New APIs and their first callers:

| API | First callers |
|---|---|
| `InputButton` | `MegaControllerButton`; the `InputFrame` queries |
| Engine `InputFrame` (`Long`) and its queries | the existing readers (unchanged call sites), `ControllerSystem` |
| The frame build from statuses | the slot-0 builder in `render` |
| The button-set check | core startup (`MegamanMaverickGame`) |
| `ControllerComponent`'s source | `ControllerSystem`, `MegaPlayer` (binds and reads), `Megaman.input` |
| Actuators indexed by button | Megaman's `defineControllerComponent` |

## Out of scope

- **The per-slot lock** (`TURN_CONTROLLER_*`, `controllerPoller.on`). It needs held bits per source and one edge
  builder; that is `031c` or `012` (item `NOTES.md`, design point 1).
- **`Esc`** (back in menus, pause in gameplay), and any back action in menus. A later branch or the start of `031c`.
- **Devices**, aliases, the stick, triggers, touch, the poller's removal (`031c`).
- **Bindings**, `selectButtonAction` per slot (`031d`).
- **An AI, replay or network source**, the update step for sources that compute a frame, possession (moving a slot to
  another entity), the thin view, faction. They wait for their first caller (item `NOTES.md`, "Proposed order").
- **`PlayerStateComponent`** changes (`029`/`012` follow-up).
- **The menus and skip screens**, unless open question 1 adds them.

## Intended changes

- **The actuator order becomes fixed:** LEFT, RIGHT, B, SELECT (index order). Today it is the hash order of the map,
  which can differ between runs. Each run already uses one of the possible orders, so no new behavior appears; the
  game stops changing between runs. With B after LEFT and RIGHT, a shot always uses the `facing` that `runLeft` or
  `runRight` set in the same frame (see "Where the order can matter").

## Affected areas and risks

- Engine: the new input package, `ControllerSystem`, `ControllerComponent`, `IButtonActuator`, `ButtonActuator`,
  `ControllerSystemTest`. Core: `input/` (`InputFrame` and `InputSource` leave), `MegaControllerButton`,
  `PlayerInputs`, `MegaPlayer(s)`, `MegamanInput.kt`, Megaman's `ControllerComponent.kt`, `MegamanMaverickGame`
  (builder, startup check, `ControllerSystem(...)`), the imports of about 20 reader files.
- **Bit layout:** `index * 2` on a `Long` must use `Long` shifts and masks (`1L`, `ushr` on `Long`). An `Int` shift
  at index 16 or more would be wrong in silence. The test at index 31 covers it.
- **Status `null` versus `RELEASED`:** today `ControllerSystem` skips a `null` status; the frame has no `null`.
  The engine never updates before the first poll (see "Current state"), so the skip never happens in the game.
- **The binding of the source by the view:** `MegaPlayer` must find `ControllerComponent` on the entity. `Megaman`
  adds it in `init()`, before `players.register` (`MegamanMaverickGame.kt:397-407`).
- **The order change** above.
- **Performance:** no allocation per read. The frame stays a primitive (`Long`); `InputSource.read()` is not
  generic; the dispatch walks an array.

## Overlap with other branches

No branch is active in a worktree. `012` (the lock) is not touched. `016` (narrow services): no new `game` field
unless open question 1 adds one. `032` (platform-neutral engine): the new engine package has no device code.

## Open questions

1. **Menus and skip screens in this branch?** Moving them to a frame is parity (the frame copies the poller, lock
   included). But they need a source that is not a player: which slot drives the menus is a `047` question.
   Proposal: leave them out of `031b`; they move with `Esc` and the back action in one menu branch, where the menu
   input design is the topic. Other choice: move them now to a `uiInput: InputSource` that reads slot 0's frame (a
   new field on `game`, with a `TODO` for `016`).
2. **The actuator supplier.** `ControllerComponent` stores `() -> IButtonActuator?`, and every caller passes a
   constant. Proposal: store the actuator itself (no supplier), and remove `putActuator` and `removeActuator`
   (no callers).
3. **Where the source lives:** on `ControllerComponent` (proposal: "this entity is driven by input"), or in its own
   engine component (an entity could read input with no actuators). Proposal: `ControllerComponent`, because the only
   entity with input has one; split when a real caller needs it.
4. **Who binds the source:** the slot view when it is built (proposal: this is the start of possession, and
   `unregister` can clear it later), or `Megaman` itself in `init()`.
