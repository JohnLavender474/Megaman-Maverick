# Notes: 029 `IPlayer` contract, players service and `CharacterDef`

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design, number checks (2026-10-22)

````text
REVIEW: players, input and entity design (items 12+14, 13, 23, 28, 29, 30, 41)
Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` (C/) or `engine/src/main/kotlin/com/mega/game/engine/` (E/), unless the path is given in full. I did not write any files.

NUMBER CHECKS (method: grep on non-import lines)
- Item 28: 889 `megaman` word uses in 224 files outside `entities/megaman/`. The list says 1096 in 235. The difference comes from the count method. The scale is the same.
- Item 29: 70 `owner ==/!= megaman` / `== megaman` checks (confirmed). 37 `is Megaman` (list says 38).
- Item 30: 55 `game.controllerPoller.is*` calls plus 8 `poller.isPressed` calls in Megaman's actuators, so 63 in total. 30 of them are in `entities/megaman/components/BehaviorsComponent.kt`. Index 0 is hard-coded at `E/controller/ControllerUtils.kt:11,23`.
- Item 13: 77 rules (begin 34, continue 26, end 17), confirmed. The "48 megaman refs" number is wrong. It counts import and package text. The real code has 16 refs: 14 `is Megaman` and 2 `megaman`. Fix this number in the backlog.
- Item 14: 117 `enum class *State`. `StateMachine` or the builder is used in 25 files and `Loop(` in 41 files. Two earlier helpers exist and are used by 4 enemies: `C/entities/utils/StateLoopHandler.kt` and `StateMachineHandler.kt`.
````

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 28: `IPlayer` CONTRACT AND PLAYERS SERVICE".

````text
1. Current state
- `game.megaman` is a `lateinit` field (`MegamanMaverickGame.kt:204, 372`), reached everywhere through `IMegaGameEntity.megaman` (`entities/contracts/IMegaGameEntity.kt:22`).
- `Megaman.kt` (1045 lines) implements 15 interfaces (`:71-73`). It mixes character physics (the direction setter copies about 10 gravity/velocity values from `MegamanValues` by sign, `:239-269`; `MegamanValues` has 86 constants), the damage reaction (`:660-707`), progress and profile data (lives `:175`, health and heart tanks `:957-988`, rewards `:79-93, 917-926`), and weapons.
- Uses outside the player package, grouped by member (dotted access only):

| member | count | goes to |
|---|---|---|
| `body` | 524 (`getX` 180, `getCenter` 136, `getBounds` 72, `getY` 46, `getMaxX` 26, `velocity` 19, `getMaxY` 13) | core |
| `direction` | 58 | core |
| `dead` | 22 | core |
| `ready` | 14 | core |
| `feetFixture` / `left/rightSideFixture` / `headFixture` / `damageableFixture` | 10 / 7+7 / 3 / 2 | core |
| `isFacing` / `facing` | 7 / 4 | core |
| `isBehaviorActive` / `isAnyBehaviorActive` | 7 / 1 (CLIMBING, WALL_SLIDING, JETPACKING) | core |
| `canBeDamaged` | 6 (spawn/end handlers, Wily, `MegaLevelScreen`) | core |
| `setAllBehaviorsAllowed` | 3 | core |
| health getters/setters | about 12 | core |
| `getFocusPosition` | 1 | core |
| `currentWeapon` | 7 | loadout |
| `weaponsHandler` | 5 (HUD, pause) | loadout |
| `charging` / `fullyCharged` / `ammo` | 4 / 2 / 1 | loadout |
| `lives` / `resetLives` / `removeOneLife` | 5 / 4 / 1 | profile |
| `hasHealthTank` / `hasHeartTank` / `addToHealthTanks` / `hasEnhancement` | 5 / 3 / 1 / 2 | profile |
| `aButtonTask` | 2 (GravitySwitcharoo, GravityCapsule) | Mega-only |
| `canMakeLandSound`, `teleporting`, `shooting`, `running` | 1 each | Mega-only |

  Conclusion: the external need is about 90% "where is the player, and what is the player doing". The minimal contract is small.
- Other single-player couplings: `PlayerStatsHandler(megaman)` (`MegaLevelScreen.kt:249`) and `cameraManagerForRooms.focus = megaman` (`:266`).
- X6 facts (`mmx-demake-tools/docs/players/map.md`, "Code layout"; `movement.md`, "Physics constants"):
  - X and Zero share `PlayerUpdate` and 32 step routines.
  - Character code is in overlays, called at fixed addresses under a test of `mega+0x02` (0 = X).
  - Character-only steps are in an extended table (X 32-47/74-77, Zero 48-63).
  - Physics values are fields on the player, written at beam-in. Armor-part bonuses are then added. The base values are the same for X and Zero in the states read.
  - Air dash is X-only in form 4 and always available for Zero.
  So X6 uses a shared core, character hooks, and data values with modifiers.

2. Options
A. Extract the interface only: `Megaman : IPlayer`. A new character subclasses `Megaman`. Pro: cheap. Con: Proto Man inherits all of Mega's internals.
B. `IPlayer` plus an `AbstractPlayer` base for shared core work (health/damage flow, spawn/room-transition events, focus, control locks). Each character is configured by a `CharacterDef`: a movement values table, an ordered ability list (slide, wall jump, dash, charge, climb, swim, jetpack as behavior factories), a body/fixture def, animations, a loadout and a damage negotiator.
C. Full composition from data: no `Megaman` class, and the player is built from components by a data file. Pro: most flexible. Con: a big-bang change, and it is hard to keep behavior the same.

3. Recommendation
B, reached through A as the first step.

Minimal interface (from the counts):
```
interface IPlayer : IMegaGameEntity, IBodyEntity, IHealthEntity, IDamageable, IFaceable, IDirectional, IFocusable {
    val playerIndex: Int
    val character: CharacterDef
    val faction: Faction                     // PLAYER (item 29)
    val dead: Boolean
    val ready: Boolean
    val feet: IFixture; val head: IFixture; fun side(left: Boolean): IFixture
    fun isDoing(action: PlayerAction): Boolean   // CLIMBING, WALL_SLIDING, JETPACKING...; false if the ability is absent
    fun lock(reason: Any); fun unlock(reason: Any)   // replaces canBeDamaged/setAllBehaviorsAllowed from handlers (item 11)
    val loadout: ILoadout                    // currentWeapon, ammo, charge status (item 23)
    val input: InputFrame                    // item 30
    fun onGravityChanged(dir: Direction)     // replaces external aButtonTask writes
}
interface IPlayers {
    fun all(): Iterable<IPlayer>
    fun byIndex(i: Int): IPlayer?
    fun nearestTo(p: Vector2, aliveOnly: Boolean = true): IPlayer?
    val primary: IPlayer
    fun anyAlive(): Boolean
}
```
- Lives, tanks and enhancements move to a `PlayerProfile` in game state, not on the character. Owner decision: one profile shared in co-op, or one per slot.
- Mega Man as the first implementation with no behavior change:
  - `Megaman : AbstractPlayer, IPlayer` with `MegamanDef`.
  - `MovementValues` is filled with the exact `MegamanValues` constants, with `forDirection(dir)` for the sign flip.
  - The ability list is the 8 current behaviors in the current order (`BehaviorsComponent.kt:778-785`), because evaluation order matters.
  - `game.players` holds one player.
  - Enemy code switches from `megaman` to `player` = `game.players.nearestTo(body center)`. With one player this is the same object, so behavior does not change.
  - Keep `megaman` as a deprecated typed alias during the migration.
  - Cross-ability couplings (NEEDLE_SPIN or RUSH_JET checks inside behaviors) become weapon-driven ability blocks and modifiers (item 23).
- Prove each step with a replay trace (item 5).

4. Open questions for the owner
- In co-op, one shared life pool, or one per player?
- Target selection: is "nearest" enough, or do some enemies need "last attacker" or "random"?
- Must camera focus follow one player or a group bounds?
- Do all characters share sliding/ladder rules, as in X6 where the steps are shared, with only values and abilities that change?

5. Backlog wording
Replace "Remove the global `megaman`" with: "`IPlayer` contract, players service and `CharacterDef`"
- `IPlayer` (body, facing, direction, dead/ready, health/damage, faction, focus, `isDoing`, locks, loadout, input). `Megaman` implements it.
- `game.players` (`all`, `byIndex`, `nearestTo`, `primary`). Migrate the 889 external `megaman` uses in 224 files.
- Move lives, tanks and enhancements to a `PlayerProfile`.
- `CharacterDef`: movement table, ordered ability set, body def, animations, loadout. `MegamanDef` keeps behavior identical (replay-proven).
- A prototype second character (Proto Man) proves the contract.

6. Dependencies
Item 29 (faction), 30 (input per player), 23 (loadout), 11 (lock reasons), 15 (`LevelContext` gives `players`), 13 (contact receivers), 5 (replay proof), 6 (profile save), 36/37 (respawn per player).
````

## From the old `BACKLOG.md` (2026-10-22)

`megaman` is reached globally through `game.megaman` (for example in `MegaLevelScreen`,
`AbstractEnemy` and `AbstractBoss`). This blocks other playable characters and multiplayer.


## Research round 2 (2026-10-23): summary

Five research agents reviewed the current design at `bacbd22c4`, after `014` merged. The owner's target for this round:

- A lightweight but complete interface that is the contract for every player in the game.
- Co-op: many players on one shared screen. Split screen and online are `TODO`s, but no choice now may block them. No logic may depend on a single player instance.

Each agent report is below, in full: review 1 (inside the player), review 2 (uses outside the player), review 3 (systems around the player), review 4 (design options), review 5 (how games and engines usually do it). This summary combines them. Recommendations are not decisions.

### Strengths of the current design

- **Per-instance state is clean.** `Megaman` has no mutable static or companion state. Timers, behaviors, fixtures, receivers and animations are made per instance in `init()` (review 1).
- **Behaviors are ordered, self-contained objects** (`IBehavior`, order at `BehaviorsComponent.kt:778-785`). They map directly to an ordered ability list (review 1). X6 uses the same "ordered try chain" model (review 4).
- **The external need is small.** About 65% of the 930 external lines read only position and bounds (targeting and proximity). About 75% of the files use only the `megaman` extension, so one helper change moves most enemies (review 2).
- **Spawning, culling and checkpoints use the camera, not the player.** A shared screen needs no change there (review 3).
- **`IFocusable` already decouples the camera from `Megaman`.** A group focus can implement it (review 3).
- **`014` receivers and capability markers** (`IGateTrigger`, `ICartRider`) already show the style that replaces `is Megaman` (reviews 1, 4).
- **`MegaWeaponHandler` is already a data-plus-lambdas slot**, a base for a loadout (review 1).
- **The engine has almost no player concept**, so the engine-side work is additive (review 3).

### Weaknesses

Ranked by how hard they block co-op and a second character:

1. **Input is global.** One poller, pad index 0, all devices merged, menus on the same poller, global `TURN_CONTROLLER_*`. 37 poller calls in the player package, plus 4 entities that read controller 0 for "the player" (`PipePortal`, `SpringBouncer`, `SpiderWeb`, `GroundSlideBlocker`). This is `031`, but `029` must not make it worse (reviews 1, 2, 3).
2. **Slot, profile and body are one object.** Lives, tanks, enhancements, unlocked weapons and the level reward table live on `Megaman` or in `GameState` with `Megaman` as the only listener. In every engine reviewed, these live longer than the body (reviews 1, 3, 5).
3. **Player events have no identity, and `PLAYER_SPAWN` is a level reset.** 24 listeners reset or destroy themselves on `PLAYER_SPAWN`. With respawn per player, one player's respawn resets the room for all (review 2).
4. **One camera focus drives room detection and transitions** (`CameraManagerForRooms.kt:180-238`, `:284-296`) (review 3).
5. **Level flow assumes one player.** Death, respawn, boss intro and defeat, pause, end level. The handlers keep `game.megaman` in a field from construction (reviews 2, 3).
6. **Ownership is `owner == megaman`.** The worst site is `AbstractEnemy.kt:255`: a second player's shots would damage no enemy. Damage scaling reads `megaman.hasEnhancement`, not the owner's (review 2). This is `030`.
7. **Exact-class `KClass` sets fail silently.** `StandardDamageNegotiator` and about 6 other entities look up `damager::class` with `==`. A second character class deals 0 damage or has no effect, with no error (review 2). This was not known before this round.
8. **Mutations from outside.** About 53 sites write player state: handlers write `ready`, `canBeDamaged`, `gravityOn` and `setAllBehaviorsAllowed`; 6 enemies write the velocity with `MegamanValues.JUMP_VEL` for stomp bounces; gravity devices write `direction` and `aButtonTask` (review 2).
9. **Self-references through the global inside the player.** 7 sites in the player package read `megaman` (the global), not `this`. Two live instances interfere (stun impulse, sprite visibility, air dash, trail sprites) (review 1).
10. **Weapon logic is inside the movement behaviors.** 47 checks of specific weapons (`NEEDLE_SPIN`, `RUSH_JET`, `RODENT_CLAWS`, `AXE_SWINGER`). A second character cannot reuse "jump" without them (review 1). This is `024`.
11. **Values are a global object plus inline numbers.** 90 `MegamanValues` entries (about 45 movement, about 30 weapon), 59 direction switches, and many inline offsets (review 1).
12. **Determinism.** Clock-seeded RNG, `Gdx.graphics.deltaTime` in the camera, UI timers that freeze systems. These block only online play (reviews 3, 5).

### Where the reviews agree

- Split the contract in layers. Enemies need a small read-only view (position, bounds, facing, direction, alive or active, `isDoing`). Commands and features go on other interfaces or behind capability queries (reviews 2, 4, 5).
- Identity is a slot, not the entity. Events carry the slot. Death or respawn changes the body, not the slot (reviews 3, 4, 5).
- A players service in fixed slot order, with no public `primary`. Every engine example and every co-op game example avoids a primary player in gameplay rules; a hidden "player 0" is the most common co-op bug (reviews 4, 5).
- Targets are picked by a policy (default: nearest), with stickiness or a re-pick at defined points, not every frame. With one player this changes nothing (reviews 2, 4, 5).
- Lives, tanks, enhancements and weapons owned go to a per-slot profile, with a policy for shared or per-player data (reviews 1, 3, 4, 5).
- Characters: composition plus data (`CharacterDef` with a movement table and an ordered ability list), not subclasses of `Megaman` (reviews 1, 4, 5).
- Level flow (camera, off-screen, room transition, death and respawn, checkpoints, boss flow, pause, HUD, pickups) becomes policy objects. Each starts with a single-player implementation that copies today's behavior (review 3).

### Where the reviews differ

- **What is on `IPlayer`.** Review 3 puts `input`, `loadout`, fixtures and `onGravityChanged` on the contract. Review 4 keeps them off: input belongs to the slot (`031`), loadout to an `IHasLoadout` capability (`024`), fixtures to receivers (`014`), gravity to a capability. Review 5 supports review 4. Recommendation: review 4.
- **Commands needed from outside.** Review 2 finds external writes that a pure view cannot cover: `bounce()` (stomps), a gravity-direction change, and locks with reasons. Review 4 has locks and capabilities but no `bounce()`. These belong on `IPlayer` (the command layer), not on the view.
- **Lifecycle.** Review 4 proposes one `PlayerPhase` value in place of `dead`, `ready` and `canBeDamaged`. Review 2 counts 22 `dead` and 14 `ready` reads outside. A phase can keep `dead` and `ready` as derived reads during the migration.

### Possible bugs and corrections found

- `swim.init` flips `swimVel` again for DOWN and RIGHT (`BehaviorsComponent.kt:133-137`). By review 1's reading, the swim impulse points toward the floor in those directions. Not verified. Do not file it before an in-game check; any refactor keeps the current result.
- `MegamanValues` has 90 entries, not 86. The old review numbers in the 2026-10-22 text above map to new IDs: faction `030`, input `031`, lock reasons `012`, replay `055`, `LevelContext` `016`.
- The 2026-10-22 plan says "replay-proven", but `055` is far away. Review 4 recommends the play-through contract plus unit tests instead.

### Decisions for the owner (merged from all reviews)

1. Maximum number of players: 2 or 4.
2. Target policy: is nearest the default? Does an enemy keep its target until the target is not targetable, or pick again at state changes? Are `LAST_ATTACKER` or `RANDOM` needed in `029`?
3. No public `primary`: slot 0 only by name where it is really needed (save, title screen)?
4. Events: which events become "any player" and which "all players" (`GauntletMaster` resets, level reset, `PLAYER_DONE_DYIN`, projectile cleanup)? Split `PLAYER_SPAWN` into a per-player spawn and a level reset?
5. Shared or per-slot data, field by field: lives (decision H), health tanks, heart tanks, enhancements, weapons owned, bosses beaten, currency.
6. Where the types live: slot, players service and targeting in the engine at once, and `IPlayer` in core first?
7. Do all characters share the ladder and slide code (only values and abilities change)?
8. Proof for `029`: play-through plus unit tests, without replay?
9. Proto Man: in `029`, or a separate item after the `CharacterDef` branch?
10. Level-flow policies for the shared screen (camera, off-screen, room transition, death, checkpoints, boss flow, pause, HUD, pickups, gravity rooms): decide now, or leave them to `047` and implement only the single-player policy in `029`?

## Review 1: the inside of the player (2026-10-23)

Research agent report at `bacbd22c4`. Its angle is `entities/megaman/` and the interfaces `Megaman` implements. The agent changed no files. `M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

### 0. Counts

- 25 files, 5392 lines. The biggest: `M/handlers/MegamanWeaponsHandler.kt` 1292, `M/Megaman.kt` 1045, `M/components/BehaviorsComponent.kt` 788, `M/components/BodyComponent.kt` 452, `M/components/ControllerComponent.kt` 247, `M/components/SpritesComponent.kt` 236, `M/sprites/MegamanAnimations.kt` 236, `M/sprites/MegamanAnimationDefs.kt` 204.
- `MegamanValues` (`M/constants/MegamanValues.kt`) has 90 `val`/`const val` entries (the 2026-10-22 review says 86).
- 8 behaviors.
- 37 `game.controllerPoller` calls in the package (31 in `BehaviorsComponent.kt`, 1 each in `Megaman.kt`, `BodyComponent.kt`, `AnimationsComponent.kt`, `MegamanWeaponsHandler.kt`, `MegamanAnimationKeySupplier.kt`), plus 8 `poller.isPressed` calls in `ControllerComponent.kt`.
- 59 direction branches (`when (direction)` or `isVertical`/`isHorizontal`): behaviors 23, weapons 15, `Megaman.kt` 8, body 6, sprites 6, animation key 1.
- 47 checks of a specific weapon (`NEEDLE_SPIN`, `RUSH_JET`, `RODENT_CLAWS`, `AXE_SWINGER`) in the components, `Megaman.kt`, the jetpack handler and the sprites, not counting the weapons handler.
- 7 self-references through the global `megaman` extension (`C/entities/contracts/IMegaGameEntity.kt:21`) inside the player package (section 2e).

### 1. Structure

**Class.** `Megaman : AbstractHealthEntity` (`M/Megaman.kt:71-73`) implements 16 interfaces: `IBodyEntity`, `ISpritesEntity`, `IBehaviorsEntity`, `IPointsEntity`, `IAudioEntity`, `IAnimatedEntity`, `IScalableGravityEntity`, `IFreezableEntity`, `IGameStateListener`, `IEventListener`, `IFaceable`, `IDirectional`, `IFocusable`, `IDamager`, `IGateTrigger`, `ICartRider`. `AbstractHealthEntity` adds `IHealthEntity`, `IDamageable` and the Points and Updatables components.

**Creation.** The game makes one instance by hand (`C/MegamanMaverickGame.kt:391-396`: `init()`, `initialized = true`, `state.addListener(megaman)`). It is not pooled.

**Components** (added in `init`, `M/Megaman.kt:343-421`):

- Body (`M/components/BodyComponent.kt:78`): 13 fixtures (PLAYER, BODY, FEET, feet-gravity CONSUMER, HEAD, two SIDE, DAMAGEABLE, WATER_LISTENER, TELEPORTER_LISTENER, needle DAMAGER, needle SHIELD, axe SHIELD). Most physics runs in `body.preProcess` (`:299-440`): sizing, gravity selection, clamp, damageable rect. The `014` receivers are registered at `:111-121, 165-168, 189, 211-215, 243`.
- Behaviors (`M/components/BehaviorsComponent.kt`). The evaluation order is the `OrderedMap` insertion order at `:778-785`: WALL_SLIDING, SWIMMING, JUMPING, AIR_DASHING, GROUND_SLIDING, CROUCHING, CLIMBING, JETPACKING. Each behavior is a closure over `this` with its own timers.
- Controller (`M/components/ControllerComponent.kt:120-125`): B = charge and shoot, LEFT/RIGHT = run, SELECT = next weapon.
- Sprites (`M/components/SpritesComponent.kt:105`): main sprite, jetpack flame, damage burst, desert tornado.
- Animations (`M/components/AnimationsComponent.kt:292`): `MegamanAnimations(game).get()` makes new `Animation` objects per instance from shared atlases. Key supplier: `M/sprites/MegamanAnimationKeySupplier.kt`.
- Audio, Points (health), Updatables (the large tick at `M/Megaman.kt:732-854`), DrawableShapes (debug, `BodyComponent.kt:442`).

**Handlers.**

- `MegamanWeaponsHandler`: one `MegaWeaponHandler` per owned weapon, with ammo, cooldown, costs, the spawned set and the fire functions.
- `MegamanJetpackHandler`: Rush Jet ammo drain and refill.
- `MegamanDamageNegotiator`: a damage table keyed by tag, size and type.

**State on the instance.** About 20 timers, 10 signed physics fields (`:322-331`), `lives` (`:175`) and `damageListeners`. `facing`, `aButtonTask`, `currentWeapon` and `running` are entity properties keyed by `MegamanProps` and `ConstKeys` (`:272-297`).

**Events.** It listens to `BEGIN/CONTINUE/END_ROOM_TRANS`, `GATE_INIT_OPENING`, `STUN_PLAYER` and `END_GAME_CAM_ROTATION` (`:163-170`, handled at `:543-643`). It submits `PLAYER_SPAWN` (`:507`), `PLAYER_JUST_DIED` (`:518`) and `START_GAME_CAM_ROTATION` (`:221`). These events carry no player identity.

**Profile hooks (`IGameStateListener`).** `onAddLevelDefeated` gives weapons through `getWeaponsFromLevelDef` (`:79-93, 917-926`). `onAdd/RemoveHeartTank` changes max health (`:928-934`). `onAddCurrency` plays a sound (`:936`). Health tanks and enhancements are read from `game.state` (`:941-988`).

**Possible dead code (guess):** `M/components/PointsComponent.kt` is shadowed by the member override at `M/Megaman.kt:723`; `M/sprites/MegamanTrailSprite.kt` (V1) is registered only in the deprecated `DecorationsFactory`; a commented block at `:353-409`.

### 2. Classification

**(a) Core contract.** Body and fixture accessors (`BodyComponent.kt:54-76`), `direction`, `facing`, `dead`, `ready`, `canMove`, the health and damage flow (`canBeDamagedBy`, `takeDamageFrom`, `invincible`; `:121-123, 645-707`), `getFocusBounds`/`getFocusPosition` (`:901-915`), `teleporting` and the teleport hooks (`:483-500`), room-transition handling (`:545-595`), `frozen`, stun, `gravityScalar`, `movementScalar`, `isBehaviorActive`.

**(b) Shared machinery other characters can reuse.**

- The movement behaviors (run, jump, wall slide, slide, climb, swim, air dash, crouch) and the `AButtonTask` state machine.
- Post-action momentum (`:1013-1040`), the feet-gravity "over ground" consumer (`BodyComponent.kt:160-185`), the gravity-selection ladder (`:370-381`).
- Damage knockback, recovery flash, freeze and unfreeze (`:812-831`).
- The `MegaWeaponHandler` slot model (`MegamanWeaponsHandler.kt:54-131`).
- Direction-aware sprite placement (`SpritesComponent.kt:42-103`).

**(c) Mega-specific content.**

- The `MegamanWeapon` enum and all weapon code, including the hard-coded spawn-offset table (`MegamanWeaponsHandler.kt:156-290`).
- Sprites and animation defs, sounds (`MEGA_BUSTER_CHARGING_SOUND` and others), the death explosion orbs (`:529-537`), `MegamanDamageNegotiator`.
- Body sizes (`BodyComponent.kt:38-42`).
- Weapon-driven ability changes inside behaviors: `NEEDLE_SPIN && shooting` blocks 6 behaviors; `RUSH_JET` turns air dash into a jet (`BehaviorsComponent.kt:281-284, 298, 311`); `RODENT_CLAWS` sticks to walls (`:96-102`).

**(d) Profile or progress data on the character.**

- `lives` (`:175, 996-1000`; reset from `C/MegamanMaverickGame.kt:762, 779, 821`).
- Heart-tank max-health bumps (`:928-934`), `addToHealthTanks`/`hasHealthTank`/`hasHeartTank` (`:957-988`), `hasEnhancement` (`:941`).
- The unlocked-weapon set inside `weaponsHandler`, the level-to-weapon reward table in the companion (`:79-93`), `game.putProperty(WEAPONS_ATTAINED)` (`:919`).
- Enhancement scalars applied in behaviors (`BehaviorsComponent.kt:140, 198, 205, 493`; `ControllerComponent.kt:65`; `:711`).

**(e) Global or single-player assumptions.**

- Self-references through the global `megaman`: `M/Megaman.kt:629, 929, 933, 990`; `BodyComponent.kt:192-195`; `SpritesComponent.kt:101`; `BehaviorsComponent.kt:279`; `MegamanAnimationKeySupplier.kt:42-43, 120`; `MegamanTrailSpriteV2.kt:50-74` (the trail sprite reads all of its data from the global).
- Input: the single `game.controllerPoller` (37 call sites), `ControllerSystem(controllerPoller)` with one poller for all entities (`C/MegamanMaverickGame.kt:859`), `ControllerUtils` index 0. `MegamanValues.BUTTONS_TO_UNFREEZE` puts a button binding in the values table (`MegamanValues.kt:115`). `game.selectButtonAction` is one global setting (`BehaviorsComponent.kt:263-271`, `ControllerComponent.kt:98`).
- Camera: `game.setFocusSnappedAway` (5 calls); `game.getGameCamera()` and `START_GAME_CAM_ROTATION` in the `direction` setter (`:217-236`); `game.isCameraRotating()` (9 calls).
- Level checks inside the character: `getMaxRunSpeed` tests `LevelDefinition.MOON_MAN` (`BodyComponent.kt:450`); `MARIO_LEVEL` sounds (`:539`, `BehaviorsComponent.kt:239`, `BodyComponent.kt:190`); the out-of-bounds kill reads `game.getTiledMapLoadResult()` (`:735-737`).
- The difficulty check from `game.state` is in `editDamageFrom` (`:718`).
- Mutable companion state: none. The companion holds `TAG`, `explosionOrbTrajectories` (copied before use) and constants. `MegamanValues` holds three gdx `Array`s that are mutable by type but only read. The top-level `BEHAVIORS_TO_END_ON_BOUNCE` is read-only.

### 3. Strengths to keep

- Per-instance state is clean. Timers, physics fields, behaviors, fixtures, receivers and animations are made per instance in `init()`. No static mutable state, so a second instance does not corrupt shared tables.
- Behaviors are self-contained `IBehavior` objects (`evaluate`, `init`, `act`, `end`) in an explicit order. They map directly to an ordered ability list in a `CharacterDef`.
- The `014` receivers keep contact reactions on the player's own fixtures.
- `MegaWeaponHandler` is already a data-plus-lambdas slot (cost, cooldown, chargeable, `canFire`, pre and post shoot). A good base for a loadout.
- The fixture accessors (`feetFixture`, `headFixture`, `leftSideFixture`, ...) are a small, stable surface.
- The engine `BehaviorsComponent.setBehaviorAllowed` already supports lock-style gating.
- `IMegamanDamageListener` is already an observer hook (used by `AbstractBoss`).

### 4. Weaknesses

**(i) A second character.**

- Everything is typed on `Megaman`: extension functions on `Megaman`, `IMegamanDamageListener.onMegamanDamaged(.., megaman: Megaman)`, the `MegamanWeapon` enum in behaviors.
- The 8 behaviors are built in one function with weapon checks inside. Proto Man cannot reuse "jump" without the `NEEDLE_SPIN`, `RUSH_JET` and `RODENT_CLAWS` logic.
- Values come from a global `object` (90 entries) and many inline numbers: crouch and slide translate offsets (`BehaviorsComponent.kt:405-415, 461-466, 508`), fixture sizes, sprite offsets, weapon spawn offsets, `MEGAMAN_LADDER_MOVE_OFFSET`, `DEATH_*_OFFSET`.
- `AButtonTask` ties the A button to JUMP, SWIM and AIR_DASH for all characters.
- Sounds and assets are named in place (`SoundAsset.MEGA_BUSTER_CHARGING_SOUND` 6 times).
- `IHazard.getDamageToMegaman()` and `MegaEnhancement` are named for Mega.

**(ii) Two `Megaman` instances alive at once.** A concrete trace: A is `game.megaman`; B is a second instance (`MegaEntityFactory` can make one, because the constructor is `(game)`).

1. **Input.** Both read the same poller, and `ControllerSystem` sends the same statuses to both. They move the same way.
2. **Room transitions.** Both listen to `BEGIN_ROOM_TRANS`. The event carries one `POSITION`, so both move to the same point (`:552`).
3. **Stun.** Each instance handles `STUN_PLAYER`, but `:629` writes `megaman.body.physics.velocity`. A gets the impulse twice; B gets none but starts its stun timer.
4. **Heart tanks.** Only A is a state listener (`C/MegamanMaverickGame.kt:396`), so B never gets a heart tank, weapon reward or currency. If B were registered, `:929`/`:933` would still change A's max health.
5. **Sprites and animation.** B's visibility depends on A's teleporter (`SpritesComponent.kt:101`). B's pipe-portal animation and weapon key check read A (`MegamanAnimationKeySupplier.kt:42-43, 120`).
6. **Air dash.** B's air dash is blocked when A faces a block (`BehaviorsComponent.kt:279`).
7. **Head-bump sound.** The Mario head-bump sound uses A's velocity (`BodyComponent.kt:192-195`).
8. **Ammo.** `translateAmmo` uses A's current weapon by default (`:990`). `MegamanJetpackHandler.kt:380` passes the weapon, so it is safe.
9. **Trail sprites.** B's trail sprites spawn at A's position with A's weapon and pose. `MegamanTrailSpriteV2` reads only the global; `tryToSpawnTrailSprite` (`:881`) passes no owner.
10. **Audio.** `stopSound(key)` stops every playing instance of that `SoundAsset` (`C/audio/MegaAudioManager.kt:172-176`). A's `stopCharging()` cuts B's charge sound.
11. **Camera.** A gravity change on either player rotates the shared camera and sets the global focus-snap flag. Climbing writes `setFocusSnappedAway` (`BehaviorsComponent.kt:635, 708`), so the two players toggle it against each other.
12. **Global properties.** `WEAPONS_ATTAINED`; `MegaGameEntities` tag lookups ("Megaman" gives two entities). The `game.updatables` delayed runs use per-instance ids, so they are safe.
13. **Lifecycle events.** `PLAYER_SPAWN` and `PLAYER_JUST_DIED` have no identity. Listeners cannot tell which player died.
14. **Friendly fire.** `canBeDamagedBy` excludes only its own projectiles (`:647-650`). `MegamanDamageNegotiator.get` gives 0 for an owned projectile only when the owner is `this` (`:310`). Whether B's bullets hurt A depends on type, size and hazard fallbacks (a plain `Bullet` probably gives 0; guess). There is no faction concept.

What does not break: timers, behaviors, fixtures, health points, weapons handler state, the out-of-bounds check, damage timers. These are per instance.

### 5. Physics and values

How values are applied. Two mechanisms:

1. The `direction` setter (`M/Megaman.kt:239-269`) copies 10 values (`jumpVel`, `wallJumpVel`, 7 gravities, `swimVel`) from `MegamanValues` and negates them for DOWN and LEFT. `body.preProcess` then selects one gravity (`BodyComponent.kt:370-381`) and applies it on the axis of the direction (`:383-437`).
2. All other values are read from `MegamanValues` at the point of use, with a `when (direction)` per call (59 switches). Examples: run impulse, air dash, slide, climb, jetpack, damage knockback (`:671-692`), stun.

Inconsistencies:

- The stun impulse assumes UP (`:627-632`, existing `TODO`).
- `swim.init` flips `swimVel` again for DOWN and RIGHT (`BehaviorsComponent.kt:133-137`), but `swimVel` is already signed. By the agent's reading, the swim impulse on DOWN and RIGHT points toward the floor. Guess; verify in game before you call it a bug. A refactor must keep the current result.

Difficulty of a per-character table: low to medium.

- About 45 of the 90 values are movement, gravity, timing or damage values. They are only read and can move into a `MovementValues` data class with `forDirection(dir)` at once. The 10 signed fields already act as a per-direction view.
- About 30 entries are weapon values (bullet, ice cube, moon scythe, needle, axe, shield gem, jetpack, charge times, `MAX_WEAPON_AMMO`). They belong to a loadout table, not to the character.
- `BUTTONS_TO_UNFREEZE` belongs to input. `START_HEALTH` and `MAX_WEAPON_AMMO` touch the profile.
- The harder part is the inline numbers: body and fixture sizes, crouch and slide offsets per direction, sprite offsets, the weapon spawn-offset table, and the scalars in `MegaEnhancement` and the companion. These need a body def and a sprite or attachment def, not only a values table.
- One "local frame" helper (local x/y to world) would make the 59 direction switches easier to keep correct. Not needed for the first step.
- Behaviors read values by name, so the first step is mechanical: replace `MegamanValues.X` with `values.x`, where `values` is an instance field with the same constants. Gameplay does not change.

### Suggested first fixes (low risk, needed for co-op)

- Replace the 7 global `megaman` self-references with `this` (in the trail sprite, an owner in the spawn props).
- Pass the player identity in `PLAYER_SPAWN`, `PLAYER_JUST_DIED` and `STUN_PLAYER`.
- Move `lives`, the tank and enhancement queries, and the level reward table out of `Megaman` into a profile.

## Review 2: uses of the player outside its package (2026-10-23)

Research agent report at `bacbd22c4`. Its angle is code in `core/` outside `entities/megaman/` that depends on the player. The agent changed no files. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Count method

- `grep -rnw` for the words `megaman|Megaman|MegamanValues` on all `.kt` files outside `entities/megaman/`, without `import` and `package` lines: 930 lines in 243 files.
- The lowercase word `megaman`: 883 in 224 files (the 2026-10-22 review says 889 in 224; the scale has not changed). Without comments and log lines: 862 in 223 files.
- Intent counts come from an ordered regex classifier, checked by hand on samples. Read them as ±10%.

### 1. Counts by package

| Package | Lines | Files |
|---|---|---|
| `entities/enemies` | 395 | 104 |
| `entities/bosses` | 140 | 22 |
| `entities/special` | 84 | 23 |
| `entities/projectiles` | 38 | 23 |
| `screens/levels/events` (spawn, death, end-level handlers) | 37 | 3 |
| `entities/blocks` | 34 | 13 |
| `screens/levels/MegaLevelScreen.kt` | 32 | 1 |
| `screens/levels/stats/PlayerStatsHandler.kt` (HUD) | 31 | 1 |
| `entities/hazards` | 31 | 16 |
| `entities/contracts` (`AbstractEnemy`, `AbstractBoss`, `ItemEntity`, `IMegaGameEntity`) | 18 | 4 |
| `screens/menus` (pause, main, level select) | 18 | 3 |
| root (`MegamanMaverickGame.kt`, `Constants.kt`) | 18 | 2 |
| `entities/items` | 16 | 8 |
| `world` (contact filter, rules, `Receivers`, `IBodyExtensions`) | 7 | 5 |
| `entities/decorations` | 7 | 5 |
| `entities/sensors` | 5 | 2 |
| `utils` (`FacingUtils`) | 4 | 1 |
| `screens/other` (`GetWeaponScreen`) | 4 | 1 |
| `damage`, `levels` (comments only), `entities/explosions`, `EntityType` | 3 / 3 / 3 / 2 | 1 / 2 / 2 / 1 |
| `drawables`, `controllers`, `events`, `state`, `pathfinding`, `spawns`, `audio` | 0 | 0 |

By pattern (lines):

- `game.megaman`: 22 in 16 files. All other uses go through the `IMegaGameEntity.megaman` extension (`entities/contracts/IMegaGameEntity.kt:21-22`).
- `is Megaman` / `!is Megaman`: 27 / 3 (19 files).
- `as Megaman`: 1 (`world/contacts/rules/ReceiverRules.kt:110`).
- `Megaman::class`: 18 in 13 files.
- Identity or ownership checks (`== megaman`, `!= megaman`, `owner == megaman`): 57 in 40 files.
- `MegamanValues`: 11 in 9 files.
- `Megaman.TAG` as a key: 13 in 7 files.
- 75 files outside the package import from `entities/megaman`: `Megaman` (49), `MegamanValues` (10), `MegaHeartTank` / `MegaHealthTank` (8 / 8), `feetFixture` (8), `left/rightSideFixture` (5 / 5), `MegamanWeapon` (5), `MegaEnhancement` (5), `AButtonTask` (2), `IMegamanDamageListener` (1).

Members reached through `megaman.`:

| Member | Count |
|---|---|
| `body.getX` | 180 |
| `body.getCenter` | 136 |
| `body.getBounds` | 72 |
| `body.getY` | 46 |
| `body.getMaxX` | 26 |
| `body.getMaxY` | 13 |
| `body.getPositionPoint` | 9 |
| `body.physics` (`velocity.y` 15, `velocity.x` 4, `gravityOn` 3) | 22 |
| `body.isSensing` (`FEET_ON_GROUND` 5, `IN_WATER` 1) | 6 |
| `direction` | 58 |
| `dead` / `ready` / `spawned` | 22 / 14 / 1 |
| `feetFixture` / `left/rightSideFixture` / `headFixture` / `damageableFixture` / `feetGravityFixture` | 10 / 7+7 / 3 / 2 / 1 |
| `isFacing` / `facing` | 7 / 4 |
| `currentWeapon` / `weaponsHandler` / `hasWeapon` / `charging` / `fullyCharged` | 7 / 5 / 3 / 4 / 2 |
| `canBeDamaged` / `setAllBehaviorsAllowed` | 6 / 3 |
| `lives` / `resetLives` / `removeOneLife` / `isAtMinLives` | 5 / 4 / 1 / 1 |
| `hasHealthTank` / `hasHeartTank` / `hasAnyHealthTanks` / `addToHealthTanks` | 5 / 3 / 2 / 1 |
| `isBehaviorActive` / `isAnyBehaviorActive` | 4 / 1 |
| health getters and mutators | about 15 |
| `aButtonTask`, `damaged`, `invincible` | 2 each |
| `teleporting`, `shooting`, `running`, `bodyOverGround`, `getFocusPosition` | 1 each |

### 2. Uses by intent

| Intent | Lines | Files | Typical form |
|---|---|---|---|
| Targeting (face, aim, move toward) | about 290 | about 130 | Face: about 180 lines like `facing = if (megaman.body.getX() < body.getX()) LEFT else RIGHT`. Aim or jump at: about 60 (`GlacierMan.kt:655`, `MechaDragon.kt:759`, `MockingByte.kt:298`) |
| Proximity or trigger | about 195 | about 80 | `SeaMine.kt:138` (`overlaps(sensor)`), `Darspider.kt:157`, `DemonMet.kt:108` (`dst2` ordering) |
| Reading player state | about 120 | about 65 | `dead`, `ready`, `direction`, `isBehaviorActive(CLIMBING)` (`MagFly.kt:121`), `body.physics.velocity.y` (`RedKoopa.kt:201`), `shooting` / `facing` (`AbstractEnemy.kt:287-290`) |
| Identity ("is this the player?") | about 70 | about 53 | `entity == megaman`, `is Megaman`, `isAny(Megaman::class, ...)`, `contactWithPlayer(megaman: Megaman)` |
| Ownership or faction | about 45 | about 32 | `owner == megaman`, `owner is Megaman`, `Megaman::class pairTo dmgNeg(...)`, `DAMAGE_INCREASE` scaling |
| Changing the player | about 53 | about 24 | see section 5 |
| UI, HUD or profile | about 49 | about 17 | `PlayerStatsHandler`, `LevelPauseScreen`, `GetWeaponScreen`, item-drop odds |
| Camera or level flow | about 13, plus the handler writes | 5 | `MegaLevelScreen.kt:266` (`focus = megaman`), beam properties, room property `megaman_direction` |
| Mega-only internals (fixtures, sprite helpers) | about 15 | about 8 | `FloorButton.kt:168-171`, `InfernoChainPlatform.kt:123`, `EndLevelEventHandler.kt:167` |
| Audio | 2 | 2 | `Life.kt:52` (one-up sound), `PlayerStatsHandler.kt:46` |
| Declarations, wiring, comments, logs | about 75 | - | |

### 3. What each intent means with two or more players

| Intent | Default with 2+ players |
|---|---|
| Targeting | Nearest player that is alive and `ready`. Keep the target for a time, or pick again each frame? That needs a policy. |
| Proximity or trigger | Any player (`players.any { overlaps }`). |
| Identity | The player in the contact. The contact already carries the entity, so this becomes `entity is IPlayer`. |
| Ownership or faction | Any player faction (`owner is IPlayer`, or a faction check). The owning player for per-player effects. |
| Reading player state | Usually the player chosen by targeting or by the contact. |
| Changing the player | The player who triggered it. Spawn and death handlers act per player. |
| UI or HUD | Per slot (HUD row `n`), or a shared pool by the `LivesPolicy` decision. |
| Camera | Policy: one focus, a group box, or one camera per player (split screen). |

Uses where the answer is not clear:

- `AbstractEnemy.kt:95-104`: item-drop odds read `lives.current` and `getHealthRatio()`. Lowest-health player, the killer, or an average?
- `AbstractEnemy.kt:329`: the `ONE_UP` floating points give a life to `megaman`. The killer, or the shared pool?
- `AbstractEnemy.kt:287-290`, `isMegamanShootingAtMe`: any player who shoots and faces the enemy?
- `AbstractBoss.kt:130`: `megaman.addDamageListener(this)`, with `PreciousWoman.kt:304` as the one real listener. The boss must listen to all players, also players who join later.
- `MoonMan.kt:522`, `GravitySwitcharoo.kt:191-193`, `GravityCapsule.kt:181-183`, `MegaLevelScreen.kt:363-371`: these change the player's gravity direction. All players, or only the one in the trigger? A room-wide `megaman_direction` suggests all.
- `GauntletMaster` (`PLAYER_JUST_DIED`, `:167`): it resets the waves on a death. On any death, or only when all are dead?
- `PreciousGem.kt:211`, `PreciousGemCluster.kt:109`, `SpiderWeb.kt:130`: they destroy themselves on `PLAYER_JUST_DIED`. Only when the owner or stuck player dies?
- `MegamanDeath.kt:45-48` and `GravityBlock.kt:210`: they kill only `megaman`. They must kill each player who overlaps.
- `Rain.kt:200` (`it != megaman || megaman.ready`), `LaserBeamer.kt:244` (`megaman.spawned && megaman.ready`), `DarknessV4.kt:543-555` (one light source keyed by `megaman`): these need "for each player".
- `WilyFinalBoss.kt:294-297`: sets `canBeDamaged = false` and `facing` on the player and turns the controller off for a cutscene. Needs "all players".
- `MegaLevelScreen.kt:298`: on a room transition it keeps only projectiles that `megaman` owns. It must keep the projectiles of any player.
- Room transitions (not in the grep): who triggers them, what happens to the other players, and what `CameraManagerForRooms.focus` follows all need a policy.

### 4. Hidden single-player assumptions (a grep on `megaman` does not find them)

1. **Exact-class sets.** They look up `damager::class` or `entity::class` with `==`, not `isInstance`. A second character class silently gets 0 damage or no effect.
   - `damage/StandardDamageNegotiator.kt:50,89,128` (`CustomMapDamageNegotiator.kt:9`: `map.get(damager::class)`)
   - `UnderwaterFan.kt:83/223`
   - `SmallIceCube.kt:67/198`
   - `SwinginJoe.kt:218`, `JoeBall.kt:150`, `Snowball.kt:91`: mask sets read by `SnowballExplosion.kt:76` (`damageMask.contains(damageable::class)`)
   - `PreciousWoman.kt:506,563,908` and `PreciousTron.kt:344`: shatter sets (`PreciousGem.kt:254`)
2. **Events carry no player id.** `PLAYER_SPAWN` (`Megaman.kt:507`), `PLAYER_JUST_DIED` (`:518`), `PLAYER_SPAWN_INIT` / `PLAYER_READY` (`PlayerSpawnEventHandler.kt:113,196`), `PLAYER_DONE_DYIN` (`PlayerDeathEventHandler.kt:73`), `ADD_PLAYER_HEALTH` / `ADD_WEAPON_ENERGY` / `ADD_CURRENCY` (`HealthBulb.kt:51`, `WeaponEnergyBulb.kt:54`, `Screw.kt:50`), `ATTAIN_*_TANK`, `STUN_PLAYER` (`TimberWoman.kt:808`), `TURN_CONTROLLER_ON/OFF`.
3. **`PLAYER_SPAWN` is a level-reset signal.** 24 files outside the player listen to it (for example `AbstractEnemy`, `AbstractBoss`, `Gate`, `PushableBlock`, `Lava`, `Cart`, `RoomShaker`, `IProjectileEntity`) and destroy or reset themselves. With per-player respawn, one player's respawn resets the room for all. A separate `LEVEL_RESET` (or "all players dead") event is needed.
4. **Global input read as "the player's input".** `PipePortal.kt:153-159`, `SpringBouncer.kt:209-216`, `SpiderWeb.kt:185` and `GroundSlideBlocker.kt:44` call `game.controllerPoller`, so they read controller 0.
5. **Global game properties keyed by `Megaman.TAG`.** `"Megaman_beam"` and `"Megaman_beam_center"` are written in `PlayerSpawnEventHandler.kt:116,125,200` and `EndLevelEventHandler.kt:108,114,215`, and read in `DarknessV2.kt:365-368` and `DarknessV4.kt:547-549`. One beam slot.
6. **Tiled property keys keyed by `Megaman.TAG`.** `megaman_direction` (`Spike.kt:182`), `megaman_direction_on_receive` (`CapsuleTeleporter.kt:71`) and the room property `megaman_direction` (`MegaLevelScreen.kt:363`). Each holds one value per room or object.
7. **Single-instance screen handlers.** `PlayerStatsHandler(megaman)` (`MegaLevelScreen.kt:249`), `cameraManagerForRooms.focus = megaman` (`:266`; one `IFocusable?` at `CameraManagerForRooms.kt:63`), and `PlayerSpawnEventHandler`, `PlayerDeathEventHandler` and `EndLevelEventHandler`, which keep `private val megaman = game.megaman` (`:50`, `:36`, `:60`) and one beam sprite each. `PlayerSpawnsManager` has one `current` spawn.
8. **`GameState` is the player profile.** It holds `heartTanksCollected`, `healthTanksCollected`, `enhancementsAttained` and `currency` (`state/GameState.kt:25-29`). `Megaman` is the only registered listener (`MegamanMaverickGame.kt:396`).
9. **Special cases in shared world code.**
   - `world/body/IBodyExtensions.kt:20`: only `Megaman` bodies update during a camera rotation.
   - `world/contacts/MegaContactFilter.kt:111,115`: `GATE` and `CART` fixtures contact only `EntityType.MEGAMAN`.
   - `MegaContactRules.kt:33`: `FeetSandRule { game.megaman.ready }` is a single-player check for every feet fixture.
10. **Snapshots and per-entity flags of the one player.** `SpiderWeb.stuckToMegaman` (`:78`), `CapsuleTeleporter.megamanDirOnReceive` (`:89`), `Spike.followMegamanDir` (`:79`), `ChargedShot.megamanDirOnShoot` / `megamanFacingOnShoot` (`:79-80`; these should come from the owner), and `GravitySwitcharoo` / `GravityCapsule.triggerEntities` (they add only `megaman`). Most enemies do not cache the body; they read `megaman.body` each frame, which helps.
11. No `MegaGameEntities` lookup by the player tag or type, and no static field that stores the player apart from `game.megaman`.

### 5. Strengths and weaknesses

Strengths:

- About 65% of the lines (targeting plus proximity) read only the position and bounds. A `body` (or `bounds` and `center`) on the contract covers them.
- About 75% of the files use only the `megaman` extension. One change to the extension, for example a `nearestPlayer(from = body)` helper, moves most enemies with no change to their logic.
- About 180 lines repeat one facing idiom, which `FacingUtils.getPreferredFacingFor` already partly wraps. A shared `facePlayer()` helper can remove many of them.
- Identity checks already get the entity from the contact. Most become `is IPlayer` with no targeting policy.
- The read-only state is small: `dead`, `ready`, `direction`, `facing`, `isBehaviorActive` (3 behaviors), `damaged`, `invincible`, feet-on-ground.

Weaknesses:

- About 53 writes from outside. Spawn, death and end-level handlers write `ready`, `canBeDamaged`, `gravityOn` and `setAllBehaviorsAllowed` (`PlayerSpawnEventHandler.kt:108-111,190-193`). Enemies write `body.physics.velocity.y = MegamanValues.JUMP_VEL ...` for stomp bounces (`Goomba.kt:177`, `RedKoopa.kt:208`, `GreenKoopa.kt:210`, `BulletBill.kt:134`, both Koopa shells) and `SwimBooster.kt:58`. Other writes: `direction` (5 sites), `aButtonTask`, `facing`, `currentWeapon`, `running`, health.
- Enemies reach Mega-only fields: `feetFixture`, `headFixture`, `left/rightSideFixture`, `bodyOverGround`, `MegamanValues.JUMP_VEL` / `SWIM_VEL_Y`, `teleporting`, `charging`, `fullyCharged`, `hasEnhancement`, `isBehaviorActive(CLIMBING)`.
- Profile data (lives, tanks, weapons, enhancements) is on `Megaman` and in `GameState`, reached from `AbstractEnemy`, items and three screens.
- Exact-class `KClass` sets fail silently.
- Several systems have one slot by design: the camera focus, the stats handler, the spawn manager, the beam properties, and events with no player id.

### 6. Worst coupling sites

1. `entities/contracts/AbstractEnemy.kt:255`: `damager.owner == megaman || damager.owner == null`. A second player's shots would not damage any enemy. This base class covers all enemies.
2. `AbstractEnemy.kt:243` and `AbstractBoss.kt:193`: damage scaling reads `megaman.hasEnhancement`, not the owner's.
3. `damage/StandardDamageNegotiator.kt:50,89,128` with `CustomMapDamageNegotiator.kt:9`: an exact-class lookup, so a new character class deals 0 contact damage.
4. The `PLAYER_SPAWN` level reset in 24 listeners: per-player respawn would reset the room.
5. `screens/levels/events/PlayerSpawnEventHandler.kt:50,108-125,190-200`: one player, direct writes to internals, global beam properties.
6. `MegaLevelScreen.kt:249,266,298,363-379,593,622,644,803,829`: one stats handler, one focus, owner-filtered projectile cleanup, per-room gravity, lives.
7. `AbstractBoss.kt:130,159`: the damage listener is on one player and typed `IMegamanDamageListener` / `onMegamanDamaged(.., Megaman)`.
8. `world/body/Receivers.kt:42`, `ReceiverRules.kt:110` (`as Megaman`), `ItemEntity.kt:7`, `PlayerItemRule.kt:19`: public signatures typed `Megaman`, already with `TODO`s for `029`.
9. The stomp bounces in 6 enemies (`Goomba.kt:177`, `RedKoopa.kt:208`, ...): they write the player's velocity with `MegamanValues.JUMP_VEL`. They need a `bounce()` capability on the player.
10. `GravitySwitcharoo.kt:156-193` and `GravityCapsule.kt:146-184`: they track only `megaman`, write `direction`, and set the Mega-only `aButtonTask` from `bodyOverGround`.
11. `world/body/IBodyExtensions.kt:20` and `MegaContactFilter.kt:111,115`: special cases on the type `Megaman` / `EntityType.MEGAMAN`.
12. `PipePortal.kt:149-159`, `SpringBouncer.kt:208-216`, `SpiderWeb.kt:185`, `GroundSlideBlocker.kt:44`: they read controller 0 for "the player".
13. `screens/levels/stats/PlayerStatsHandler.kt` (31 lines) and `LevelPauseScreen.kt` (16 lines): they read and write profile data, weapons and health on the one player.
14. `MegamanDeath.kt:45-48` and `GravityBlock.kt:210`: kill zones that check only `megaman`.
15. `FloorButton.kt:168-171` and `InfernoChainPlatform.kt:123`: they reach Mega's named fixtures and the raw `body.fixtures`. The contract must expose fixtures by role, or these entities must use contacts.

Guess, not verified: a contract of `body`, `facing`, `direction`, `dead`/`ready`, a few `isDoing` flags, faction, plus `bounce()`, `setGravityDirection()` and lock reasons covers about 90% of the external uses. The hard part is the policies (target choice, resets, camera, profile) and the missing player id in events, not the size of the interface.

## Review 3: the systems around the player (2026-10-23)

Research agent report at `bacbd22c4`. Its angle is the game-level systems: the game object, the level screen and its handlers, the camera, spawning and culling, the HUD, input, events and the engine. The agent changed no files. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` unless they start with `engine/`. A line with `~` is close but not exact.

### 1. How each system finds and uses the player today

**Game object.** `MegamanMaverickGame.megaman` is a `lateinit` field (`MegamanMaverickGame.kt:207`). `postCreate` makes it once, calls `init()` by hand and adds it as the only player listener on `GameState` (`:391-396`). Player progress is in three places:

- `GameState`: levels beaten, heart tanks, health tanks, enhancements, currency, difficulty (`state/GameState.kt:25-30`). It has no player key.
- Lives are on the entity: `Megaman.lives` (`Megaman.kt:175`, `:998-1000`).
- Weapons are on the entity (`putWeapon`, `clearWeapons`), plus a `WEAPONS_ATTAINED` property-bag entry that `Megaman.onAddLevelDefeated` writes (`Megaman.kt:917-919`).

`resetPlayerProgress`, `applyLevelOverride` and `loadSavedState` change the entity directly (`MegamanMaverickGame.kt:~758-780`, `:~818-822`). The save file holds only `GameState.toString()` (`:~784-791`).

**Level screen.** `MegaLevelScreen` has `val megaman get() = game.megaman` (`MegaLevelScreen.kt:152`). Uses:

- spawn: `megaman.spawn(playerSpawnsMan.currentSpawnProps)` (`:593`)
- lives: `removeOneLife` (`:622`); `isAtMinLives` leads to game over (`:642-645`)
- pause: only if not `dead` and `ready` (`:913`); forced resume on death (`:919`)
- boss flow: early return if `megaman.dead` (`:788`, `:817`); `canBeDamaged` false, then true (`:803`, `:829`)
- room transition end: the room property `megaman_direction` sets the gravity direction (`:362-376`); `running = false` (`:379`)
- projectile cleanup on transition: `owner != megaman` (`:298`)
- focus snap check: `megaman.getFocusPosition()` (`:970`)
- death overlay: `if (megaman.dead)` (`:948`, `:1044`)

**Handlers.** Each handler copies `game.megaman` into a field when it is built:

- `PlayerSpawnEventHandler.kt:50`: sets `ready`, `canBeDamaged`, gravity and `setAllBehaviorsAllowed` (`:108-111`, `:190-193`).
- `PlayerDeathEventHandler.kt:36`, `:49-50`.
- `EndLevelEventHandler.kt:60`: the beam-out uses `direction`, `currentWeapon` and the facing (`:104-105`, `:156-167`).
- `PlayerStatsHandler(megaman)` (`MegaLevelScreen.kt:249`; `PlayerStatsHandler.kt:29`).

**Camera.** `CameraManagerForRooms.focus: IFocusable?` holds one focus (`CameraManagerForRooms.kt:63`), set to `megaman` (`MegaLevelScreen.kt:266`). The focus drives:

- room detection: `nextGameRoom` checks if the focus center is in a room (`:284-296`)
- room exit and transition start: the focus bounds overlap the next room (`:197-238`)
- transition targets: `focusStart` (`:149-176`)
- follow with clamp: `setCameraToFocusable` (`:303-315`)

`IFocusable` is in `core` (`:319-324`). `Megaman.getFocusPosition` has a `ConstKeys.FOCUS` property override (`Megaman.kt:901-915`). `CameraShaker` works on the one camera.

**Checkpoints.** `PlayerSpawnsManager` activates the next checkpoint when the camera overlaps the spawn rectangle (`PlayerSpawnsManager.kt:67-69`). It does not use the player body. There is one `current` spawn for the level.

**Spawning and culling** use the game camera, not the player:

- `SpawnerFactory.spawnerForWhenInCamera` with `camera.getRotatedBounds()` (`spawns/SpawnerFactory.kt:13-28`; used at `SpawnersLayerBuilder.kt:178-183`).
- `getGameCameraCullingLogic` and `getUpdateWhenNearCameraPredicate` (`entities/utils/GameEntityUtils.kt:56-71`). About 129 call sites use the culling helpers. 61 files call `getGameCamera()`.
- The spawner gate is `predBasedOnRoomTrans` (`SpawnersLayerBuilder.kt:60-63`), a global room-transition flag.
- One exception uses the player: `Gate` turns its collision on only near `megaman` (`entities/sensors/Gate.kt:342-349`). The code comment says "ONLY Mega Man can ever make contact with a Gate".

**HUD and pause.**

- `PlayerStatsHandler` draws one health bar and one weapon bar at fixed positions (`HEALTH_BAR_X`, `WEAPON_BAR_X`, `STATS_BAR_Y`; `PlayerStatsHandler.kt:~63-94`). They read `megaman.getHealthPoints()`, `ammo` and `currentWeapon`.
- `LevelPauseScreen` reads `megaman.lives`, weapons, ammo and health tanks, and sets `currentWeapon` (`LevelPauseScreen.kt:229`, `:265-290`, `:553`, `:569-585`).
- `GetWeaponScreen.kt:157`, `:199` and `MainMenuScreen.kt:135` also use `game.megaman`.

**Input.**

- One `controllerPoller` for the game (`MegamanMaverickGame.kt:196-197`, `:280-281`), polled once per frame (`:515`).
- `ControllerSystem(controllerPoller)` gives the same poller to every `ControllerComponent` (`MegamanMaverickGame.kt:859`; `engine/.../controller/ControllerSystem.kt:10-37`).
- `ControllerUtils` uses index 0 (`engine/.../controller/ControllerUtils.kt:11`, `:23`). `MegaControllerPoller` binds only the first pad (`controllers/MegaControllerPoller.kt:22-24`).
- `ControllerButton.isPressed` is keyboard OR pad OR alternates (`engine/.../buttons/ControllerButton.kt:16-21`). All devices merge into one player.
- Menus, pause and the player read the same poller (`MegaLevelScreen.kt:900-901`).
- `TURN_CONTROLLER_ON` and `TURN_CONTROLLER_OFF` set `controllerPoller.on` and `ControllerSystem.on` for everyone (`MegamanMaverickGame.kt:624-640`).

**Events.**

- `PLAYER_SPAWN_INIT`, `PLAYER_SPAWN`, `PLAYER_READY`, `PLAYER_JUST_DIED`, `PLAYER_DONE_DYIN`, `ADD_PLAYER_HEALTH`, `ADD_WEAPON_ENERGY` and `STUN_PLAYER` (`events/EventType.kt:8-14`, `:48`) carry no player identity.
- 25 files use `PLAYER_SPAWN`, mostly as a level-reset signal. `BEGIN_ROOM_TRANS` and `END_ROOM_TRANS` have about 35 users each.
- `Megaman` listens to the room transition events, `STUN_PLAYER` and `GATE_INIT_OPENING` (`Megaman.kt:163-170`).
- `ADD_PLAYER_HEALTH` and `ADD_WEAPON_ENERGY` come from `HealthBulb`, `WeaponEnergyBulb` and others, with no target.

**Engine.** `engine/` has no player concept. Its single-player items are index 0 in `ControllerUtils` and one poller in `ControllerSystem`. `TiledMapLevelRenderer.render(camera)` is per camera, which is fine. A determinism problem: `UtilMethods.random` is seeded from the clock (`engine/.../common/UtilMethods.kt:22`), and so is `superRandom` (`ArrayExtensions.kt:112`).

### 2. What must change for each system

| System | Shared screen (2+ players) | Split screen | Online |
|---|---|---|---|
| Player ownership | `game.players` (list of slots); spawn and destroy each player | no change | slot ids stable and the same on all peers |
| Progress | `PlayerProfile` per slot or shared, behind `LivesPolicy` (decision H); lives move off the entity | no change | profiles are part of the synced state |
| Spawn, death, respawn | handlers work on one player each. Today one death causes "music off, life lost, fade, level respawn" (`MegaLevelScreen.kt:616-650`). This becomes per player, with a level policy for "all dead" | fade per viewport | respawn timing on the sim tick, not a UI timer |
| Camera | group focus: an `IFocusable` from the bounds of the living players, or a leader rule. Room detection must not use one focus center | one `CameraManagerForRooms` per view; rooms can differ per view | camera state is a pure function of the sim, or kept outside it |
| Room transition | policy: who triggers it and what happens to players outside it. `Gate` assumes one trigger (`Gate.kt:342-349`) | rooms per view, or a leash rule | transition start is a sim event |
| Checkpoints | camera overlap still works with one camera | union of cameras, or "first camera that sees it" | fine if cameras are in the sim |
| Spawn and cull | the single camera is fine | all camera spawn and cull helpers must test a set of views ("visible in any view"). 129 cull sites and 61 `getGameCamera()` files call it directly. Recommendation: put a `ViewSet` service behind the helpers now | culling deterministic; spawn timers use the sim delta |
| HUD | bars per slot; layout policy | HUD per viewport | none |
| Pause | who can pause; who drives the menu; tanks and weapon choice per player | same | pause is a global session event, or not allowed |
| Input | `InputFrame` per slot (`031`); router from devices to slots; menus read `UiInput`; a lock per player replaces global `TURN_CONTROLLER_*` | no change | frames are the lockstep payload; no direct `Gdx.input` in entities |
| Freezes | `PlayerStatsHandler.addHealth` turns off all systems except sprites (`:~299`), and so do the boss intro (`MegaLevelScreen.kt:~727-760`) and gates (`:~680-700`). With two players both freeze, which is acceptable | same | freezes are sim state, not UI timers |

Choices now that would block split screen or online:

- More global singletons, such as `game.camera` or `cameraManager.focus = players.primary`.
- `getGameCamera()` as the culling source in new code.
- Event payloads without a player id.
- System freezes from UI handlers.
- `Gdx.graphics.deltaTime` in the camera interpolation (`MegaLevelScreen.kt:262`).
- Clock-seeded RNG.
- Lives on the entity.
- One poller shared by all entities.

### 3. Policy questions for the owner

1. **Camera rule.** Group bounds with no zoom and a leash, or a leader with the others teleported to it? Does the camera stay clamped to one room?
2. **Off-screen rule.** A player who leaves the view: blocked at the edge, teleported, or damaged? (Mega Man games have no zoom.)
3. **Room transition.** Triggered by any player, by all players at the edge, or by the leader? The others: warp, or wait?
4. **Death.** One death while others live: bubble, respawn at the leader, or wait? Is "all dead" the only trigger for the level respawn and `PLAYER_DONE_DYIN`? Does the music still stop at the first death (`:619`)?
5. **Lives.** `LivesPolicy` is decided. Still open: the game-over condition with per-player lives, and the maximum number of players (2 or 4).
6. **Checkpoints.** Level-wide (today) or per player?
7. **Boss intro and defeat.** Lock all players? Today `megaman.dead` (`:788`, `:817`) cancels the victory; with two players this becomes "all dead". Does the end-level beam-out beam out all players?
8. **Pause.** Can any player pause? Who controls the menu? Are the health tank and weapon screens per player?
9. **HUD layout.** Bars side by side, one corner per player, or a compact co-op bar?
10. **Pickups.** Do `ADD_PLAYER_HEALTH` and `ADD_WEAPON_ENERGY` go to the player who picks up, or to all? Today they carry no target.
11. **Gravity rooms.** Does `megaman_direction` apply to all players?
12. **Friendly fire and pushing.** This is `030`.

### 4. Strengths to keep

- Spawning and culling depend on the camera, not on the player. A shared screen is easy; a view abstraction gives split screen.
- `IFocusable` already decouples the camera from `Megaman`. A group focus can implement it.
- Checkpoint activation uses the camera, so with one camera no player choice is needed.
- Handlers are separate classes with `init`, `update` and `finished`. They are easy to make per player.
- `GameState` is already separate from the entity, has a listener pattern and is saved as text.
- `engine/` has almost no player assumptions.
- Room transitions are events with full props, a good seam for a policy layer.

### 5. Weaknesses, ranked by how hard they block co-op

1. **Global input.** One poller, pad index 0, all devices merged, one `ControllerSystem` poller, menus on the same poller, global `TURN_CONTROLLER_*`. Co-op is not possible without `031`.
2. **One camera focus** drives room detection and transitions (`CameraManagerForRooms.kt:180-238`, `:284-296`).
3. **Level flow assumes one player** on death, respawn, boss defeat, pause and end level (`MegaLevelScreen.kt:572-829`, `:913-919`). The handlers capture `game.megaman` when they are built.
4. **Lives and weapons on the entity.** Lives, the weapon set and `WEAPONS_ATTAINED` are on the entity or in the property bag. Progress resets write to the entity.
5. **Player events carry no identity.** Pickups have no target.
6. **HUD and pause screen** are bound to one `Megaman` with fixed positions.
7. `Gate` and the projectile cleanup on transition (`owner != megaman`) assume one player. Mostly covered by `030`.
8. **Determinism.** Clock-seeded RNG, `Gdx.graphics.deltaTime`, UI timers that freeze systems. These block only online.
9. The handler order in `MegaLevelScreen.render` (`:~940-946`) is fragile. Per-player handlers make it harder.

### 6. Where each concern belongs

- **`IPlayer` contract:** slot; body and fixtures; facing; direction; `dead` and `ready`; health; `IFocusable`; `lock(reason)` and `unlock(reason)` in place of the handler writes to `canBeDamaged`, gravity and `setAllBehaviorsAllowed`; `beamIn` and `beamOut` hooks (the beam-out sprite comes from the character); `loadout`; `input`; `onGravityChanged`. Not lives, tanks or the progress reset. (Review 4 recommends that `input` and `loadout` stay off the core contract.)
- **Players service** (`game.players`, later through `LevelContext` from `016`): `all`, `alive`, `byIndex`, `nearestTo`, `anyAlive`, `allDead`; spawn and despawn of slots; a group `IFocusable`.
- **`PlayerProfile`** (per slot, or shared through `LivesPolicy`): lives, heart tanks, health tanks, enhancements, unlocked weapons, maybe currency. `GameState` keeps the world progress (levels beaten, difficulty). The save format changes here.
- **Input router (`031`):** device-to-slot assignment, `UiInput` for menus, locks per slot.
- **Level-flow policy objects** (in `MegaLevelScreen` or `LevelContext`): `CameraPolicy`, `OffscreenPolicy`, `RoomTransitionPolicy`, `RespawnPolicy` and `GameOverPolicy` (with `LivesPolicy`), `CheckpointPolicy`, `BossFlowPolicy`, `PausePolicy`, `HudLayout`, `PickupPolicy`. Each starts with a single-player implementation that copies today's behavior exactly.
- **Views service** (before split screen): wrap `getGameCamera()` in the culling and spawn helpers, so that one later change makes it "any view".

## Review 4: the design options for `IPlayer` (2026-10-23)

Research agent report at `bacbd22c4`. Its angle is the design options, not the code inventory. The agent changed no files. Recommendations are the agent's own, not decisions.

### Documentation problems to fix first

- The 2026-10-22 review text above uses the old review numbers. For example, "Faction (item 29)" is now `030`, "input (item 30)" is `031`, "lock reasons (item 11)" is `012`, "replay (item 5)" is `055`, and "`LevelContext` (item 15)" is `016`.
- `ITEM.md` says "Depends on: none", but the proof plan ("replay-proven") needs `055`, which is far away (after `010`, `011`, `006`, `031`). Recommendation: do not wait for replay. Prove `029` with the contract play-through and unit tests of the players service.

### 1. Critique of the `IPlayer` sketch

The sketch tries to be both the external contract and the character implementation. It is not lightweight.

Too much:

- Its supertypes are `IMegaGameEntity` (which carries `game: MegamanMaverickGame`), `IHealthEntity` (about 15 mutators), `IDamageable` and `IFocusable`. A caller that needs only a position also sees every setter, for example `setHealth` and `depleteHealth`.
- `character: CharacterDef` exposes the implementation. A caller that reads the def can branch on it, and that is `is Megaman` under a new name.
- `input: InputFrame` must not be public on the player. Input belongs to the slot (`031` routes sources to slots). Only the player's own behaviors read it. If enemies or blocks can read a player's input, the `aButtonTask` kind of coupling comes back.
- `loadout` is used by the HUD, the pause screen, the end-level handler and `GetWeaponScreen`. These are screens, not world entities. Recommendation: a separate interface (`IHasLoadout`), not in the core contract.
- `onGravityChanged(dir)` is a symptom. `GravitySwitcharoo` and `GravityCapsule` write `aButtonTask`. Model this as a capability, or as a direction change through `IDirectional`, not as a member of every player.
- `feet`, `head`, `side(left)`: 29 external uses, most of them in contact rules. `014` moved those checks to receivers. Check what remains after `014d`; a receiver is probably enough.

Missing:

- One lifecycle value instead of `dead` plus `ready` plus `canBeDamaged`, for example `enum PlayerPhase { SPAWNING, ACTIVE, DYING, DEAD, EXITED }`. Co-op needs a state "not in the world, but the slot is still in the game" (a player who waits to respawn).
- A slot id that is not a bare `Int` index (see section 3).
- A stable way to attribute damage and kills (an `ownerSlot` on projectiles; `030` already plans `ownerPlayerIndex`).
- Locks with reasons (`lock(reason)`) are correct. Tie them to `012` and type them (`LockReason`, not `Any`). Two handlers can then lock the same player and unlock in any order. This is a real co-op need (spawn handler plus boss intro).

Who outside the player needs what: about 90% of the uses are position, bounds, direction and facing, alive or active state, and `isDoing(...)`. The core contract is small. The rest (profile about 20 uses, loadout about 19, Mega-only about 6) has 3 or 4 consumer groups. Each group should get its own type.

### 2. Recommended layered shape

```kotlin
// engine: genre level, no Megaman Maverick content
@JvmInline value class PlayerSlot(val value: Int)

interface ITargetable {
    val targetCenter: Vector2
    val targetBounds: GameRectangle
    val isTargetable: Boolean
}

interface IPlayerView : ITargetable, IReadFaceable, IReadDirectional {
    val slot: PlayerSlot
    val phase: PlayerPhase
    fun isDoing(action: PlayerAction): Boolean
}

interface IPlayer : IPlayerView, IBodyEntity {
    fun lock(reason: LockReason); fun unlock(reason: LockReason)
    fun <C : Any> capability(type: KClass<C>): C?
}

// core: optional capabilities
interface IHasLoadout { val loadout: ILoadout }
interface IGravityRemappable { var gravityJumpRemap: GravityRemap? }
interface IHealthOwner { val health: IHealthView }
```

- Enemies depend on `ITargetable` or `IPlayerView`, not on `IPlayer`. They get a target from the service (`targets.select(self, policy)`) and keep only the view. An enemy then cannot move or damage a player directly. The 524 `megaman.body.*` uses are mostly `getX`, `getCenter` and `getBounds`, so `targetCenter` and `targetBounds` cover most of them.
- The engine `IFaceable` and `IDirectional` have only `var`. Recommendation: add read-only interfaces (`IReadFaceable`, `IReadDirectional`) and let the current ones extend them. Small and additive.
- `isDoing(action)` is for short states (CLIMBING, WALL_SLIDING, JETPACKING). `capability(type)` is for features a character has or does not have (gravity remap, loadout, charge). Both tell the caller when the feature is absent, so callers never test the concrete type.
- `PlayerAction` must be an open set, so a new character can add actions. A core enum is fine for now; a sealed interface with core and per-character objects scales better.
- This matches the `014` receiver style. `HIT_BY_PLAYER` takes `Megaman` today (the `TODO` in `ReceiverRules.kt:108` points at `029`). Change it to `(player: IPlayerView) -> Unit`. `SmallIceCube` reads `canBeDamaged`; that becomes `phase`, a capability, or a typed lock query.

### 3. Co-op correctness

Identity: use `PlayerSlot`, not the entity reference and not `Megaman.id`. Kill credit, profiles, input routing, the HUD and online replication use the slot. The entity can change (character swap, re-creation); the slot stays.

Players service (recommended):

```kotlin
interface IPlayers {
    val slots: List<PlayerSlot>
    fun get(slot: PlayerSlot): IPlayer?
    fun active(): Sequence<IPlayer>
    fun nearestTo(p: Vector2): IPlayerView?
    fun anyActive(): Boolean
}
interface ITargetPolicy { fun select(self: IBodyEntity, players: IPlayers): ITargetable? }
// NEAREST (default), LOCKED_ON_SPAWN, LAST_ATTACKER, ROUND_ROBIN(rng stream)
```

`slots` and `active()` are in ascending slot order. `nearestTo` gives ties to the lower slot.

Traps:

- **`primary` becomes the new global.** If `players.primary` exists, the 889 call sites migrate to it and nothing improves. Recommendation: no public `primary`. Code that needs the main player (save, title screen) asks for `PlayerSlot(0)` by name, so a grep finds these sites.
- **Retargeting each frame.** An enemy that calls `nearestTo` every frame can flip between two players at the midpoint, and its state machine behaves in new ways. Recommendation: an enemy picks a target at a defined point (spawn, state entry, attack start) and keeps it until the target is not targetable. With one player this is the same as today.
- **Determinism (online lockstep).** Iterate in slot order. Never iterate an `ObjectSet` or `ObjectMap` of players (hash order is not stable across machines). Distance ties go to the lower slot. Random targeting uses a named `011` stream.
- **Stale references.** An enemy that keeps an `IPlayer` after the player dies, or after the entity is reused, points at a reused object. `PreciousGemCluster` (`owner == megaman` plus `PLAYER_JUST_DIED`) shows this risk. Recommendation: keep `PlayerHandle(slot, generation)` and resolve it through the service on each use. A different generation means the handle is stale.
- **Pooled players.** `megaman` is made once and not pooled. Keep that: one entity per slot, owned by the service, never from `MegaEntityFactory`.
- **Per-player events.** `PLAYER_SPAWN`, `PLAYER_JUST_DIED`, `PLAYER_READY` and `STUN_PLAYER` carry no slot. Add `ConstKeys.SLOT` to the props now (always 0 with one player). Then `SpiderWeb` can destroy itself only when its own target dies. `GauntletMaster` must choose "any player died" or "all players died"; this is a behavior decision.
- **Attribution.** `AbstractEnemy` reads `megaman.lives` for the drop chance (`:96`) and gives `ONE_UP` to `megaman` (`:329`). In co-op these go to the killer's slot (`030` `ownerPlayerIndex`) and to the `LivesPolicy`. Item pickups go by contact, so they are already correct.
- **Profile against shared pool.** Decision H gives `LivesPolicy { SharedPool, PerPlayer }`. Lives are not the only such data: health tanks, heart tanks, enhancements, weapons owned and bosses beaten each need the same choice. X6 shares the beaten-boss byte between X and Zero (`mmx-demake-tools/docs/players/zero.md`, "Technique bits"). Recommendation: one policy per field group, and a profile store keyed by slot that can point two slots at the same record.
- **Locks and level handlers.** `PlayerSpawnEventHandler`, `EndLevelEventHandler`, `MegaLevelScreen:803` and `WilyFinalBoss:294` set `canBeDamaged` and behaviors on the single player. They must loop over `active()`, each with one `LockReason`.

### 4. Engine or core

| Concept | Where | Reason |
|---|---|---|
| `PlayerSlot`, `PlayerHandle`, `PlayerPhase` | engine | every Mega Man-like game has player slots |
| `ITargetable`, `ITargetPolicy`, nearest-target helper | engine | genre targeting |
| `IPlayers` interface and a default implementation | engine | pairs with `001b` services and `016` `LevelContext` |
| `IReadFaceable`, `IReadDirectional` | engine | small additions next to the current interfaces |
| `LockReason` | engine type, core values | same pattern as `012` |
| `PlayerAction` base | engine (sealed interface); core defines CLIMBING and so on | genre verbs, content values |
| `IPlayer`, `CharacterDef`, `MovementValues`, ability factories | core first; maybe the engine later | they depend on `Faction`, `FixtureType` and the behaviors |
| `ILoadout`, `WeaponDef`, profile, `LivesPolicy` | core | game rules and save format |

Recommendation: do not put `IPlayer` in the engine in `029a`. It touches `FixtureType` and `MegaGameEntity`. Move it later, after `016` and `001b` give the engine a services seam.

### 5. Customization per character

| Approach | Strengths | Risks |
|---|---|---|
| A. Subclass `Megaman` | cheap; Proto Man works at once | Proto inherits 1045 lines and 86 constants; a change to Mega can break Proto |
| B. `AbstractPlayer` plus `CharacterDef` | X6-like: shared core, data values, hooks; ordered abilities keep the evaluation order | the base class can become a new deep hierarchy; the ability order is easy to break without a test |
| C. Full data | most flexible | big-bang; hard to keep behavior the same |

X6 facts that support B (inspiration only):

- `map.md`, "Code layout": the 32 steps are shared; per-character hooks run under a test of `mega+0x02`. The stand step is an ordered chain of try calls, and the first that succeeds ends the update. This is the "ordered abilities" model.
- `movement.md`, "Physics constants": base values are written at beam-in, then part bonuses are added.
- `steps-extra.md`, "What each form changes": a form changes data (wall slide speed 0, spike immunity, hurt boxes, energy cost) more often than code.

Recommendation: compose, do not inherit.

```kotlin
class CharacterDef(
    val id: CharacterId,
    val movement: MovementValues,
    val modifiers: List<MovementModifier> = emptyList(),
    val abilities: List<AbilityFactory>,
    val body: PlayerBodyDef,
    val animations: AnimationSetDef,
    val loadout: LoadoutDef?,
    val damage: DamageReactionDef,
)
```

- Keep `AbstractPlayer` thin: phase, locks, slot, focus, health flow, spawn and room events. If it grows past about 300 lines, move parts into components.
- Apply `MovementValues.forDirection(dir)` once in the direction setter, in place of the sign copy at `Megaman.kt:239-269`. It must give exactly the current values.
- Risk: cross-ability couplings (NEEDLE_SPIN or RUSH_JET checks inside behaviors) break the "independent abilities" idea. Move them to modifiers or weapon-driven ability blocks (`024`) before Proto Man exists, or Proto copies them.
- Ladder and slide (open question 3): recommendation: shared, as in X6. Characters change values and the ability set, not the ladder code.

### 6. Migration path

- **`029a` seam.** Add `PlayerSlot`, `PlayerPhase`, `ITargetable`, `IPlayerView`, `IPlayer` and the read-only engine interfaces. Add `game.players` with one slot that wraps `megaman`. `Megaman : IPlayer`. Add the slot to the player events (always 0). `HIT_BY_PLAYER` takes `IPlayerView`. Mark `IMegaGameEntity.megaman` `@Deprecated`. No new behavior.
- **`029b` call sites by intent**, in batches: (1) enemies and bosses: target selection and `ITargetable`; (2) hazards, blocks, special: `IPlayerView` and `isDoing`; (3) screens and handlers: locks with reasons, loops over `active()`; (4) `aButtonTask` to `IGravityRemappable`.
- **`029c` profile.** Lives, tanks, enhancements to `PlayerProfile` keyed by slot, with only `LivesPolicy.SharedPool` used. Coordinate with `007` (save format).
- **`029d` `CharacterDef` and `AbstractPlayer`.** `MegamanDef` with the exact `MegamanValues` and the current ability order (`BehaviorsComponent.kt:778-785`). A unit test fixes the ability order and the direction table.
- **`029e` (optional)** a Proto Man prototype behind a dev flag.

Hand to other items: `030` faction and `ownerSlot`; `031` `InputFrame` per slot (in `029` the player reads the global poller through one private adapter); `024` `ILoadout` and `WeaponDef`; `012` `LockReason`; `016` `LevelContext.players`; `047` camera group bounds, off-screen rule, second slot spawn; `037`/`038` respawn per slot; `055` replay proof later.

### 7. Decisions for the owner

1. Maximum players: 2 or 4.
2. Target policy: keep the target until it dies, or retarget at state changes? Is "nearest" the default? Are `LAST_ATTACKER` and `RANDOM` needed now?
3. No public `primary`; slot 0 by name only where needed?
4. Events: "any player died" or "all players died" for `GauntletMaster`, the level reset and the projectile cleanup?
5. Shared or per-slot data, field by field: lives, tanks, enhancements, weapons owned, bosses beaten.
6. Where `IPlayer` lives: core now, engine later? Or only the slot, service and targeting types in the engine at once?
7. Do all characters share the ladder and slide code?
8. Proof: play-through plus unit tests, given that replay (`055`) is far away?
9. Proto Man: in `029`, or a separate feature item?

## Review 5: how games and engines usually set this up (2026-10-23)

Research agent report, from web research and training knowledge. **[web]** means a fetched source supports the point (see "Sources"). **[training]** means it comes from the agent's own knowledge and was not checked on the web.

### 1. Engine patterns: player identity and the controlled body

**Unreal** (the best-documented split):

- **[web]** `GameMode` sets the rules of a session and creates the other framework classes. It exists only on the server and only for the current level. `GameState` holds shared match data (scores, the list of players) and replicates to all clients.
- **[web]** `PlayerController` handles input from one human player and possesses a `Pawn`, "the physical manifestation of the player in the game world". `Character` is a `Pawn` with a capsule and a movement component.
- **[web]** `PlayerState` holds the data of one player and replicates to all clients.
- **[web]** "The PlayerController persists throughout the game, while the Pawn can be transient... you may die and respawn, so you would get a new Pawn but your PlayerController would be the same." A score on the `Pawn` resets on respawn; a score on the controller does not.
- **[web]** For simple cases, input can go in the `Pawn`. For complex cases, the controller "decides what to do and then issues commands to the Pawn".
- **[web]** Split screen: `UGameInstance::CreateLocalPlayer(controllerId)` adds a `ULocalPlayer`. The `GameInstance` keeps a `LocalPlayers` list. `MaxSplitscreenPlayers` sets the maximum number of local players.
- **[web]** Damage carries attribution: `ApplyDamage` and `TakeDamage` take an `EventInstigator` (the controller responsible, for example the player who threw a grenade) and a `DamageCauser` (the actor that hit, the grenade).
- **[training]** AI uses the same `Pawn` through an `AIController`, so "controlled by a human" is not a property of the body.

**Unity Input System:**

- **[web]** "The Player Input component represents a single player." Each `PlayerInput` maps to one `InputUser`, with its own devices.
- **[web]** `PlayerInputManager` "automatically manages the creation and lifetime of PlayerInput instances as players join and leave". Join modes: a button press on an unpaired device, a specific action, or manual. It sends `PlayerJoined` and `PlayerLeft`.
- **[web]** With split screen on, the manager "automatically splits the available screen space between the active players". Each player prefab has its own `Camera`. A `MultiplayerEventSystem` lets each player's UI take input from that player only.
- **[training]** `PlayerInput.playerIndex` is a stable index from 0.

**Godot:**

- **[web]** The input map has no per-player concept. The usual workaround is one set of actions per player (`jump_p1`, `jump_p2`), or a router that maps `device_id` to `player_id`.
- **[web]** Networking: each peer has a unique id; the server is always `1`. Authority is "the server by default, but can be changed per-node" with `set_multiplayer_authority`.
- **[training]** A common convention names each player node after its peer id; the node checks `is_multiplayer_authority()` before it reads local input.

**2D frameworks.** **[training]** MonoGame, LibGDX and Celeste-style code have no player framework. The game makes its own player list. Celeste has one player and keeps all player logic in one `Player` class.

**The general separation:**

| Layer | Lifetime | Holds | Unreal / Unity / Godot |
|---|---|---|---|
| Slot or local user | join to leave | index, device pairing, viewport and camera | `LocalPlayer` / `PlayerInput` + `InputUser` / device-to-player map |
| Controller | session | turns input into commands; possesses a body | `PlayerController` / `PlayerInput` callbacks / authority node |
| Persistent per-player state | session (survives death) | lives, score, chosen character, loadout | `PlayerState` / custom component / custom node |
| Body | spawn to death | physics, health, abilities | `Pawn`/`Character` / prefab / `CharacterBody2D` |
| Match or session state | level or session | player list, rules, shared pools | `GameMode` + `GameState` / a manager / an autoload |

### 2. How enemies address "the player" in co-op

Target selection:

- **[web]** Terraria: "Most enemies prioritize to attack the player they are closest to". The distance is taxicab distance between hitbox centers, minus each player's `aggro` stat. If only one player is alive, every enemy targets that player.
- **[web]** A weighted score (distance, recent damage, visibility, designer bias), with a "stickiness" bonus of about 10-20% for the current target. Without it, enemies flip between targets. Threat from damage decays over about 8-12 seconds. Score again on events (hit, target dead or lost, new hostile) plus a slow timer, not on every tick.
- **[web]** A real bug from a co-op devlog: enemies always targeted the host because the AI used the default "get player controller" lookup. The fix: loop over the player list, pick the nearest, and measure from the body, not from the controller.
- **[web]** An MMO-style rule (anecdotal): switch target only when another player's threat is about 2x the current target's.
- **[training]** Other common rules: random choice at spawn, then keep it; round-robin for bosses with alternating attacks; last attacker; lowest health. Turret-style enemies often pick the nearest player on each attack cycle.

Co-op game examples:

- **NSMB Wii [web].** One shared camera that pans and zooms with the distance between players, up to a limit. A player who falls too far behind loses a life. A dead player comes back in a bubble and cannot be hurt in it; a free player pops it by touch or with a projectile. Players can enter a bubble on purpose. If all players are dead or in bubbles at once, the level restarts from the start or the midway flag. In NSMB 2, a player who falls behind goes into a bubble and does not die.
- **Rayman Legends [web].** Shared screen. The camera can move past players who are behind, which kills them. A player who is hit or too far behind becomes a balloon; a teammate's touch rescues them. If all are balloons, the group restarts at the checkpoint.
- **Cuphead [web].** A dead player's soul rises. The other player parries it before it leaves the screen, and the dead player comes back with 1 HP.
- **Spelunky 2 [web].** Local play has no split screen. The camera follows the player with a white flag; when that player dies, the flag goes to the next player. A level that starts with a dead player spawns a coffin on the main path; a live player breaks it to revive them. Reviewers call the shared leader camera the worst part of co-op.
- **Shovel Knight [web].** Gold is shared. When a player dies and respawns, the remaining health is split between the two players. Bosses have more health in co-op. Stats are compared at the end of each stage. **[training]** Camera rule not confirmed: the agent's memory is that the screen does not scroll away from a player, and players at the edge are pulled along at screen transitions.
- **Mega Man [web].** Mega Man 10 prototypes had an unused co-op assist mode (Mega Man and Proto Man) with "Sync Up" obstacles that both players had to break together. Nothing documented on its camera or screen transitions. Mega Man 2.5D (a fan game) has local co-op with Proto Man as player 2.
- **[training]** Contra and Broforce: shared screen; a player who falls behind is pulled along or dies. Each player has their own lives; some versions let a player take a life from the partner. In fixed-screen rooms, the usual solution is to move every player to the transition point when one player triggers it. Players who are not there are teleported or "bubbled".

Common theme: no game in this list has a "primary player" in its gameplay rules. Where a leader exists, it is a camera choice only (Spelunky's flag), and it moves to another player when the leader dies. Reviewers criticize this design.

### 3. Character customization

- **[web] Celeste.** One `Player.cs` of about 5,400 lines. Tuning is a block of constants (`MaxRun = 90f`, `RunAccel = 1000f`, `Gravity = 900f`, `JumpSpeed = -105f`, `VarJumpTime = .2f`, `JumpGraceTime = 0.1f`, `DashSpeed = 240f`, `DashTime = .15f`). States are integer ids (`StNormal = 0`, `StClimb = 1`, `StDash = 2`, ... `StAttract = 22`) in a `StateMachine` with update, begin and end callbacks per state. A good model for a movement table, but the game has one character.
- **[web] Type Object pattern.** "Define new types... completely in data." Per-type data goes in a shared type object; each instance keeps only its own state. The limit: it is "easy to use type objects to define type-specific data, but hard to define type-specific behavior". The usual fix is a reference to one of a set of behavior functions.
- **[web] Unity.** A `CharacterStats` ScriptableObject that many characters share, cloned at runtime so changes during play do not edit the asset.
- **[training] Common platformer design.** One shared motor (gravity, run, jump, collision, ladder, water) that reads a per-character stats record. Abilities are separate components or a list (slide, dash, charge shot, double jump, wall jump). The state machine has a core set of states; each ability can add states.
- **[training]** Smash fighters share one engine state machine; each fighter has an attribute table (walk speed, jump squat, gravity, fall speed, weight) and its own move scripts. Mega Man X and Zero (X4 onward) share the X physics; Zero swaps the buster for saber combos. Shovel Knight's later campaigns reuse the engine with very different movement code. Most games use composition plus data, not a deep class tree.

### 4. Interface design advice

- **[web]** "Anytime you make something accessible to every part of your program, you're asking for trouble." A service locator is "a more flexible, more configurable cousin of the Singleton", but "it's harder to understand what your dependencies are". Advice: "first consider passing the object to it instead."
- **[training] Interface segregation.** Most code that uses the player needs a small read-only view: position and bounds, facing, alive or dead, an id. Keep commands (`damage`, `knockback`, `teleport`) and per-character abilities on separate interfaces. Use capability queries (`player.get(CanSlide::class)` or a component check) in place of `is Megaman`. Unreal's `Pawn`, `Controller` and `PlayerState` split follows the same principle.
- **[training] Player registry.** A `PlayerManager` (or `GameState.PlayerArray` in Unreal) owns the ordered list, with helpers such as `nearestTo(pos)`, `alive()`, `byId(id)` and `forEach`, and sends join, leave, death and respawn events.
- **Events and attribution.** **[web]** Unreal damage carries instigator and causer. **[training]** Events about a player (death, pickup, boss defeat, checkpoint) carry a `playerId`, not an object reference, so they stay valid after the body is destroyed and can go over the network.

### 5. Readiness for split screen and online play

- **[web] Lockstep.** Send "the inputs that control that system, rather than the state". A peer "can only simulate frame n when it has the input for that frame". Determinism must hold at the bit level. Unseeded random generators, variable delta time and float differences between builds or platforms break it.
- **[web] Rollback.** The game must save and load the full state quickly and be deterministic. "Game logic must be independent from everything else in your gameplay loop": rendering, audio and the camera must not run during re-simulation. It must run several logic frames in one real frame. Making rollback "a priority early in development will greatly reduce the amount of investment required". Keep destroyed objects for a few frames, and delay dramatic events (camera moves) until they are certain. GGRS has a `SyncTestSession` that re-runs frames and compares checksums.
- **[training] Early choices that block online play later:** global singletons for the player or the camera; entities that read the keyboard or pad directly instead of a per-player input frame; iteration over a hash set or map with no fixed order; variable time steps and unseeded `Random` in gameplay; one camera mixed with gameplay rules (culling, spawns, room transitions keyed to "the camera"); a HUD that reads one global player.
- **[training] Ready designs:** an `InputFrame` per player and tick in a buffer; players in a fixed order (slot index); a fixed time step; seeded random generators in the game state; a camera or viewport per local player, with culling and spawners keyed to the union of all player views or to each player.

### 6. Summary

Common, well-supported conventions:

1. **Separate the slot or controller from the body.** The slot (index, device, viewport) and the per-player state (lives, chosen character) live longer than the body. For this game: `game.megaman` mixes slot, state and body. `IPlayer` is the body contract; a separate slot and state own lives, the character choice and input. Death or respawn replaces or resets the body, not the slot.
2. **A player registry, not a global.** An ordered list with helpers (`nearest`, `alive`, `byId`). For this game: most of the 900 `megaman` sites become `players.nearestTo(this)` or `players.forEach`.
3. **Enemies choose targets by a rule.** Default: nearest living player, with stickiness and an event-driven re-check. With one player the result does not change.
4. **Input frames per player.** The body reads commands from its own controller, never from devices. For this game: each player's `ControllerComponent` reads the input source of its slot.
5. **Data plus composition for characters.** A stats table per character (like Celeste's constants) and abilities as optional components or capabilities. For this game: Mega's constants go in a character definition; Proto Man is another definition with its own ability set. Contact rules query capabilities, which matches the `014d` receivers.
6. **Small read-only views for consumers.** Most consumers need position, bounds, facing, alive and id. Split `IPlayer` into a view part (enemies, triggers, HUD) and a command part (damage, teleport). Abilities go behind capability queries.
7. **Player ids in events, and damage attribution.** Events about the player carry a `playerId`. Projectiles keep `owner` as the instigator.
8. **Choose a co-op death design before you build it.** Choices: bubble or balloon, parry revive, coffin, shared health. A team wipe means all players are down at once. For fixed-screen rooms, a common choice is "everyone moves to the transition; downed players come back at the next room or checkpoint".
9. **The camera is a local, per-viewport concept.** It frames a set of players; it is not "the player". `CameraManagerForRooms` and culling should use a target set, so split screen can add viewports later.

Common pitfalls:

1. **A hidden "player 0" default.** For example the host-targeting bug (`getPlayerController(0)`). For this game: a `players.first()` convenience brings back the global. Do not add it, or flag every use.
2. **A leader camera or "primary player" in gameplay rules.** Room transitions, spawners and cullables that check "Megaman's room" must change to "any player's room" or "all players".
3. **Target flip-flop.** A pure nearest rule, re-checked every frame, makes enemies jitter between two players. Add stickiness, or re-check only on an attack cycle.
4. **No fixed iteration order, unseeded RNG.** These block lockstep and rollback. Keep players in slot order (`Array`, not `ObjectSet`) and gameplay random generators in the game state.
5. **State on the body that should survive death.** Health tanks, lives, weapons and the character choice belong to the per-player state, not to the `Megaman` entity.

### Gaps

- No primary source documents the camera and room-transition rules for Shovel Knight co-op, the Mega Man 10 prototype co-op or Mega Man Powered Up. The Shovel Knight wiki page returned HTTP 402.
- The Smash, Mega Man X/Zero and King Knight/Specter Knight notes come from training knowledge only.

### Sources

- Gameplay Framework in Unreal Engine: https://dev.epicgames.com/documentation/en-us/unreal-engine/gameplay-framework-in-unreal-engine
- Player Controllers in Unreal Engine: https://dev.epicgames.com/documentation/en-us/unreal-engine/player-controllers-in-unreal-engine
- `UGameInstance::CreateLocalPlayer`: https://dev.epicgames.com/documentation/unreal-engine/API/Runtime/Engine/UGameInstance/CreateLocalPlayer
- Maximum number of local players (Unreal forum): https://forums.unrealengine.com/t/maximum-number-of-local-players-on-one-pc-can-you-have-more-than-4/303314
- `UGameplayStatics::ApplyDamage` (UE 4.27): https://docs.unrealengine.com/4.27/en-US/API/Runtime/Engine/Kismet/UGameplayStatics/ApplyDamage/
- `AActor::TakeDamage` (UE 4.26): https://docs.unrealengine.com/4.26/en-US/API/Runtime/Engine/GameFramework/AActor/TakeDamage/
- Damage in UE4 (Unreal blog): https://www.unrealengine.com/blog/damage-in-ue4
- Unity Input System, Player Input Manager: https://docs.unity3d.com/Packages/com.unity.inputsystem@1.11/manual/PlayerInputManager.html
- Unity Input System, Player Input: https://docs.unity3d.com/Packages/com.unity.inputsystem@1.11/manual/PlayerInput.html
- Godot, High-level multiplayer: https://docs.godotengine.org/en/stable/tutorials/networking/high_level_multiplayer.html
- Customized controls per player for local multiplayer (Godot forum): https://forum.godotengine.org/t/customized-controls-per-player-for-local-multiplayer-coop/277
- GDQuest, local multiplayer input: https://gdquest.com/tutorial/godot/2d/local-multiplayer-input/
- Terraria Wiki, Aggro: https://terraria.wiki.gg/wiki/Aggro
- AI threat tables and target selection in UE5 (StraySpark): https://www.strayspark.studio/blog/ai-threat-tables-target-selection-ue5
- Enemy targeting devlog (itch.io): https://itch.io/devlog/798680/enemy-targeting.amp
- Tremblay et al., FDG 2014, target selection for AI companions: https://www.cs.mcgill.ca/~jtremb59/Papers/fdg2014_paper_32.pdf
- New Super Mario Bros. Wii (Super Mario Wiki): https://www.mariowiki.com/New_Super_Mario_Bros_Wii
- Bubble (Super Mario Wiki): https://mariowiki.com/Bubble
- Rayman Legends Co-Op Review (Co-Optimus): https://co-optimus.com/review/1347/page/2/rayman-legends-co-op-review.html
- Cuphead (Wikipedia): https://en.wikipedia.org/wiki/Cuphead
- Spelunky Wiki, Other Game Features (2): https://spelunky.fandom.com/wiki/Other_Game_Features_(2)
- Spelunky 2 review (GamingTrend): https://gamingtrend.com/reviews/unfair-fun-spelunky-2-review/
- Shovel Knight devs discuss co-op (Game Informer): https://gameinformer.com/b/features/archive/2014/07/29/shovel-knight-devs-discuss-co-op-pogo-jumping-and-beyond.aspx
- Shovel Knight Update Brings Co-op Play (TrueTrophies): https://i1.truetrophies.com/n11540/shovel-knight-update-brings-coop-play
- Mega Man 10 prototype co-op (Rockman Corner): https://rockman-corner.com/2024/01/mega-man-10-prototypes-include-evidence.html
- Mega Man 2.5D (Wikipedia): https://en.wikipedia.org/wiki/Mega_Man_2.5D
- Celeste `Player.cs` (GitHub, NoelFB/Celeste): https://github.com/NoelFB/Celeste/blob/master/Source/Player/Player.cs
- Celeste: open-source Player class (itch.io): https://notapixel.itch.io/platformer-test/devlog/729893/celeste-open-source-player-class
- Game Programming Patterns, Type Object: https://gameprogrammingpatterns.com/type-object.html
- Game Programming Patterns, Service Locator: https://gameprogrammingpatterns.com/service-locator.html
- How to use Scriptable Objects in Unity (Red Gate): https://www.red-gate.com/simple-talk/development/dotnet-development/how-to-use-scriptable-objects-in-unity/
- Gaffer On Games, Deterministic Lockstep: https://gafferongames.com/post/deterministic_lockstep/
- Infil, fighting game netcode (rollback, part 5): https://words.infil.net/w02-netcode-p5.html
- Odin GGPO bindings: https://pkg.odin-lang.org/vendor/ggpo
- Rollback netcode explained (Supercraft): https://crux.supercraft.host/blog/rollback-netcode-explained/

## Co-op gaps and ideas (2026-10-23)

Owner decision: `029` keeps the current level-flow rules. It builds the interface and removes the coupling to `megaman`. This section lists what `029` leaves for other items, so that their decisions tie back to `029`.

### Players service helpers

The owner's direction: an interface and a base class in the engine. The base class gives standard helpers, and a game can add its own. Ideas for the scope step:

- Keep the interface small and primitive: the slots in slot order, `get(slot)`, the active players, iteration.
- The base class (or engine extension functions on the interface) gives the standard helpers: `nearestTo`, `any`, `all`, `active().forEach`, a distance filter, and so on.
- Custom helpers: a Kotlin extension function on the interface works at every call site with no cast and no subclass. A subclass with extra members needs callers to hold the subclass type. Compare both in the scope step.
- Rules for every helper: slot order for every iteration and tie (determinism for online, `047` step 3); no helper that gives "the first player" without a named slot.

### Session lead slot, shared earnings and save files

Owner's input (2026-10-23): saves must not depend on the player count. Example: two players play the campaign on day 1, and one player continues the same save on day 2. One player is registered at boot, and more players can join or leave later. The owner suggests that the camera follows the primary player, the primary player is the "source of truth", and what the primary player earns is shared with the other players.

- This fits the "no public `primary`" rule if the lead slot is a session policy (camera focus policy, profile owner), not a helper in the players query API. Enemies, hazards and triggers still ask for nearest, any, all or the contact player.
- "Shared earnings" is one profile for the session. Every slot points at the same profile record (review 4, section 3: a profile store keyed by slot can point two slots at the same record). The save holds that one profile and no player count.
- Open: what happens when the lead player leaves the session? Does the next slot become the lead?
- Recorded in `047` (owner's input) and `007` (save files).

### Ownership of projectiles and other entities

The owner notes that one `owner` per entity is probably a mistake (for example a reflected shot, a projectile that spawns an explosion, or a shot from a turret that a player placed). Engines often keep two values: the instigator (the player or controller responsible) and the causer (the entity that hit) (review 5, section 1, Unreal `ApplyDamage`). This goes to `030` (faction and root owner).

### Events

- `029`: add a player slot prop to `PLAYER_SPAWN`, `PLAYER_JUST_DIED`, `PLAYER_READY`, `PLAYER_DONE_DYIN`, `STUN_PLAYER` and the pickup events (`ADD_PLAYER_HEALTH`, `ADD_WEAPON_ENERGY`, `ADD_CURRENCY`, `ATTAIN_*_TANK`). With one player the value is always slot 0.
- `038` (inside the `013` flow): split `PLAYER_SPAWN` into a spawn event for each player and a level reset. Move the 24 listeners that use `PLAYER_SPAWN` as a level reset (review 2, section 4, item 3). The owner wants one orchestrator to own the level reset, not logic spread over many listeners.
- `057` (split out of `053`): typed event channels (player, level, controller) with typed payloads, in small stages.

### Rules that `029` leaves unchanged

Each gap is listed with the item that takes it. The details are in reviews 2 and 3.

| Gap today | Item |
|---|---|
| One camera focus drives room detection and transitions | `047`, `051` |
| Off-screen rule, room transition with players apart | `047` |
| Death and respawn of one player while others live; drop-in and drop-out | `038`, `047` |
| `GauntletMaster` resets on any `PLAYER_JUST_DIED` | `047` |
| `PlayerStatsHandler` draws one set of bars | `047` |
| Pause owner and the pause menu | `013`, `047` |
| Boss intro and defeat lock one player; `megaman.dead` cancels the victory | `013`, `047` |
| Pickups (`ADD_PLAYER_HEALTH`, `ADD_WEAPON_ENERGY`) have no target | `029` adds the slot prop; the rule is `047` |
| Room gravity (`megaman_direction`) for all players | `047` |
| Kill zones (`MegamanDeath`, `GravityBlock`) check only `megaman` | `029` (decoupling: each player in the contact) |
| `AbstractEnemy.kt:255` owner check, enhancement damage scaling | `030` |
| Exact-class `KClass` damage and mask sets | `030` or `023` |
| Global input, controller 0 in 4 entities | `031` |
| Clock-seeded RNG, `Gdx.graphics.deltaTime` in the camera | `011`, `010` |

## Targeting design (2026-10-23)

From the discussion after the `029a` draft scope. Not a branch scope yet; `029b` decides the details.

### Call sites name the intent, not the rule

Today's enemies are not designed around "nearest": there is one player, and they target it at any distance. `029` must not decide a co-op targeting rule (that is `047`), and it must not write `players[0]` at about 900 sites (the hidden "player 0" trap, and a second pass over every site for co-op). Each call site uses a helper for its intent:

| Intent at the call site | Helper | In `029` | Changes later in |
|---|---|---|---|
| Aim at, face or move toward "the player" (about 290 lines) | `players.targetFor(this)` | slot 0 | `047`: the default policy, in one place |
| Proximity or trigger (about 195 lines) | `players.any { ... }` | true if any player matches | nothing |
| The player in a contact | the entity from the contact | already known | nothing |
| All players (locks, cutscenes, a shot at each player) | `players.forEachPresent { ... }` | one player | nothing |
| Save, title screen, single-player flows | `players.require(PlayerSlot(0))` | slot 0, named | only where co-op changes the flow |

In `029`, `targetFor` gives Megaman also while he is dead, as `megaman` does today. With capacity 1 it never gives null.

### The policy is a component, not an extension `when` or an open method

The owner's concern: one extension function that supports every entity grows into a large `when`, and an open function on `AbstractEnemy` is inheritance (`048`). The design:

```kotlin
fun interface TargetPolicy<V : PlayerView> {
    fun select(seeker: IGameEntity, players: IPlayers<V>): V?
}

class TargetingComponent<V : PlayerView>(var policy: TargetPolicy<V>) : IGameComponent

fun MegaPlayers.targetFor(seeker: IGameEntity): MegaPlayer? =
    (seeker.getComponent(TargetingComponent::class)?.policy ?: defaultTargetPolicy).select(seeker, this)
```

- The dispatch function never names an entity class. Per-entity behavior is data on the entity, like the `014` receivers.
- `MegaPlayers.defaultTargetPolicy` is "slot 0" in `029`. `047` changes the default once (for example nearest, or sticky nearest). An entity with its own rule (for example a boss that alternates players) attaches a `TargetingComponent`.
- Reusable policies: `SlotZero`, `Nearest`, `Sticky(inner)`, `LastAttacker`, `RoundRobin`.
- A stateful policy (sticky, last attacker) is an instance per entity, held by the component. The component's `reset()` clears it, so pooled entities start clean.
- `TargetPolicy` and `TargetingComponent` assume only `PlayerView`, so they can live in the engine (targeting is a genre concept). Policies that need a position (`Nearest`) are in core.
- Cost: one component lookup per call; no allocation (policies are made once).
- Timing: `029b` adds `targetFor`, `TargetPolicy` and the `SlotZero` default when it moves the first enemy. `TargetingComponent` comes the first time an entity needs its own rule (probably `047`, or `029c` if a boss shows a real need). `029a` does not include targeting.

### Update (2026-10-23): targeting is not global

Owner preference after `029b`-`029d`: the service-wide `targetFor` and the `targetPlayer` extension on every entity are a migration path, not the design. Not every entity needs a target player (blocks do not), and each entity that targets does it its own way. The `TargetingComponent` idea above fits that: an entity that targets owns its policy; an entity that does not target has none, and no "target player" API reaches it. When the first entity needs its own rule (or co-op arrives in `047`), the call sites that use `targetPlayer` move to the owning entity's component or policy. `TODO`s at `MegaPlayers.targetFor` and `IMegaGameEntity.targetPlayer` point here.

### One target per policy

The owner asked whether a policy returns one player or several.

- A policy returns one player. Most sites need one target (an enemy faces one way and aims at one point), stateful rules are about one target, and a list would allocate on each call or need `[0]` at every site.
- "Every player" and "every player that matches" are not targeting choices. They use `forEachPresent` and the filter helpers, with no policy.
- If a real case needs a policy to choose a subset (for example a boss with 4 players that picks the 2 with the most threat), add a second operation and keep the single one:

  ```kotlin
  fun interface MultiTargetPolicy<V : PlayerView> {
      fun select(seeker: IGameEntity, players: IPlayers<V>, out: Array<V?>): Int
  }

  inline fun MegaPlayers.forEachTarget(seeker: IGameEntity, action: (MegaPlayer) -> Unit) { /* entity's multi policy */ }
  ```

  It fills a reused buffer and never returns a list. Do not build it before a real case exists; today no entity chooses a subset.

## `CharacterDef` design (2026-10-25)

From the `029g` planning (branch `docs/NOTES.md`, "Decisions" and "Review"). `029g` only splits the behaviors
into factory functions. `CharacterDef` waits for the Proto Man item, so that two characters show its shape.

### Decisions so far

- Name: `CharacterDef`, not `PlayerDef`. A player is a slot and its controller; the def describes the body it
  controls (review 5, `Pawn` and `Controller`). A later NPC sidekick could use it once input (`031`) and the
  player-only parts (`048`) allow it. Package: `characters/` is recommended over `players/` (not decided yet).
- Place: core first. Move to the engine when two characters show the shared fields and factory signature.
- No player base class (`048`).
- No `CharacterValues` table with Mega's field list: it would claim to be generic while its fields are the
  settings of Mega's behaviors. Mega's numbers stay in `MegamanValues`; a second character gets its own values
  file.
- No helper that selects a value by the target player's character (`is Megaman` under a new name). Each
  character gives its own value, and readers (the stomp bounce in 6 enemies, `SwimBooster`) read it from the
  contact player.

### How the values reach the code

- The behaviors read their values through parameters on the factory functions. Suppliers fit the values that
  change with the direction (`{ jumpVel }`) or at runtime. Use a primitive `fun interface FloatSupplier`, not
  `() -> Float` (boxing on each call).
- Parameters on the behaviors alone are not enough. The values that set how a character feels are elsewhere:
  - jump and swim velocities and the 7 gravities: the `direction` setter (`Megaman.kt:239-265`) and
    `BodyComponent` `preProcess`;
  - the run steps and impulses: `ControllerComponent.kt:178-229`; max speed and clamp: `BodyComponent.kt:392,
    447`;
  - timers made at construction: crouch delays, wall-jump impetus, wall-slide lockout (`Megaman.kt:146,
    153-154, 340`);
  - damage, stun and freeze values in `Megaman.kt`.

  `defineBodyComponent` and `defineControllerComponent` need the same kind of parameters or suppliers.

### What a second character needs first

1. Its jump, gravity and run values (above).
2. A body def: the sizes (`BodyComponent.kt:36-41`) and the fixtures.
3. Sprites, animations (the key supplier picks animations by `BehaviorType`) and sounds.
4. A loadout (`024`): Proto Man's shield and buster are weapon or ability work. `JETPACKING` is a Rush Jet
   ability and leaves the ability list with `024`.
5. An ability-set contract: which `BehaviorType` keys outside code may expect (141 uses outside the behaviors,
   some in enemies and blocks); the engine `setBehaviorAllowed` throws for a missing key; the `aButtonTask`
   transitions (wall slide and climb set `AIR_DASH` when they end).
6. A damage reaction and a damage negotiator per character.
7. The remaining Mega-named APIs (`IHazard.getDamageToMegaman`, `MegaPlayer.megaman`) and `is Megaman` checks.
8. Factories that a separate player class can use (they read `Megaman` fields today; `048`).

## Who drives an entity (2026-10-26)

A proposal that touches this item (driver, thin slot view, faction; the owner's "boss becomes playable" case) is in
`031` `NOTES.md`, "Who drives an entity: driver, slot view and side". Not decided.

## `MegaPlayer` review and engine patterns (2026-10-26)

Two research agents, on `master` at `43f1d5742`. A proposal for discussion, not decided. The owner's questions after
the first two reports are at the end of this section; a third research report (functions, components and events
instead of classes) follows in its own section.

### Why `MegaPlayer` is big

`MegaPlayer` (`core/.../players/MegaPlayer.kt`, 150 lines, 37 public members) is a forwarding façade. `029` built it
as a one-to-one replacement for each `game.megaman.X` call site. In effect it is "Megaman, with its type hidden".

Member inventory (from `grep` on `core/src/main`; receivers `targetPlayer`, `player`, `it` in `players.*` lambdas,
`asPlayer`; the engine has no callers):

| Member | Lines / files | Who uses it | Note |
|---|---|---|---|
| `body` | ~414 / 136 (`targetPlayer.body`) + 62 (`player.body`) | enemies, bosses, hazards | `getX` 172, `getCenter` 138, `getBounds` 74; writes: `physics` 32, `setCenter*` 4 |
| `direction` (get/set) | 27 | enemies, bosses | only setter: `WorldGravity.kt:13` (runs Megaman's gravity table, `Megaman.kt:239-265`) |
| `facing` (get/set) | 8 | blocks, hazards | only setter: `WilyFinalBoss.kt:299` |
| `dead` | 21 | enemies, level screen | forwards to `MegaGameEntity.dead` |
| `ready` (get/set) | 13 | handlers write, hazards read | flag in `PlayerStateComponent` |
| `canBeDamaged` (get/set) | 7 / 5 | handlers, 1 boss, 1 hazard | only `Megaman.invincible` reads it (`Megaman.kt:111`) |
| `setAllBehaviorsAllowed` | 5 / 3 | handlers, `SpiderWeb` | forwards to `BehaviorsComponent` |
| `allowMovement` | 2 / 1 | `SpiderWeb` | only `Megaman.canMove` reads it (`Megaman.kt:124`) |
| `input` | 9 / 5 | special, block, level screen | duplicates `ControllerComponent.input` |
| `slot` | 11 / 10 | items, enemy drops | real slot identity |
| six fixture getters (`feet`, `left/rightSide`, `head`, `damageable`, `feetGravity`) | 1-10 each | blocks, special | string-keyed body properties with Megaman's fixture names |
| `currentHealth`, `maxHealth`, `translateHealth`, `healthRatio`, `depleteHealth` | 1-5 each | HUD, pause, drops, kill zones | forward to `IHealthEntity` |
| `isBehaviorActive`, `isAnyBehaviorActive` | 4, 1 | few | forward to `BehaviorsComponent` |
| `light` | 4 | `Darkness*` only | Megaman-only (`PlayerKeys.LIGHT`) |
| `onGravityDeviceSwitch` | 1 | `GravitySwitcharoo` | Megaman-only |
| `shooting`, `damaged` | 1, 3 | `AbstractEnemy`, 2 bosses | borrow Megaman's `shootAnimTimer` and `damageTimer` |
| `teleporting`, `invincible`, `focus`, `spawn`, `present` | 1-2 each | few | flags or forwards |
| `add/removeDamageListener` | 1 each | `AbstractBoss` (every boss registers) | only `PreciousWoman.kt:305` reacts |
| `entity` | 5 / 3 | escape hatch | followed by a cast |

- About 22 of the 37 members only forward to a component or an interface the entity already has.
- 9 members have one caller file. Megaman-specific: `light`, `onGravityDeviceSwitch`, `shooting`, the 6 fixture
  getters, and the `megaman` downcast (`PlayerExtensions.kt:12`, 19 uses in 7 files: weapon, ammo, sprite look).
- No fully dead members.

### Smells

- **Forwarding façade and fat interface.** A caller that needs a position also gets `depleteHealth`, `spawn`,
  `setAllBehaviorsAllowed` and the `facing` setter. Enemies can write the player's state.
- **The constructor forces Megaman's shape** (`MegaPlayer.kt:31-48`): `IDirectional`, `IFaceable`, `IDamageable`,
  `IHealthEntity`, `MegaGameEntity`, `IFocusable`; `BodyComponent`, `BehaviorsComponent`, `PlayerStateComponent`,
  `ControllerComponent`; two property lambdas. A boss or Proto Man must fake the lambdas to register. The engine
  `IFaceable` and `IDirectional` have only `var`, so 35 read-only callers get write access for 2 writers.
- **`PlayerStateComponent` is honored only by `Megaman`** (`Megaman.kt:111`, `:124`, `:726`; timers borrowed at
  `Megaman.kt:138`; no `reset()`). Handlers that set the flags on any other entity have no effect, and nothing
  reports it.
- **`asPlayer` means three things** (side, player-body rules, player state); 152 uses in 64 files; a property lookup
  on each call.
- **String-keyed fixtures**, two cast styles, one map lookup per access.
- **Lifecycle:** `unregister` leaves `PlayerKeys.VIEW` and the input source on the entity (`TODO` at
  `MegaPlayer.kt:49`); `AbstractBoss` adds its damage listener only to players present at its spawn.
- **Reversed dependency:** `Megaman` reads its own view to fire its damage listeners (`Megaman.kt:725`).
- **Co-op leak:** `FeetSandRule { game.players.any { it.ready } }` (`MegaContactRules.kt:37`) checks any player, not
  the player in the contact.
- **`invincible` and `canBeDamaged` overlap:** `Megaman.invincible` includes `!canBeDamaged || !ready`.

### Options compared (first review)

| | (a) Thin view + capabilities | (b) Role interfaces | (c) Events for writes | (d) Plain component reads |
|---|---|---|---|---|
| Targeting | `view.entity` → body component, facing | `ITargetable` on the entity | not suitable | read the components |
| Damage gating | a locks capability | `IDamageGate` | an event: 1 frame late | read a locks component |
| Level locks | capability lookup | `ILockable.lock(reason)` | handlers only | write a locks component |
| HUD | loadout capability | `IHasLoadout` | not suitable | loadout component |
| Proto Man | supplies only what it has | implements the roles it has | same | adds the components it has |
| Playable boss | new view; missing parts give `null` | must implement the roles | — | add components at the switch |
| Cost per call | 1 map lookup (cacheable) | a virtual call | allocates an `Event` and `Properties`; 1 frame late | 1 map lookup (cacheable) |

Events in this engine are buffered until `eventsMan.run()` after the render, so a write sent as an event acts one
frame late. The first review rejected events for locks, gravity and targeting for that reason, and kept them only
for the player-damaged notification (only `PreciousWoman` reacts; a one-frame delay there is probably harmless,
inferred).

### How other engines and games do it (second review)

Scope: beyond reviews 4 and 5 above. Tags: [web] with a source, [code] this repo, [training] unchecked.

- **Unreal Gameplay Ability System.** Gameplay tags are hierarchical labels (`Movement.Mode.Swimming`), queried with
  `HasTag`, `HasAny`, `HasAll`; `IGameplayTagAssetInterface` gives an object's tags without a cast. The ability
  system keeps a count per tag: a tag stays on until its count is 0 (`AddLooseGameplayTag`,
  `RemoveLooseGameplayTag`); a stun is the tag `State.Debuff.Stun` for its duration; a delegate fires on add and
  remove. Abilities have `ActivationBlockedTags` and `ActivationRequiredTags`, so "can this start" is a tag query.
  The owner actor (often `PlayerState`) differs from the avatar actor (the body). [web]
  <https://dev.epicgames.com/documentation/en-us/unreal-engine/using-gameplay-tags-in-unreal-engine>,
  <https://github.com/tranek/GASDocumentation>
- **Unity (classic).** Components on GameObjects; `TryGetComponent<T>` does not allocate when the component is
  missing [web] <https://docs.unity3d.com/ScriptReference/Component.TryGetComponent.html>. A common pattern puts an
  interface such as `IDamageable` on a component, and a bullet calls `TryGetComponent<IDamageable>` on what it hit,
  so it never learns the concrete class [training].
- **Godot.** Groups work like tags; a node can be in many groups; `call_group("guards", "enter_alert_mode")` calls a
  method on every member [web] <https://docs.godotengine.org/en/stable/tutorials/scripting/groups.html>. Type tests
  use `is_in_group("player")` and `has_method("hit")` (duck typing) [training].
- **ECS engines (Bevy, EnTT, flecs).** No interfaces: marker components and queries. Bevy filters with `With<Player>`
  and `Without<Enemy>`; `Option<&T>` matches entities with or without a component [web]
  <https://bevy-cheatbook.github.io/programming/queries.html>. EnTT does not allocate per entity for empty tag types;
  views take `entt::exclude<T>` [web] <https://github.com/skypjack/entt/wiki/Entity-Component-System>. In archetype
  ECS, adding or removing a tag moves the entity to another table, so engines advise against tags that change every
  frame [training].
- **Sonic Mania (decompilation).** One `EntityPlayer` for all five characters. `Player_ChangeCharacter` sets
  `characterID`, the sprite set, sensor offsets and `stateAbility` (a function pointer).
  `Player_UpdatePhysicsState` picks a physics table per character, then a row by flags (underwater, super, speed
  shoes). Other objects ask through shared functions (`Player_Hurt`, `Player_CheckAttacking`,
  `Player_CheckValidState`), not by reading fields. Cost: those functions contain `switch (characterID)` (19
  comparisons in `Player.c`). [web]
  <https://github.com/RSDKModding/Sonic-Mania-Decompilation/blob/master/SonicMania/Objects/Global/Player.c>
- **Sonic Robo Blast 2.** An `S_SKIN` record per character: `ability`, `ability2`, `normalspeed`, `actionspd`,
  `jumpfactor`; unset values take defaults; ability enums select shared code [web]
  <https://wiki.srb2.org/wiki/Custom_character_tutorial/S_SKIN>.
- **Fighting games.** Every Smash fighter has the same attribute set (weight, walk speed, gravity, fall speed,
  jumpsquat); moves differ per fighter [web] <https://www.ssbwiki.com/Template:Attributes>.
- **Mega Man X6 (ideas only).** X and Zero share 32 step functions; a step calls the character's overlay after a test
  of `mega+0x02`; Zero's extra steps are in his own overlay (`mmx-demake-tools/docs/players/map.md:12-25`,
  `zero.md:1-40`).
- **Component communication (Nystrom, "Component").** Three ways: shared state in the container (decoupled, but
  depends on update order), direct references (fast, coupled), messages (decoupled, most complex) [web]
  <https://gameprogrammingpatterns.com/component.html>.

How these answer `MegaPlayer`'s questions:

| Concern | Common pattern | Examples |
|---|---|---|
| Is this entity a player? | a tag or marker, not the type | Godot group `player`; Bevy `With<Player>` |
| Can it be damaged now? | ask the target through one function; the target decides | Mania `Player_Hurt`; engine `IDamageable.canBeDamagedBy` |
| Is it climbing or sliding? | a tag query or a read-only state query | GAS `HasTag(Movement.Mode.Climbing)`; Mania `player->state` |
| Who writes facing, direction, body? | outsiders read; writes go through commands or effects | GAS effects; Unreal `TakeDamage` |
| Health | an attribute that outsiders read; changes through damage or heal requests | GAS `AttributeSet` |
| Optional features (light, gravity device, charge) | optional component or capability; absent means no effect | Unity `TryGetComponent`; Bevy `Option<&T>` |
| Per-character values | one record of the same fields per character | Smash attributes; SRB2 `S_SKIN`; Mania physics table |
| Per-character behavior | function pointers or ability lists selected per character | Mania `stateAbility`; SRB2 `CA_*`; X6 overlays |
| Body shape | data per character (sensor offsets, hitboxes) | Mania `Player_ChangeCharacter` |

State flags: the common fix for "last writer wins" booleans is counted reasons (GAS tag counts): `lock(reason)` and
`unlock(reason)`; the flag is on while the count is above 0. Derived values (`canMove`) stay derived from counted
inputs. Short effects that end by themselves (stun, invincibility frames) are timers or status effects with a
duration, not flags someone must clear [training].

Fit for this game: Mania, SRB2 and X6 are 2D games that run well on weak hardware, and all three use one shared
motor, one values record per character and function hooks. Full GAS (effects, prediction, replication) is too large;
string tags add hashing on hot paths (use an enum or a bit set); marker components that change often change system
membership, which only `GameEngine.spawnNow` refreshes; Mania's `switch (characterID)` in shared functions is
`is Megaman` with a new name; messages for per-frame queries add complexity and order depends on the events manager.

### First recommendations (superseded in part by the owner's questions below)

1. A thin view with `slot`, `entity` and the cached `body`.
2. Reads through small read-only roles on the entity (`IReadFaceable`, `IReadDirectional`, `IHealthEntity`, optional
   `IEmitsLight`, `IGravityDeviceUser`, a loadout interface); a missing role gives `null` or a default.
3. Counted locks (`LockReason`) in one component that shared code honors (damage gating, shared movement behaviors,
   the input edge builder), replacing `ready`, `canBeDamaged`, `movementAllowed` and `setAllBehaviorsAllowed`.
4. Events only for notifications that nothing reads in the same frame.
5. "Player side" to faction (`030`); `asPlayer` then means only "has a slot view".

Proposed migration (each step "plays the same", each with a real caller): a `029` cleanup of 1-2-caller forwarders;
read-only engine roles; counted locks (`012`); the damage notification as an event; faction (`030`); loadout (`024`);
Proto Man with optional roles and typed fixtures (`060`); possession (`047` or a playable boss).

### The owner's questions (2026-10-26)

- **The body.** "Should we even require the cached body? It is not a guarantee that every player will have a body."
  Answer: no. The view should hold only `slot` and `entity`; a caller that needs a body asks for the body component
  and handles its absence.
- **Interfaces.** "Is doing it via interfaces the approach that the other game engines do?" Answer: it is one pattern
  among several. Unity mixes components and interfaces on components; ECS engines (Bevy, flecs, EnTT) use components,
  queries and free functions with no interfaces; Godot uses groups and duck typing; GAS uses tags and attributes;
  Mania uses shared functions over one entity struct.
- **Functions instead of classes.** The owner is not a fan of OOP for this, because it makes every entity extend a
  class. Why not functions and components, with global functions that reconcile, instead of tying things to class
  instances? The counted-locks proposal is already a component, but its reader was `AbstractHealthEntity` (the OOP
  part). A free function over the component (for example `canBeDamaged(entity)`) would serve any entity.
- **Events.** What about direct, instantaneous event dispatch and reads, and static/global functions? How do other
  engines do this?
- The owner asked for an extensive record of how other engines handle these problems. A third research agent covers
  functions and systems instead of methods, locks without base classes, immediate versus deferred events, querying a
  player without an interface, and static/global functions. Its report goes in the next section.

## Functions, components and events instead of classes (2026-10-26)

The third research report, for the owner's questions at the end of the section above. Read-only research agent, on
`master` at `4abf1ff7f`. A proposal for discussion, not decided. Tags: [web] = from a fetched page (URLs in
"Sources" below), [code] = this repo, [training] = the agent's own knowledge, not checked. The claim about
`EventsManager` order (below) was checked again in the code (`engine/.../events/EventsManager.kt:71-76`).

### Summary

- Every ECS engine checked writes logic as **free functions over component queries**: Bevy, flecs, EnTT, and the
  C-style 2D games (Sonic Mania's `Player_*` functions). Methods on an entity class are the exception. This engine
  already does it in part: `GameSystem` is a function over a component mask.
- **Locks** come in four forms: a counted set of tags (Unreal GAS); an on/off bit on a component with no structural
  cost (Unity `IEnableableComponent`); a tag or relationship pair per reason (flecs `(LockedBy, X)`); presence in a
  table (Fabian). For this game the best fit is a small `LocksComponent` with a count per reason, read by free
  functions such as `canBeDamaged(e)`.
- **Events:** all major engines have both a buffered channel (Bevy `Messages`, EnTT `enqueue`/`update`, Godot
  `call_deferred`, Unity ECB) and an immediate channel (Bevy observers, flecs `emit`, EnTT `trigger`/`sigh`, Godot
  signals, Unreal delegates). Almost none guarantees the order of immediate listeners. Each one limits re-entrancy:
  commands are queued inside handlers, and listeners must not connect or disconnect while dispatch runs.
- The repo **already has immediate observers**: `damageListeners`, `onDamagedCallbacks`, `invinciblePredicates`,
  `runnablesOnSpawn`. They are ad-hoc lambdas on the entity class. A typed synchronous registry would make them
  uniform, with no engine change.
- **Querying a player:** engines use a marker component plus a query or tracker (Bevy `With<Player>`, Celeste
  `Tracker.GetEntity<Player>()`), or a shared struct plus global functions (Mania). Optional features are optional
  components, not interface members.
- **Static functions** are fine when they are stateless and take their data as parameters. Global *state* is the
  risk. The repo's `game.players` service is a good middle form.
- **Engine changes needed:** none for free functions, locks or a sync registry. A small change is useful if a system
  should skip locked entities without a per-entity check, or to fix the delivery order of `EventsManager`.

### Functions and systems instead of methods

- **Bevy.** "Systems: normal Rust functions." The parameters declare the data:
  `fn greet_people(query: Query<&Name, With<Person>>)` [web]. Global data is a `Res<T>`/`ResMut<T>` parameter, a typed
  singleton that the scheduler injects, so a dependency shows in the signature [training]. Order comes from
  `.before()`, `.after()` and `.chain()`; two systems that access the same data with no order are "ambiguous", and Bevy
  can report them [web]. `run_if` gates a system with a condition that is "evaluated at most once (per schedule run)";
  Bevy warns that "there is no guarantee that the condition is upheld after the first system has run" [web].

  ```rust
  fn hurt(mut q: Query<(&mut Health, &Locks), With<Damaged>>) { for (mut h, l) in &mut q { if l.is_empty() { h.0 -= 1 } } }
  app.add_systems(Update, (move_sys, hurt).chain().run_if(in_state(Level::Playing)));
  ```

- **flecs.** "Systems are queries + a function". A pipeline runs the phases `OnStart, OnLoad, PostLoad, PreUpdate,
  OnUpdate, OnValidate, PostUpdate, PreStore, OnStore`; inside a phase, systems run in entity-id order, which
  "generally means that systems will be ran in the order they are declared" [web].
  `world.system<Position, const Velocity>("Move").each([](Position& p, const Velocity& v){...})` [web].
- **EnTT.** Systems are "plain functions, functors, lambdas", with no registration [web]; the call order is your code
  [training].

  ```cpp
  void movement_system(entt::registry &r) { r.view<position, velocity>().each([](auto, auto &p, auto &v){ p.x += v.dx; }); }
  ```

- **Unity DOTS.** `ISystem` structs with `OnUpdate(ref SystemState)`, iterating
  `SystemAPI.Query<RefRW<LocalTransform>, RefRO<Speed>>()`; order from `[UpdateInGroup]`, `[UpdateBefore]`,
  `[UpdateAfter]` [training].
- **Sonic Mania (decomp).** One shared `EntityPlayer` struct; all player logic is global C functions that take the
  struct pointer: `Player_CheckCollisionTouch`, `Player_Hurt`, `Player_CheckBadnikTouch`, `Player_ProjectileHurt`
  [web, `Player.c:2255-2684`]. A badnik loops `foreach_active(Player, player) { if (Player_CheckBadnikTouch(player,
  self, &hitbox)) Player_CheckBadnikBreak(player, self, true); }` [web, `Motobug.c:92-95`]. Per-character differences
  are branches on `player->characterID` (19 tests in `Player.c`). The state is a function pointer
  (`StateMachine(state)`), and so is input (`StateMachine(stateInput)`) [web, `Player.h:368,441`]: the "driver" idea
  of item `031` already exists there as a swappable function.
- **Celeste/Monocle.** Classic OOP entities with `Component.Update()`; the `Tracker` adds lookups by type:
  `GetEntity<T>()`, `GetEntities<T>()`, `GetComponents<T>()`, `GetNearestEntity<T>()` for `[Tracked]` types [web].
- **Carmack, "Functional programming in C++".** "A pure function only looks at the parameters passed in to it."
  "Pure functions are trivial to test." "Moving a function towards purity improves the code, even if it doesn't reach
  full purity." Passing an all-globals pointer defeats the purpose [web].
- **Acton (CppCon 2014).** "The purpose of all programs ... is to transform data"; "where there is one, there are
  many": write functions for the batch, not the object [training].
- **Overwatch (Ford, GDC 2017).** Systems hold no state; components hold no behavior; global state is in "singleton
  components"; shared helpers are "utility functions" with few side effects and few call sites; side effects are
  deferred to one place in the frame [talk exists: web; contents: training].
- **This repo.** `GameSystem.process(on, entities, delta)` is already a function over a mask; most logic is still in
  entity methods and in lambdas in `UpdatablesComponent` [code].

### Locks and state flags without base classes

- **Unreal GAS: tags with counts.** `FGameplayTagCountContainer` stores "the number of instances of that
  GameplayTag"; `HasTag()` is false at count 0 [web]. Loose tags "are not replicated and must be managed manually"
  [web]. An ability declares `ActivationBlockedTags` (blocked by **any**), `ActivationRequiredTags` (needs **all**),
  `ActivationOwnedTags`, `BlockAbilitiesWithTag`, `CancelAbilitiesWithTag` [web]. A stun is `State.Debuff.Stun` for
  its duration [web]. `RegisterGameplayTagEvent` fires on `NewOrRemoved` or `AnyCountChange` [web]. The rule is
  declarative: the gate lists the tags that block it; the lock writer only adds a tag.

  ```cpp
  ASC->AddLooseGameplayTag(TAG_State_BossIntro);    // count 1
  ASC->AddLooseGameplayTag(TAG_State_BossIntro);    // count 2 (two sources)
  ASC->RemoveLooseGameplayTag(TAG_State_BossIntro); // count 1: still blocked
  ```

- **Unity DOTS: enableable components.** "An entity with component T disabled matches queries as if it doesn't have
  component T at all." `SetComponentEnabled<T>(e, bool)` does not change the archetype or move data, so no ECB or sync
  point is needed; "ideal for states that you expect to change often and unpredictably" [web]. It is one bit, not a
  count [training].
- **flecs: tags and relationship pairs.** A pair is "two elements encoded into a single 64-bit id", for example
  `e.add(LockedBy, BossIntro)`; a query can use `(LockedBy, *)` [web]. Cost: each new combination makes a new table,
  and table churn is expensive; `DontFragment` relationships reduce it [web]. A pair is a set of reasons, not a count
  [training].
- **EnTT: empty-type tags.** "When an empty type is detected, it is not instantiated"; tags filter views:
  `registry.view<Health>(entt::exclude<Invulnerable>)` [web; `exclude` from training].
- **Bevy: markers, States and run conditions.** `Without<Locked>` filters entities; insert and remove are structural
  [training]. Game-wide locks use `States` with `OnEnter`/`OnExit` and `run_if(in_state(..))`; `SubStates` exist only
  inside a parent state; `ComputedStates` are "fully derived from other states" [web]. In Bevy terms, item `012`'s
  freeze table is global states that gate system sets, and per-entity locks are components.
- **Fabian, existence-based processing.** A flag becomes "an implicit boolean hidden in the row existing in the
  table"; a state change "is an insert into a table or a migration from one table to another". Costs: reading an
  enum-like state needs a check of all tables, and moves between tables cost, so states that change very often may
  stay plain values [web].
- **Sonic Mania: a predicate over plain fields.** `Player_Hurt` returns false if
  `state == Player_State_Hurt || state == Player_State_Death || ... || invincibleTimer || blinkTimer > 0`
  [web, `Player.c:2392`]. No lock object: one function reads timers and state. Each new reason edits the function.
- **X6 (repo notes).** 10 stop bytes per object class, a per-object exempt bit `0x08`, a separate input lock; no count
  [code, `docs/mega-refactor/items/012-named-freezes/NOTES.md`].
- **This repo today: last writer wins.** `canBeDamaged` is one `Boolean` in `PlayerStateComponent` [code,
  `players/PlayerStateComponent.kt:11`]. Writers: teleport start and end (`Megaman.kt:507,516`), boss defeat
  (`MegaLevelScreen.kt:808`), intermediate boss death (`:834`), player spawn (`PlayerSpawnEventHandler.kt:119,201`),
  level end (`EndLevelEventHandler.kt:109`), `WilyFinalBoss.kt:298` [code]. A teleport that ends during a boss defeat
  sets `canBeDamaged = true` and cancels the boss lock. `ready` (6 writers) and `movementAllowed`
  (`SpiderWeb.kt:120,153`) have the same shape [code]. `AbstractHealthEntity.invinciblePredicates:
  Array<() -> Boolean>` is already an "OR of sources" lock, but the sources have no names [code,
  `AbstractHealthEntity.kt:31-35`].

### Events: immediate and deferred

- **Bevy.**
  - *Messages* (buffered, pull): `Messages<T>` holds "the messages that occurred within the last two
    `Messages::update` calls"; `MessageReader` "reads messages of type T in order and tracks which messages have
    already been read" [web, Bevy 0.20]. A reader after the writer in the same frame sees the message that frame; a
    reader before it sees it next frame; after two updates it is gone [training for the same-frame rule]. (Earlier
    versions called these `Events<T>`/`EventReader` [training].)
  - *Observers* (immediate, push): `World::trigger` runs observers "immediately"; `Commands::trigger` waits for the
    next sync point; observer order "is considered to be arbitrary"; commands queued by observers apply after all
    observers run, and nested triggers are "evaluated recursively until no commands remain" [web]. `EntityEvent`
    targets one entity and can propagate up `ChildOf` or a custom `Traversal` [web].
  - *Hooks*: one hook per kind per component (a second registration panics); they get a `DeferredWorld`, so
    structural changes go through `.commands()`; `on_add` runs before `on_insert`, `on_discard` before `on_remove`
    [web, 0.20 naming].
  - Re-entrancy rule: a handler cannot change the world structure directly; it queues commands, flushed after it.
- **flecs.** "Observers are always executed when the operation that triggered the observer happens"; in deferred mode
  (inside systems) "invoking the observer will also be delayed", so most observers run at sync points [web]. Custom
  events: `world.event<Synchronized>().id<Position>().entity(e).emit()`; "emit ... invokes observers directly, whereas
  enqueue adds the event to the command queue" [web]. "Observer order is undefined"; OnSet delivery follows emit
  order; hooks run before OnAdd/OnSet and after OnRemove; "Observers should never mutate a component" [web].
- **EnTT.** `dispatcher.trigger(e)`: "Listeners are invoked immediately, order of execution is not guaranteed".
  `enqueue` stores events "until the `update` member function is invoked"; per-type flush with `update<T>()` [web].
  "connecting listeners within event handlers can result in undefined behavior" [web]. Registry signals
  (`on_construct`, `on_update`, `on_destroy`): destroy listeners run "before components have been destroyed"; do not
  remove the same component inside its construct or update listener; adding or removing components in a destroy
  listener can be undefined behavior [web].
- **Unity classic.** `UnityEvent.Invoke` and C# `event` are synchronous [training]. `SendMessage` "calls the
  specified method on every MonoBehaviour attached to the GameObject"; reflection-based and slow [web; cost:
  training]. ScriptableObject event channels: an asset with a delegate; "RaiseEvent ... invokes the delegate"; "the
  event in the middle works like a buffer" [web]; synchronous, in subscription order [training].
- **Godot.** `emit()` calls connected functions at once. `CONNECT_DEFERRED`: "Deferred connections trigger their
  Callables on idle time (at the end of the frame)". `call_deferred` calls run "in the order in which [they were]
  called", and calls deferred from deferred calls run in the same idle cycle; Godot warns about infinite recursion
  [web]. `call_group` "acts immediately on all selected nodes at once"; `GROUP_CALL_DEFERRED` and `GROUP_CALL_UNIQUE`
  give a deduplicated deferred call; `get_nodes_in_group` returns nodes "in scene hierarchy order" [web].
- **Unreal.** Multicast `Broadcast()` is synchronous; "The execution order of bound functions ... is not defined"
  [web]. The Lyra `GameplayMessageSubsystem` has gameplay-tag channels with `BroadcastMessage`/`RegisterListener` and
  exact or partial matches; broadcasters "do not know which objects will receive their messages" [web for the API];
  delivery is synchronous [training]. `FTimerManager::SetTimerForNextTick` is the usual deferral [training].
- **Game Programming Patterns (Nystrom).** "With a synchronous notification, execution doesn't return to the sender
  until all of the receivers have finished"; synchronous cycles "overflow the stack and crash your game"; rule: "avoid
  sending events from within code that's handling one". For queues: "be careful not to assume the current state of the
  world reflects how the world was" at send time; "You only need a queue when you want to decouple something in time"
  [web].

Comparison:

| Mechanism | Sync or deferred | Order rule | Re-entrancy rule | Allocation |
|---|---|---|---|---|
| Bevy `Messages`/`MessageReader` | deferred; read same or next frame by system order | send order per type | 2-update lifetime | double buffer per type, reused [training] |
| Bevy observer (`world.trigger`) | immediate | arbitrary | observer commands flushed after all observers; nested triggers recursive | cached lookup [web] |
| Bevy `commands.trigger` | next sync point | order of the queuing systems | as above | command queue |
| Bevy hooks | immediate, inside the structural change | `on_add` < `on_insert`; `on_discard` < `on_remove` | `DeferredWorld`: no direct structural change | none extra |
| flecs `emit` / observers | immediate (delayed in deferred mode) | undefined; OnSet in emit order | observers must not mutate the component | event data copied when enqueued [web] |
| flecs systems in deferred mode | merged at sync points | declaration order per phase | structure read-only | command queue |
| EnTT `dispatcher.trigger` | immediate | not guaranteed | no connect inside a handler | none |
| EnTT `enqueue`/`update` | deferred to `update()` | queue order per type [training] | same | queue per type |
| EnTT `on_construct`/`on_destroy` | immediate | connection order [training] | no connect/disconnect in a listener; destroy listener only cleans up | none |
| Unity DOTS ECB | deferred to playback | sort key in parallel jobs | one ECB per job | command buffer |
| Unity `UnityEvent` / C# event | immediate | subscription order [training] | none built in | delegate list |
| Unity `SendMessage` | immediate | component order [training] | none | reflection, slow [training] |
| Godot signal | immediate | not stated | none; freed objects disconnect | none |
| Godot `CONNECT_DEFERRED` / `call_deferred` | end of frame (idle) | call order | nested deferred calls in the same idle cycle | message queue |
| Unreal multicast delegate | immediate | undefined | none built in | invocation list |
| Lyra message subsystem | immediate [training] | registration order [training] | none | listener map by tag |
| This repo `EventsManager` | deferred to after the screen render | listener order, then the listener's `eventKeyMask` order (see below) | events submitted during `run()` go to the next `run()` | `Array` per key; `Event` + `Properties` per submit |
| This repo `damageListeners` | immediate | `OrderedSet` order | none | none |

How engines choose:

- **Immediate** when the reaction must see the exact state at the time of the cause and finish before the cause
  continues: a damage negotiation that may cancel damage, a hook that keeps an index up to date, a "player was hit"
  callback to a boss [training; Bevy documents hooks and observers for these uses, web].
- **Deferred** when the reaction changes structure (spawn, destroy), crosses systems, must be batched, or the cause
  runs inside an iteration that must not change. Bevy and flecs force this: structural changes inside systems become
  commands [web].
- No engine guarantees the order of immediate listeners (Bevy, flecs, EnTT, Unreal say so) [web]. An engine that
  needs a fixed order makes it explicit: a chain of systems, or one handler that calls the steps in sequence.
- Re-entrancy is limited by: (a) handlers queue structural changes instead of doing them; (b) no subscribe or
  unsubscribe during dispatch; (c) a depth limit or a "no send from a handler" rule.

A fact about this repo's `EventsManager` (checked in the code): `run()` loops over listeners, then over each
listener's `eventKeyMask`, then over the events for that key (`EventsManager.kt:71-76`). So:

- Within one key, events arrive in submit order.
- Across keys, the order is the listener's `eventKeyMask` iteration order. That is an `ObjectSet`, so hash order, not
  submit order. If a listener subscribes to `A` and `B`, and `B` is submitted first in the frame, the listener can
  get `A` first. For enum keys the hash order is stable within one build, but a change to the mask set can change it
  [code reading; not run].
- An event submitted inside `onEvent` goes to `eventsToAdd` and is delivered at the next `run()`, one more frame
  later (`EventsManager.kt:30`, `:61`).

This explains part of the "fragile event ordering" in item `053`. A fix that keeps one global order (submit order,
then listener order) is an engine change that can change behavior, so it needs its own contract check.

### Querying a player without an interface

- **Bevy:** `Query<(&Transform, &Health), With<Player>>`; one player: `single()`; optional parts: `Option<&Light>` or
  `Has<Light>` [training]. "Player" is a marker component; no `IPlayer`.
- **flecs/EnTT:** a `Player` tag; `registry.view<Position, Player>()`; optional parts: `registry.try_get<Light>(e)`
  [training].
- **Unity DOTS:** `SystemAPI.GetSingletonEntity<PlayerTag>()` or a query with `PlayerTag`; optional parts:
  `HasComponent<T>` [training].
- **Godot:** `get_tree().get_first_node_in_group("player")`, or a group loop in hierarchy order [web]; optional
  features: `has_method` or a child node [training].
- **Unreal:** `PlayerController` → `Pawn`; capabilities are components found with `FindComponentByClass<T>()`, or
  gameplay tags on the ability system [training].
- **Celeste:** `Scene.Tracker.GetEntity<Player>()`, `GetNearestEntity<T>(pos)` [web]; one concrete `Player` class.
- **Sonic Mania:** `foreach_active(Player, player)` over a shared `EntityPlayer`; optional features are fields
  (`shield`, `characterID`) read by global functions [web].

The shared rule: the query asks for **data**. "Is a player" is a marker. A feature such as a light is an optional
component, and the caller checks that it is there. This matches the `048` decision ("a capability lookup with
`getComponent(X::class)`") and `029` review 4 (`capability(type)`).

### Static/global functions: where engines use them, and the risks

Where: stateless helpers over data (Mania `Player_*`, Bevy/EnTT systems, Overwatch "utility functions"); engine-wide
services that are injected, not hidden (Bevy `Res<T>`, Overwatch singleton components, Godot autoloads)
[web/training].

Risks and how engines limit them:

- **Hidden dependencies.** A function that reads a global does not show it in its signature. Bevy's answer: every
  dependency is a parameter (`Res`, `Query`), so the scheduler knows the access and detects conflicts [web]. Nystrom:
  "first consider passing the object to it instead" [web].
- **Testability.** Pure functions are easy to test; a globals pointer "defeats the purpose" (Carmack) [web]. A Kotlin
  top-level function that takes `(entity)` and reads only its components is testable with a fake entity
  (`MegamanHeartTankTest` already builds entities with a relaxed MockK game) [code].
- **Determinism.** The order of global calls is the order of the code; deterministic only if collections iterate in
  a stable order. `029` review 4 warns against iterating `ObjectSet`/`ObjectMap` of players; `EventsManager` has the
  same risk (above).
- **Re-entrancy.** A global function that triggers immediate events can call back into itself. Use the Nystrom rule,
  or queue structural changes as Bevy and flecs do.
- **Mania's limit.** Per-character branches inside shared functions (`characterID == ID_MIGHTY`) grow with each
  character [web]. The fix is data or optional components, not more branches.

### Translation to this codebase (sketches, not decided)

Free functions over components (no engine change). Kotlin top-level or extension functions in core, one file per
concern; they take the entity and read components with `getComponent`/`require`:

```kotlin
// core/.../damage/DamageRules.kt
fun canBeDamagedBy(e: IGameEntity, damager: IDamager): Boolean {
    val h = e.getComponent(HealthComponent::class) ?: return false
    val locks = e.getComponent(LocksComponent::class)
    return h.alive && !h.recovering && (locks == null || !locks.blocks(LockGroup.DAMAGE)) && h.negotiator.get(damager) != 0
}
```

- `IComponentBucket.require` already exists. `getComponent` is one `OrderedMap` lookup by `KClass` plus a cast; fine
  at contact rate; for per-frame loops over many entities, cache the component in `init()`.
- "Plays the same": first move the body of `AbstractHealthEntity.canBeDamagedBy` (`:73`) and the `Megaman.invincible`
  getter (`Megaman.kt:110`) into the function unchanged, then switch the callers (`DamageRule.kt:27`).

Counted locks with reasons (no engine change):

```kotlin
// engine (genre mechanism, no content)
interface LockReason { val index: Int; val groups: Int }   // groups: bit mask of LockGroup
class LocksComponent : IGameComponent {
    private val counts = IntArray(MAX_REASONS)             // by reason.index, no boxing
    private var blockedMask = 0                            // recomputed on change
    fun lock(r: LockReason) { /* count++, update mask */ }
    fun unlock(r: LockReason) { /* count--, never below 0 (log an error), update mask */ }
    fun blocks(group: Int) = blockedMask and group != 0
    override fun reset() { counts.fill(0); blockedMask = 0 }
}
fun lock(e: IGameEntity, r: LockReason) = e.require(LocksComponent::class).lock(r)
```

- The GAS model (a count per tag; the reason declares the groups it blocks) with the X6 groups from `012`.
- An `IntArray` and a bit mask, not an `ObjectMap<Reason, Int>`: a check is one `and`, with no allocation and no hash
  order.
- `reset()` runs on destroy (the engine resets all components), which fixes the stale state for pooled entities.
- Per-entity locks (`DAMAGE`, `MOVE`, `INPUT`, `BEHAVIORS`) go here. Game-wide freezes (pause, room transition) stay
  in the `012` freeze controller: the Bevy split of global states versus per-entity components.
- Not one tag component per reason: `addComponent`/`removeComponent` does not update system membership by itself
  (only `GameEngine.spawnNow` calls `updateSystemMembershipsFor`), and it is easy to forget. A count inside one
  component makes no structural change: the Unity enableable-component reason.
- **"Plays the same" risk.** Today the last writer wins; with counts, the teleport end no longer cancels a boss-defeat
  lock. That is a behavior change in an overlap case. Two ways: (1) port each writer pair 1:1 to `lock`/`unlock` with
  its own reason, test the overlaps in play (teleport during a boss defeat, a spawn during a gate), and log each new
  behavior in `docs/issues/`; or (2) start with an exact-copy mode: one `LEGACY` reason set or cleared like the
  boolean, then move the writers to their own reasons one by one, with a contract check each time.
- `invinciblePredicates` can stay for computed conditions (timers); give each one a name for a debug overlay.
- The input lock (`InputFrameBuilder.locked`) can be driven by the `INPUT` group of the same component or of the
  `012` controller.

A synchronous observer registry next to the buffered one (no engine change; the engine can host it later):

```kotlin
class Signal<T> {                                          // generic
    private val listeners = Array<(T) -> Unit>()           // fixed order = add order
    private var dispatching = false
    private val pendingAdd = Array<(T) -> Unit>()
    private val pendingRemove = Array<(T) -> Unit>()
    fun connect(l: (T) -> Unit) { if (dispatching) pendingAdd.add(l) else listeners.add(l) }
    fun disconnect(l: (T) -> Unit) { if (dispatching) pendingRemove.add(l) else listeners.removeValue(l, true) }
    fun emit(value: T) {
        check(!dispatching) { "re-entrant emit" }         // Nystrom rule; or a depth limit
        dispatching = true
        for (i in 0 until listeners.size) listeners[i](value)
        dispatching = false
        // apply pendingAdd and pendingRemove
    }
}
class PlayerDamaged(var player: IGameEntity? = null, var damager: IDamager? = null) // one reused payload
```

- Rules taken from the engines: a fixed, documented order (stronger than Bevy, EnTT or Unreal give); add and remove
  deferred during dispatch; re-entrant emit forbidden or limited; handlers that spawn or destroy use `spawn`/`destroy`,
  which the engine already queues.
- Allocation: one reused payload per signal; a lambda that captures nothing is a singleton, and one that captures
  `this` is allocated once at `connect`.
- First real callers exist (not speculative): `PlayerStateComponent.damageListeners` (`Megaman.kt:726` →
  `PreciousWoman.onPlayerDamaged`), `onDamagedCallbacks`, `onHealthDepletedCallbacks`. Moving them keeps the same
  timing (immediate) and order (`OrderedSet` add order → `Array` add order).
- Keep `EventsManager` for deferred, cross-system flow (level flow, room transitions). Moving a buffered event to a
  `Signal` changes its timing by up to one frame (two for nested submits), and listeners in `MegaLevelScreen` depend
  on that, so each move needs a contract check.
- Item `057` (typed channels) can host both kinds: a channel declares `Deferred` or `Immediate`. That answers its open
  question "let some channels deliver at once?".

The player as a marker component, not a view interface:

```kotlin
class PlayerComponent(val slot: PlayerSlot) : IGameComponent       // marker + slot
fun IGameEntity.center(): Vector2 = require(BodyComponent::class).body.getCenter()
fun IGameEntity.facingOrNull(): Facing? = getComponent(FacingComponent::class)?.facing
fun IGameEntity.lightOrNull(): PlayerLight? = getComponent(LightComponent::class)?.light
fun IPlayers<*>.entities(): Sequence<IGameEntity>                   // slot order
```

- Outside code (enemies, hazards, HUD, camera) gets an entity from `game.players` in slot order and calls the free
  functions; it never casts to `Megaman`. This is Bevy `With<Player>` plus `Option<&Light>`.
- `MegaPlayer` can shrink step by step: each accessor becomes a free function over a component, and its required
  parts drop to the ones a caller really uses. Facing and direction are interfaces on the entity today; moving them
  into components is `048` work. Until then the free function reads the interface; only its body changes later, not
  the callers.
- Performance: 1-2 players, so a lookup per call is negligible; loop over the slot array, not a hash set. Note: the
  sketch `fun center(): Vector2` allocates; the real form should take an output vector or return the body's cached
  center.
- "Plays the same": each step replaces `player.x` with `x(entity)` and reads the same field.

What needs engine changes:

| Piece | Engine change? |
|---|---|
| Free functions over components | no |
| `LocksComponent` with counts | no (core first; the engine when a second game needs it) |
| A system that skips locked entities without a per-entity check | optional: a mask filter or a predicate hook in `GameSystem.qualifies`; only if profiling shows the check matters |
| `Signal<T>` immediate registry | no (engine if shared) |
| Submit-order delivery in `EventsManager` | yes, with a contract check (it changes order) |
| Hooks on component add/remove (Bevy-style) | yes, in `GameEntity.addComponent`/`removeComponent`; no current caller, so not now |
| `PlayerComponent` marker | no (core); the engine already has `PlayerSlot` |

### Sources

- Bevy ECS quick start: <https://bevy.org/learn/quick-start/getting-started/ecs/>
- Bevy observers: <https://docs.rs/bevy/latest/bevy/ecs/observer/index.html>,
  <https://docs.rs/bevy/latest/bevy/ecs/observer/struct.Observer.html>
- Bevy `EntityEvent`: <https://docs.rs/bevy/latest/bevy/ecs/event/trait.EntityEvent.html>
- Bevy messages (0.20): <https://docs.rs/bevy/latest/bevy/ecs/message/index.html>
- Bevy lifecycle and hooks: <https://docs.rs/bevy/latest/bevy/ecs/lifecycle/index.html>,
  <https://docs.rs/bevy/latest/bevy/ecs/lifecycle/struct.ComponentHooks.html>
- Bevy states: <https://docs.rs/bevy/latest/bevy/state/index.html>
- Bevy `run_if` and ordering: <https://docs.rs/bevy/latest/bevy/ecs/schedule/trait.IntoScheduleConfigs.html>
- flecs observers: <https://www.flecs.dev/flecs/ObserversManual.html>
- flecs relationships: <https://www.flecs.dev/flecs/Relationships.html>
- flecs systems: <https://www.flecs.dev/flecs/Systems.html>
- EnTT events and signals: <https://github.com/skypjack/entt/wiki/Events,-signals-and-everything-in-between>
- EnTT ECS: <https://github.com/skypjack/entt/wiki/Entity-Component-System>
- Unity enableable components:
  <https://docs.unity3d.com/Packages/com.unity.entities@1.3/manual/components-enableable-intro.html>,
  <https://docs.unity3d.com/Packages/com.unity.entities@1.3/manual/components-enableable-use.html>
- Unity ECB: <https://docs.unity3d.com/Packages/com.unity.entities@1.3/manual/systems-entity-command-buffers.html>
- Unity `SendMessage`: <https://docs.unity3d.com/ScriptReference/GameObject.SendMessage.html>
- Unity ScriptableObject event channels: <https://unity.com/how-to/scriptableobjects-event-channels-game-code>
- Godot `Object` (signals, `CONNECT_DEFERRED`, `call_deferred`):
  <https://docs.godotengine.org/en/stable/classes/class_object.html>
- Godot signals tutorial: <https://docs.godotengine.org/en/stable/getting_started/step_by_step/signals.html>
- Godot `SceneTree` (groups, `call_group`): <https://docs.godotengine.org/en/stable/classes/class_scenetree.html>
- Unreal multicast delegates: <https://dev.epicgames.com/documentation/en-us/unreal-engine/multicast-delegates-in-unreal-engine>
- GAS documentation (tranek): <https://github.com/tranek/GASDocumentation>
- Gameplay Message System (community wiki): <https://unrealcommunity.wiki/gameplaymessagesystem-74b916>
- Game Programming Patterns, Event Queue: <https://gameprogrammingpatterns.com/event-queue.html>
- Fabian, Data-Oriented Design, existence-based processing: <https://www.dataorienteddesign.com/dodbook/node4.html>
- Carmack, Functional programming in C++ (repost): <http://sevangelatos.com/john-carmack-on/>
- Overwatch Gameplay Architecture and Netcode (GDC 2017):
  <https://gdcvault.com/play/1024001/-Overwatch-Gameplay-Architecture-and>
- Monocle `Tracker.cs`: <https://github.com/JamesMcMahon/monocle-engine/blob/master/Monocle/Util/Tracker.cs>
- Sonic Mania decompilation (`Player.c`, `Player.h`, `Motobug.c`): <https://github.com/RSDKModding/Sonic-Mania-Decompilation>
  (read for facts only; nothing copied)
- Mike Acton, Data-Oriented Design and C++ (CppCon 2014): <https://www.youtube.com/watch?v=rX0ItVEVjHc> [training;
  not fetched]

Gaps: not confirmed from a fetched page: that the Lyra message subsystem is synchronous, that Godot signal emission
runs listeners in connection order, and what the Overwatch talk says about singleton components (marked [training]).

### Correction: "or null" lookups versus query filters (2026-10-26)

The owner asked if the "or null" form (`facingOrNull()`, `lightOrNull()`) matches other engines. It does, for one
entity: most engines have an optional lookup that returns an empty result. But engines do not use it as the main tool
for parts that a caller needs.

| Engine | Optional lookup | Result when missing |
|---|---|---|
| Bevy | `world.get::<T>(entity)`; `Option<&T>` in a query; `Has<T>` | `None`; `false` for `Has` |
| EnTT | `registry.try_get<T>(entity)`; `all_of<T>` | `nullptr`; `false` |
| flecs | `entity.get<T>()` (newer versions: `try_get`) | `nullptr` |
| Unity | `TryGetComponent<T>(out T)`; `GetComponent<T>()` | `false`; `null` |
| Unreal | `FindComponentByClass<T>()` | `nullptr` |
| Godot | `get_node_or_null(path)`; `has_method(name)` | `null`; `false` |

Bevy `Option<&T>`/`Has<T>` and Unity `TryGetComponent` are [web] (sources in the section above); the other rows are
[training].

ECS engines handle a missing part mostly at the query level: a system asks for "entities that have a body and a
facing", and an entity without them is not visited, so the loop body has no null checks. Nullable lookups are for
truly optional parts. So the direction uses two forms:

1. **Parts a caller needs** (an enemy that aims needs the player's body and facing): filter, do not null-check. For
   example a loop over the players that have the part, or a function that skips players without it. A player with
   no body is then "not targetable" in one place, not through `?.` at about 414 call sites.
2. **Optional features** (light, gravity device, loadout): `lightOrNull()`; `null` means the feature does nothing
   here. A default is `lightOrNull() ?: PlayerLight.STANDARD` at the call site; no separate API.

Performance: a nullable enum (`Facing?`) does not box; a nullable value class (`InputFrame?`) does. So nullable
returns do not suit input frames (this is why `ControllerSystem` checks the source first and then reads the frame).

This corrects point 2 of the owner-facing summary ("free functions over components ... return null when the feature
is missing").
