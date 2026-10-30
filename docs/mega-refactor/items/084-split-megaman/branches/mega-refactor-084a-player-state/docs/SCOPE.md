# Scope: mega-refactor-084a-player-state

- Status: approved
- Approved: 2026-10-30

## Goal

Take the player state and the teleport hooks out of the entity property bag. `facing`, `aButtonTask`,
`currentWeapon` and `running` become typed fields on `Megaman`. The teleport hooks become typed hooks on
`TeleportableComponent`. Mega plays the same.

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Typed state in place of the property bag:** `facing`, `aButtonTask`, `currentWeapon` and `running` are read and
> written through `getProperty`/`putProperty` (`MegamanProps`, `ConstKeys.RUNNING`). The teleport hooks are lambdas
> stored as properties (`ConstKeys.ON_TELEPORT_START`, `ON_TELEPORT_END`). Give them typed fields or components.

## Current state

Scan on 2026-10-30, branch at `c6dd8b109`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`M/` = `C/entities/megaman/`.

### The 4 fields

They are properties with a getter and a setter on `Megaman` (`M/Megaman.kt:266-291`). Only the bodies use the bag:

| Field | Storage | Notes |
|---|---|---|
| `facing` (override of `IFaceable`) | `MegamanProps.FACING` | First written in `onSpawn` (`:447`). A read before that throws (`null as Facing`). |
| `aButtonTask` | `MegamanProps.A_BUTTON_TASK` | Written in `init()` (`:413`), by the `GravityDeviceComponent` lambda (`:415-420`) and in `onSpawn` (`:452`). |
| `currentWeapon` | `MegamanProps.WEAPON` | The setter calls `weaponsHandler.onChangeWeapon(value, previous)` before it stores the value. On the first set (`init()`, `:411`) `previous` is `null`. The getter uses `!!`. |
| `running` | `ConstKeys.RUNNING` | Getter: `inPlay && getProperty(...)!!`. First written in `onSpawn` (`:459`). |

- The callers use the property names, not the keys. The keys `MegamanProps.*` and `ConstKeys.RUNNING` have no reader
  outside `M/Megaman.kt`. So the callers do not change: `aButtonTask` has 17 uses in 7 other files, `currentWeapon`
  has 55 in 20 other files, `running` (of `Megaman`) has readers in `ControllerComponent`, `MegamanWeaponsHandler`,
  `MegamanAnimationKeySupplier`, `MegaLevelScreen:397` and others.
- `MegamanProps` (`M/constants/MegamanProps.kt`) has only these 3 keys and only `Megaman.kt` uses it.
- The engine never clears an entity's property bag, and `Megaman` is not pooled (`MegamanMaverickGame.kt:425-428`). So
  a value in the bag and a value in a field live equally long.

### The teleport hooks

- `Megaman.onSpawn` puts 3 lambdas in the bag on each spawn (`M/Megaman.kt:492-505`):
  - `ON_TELEPORT_START`: `stopCharging()`, `standardOnTeleportStart(this)`, reset of `AIR_DASHING`, damage lock.
  - `ON_TELEPORT_CONTINUE`: through `setStandardOnTeleportContinueProp(this)` (`C/entities/utils/GameEntityUtils.kt:151`),
    which calls `standardOnTeleportStart(entity)`.
  - `ON_TELEPORT_END`: `stopCharging()`, `standardOnTeleportEnd(this)`, `aButtonTask = AIR_DASH`, damage unlock,
    `services.camera.focusSnappedAway = false`.
- Readers:
  - `beginTeleport` and `endTeleport` (`C/entities/state/Teleports.kt:14-24`) call the start and end hooks, then set or
    clear `TeleportableComponent.teleporter`.
  - `ON_TELEPORT_CONTINUE` is read with a cast in 5 places: `CapsuleTeleporter.kt:224,250`, `PipePortal.kt:195,225`,
    `PortalHopper.kt:244`.
- `TeleportableComponent` (`C/entities/state/TeleportableComponent.kt`) holds only `teleporter`; `reset()` clears it.
  Only `Megaman` adds it (`M/Megaman.kt:338`). `beginTeleport` requires it, so only `Megaman` can be teleported.
- The lambdas capture only `this`, so the lambdas made on each spawn do the same thing every time.
- `TeleportsTest` (`core/src/test/.../entities/state/TeleportsTest.kt`) puts the hooks in the bag to test the order:
  the start hook runs before `teleporter` is set, the end hook before it is cleared.

### Problems

- The type of each value is only in a cast at the read (`as Facing`, `as? () -> Unit`). A wrong type or a missing key
  fails at run time, or the hook silently does not run.
- The teleport hook keys are strings in `ConstKeys`, in the same space as about 400 other keys.

## In scope

1. **The 4 fields become typed fields on `Megaman`.** The getters and setters keep their names and their behavior:
   - `facing`: `override lateinit var`. A read before the first spawn still throws.
   - `aButtonTask`: `lateinit var`.
   - `currentWeapon`: a private nullable backing field. The setter still calls `onChangeWeapon(value, previous)` before
     it stores the value, with `previous = null` on the first set. The getter still uses `!!`.
   - `running`: a private `Boolean` backing field (default `false`). The getter is still `inPlay && <field>`.
2. **Delete `MegamanProps`** and **`ConstKeys.RUNNING`**. They have no other user.
3. **The teleport hooks become constructor parameters of `TeleportableComponent`**: `onStart: ((ITeleporterEntity) ->
   Unit)?`, `onContinue: (() -> Unit)?`, `onEnd: (() -> Unit)?`, each `null` by default. `reset()` still clears only
   `teleporter`; the hooks stay, because they are set once.
   - `Megaman` passes its 3 hooks where it adds the component in `init()` (`M/Megaman.kt:338`). The lambda bodies do
     not change. The 3 `putProperty`/`setStandardOnTeleportContinueProp` lines leave `onSpawn`.
   - `beginTeleport` and `endTeleport` call `teleportable.onStart` and `teleportable.onEnd`, in the same order as now.
   - The 5 continue reads call `entity.getComponent(TeleportableComponent::class)?.onContinue?.invoke()`.
4. **Delete** `ConstKeys.ON_TELEPORT_START`, `ON_TELEPORT_CONTINUE`, `ON_TELEPORT_END` and
   `setStandardOnTeleportContinueProp`. `standardOnTeleportStart` and `standardOnTeleportEnd` stay (they are the hook
   bodies).
5. **Tests:** change `TeleportsTest` to give the hooks to the component, and add a check that `reset()` keeps the
   hooks. Add a test with `testServices()` for the `currentWeapon` setter: the handler sees the old weapon as
   `previous`. The contract decides the final list.

### New API and its first caller

| API | First caller |
|---|---|
| `TeleportableComponent.onStart`, `onEnd` | `beginTeleport`, `endTeleport` (`Teleports.kt`); written by `Megaman.init()` |
| `TeleportableComponent.onContinue` | `CapsuleTeleporter`, `PipePortal`, `PortalHopper`; written by `Megaman.init()` |

No other new type, function or field outside `Megaman`.

## Out of scope

- A component for the 4 fields (for example a player state component that `060` Proto Man shares). Today only
  `Megaman` and its own behaviors use them. A later 084 branch decides it when it has a second user.
- The other keys that `Megaman` and its parts keep in the bag: `ConstKeys.FOCUS` (`051`),
  `MegamanKeys.DIRECTION_ON_AIR_DASH`, the `SHOOT_DOWN` keys, `ConstKeys.ANIMATION_KEY`, `ConstKeys.SIDE` and others
  (15 bag calls in `M/`). They go to `docs/TODO.md` as candidates for later 084 branches.
- The `focusSnappedAway` write in the end hook. It moves with the lambda as it is; `051b` changes it.
- `PipePortal.kt:129` sets `teleporter` directly without the hooks. That stays.
- All other parts of the 084 `ITEM.md` (damage reaction, momentum, effects, `AButtonTask` transitions, resets).

## Affected areas and risks

Files: `M/Megaman.kt`, `M/constants/MegamanProps.kt` (deleted), `C/Constants.kt`,
`C/entities/state/TeleportableComponent.kt`, `C/entities/state/Teleports.kt`, `C/entities/utils/GameEntityUtils.kt`,
`C/entities/special/CapsuleTeleporter.kt`, `PipePortal.kt`, `PortalHopper.kt`, `TeleportsTest.kt`, and one new test.

Risks:

- **Hook timing.** The hooks are now made in `init()`, not on each spawn. They capture only `this`, and nothing else
  writes them, so the hook that runs is the same. A component `reset()` that cleared the hooks would break teleports
  after the first death; the tests check that it does not.
- **Weapon setter order.** `onChangeWeapon` must still see the old weapon and run before the store (it resets
  `shooting` and despawns weapon entities of the old weapon).
- **A read before the first spawn.** Today it throws for `facing`, `aButtonTask` and `currentWeapon`. With `lateinit`
  and `!!` it still throws. `running` returned `false` or threw; now it returns `false`. No caller reads `running`
  before the first spawn, because `inPlay` is `false` then.
- Gameplay that the change can touch: teleporters (`CapsuleTeleporter`, `PipePortal`, `PortalHopper`), weapon switch,
  the A button task after a teleport, a gravity change and a wall slide, running, facing at spawn.

## Overlap with other branches

See `docs/NOTES.md`. In short: `031d` has no overlap. `051a` changes `M/Megaman.kt:214` only. `051b` (not started)
changes the `focusSnappedAway` write that this branch moves into `init()`.

## Open questions

None. The 2 questions of the draft are answered in `docs/NOTES.md`, "Decisions".
