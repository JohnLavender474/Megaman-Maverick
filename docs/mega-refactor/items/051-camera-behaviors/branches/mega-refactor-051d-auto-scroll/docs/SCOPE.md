# Scope: mega-refactor-051d-auto-scroll

- Status: approved
- Approved: 2026-10-30

This draft assumes that `051c` is merged. Check each reference again at branch start.

## Goal

A level can define scroll sections in Tiled. A section takes control of the camera, moves it along one axis, and holds
the players in view with players-only blocks. Levels with no sections play the same.

## Item

`051`, from the `ITEM.md`, "Decisions" of 2026-10-30: the scroll section, the stop modes, the permanent trailing block,
the room rules, spawn and rotation in a section, and the `next` chain. The research is in the item `NOTES.md`, "Camera
rules research for `051c` and `051d`".

## Current state

- `051c` adds the limits result (ranges), the Tiled layer, the hold test, the edge block and `CrusherComponent`. Zone limits
  are Tiled object references, and `051c` has no pin.
- X6 (`enemies/conveyor-belt.md`, "The forced scroll"): rightward only, min x = max x = camera x, and it writes current
  and target limits together, so it has no ease lag. A stage flag ends it.
- `PipePortal.kt:92` and `CapsuleTeleporter.kt:124` link Tiled objects with a next key.
- The pan cannot keep a scroll running: it waits 0.35 s, moves 1 s and destroys the enemies (`LevelRoomHandler.kt`).

## In scope

1. **The section.** `type=scroll` objects in the `051c` layer, with a direction (left, right, up, down) and a speed.
2. **The scroll part.**
   - The section starts when `getFocusPosition()` enters the rectangle. It takes control from the current camera
     position, with no jump.
   - It writes a pin on its axis, which this branch adds to `CameraAxisLimit`. The pin skips the ease, as in X6. The other
     axis keeps its follow and its limits.
   - The camera position is the scroll position, so a pan needs no stored state. The section keeps its own value in the
     frame, so that shake does not add to it.
   - One scroll is active at a time. A zone still applies on the other axis.
   - The room cut stops the camera at a room edge, and the section waits there. After a pan, the scroll continues from
     the camera if the section overlaps the focus.
3. **Stop.** Mode `exit` (default): the trailing edge of the view passes the far edge of the rectangle. The pin goes
   away, and the follow takes control with an ease.
4. **Edges.**
   - The leading edge has a block. It turns off at a room edge.
   - The trailing edge has a block, and a crush kills a player pinned against a solid. It stays on at a room edge.
   - A trailing edge in the direction of gravity kills in place of a block, as a fall off the screen does.
   - After the stop, the trailing block and its limit stay. A respawn clears them, then derives them: a section counts
     as done if the camera at the spawn is already past the stop.
   - A per-section `edge_blocks` property overrides the defaults.
5. **Spawn and holds.** The scroll runs while a player spawns. Edge blocks and crush stay off until `PLAYER_READY`. The
   scroll waits for a pan, a gate, the boss health fill, a camera turn and a dead player, through the `051c` hold test.
6. **Chains**, a separate commit group. A `next` object reference, one way, starts that section when this one stops, with no
   position test. Stop mode `arrive`: the leading edge reaches the far edge. A section with no `next` that overlaps
   another one starts after the first stops, in Tiled order.
7. **Test rooms** in `RefactorTest051.tmx`. The basic rooms come first and are the first play checks (owner decision,
   2026-10-30); the other rooms come after they pass.
   - `scroll_corridor`: a long corridor. The scroll starts when Megaman reaches a set spot, runs right, and stops at the
     limit. Low floor blocks that he can jump, and a block that pins him against the trailing edge to test the crush.
     Enemies and hazards along the way show that they work in a scroll.
   - `scroll_turn`: a scroll hands over to a scroll in a new direction, for example right then up, through `next` and
     stop mode `arrive`.
   - Other rooms: a section inside a wider room; a wall in the path; a gate in a section; a gravity switch in a section;
     a section across 2 rooms; a spawn inside a section, and one past its stop; a section over a pit.
8. **Permanent tests** for the start, the stop modes, the respawn derivation, the room rules, and the chain.

## Out of scope

- A speed change for the player. Item `086` holds the component. A boost would be a term in `getMaxRunSpeed()`, not
  `movementScalar`, which does not change the top speed. X6 adds its boost in a room with a belt that slows the player, so
  a Maverick scroll with no belt may not need one (inferred).
- Diagonal scrolls, and a scroll that does not stop at a room edge.
- A section that ends on an event, for example a boss death. See question 2.
- A parent object for chains, and a level validator (owner decisions).
- Rotation as a part: `051e`.

## Affected areas and risks

- Files: the new scroll part, the `051c` layer builder, `CameraEdgeBlock`, `MegaLevelScreen.kt`, tests, the test map,
  root `CLAUDE.md`.
- Risk: a scroll moves enemies and blocks out of view, and 197 lines use camera culling, which normally acts after 1 s.
  A scroll removes what it leaves behind, as X6 does. Check the test rooms for an entity that must stay.
- Risk: a checkpoint pops when the camera overlaps it (`PlayerSpawnsManager.kt:69`), so a scroll can activate one before
  the player reaches it. This is a level design matter, with `036` as the later answer.
- Risk: at a room edge, the trailing block must never block the only way forward.

## Overlap with other branches

Check at branch start. `084c-movement` and later `084` branches may change the player fixtures that `CrusherComponent`
reads.

## Open questions

None. Answered on 2026-10-30:

1. A respawn in a section: the camera resets to the spawn and the scroll continues from there, also near the far edge.
   The designer chooses the spawn point.
2. No event field until a level needs one (owner accepted the recommendation).
3. The trailing edge kills in the direction of gravity, so the rule follows a rotated player.

## Scope changes during the branch

Owner approved on 2026-10-31, after the first play test:

- A crush needs a marked `crush_surface` in place of any solid. `needsSolidOpposite` and the body sense go away, so a contact with
  the floor cannot cause a death. An edge block crushes only in a frame where it moves into the view.
- A sensor has a `section` object reference to a zone or a scroll section, because Tiled properties cannot hold a list. It is
  active while that owner is active. There is no "in view" test, because a crush touches an edge block at the view edge.
- A player fully past the trailing edge of a running section dies, so that an unmarked solid cannot cause a soft lock.
- A block covers the rectangle of its section or zone across its edge, not the view. There is no property for this. If a level
  needs a different extent, add one optional object reference then.
- An edge in the fall direction has no block, and a player fully past it dies. This applies to the leading edge too, except at a room
  edge, where the normal room transition takes over. A listed `edge_blocks` edge gets a block and no fall kill. This replaces item 4:
  "A trailing edge in the direction of gravity kills". The tables are in the item `NOTES.md` and on `CameraScroll`.
- The scroll waits until the player is ready. This replaces item 5, "The scroll runs while a player spawns". With the old rule, the
  scroll moved during the beam and the spawn point could leave the view. The owner played the camera jump at the ready frame.
- A pinned axis writes to the limits during a hold, and the view size blends over a camera turn, for the clamp, the target and the
  out-of-view test. `viewWidth` and `viewHeight` stay final. Levels with no section can change by 1 tile or less for 0.75 s or less,
  near a room limit, in a turn that changes the view size (owner approved). A snap of up to 1 tile can stay at the end of a turn near a
  room cap or a stop position. It waits for a real level.
