# Players

This package holds the players service (`MegaPlayers`) and the rules that every player class shares: how a player
spawns, dies, moves through a room transition, takes damage and is blocked.

A second playable character must not copy `Megaman.kt` and must not extend a player base class (`O8-R1`, `O1-R2`).
So the shared player rules are free functions over the entity. Each player class keeps its own Kotlin class and its
own hooks (`onSpawn`, `onDestroy`, `onEvent`, `takeDamageFrom`), and each hook calls the shared function for its
part. The class adds only what is its own, for example its sounds, its weapons and its values.

The research and the decisions are in `docs/mega-refactor/items/081-playable-x/` (branch `081a`).

The examples in this file are hypothetical. The real functions are in `PlayerLifecycle.kt` and `PlayerRules.kt`.

## Status

`Megaman` is the only player class. X (`081b`) is the second caller. Some rules still have another owner, and a
`TODO` at the function names it: the difficulty change of the damage (`023`), the owner chain of a shot (`030`), the
spawn event as a level reset (`038`), the death event (`119`), the health base class (`048`), and the locks for stun,
freeze and damage (`126`).

## The parts

```mermaid
flowchart LR
    Class["a player class<br/>(for example Megaman)"] -- "its hooks call" --> Lifecycle["PlayerLifecycle.kt<br/>spawn, destroy, room transition,<br/>out-of-level kill"]
    Class -- "its rules call" --> Rules["PlayerRules.kt<br/>invincible, can move,<br/>damage permission, damage tail, damage edit"]
    Class -- "damageNegotiator" --> Negotiator["PlayerDamageNegotiator<br/>the damage table for players"]
    Lifecycle --> State["the mechanism components<br/>(entities/state/)"]
    Rules --> State
```

## The order of a spawn

The shared spawn work is 2 calls, so that the class can put its own lines between them.

```mermaid
sequenceDiagram
    participant C as player class onSpawn
    participant S as shared functions
    C->>C: super.onSpawn
    C->>S: playerSpawnStart(spawnProps)
    Note over S: listener, body position and physics, facing,<br/>world gravity, health, lock clears, teleporter
    C->>C: its own lines (for example weapons, timers)
    C->>S: playerSpawnEnd()
    Note over S: camera follow stop, PLAYER_SPAWN for slot 0
```

A hypothetical player class:

```kotlin
override fun onSpawn(spawnProps: Properties) {
    super.onSpawn(spawnProps)
    playerSpawnStart(spawnProps)
    dashTimer.reset()
    playerSpawnEnd()
}

override val invincible get() = playerInvincible
```

## Rules a change must keep

- **Compute, do not store.** `playerInvincible` and `playerCanMove` read their state at each call. A timer that ends
  writes nothing, so a stored value goes stale.
- **Fail fast.** The rules read their components with `require(...)`, not with the queries that return a silent
  default. A player class that lacks a component throws at once. The components that each function needs are the
  ones it reads with `require`.
- **The gravity set comes before the lock clears** in `playerSpawnStart`. The gravity set can start a camera turn
  that locks `MOVEMENT`, and the lock clear must remove it.
- **One copy.** A rule that two player classes need goes here, not into each class.
