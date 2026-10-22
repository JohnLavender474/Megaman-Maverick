# Notes: 036 Checkpoints as typed Tiled data: respawn point, triggers, order

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

## Review: Player spawn and checkpoint config (2026-10-22)

Copied as written.

````text
Report: player spawn and checkpoint config (read-only review)

Path note: `C/` means `core/src/main/kotlin/com/megaman/maverick/game/`. "Live maps" means the 18 TMX files that `LevelDefinition` names. No files were changed.

== Q1. How spawns and checkpoints work today ==

1. Load
- `C/screens/levels/tiled/layers/PlayerLayerBuilder.kt:16-20` collects every `RectangleMapObject` in the `player` layer. It puts them under `player_spawns` and reads no properties.
- `C/screens/levels/MegaLevelScreen.kt:512-514` calls `playerSpawnsMan.set(...)`.
- `C/screens/levels/spawns/PlayerSpawnsManager.kt:30-43`: the start spawn is the object whose name sorts first as a string (`compareTo`, so "10" comes before "2"; only index 0 is used). It is removed from the list.

2. Activation
- `PlayerSpawnsManager.run()` (`:61-70`) runs once per frame after `engine.update` (`MegaLevelScreen.kt:924`).
- It pops the first remaining spawn whose rectangle overlaps the game camera rectangle (`:45-59`). The test is the camera, not the player, so a checkpoint can activate up to about half a screen before the player reaches it, and also during room transitions.
- A popped spawn is removed for good. You cannot go back to an earlier checkpoint, and any unvisited spawn that the camera touches becomes current, in any order.
- The object name is used only for the start sort, the "CHECKPOINT <name>" text and `remove(name)`.

3. Change callback (`MegaLevelScreen.kt:230-245`)
- If `megaman.ready`, it shows "CHECKPOINT <name>" for 2 s and reads `duplicate`. If `duplicate` is true, it removes all other spawns with the same name.
- If not ready, it clears the text. The current spawn still changes, so a checkpoint can change silently during the beam-in or the death animation.

4. Death
- `Megaman.onDestroy` (`C/entities/megaman/Megaman.kt:518`) submits `PLAYER_JUST_DIED`.
- `MegaLevelScreen.kt:587-606` then stops the music and looping sounds, removes one life and starts `PlayerDeathEventHandler`.
- `PlayerDeathEventHandler.kt` turns off the controller and gravity, waits 4 s, fades out for 0.25 s, then submits `PLAYER_DONE_DYIN`.

5. `PLAYER_DONE_DYIN` (`MegaLevelScreen.kt:608-623`)
- It replays the level music (`LevelDefinition.music`).
- If the player has no lives left, it goes to the game over screen. If not, it calls `playerSpawnEventHandler.init()`.

6. `PlayerSpawnEventHandler.init()` (`events/PlayerSpawnEventHandler.kt:98-121`)
- Sets Megaman not ready, not damageable, gravity off and all behaviors off.
- Submits `PLAYER_SPAWN_INIT` and `TURN_CONTROLLER_OFF`.
- Then runs fade-in, a 1 s pre-beam, the beam-down and the beam-land animation. At the end (`:187-205`) it turns gravity, damage, behaviors and the controller back on, and submits `PLAYER_READY`.

7. `PLAYER_SPAWN_INIT` (`MegaLevelScreen.kt:545-580`)
- Turns backgrounds to UP at once, calls `cameraManagerForRooms.reset()`, unsets the boss health bar and turns all systems on.
- Clears focus-snap and room-transition flags, destroys all enemies, and calls `megaman.spawn(playerSpawnsMan.currentSpawnProps)`.
- Stops the camera shaker, then runs a room-music block (see Q3, defect).

8. `Megaman.onSpawn` (`Megaman.kt:423-508`) reads:
- `bounds`: placed at bottom-center.
- `facing`: default right.
- `direction`: default up. The setter starts an animated camera rotation if the direction differs (`Megaman.kt:210-232`).
- `gravity_scalar`, `movement_scalar`, `apply_scalar_to_children`.
- It also resets health and all ammo to max, sets the weapon to the Mega Buster, resets timers and flags, and submits `PLAYER_SPAWN` (`:507`).

9. Camera
- `CameraManagerForRooms.reset()` (`camera/CameraManagerForRooms.kt:122`) only sets a flag.
- On the next `update()` (`:98-116`) it centers the camera on Megaman (x clamped to the absolute map limits), sets `currentGameRoom = nextGameRoom()` (the first room that contains the focus center, `:284-301`), then calls `onFirstUpdateAfterReset` → `tryToPlayCurrentRoomMusic()` (`MegaLevelScreen.kt:267`, `:1116`).
- The camera position comes from the player position and the room. No checkpoint holds camera data.

10. Other parts
- Gravity: restored only through the spawn prop `direction`. Room `megaman_direction` is applied only at the end of a room transition (`MegaLevelScreen.kt:362-372`).
- Backgrounds: only rotation is reset. The hidden set `backgroundsToHide` is NOT reset (`:183`, `:509`, `:847-857`).
- Spawners: `SpawnsManager` is not reset on respawn. Entities other than enemies stay unless they listen to the respawn events themselves (Q2).

== Q2. Custom logic at checkpoints ==

No code checks a specific spawn name or checkpoint. I searched for `currentSpawn`, "checkpoint", spawn names and `remove(` call sites. The only call to `PlayerSpawnsManager.remove` is the `duplicate` branch. So the owner's claim is only partly correct. The special per-checkpoint behavior is in four places, as follows.

A. Spawn object properties in TMX files.

Live maps (18 maps, 104 spawn objects):
- `facing=left`: 7 objects in 6 maps.
- `gravity_scalar=0.35`, `movement_scalar=0.8`, `apply_scalar_to_children=true`: 10 objects, only `MoonMan_16x14_v5.tmx`, the same values on all 10 spawns.
- `direction=up`: 5 objects, only MoonMan. This is the default value, so it has no effect.
- `duplicate`: 0 objects.

All 87 TMX files: the same keys, plus `camera_position` (11 objects in 3 old maps). No code reads `camera_position`, so it is dead data.

`duplicate` was added with the two spawns named "4" in `ReactorMan_v3.tmx` (commit `6a0803668`), but no live object sets it. The two "4" spawns (ids 200 and 201, x 6496 and 6784, the second with `facing=left`) are both in `room7`, where the main path and the alt path meet. `DesertMan_16x14_v4.tmx` also has two spawns named "4".

Without `duplicate`, both spawns pop one after the other as the camera slides in, and the last one popped wins. By my reading, the result is the spawn on the far side from where the player came in. This is likely the opposite of what was intended. I did not test it in the game.

B. Tiled workaround: `EventTrigger` objects at the same place as checkpoints. This is data, but implicit.
- `C/entities/special/EventTrigger.kt:22-34` submits its `event_type` when the camera spawner spawns it.
- `DesertMan_16x14_v4.tmx` has 15 triggers in sets of three at spawn "0" (x 96/128), spawn "3" (x 6816) and near spawns 8/9. They show and hide `SunriseHills`, `DesertSunSky` and `SunsetHills`, so the right backgrounds show after a respawn.
- `WilyStage1_v2.tmx` has 3 triggers (one at spawn "0" x 64/96, one near spawn "6" x 10240/10304).
- These triggers are the only thing that restores the background visibility set on respawn.

C. Entities that react to respawn events. This is code; most rules depend on the current room, not on the checkpoint.
- `PLAYER_READY`:
  - `DarknessV2.kt:242`, `DarknessV4.kt:326`: dark mode from the current room. Used in DesertMan and WilyStage1.
  - `RisingLavaRiver.kt:126-130`: rises if the room equals `rise_room`. It also goes dormant on `PLAYER_DONE_DYIN`. Used in Inferno and Wily3.
  - `SpawnersLayerBuilder.kt:113-121`: `spawn_room` spawners accept `PLAYER_READY` by default.
- `PLAYER_SPAWN`, destroy or reset:
  - Destroy: `AbstractEnemy.kt:222`, `AbstractBoss.kt:184`, `BigAssMaverickRobotBody.kt:65`, `Rain.kt:155`, `Togglee.kt:202` (comment: "assumes that a player spawn is never in the same room as a Togglee").
  - Reset: `Gate.kt:177-179` (reset unless `reset=false`; 0 uses in live maps), `PropellerPlatform.kt:120`, `GearTrolley.kt:87`, `SwingingPlatform.kt:69`.
  - Cull logic: `RoomShaker`, `Cart`, `PushableBlock`, `Lava`, `SeaMine`, `FireballBar`, `InfernoMeteorShower`, `AsteroidsSpawner`, `ExplosionOrb`, `MuzzleFlash`, `IProjectileEntity.kt:57`, `GameEntityUtils.kt:41`.
- `PLAYER_JUST_DIED`: `GauntletMaster.kt:167` (`resetGauntlet`), `SpiderWeb.kt:130`, `PreciousGem.kt:211`, `PreciousGemCluster.kt:109`.
- `PLAYER_DONE_DYIN`: `QuestionBlock.kt:125` (re-arm), `LaserBeamer.kt:176`, `DrWily.kt:121`.

D. Code that checks the level identity. This is code; it applies to the whole level, not to a checkpoint.
- Moon physics:
  - `getCurrentLevel() == MOON_MAN` in `MoonMan.kt:246,322,664,693`, `AbstractItem.kt:113,157`, `megaman/components/BodyComponent.kt:377`, `MegamanWeaponsHandler.kt:842,1036`.
  - The player's moon gravity is instead copied on each MoonMan spawn object (item A). So the same rule is half in data and half in code.
- `HeartTank.kt:153`: DESERT_MAN light source.
- Inferno frozen variant:
  - `LevelUtils.kt:7`, `FrozenBlock.kt:46` (`when (getCurrentLevel())`), `RisingLavaRiver.kt:164`, `InfernoDeath.kt:23-24`.
- `LevelType.MARIO_LEVEL` checks: `BodyComponent.kt:146`, `BehaviorsComponent.kt:239`, `Megaman.kt:539`.
- `levelOverride` (`levels/LevelOverride.kt`, `LevelDefinition.kt:137-180`): sets loadout, difficulty, lives and a `canSpawn` filter at level start for `GAUNTLET_1`, `GAUNTLET_2` and `RUMBLECADE_FACTORY`. It has nothing for checkpoints.

== Q3. What a respawn restores and what it does not ==

Restored:
- Player: position, facing, direction, scalars, full health and ammo, Mega Buster, timers.
- Camera: re-centered and room recomputed.
- Background rotation.
- All systems on, boss bar unset.
- Enemies, bosses and most projectiles destroyed.
- Gates closed again.
- Moving platforms reset.
- Gauntlet reset.
- Level music replayed.
- Darkness and rising lava recomputed from the room.

Not restored, or restored only by ad hoc means:
1. Background visibility (`backgroundsToHide`). It is not reset. The co-located `EventTrigger`s (item B) fix it, and that works only where someone placed them.
2. Mini-bosses and bosses. Rooms with `event=boss` start the fight each time the player enters (`MegaLevelScreen.kt:334-357`, `:694-718`). A defeated mini-boss is fought again after a respawn behind it. No per-run "defeated" flag exists, except `game.state.isLevelDefeated` for the end boss on a replay (`:704-711`).
3. Collected items. All spawners default to `respawnable=true`. Live maps have 0 `respawnable=false` and 0 `reset=false`, so pickups come back. Heart tanks and similar items use `game.state`.
4. Coins and level points are reset only in `start()` (`:476-478`), not on respawn. I did not check whether that is intended.
5. Room music on respawn: three code paths.
   - The level music in `PLAYER_DONE_DYIN`.
   - The block in `PLAYER_SPAWN_INIT` (`:570-579`). It calls `nextGameRoom()` before the queued `megaman.spawn` runs (`GameEngine.spawn` only queues, `engine/.../GameEngine.kt:63-70`). So it tests the death position, then reads `currentGameRoom!!`. That is a stale room, and an NPE if it is null.
   - `onFirstUpdateAfterReset`.
   No live room has a `music` property, so paths 2 and 3 do nothing today. Path 2 is a latent bug.
6. Game camera rotation is not reset at once on respawn (only in `reset()` at `:1083`). It is fixed only as a side effect of the `Megaman.direction` setter starting an animated rotation.
7. Order dependence:
   - The handler `when` chain in `render()` (`:908-913`, commented as fragile).
   - The event chain `PLAYER_DONE_DYIN` → `PLAYER_SPAWN_INIT` → `PLAYER_SPAWN` → `PLAYER_READY` across frames.
   - Music set first by `PLAYER_DONE_DYIN`, then possibly by the room.

== Q4. Mega Man X6 (from `mmx-demake-tools/docs`) ==

- Markers: stage-logic type 6 `CheckpointMarker` sets `game.checkpoint` (`game+0x1D`) = `id & 0x7F` when the player's x reaches the record x. With `id` bit 7, it tests y. The marker deletes itself, and does so at once if that checkpoint is already set (`engine.md:439`, `stages/turtloid.md:129-143`). The test uses the player, not the camera. Checkpoint numbers need not follow stage order (`stages/yammark.md:146`).
- Respawn data: `CheckpointStart` reads a fixed 36-byte record from the per-stage/area table `checkpoint_start_tbl` (`turtloid.md:145-160`). It holds:
  - player x, y
  - camera x, y of layers 0, 1 and 2
  - camera limits min/max x/y (written as both current and target)
  - facing
  - parallax offsets of layers 1 and 2
- Other state: after a respawn, every record's "spawned" bit is cleared, so the world rebuilds from records. Single-use records (`DeleteObjectKeep`, flag bit 7) stay used, for example a rescued Reploid (`engine.md:443-465`).
- Checkpoint-gated objects: objects read `game.checkpoint`. Scenery deletes itself at checkpoint ≥ 4 (`turtloid.md:184`), grey blocks at checkpoint ≥ `var` (`scaravich.md:226`), and `SheldonBgSwitch` changes background layers at checkpoint ≥ 1 (`engine.md:826`). Per-stage flags live in `game+0x26..0x28`, and one boss writes `game[0x2C + checkpoint]`.
- Summary: X6 uses an index, a data table per checkpoint, and objects that compare against the index. Very little code is specific to one checkpoint.

== Q5. Is a data-driven approach better? Recommendation ==

Yes, but small in scope. The problem today is not checkpoint-specific code (there is none). The problems are:
- Respawn state is implicit: co-located triggers, room-derived rules spread across entities, unreset screen state.
- The level-wide rules are duplicated on every spawn.
- Config is unvalidated: `duplicate` is never used, `camera_position` is dead, and unknown keys are silently ignored.

Checkpoint record fields for this game:
- `id`: the display name. Keep the object name. Add an explicit `start=true`, or keep the "lowest name" rule for compatibility.
- Position: the object rectangle (as now).
- `facing`, `direction` (as now).
- `room` (optional): explicit start room, checked at load against the room containing the position.
- `backgrounds_visible` / `backgrounds_hidden` (optional): replaces the 18 co-located `EventTrigger`s.
- `music` (optional): overrides the level music.
- `on_respawn` (optional): a list of named actions with arguments, for example `hide_backgrounds:SunsetHills`.
- Activation, optional:
  - `activate_on=camera|player_x|player_y|rect`. The default `camera` keeps the current behavior.
  - Extra activation rectangles that point to one checkpoint id, to replace the `duplicate`/same-name pattern for alt paths with a clear choice of spawn.
- Later, for the X-style camera backlog item: camera position and limits, as in the X6 record.

Level-wide values go on the map or the `player` layer properties as defaults, not on each spawn: `gravity_scalar`, `movement_scalar`, `apply_scalar_to_children`. This removes the 10 copies in MoonMan.

Where the data should live:
- Use Tiled object properties on the `player` layer, with a Tiled custom class (for example `Checkpoint`) so fields are typed and listed.
- Do not use a separate file. Positions and rooms are map data, and a second file would drift. `utils/level_analyzer` and `level_builder` already parse the TMX.
- Add a load-time check that logs an error for unknown keys, duplicate ids without an activation rule, and a spawn outside every room.

What still needs code:
- A `CheckpointActions` registry (name → `(game, args) -> Unit`), registered once at start. One small function per action (backgrounds, music, later flags).
- One `RespawnRoutine` that runs the restore steps in a fixed order. It replaces the multi-event chain and removes the stale music block at `MegaLevelScreen.kt:570-579`.
- Room-derived entity rules (Darkness, `RisingLavaRiver`, `spawn_room` spawners) can stay as they are. They are room behavior, not checkpoint behavior.
- Optional, following X6: a `checkpointIndex` that entities can read, and a per-run flag set (for example "mini-boss X defeated") that survives a respawn. Only add this if the owner wants that gameplay change; today mini-bosses are fought again.

Risks to current gameplay:
- Changing activation from camera overlap to player position changes which checkpoint is active in edge cases (the ReactorMan "4" pair, early activation). Keep `camera` as the default.
- Replacing the `EventTrigger`s could change the frame on which backgrounds switch. Compare visually in DesertMan and WilyStage1.
- Moving the moon scalars to a level default is safe only if the values match exactly (0.35 / 0.8 / true).
- Removing the stale music block does nothing today, because no room has `music`.
- Whether to keep the silent checkpoint change while not ready must be a deliberate choice.

== CANDIDATE BACKLOG ITEMS ==

1. Title: Make checkpoints typed, validated Tiled data
- Problem: The player spawn props are untyped and unvalidated (`PlayerLayerBuilder.kt:16-20`, `Megaman.kt:429-460`). `duplicate` is read (`MegaLevelScreen.kt:237`) but used by 0 objects. `camera_position` is in 11 old objects and no code reads it. MoonMan repeats the same 3 scalars on all 10 spawns. The start spawn is picked by string sort (`PlayerSpawnsManager.kt:35-43`). The ReactorMan same-name "4" pair likely picks the far-side spawn.
- Why it matters: Level makers cannot see or trust the checkpoint options. Mistakes fail silently.
- Suggested direction: A Tiled custom class `Checkpoint` with the fields in Q5. Level-wide defaults go on the map or player layer. Add load-time validation. Add an explicit activation rule (keep `camera` as the default) and an explicit start flag. Write an ADR first on whether to fix the ReactorMan "4" behavior.
- Size: M
- Depends on / enables: enables 2 and 3; supports "Make values configurable" and "Define entities in data files".
- Risk to current gameplay: low if the defaults copy today's rules exactly. Medium if activation or the ReactorMan pair is changed.

2. Title: Explicit respawn state: named "on respawn" actions and background state
- Problem: Background visibility is not reset on respawn (`MegaLevelScreen.kt:183`, `:847-857`). It is restored only by 18 `EventTrigger` objects placed at checkpoints (15 in `DesertMan_16x14_v4.tmx`, 3 in `WilyStage1_v2.tmx`). Other restore rules are spread over about 30 entity event handlers (Q2-C).
- Why it matters: A new checkpoint in those levels needs hand-placed triggers. Forgetting one gives wrong backgrounds after death, and nothing reports it.
- Suggested direction: Add checkpoint fields `backgrounds_visible`/`backgrounds_hidden`/`music` and an `on_respawn` list resolved through a `CheckpointActions` registry registered once. Optionally add a `checkpointIndex` and a per-run flag set (X6 style) for future "stays defeated/opened" rules. Make it explicit that today mini-bosses and pickups come back.
- Size: M
- Depends on / enables: depends on 1; overlaps "Clean up event-driven logic".
- Risk to current gameplay: medium. Background switch timing and music order must match. Compare DesertMan and WilyStage1 frame by frame (record and replay item).

3. Title: One ordered respawn routine instead of an event chain
- Problem: The respawn is spread over `PLAYER_JUST_DIED` → `PLAYER_DONE_DYIN` → `PLAYER_SPAWN_INIT` → `PLAYER_SPAWN` → `PLAYER_READY` across frames (`MegaLevelScreen.kt:545-623`, `PlayerSpawnEventHandler.kt:113,196`), with a fragile handler order (`:908-913`). The room-music block in `PLAYER_SPAWN_INIT` (`:570-579`) reads the room before the queued spawn moves Megaman and can NPE (dead today, because no room has `music`). Music is set in up to three places.
- Why it matters: The order bugs are hard to see. The camera refactor and the removal of the global `megaman` both touch this code.
- Suggested direction: A `RespawnRoutine` with fixed steps (reset world, place player, reset camera/room, apply checkpoint data, music, beam-in, ready). Keep the public events for entities, but submit them from one place in a set order. Remove the stale music block.
- Size: M
- Depends on / enables: helps "Clean up event-driven logic", "Support X-style camera bounds" (respawn camera position and limits) and "Remove the global megaman"; better after 1.
- Risk to current gameplay: medium. Many entities depend on event timing (`PLAYER_READY` spawners, Gate reset, `RisingLavaRiver`).

4. Title: Move level-identity checks into level data
- Problem: The code branches on level identity:
  - `MOON_MAN`: 9 checks (`MoonMan.kt:246,322,664,693`, `AbstractItem.kt:113,157`, `BodyComponent.kt:377`, `MegamanWeaponsHandler.kt:842,1036`), while the player's moon gravity is on the spawn objects.
  - Inferno frozen variant: `FrozenBlock.kt:46`, `RisingLavaRiver.kt:164`, `InfernoDeath.kt:23`, `LevelUtils.kt:7`.
  - Desert light: `HeartTank.kt:153`.
  - `MARIO_LEVEL`: 3 checks.
- Why it matters: The same rule is split between data and code. A new low-gravity or variant level needs code changes in many files.
- Suggested direction: Add level properties (map properties or a `LevelDefinition`/`LevelOverride` field), for example `gravity_scalar`, `movement_scalar` and a `variant` predicate. Entities read the properties, not the level enum.
- Size: S-M
- Depends on / enables: pairs with 1 (level defaults); supports "Make values configurable".
- Risk to current gameplay: low-medium. The values must match exactly. MoonMan physics and the Inferno variant need a check.
````
