# Process log: mega-refactor-029a-player-seam

## Status

- State: merged
- Goal: Player slot, player view and players service in the engine; the game view `MegaPlayer`; `Megaman` registered in slot 0
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `504bcd015`. Draft `docs/SCOPE.md` written.
- Draft scope revised after discussion: no `IPlayer` on the entity; engine `PlayerView` interface (choice A), `Players<V>` with inline helpers, no position in the engine; game view `MegaPlayer` resolves required components at registration; capacity 1.
- Scope: added the `IMegaGameEntity.players` extension and where `game.players` lives (owner's request).
- Owner answered the open questions: names approved, new `ConstKeys.SLOT` (and `SLOTS`), `STUN_PLAYER` carries target slots (room shake: all players). Scope updated.
- Scope approved by the owner. Draft `docs/CONTRACT.md` written.
- Scope (with the test map) and contract approved by the owner. Draft `docs/PLAN.md` written.
- Plan approved by the owner, with option (a): the dev start applies a level's override.
- Step 1: branch start on `master`, worktree created.
- Step 2: baseline: clean build passes; 454 engine and 95 core tests, 0 failures; 114 compiler warnings.
- Step 3: `LevelDefinition.REFACTOR_TEST_029` (`TEST_LEVEL`, `RefactorTest029.tmx`, loadout `FRIGID_SHOT` and
  `MOON_SCYTHES`) and `entities/special/TestStunPulser` (while the game camera overlaps its bounds, it submits
  `STUN_PLAYER` with `STUN_BOUNCE_IF_ON_SURFACE` and Timber Woman's `SHAKE_CAM` each `interval`, default 3 s).
  QoL (owner decision (a)): the dev start (`--level`) applies the level's override when it has one. Build and
  tests pass (454, 95). The game cannot boot until the map exists (every `LevelDefinition` map loads at boot), so
  the launch check moves to step 4.
- Step 4: a subagent built `RefactorTest029.tmx` with the `level-builder` skill: rooms `trail`, `stun`, `heart`,
  `pickups`, `death` (closed, not connected; one spawn each), a `TestStunPulser` (interval 3) over the `stun` room,
  `HeartTank` `A`, `HealthBulb`, `WeaponEnergyBulb`, `Screw`, `HealthTank` `A`, a `Met`, a 4-tile pit with an
  instant `Death` sensor. The CLI cannot place items, specials or properties, so those were added by hand in the
  XML. The map had no tile layer (blocks invisible); on the owner's request a `tiles` layer now paints tile 175 of
  `Tileset1` on every block cell, and the rule is now in the `level-builder` skill and in "Test maps" (`master`).
  Launches of every room (45 s each) show no exception. The owner's baseline check of the rooms is still to do.
- Step 5: engine `players/PlayerSlot`, `players/PlayerView` (`slot`, `entity`, `present`), `components/ComponentKey` with a `get` operator on `IComponentBucket` and `PlayerView`, and `IComponentBucket.require`. Deviations: `ComponentKey` is in `components/`, not next to `PropertyKey` in `common/objects/`, so that `common` does not depend on `components`; the `require` message says "Entity <entity> has no <component>", because `require` is not only for players. `ComponentKeyTest` (4 tests).
- Step 6: engine `IPlayers` (`capacity`, `get`), `Players` (register, unregister, get; fails on a full or out-of-range slot and on a capacity below 1), `inline` helpers `forEach`, `forEachPresent`, `any`, `all`, `firstOrNull`, `minByOrNull` (tie to the lower slot) and `slotOf`. `PlayersTest` (16 tests, capacity 3). Engine tests: 474.
- Step 7: core `players/MegaPlayer` (requires `BodyComponent`, reads `body` through it), `players/MegaPlayers` (capacity 1) and `nearestTo`; `MegamanMaverickGame.players` with `Megaman` registered in slot 0 after `init()`; `IMegaGameEntity.players`. `MegaPlayersTest` (4 tests). Core tests: 99. Steps 5-7 add types only; the gameplay code is still the baseline.
- The owner checked every test room on the baseline code (after step 7): all pass.
- Step 8: the global `megaman` reads in the player package became `this` (`Megaman.kt` stun impulse, heart-tank
  max health, `translateAmmo` default; `BodyComponent` head-bump velocity; `SpritesComponent.shouldHideSprite`;
  `BehaviorsComponent` air dash `isFacingBlock(this@defineBehaviorsComponent)`; `MegamanAnimationKeySupplier` pipe
  portal and the weapon check, at `:120`, not `:48` as the scope says). `MegamanTrailSpriteV2` reads its `Megaman`
  from a `ConstKeys.OWNER` spawn prop, which `Megaman.tryToSpawnTrailSprite` sets; it is a local in `onSpawn`, so
  nothing is kept to clear on destroy. Deviation: the old `MegamanTrailSprite` (V1) still reads the global. It is
  dead code (its pool is made only in the deprecated `DecorationsFactory`, and nothing fetches it), so it stays for
  `039`; backlog candidate in `docs/TODO.md`. Build and tests pass (474, 99). Launch of the `trail` room: no
  exception.
- Step 9: `ConstKeys.SLOT` and `ConstKeys.SLOTS`, used through typed keys `PlayerEventKeys.SLOT`
  (`PropertyKey<PlayerSlot>`) and `PlayerEventKeys.SLOTS` (`PropertyKey<Array<PlayerSlot>>`), so that listeners
  read them with no cast (refinement of the plan, which named only the `ConstKeys`). `SLOT` (from `slotOf`) on
  `PLAYER_SPAWN`, `PLAYER_JUST_DIED`, `PLAYER_SPAWN_INIT`, `PLAYER_READY`, `PLAYER_DONE_DYIN`, `ADD_PLAYER_HEALTH`
  (`HealthBulb`, `PlayerStatsHandler`), `ADD_WEAPON_ENERGY`, `ADD_CURRENCY`, `ATTAIN_HEALTH_TANK`, `ATTAIN_HEART_TANK`.
  `STUN_PLAYER` from `TimberWoman` and `TestStunPulser` sends `SLOTS` with every registered slot, from a new engine
  helper `registeredSlots(out)` (plan addition, tested). `Megaman` returns at the start of its `STUN_PLAYER` handling
  when `SLOTS` is missing or does not hold its slot; the rest is unchanged. `MegamanStunTargetTest` (3 tests, with
  `STUN_BOUNCE_ALWAYS` so the surface check cannot hide the filter); a mutation check (filter removed) fails the two
  "ignored" tests. Build and tests pass (475, 102). Launches of `stun`, `pickups`, `death`: no exception.
- Step 10 (automated): clean build passes; 475 engine and 102 core tests, 0 failures; 114 compiler warnings, the
  same list as the baseline; `ContactRuleSelectionTest` passes and the reference file is unchanged; in the player
  package only the dead `MegamanTrailSprite` (V1) reads the global `megaman` (deviation logged in step 8); the
  engine `players` package names no `megaman`, `BodyComponent`, `Body` or `Vector2`; no `primary` or `first()`;
  all 13 player event constructions carry `PlayerEventKeys.SLOT` (`SLOTS` for the 2 `STUN_PLAYER`); outside the
  player package only the registration, the `players` extension, the submit sites, `TestStunPulser` and the key
  definition use the players service; `TestStunPulser` is only in `RefactorTest029.tmx`; the step 8 diff is
  `megaman` to `this` (or to the owner in `MegamanTrailSpriteV2`) with nothing else changed. Waiting for the
  owner's play test.
- Step 10: the owner's play test passed (test rooms, `SMB3_1`, `TIMBER_WOMAN`, boot). All contract checks pass.
- Step 11: the owner approved the merge (2026-10-23).
