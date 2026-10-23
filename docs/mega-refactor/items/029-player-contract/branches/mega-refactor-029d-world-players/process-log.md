# Process log: mega-refactor-029d-world-players

## Status

- State: merged
- Goal: World entities, contact rules and world body code reach the player through the players service; `HIT_BY_PLAYER` takes the player view
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `9432e437b`. Draft `docs/SCOPE.md` written.
- The owner answered open questions 1-4 (gravity-device receiver; `teleporting` in `PlayerStateComponent`;
  light-radius receiver; `players.any { it.ready }`), asked that `PushableBlock` and `FlipperPlatform` work for any
  player in contact, and asked to rename `MegamanDeath` to `PlayerDeath` (class and the 29 TMX objects) with a test
  room. Scope updated.
- The owner asked for one source of truth for the gravity direction. Scan of every write to the player's direction
  (on `9432e437b`): all go through `Megaman.direction`'s setter, which sets the body direction, the physics values
  and, when the direction changes, starts the camera rotation (`START_GAME_CAM_ROTATION`) and stops the player
  until it ends. The writers:
  1. `GravitySwitcharoo:191` (live in `MOON_MAN` and `WILY_STAGE_3`) and `GravityCapsule:181` (in no TMX file);
  2. `MoonMan.activateGravityChange` (`MoonMan:524`; skips a dead player);
  3. direct writes, which do happen:
     - `Megaman.onSpawn` sets the direction from the player spawn's `direction` property (5 spawns in
       `MoonMan_16x14_v5.tmx`);
     - `PortalHopper` sets `entity.direction` from `next_entity_direction` (3 hoppers in `MoonMan_16x14_v5.tmx`),
       and could set `entity.body.direction` from `next_body_direction` (no live map uses it; it would skip the
       setter);
     - `CapsuleTeleporter:208` sets the received player's direction from `megaman_direction_on_receive` (1 in
       `WilyStage3_v4.tmx`);
     - `MegaLevelScreen:367` sets it at the end of a room transition from the room property `megaman_direction`
       (2 rooms in `WilyStage3_v4.tmx`).
  So the direct writes exist, but every one of them still goes through the setter and rotates the camera: today
  the player's direction and the camera's direction are the same "world" gravity, also for the direct writes.
- The owner decided the gravity questions: `WorldGravity` on the game; all writers call `gravity.set`; world-direction readers read it; camera rotation stays in the player's setter with a `TODO`; Moon Man sets gravity only when the target player is alive (`TODO` for `047`); all in `029d`. Scope updated.
- Owner decision: comment out `GravityCapsule` and `PortalHopper`'s `next_body_direction` (both unused) with `TODO`s. Scope updated.
- Scope approved by the owner. Draft `docs/CONTRACT.md` written; real-level rooms found from the live TMX files (the room with the object, or the nearest earlier room with a player spawn).
- Contract approved by the owner. Draft `docs/PLAN.md` written (no baseline play step).
- Plan approved by the owner.
- Step 1: branch start on `master`, worktree created.
- Step 2: baseline: clean build passes; 470 engine and 110 core tests, 0 failures; 114 compiler warnings.
- Step 3: a subagent added `player_death` (pit with an instant `MegamanDeath`, a `GravityBlock` under the ceiling),
  `push_flip` (`PushableBlock`, `FlipperPlatform`), `hit_by_player` (`SmallGrass`, `Togglee` lever) and
  `gravity_switch` (`GravitySwitcharoo` `direction=down`, `ConveyorBelt`); map widened to 316 columns, tiles painted,
  new layer `decorations`. I added a second `GravitySwitcharoo` (`direction=up`) near the ceiling so gravity can be
  flipped back. `SmallIceCube` was not placed: its `onSpawn` needs a `Vector2` position prop that a map cannot give
  (only code spawns it); the contract row now covers `SmallGrass` and `Togglee`, and the `SmallIceCube` change is
  checked by review. The `GravityBlock` falls about 0.8 s after the room spawns, so a head hit needs the player to be
  under it then (owner can adjust). Short launches of the 4 rooms: no exception.
- Step 4: `PlayerStateComponent.teleporting` (`Megaman.teleporting` reads it; its setter stays private). New core
  `PlayerKeys` (typed entity keys `GRAVITY_DEVICE_SWITCH: () -> Unit`, `LIGHT: () -> PlayerLight`) and
  `PlayerLight` (`STANDARD`, `HALF_CHARGE`, `FULL_CHARGE`, `JETPACK`). Megaman registers both in `init()`: the device
  receiver sets `aButtonTask` (jump on the ground, else air dash, as `GravitySwitcharoo` does today); the light
  receiver gives the light state from `charging`, `fullyCharged` and jetpacking. Refinements of the scope wording:
  the device sets the direction through `WorldGravity` once and the receiver only adjusts `aButtonTask` (same
  order and result); the light receiver gives a state, not a radius, because each Darkness version has its own radius
  constants and `DarknessV3` lights only while charging. `MegaPlayer` requires `IHealthEntity` and both receivers,
  and gets `depleteHealth()`, `headFixture`, `feetGravityFixture`, `isAnyBehaviorActive`, `teleporting`, `light`,
  `onGravityDeviceSwitch()`. `WorldGravity(players)` (`direction`, `set`), `game.gravity` made after `players`.
  Tests: `MegaPlayersTest` +2 (470, 112). The Megaman receivers are not unit-tested (they need `init()`); the
  `gravity_switch` room and the dark rooms cover them.
- Step 5: writers call `game.gravity.set`: `Megaman.onSpawn`; `MoonMan` only while `targetPlayer` is not dead
  (`047` `TODO`); `GravitySwitcharoo` (sets gravity once, then calls each player's device receiver; its trigger
  set and checks use the players service and `gravity.direction`); `PortalHopper` for a player entity (others keep
  `entity.direction`); `CapsuleTeleporter` for a received player; the level screen's room property. A camera
  rotation `TODO` is in `Megaman.direction`'s setter. The world-direction readers (15 reads in `MagmaWave`,
  `MagmaGoop`, `PortalHopper`, `PreciousShard`, `PreciousGem`, `MagmaPellet`, `GravityBlock`, `ConveyorBelt`,
  `Bullet`, `Spike`, `DustPuff`, `MagmaFlame`, `Disintegration`) read `game.gravity.direction`. The direction-write
  grep found a 7th writer the scan had missed: `BodyGravityChangeRule` (a `GravityChange` area; live only in
  `Test1.tmx`/`TEST_1`). It is now a class that takes a `changeDirection` function; `megaContactRules` passes one that
  calls `gravity.set` for a player and sets `entity.direction` otherwise. Scope wording updated (same rule, one more
  writer). After this step the grep finds no player direction write outside `WorldGravity.set` and the setter
  (`GravityCapsule` is commented out in step 10). Build and tests pass (470, 112). Short launches of
  `gravity_switch` and `MOON_MAN` `room6`: no exception.
- Step 6: `Receivers.HIT_BY_PLAYER` takes `MegaPlayer`; `BodyPlayerRule` passes
  `playerEntity.game.players.viewOf(playerEntity)!!` (the `as Megaman` cast and its `029` `TODO` are gone);
  `SmallGrass`, `Togglee`, `SmallIceCube` compile against the view unchanged (`SmallIceCube` reads
  `it.canBeDamaged`). `IBodyExtensions.defaultDoUpdate` uses `game.players.viewOf(entity) == null`. `FeetSandRule` gets
  `{ game.players.any { it.ready } }` with the `047` `TODO`. No unit test for `BodyPlayerRule`: it needs a full
  registered test player in `core/src/test/.../world/` (the helper entity is private to `MegaPlayersTest`); the
  `hit_by_player` room covers it. Build and tests pass (470, 112). Short launch of `hit_by_player`: no exception.
- Scope addition (owner decision, during step 7): `PlayerKeys.VIEW` and `Any?.asPlayer`. `MegaPlayer` stores
  itself on its entity in `init` (with the `047` `TODO` for unregistering); a script replaced all 37
  `players.viewOf(x)` sites in 29 core files with `x.asPlayer` (diff check: 36 lines differ only in that swap; one
  had redundant parentheses, fixed by hand). `MegaPlayersTest` +1 (470, 113). Short launches of `enemy_contact` and
  `hit_by_player`: no exception. Step 7 continues with `asPlayer`.
- Step 7 (by hand, with `asPlayer`): ownership and identity (about 40 sites: water, projectiles, Wily capsules, pink
  and breakable blocks, wood crates, crushers, toxic waterfall, lasers, smoke puffs, `isAny(Megaman::class, X)` as
  `asPlayer != null || is X`); kill zones with `players.forEach` and today's checks (`MegamanDeath.update`,
  `GravityBlock`); any player in contact (`PushableBlock` x4, `FlipperPlatform` x2 plus its block filter on the
  contact entity, `FloorButton` adds every player's shapes, `CartV2`, `DropperLift`, `InfernoChainPlatform`);
  `PipePortal` and `QuestionBlock` act on the player in the contact; `GroundSnow` reads the contact player's
  behavior; `Rain`, `LaserBeamer`, `SpringBouncer`, `PreciousGem` (owner's `teleporting`), `SmallIceCube` (passes
  its owner). The Darkness light is one light per player from `player.light` (V2 and V4 keep their radius rule:
  full charge, half for half charge or jetpack, standard; V3 lights only while charging; V4's beam light keys on the
  target player). `SwimBooster` (`029g` `TODO`) and `ChargedShot` (`030` `TODO`) read the target. `SpiderWeb` sticks
  to the target and uses two new view commands with a caller here: `allowMovement` (the raw `canMove` flag, now
  `PlayerStateComponent.movementAllowed`; `Megaman.canMove` keeps its stunned/damaged/frozen checks) and
  `setAllBehaviorsAllowed`. Build and tests pass (470, 113). Short launches of `player_death`, `push_flip` and
  `WILY_STAGE_1` `room8`: no exception.
- Owner preference (2026-10-23): `targetFor` and `targetPlayer` should not be global (service-wide and on every entity); targeting belongs to the entities that need it, each with its own behavior. The `TODO`s at `MegaPlayers.targetFor` and `IMegaGameEntity.targetPlayer` now say so; the item docs on `master` record it. No code change in this branch.
- Step 8: a script replaced the remaining `megaman.` reads in the world packages (24 files) with `targetPlayer.`;
  diff check: 34 lines differ only in the reference. Two sites were done by hand: `Laser`'s axe-shield check uses
  the player who holds the shield (`shieldEntity.asPlayer`), and the old `Darkness` (V1, still in `Test1.tmx`) gives
  one light per player while charging, like `V3`. What is left in the world packages: the tag string
  `"megaman,..."` in `DecoratedGravityForce` and `PlayerItemRule` (`029e`). Build and tests pass (470, 113).
- Step 9: `MegamanDeath` renamed `PlayerDeath` (file, class, `TAG`), the comment in `Death.kt`, and the TMX objects: `InfernoMan_16x14_v3.tmx` 13, `MoonMan_16x14_v5.tmx` 14, `TimberWoman_16x14_v4.tmx` 1, `TimberWoman_16x14_v5_draft_1.tmx` 1 (29), plus 1 in `RefactorTest029.tmx`. A grep finds no `MegamanDeath`. Build and tests pass (470, 113). Short launches of `player_death` and `MOON_MAN` `room6`: no exception, no unknown-entity log.
- Step 10: `GravityCapsule` commented out line by line (package line kept) with a `TODO`; `PortalHopper`'s `next_body_direction` (field, spawn-prop read, event property read and write, the body write) commented out, with the `TODO` at the field; `receiveEntity` lost its unused `bodyDirection` parameter. Greps: nothing else uses them. Build and tests pass (470, 113). Short launch of `MOON_MAN` `room11` (portal hoppers): no exception.
- Step 11 (automated): clean build passes; 470 engine and 113 core tests, 0 failures; 114 compiler warnings, the
  same list as the baseline; `ContactRuleSelectionTest` passes and the reference file is unchanged; the global
  player grep over the world packages, `world/` and `damage/` finds only the tag string in
  `DecoratedGravityForce` and `PlayerItemRule` (items are `029e`, logged as the one exception) besides the
  exact-class `Megaman::class` sets; the only direction writes outside `WorldGravity.set` and the player's setter
  are for non-player entities (`BodyGravityChangeRule`'s non-player branch, `PortalHopper`'s non-player branch);
  the five `TODO`s exist; no `MegamanDeath` in Kotlin or TMX, 30 `PlayerDeath` objects; `GravityCapsule` and
  `next_body_direction` are commented out; `HIT_BY_PLAYER` takes `MegaPlayer` and `BodyPlayerRule` has no cast
  or `029` `TODO`; the reviews are in the step entries. Waiting for the owner's play test.
- Step 11: the owner's play test passed. All contract checks pass.
- Step 12: the owner approved the merge (2026-10-23).
