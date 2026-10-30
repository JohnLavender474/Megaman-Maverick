# Notes: 051 X-style camera bounds, auto-scroll and other camera behaviors

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Today `CameraManagerForRooms` supports only the classic Mega Man way to set camera bounds: the
bounds are the current room, and the bounds change only through a room transition.

Camera behaviors to support:

- **Room bounds** (current behavior): the camera follows the player inside the current room, and
  a room transition moves it to the next room.
- **X-style bounds:** objects in the stage change the camera bounds while the player stays in the
  same area, with no room transition.
- **Auto-scroll:** in some sections, the screen scrolls by itself and the player must keep up.
- Other behaviors that may come later, for example a locked camera for a boss fight, a camera
  that moves along a path, or a camera that follows a target other than the player.

What X6 does (from `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, section "Camera", and
`docs/stages/shark-2.md`):

- Each layer camera has current limits (min/max x and y) and target limits. The current limits
  move toward the target limits at a set speed.
- Camera zones (`CameraZone`) are rectangles in the stage. While the player is in a zone, the zone
  writes new target limits. Boss gates and some doors also set target limits.
- Auto-scroll (`AutoScroll` in Commander Yammark's stage, area 1) is an object that sets
  min x = max x = camera x and moves it to the right at 0.5 px per update. So in X6, auto-scroll
  is only bounds that move, and the normal follow logic stays the same. A stage flag ends it.

Problems now:

- `MegaLevelScreen` is tightly coupled to `CameraManagerForRooms`. It sets the transition
  callbacks, reads `currentGameRoom` and `priorGameRoom`, and sends the room events
  (`BEGIN_ROOM_TRANS`, `CONTINUE_ROOM_TRANS`, `END_ROOM_TRANS`, `SET_TO_ROOM_NO_TRANS`) from those
  callbacks.
- Room logic and camera logic are mixed in one class. Music, boss rooms, gravity direction and
  enemy cleanup are tied to room transitions in the level screen.
- No logic exists for X-style camera bounds or auto-scroll.

Goal:

- An interface for camera control, so that the level screen does not depend on one camera
  manager.
- Room-based bounds, X-style bounds and auto-scroll as options that a level can use. A level can
  use more than one, for example room bounds with one auto-scroll section.

Research first (write the result in the branch `docs/NOTES.md` before any code changes):

1. **Reuse the focus logic, or make a first-class design?**
   - Option A: keep the current `IFocusable` follow logic. For auto-scroll, the camera follows an
     invisible focusable object that moves at a set speed.
   - Option B: make each behavior first-class, as its own class, function or branch of logic.
   - Option C (the X6 way): keep one follow logic, and express each behavior as changes to the
     camera bounds (for example, auto-scroll is bounds that move).
   - For each option, find out: how does the player interact with the scroll (pushed by the screen
     edge, crushed against a wall, killed off screen)? How does it work with room transitions,
     checkpoints and respawn? How do level designers set it up in Tiled?
2. **One class or several?**
   - One interface with one implementation class that handles all camera behaviors.
   - Or one interface with several classes (for example one per behavior, or small parts that
     combine: a target source, a bounds source, a mover).
   - Compare: how easy it is to add a new behavior, to test a behavior alone, to switch between
     behaviors in one level, and to read the code.
3. **Who owns the room logic?** Find out which of the room-transition side effects (music, boss
   rooms, gravity direction, enemy cleanup, controller on/off) belong to the camera and which
   belong to a separate room or level system.
4. **Examples from other games and engines** for camera bounds, camera zones and auto-scroll.

Contract notes: the current room-based camera behavior must not change in any existing level.

## Camera rules research for `051c` and `051d` (2026-10-30, at `6b0097aa1`)

A research agent stress-tested a first model. It read the code and the live TMX files; it did not run the game. Results
marked `[inferred]` were not verified. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`. `MLS` = `C/screens/levels/MegaLevelScreen.kt`. X6 files are in
`~/IdeaProjects/mmx-demake-tools/docs/`.

### X6 facts

- Limits (`engine.md`, "Camera"): current and target limits, written as camera positions in a view that never rotates.
  `CameraLimitEase` moves a limit relative to the camera.
- Zones (`engine.md`, "Camera zones"): a rectangle and 4 limits (`0xFFFF` = keep). While the player is inside, the zone
  writes the target limits. Nothing restores them when the player leaves. Off screen, the zone deletes itself.
- Gates (`enemies/boss-gate.md`): a gate writes new limits. Gate id 30 leaves max below min, and the camera stays at min.
- Warps (`stages/shark-2.md`): the warp writes the camera and both limit sets directly.
- Forced scroll (`enemies/conveyor-belt.md`, "The forced scroll"): rightward only, min x = max x = camera x (current and
  target), 0.5 px per update. It adds 0.5 to the walk speed. A stage flag ends it. It waits for a checkpoint and a player
  state. `stages/shark-alt.md`, "Checkpoints": a checkpoint deletes the scroll.
- Player at the edge (`engine.md`, "Camera"): `CameraClamp` clamps the player in x only. Y has a fall death.
- Crush (`players/ladder-hurt-events.md`, step 1): terrain on both sides. The screen-edge crush is documented for the Ride
  Armor (`players/ride-armor.md`, "Crush, spikes and pits while riding"). The on-foot case is not documented.

### Corrections to the first model

- `CameraRoomBounds` runs before the transition (`MLS:287-290`). On a reset it ends the mover list, so that frame has no
  clamp (`CameraRoomBounds.kt:61-67`). This tracking step must stay first.
- Effects run during a pan (`CameraController.kt:24-25`). The END frame of a pan also holds, and `onEnd` runs there.
- `CameraFollow.maxX` is the world width with no half view subtracted (`MLS:386`), so it does nothing in practice.
- `Float.coerceIn` throws when min > max (`CameraFollow.kt:38,59,65`), so a rule must never write an empty range to these
  fields.
- A pipe exit is a hard cut: `PipePortal.pushEntityOut` calls `stopSmoothFollow()`. The problem there is the limits.
- Maverick has kill paths that a crush can reuse. The agent's claim that none exists was wrong. See "Crush".

### Failures of the first model

34 combinations of 2 or more rules: 23 break, 7 are ambiguous, 4 work. The first model had "last entered zone wins", a
stack of zones, snap for rooms, and "limits ease after a pan".

| Root cause | Combinations | Result |
|---|---|---|
| "Last entered wins" depends on the path | same overlap walked from 2 sides; respawn in an overlap; zone exit during a pan | Same tile gives 2 views; a respawn cannot reproduce them |
| Snap and ease conflict, with gaps | scroll end in a wider room; pipe into a far zone; pan with a different cross axis; zone in a gap | Up to 23 live room pairs (upper bound) would change, for example `DesertMan` `room9` to `room10`; Megaman stays out of view during an ease |
| The scroll waits only for a pan | rotation in a scroll; gate in a scroll; boss health fill | The camera scrolls past a frozen world (`MLS:601-614`, `MLS:654-683`) |
| The edge rule can soft-lock | limit at a room edge; bottom edge over a pit; pinned against a wall | The body never reaches the next room; the collision handler can push a body through a wall (`StandardCollisionHandler.kt`) |
| A limit has no kind | rotation at a tight limit; locked boss zone | A rotated view swaps 16 and 14 tiles (`CameraRoomClamp.kt:33-39`), so a lock as a range has slack |
| "Nothing shows outside the room" is false | shake; reset frame; mid-turn rotation; room shorter than the rotated view | The rule needs a defined meaning in a gap, where `currentGameRoom` is `null` |

Other findings:

- A shake writes `position` after the clamp. The ease must not read a shaken position, or the limit takes a false step.
- A zone test on a body rectangle can flip at the border. Test the focus point.
- Enemies that spawn behind a limit are culled after 1 s, and ABSTRACT bodies freeze out of view
  (`IBodyExtensions.kt:24-30`). This is the open `041` question.
- A non-player camera target drags Megaman through a pan, because the pan reads `follow.focus` (`CameraRoomTransition.kt:54`,
  `Megaman.kt:544-547`). A future target must separate "camera target" from "subject".
- The screen-edge rule must run before collision. `GravityBlock` shows the pattern (`body.preProcess`).

### Model comparison

| Model | Same tile, same view | Respawn | Scroll | Cost |
|---|---|---|---|---|
| Last entered, per edge | no | no | needs pins | stack state |
| Numeric priority per edge | yes | yes | needs pins | numbers to maintain |
| Intersection of all sources | yes | yes | a scroll cannot widen past a zone; a handover shrinks the view | no override possible |
| Fold in Tiled order, with pins (chosen) | yes | yes | yes | object order in Tiled |
| X6 latch | no, by design | only with limits in checkpoint data (`036`) | yes | one limits record per checkpoint |

### Crush

Maverick has 3 kill paths, and none tests for a player pinned between 2 solids:

- `GravityBlock.kt:213-218`: in `body.preProcess`, the block is not on the ground and overlaps the player's head fixture.
  It ignores invincibility.
- `CeilingCrusher` with `DeathRule`: a `DEATH` fixture with `ConstKeys.INSTANT`.
- `PlayerDeath`: a sensor rectangle with an `instant` option.

The last 2 are kill areas. `GravityBlock` is the real crush case.

### Players-only blocks

- `AbstractBlock` has `NO_BODY_TOUCHIE`, `NO_SIDE_TOUCHIE`, `NO_FEET_TOUCHIE`, `NO_PROJECTILE_COLLISION` and a
  `Receivers.FILTER` that accepts only players. `GroundSlideBlocker` extends it.
- That filter acts on contacts only. `WorldSystem.resolveCollisions` (`WorldSystem.kt:255-272`) calls the collision
  handler with no contact filter, so body collision reads only `blockFilters` (`MegaCollisionHandler.kt`). A dynamic
  enemy collides with an `AbstractBlock`.
- `AbstractBlock.onSpawn` replaces `ConstKeys.BLOCK_FILTERS` with a tag set, so a lambda that a caller passes is lost. A
  subclass must add its filter after `super.onSpawn`.
- `NO_SIDE_TOUCHIE` means that an edge block never sets a side sense. A set sense on the opposite side is a real wall.

### Other games (from general knowledge, not checked against sources)

Mega Man X: limits usually sit at real walls and doors, with a player clamp. Super Mario Bros. 3: in auto-scroll, the
screen edges act on the player. Sonic: the player stops at the camera limits in boss arenas. Hollow Knight: camera locks
do not stop the player; real walls do.
