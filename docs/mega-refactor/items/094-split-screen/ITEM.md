# 094: Split screen (`O2` phase 2)

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`, `010`
- Related: `047`, `051`, `016`, `108`, `109`, `111`
- Branches: none yet

## Goal

Two players play on one machine with one view each, and both stay in the same room. This is phase 2 of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Change the one `gameCamera` in `MegaServices` to a set of views.
- Make spawn and cull ask about all views. `overlapsGameCamera` and `getGameCameraCullingLogic` carry much of this; 115 direct lines in 87 entity files need a review.
- One `CameraController` per view. Give scroll deaths, edge blocks and crush surfaces a rule for two views of one room.
- HUD and fades per view.

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

### Effect of the dedicated modes

- The owner's decision of 2026-11-01 puts multiplayer in dedicated modes (`108`, `109`). Split screen serves missions and arenas first, not the main stages. Questions marked **(mode)** become smaller, because content can be made for two views, and they stay here until a mode shows that they do or do not apply.
- The first PvP target is one large room, so the `O2-R6` same-room limit holds here. Multi-room PvP comes later (`111`).

### Culling and spawn

- **Owner's seed example:** off-screen culling must change for multiple players on different screens. The owner imagines two classes. For certain entities, cull when no player's screen renders them. For other entities, possibly never cull until they are no longer in a room or area that is occupied.
  - Which entities go in which class? Today all use one test: `CullableOnUncontained` on `getGameCameraCullingLogic`, for blocks (`Block.kt:75`), enemies (`AbstractEnemy.kt:214`), projectiles, items and 88 files with `CULL_OUT_OF_BOUNDS`.
  - What does "occupied" mean? The `O2` decision keeps all views in one room, so the occupied room is `levelState.currentRoom` in phase 2. Rooms that differ per player come with `111`. Does "area" mean something smaller or larger than a room?
  - Does an entity of the second class keep its behavior out of view, or only stay in the world? This affects cost on weak hardware (`O5`). See the classes of `041`.
- Spawn on view: `SpawnerForBoundsEntered` fires on the first overlap with one rectangle (`spawnCameraBounds`). With two views, does the test use "any view"? If a view still holds the point, a killed enemy cannot respawn when the other view comes back. Is that correct, or does each view have its own trigger?
- Activity gates: some entities act only in view, for example `defaultDoUpdate` for `ABSTRACT` bodies (`IBodyExtensions.kt:29`), `Saw.kt:245`, `FlipperPlatform.kt:139` and `InfernoDeath.kt:34`. Is the rule "any view", or the view of the nearest player?
- Placement: some entities use the camera as a position, for example `Jetto.kt:214` (returns at a view edge), `AsteroidsSpawner.kt:133`, `RisingLavaRiver.kt:343`, `WilyFinalBoss.kt:1711` and `Rain.kt:258`. Which view does such an entity use? Options: the view of its target, the nearest view, one instance for each view.
- Checkpoints: `PlayerCheckpointManager.shouldPop` tests one camera. Does any view activate a checkpoint for all players, or only the lead's view (`036`)? **(mode)**

### Views

- What is the size of each view? The levels are designed for 16x14 tiles. Options: keep 16x14 in each view at a smaller scale, or show a smaller part of the world. 10 lines in 6 entity files read `ConstVals.VIEW_WIDTH` or `VIEW_HEIGHT` as a gameplay range (`SpringHead.kt:70`, `MagFly.kt:117`). See the `035` decision J.
- What is the layout: side by side, stacked, or a window choice? What is the layout for 3 or 4 players (`O2`: the design must scale to 4 or more)?
- Fixed split, or one view that splits only when the players are apart? In 122 of the 214 live rooms, the room fits in one view. So both views show the same image in those rooms, including most boss rooms (inference). **(mode)**

### Rooms and camera parts

- How does "all views stay in the same room" hold? A room is larger than a view, so one player can reach the edge with the other player far away. Options: hold the player at the edge until the other arrives, warp the other player, let either player start a pan for both. The `047` rule for a shared screen can differ. **(mode)**
- Which camera parts belong to a view, and which to the session? `094` proposes one `CameraController` per view, but `CameraRoomBounds` holds the one current room, and `LevelRoomHandler` submits one set of transition events with one `POSITION`. One session room with many view pans, or one room tracker in each view?
- Scroll sections: does a scroll move all views, or only views inside the section? Is the scroll state (`CameraScroll` holds the running section and the done set) per view or per session? **(mode)**
- Edge blocks: a `CameraEdgeBlock` is a world solid for all players. A block for one view stops the other player too. Options: a block that acts for one slot, or one set of limits for all views. A crush has the same choice. **(mode)**
- Is rotation per view? `058` asks this. Note: all non-player bodies stop while `gameCamera.rotating` is true (`IBodyExtensions.kt:22`), so with per-view rotation, a rule must say when the world moves again.

### Effects and overlays

- Which overlays are per view, and which are for the window: HUD, boss bar (`BossHealthHandler`), `READY` text, checkpoint text, pause menu, game over? The death fade has one rectangle (`PlayerDeathEventHandler.kt:76`). Slot 0 death resets all players, so does the fade cover all views?
- Does a shake (`SHAKE_CAM`, 7 entity sites, one `CameraShake`) act on all views, or only on views that see its source?
- Darkness is computed from one camera (`DarknessV2.kt:331`, `DarknessV4.kt:593`). Does each view get its own light state?

### Audio

- 75 lines gate a sound with `overlapsGameCamera()`. With one audio mix, does a sound play if any view sees its source? Is there stereo pan by view? Each player then hears events from the other view.

### Performance

- What is the budget for two passes of the map, the backgrounds, the sprites and darkness? Must split screen run on the lowest `--performance` tier, or does it need a minimum tier, or lower cosmetic budgets (`O5`)?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
