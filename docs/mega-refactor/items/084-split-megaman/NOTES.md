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
