# Notes: 062 One engine-owned entity existence state

## Review: remove `spawned` and `dead`, and ask the engine (2026-10-26)

A read-only review agent on `master` at `d5c2370c0`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/`
= `core/src/main/kotlin/com/megaman/maverick/game/`. Tags: [code] read in the repo, [inferred] reasoned, not tested in
the game, [training] unchecked. Checked again by hand: `CannoHoney.kt:221` drops a child when `child.dead`.

### Summary

- The diagnosis is right: two flags describe existence, written by different code at different moments, and they
  disagree on three paths (`reset`/`dispose`, a respawn while a destroy is queued, a destroy of an entity that is not
  live). [code]
- The idea as stated (no flags; engine queries or a global helper) has three problems: set membership cannot
  reproduce `dead` ("the last queue operation was a destroy") when an entity is in both queues; `IGameEntity` and
  `PlayerView` code has no engine reference, so a global helper adds global state; the cost moves from a field read
  to hash lookups in per-contact code.
- Verdict: the goal is solid, the mechanism is not. Option (b) instead: the engine is the only writer of one
  `EntityState` on the entity; readers use read-only helpers; `spawned` and `dead` become derived, then go.
- A generation or handle (option (c)) answers another problem: stale references to pooled objects. The risk is real
  (commit `4511bef40` fixed one case); it belongs after `019b`, and only if a bug is shown.

### Writers [code]

| Flag | Writer | Moment |
|---|---|---|
| `spawned` | `E/GameEngine.kt:77` | `spawnNow`, after `onSpawn` |
| `spawned` | `E/GameEngine.kt:100` | `destroyNow`, after `onDestroy` |
| `dead` | `C/MegamanMaverickGame.kt:928` | `onQueueToSpawn` → `false` |
| `dead` | `C/MegamanMaverickGame.kt:929` | `onQueueToDestroy` → `true` |
| `dead` | `C/entities/contracts/MegaGameEntity.kt:28` | initial value `true` |

`engine.reset()` (`E/GameEngine.kt:132`) and `dispose()` (`:146`) call `destroyNow` directly, so they do not write
`dead`. Tests also write both flags (`MegaPlayersTest`, `PlayersTest`).

### Readers [code]

`spawned` (about 16 in `main`):

| Question | Samples |
|---|---|
| Live now (applied)? | `ReactorMan.kt:412,415`; `WilyFinalBoss.kt:2577-2578` (the same class uses `dead` at `:3016-3018`); `MegamanWeaponsHandler.kt:376`; `QuestionBlock.kt:127`; `FreezableEntityHandler.kt:64,83` |
| Wait until the spawn is applied | `WilyCapsuleTentacle.kt:330,380`; `TestWavyTentacleController.kt:169` |
| Player present | `E/players/PlayerView.kt:12` (`present = entity.spawned`) → `LaserBeamer.kt:248` |
| Pooling | `C/MegamanMaverickGame.kt:934` (`!spawned && !engine.contains(e, containedIfQueuedToSpawn = true)`) |
| Engine | `E/GameEngine.kt:132,146` (the reset/dispose filter) |
| Debug text | `ElecDevilBody.kt:221` and two siblings |

Not the entity flag: `Spawner.spawned` (`C/spawns/Spawner.kt:18`, means `spawn != null`) and the
`MegamanWeaponsHandler.spawned` map.

`dead` (about 88 lines in 60 files, plus 31 imports of the player `dead`):

| Question | Count (approx.) | Samples |
|---|---|---|
| "Did the player die?" (works because the player's health death queues a destroy) | 22 player + 11 self | `MegaLevelScreen.kt:802,831,927,933,962,1058`; `DesertMan.kt:609`; `PlayerDeath.kt:49`; `Megaman.kt:117,670`; the behaviors |
| Is an owned reference still valid (drop children, clear a slot)? | ~35 | `Spawner.kt:27`; `GutsTank.kt:344,355,388`; `CannoHoney.kt:222,305`; `FireMetSpawner.kt:117`; `SigmaRat.kt:245`; `PreciousWoman.kt:362,470`; `MegamanWeaponsHandler.kt:93,373` |
| Guard before a destroy or spawn | 6 | `BigAssMaverickRobotHand.kt:231`; `ReactorMonkey.kt:153`; `CrumblingBlock.kt:104`; `FreezableEntityHandler.kt:59`; `ShieldEntity.kt:68` |
| Damage gates (per contact) | 3 | `AbstractHealthEntity.kt:76`; `AbstractEnemy.kt:257`; `Megaman.kt:670` |
| Is the other entity still valid after a callback or contact? | ~12 | `CapsuleTeleporter.kt:218,244,270`; `PipePortal.kt:189,219`; `GravitySwitcharoo.kt:219`; `RotationAnchor.kt:110,114` |

No reader uses `dead` on a non-player entity to mean "health ran out" (bosses use `defeated`). Only for the player do
the two meanings coincide.

Other existence sources: `engine.contains(entity, containedIfQueuedToSpawn)` (one caller,
`MegamanMaverickGame.kt:934`); the private queues `entitiesToSpawn` and `entitiesToKill`; `initialized`; `canSpawn`
and `onSpawnRejected`; the pool return through `runnablesOnDestroy[POOL]` (`Pool.fetch` pops the last freed object
first); the `MegaGameEntities` registry (add in `onSpawn`, remove in `onDestroy`), a third existence index.

### Entity states and disagreements [code]

States: `UNSPAWNED` (never spawned, or destroyed), `SPAWN_QUEUED`, `LIVE`, `DESTROY_QUEUED` (live and in the kill
queue); two combinations the engine allows but does not model (live + spawn queued; live + destroy queued + spawn
queued). `update()` applies all spawns, then all destroys. `spawned` = `LIVE` or `DESTROY_QUEUED`; `!dead` ≈
`SPAWN_QUEUED` or `LIVE` (the last queue call was a spawn), not written by `reset`/`dispose`.

| Path | `spawned` | `dead` | Effect |
|---|---|---|---|
| `engine.reset()`/`dispose()` on live entities | `false` | stays `false` | Megaman keeps `dead = false` between levels; at the 2nd and later level starts, `player.dead` is `false` until the spawn is queued (the first level starts with `true`). Readers in that window: `MegaLevelScreen.kt:933,962,1058`. Visible effect unknown [inferred] |
| `reset()` while entities are queued to spawn | `false` | `false` | they never spawn and are never freed to a pool (a small leak) |
| `destroy()` then `spawn()` in one frame on a live entity | `false` at the end | `false` | `onSpawn` runs again on a live entity, then `destroyNow`; it ends destroyed but "not dead"; `FreezableEntityHandler.kt:59-64` can hit this (unfreeze and refreeze in one frame) [inferred, rare] |
| `spawn()` on a live entity | `true` | `false` | `onSpawn` twice with no `onDestroy` between |
| `destroy()` on an `UNSPAWNED` entity | `false` | `true` | `onDestroy` runs again (pool free, registry remove); callers guard with `!dead` |
| Spawn rejected, double destroy, spawn and destroy in one frame | — | — | consistent |
| Inside `onSpawn` / `onDestroy` | `false` / `true` | `false` / `true` | each flag is "late" in its own callback; no reader found that depends on it |

Stale references (ABA), apart from the flags: a holder keeps object E; E is destroyed, freed to its pool, and popped
and spawned by another owner before the holder polls; the holder then treats the other spawn as its own.
`4511bef40` fixed this for rejected spawns; `019` `NOTES.md:152` names the general case. A plausible remaining case
[inferred, not tested]: the `Bullet` pool is shared by the buster (`MegamanWeaponsHandler.kt:770`) and 12 enemy
files; `cullAllDead()` (`:118`) runs in Megaman's update, after `destroyNow`; an enemy updated before Megaman could
reuse a freed bullet, which Megaman would then count against his shot limit. Child lists in parents of the same class
(two `CannoHoney`, two `FireMetSpawner`) have the same exposure.

### The idea evaluated

1. Correctness: one source of truth, if `reset`/`dispose` are fixed; but set membership cannot express "the last
   operation wins" when an entity is in both queues, so exact parity with `dead` needs per-entity order state (a state
   field after all). A plain `dead` → `!engine.isLive(e)` changes play: a child spawned this frame is queued, not
   live, so `if (child.dead) iter.remove()` (`CannoHoney.kt:221` and about 30 others) would drop it at once. Each
   reader must map to the right set of states.
2. Cost: today a field read. Queries need 1 to 3 hash probes per read, in per-contact gates and per-frame behaviors.
3. The engine reference: `GameEntity` has `engine`; `IGameEntity` and `PlayerView.present` do not. A global helper
   needs a global engine, against the refactor goal; tests with a relaxed MockK game would get mock answers.
4. Pooling: an engine query is per object, like the flags, so it does not fix the stale-reference problem either.
5. Determinism: neutral.
6. Readability: "ask the engine" is explicit; `targetPlayer.dead` reads better than `!engine.isLive(player.entity)`,
   and for the player it does mean death.

### Alternatives

| | (a) No flags; engine queries or global helpers | (b) One engine-owned state field; read-only helpers | (c) Generation / handle |
|---|---|---|---|
| One truth on all paths | yes, with the reset fix; needs extra order state for `dead` | yes; the engine writes in every path | orthogonal; needs (a) or (b) under it |
| Cost per read | 1 to 3 hash probes | a field read (as today) | a field read plus an int compare |
| Needs the engine reference | yes (or a global) | no | no |
| Fixes pooled reuse (ABA) | no | no | yes |
| Size of change | all readers at once | phased: derive the old flags, then move readers | a new `EntityRef` at each holder (~40 sites) |
| "No speculative APIs" | yes | yes | only with a shown bug |

How engines answer "is it alive / pending destroy" [training]: Godot `is_instance_valid(obj)` and
`is_queued_for_deletion()`; Unreal `IsValid(obj)`, `AActor::IsActorBeingDestroyed()`, `TWeakObjectPtr` (a serial
number that detects slot reuse); Unity's destroyed object compares equal to `null`, `Destroy` is deferred to the end
of the frame, no "pending" query; Bevy `Entity` = index + generation, `world.get_entity(e)` fails for a despawned or
reused id, `Commands::despawn` is deferred; EnTT `registry.valid(e)` checks the version. The pattern: one
engine-owned existence state, read on the object; engines that pool or reuse slots add a generation in the handle.

Sketch of (b):

```kotlin
enum class EntityState { UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED }
// GameEntity: var state = UNSPAWNED; internal set   (written only by GameEngine)
val IGameEntity.inEngine get() = state == LIVE || state == DESTROY_QUEUED   // == today's spawned
val IGameEntity.exists   get() = state == SPAWN_QUEUED || state == LIVE     // == today's !dead
```

`internal set` keeps `core` from writing the state, because `core` is another module.

### Risks

- The reset fix changes `player.dead` at the 2nd and later level starts; it must be checked in the game.
- The reader mapping is easy to get wrong (`!isLive` in place of `dead` drops queued children); mitigation: `062a`
  keeps the derived `dead`, so `062b` is a rename with a review per site.
- A wide rename conflicts with parallel branches; small batches limit that.

### Related items

`019b` (destroy notification: agree the order; it can use the state or its transition point), `061b` (`PlayPhase`
stays a separate axis), `048` (existence stays on the entity, not in a component), `021` (the registry could follow
the state), `038` and `047` (consumers; `dead` per player in co-op).
