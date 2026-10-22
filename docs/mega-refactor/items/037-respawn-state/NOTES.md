# Notes: 037 Explicit respawn state and named `on_respawn` actions

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEMS 35 + 36: CHECKPOINTS AS DATA, EXPLICIT RESPAWN STATE".

````text
(1) Current state

- `PlayerLayerBuilder.kt:16-19` collects every rectangle in the `player` layer.
- `PlayerSpawnsManager.kt`:
  - The start is the lowest name by string sort (`popFirstSpawn`, lines 35-43). With names "0".."10", "10" sorts before "2". This matters only for the start today.
  - Activation is the first remaining rectangle in TMX order that overlaps the camera (`shouldPop`, lines 57-59), checked each frame (`MegaLevelScreen.kt:924`).
  - Each rectangle can activate once.
  - There is no order check. If the player skips a spawn because it never came into frame and then goes back, the older spawn activates and the checkpoint goes backwards.
  - "One checkpoint in two places" is done with the same name and `duplicate=true` (`MegaLevelScreen.kt:237-238`). DesertMan and ReactorMan both have two spawns named "4".
- On respawn, `megaman.spawn(currentSpawnProps)` (`MegaLevelScreen.kt:563-566`) passes all rectangle properties.
- The MoonMan spawns repeat the level scalars (see item 24).
- Backgrounds are not reset on respawn: `backgroundsToHide` changes only through events (`MegaLevelScreen.kt:850-856`). The workaround is `EventTrigger` objects.
- In 18 live maps there are 1-12 player spawns per map.

X6 (engine.md "Checkpoints"; stages/turtloid.md "Checkpoints"):
- The activation marker and the respawn record are separate data.
- The marker is a stage object. It activates when the player's x is ≥ the marker x (y when id bit 7 is set). This is a line crossing by the player, not camera overlap.
- It sets `game.checkpoint = id`.
- A marker whose id equals the current checkpoint deletes itself. The doc does not show a forward-only compare.
- The respawn record (36 bytes, in `checkpoint_start_tbl`) holds:
  - player x, y and facing;
  - the camera of all 3 BG layers;
  - the camera limits;
  - the layer offsets;
  - the beam flag.
- Turtloid has 5 markers and 6 records (0 = start). The start is record 0 by index, not by sort.
- On restart, `RecordsReset` resets every spawn record.

(2) Proposed data model

- Checkpoint (the respawn point). A Tiled object of class `Checkpoint` in the `player` layer. Its fields:
  - `index` (int, explicit order);
  - `start` (bool; exactly one per level, checked by the validator);
  - `facing`;
  - `room` (inferred from geometry by default);
  - `backgrounds_visible` / `backgrounds_hidden` (lists);
  - `music` (optional override);
  - `on_respawn` (an ordered list of named actions, for example `hide_bg:stars`, `set_flag:lava_drained`, `play_music:X`), resolved from a code registry of actions.
- Activation trigger. Separate objects of class `CheckpointTrigger`. Each has:
  - an object-reference property `checkpoint` that points to its checkpoint. Tiled draws a line between them, so they stay easy to read. Several triggers can point to one checkpoint, which replaces `duplicate=true`.
  - `rule`: `area` (the player body enters the rectangle; the default), `cross_x` / `cross_y` (X6-style line), or `room_enter` (the player enters the room).
- With no trigger, the checkpoint's own rectangle is the activation area, tested against the player body, not the camera. This keeps old maps working with the new rule.
- Order: a trigger activates only if `index` > the current index (forward-only). Per-checkpoint `allow_backtrack=true` opts out.
- Level-wide defaults: Tiled map properties (or a `LevelDefaults` class) such as `gravity_scalar`, `movement_scalar`, `apply_scalar_to_children`, default `facing`, initial background visibility and music. A checkpoint property overrides a default.
- Respawn state (item 36):
  1. Reset to the level's initial state: backgrounds and BG offsets, music, and flags marked "reset on respawn". X6 stores the BG layer cameras in each record.
  2. Apply the checkpoint's declared state.
  3. Run `on_respawn` in order.
- This removes the background `EventTrigger` workarounds.

Readability in Tiled:
- `propertyTypes` classes `Checkpoint` and `CheckpointTrigger` (item 21), with templates.
- The object name shows "CP 3".
- The validator checks: unique indexes, one start, every trigger resolves, and the checkpoint is inside a room.

(3) Options for respawn state

a) Declared state only (above). Pro: predictable. Con: the designer must list what changes.
b) A snapshot at activation of a fixed list of fields (backgrounds, music, flags). Pro: no authoring. Con: it can capture state in the middle of an event (for example during a background transition).
c) Declared state plus "reset to initial" by default (recommended). Add snapshot fields later, only if a real need appears.

(4) Recommendation

- Use (c) with the trigger/checkpoint split.
- Make the default activation "player body overlaps area", which removes the need for the camera to see the spawn.
- Make `room_enter` available as a one-field option.

(5) Open questions for the owner

- Should a backward activation ever be allowed by default? Today it can happen by accident.
- Should the "CHECKPOINT n" text show the index or a name?
- Which states must survive a respawn (for example collected items, opened gates)? X6 keeps the Rescue Reploid status and resets all spawn records.

(6) Backlog wording

Item 35 title: "Checkpoints as typed Tiled data: respawn point, triggers, order"
- `Checkpoint` (index, start, facing, room, state) and `CheckpointTrigger` (object reference to the checkpoint; rule `area` | `cross_x` | `cross_y` | `room_enter`), tested against the player body, not the camera.
- Forward-only by index unless `allow_backtrack`. Several triggers per checkpoint replace `duplicate=true`.
- Level-wide defaults in map properties (replace the moon scalars repeated on each spawn). The validator checks one start, unique indexes and resolved references.
- Old maps keep working: a rectangle with no trigger is its own activation area.

Item 36 title: "Explicit respawn state and named on_respawn actions"
- On respawn: reset to the level's initial state (backgrounds, BG offsets, music, flags marked reset-on-respawn), then apply the checkpoint state, then run `on_respawn` actions from a named-action registry.
- Remove the `EventTrigger` workarounds for background visibility.

(7) Dependencies

Items 21, 2, 37 (the ordered respawn routine runs step 1-3), 12, 46 (BG state on respawn), 40 (room inference), and the camera backlog item (camera zones change what "room" means).
````
