# Scope: mega-refactor-047a-second-slot

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "1. yes, 2. master, 3. yes, 4. yes")

## Goal

With `--players 2`, a second `Megaman` plays in a level on its own input device. Without the flag, the game does not
change.

This branch makes player 2 exist. It does not make co-op correct. The rules for targeting, friendly fire and pickups
are in `047b`.

## Item

`047`: Co-op with multiple controllers. This branch takes these decisions, copied from `ITEM.md`:

- 2026-10-23: "the target is 2 players; the design must scale to 4 or more."
- 2026-11-01: "this item holds phase 1 of `O2`: two players on one machine, on one shared screen."
- 2026-11-01: "for the first pass, each player is a copy of `Megaman`."
- 2026-11-01: "for now, the death of slot 0 resets the level for all players. [...] Only the first option is built now.
  The other two are `TODO`s."
- 2026-11-01: "for now, player 2 has a debug tint as a marker."

Friendly fire (decision 2026-11-01) is in `047b`. The join step, one claimed device per player and the unplug prompt
(decision 2026-10-25) are in a later branch.

## Current state

### Slots and input

- `MegamanMaverickGame.kt:236` builds `MegaPlayers()` with capacity 1. Two tests assert this value:
  `GamePlayersFieldsTest.kt:30` for the game, and `MegaPlayersTest.kt:50` for the default.
- `postCreate()` builds one `Megaman`, adds its `PlayerProgressComponent` as a game state listener, and registers it in
  slot 0 (`MegamanMaverickGame.kt:439-448`). `MegaPlayers.register` already binds the input of the slot.
- One `InputFrameBuilder` exists for slot 0 (`MegamanMaverickGame.kt:224`). It runs once per render (`:573`).
- `TURN_CONTROLLER_OFF` and `TURN_CONTROLLER_ON` set its `locked` value (`:681`, `:690`).
- `GamepadInput.held()` puts all connected pads into one mask. It takes the bindings from `pads[0]`.
- `KeyboardInput` and `GamepadInput` are built for slot 0 (`:316-317`). `ControllerSettingsScreen` uses
  `game.gamepadInput` (`ControllerSettingsScreen.kt:201`).
- Menus and pause read `MegaPlayers.menuInput`, which is slot 0.
- `PlayerSettingsStore(slot)` already keeps one file per slot.

### Spawn

- `PlayerSpawnEventHandler` acts on slot 0: the play phase, the locks, the gravity and the beam.
- The screen spawns slot 0 at `PLAYER_SPAWN_INIT` (`MegaLevelScreen.kt:548`).
- `Megaman.onSpawn` submits `PLAYER_SPAWN` (`Megaman.kt:355`). This event name appears in 25 files, and many of them
  use it as a level reset. A second spawn would run these resets a second time.
- `Megaman.onSpawn` also calls `services.gravity.set(...)`, which writes the direction of every player
  (`Megaman.kt:322`, `WorldGravity.kt:13`). Slot 1 repeats the value of slot 0, so this is safe at spawn.

### Death

- `Megaman.onDestroy` submits `PLAYER_JUST_DIED` on every destroy, also when the destroy is not a death
  (`Megaman.kt:364`).
- The screen handles this event with no slot check: it stops the music, removes a life and starts the death handler
  (`MegaLevelScreen.kt:569`). `PlayerDeathEventHandler` then acts on slot 0.
- So a destroy of slot 1 runs the full death flow on slot 0.
- `GauntletMaster.kt:168`, `SpiderWeb.kt:137`, `PreciousGem.kt:214` and `PreciousGemCluster.kt:109` also act on any
  `PLAYER_JUST_DIED`, with no slot check.

### Level end

- `EndLevelEventHandler` locks slot 0 and sets its play phase (`EndLevelEventHandler.kt:114-115`, `:166`).
- `MegaLevelScreen.reset()` calls `engine.reset()` (`:1069`).

### Sprites

- The main sprite gets its alpha in a pre-process step (`SpritesComponent.kt:130`). No player sprite has a tint today.
- `Megaman.shouldHideSprite()` hides a player that is not in play (`SpritesComponent.kt:102`).

## In scope

1. **Capacity:** the game builds `MegaPlayers(2)`. Slot 1 stays empty without the flag, so all player loops act as
   they do today. `GamePlayersFieldsTest` changes to 2. The default of the `MegaPlayers` constructor stays 1, because
   the tests use it.
2. **Launch flag:** `--players <1|2>`, default 1. `resolveLaunchOptions` checks the value, and a bad value exits with
   code 2, as the other dev options do.
3. **Slot 1 entity:** with the flag, `postCreate()` builds, initializes and registers a second `Megaman` in slot 1, with
   its own game state listener.
4. **Frames:** one frame builder per registered slot. `TURN_CONTROLLER_*` lock and unlock all of them. `inputLocked`
   keeps its meaning.
5. **Devices with `--players 2`:**
   - Slot 1 gets the last connected pad.
   - Slot 0 gets the keyboard and all other pads.
   - With 1 pad: the keyboard is player 1, and the pad is player 2.
   - With 2 pads: pad 0 and the keyboard are player 1, and pad 1 is player 2.
   - With no pad: player 2 stands still, and the game logs an error.
   - The split is read each frame. If a second pad connects during play, the first pad moves from player 2 to player 1,
     and the new pad goes to player 2. If a pad disconnects, the split changes again. A `TODO` at the split names this
     edge case and points to the device claim in `047f`.
   - "Last" is the last entry of `Controllers.getControllers()`. Check at plan time that this order is the connection
     order.
   - A second key set on one keyboard is not in scope. The decision of 2026-10-25 says that two players never share a
     keyboard.
6. **Spawn:** slot 1 spawns in `PLAYER_SPAWN_INIT`, at a point that the map can set.
   - A `player` layer spawn object can have an object property named after a player: `player_2` for slot 1. It points
     to a rectangle object in a new layer, `player_slot_spawns`.
   - This layer has no builder, so the map loader skips it and its objects never become checkpoints. The live code
     does not read the layer name: any layer with no builder works, for example `children` or `temp`. The new layer is
     a choice for clarity in Tiled. A layer with a builder does not work, because its builder acts on the object.
   - References across layers already work: the `.tmx` files have 1923 of them, for example `enemies` to `children`.
   - With no reference, slot 1 spawns 1 tile to the right of the slot 0 point. "Right" is relative to the spawn's
     `direction` (default `up`), through `direction.getRotatedClockwise()`. So the 174 live spawns need no edit to load.
   - Slot 1 takes all other spawn props from the slot 0 spawn, for example `direction` and `facing`. Only the bounds
     differ.
   - The game does not check for a block or a pit at the slot 1 point. A map author adds a reference where the default
     is bad.
   - The phase, lock and gravity steps of `PlayerSpawnEventHandler` apply to all players. The beam stays on slot 0, and
     slot 1 appears when the beam ends. Only slot 0 submits `PLAYER_SPAWN`, so the level resets run once.
   - For more players, the same rule uses `player_3` and `player_4`. The default for a slot is `slot.index` tiles to the
     right. The code reads the key from the slot index, so this needs no new mechanism. A join during play does not use
     these objects (`047f`).
7. **Death, first option only:**
   - The death of slot 0 runs the current flow. Slot 1 is destroyed with it, and it spawns again with slot 0.
   - The death of slot 1 takes it out of play until the next reset. It removes no life and starts no death flow.
   - The screen ignores a `PLAYER_JUST_DIED` event that is not for slot 0.
   - A `TODO` at the screen names the other two death options from `ITEM.md`.
8. **Level end:** the locks and the play phase apply to all players. The beam stays on slot 0, and slot 1 disappears
   when the beam starts.
9. **Tint:** slot 1 draws its main sprite with a debug tint. Effect sprites, for example the jetpack flame, have no
   tint.
10. **Test map:** `RefactorTest047.tmx`, with a `LevelDefinition` entry `REFACTOR_TEST_047`. It has rooms for the default
    point, a `player_2` reference, and a spawn with `direction=left`.
11. **Tests:** the launch option check, the pad split in `GamepadInput`, the slot 1 spawn point with a reference and for
    each direction with no reference, and the slot filter on the death event, if it can be tested with `testServices()`.

## New APIs and their first callers

| New API | First caller |
|---------|--------------|
| `--players` in `LaunchOptions` and `DesktopAppArgs` | `resolveLaunchOptions` |
| A player count in the resolved launch | `postCreate()` |
| A pad choice in the `GamepadInput` constructor | `create()`, one `GamepadInput` per slot |
| A per-slot frame builder array, in place of `slot0FrameBuilder` | the frame step in `render()` |
| A function that gives the slot 1 spawn props from the slot 0 spawn props | `PLAYER_SPAWN_INIT` in `MegaLevelScreen` |

There is no death policy type. Only one option exists, so a type would have no second caller (`O1-R5`). The other two
options stay as a `TODO`.

## Out of scope

| Work | Branch |
|------|--------|
| `targetFor` picks the nearest present player | `047b` |
| Friendly fire is off, and shots pass through teammates | `047b` |
| `ChargedShot` reads the owner, not the target player | `047b` |
| Pickups go to the player that took them | `047b` |
| Slot 1 respawns near the lead while the lead lives | `047c` |
| `GauntletMaster`, `SpiderWeb` and `PreciousGem` read the slot of the death event | `047c` or `047e` |
| HUD and pause for player 2; player 2 can pause | `047d` |
| Boss intro and defeat rules | `047e` |
| Join and leave during play, device claim, unplug prompt | `047f` |
| Bindings and settings screens for slot 1 | `047f` |
| Rotation when both players change gravity | see "Affected areas and risks" |

## Affected areas and risks

- **Single player is the main risk.** The changes touch the frame step, the spawn handler, the death event and the
  level end. Each change must act the same when slot 1 is empty.
- **Known wrong with `--players 2`, until `047b` to `047e`:**
  - Shots from player 1 hurt player 2, and the reverse.
  - Enemies target only slot 0.
  - Player 2's pickups go to slot 0.
  - The death of player 2 resets a gauntlet, and removes a `PreciousGem` shield and a `SpiderWeb`.
  - Only slot 0 can pause.
  - The boss intro can wait for a dead player 2.
- **Gravity:** `WorldGravity.set` writes each player's direction, and each write can submit
  `START_GAME_CAM_ROTATION` (`Megaman.kt:141`). Slot 1 can send a second event in the same frame. I did not test this.
  The contract includes a gravity room. A fix goes to the branch that the result points to.
- **`STUN_PLAYER`, room transitions and carts:** these reach slot 1 through the existing listeners. I did not test them
  with two players.
- **Spawn points:** the live maps have 174 `player` layer spawns, and I did not check the tile beside each one. Some
  defaults can put slot 1 in a block or over a pit. Of these spawns, 2 use `direction=left`, and no spawn in a live map
  uses `down`.
- **Checkpoints:** `PlayerLayerBuilder` makes every rectangle in the `player` layer a checkpoint. A `player_2` target in
  that layer by mistake becomes a checkpoint with no error.
- **Input cost:** the pad split must not allocate per frame (`O5-R1`).
- **Docs:** root `CLAUDE.md` lists the launch flags. The new flag needs a line there.

## Overlap with other branches

- `mega-refactor-004a-entity-smoke-test`: a test suite only, so no overlap is expected. Read it again at branch start.
- `compactor-level`: a level branch, so no overlap is expected.
- `019` is `active` in `ITEMS.md`, but it has no worktree. Check it at branch start, because spawn order is in its area.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | The new values live on the game and on `MegaPlayers`. |
| `O1-R3` No new fragile order | no effect | Slot 1 spawns in the same step as slot 0. Check this at plan time. |
| `O1-R4` Typed state | helps | The player count is a typed launch value. |
| `O1-R5` Code only with a production caller | no effect | See "New APIs and their first callers". |
| `O2-R1` Input through frames | helps | Slot 1 reads only its own frame. |
| `O2-R4` Stable order | no effect | Slots iterate in slot order. |
| `O2-R5` No single-player logic | helps | Spawn and level-end steps move to all players. Many slot 0 rules remain, each with its `TODO`. |
| `O2-R6` No single-view logic | waiting | One camera stays. |
| `O2-R7` Gameplay apart from rendering | no effect | The tint is draw state only. |
| `O2-R8` Local data stays local | no effect | Device choice stays out of the simulation. |
| `O2-R9` Events name a slot | conflict | Only slot 0 submits `PLAYER_SPAWN`, so slot 1 has no spawn event. Accepted gap (owner, 2026-11-01), until `038`. |
| `O5-R1` No per-frame allocation | no effect | See "Input cost". |
| `O6-R1`, `O6-R2` | no effect | The contract names the checks. |
| `O8-R2` Capabilities, not character checks | no effect | No character check is added. |
| `O9-R1` Playable with an NES-style pad | no effect | Bindings do not change. |

## Open questions

None.

## Decisions on the questions

- `O2-R9` (owner, 2026-11-01): accepted gap. Only slot 0 submits `PLAYER_SPAWN` until `038`. A `TODO` at `Megaman.kt:355`
  points to `038`.
- Merge target (owner, 2026-11-01): each `047` branch merges directly into `master`.
- Test devices (owner, 2026-11-01): the owner has 1 keyboard and 1 gamepad for the play test.
