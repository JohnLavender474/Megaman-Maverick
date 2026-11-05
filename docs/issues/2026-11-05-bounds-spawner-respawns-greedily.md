---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: owner play test of x6-intro-extra-level (2026-11-05)
related-items: []
---

# A camera-bounds spawner respawns its enemy after a step of 1 px out of the view and back

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/spawns/SpawnerForBoundsEntered.kt`: one rectangle (the camera bounds) starts the spawn and also re-arms the spawner. `onReleased()` sets `isEntered` to the current overlap.
- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/tiled/layers/SpawnersLayerBuilder.kt`: the `else` branch makes this spawner for each map object with no `spawn_type`.

## What happens

1. The spawn shape of an enemy enters the camera bounds, and the enemy spawns.
2. The player kills the enemy.
3. The player moves so that the spawn shape leaves the camera bounds by 1 px or more. The spawner re-arms.
4. The player moves back. The spawn shape enters the camera bounds again, and the enemy spawns again.

In the X6 intro stage, two Meta Wheels roll to the left. The player goes back to the left to avoid them and kills them. When the player goes to the right again, the wheels spawn again at once. The player can be held in this loop.

The start edge and the re-arm edge are the same line. Found in play; the cause comes from reading the code and from the test `SpawnerReleaseTest` ("respawns an entity that died on screen only after its area leaves the camera and enters again").

## What should happen

A map object can set a re-arm margin. The spawner re-arms only when the spawn shape is outside the camera bounds grown by the margin on each side. The spawn still starts at the normal camera edge. With no margin, the behavior does not change. This is near to how the X games keep a killed enemy off until the player goes a distance away (owner decision, 2026-11-05: option 3 of three options; a respawn delay and a re-arm zone object were not chosen).

## How to reproduce

1. `./run-game-alpha.sh --level X6_INTRO` on the branch `x6-intro-extra-level`, in the room with the two Meta Wheels.
2. Go back to the left, and kill both wheels.
3. Go to the right. The wheels spawn again as soon as their spawn shapes enter the view.

## Short contract

Branch: `fix/spawner-rearm-margin`, off `master`, in `.worktrees/fix/spawner-rearm-margin`.

Intended changes:

- New map object property `rearm_margin` (tiles, float, default `0`) on an object of a spawner layer with no `spawn_type`. After its entity is destroyed, the spawner re-arms only when the spawn shape does not overlap the camera bounds grown by `rearm_margin` tiles on each side. A negative value throws at map load.
- The spawner checks the re-arm rule on each frame that the game is not paused, also during a room transition, when spawns are not tested. So a camera that passes the margin during a transition pan re-arms the spawner.
- At `PLAYER_SPAWN_INIT` (a respawn after a death), each spawner with a margin above `0` re-arms, so that a new attempt sees all its enemies again. A spawner with margin `0` keeps its current behavior at a respawn.
- New test map `FixTestSpawnerRearm.tmx`, with the `LevelDefinition` entry `FIX_TEST_SPAWNER_REARM` of type `TEST_LEVEL`. It stays on `master` after the merge.

Changed files: `SpawnerForBoundsEntered.kt`, `SpawnerFactory.kt`, `SpawnsManager.kt`, `ISpawner.kt`, `SpawnersLayerBuilder.kt`, `MegaLevelScreen.kt` (one call at `PLAYER_SPAWN_INIT`), `Constants.kt` (one key), `LevelDefinition.kt` and `LevelCompletionScreensTest` (the test level), tests, the test map, and the docs for the property (`level-designer` skill, rule 10; the `level-builder` skill writes no enemy properties).

Not changed:

- A spawner with margin `0` (all objects in the current maps): it spawns, re-arms and respawns as today, also at a respawn after a death.
- `SpawnerForEvent` and the `spawn_room` and `spawn_event` spawners.
- `respawnable = false`: such a spawner spawns once, as today.
- The spawn edge: the spawn still starts when the spawn shape enters the camera bounds.
- `engine/`.

Checks:

- Unit tests of `SpawnerForBoundsEntered` with no world:
  - margin `0`: the current `SpawnerReleaseTest` cases still pass.
  - kill in view, a step out by less than the margin and back: no respawn.
  - kill in view, a step out by more than the margin and back: one respawn.
  - kill out of view, past the margin: a respawn at the next entry.
  - the margin is passed while `shouldTest` is false (room transition): the spawner re-arms.
  - a level reset (`rearm`) with margin above `0`: the spawner spawns at once if its shape is in view; with margin `0`, no change.
  - `respawnable = false` with a margin: one spawn only.
- A test that a negative `rearm_margin` throws, with a clear message.
- Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/spawner-rearm-margin`:

- `./run-game-alpha.sh --level FIX_TEST_SPAWNER_REARM --room margin`: kill the enemy. Go back by a few tiles, so that its spawn point just leaves the view, and come back. The enemy does not spawn again. Go back past the marker, and come back. The enemy spawns again.
- `./run-game-alpha.sh --level FIX_TEST_SPAWNER_REARM --room death`: kill the enemy near the start, then die in the pit. After the respawn, the enemy is there again.
- `./run-game-alpha.sh --level FIX_TEST_SPAWNER_REARM --room control`: the same layout as `margin`, with no `rearm_margin`. Kill the enemy, step back until its spawn point leaves the view, and come back. The enemy spawns again, as today.
- `./run-game-alpha.sh --level INTRO_STAGE`: play the first rooms. Enemies spawn and respawn as before.

## Notes

- A spawner with margin `0` can stay off after a death near the checkpoint: at the destroy, it reads the camera of the death position, not the camera at the checkpoint. This fix does not change that (see "Not changed"). It is a question for the owner.
- After the merge, `master` is merged into `x6-intro-extra-level`, and the Meta Wheels in `X6Intro_16x14_v1.tmx` get a `rearm_margin`. The value comes from a play test there.
