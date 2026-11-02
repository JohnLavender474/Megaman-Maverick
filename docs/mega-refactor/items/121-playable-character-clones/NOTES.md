# Notes: 121 Enemy clones of a playable character

Research for this item. Each report is kept in full, with its own date and `master` commit. A note above each report says what later research or an owner decision replaced. If a report and `ITEM.md` disagree, `ITEM.md` wins.

## Agent report: P2 as an enemy, and clones (2026-11-02)

Note: The "fight entity" here is the plain `Fight` of `118`. `roomBound` is a proposed field from `048` research; it does not exist in the code.

### Hostile playable characters: P2 as an enemy, and clones

Base: `master` at `44a4c7e61`. I read and grepped only. I did not run the game or a build. Counts are grep results, so they can include a few wrong lines.

Paths:
- `C/` = `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/`
- `M/` = `C/entities/megaman/`

#### Short answer

- **Single P2 as an enemy:** the design works. `BossComponent` goes away. A fight entity stays only if the room must lock.
- **Clones:** the component design works. The model "a `Megaman` with a slot" does not. A clone must be a slot-free pooled class in `enemies/`.
- **Spawn fault:** a third `Megaman` crashes in `onSpawn` today. If that crash is removed, it still fails in silence (see Q2).
- **Moves:** `084` split the support state, not the moves. The 8 behaviors, the body and the controller still have `Megaman` as receiver.

#### 1. Single P2 as an enemy

**The fight entity has 3 jobs.** This split is my inference from the design you gave.

| Job | Boss case | Enemy-only case |
|---|---|---|
| Hold fight data (`BossComponent`) | Yes | Goes away. No intro, no boss bar, no boss defeat flow. |
| Switch P2 to hostile and back | Yes | Needed only if hostility has a limited scope. If the level or mode sets it for the whole session, no entity is necessary. |
| End the room | Yes | Needed only if the room locks. |

**Who ends the room.** Only 2 mechanisms exist today:
- The `Gate` boss key.
- The `Gate` `gauntlet_key`, with `GAUNTLET_CLEARED` (`C/entities/sensors/Gate.kt:151,212-225`).

With neither, nothing ends the room. The player walks out.

**Does a fight entity still exist?**
- Open room: no.
- Locked room: yes, but it is a `GauntletMaster`, not a boss.

**New problems that the boss case hides:**

1. **A room transition moves P2.** `Megaman.onEvent` sets every instance to the transition point (`M/Megaman.kt:391-396`). An enemy is destroyed instead (`C/screens/levels/LevelRoomHandler.kt:72`). `LevelRoomHandler` selects by `EntityType.ENEMY`, and P2 has type `MEGAMAN` (`M/Megaman.kt:532`). So a hostile P2 follows P1 into the next room. In a boss room the closed `Gate` prevents this.
2. **P2 can open a `Gate`.** `GateRule` tests `isPlayer` (`C/world/contacts/rules/GateRule.kt:15`). A hostile P2 can start a transition for all players.
3. **The `PLAYER_JUST_DIED` event resets a gauntlet.** `GauntletMaster` resets on the death of any slot (`C/entities/special/GauntletMaster.kt:168-171`). So the death of a hostile P2 clears the room state and does not count as a kill.
4. **`GauntletMaster` cannot hold P2.** It fetches waves from the factory by `EntityType.ENEMY` (`:143,:210`). Its kill tracking through `runnablesOnDestroy` (`:227`) does work on any `MegaGameEntity`.
5. **The P2 respawn acts like an enemy respawn.** `PLAYER_JUST_DIED` for slot 1 returns early (`C/screens/levels/MegaLevelScreen.kt:584`). The next `PLAYER_SPAWN` spawns slot 1 again (`:550-553`). No other respawn exists until `047c`.
6. **Cull, drops and spawn blink do not apply.** All 3 are in `AbstractEnemy`. `Megaman` extends `AbstractHealthEntity` (`M/Megaman.kt:72`) and has no `CullablesComponent`. Its `invincible` getter ignores `invinciblePredicates` (`M/Megaman.kt:88-89`).

**These are the same as in the boss case:**
- `030` is needed. The 156 `asPlayer` lines in 66 files read P2 as being on the player's side.
- P2 shots hurt enemies (`C/entities/contracts/AbstractEnemy.kt:273`).
- Enemies target P2 (`C/players/MegaPlayers.kt:39`). This needs `112`.
- P1 shots cannot hurt P2 unless `rules.friendlyFire` is on (`M/Megaman.kt:433-437`). Only `GameMode.PVP` turns it on (`C/state/SessionRules.kt:20`).

**Conflict with the owner decision "must die from every cause":**

| Cause | Site | Slot 1 | Slot-free body |
|---|---|---|---|
| `DEATH` fixture | `C/world/contacts/rules/DeathRule.kt:28` | Yes | Yes |
| Damage to 0 | `AbstractHealthEntity` | Yes | Yes |
| `PlayerDeath` sensor (28 objects in 3 stage maps) | `C/entities/sensors/PlayerDeath.kt:47`, `players.forEach` | Yes | **No** |
| `CameraEdgeBlock` crush | `C/entities/blocks/CameraEdgeBlock.kt:42`, `players.forEach` | Yes | **No** |
| `GravityBlock` crush | `C/entities/blocks/GravityBlock.kt:214`, `players.forEach` | Yes | **No** |
| Past the camera edge | `MegaLevelScreen.kt:333`, `player` is slot 0 (`:181`) | **No** | **No** |
| Out of level | `M/Megaman.kt:487`, in the class updatable | Yes | Only if it shares that code |

The row "Past the camera edge" is a gap for P2 today. It breaks the owner decision.

#### 2. Clones: can more than 2 `Megaman` exist?

**Yes as objects. No as working entities.**
- The class has no instance limit, and its constructor fits the factory.
- `MAX_PLAYERS` (`C/LaunchOptionsResolver.kt:46`) limits only slots, `PlayerInputs` and `frameBuilders`.
- The `init()` slot-free claim is true by code read. `M/Megaman.kt:244-312` and the `define*` functions read no slot during `init()`. The slot reads are in closures that run later.

**Order of failure for a slot-free `Megaman`:**
1. `onSpawn` calls `services.gravity.set` (`M/Megaman.kt:329`). This writes world gravity and every player's direction.
2. `onSpawn` calls `stopSmoothFollow` (`:360`).
3. `playerSlot!!.index` throws (`:364`).

**If the crash is removed, these silent failures remain:**
- **It never moves.** `PlayPhaseComponent` starts at `PRE_PLAY` (`C/entities/state/PlayPhaseComponent.kt:5`). Only `game.players.forEach` sets `IN_PLAY` (`C/screens/levels/events/PlayerSpawnEventHandler.kt:213`). So the body zeroes velocity and gravity (`M/components/BodyComponent.kt:340-344`), the sprite hides (`SpritesComponent.kt:106`), and it is invincible (`M/Megaman.kt:89`).
- **It has no physics values.** `jumpVel` and the gravities are 0 (`M/Megaman.kt:228-236`). Only the `direction` setter writes them. `WorldGravity` reaches only `players` (`C/players/WorldGravity.kt:14`).

**Each assumption:**

| Thing | Site | Result with no slot |
|---|---|---|
| `PlayerComponent` | `Players.kt:17`, the only adder | No `isPlayer`, no `asPlayer` |
| Slot reads | `M/Megaman.kt:364,365,374`, `ControllerComponent.kt:115`, `AirDashBehavior.kt:38,43` | 6 `!!` crashes |
| `asPlayer!!` | `M/Megaman.kt:456` | Crash on the first damage |
| `PLAYER` fixture | `C/world/contacts/rules/ReceiverRules.kt:109` | Crash at contact with `SmallGrass`, `Togglee` or `SmallIceCube` |
| `players` iteration | 37 `forEach`, 72 `any`, 11 `all`, 6 `anyLive` lines | The clone is not seen. This is correct for targeting and wrong for the kill loops above. |
| Input | `C/players/MegaPlayers.kt:31` | `input` is null, so all frames read `RELEASED`. Not a crash. |
| Camera | `M/Megaman.kt:143-162,264,360`, `ClimbBehavior.kt:146,219` | A clone can start a world camera turn or a smooth follow. |
| Direction change | `C/world/contacts/rules/BodyGravityChangeRule.kt:24` | Calls the setter of the non-player, so the camera turn starts. |
| HUD and pause | `PlayerStatsHandler.kt:57`, `LevelPauseScreen.kt:110` | Slot 0 only. No effect on a clone. |
| Weapons handler | `M/handlers/MegamanWeaponsHandler.kt:140` is typed on `Megaman`; `hasWeapon` reads the session (`:513`) | A clone can fire only what the player owns. |
| Enhancements and hard mode | 12 `gameState` lines in `M/` | The player's upgrades apply to the clone. |
| Stats handler | `PlayerStatsHandler.kt:185,232,237` | Takes a slot `Megaman`. No effect on a clone. |
| Health tanks, heart tanks | `C/MegamanMaverickGame.kt:453` listener | Added only in `postCreate`, so a clone is outside this. |
| Saved state | Session `GameState` only | Entities write no save data. No effect. |
| Events | 6 keys (`M/Megaman.kt:107-114`) | Room events move every instance. `onDestroy` submits `PLAYER_JUST_DIED`. |
| Stun | `C/entities/state/Stuns.kt:21` | Null-safe. Never a target. |
| Audio | 18 sound lines in `M/`, 0 camera gates. `services.audio.stopSound` is by key (`M/Megaman.kt:376`). | One clone death stops the player's charge sound. N clones stack sounds. `AbstractEnemy` gates its sounds with `overlapsGameCamera`. |

**Measured `Megaman`-typed sites:**
- 55 `Megaman.` receivers in `M/`.
- 31 files outside `M/` import `Megaman`.
- 15 `Megaman::class` lines, of which 14 are outside `M/`.
- 10 live casts (9 `as`, 1 `as?`).
- 3 `Megaman` parameters in `PlayerStatsHandler`.
- 3 `Megaman.TAG` uses.
- 1 constructor call.

**Measured shared-state lines in `M/`:**

| State | Lines |
|---|---|
| Camera | 16 |
| `gameState` | 12 |
| `levelState` | 11 |
| `playerSlot` (including imports not removed by my filter is false: imports are excluded) | 7 |
| Players | 4 |
| Events | 4 |
| `services.audio` | 4 |
| Gravity | 1 |
| Updatables | 1 |
| Rules | 1 |

**Is `Megaman` pool-safe? No.** It is safe as one instance that respawns. These states survive a destroy, and `onSpawn` does not clear them:
- `PlayPhaseComponent.reset()` does nothing (`:7`).
- `MegamanWeaponsHandler.reset()` has 0 callers (`:156`), so cooldowns survive. The `spawned` sets clear only through `cullAllDead`.
- `MegamanJetpackHandler.reset()` runs only when 2 behaviors end.
- Property bag values `slash_index`, `shoot_down` and `shoot_down_id` survive.
- `airDashDirection` and `currentAnimKey` survive.
- `delayRun` work survives (`MegamanWeaponsHandler.kt:662,707`). It can run on a reused instance (inference).
- The trail and bubble timers survive by owner decision (`084`).

`LocksComponent.reset()` does clear all lock groups (`LocksComponent.kt:39`).

#### 3. Can the moves be split from being a player?

**Yes, but `084` did not do this part.**

**What `084` gives today:** `C/entities/state/` has 29 files and 626 lines. They contain 0 references to `Megaman`, `MegamanValues` or `MegamanWeapon`. They cover stun, knockback, freeze, recovery, momentum, room carry, bubbles, trail, run, charge timer, focus, play phase and teleport. A clone can use all of these now.

**What still ties to the class** (grep lines; the class-member list is my own, so treat those counts as approximate):

| File group | Lines | Class-only lines | `MegamanValues` | `MegamanWeapon` | `gameState` | Camera |
|---|---|---|---|---|---|---|
| 8 behaviors | 926 | 63 | 29 | 16 | 4 | 7 |
| `BodyComponent.kt` | 436 | 26 | 6 | 2 | 0 | 0 |
| `ControllerComponent.kt` | 232 | 22 | 5 | 5 | 1 | 2 |
| `MegamanAnimationKeySupplier.kt` | 139 | 10 | 0 | 5 | 0 | 1 |
| `MegamanWeaponsHandler.kt` | 1289 | Typed on `Megaman` | | | | |

`081` already owns the parameters for the behaviors and the shared functions. `024` owns the weapon data.

**A clone is a new class, not an instance of `Megaman`.** Reasons:
- Tiled resolves `entities.enemies.<name>`. No `EntityType` branch reaches `entities.megaman` (`C/screens/levels/tiled/layers/SpawnersLayerBuilder.kt:47-56`).
- A subclass of `Megaman` is a player base class. `O8-R1` and `084` rule that out. `O1-R2` also applies.
- The pool key is the class (`C/entities/MegaEntityFactory.kt:25,55`). `init()` gets no spawn props (`engine/.../GameEngine.kt:134`). So each character needs its own clone class. One class with a `def` spawn prop cannot build different parts in `init()` (inference).

**Per-entity AI input with no slot.** No engine change is necessary.
- `ControllerComponent.input` is per entity. `ControllerSystem` reads each one (`engine/.../controller/ControllerSystem.kt:17`).
- `InputFrameBuilder` is a plain per-source object with a cached `frame` (`engine/.../input/InputFrameBuilder.kt:9-14`).
- So a clone holds its own builder. A brain calls `build(heldBits)` once per tick. `read()` returns `builder.frame`. This meets the cache need for the 42 read sites in `M/`, and it does not box.
- Timing: `Updatables` runs after `Controller` and `Behaviors`. A brain in an updatable has 1 tick of delay. This adds no new system (inference).
- `TURN_CONTROLLER_OFF` locks only the game's builders (`C/MegamanMaverickGame.kt:691`). A clone does not stop for it.
- `011` is needed for seeded AI randomness.

**A boss version needs no invisible entity (inference).** A CPU clone is pooled and slot-free, so `BossComponent` can be on the clone itself. The fight entity is needed only when the fighter is a slot player.

#### 4. Options

##### (a) `Megaman` class with no slot

Needs:
- Change the 7 slot and `asPlayer!!` lines.
- Split the `direction` setter.
- Remove the camera calls from 6 sites.
- Add an `IN_PLAY` path.
- Turn off the `PLAYER` fixture.
- Change `canBeDamagedBy`, because player shots are friendly fire (`M/Megaman.kt:435-436`).
- Change `getType()`, because type `MEGAMAN` is not destroyed at a room transition and skips the enemy contact filter (`C/world/contacts/MegaContactFilter.kt:106`).
- Add a contact `DAMAGER` fixture. The only one is the needle spin fixture.
- Add a new Tiled path.

Breaks:
- `isPlayer` branches go into one class.
- It has no drops, cull or spawn blink.
- All `gameState` reads still apply.
- `onDestroy` and all events still need gates.
- All 431 animations and all weapons stay.
- It is not pool-safe.

**Not recommended.**

##### (b) Shared parts, `MegamanClone` in `enemies/`

Needs:
- `081` work: move the behaviors, body and controller off the `Megaman` receiver.
- A small typed state component for `aButtonTask`, the signed values and the air-dash state.
- Hooks for camera and `gameState` reads.
- A move list per class. `setBehaviorAllowed` throws for a missing key (`029` `NOTES.md:1145-1146`), so test all outside callers.
- A way to fire the buster without `MegamanWeaponsHandler`, or `024` first.
- An animation set limited by weapon.
- An AI source.
- Extend `AbstractEnemy` until `048`. This gives drops, cull, blink, `ENEMY` type and the enemy contact filter at once.

Breaks:
- `ChargedShot.kt:118` uses the target player's direction when the owner is not a player. It has a `TODO` for `030`.
- `MegamanTrailSpriteV2.kt:49` needs a `Megaman` owner. Drop the trail or fix that line.
- `Bullet.kt:141` depends on `AbstractEnemy` until `030`.

**Recommended.** It meets `O8-R1`, and X and Proto Man use the same seam.

##### (c) Simple enemy that imitates

Needs:
- One `AbstractEnemy` class with a state machine.
- It can read `MegamanValues` for the same numbers.
- It can use `Megaman_v3.txt` regions.
- Pattern: `C/entities/enemies/SniperJoe.kt` (546 lines).

Breaks:
- The owner's requirement. The code is a second copy, so the moves drift when Mega changes.
- `031` `NOTES.md` confirms the difference: state machines set exact velocities, and an input-driven AI gets the character's physics.

Use it only for a one-move decoy.

No copy-type enemy exists today. A search for copy, clone and mimic names found none.

#### 5. Cost per instance

These are counts, not timings.

| Measure | `Megaman` today | Typical enemy | `MegamanClone` under (b) (proposal) |
|---|---|---|---|
| Components | 28 | 9 (`AbstractEnemy` 7, plus animations and shapes) | Depends on the chosen parts |
| Fixtures | 13, with 10 active normally | Median 4, p90 7, max 13 (127 files) | 8 if `PLAYER`, teleporter, needle ×2 and axe go |
| Behaviors per tick | 8 | 0 (no other entity has `BehaviorsComponent`) | Size of the move list |
| Controller actuators | 4 | 0 | 2 to 4 |
| Sprites | 4 | Median 1, max 3 | 1 or 2 |
| Animators | 3 | 1 | 1 |
| `Animation` objects | 431 | `Met` has 7 | 68 for the buster atlas |
| Weapon sets walked per tick | 30 (10 handlers × 3 statuses) | 0 | 0 to 3 |
| Event listener keys | 6 | Cull listener | Cull listener |
| Death spawns | 8 orbs | 1 `Disintegration` | Choice |

**Other facts:**
- `body.preProcess` for `Megaman` is 142 lines (`BodyComponent.kt:283-424`). It runs in each fixed step. All 4 `Performance` values give 2.5 steps per frame (`C/state/Performance.kt:4-7`).
- `collectContacts` runs per active fixture in each step (`engine/.../world/WorldSystem.kt:230`). So the fixture count multiplies there. The `ENEMY` filter acts after the grid lookup, not before.
- The first `init()` of each instance does 1370 `containsRegion` calls (137 keys × 10 atlases) and builds 431 animations (`M/sprites/MegamanAnimations.kt:47-62`). `TextureAtlas.findRegion` is a linear scan (from my knowledge of libGDX, not read here). This happens in play at the first spawn, unless the pool is preloaded.
- The animation key builds strings each frame. `buildFullKey` calls `lowercase()`, `replace()` and a template (`MegamanAnimations.kt:40-41`).

#### 6. Tiled placement

**Placement:**
- Put a rectangle object in an `enemies` layer with name `MegamanClone`.
- The class is `com.megaman.maverick.game.entities.enemies.MegamanClone`, with constructor `(services: MegaServices)`.
- A rectangle gives `ConstKeys.BOUNDS` (`C/utils/extensions/Extensions.kt:73`).
- Gauntlet: `wave_<w>_<n>` works with no change (`GauntletMaster.kt:143`).
- `GAUNTLET_1` has a `canSpawn` filter on `Life` and `WeaponEnergyBulb` (`C/levels/LevelDefinition.kt:185-188`), so drops of those 2 items do not spawn there.
- `utils/level_builder/level_builder.py` and 2 skills name `SniperJoe`. I did not check if they need a new entry.

**Props:** `spawn_type`, `respawnable`, `spawn_blink`, `cull_out_of_bounds`, `cull_time`, `cull_events`, `drop_item_on_death`, `hard_mode_only`, `group`. The clone reads its own `facing` and `direction`.

**Which rules apply, if the clone extends `AbstractEnemy`:**

| Rule | Applies | Site or note |
|---|---|---|
| Drops | Yes | `AbstractEnemy.kt:147-170`. `targetPlayer.playerSlot!!` is safe, because the target is a slot. |
| Cull | Yes | Default on. A gauntlet turns it off (`GauntletMaster.kt:216-219`). |
| Spawn blink | Yes | Only through `invinciblePredicates`. The clone must not copy the `Megaman.invincible` override. |
| `roomBound` | Not found | 0 hits in code and docs. Nearest are `GauntletMaster.destroyChildrenOutOfRoom` (`:241-262`) and `isOutsideLevel`. If the design means a new rule, define it. |

For a slot P2, none of these apply today.

#### New APIs

None has a production caller today. Each proposed first caller is below.

| API | First caller | Owner |
|---|---|---|
| `FactionComponent` | Hostile P2 damage check | `030` |
| `CharacterDef` | `Megaman` and X | `081` |
| Typed move state component (name open) | `Megaman` behaviors | `081` |
| Limit on the animation set | `MegamanClone` | New |
| Slot-free buster fire | `MegamanClone` | `024` or new |
| `InputSource` for AI, with its own builder | `MegamanClone` | New |
| `MegamanClone` | TMX object; none exists | New |
| Slot child in `GauntletMaster` | Hostile P2 in a locked room | `047e` or new |
| Hostile-aware `PLAYER_JUST_DIED` filter | `GauntletMaster` | `030` or `047e` |
| Kill capability or list for slot-free bodies | `PlayerDeath`, `CameraEdgeBlock`, `GravityBlock`, `MegaLevelScreen:333` | Only if Q2 below says yes |

`MegaLevelScreen:333` for slot 1 needs no new API. It needs a loop over the slots.

#### Direct answer

- **P2 as an ordinary enemy:** yes. Remove `BossComponent`. Keep a `GauntletMaster`-type entity only for a locked room.
- **Many clones:** yes, but as `MegamanClone` with shared parts, no slot and an AI `InputSource`.
- **Not supported:** many `Megaman` instances.
- **The design rule:** a slot means a person or net peer owns the body. Hostility is a faction on top of that. A clone is an enemy that uses the same moves.

#### Recommendation

1. Take option (b). Keep `Megaman` slot-only.
2. Build nothing for clones now. Add the seam through `081`, because it has a real caller.
3. Build `MegamanClone` after that, with a limited move list and the buster atlas. Add a brain as its own part.
4. Keep `AbstractEnemy` on the clone until `048` removes it.
5. Fix the slot 1 camera-edge death on its own, because it breaks an owner decision today.

#### Risks

- `081` could choose one `PlayerEntity(services, def)` class. That does not fit the per-class pool for clones (see Q3).
- Silent failures: `PRE_PLAY` and zero gravity give no error.
- The `Megaman::class` exact keys do not match a clone. `DarknessV2`, `DarknessV4` and the masks ignore it (`AbstractHealthEntity.kt:75` also keys on the exact class).
- `setBehaviorAllowed` throws on a move that a clone lacks.
- N clones with one brain and no collision between them stack on one spot (inference). `MegaContactFilter.kt:106` removes enemy-to-enemy contact.
- N clones stack sounds unless they use a camera gate.
- First-spawn work for 68 animations falls in play, unless the pool is preloaded.
- `058`, `114` and the open `047` gravity question overlap with the `direction` setter split.

#### Open questions for the owner

1. Is hostility set for a session, a level or a room? **Recommend:** by room data, with an entity that owns the switch.
2. Must clones die from player-only causes (`PlayerDeath`, edge crush, `GravityBlock`)? 28 `PlayerDeath` objects are in 3 stage maps. **Recommend:** only the causes in shared contact rules, plus cull. Otherwise add one capability for all 4 sites.
3. Can a hostile P2 leave a room with P1? **Recommend:** no. Lock the room with `gauntlet_key`.
4. Does a dead human P2 stay dead until the next `PLAYER_SPAWN`? **Recommend:** yes, until `047c` decides.
5. Do player enhancements and owned weapons apply to clones? **Recommend:** no. The class chooses its own list.
6. Which moves does a clone have at first? **Recommend:** run, jump, air dash and buster.
7. What does `roomBound` mean in the design? **Recommend:** define it as a rule, or drop the name.

#### Not read in full

- `029` `NOTES.md` (only searched sections).
- `031` `NOTES.md` (only "Who drives an entity").
- `047` `NOTES.md`.
- `MegaLevelScreen.kt`.
- `MegamanWeaponsHandler.kt` (only some ranges).
- The bodies of the 8 behaviors (grep only).
- `WorldSystem.kt` (only some ranges).
- `MegaContactFilter.kt` outside `:100-125`.
- `AbstractBoss.kt`.
- Enemy fixture counts are per file. A class can add fixtures that my grep misses.
- I did not trace how quicksand kills a player. `QuickSand.kt:182` acts only on `SUBMERGE`, and `Megaman` uses `SINK`.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/behaviors/JumpBehavior.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/handlers/MegamanWeaponsHandler.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/special/GauntletMaster.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
