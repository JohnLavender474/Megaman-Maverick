# Contract: mega-refactor-062b-spawn-order-guards

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: on every call order that is not one of the four odd paths (A to D in `docs/SCOPE.md`), the
engine does exactly what it does today: the same `init`, `onSpawn`, `onDestroy`, component resets and system changes
in the same order, the same return values, the same state. On the four paths it follows the new rules, and nowhere
else does anything change.

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [ ] The build passes and the jar builds.
- [ ] All engine and core tests pass, 0 failures.
- [ ] The compiler warnings are the same set as the baseline (counted at plan step 1).
- [ ] **Temporary contract suite** `TempOldEngineContractTest` with the frozen copy `TempOldGameEngine` (today's
  engine, after `062a`), as in `062a`: at least 10,000 seeded random sequences with nested calls from
  `onSpawn`/`onDestroy` and deferred resets. Before each call, the suite decides from the old engine's state whether
  the call starts one of the four odd paths. Until the first odd call in a sequence, both engines must give the same
  action log, return values and states after every call. From the first odd call on, the new engine alone is checked
  against the rules by an oracle that the suite keeps itself (which entities must be live after the update, and the
  order of `onSpawn`/`onDestroy` per entity). The suite counts each odd path (each must occur at least once). Deleted
  in the last commit before the merge; that commit runs the other checks again.
- [ ] **Permanent engine tests:** one test per rule (1 to 4 in `docs/SCOPE.md`), the `RESPAWN_QUEUED` state and its
  `spawned`/`dead` values, the self-destroy inside `onSpawn` (`EventTrigger` path) still works, and on seeded random
  sequences: `onSpawn` and `onDestroy` alternate for every entity, `spawned` = in the engine, not `dead` = in the
  engine after the next update.
- [ ] **Core tests:** the ice shield after a thaw and a freeze in one frame is live after the update; a child enemy
  and its parent destroyed in the same frame: the child's `onDestroy` runs once (its destroy runnables run once).
  Both written first and run on the baseline, where they must fail (the result goes in the process log).
- [ ] `contact-rule-selection.txt` is unchanged.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree. Each command ends with a copy of the log
(each run deletes `assets/logs.txt` at start); the `GameEngine` lines show any refused call.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim --logTags GameEngine; cp assets/logs.txt logs-062b-enemy_aim.txt`:
  freeze the `Met`, let it thaw, freeze it again: the shield shows each time; kill it; it respawns when the camera
  leaves and returns.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups --logTags GameEngine; cp assets/logs.txt logs-062b-pickups.txt`:
  each pickup works; a killed `Met` can drop an item.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death --logTags GameEngine; cp assets/logs.txt logs-062b-death.txt`:
  the death and the respawn; then lose all lives: the game over and a second level start.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room5 --logTags GameEngine; cp assets/logs.txt logs-062b-cannohoney.txt`:
  the `CannoHoney` hives send bees; kill bees and a hive; leave and come back: the hives and bees come back.
- `./run-game-alpha.sh --level DESERT_MAN --room room3 --logTags GameEngine; cp assets/logs.txt logs-062b-toriko.txt`:
  the `TorikoPlundge` enemies and their blocks: kill them, leave and come back.
- `./run-game-alpha.sh --level RODENT_MAN --room room13 --logTags GameEngine; cp assets/logs.txt logs-062b-bigjoe.txt`:
  go on to the `BigJumpingJoe` (two rooms on); kill it: exactly one `SniperJoe` comes out.
- `./run-game-alpha.sh --level RODENT_MAN --room room16 --logTags GameEngine; cp assets/logs.txt logs-062b-rodentman.txt`:
  the boss fight with its rats; the defeat (one set of defeat orbs) and the beam-out.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --logTags GameEngine; cp assets/logs.txt logs-062b-intro.txt`:
  the boss intro, fight, defeat and beam-out.

## Behaviors to check by review

- `EventTrigger` and `MegamanTrailSpriteV2` destroy themselves in `onSpawn`: still the normal path (a permanent test
  covers it).
- `MegaLevelScreen.kt:948,973`: a refused spawn of a live entity would call the spawner's `onRejected`; spawners
  supply fresh entities, so this does not happen (scope scan).

## Intended changes

1. A thaw and a freeze of an enemy in one frame leave its ice shield live after the update (today it is destroyed
   until the next freeze).
2. A child and its parent destroyed in the same frame: the child's `onDestroy` runs once (today twice: a second
   disintegration, item roll, `BigJumpingJoe`'s `SniperJoe`, or boss defeat orbs).
3. No known caller: a spawn of a live entity is refused (today `onSpawn` runs again); a destroy of an unspawned entity
   is refused (today `onDestroy` runs again); a spawn and then a destroy in the destroy phase end with the entity
   destroyed after `onSpawn` and `onDestroy` (today `onDestroy` runs first and the entity ends live).
