# Scope: mega-refactor-051-fix-scroll-room-scope

- Status: approved
- Approved: 2026-10-31

The combined play test found that Megaman dies at the end of a pan down from `basic_up` to `basic`.

## In scope

1. `deriveDoneSections` checks only the sections that overlap the room of the spawn.
2. Kept edges apply only in a room that their section overlaps.
3. `CameraLimitStage` gives no edge while `limitRoom` is not the current room.

## Out of scope

A section in an earlier room does not count as done after a respawn in a later room. Kept edges normally stop that walk back. A `TODO`
marks this.
