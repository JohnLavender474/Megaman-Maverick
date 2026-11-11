# Players

This package holds the players service (`MegaPlayers`) and the rules that every player class shares: how a player
spawns, dies, moves through a room transition, takes damage and is blocked.

A second playable character must not copy `Megaman.kt`. Each player class extends the abstract base class
`MegaPlayer` (`O8-R1` allows a thin base). `MegaPlayer` owns the lifecycle that every player runs: the final
`onSpawn`, `onDestroy`, `onEvent` and `takeDamageFrom`, the shared components and the shared damage rules. A class
gives its own parts through abstract members (body, momentum, knockback, stun, behaviors, controller, sprites,
animations, the camera focus height) and puts its own lines in open hooks (`onPlayerSpawn`, `onPlayerDestroy`,
`onPlayerEvent`, `afterPlayerDamage`, `onLevelDefeated`, `teleportableComponent`). The moves that only some characters
use are plain functions in this package, and a class opts in by calling them.

The research and the decisions are in `docs/mega-refactor/items/081-playable-x/` (branch `081a`) and
`docs/mega-refactor/items/060-proto-man/` (branch `060j`, the base class; branch `060b`, the first class that only
calls the shared moves).

The examples in this file are hypothetical. The real code is in `MegaPlayer.kt`.

## Status

`Megaman`, `MegamanX` (`081b`) and `Protoman` (`060b`) are the player classes, all on `MegaPlayer` (`060j`). The
classes show the two forms of a class: X has its own body and run (the X6 feet and walk); `Protoman` moves like Megaman,
so it has no move code of its own. It calls `playerBody`, `playerRunButton` and `playerJumpBehavior` with its own
values file, and gives only its values, its sprites and its list of moves. `MegaPlayer` reads the
shared values (damage time, start health, freeze) from `MegamanValues` until a character needs other values. Some rules still have another owner, and a `TODO` at the
function names it: the difficulty change of the damage (`023`), the owner chain of a shot (`030`), the death event
(`119`), the health base class (`048`), and the locks for stun, freeze and damage (`126`). Only slot 0 can change its character, at the level start (`047` for co-op, `113` for a select screen).

## The parts

```mermaid
flowchart LR
    Class["a player class<br/>(for example Megaman or Protoman)"] -- "extends" --> Base["MegaPlayer<br/>lifecycle, shared components,<br/>damage rules, hooks"]
    Base -- "damageNegotiator" --> Negotiator["PlayerDamageNegotiator<br/>the damage table for players"]
    Class -- "its behaviors call" --> Moves["PlayerJump.kt, PlayerWallSlide.kt, PlayerAirDash.kt<br/>each with the class's params"]
    Class -- "its run buttons" --> Run["PlayerRun.kt<br/>the run gate and buttons"]
    Run -- "calls" --> Step["entities/movement/Run.kt<br/>runStep, maxRunSpeed, RunParams<br/>for any body"]
    Class -- "defineBodyComponent" --> Body["PlayerBody.kt<br/>shared fixtures and pre-step,<br/>with PlayerBodyParams and hooks"]
    Body -- "reads" --> Gravity["PlayerGravityComponent<br/>gravity table, signed by direction"]
    Moves --> Input["PlayerInput.kt, the air dash input reads<br/>the run side, A or SELECT"]
    Game["the game, at level start"] -- "putInSlot0" --> Roster["PlayerRoster<br/>one entity per PlayerCharacter;<br/>slot 0 gets the level's character"]
    Base --> State["the mechanism components<br/>(entities/state/)"]
    Moves --> State
```

## The order of a spawn

`MegaPlayer.onSpawn` is final. The class lines go in the `onPlayerSpawn` hook, between the shared spawn start and
the shared spawn end.

```mermaid
sequenceDiagram
    participant P as MegaPlayer.onSpawn
    participant C as the class hook
    P->>P: max health from the heart tanks
    P->>P: super.onSpawn (health to max, damage timers)
    P->>P: spawn start
    Note over P: listener, body position and physics, facing,<br/>world gravity, health, lock clears, teleporter
    P->>P: damage timer to the end
    P->>C: onPlayerSpawn(spawnProps)
    Note over C: its own lines (for example weapons, timers)
    P->>P: momentum reset
    P->>P: spawn end (camera follow stop)
```

A spawn sends no level event. The respawn routine sends `LEVEL_RESET` when it resets the level (see `screens/levels/respawn/README.md`).

The other final methods follow the same form: `onDestroy` runs the shared destroy and then `onPlayerDestroy`;
`onEvent` runs the shared case for each key and then `onPlayerEvent`; `takeDamageFrom` runs the shared damage tail,
then `afterPlayerDamage`, then the damage sound.

A hypothetical player class:

```kotlin
class Dasher(services: MegaServices) : MegaPlayer(services) {

    override val cameraFocusHeight = 1.5f

    override fun onPlayerSpawn(spawnProps: Properties) {
        dashTimer.reset()
    }

    override fun afterPlayerDamage(damager: IDamagerEntity) {
        dashTimer.setToEnd()
    }

    // ... the abstract define...Component members
}
```

## Rules a change must keep

- **`MegaPlayer` holds only what every player class shares.** A part that one class has, or that has different
  values in two classes, stays in the class (an abstract member or a hook). Examples that stay out: the charge, the
  weapons, the gravity scalar (`IScalableGravityEntity`), `direction`.
- **A function that every player runs as part of the lifecycle is a private member of `MegaPlayer`.** A move that
  only some characters use is a plain function in `players/` or a generic helper, and a class opts in by calling it.
- **A class line goes in its hook at the same point.** The hooks run where the class lines ran before the base
  class. A new hook needs a caller in a player class.
- **Compute, do not store.** `MegaPlayer.invincible` and `playerCanMove` read their state at each call. A timer that ends
  writes nothing, so a stored value goes stale.
- **Fail fast.** The rules read their components with `require(...)`, not with the queries that return a silent
  default. A player class that lacks a component throws at once. The components that each function needs are the
  ones it reads with `require`.
- **The gravity set comes before the lock clears** in the spawn start of `MegaPlayer`. The gravity set can start a camera turn
  that locks `MOVEMENT`, and the lock clear must remove it.
- **One copy.** A rule that two player classes need goes here, not into each class. A difference between the
  classes is a parameter (for example `JumpParams`, `WallSlideParams`, `AirDashParams`), not a type check in the shared
  code. A shared behavior keeps the gate and the physics that every class has; the params give the class's own start
  rules, speeds, timers and side effects.
- **The body keeps its fixture order.** The order of the fixtures sets the order of the contacts. `playerBody` adds
  the shared fixtures first; a class adds its own fixtures after the call, and its own pre-step lines in the
  `onPreStep` hook, which runs where Megaman's old pre-step ran them (before the body size and the out-of-play
  velocity zero). `MegamanBodyFixturesTest` and its reference file `megaman-body-fixtures.txt` hold Megaman's
  order, sizes and gravity.
- **A class with its own move set keeps its own body.** X has its own body and run (X6 feet and walk). The shared
  player run gate and body are for a class that moves like Megaman: such a class (for example `Protoman`) calls
  them with its own params. `runStep` in `entities/movement/` has no player rules, so any entity with a body and a
  `RunComponent` can run like Megaman.
- **A rule with 3 callers lives here.** Each class calls the shared sprite hide and flash rule
  (`applyHideAndFlash` in `PlayerSprite.kt`) and the direction sign (`Direction.upSign()` in `PlayerGravity.kt`). Some
  parts are still per class: the updatables list (its frame order differs per class), the land sound and the
  `GutsTank` side-stick rule (2 callers), each with a `TODO` that names item `060`; and the sprite size and place
  and the animation key. Share such a part when a third class needs the same rule.
- **Swap only out of the engine.** `PlayerRoster.putInSlot0` throws when the outgoing player is in the engine. Call it
  before the level screen starts, and set the camera focus after it.
