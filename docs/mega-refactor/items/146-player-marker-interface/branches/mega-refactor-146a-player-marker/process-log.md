# Process log: mega-refactor-146a-player-marker

## Status

- State: active
- Goal: A marker `IPlayer` in the engine; player call sites take the player type
- Done: branch start, planning, steps 1-10, 2 reviews before the play test
- Next: the owner play test and the Diffity review
- Blocked:
- Brief: https://claude.ai/artifact/Lw2BdizeKYA9Fq7AzC7cLG

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-10

- Branch start. Owner decisions (in `ITEM.md`): the marker design, merge directly into `master`, start now in
  parallel with `060a` and `081f`, and `Event.isDeathOf` changes here.
- Planning scan (1 read-only agent; results in `temp/mega-refactor-146a-player-marker/scan.md`). Draft `SCOPE.md`,
  `CONTRACT.md` and the brief. The brief asks 3 decisions: a core `IMegaPlayer` now or not, `slot` on a character
  that is not in a slot, and the return type of `targetPlayer`.
- The owner approved the brief and chose the 3 recommendations: no core `IMegaPlayer` (the engine `IPlayer`
  everywhere), `slot` throws on an entity that is not in a slot, and `targetPlayer` returns `IPlayer`. `SCOPE.md` and
  `CONTRACT.md` approved. `PLAN.md` written (6 steps).
- Step 1 (engine `IPlayer`, `PlayerNotRegisteredException`, typed `Players`): `engine:test` passes; `PlayersTest` has
  3 new tests.
- Steps 2 and 3: `Megaman`, `MegamanX`, `MegaPlayers`, the roster, `asPlayer`, `rootPlayer`, `targetPlayer`,
  `isDeathOf` and the player call sites take `IPlayer`. The 26 `playerSlot!!` reads on a player became `slot`. The
  `DarknessV4` light map and the `QuickSand` floor map key on `IGameEntity`, because they also hold other entities.
- Owner decision D4 (see `SCOPE.md`): the block filters take `IGameEntity`, and `playerAllowsDamageFrom` takes
  `IPlayer` and the session rules. Found because `WarpSpotTest` needs both.
- Observation, not checked: `playerAllowsDamageFrom` returns `false` for a player that is not live, so a damager fixture
  may never count as unsafe when `SlotRespawn` tests spots for a dead player. Not changed (refactor only).
- Step 4: the 8 player test doubles implement `IPlayer`. The relaxed `MegaGameEntity` mocks that stood in for a player
  (`targetFor` stubs, `MegaLevelScreenEventsTest`, `LevelRoomHandlerTest`, `CameraEdgeBlockTest`) are now
  `mockk<IPlayer>`. Plan change: the `LevelRoomHandlerTest` mock stays a mock (typed `IPlayer`) and does not become a
  real double, because it reads only the body component, which the test stubs. The `isDeathOf` test "false for an
  owner that is not a player" is removed: the parameter type now forbids that call. New check in `PlayerRosterTest`:
  the roster character that is not in a slot gives `asPlayer == null`, and `slot` throws.
- `core:test` 1293 tests and `core:smokeTest` 451 tests pass. Mutation check: `asPlayer` with no component check
  fails 7 tests, `PlayerRosterTest` among them.
- Step 5: root `CLAUDE.md` explains `IPlayer` (type versus membership) and the return type of `targetPlayer`. No `players/` README on this branch (`060a` adds it).
- Review before the play test (2 agents, read-only, no game launch):
  - Diff review: no bug and no behavior change outside the contract. Fixed: `playerAfterDamage` and `playerOnDestroy`
    are bound to `IPlayer` (no `asPlayer!!`, no `playerSlot!!`); a blank line in `Players.kt`; the scripted imports
    moved to their sorted places (58 files). Left: `PlayerAirDash` and `ControllerComponent` (backlog candidate);
    the `LevelRoomHandlerTest` mock (plan change in `SCOPE.md`). Overlap: `git merge-tree` shows no conflict with
    `060a` (only `Megaman.kt`) or `081f` (only `D1000.kt`); neither adds code that reads a player as `MegaGameEntity`.
    Bug candidate: the inverted loop in `DarknessV4.kt:572` (also on `master`).
  - Play-check review: checks 1, 2, 3 and 5 to 7 are valid; rooms chosen: `X6_INTRO room1`, `PRECIOUS_WOMAN room7`
    (gate room), `DESERT_MAN room1` (`QuickSand`). Check 4 (`koopa_kick`) said "hurts enemies, not players", which is
    wrong: the room has no other enemy, and a rolling shell hurts a player (as on `master`). Text fixed in
    `CONTRACT.md` and the brief. `pickups` gets `--weapons all`.
- Diffity round 1 opened and then stopped with no threads: the owner extended the branch before the review.
- Scope change (owner, 2026-11-10): the tag work joins `146a` (not a `146b`). Tags are a `core` idea: `ITaggable`
  moves out of `engine`; the duplicate `getTag()` overrides go if an investigation confirms that no tag changes. A
  read-only agent runs the investigation (`temp/mega-refactor-146a-player-marker/tags/`). Then `SCOPE.md`,
  `CONTRACT.md`, the brief and `PLAN.md` change, and the owner answers the new decisions.
- Tag investigation (1 read-only agent): 155 overrides, 148 repeat the class name, 4 bases give 34 subclasses the
  parent's tag, 7 differ. Owner chose a `TagsComponent` (not `ITaggable` in core): D5 own name + base name, D6 two
  components (tags and groups), D7 no single-tag accessor. `SCOPE.md`, `CONTRACT.md`, the brief and `PLAN.md`
  (steps 7-10) updated.
- Owner decision D8 (see `SCOPE.md`): `MegaGameEntity.init()` adds the `TagsComponent`; 47 entity `init()` overrides
  that did not call `super.init()` now call it (the first count of 52 included helper classes that are not entities).
- Step 7: `TagsComponent` (`entities/state/`), `defineTagsComponent()`, `tags`, `hasTag`, `hasAnyTag`; the
  `MegaGameEntities` index adds and removes under each tag. `TagsComponentTest`: 8 tests.
- Step 8: 144 duplicate `getTag()` overrides deleted; the 4 bases give own name + base name (`AbstractBlock` also
  `Block`); the 7 different tags override `defineTagsComponent()`. The 41 readers use `hasTag`, `hasAnyTag`, the own
  `TAG` or the tag list; `PlayerDamageNegotiator` takes the first tag that has a table entry. `ITaggable` is deleted
  from `engine`.
- Found by the tests: `MegaLevelScreen` builds a `ViewEdgeHazard` damager that is never spawned, so it had no tags
  and the co-op view-edge damage would throw. It now calls `init()`. A search of all entities built directly in game
  code found no other case (`Megaman` and `MegamanX` are initialized in `postCreate()`; the others are spawned).
- Tests that skip `init()` on purpose add the `TagsComponent` by hand (as they add a `BodyComponent`); test damagers
  call `init()`; relaxed mocks stub `getComponent(TagsComponent::class)`.
- `engine:test` 673, `core:test` 1301, `core:smokeTest` 451 pass; `lwjgl3:jar` builds.
- Step 9: `EntityInitSmokeTest` checks the tags of every concrete entity class after `init()` against
  `core/src/smokeTest/resources/entity-tags.txt` (446 classes). The list is built apart from the Kotlin code: the
  investigation parser, run on `master` with the class ancestors, plus the D5 rule. Checked: every class keeps its old
  tag, and each added tag is its own name or a base name (44 classes have more than their old tag). The parser missed
  `FirePellet` (a class header split over 2 lines); added by hand, no override, so its tag is its name. Mutation: no
  base tag in `Block` fails 35 smoke tests.
- Step 10 (docs): `CLAUDE.md` explains the tags component and `super.init()`; `IMegaGameEntity` holds only the type.
- Second review before the play test (2 agents, read-only, no game launch):
  - Tag diff review: no bug and no behavior change in play. All 47 `super.init()` insertions are in direct children
    of `MegaGameEntity` (or `Death`/`DynamicBlock`, whose children already called it), so no parent `init()` runs
    twice or for the first time. Fixed: 7 blank lines after the imports in 3 files (from the import script); the
    commented `canSpawn` code in `LevelDefinition` uses `hasTag`; `Force` stops at the first matching tag. Left: the
    commented-out `getTag()` in the retired `ReactorMan_OLD` and `ColtonJoe`. Overlap with `081f`: 3 new entities
    use the old tag API (`NOTES.md`); it needs the owner at the merge.
  - Play-check review: 6 new checks (co-op view-edge damage in `group_tall`, `GAUNTLET_1` map `block_filters`, Moon
    Scythes through a `GroundSlideBlocker`, `GLACIER_MAN` icicles, `INTRO_STAGE` copter teeth, `TIMBER_WOMAN`
    deflection). None of the 4 earlier co-op rooms reaches the `ViewEdgeHazard` tag read (each room is one view).
- Diffity round 2, owner question (D9): the base overrides call `super.defineTagsComponent()` and `add` their `TAG`.
  `TagsComponent.add` added (test). `AbstractBlock` no longer repeats `Block.TAG`; 2 lines of `entity-tags.txt`
  change order (`CameraEdgeBlock`, `GroundSlideBlocker`). The 7 different tags still replace. Runtime `remove` and
  index sync: backlog candidate. `core:test` 1302, `core:smokeTest` 451 pass; `lwjgl3:jar` builds. Uncommitted until
  the round closes.
- Play test, owner: `GAUNTLET_1 --room room1` crashed at level start with `MissingComponentException`. Cause: the
  engine calls `canSpawn` in `spawn()`, before `init()`; the `GAUNTLET_1` level filter read tags there. The reviews
  missed it (they checked `init()` and `onSpawn()`, not `canSpawn`). Fix: the filter checks the class
  (`it !is Life && it !is WeaponEnergyBulb`; both classes are final, so the result is the same as the old tag test).
  No other `canSpawn` or spawn-reject code reads tags. New `LevelOverrideCanSpawnTest` (2 tests) runs the filter on
  entities that did not run `init()`; with the old tag read, both fail. `CLAUDE.md`: a `canSpawn` check must not read
  tags. `core:test` 1304, `core:smokeTest` 451 pass; `lwjgl3:jar` builds. Uncommitted until the round closes.
- Spawn-rules probe (1 read-only agent): no spawn path bypasses `engine.spawn`; the safety against reads before `init()` comes from a rule signature with no instance (a central table, option C, adds O7 data value but no extra safety). 2 bug candidates added (`TellySaucer` side choice, preload double `init()`). The owner wants to discuss the spawn-rules item before it is created.
- Diffity round 2 closed with no threads (the owner skimmed the code and found no other issues). The fixes of the
  round are committed: D9 (base tags by `super` + `add`), the `GAUNTLET_1` filter by class, 2 bug candidates.
- Scope change (owner, 2026-11-10): layers 1 and 2 of the spawn-flow design join `146a`; layer 3 goes to item `021`.
  Research: `temp/mega-refactor-146a-player-marker/spawn-design/` (4 read-only agents).
- Owner decision D10: `TellySaucer` side from the live player nearest the spawn (`MegaPlayers.nearestLiveTo`). `SCOPE.md`, `CONTRACT.md`, the brief and `PLAN.md` (steps 11-13) updated; the spawn-flow research summary is in `NOTES.md`.
- Step 11: `GameEngine.spawn()` runs `init()` before `canSpawn`; `spawnNow` no longer inits; the preload sets `initialized`. 2 engine tests (the first failed before the change). `engine:test` 675, `core:test` 1304, `core:smokeTest` 451 pass.
- Step 12: `MegaPlayers.nearestLiveTo(point)` (`targetFor` uses it; same behavior); `TellySaucer.canSpawn` picks its
  side from the player nearest the spawn (D10); `HeartTank` and `HealthTank` parse their value in a pure function,
  and only `onSpawn` sets the field. `CanSpawnContractTest` (3 tests) and a `nearestLiveTo` test; the saucer test
  fails with the old `targetPlayer` read. `CLAUDE.md`: the spawn order and the `canSpawn` contract (the earlier
  "must not read tags" line is replaced). Blank lines that the import script added to new files are collapsed.
  `core:test` 1308, `core:smokeTest` 451 pass; `lwjgl3:jar` builds.
- Third review before the play test (2 agents, read-only, no game launch):
  - Spawn-flow diff review: no bug. `init()` at the spawn request is safe in all 468 `init()` bodies (every spawn or
    fetch is in a lambda or in `onSpawn`; no listener, registry, static set, level or camera read). No leak for
    rejected entities. `nearestLiveTo` gives the same results as the old `targetFor`. Fixed: the engine test now
    checks the init count after the rejected spawn (it passed on the old code); the saucer test has a case with a
    body far from the spawn; a real-flow test checks the heart tank value from `onSpawn`; `ViewEdgeHazard` sets
    `initialized`; a stray blank line in `ItemEntity`. Bug candidate: `HeartTank.spawnOnContact` is never cleared
    (same on `master`).
  - Play checks: 8 new checks (`MOON_MAN room13` with 1 and 2 players, `REFACTOR_TEST_029` tank rooms with and
    without the tank, `WILY_STAGE_2 room9` GutsTank, `REACTOR_MAN room4`). The 2 players cannot stand on opposite
    sides of a saucer spawn (both stay in view), so that case is covered only by `CanSpawnContractTest`.
  - `engine:test` 675, `core:test` 1309, `core:smokeTest` 451 pass; `lwjgl3:jar` builds.
- Owner rule (on `master`, `2ff150091`): a boss play check adds `--heartTanks all --healthTanks all`. The 3 boss checks (`WILY_STAGE_2`, `PRECIOUS_WOMAN`, `TIMBER_WOMAN`) now have the options. Uncommitted until the Diffity round closes.
- Play test (owner, 2026-11-10): all checks pass. Diffity round 3: no threads.
