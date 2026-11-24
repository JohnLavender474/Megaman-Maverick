---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: compactor-level play test (2026-11-01)
related-items: [045]
---

# Entities keep a pooled scratch object in a field, so `CeilingCrusher` teleports and kills at once

## Where

Confirmed by reading:

- `core/src/main/kotlin/com/megaman/maverick/game/entities/hazards/CeilingCrusher.kt:96` (`start`, read at lines 97, 219, 232 and 234)
- `core/src/main/kotlin/com/megaman/maverick/game/entities/blocks/Lift.kt:64` (`stopPoint`, read at lines 65, 112-115 and 155)

Same pattern, not yet checked for a stale read:

- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/UnderwaterPenguinBot.kt:85` (`startPosition`, read at line 99)

The pool code:

- `core/src/main/kotlin/com/megaman/maverick/game/utils/extensions/GameRectangleExtensions.kt:14` (`getPositionPoint`, `reclaim` defaults to `true`)
- `core/src/main/kotlin/com/megaman/maverick/game/utils/GameObjectPools.kt` (`fetch`, `performNextReclaim`)

(`master` `d81b58a92`)

## What happens

`onSpawn` stores the result of a pooled helper in a field:

```kotlin
start = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
```

The helper returns a `Vector2` from `GameObjectPools` with `reclaim = true`. The pool frees it at the end of frame N+2. The field still points at the same object, so a later `fetch` can reuse it and overwrite the value.

For `CeilingCrusher`, this gives three symptoms:

1. Wrong position: in `WAITING` the crusher moves to `start` on each update, so it goes to whatever the new owner wrote.
2. Flicker: the vector is rewritten again and again, so the crusher jumps from frame to frame.
3. Instant kill: if the crusher lands near Mega Man, its trigger area overlaps him and it enters `DROPPING`. The `DEATH` fixture is active from that moment, also during the `0.1` s drop delay. It has `INSTANT` set.

The crusher has no `CullablesComponent`, so it stays live with the bad value after a player death or a room exit.

## What should happen

An entity keeps its own copy of any value that it reads after the frame of the fetch. A `CeilingCrusher` stays in its slot until it drops, and it returns to the same slot.

## How to reproduce

On branch `compactor-level`, run `./run-mega-man-maverick-alpha.sh --level COMPACTOR` and enter room `blind_line`. The owner saw the crushers appear over Mega Man, flicker and crush him. Reuse depends on how many vectors each frame fetches, so other levels with these entities may not show it.

## Scan

Find every entity that has this fault, and list each result in this file. Scan `core/src/main/kotlin/com/megaman/maverick/game/entities/`, including `megaman/`, and other code that holds a value across frames.

1. List the pooled sources. Every function that returns an object from `GameObjectPools.fetch` with `reclaim = true`, also through a default parameter. Start with `utils/extensions/` (34 `fetch` lines), for example `getCenter`, `getPosition`, `getPositionPoint`, `getSize`, `toGameRectangle`, `toGameCircle` and `toGamePolygon`. Also check engine functions that take a pooled `out` argument, and `Camera.toGameRectangle()`, which hides the fetch in a default parameter.
2. List the stores. Every assignment of such a result to something that outlives the frame:
   - a `var` or `lateinit var` field; a first `grep` finds 43 candidate field declarations of type `Vector2`, `Vector3`, `GameRectangle`, `GameCircle` or `GameLine`
   - a collection field (`Array`, `ObjectMap`, `Queue`)
   - a value captured in a lambda that runs later, for example a `Timer` runnable, a `runQueue` entry or a `MotionDefinition`
   - `Properties` that wait in a spawn queue, or a `spawnProps` value that the entity keeps
   - a value passed to a constructor or function that keeps it, for example a trajectory or a motion object
3. Exclude the safe cases: a result that is copied (`.cpy()`, `.set(...)`) or fetched with `reclaim = false` and owned, and a value that is read only in the same frame.
4. Check for a stale read. For each store, name the line that reads the value later, and say if the read can come 3 or more frames after the fetch.
5. Record each result: file and line, the field, the read, how the entity would fail, and which live levels use the entity.

A `grep` for one line of the form `x = y.getCenter()` finds only the simple cases. It found the 3 entities above, and it does not find a store in a collection, a lambda or a `Properties` object. The scan must read those cases.

To help find what the reading misses, a temporary debug check can write `NaN` into each object at reclaim, as option (e) in the `045` notes describes. A use after reclaim then shows at once in a play test. Do not keep this check as part of this issue.

## Notes

The fix for each entity is to own the value, as option (d) in the `045` notes describes:

```kotlin
private val start = Vector2()
// in onSpawn:
start.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER))
```

`.cpy()` also works, and it allocates one vector for each spawn. No map change can fix this fault, because the map does not write the field.

Item `045` plans the pool rules and the debug checks for the whole game. This issue covers the entities that fail now.

The cause comes from reading the code and from the owner's play test. No one has confirmed it with a fix in the game. The value of `start` at the time of the failure was not observed.

The `Pool` fix `d8f846d8a` (2026-08-31) stopped `free()` from dropping objects, so reuse is more likely now than in older play tests. Levels with `CeilingCrusher` and `Lift` that passed before that date may not be safe.
