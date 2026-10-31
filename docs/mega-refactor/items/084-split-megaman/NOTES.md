# Notes: 084 Split `Megaman` into player components

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Scan of `Megaman.kt` (2026-10-30, `master` at `a5deca7ce`)

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`. Line numbers are in `M/Megaman.kt`.

### Counts

- 990 lines. 104 property declarations (`val`/`var`, all visibilities).
- The class line names `AbstractHealthEntity` and 17 interfaces (`:87-89`): `IBodyEntity`, `ISpritesEntity`,
  `IBehaviorsEntity`, `IPointsEntity`, `IAudioEntity`, `IAnimatedEntity`, `IScalableGravityEntity`, `IFreezableEntity`,
  `IGameStateListener`, `IEventListener`, `IFaceable`, `IDirectional`, `IFocusable`, `IDamagerEntity`, `IGateTrigger`,
  `ICartRider`.

### Where the logic is

| Concern | Lines | Callers outside `Megaman.kt` | Other item |
|---|---|---|---|
| Property-bag state: `facing`, `aButtonTask`, `currentWeapon`, `running` | `:266-291` | `aButtonTask`: 17 lines in 7 files (`AirDashBehavior`, `BodyComponent`, `ClimbBehavior`, `GravityCapsule`, `JumpBehavior`, `SwimBehavior`, `WallSlideBehavior`) | |
| Teleport hooks as property lambdas | `:492-505` | `Teleports.kt` reads them | |
| `direction` setter: camera rotation | `:206-230` | | `058` |
| `direction` setter: signed gravity and jump table | `:233-263` | | `060` (values) |
| Charge state, `ammo`, `shoot()`, `translateAmmo` | `:139-196, 941-956` | | `024` (open) |
| Post-action momentum | `:149-150, 769-788, 958-985` | 15 lines in `AirDashBehavior`, `BodyComponent`, `ControllerComponent`, `GroundSlideBehavior`, `JumpBehavior` | |
| Room-transition velocity carry | `:546-596` | `roomTransPauseTimer`: `MegamanAnimationKeySupplier` | `053` (umbrella only) |
| Stun (`STUN_PLAYER`) | `:604-642, 751` | `stunTimer`: 5 lines in `RodentMan` | |
| Damage knockback, `noDmgBounce`, freeze on `IFreezerEntity` | `:663-710` | | |
| Recovery flash | `:755-763` | `recoveryFlash`: `SpritesComponent` | `017` (damage blink, not named) |
| Freeze and mash-to-unfreeze, ice shards | `:814-834` | none | `048`/`063` (`IFreezableEntity`) |
| `editDamageFrom` (enhancement, hard mode) | `:712-724` | | `023` |
| `canBeDamagedBy` owner walk | `:652-659` | | `030` |
| Death explosion orbs, Mario sound | `:526-542` | | `025` (Mario check) |
| Out-of-bounds kill | `:738-744` | | |
| Trail sprites | `:806-810, 859-886` | none | `024` (weapon checks) |
| Underwater bubbles | `:790-804, 888-902` | none | |
| `AButtonTask` transitions | `:840-855` | the behaviors set it too | `069` (dash button) |
| Focus position | `:904-918` | `CameraManagerForRooms`, `EntityQueries` | `051` |
| Heart tanks, currency sound, weapon rewards | `:920-939` | | `059` |
| Manual resets in `onSpawn` | `:445-490` | | |

### Notes

- The updatables lambda (`:735-857`) holds about 10 unrelated concerns. It is the largest single block to split.
- A component's `reset()` runs on destroy (the engine resets every component). Each concern that moves into a component
  takes its reset lines out of `onSpawn`. Check the order: `onSpawn` resets some timers with `reset()` and others with
  `setToEnd()`, and a component must start in the same state.
- The stun impulse assumes `UP` (`TODO` at `:634`). Keep it as is in this item.
- Prior analysis: `029` `NOTES.md`, "Review 1: the inside of the player", section 2 (b), "Shared machinery other
  characters can reuse", lists most of the concerns above. `029` and `061` did sections (d) and (e) only.

## Design research (2026-10-30, `master` at `5d4a37eac`)

Two read-only research passes. Nothing here is a runtime measurement. Only the decisions in `ITEM.md` are approved; the
rest is a proposal for each branch `SCOPE.md`. Line numbers are in `M/Megaman.kt` at that commit.

### The form of each part

- A component holds the data; its methods touch only its own fields, and it holds no entity reference. A `<Name>s.kt`
  file of free functions touches the body and other components (`LocksComponent` + `Locks.kt`, `TeleportableComponent`
  + `Teleports.kt`).
- Character effects (sound, spawns, input, impulse) are constructor hooks. Values are constructor arguments, set in
  `define<X>Component()` functions in `M/components/`. Only these functions read `MegamanValues`, and they are the
  future seam for `060`.
- All parts go in `core`, in `entities/state/`. Those files must not import `Megaman`, `MegamanValues` or `MegamanWeapon`.
- `Megaman`'s updatables lambda stays as the schedule: one call per part, at the same position.
- Rejected: a `GameSystem` per mechanism (it cannot run in the middle of the lambda, and other entities read the
  player during `UpdatablesSystem`; `017` also says that sense and velocity logic does not go in a per-frame system);
  a handler typed on `Megaman`; a component that holds the entity; `by` delegation; `IBehavior` for stun and freeze
  (`BehaviorsSystem` runs before `WorldSystem`); one `DamageReactionComponent` (`048` would have to split it).

### Frame-order constraints in the lambda (`:730-849`)

- The base lambda runs first, so `damageTimer.isJustFinished()` is valid in `Megaman`'s lambda in that frame.
- The out-of-bounds check stays first, because `destroy()` changes `existenceState` at once.
- `:744` runs before `:746`. `:745` runs before `:748` and may move after `:746`. `:756` stays outside the `:748` guard.
- `:759` runs before `:799-803`. `:831` runs before `:833`.
- The thaw stays inside `UpdatablesSystem`, because the animation and sprite systems read `frozen` in the same frame.
- `STUN_PLAYER` arrives in `events.run()`, so `Megaman` stays the listener.

### Corrections to the scan above

- `RodentMan`, `PreciousWoman` and `BigAssMaverickRobot` have their own private `stunned`. The player's outside readers
  are `ControllerComponent` and `MegamanAnimationKeySupplier`.
- `frozen` has outside readers: `BodyComponent:374,378`, `SpritesComponent:101`, `MegamanAnimationKeySupplier:39`,
  `MegamanWeaponsHandler:585`.
- Momentum has 9 outside lines. The class line has 16 interfaces.
- An entity holds one component per class, so a generic emitter cannot be added twice.
- `trailSpriteTimer` and `underWaterBubbleTimer` are not reset in `onSpawn`, so they survive a respawn.
- The 15 bag calls in the `084a` backlog candidate include keys on events and on other objects. Recount them.

### Proposals for the later branches

- Movement: `PostActionMomentumComponent` and `PostActionMomentum.kt` with the same function names, so that most callers
  change only an import. `RoomTransitionCarryComponent`, with a typed field in place of the 2 bag keys on the body.
  `Megaman.updateAButtonTask()` with no component, because `069` gives X a separate dash input.
- Effects: `BubbleEmitterComponent` and `TrailSpriteComponent`, each with a hook and a `reset()` that does nothing;
  `isOutsideLevel(...)` with no component (it is a level rule, not an effect); `ExplosionOrb.spawnRing(center)`, which
  also replaces the same table in `AbstractBoss`.
- Possible bug, read and not reproduced: if Mega is in the air at `BEGIN_ROOM_TRANS` and over ground at `END_ROOM_TRANS`,
  `:576` or `:580` hits `!!` on `null`.

### Invincibility research

- `invincible` is derived everywhere: 1 base getter and 17 overrides in `core/src/main`, with no side effects.
  8 use `super.invincible || x`, 4 have an own timer, 4 are constants, and `Megaman` replaces the base rule.
- `invinciblePredicates` has 2 writers (`AbstractEnemy:137`, `FreezableEntityHandler:38`), and `Megaman` does not read it.
- Mega is not invincible while stunned or frozen. Enemies with a handler and the 3 stunned bosses are. So a part cannot
  add a source; the entity must decide.
- A list of OR terms cannot express the 4 constants: `BulbBlaster` is always `false`.
- `!inPlay` is required: it is what protects Mega during the beam-in. The `SPAWN` damage lock
  (`PlayerSpawnEventHandler.kt:127`) appears to have no effect, because `onSpawn` clears it (`:471`). Traced, not run.
- A read must show a hit at once, because a later contact in the same pass must see it. Do not cache it per frame.
- A merged timer for damage and recovery moves the end of invincibility by 1 frame at 60 fps. Do not merge them.
- A mask of reasons exists for the player as `MegaLockGroup.DAMAGE`. It does not fit all entities, because timers end
  with no write.
- X6 has no general invincibility: each hit source decides (`~/IdeaProjects/mmx-demake-tools/docs/engine.md`, "Damage
  to an object" and "Damage to the player").

## Movement multipliers (2026-10-30, at `ba4b3ca1c`)

This item does not entail a modifier component. `084c` leaves out `getMaxRunSpeed()` and the scalars, and it stays as it
is. The owner wants the component later: see item `086`, which has the inventory. A typed field move would fit this
item in a new branch, but it gives no outside hook.

## Interfaces and a second playable entity (2026-10-31, `master` at `96060fe5d`)

One read-only research pass, not approved. The owner asked whether an item already reduces the interfaces on `Megaman`, and whether the work fits in
`084` from the angle of more than one playable entity. No game ran. The owner's decisions are open.

### What blocks a second player

The 15 interfaces are not the main block: 6 are accessors with default methods, which cost one word on a class line. The main blocks are 57 `Megaman` receivers,
42 members that exist only on the class, 20 casts (19 lines) and 15 `Megaman::class` keys. Of the 57 receivers, 26 read no class-only member. The 15 cast lines that read weapon data
are `024` work. The 8 behaviors read weapon, movement and signed values, and 5 of them read `aButtonTask`, so they stay on the `Megaman` receiver until `024` and `060`.

`entities/state/` (22 files) and `players/` (7 files) name no `Megaman`.

### Who owns each interface

| Interface | Owner | Plan |
|---|---|---|
| `IPointsEntity` | none | Remove; `IHealthEntity` already gives it |
| `IAnimatedEntity` | none | Remove; 2 use sites |
| `IBehaviorsEntity` | none | Remove and delete; 1 implementer; engine queries exist |
| `IGateTrigger`, `ICartRider` | none | Remove and delete; 1 implementer; use `isPlayer` (`014d` added them before `isPlayer` existed) |
| `IFocusable` | `051`, `085` | A component implements it; 1 entity implementer |
| `IGameStateListener` | `059` in part | A listener in a component; 1 implementer; `MegamanMaverickGame.kt:444` names `megaman` |
| `IScalableGravityEntity` | `086`, `048` | Later |
| `IFaceable` | `048` and `063` disagree | Later |
| `IBodyEntity`, `ISpritesEntity`, `IAudioEntity` | `048` | Keep. `GameEntityUtils.kt:149,161` use `is ISpritesEntity`, so a removal stops the teleport hide with no error |
| `IDirectional`, `IEventListener` | `058`, `060`; `053`, `057` | Keep; true capabilities |
| `IDamagerEntity` | `023`, `048` | Keep while the `DAMAGER` fixture is on the body |

### Design options

A thin `Megaman` and `Protoman` with components and shared functions fits all decisions. One `PlayerEntity(services, def)` needs `CharacterDef` first, so that choice belongs to `060`. A shared
`IPlayerEntity` (`029`, `048`) and a player base class (`084`, `029`, `061`) are ruled out. The branches below serve both open options.

### Test for the work in `084`

Work belongs in `084` only if existing logic moves or code is removed, and `Megaman` or a screen calls the result today. `CharacterDef`, supplier parameters, a shared `onSpawn` and `Protoman` go in
`060`. Weapon data goes in `024`. Rules for all entities go in `048`.

### Proposed branches

- `084e-class-line`: the class line goes from 15 to 10 interfaces. Remove the 5 interfaces above, and delete `val Megaman.input`, the fixture accessors that copy `EntityQueries.kt`, and the dead `spawningTimer`,
  `wallSlideGravity` and `M/components/PointsComponent.kt`. Widen the 3 sprite helpers, so 4 casts go. New API: `IGameEntity.resetBehavior(key)`. It needs a `cart` test room.
- `084f-focus-progress`: 10 to 8 interfaces. `FocusComponent` and `PlayerProgressComponent`. The dead `ConstKeys.FOCUS` reads go, which closes `085`.
- `084g-movement-state`: `canMove` and `RunComponent`; the 2 crouch timers move into `crouchBehavior()`; `ANIMATION_KEY` and `DIRECTION_ON_AIR_DASH` become typed; `runLeft` and `runRight` become one function (keep the `RODENT_CLAWS`
  difference at `M/components/ControllerComponent.kt:163-168,207-210`). Start after `031e` merges.
- `084h-charge`: only if the owner decides that the charge state moves here. `ChargeComponent`, with the charge rules left for `024`.

9 of the 15 bag lines are weapon lines and go to `024`; 4 go to `084g`, and 2 go to `084f`.

### Silent failures for `060`

A new player class compiles and then plays wrongly here: exact class matches (`AbstractHealthEntity.kt:75`, `UnderwaterFan.kt:224`, `PreciousGem.kt:257`, `DarknessV4.kt:421`); `MegaContactFilter.kt:110-116` (`EntityType.MEGAMAN`);
`MegamanMaverickGame.kt:444`; `entities/EntityQueries.kt:42` (`shooting`); `GameEntityUtils.kt:149,161`; `WorldGravity.kt:14`.

### Stale lines

`084` `ITEM.md`: "990 lines, 17 interfaces". `060` `ITEM.md` and `029` `NOTES.md`: `MegaPlayer.megaman`. `030` `ITEM.md`: "37 `is Megaman`" (now 0). `029`: the title and scope say `IPlayer`.
