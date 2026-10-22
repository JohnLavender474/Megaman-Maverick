# Contract: mega-refactor-019a-remove-second-spawn-pass

The gameplay that this branch must not change, and how to check it. Do the checks before the
branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-10-22

This branch removes one call. In normal play, nothing should change: no spawn gets earlier or
later (see `docs/SCOPE.md`). The play test checks the spawns that depend on spawner state:
event (`spawn_room`) spawners and camera spawners.

## Baseline

Take it on the branch before any change, with the command below and Corretto JDK 17. Expected
from the `001a` merge: `engine:test` 427 tests and `core:test` 49 tests, all pass. Write the
exact numbers in `NOTES.md`.

## Automated checks

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

- [x] The build succeeds and the JAR builds.
- [x] `engine:test` and `core:test` give the same results as the baseline.

## Source checks

- [x] The diff changes only `MegaLevelScreen.render()`: one `spawnsMan.update(delta / 2f)` call is
  removed, the other becomes `spawnsMan.update(delta)`, and one `TODO` comment is added.
- [x] `playerSpawnsMan.run()` and the second drain loop of `spawns` are still in place, in the
  same order.

## Manual playthrough

Result (2026-10-22, owner): pass. Entities spawn as expected.

| Level | What to check |
|-------|---------------|
| Intro stage | Enemies, blocks and items appear as before when the camera reaches them. Leave a room and come back: the room's `spawn_room` objects appear again. |
| Desert Man | Room `room14`: `TorikoPlundge` (a `spawn_room` enemy) spawns when you enter the room, and again after you leave and come back. |
| Wily Stage 1 | Room `room8`: `BulbBlaster` (a `spawn_room` enemy) spawns when you enter the room. |
| Any level | Die once. After the respawn, the room's objects spawn as before. |

## Behaviors to check

- [ ] Room transitions: objects of the new room appear at the same moment as before (no visible
  delay).
- [ ] Enemies that leave the screen and come back into view respawn as before.

## Intended changes

Two rare cases that the owner accepted (2026-10-22). Neither was seen in normal play:

- A `spawn_room` entity that dies in `engine.update` in the same frame as an accepted event for
  its own room no longer accepts that event. It appears again at the next room entry or respawn.
- A camera-spawned entity that dies off screen in the frame just before its spawn point scrolls
  into view respawns only after the camera leaves its spawn area and comes back.

A `TODO` comment at the spawn pass names both cases. The fix is the destroy notification in
`019b`.
