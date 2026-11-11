# Contract: mega-refactor-060b-protoman-move

- Status: approved
- Approved: 2026-11-11

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests for Proto Man's run and jump through `InputFrame`, and the
  `REFACTOR_TEST_060` row in `LevelCompletionScreensTest`.
- [ ] `./gradlew core:smokeTest` passes: `Protoman.init()` finds every atlas region; `entity-tags.txt` has his line.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from the branch worktree. Proto Man checks first (new behavior), then the control checks.

Proto Man (new):

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room walk`: he beams in (Megaman's blue beam for now) and stands;
  the scarf flicks now and then; walk both ways (4-frame cycle), stop (the step frame shows); his feet touch the floor;
  he faces the way he walks.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room jump`: tap and hold `A` for a short and a high jump; fall off
  a ledge; land; fall into the pit: death (orb ring) and respawn.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room water`: in water he falls slower and jumps higher from the
  floor, and he can jump out of the pool. He does not swim: `A` in mid-water does nothing (owner, 2026-11-11).
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room transition`: walk into the next room; the room transition
  carries him.

Control (no change):

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: Megaman runs, jumps, slides and air dashes as
  before.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat`: X walks, jumps, dashes and slashes as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: Megaman blinks after a hit, as before (the shared
  sprite hide and flash rule).
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: Megaman in each gravity direction; his sprite and
  his jump point the right way (the shared direction sign).
- `./run-game-alpha.sh --level X6_INTRO --room room1 --heartTanks all`: X blinks after a hit, as before.

## Behaviors to check

- The pause menu and the HUD health bar show for Proto Man (any Proto Man check).

## Intended changes

- A new playable character, Proto Man, on the new test level `REFACTOR_TEST_060` only. No real level changes.
