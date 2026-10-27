# Contract: mega-refactor-062c-existence-readers

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: everything. The branch only changes how code reads the existence state. Every read gives
the same value as before on every path: `x.existenceState.liveNow` = the old `x.spawned`, and
`x.existenceState.liveAfterUpdate` = the old `!x.dead`. No reader changes the question it asks.

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [ ] The build passes and the jar builds.
- [ ] All engine and core tests pass, 0 failures. The counts go up only by the new tests.
- [ ] The compiler warnings are the same set as the baseline (counted at plan step 1), less any warning that a
  removed cast had.
- [ ] **Temporary contract suite** `TempOldExistenceFlagsTest` (engine test sources), with a frozen copy of the old
  getters (`TempOldExistenceFlags`: today's `spawned` and `dead` formulas over the state): for each of the five
  states, `liveNow` equals the old `spawned`, and `liveAfterUpdate` equals the old `!dead`. Deleted in the last commit
  before the merge; that commit runs the other checks again.
- [ ] **Substitution check** (a scratchpad script, `062c-check-substitutions.py`, run at the end of each batch and on
  the full branch diff): in `main` code, every changed line, after the substitution is undone
  (`!x.existenceState.liveAfterUpdate` → `x.dead`, `x.existenceState.liveAfterUpdate` → `!x.dead`,
  `x.existenceState.liveNow` → `x.spawned`, and the dropped casts put back), equals its old line. The only other
  changed lines are the enum, the removed getters and `MegaGameEntity.toString`. The script lists every line it cannot
  match, and each one is reviewed by hand and written in the process log.
- [ ] **Permanent engine test:** both values for each of the five states (the table in `docs/SCOPE.md`).
- [ ] `IGameEntity.spawned` and `MegaGameEntity.dead` no longer exist, so the compiler shows that no reader was left
  out. `Spawner.spawned` and the `MegamanWeaponsHandler.spawned` map are unchanged.
- [ ] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

A short sanity pass. The automated checks above prove that each read gives the same value, so these checks only look
for a mistake that the checks cannot see. The owner plays them once, after the implementation, from the worktree.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die; during the death, the pause button does
  nothing; the respawn. Then lose all lives: the game over screen, the save screen, and a second level start from
  the menu that looks and acts as the first (the beam-in, when the pause button starts to work).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: jump, slide, wall slide, air dash, swim or climb
  where the room has it; shoot (the shot limit works: shots that leave the screen free a slot), charge, switch
  weapons, pause.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: freeze the `Met` with Frigid Shot: the ice shield
  shows; it thaws; freeze it again: the shield shows again. Kill it: it takes no more damage after its death; it
  comes back when the camera leaves and returns.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room5`: the `CannoHoney` hives send bees; kill bees and a hive;
  leave and come back: the hives and bees come back.
- `./run-game-alpha.sh --level DESERT_MAN --room room3`: the `CrumblingBlock`s crumble once and come back.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room2`: the `LaserBeamer` fires at Megaman; it stops while
  Megaman is dead.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: go on to the boss. Her shield gems form and break; use
  Precious Guard after the fight if it is available (the gem cluster forms around Megaman and drops lost gems).
- `./run-game-alpha.sh --level MOON_MAN --room room14`: go on to the boss. The scythes, the gravity changes, the
  defeat and the beam-out.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: use a `CapsuleTeleporter` and a `GravitySwitcharoo`.

## Behaviors to check by review

- The two holders that mix the questions (`WilyFinalBoss` orbs at `:2576` and `:3015`, `FreezableEntityHandler` at
  `:59` and `:64`) keep their two different questions.
- `MegamanMaverickGame.kt:931` (the pool return of a rejected spawn): `!entity.spawned` becomes
  `!entity.existenceState.liveNow`; `engine.contains(..., containedIfQueuedToSpawn = true)` stays for `062d`.
- `GameEngine.reset()`/`dispose()`: the filter `it.spawned` becomes `it.existenceState.liveNow`.

## Intended changes

None.
