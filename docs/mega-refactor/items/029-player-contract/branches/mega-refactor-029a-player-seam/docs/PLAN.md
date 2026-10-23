# Plan: mega-refactor-029a-player-seam

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-23

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results` (a Kotest `--tests` filter does not match).

Each step is its own commit. After each code step, the build and the tests pass, and a short launch
of `REFACTOR_TEST_029` (once the map exists) shows no exception.

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029a` row in `ITEMS.md` to `active` with the
  start date; set item `029` to `active` with the start date; mark `029a` active under "Branches" in
  `ITEM.md`; set the branch `process-log.md` state to `active`. Then `git worktree add -b
  mega-refactor-029a-player-seam .worktrees/mega-refactor-029a-player-seam master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 454 engine, 95 core) and the warning count
  (expected 114) in the process log.
- Done: no

### 3. Test level entry and test entity

- Changes:
  - `LevelDefinition.REFACTOR_TEST_029` (`TEST_LEVEL`, `tmxMapSource = "RefactorTest029.tmx"`, the
    same music and completion screen as `TEST_1`), with a `levelOverride` loadout that gives at
    least one special weapon (for example `FRIGID_SHOT`).
  - `entities/special/TestStunPulser`: spawned from Tiled with an `interval` property. Each time the
    interval ends, it submits `STUN_PLAYER` with `StunType.STUN_BOUNCE_IF_ON_SURFACE` and a
    `SHAKE_CAM` with Timber Woman's shake values. It stops when destroyed (culled with the room).
  - The dev start (`--level`, `MegamanMaverickGame.kt:425`) applies the level's override when the
    level has one, as `startExtraLevel` does (owner decision (a), 2026-10-23; a small dev-tool QoL
    change, logged in the process log).
- Check: build and tests pass. `TestStunPulser` is in no TMX file yet.
- Done: no

### 4. Test map (subagent)

- Changes: a subagent builds `assets/tiled_maps/tmx/RefactorTest029.tmx` with the `level-builder`
  skill. Rooms, each with a player spawn so that `--room` works:
  - `trail`: a long floor with a wall at one end.
  - `stun`: a floor and a wall high enough to wall slide on, with a `TestStunPulser` (interval about
    3 seconds).
  - `heart`: a `HeartTank`.
  - `pickups`: a health bulb, a weapon energy bulb, a screw and a health tank, with an enemy or a
    hazard nearby to lose some health and ammo first, if the builder can place one.
  - `death`: a pit or spikes.
  The subagent reads the reference levels that the `level-builder` and `level-designer` skills
  name, and reports what it could not place.
- Check: a launch of each room (`./run-game-alpha.sh --level REFACTOR_TEST_029 --room <room>`) shows
  no exception and the player in the room. Then the owner checks each room on this baseline code
  and the results go in the process log. The owner may fix the map by hand.
- Done: no

### 5. Engine player types

- Changes: new package `engine/.../players/`: `PlayerSlot` (`@JvmInline value class`),
  `PlayerView` (interface: `slot`, `entity`, `present` with the default `entity.spawned`).
  `IGameEntity.require(type)` with the message "Player entity <entity> has no <component>".
  `ComponentKey<T>` next to `PropertyKey<T>` (`common/objects/`), with a `get` operator on
  `IGameEntity` and on `PlayerView`.
- Check: engine unit tests: `require` returns the component and fails with the message;
  `ComponentKey` lookup gives the component or null. Build and tests pass.
- Done: no

### 6. Engine players service

- Changes: `IPlayers<out V : PlayerView>` (`capacity`, `get(slot)`); `open class Players<V>`
  (`capacity`, the view factory, `register`, `unregister`, `get`; `register` fails for a slot that is
  out of range or not free). `inline` extension helpers on `IPlayers`: `forEach`, `forEachPresent`,
  `any`, `all`, `firstOrNull`, `minByOrNull` (slot order; a tie goes to the lower slot), and
  `slotOf(entity)` (the slot of a registered entity, or null). No `primary`, no `first()`.
- Check: engine unit tests with capacity 3: register, unregister, a full or out-of-range slot, `get`
  of an empty slot; slot order of `forEach` with a gap; the `present` filter; `minByOrNull` tie; `any`,
  `all`, `firstOrNull`; `slotOf`; a custom extension helper declared in the test. Build and tests
  pass.
- Done: no

### 7. Core view, service and entry point

- Changes:
  - `MegaPlayer(slot, entity) : PlayerView` with `private val bodyComponent =
    entity.require(BodyComponent::class)` and `val body get() = bodyComponent.body`.
  - `MegaPlayers : Players<MegaPlayer>(capacity = 1, ::MegaPlayer)` and the core extension
    `nearestTo(point)` on `minByOrNull` (distance from the body center).
  - `MegamanMaverickGame.players`: made in `postCreate`; `players.register(PlayerSlot(0), megaman)`
    after `megaman.init()`.
  - `val IMegaGameEntity.players get() = game.players` next to the `megaman` extension.
- Check: core tests: `MegaPlayer` fails without a `BodyComponent`, and gives the body through the
  component (a new `Body` set on the component is seen). Build and tests pass; a launch shows no
  exception.
- Done: no

### 8. Self-references

- Changes: in the player package, replace the global `megaman` with `this` at
  `Megaman.kt:629, 929, 933, 990`, `BodyComponent.kt:192-195`, `SpritesComponent.kt:101`,
  `BehaviorsComponent.kt:279` and `MegamanAnimationKeySupplier.kt:42-43, 48`. Remove the then
  unused imports. `MegamanTrailSpriteV2` gets its `Megaman` from a `ConstKeys.OWNER` spawn prop on
  each spawn (set in `Megaman.tryToSpawnTrailSprite`), and clears it on destroy.
- Check: the contract grep for global `megaman` reads in the player package finds none. A review
  shows each change as `megaman` to `this` (or to the owner in the trail sprite). Build and tests
  pass. Launch `REFACTOR_TEST_029`, room `trail`: trail sprites show.
- Done: no

### 9. Player identity on the events

- Changes:
  - `ConstKeys.SLOT` and `ConstKeys.SLOTS`.
  - `SLOT` (from `players.slotOf(<the player>)`) on `PLAYER_SPAWN`, `PLAYER_JUST_DIED`
    (`Megaman`), `PLAYER_SPAWN_INIT`, `PLAYER_READY` (spawn handler), `PLAYER_DONE_DYIN` (death
    handler), `ADD_PLAYER_HEALTH` (`HealthBulb`, `PlayerStatsHandler`), `ADD_WEAPON_ENERGY`,
    `ADD_CURRENCY`, `ATTAIN_HEALTH_TANK`, `ATTAIN_HEART_TANK` (items, from the contact player).
  - `STUN_PLAYER`: `TimberWoman` and `TestStunPulser` send `SLOTS` with every registered slot
    (`players.forEach`). `Megaman` returns at the start of its `STUN_PLAYER` handling if its slot is
    not in `SLOTS`; the rest is unchanged.
- Check: the contract grep and review of the submit sites. A core test, if practical with the relaxed
  game mock: `Megaman` ignores a `STUN_PLAYER` whose `SLOTS` does not hold its slot. Build and tests
  pass. Launch `REFACTOR_TEST_029`, room `stun`: Megaman bounces on the floor.
- Done: no

### 10. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test of the test
  rooms, `SMB3_1` and `TIMBER_WOMAN`. Write the results in the process log.
- Done: no

### 11. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no

## Decisions

- 2026-10-23: the dev start applies a level's override when the level has one (option (a)). Without
  it, the test level's loadout would not apply on `--level`.
