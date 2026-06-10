# Room Transitions

## Auto-transition (no Gate needed)
`CameraManagerForRooms` automatically transitions the camera when the player's focus bounds
overlap a neighboring room's rectangle. This means **adjacent rooms with open shared boundaries
transition automatically**.

Rooms that flow into each other horizontally typically have open shared boundaries except at
meaningful checkpoints in the level (before a boss, mini-boss, or other significant room).

Vertically stacked rooms (same x range, touching y boundaries) work this way too, but there 
should be either a ladder or gate between the two.

## Gate sensor (locked door)
Use a `sensors:Gate` object when a physical door must block the player until triggered.

Standard placement for a **horizontal** transition (room A left of room B):
- Gate is 64 × 96 px, straddling the shared x boundary (e.g. x=boundary−32, so it overlaps both rooms).
- Gate has property `room=<target_room_name>`.
- Accompanied by a full-height wall block (same x, full interior height) that surrounds the door.
- Accompanied by a gate-height block (same x, same 64×96 size) that overlaps the Gate position.

See `assets/tiled_maps/tmx/Test8.tmx` object id 40 (Gate at x=1568) for a canonical example.

## Vertical transition via ladder
For a ceiling exit (room below → room above), leave the ceiling/floor tiles open at the ladder
column and use a `specials:Ladder` spanning both rooms. See `LadderSetup.md`.
