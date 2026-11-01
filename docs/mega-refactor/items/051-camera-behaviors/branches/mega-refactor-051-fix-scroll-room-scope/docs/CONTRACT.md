# Contract: mega-refactor-051-fix-scroll-room-scope

- Status: approved
- Approved: 2026-10-31

## Automated checks

- [x] `./gradlew engine:test core:test lwjgl3:jar` passes.
- [x] No unique compiler warning is new.
- [x] Tests: a section in another room is not done after a respawn, a kept edge does not apply in another room, and no edge acts at
      the end of a pan.

## Manual playthrough

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051-fix-scroll-room-scope`

1. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`: climb into the top room and back down. He does not die.
2. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_rooms`: the scroll waits at the room edge, the pan runs, and it continues.
3. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_spawn_past`: walk left. The section stays done, and its limit holds.

The combined play test on the parent continues after this branch merges.

## Intended changes

None for levels with no scroll section.
