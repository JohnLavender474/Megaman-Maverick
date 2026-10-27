# Scope: mega-refactor-062c-existence-readers

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-27

## Goal

Each reader of `MegaGameEntity.dead` and `IGameEntity.spawned` asks a named question of the engine-owned
`EntityExistenceState`. The questions are properties of the state type, not of the entity: the entity shows only
`existenceState`, which only the engine writes. Each new name gives the same value as the old flag on every path, so the game plays the same.
`dead` no longer reads like health death on entities that do not have health.

## Item

From item `062` `ITEM.md`, "Proposed scope":

> - `062c`: move the readers in batches (bosses, enemies, special and blocks, Megaman, screens) to named read-only
>   helpers (for example `inEngine` = today's `spawned`, `exists` = today's `!dead`), each reader mapped by the question
>   it asks. Keep a player-level `dead` derived from the state where it means "the player died".
> - `062d`: remove the `spawned` `var`, `MegaGameEntity.dead`, and `engine.contains(..., containedIfQueuedToSpawn)` if
>   the state replaces it; `PlayerView.present` reads the state.

Open question 2 of the item (the helper names, and whether to keep `dead` only on the player) is decided in this
scope (see "Open questions").

## Current state

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.
The full lists are in the scratchpad (`062c-dead.txt`, `062c-spawned.txt`).

### The two flags today

Both are getters on the state (`062a`). Since `062b` the state has five values.

| Flag | Where | True for | Meaning |
|---|---|---|---|
| `spawned` | `E/entities/IGameEntity.kt:17` | `LIVE`, `DESTROY_QUEUED`, `RESPAWN_QUEUED` | `onSpawn` ran, and the matching `onDestroy` did not run yet: the entity is in the engine and its systems now |
| `dead` | `C/entities/contracts/MegaGameEntity.kt:32` | `UNSPAWNED`, `DESTROY_QUEUED` | the entity is not live after the next engine update (the last accepted call was a destroy, or no spawn was ever accepted) |

`!dead` is true for `SPAWN_QUEUED`, `LIVE` and `RESPAWN_QUEUED`.

One small gap in the name "live now": in the destroy phase of an update, an entity that waits to respawn
(`RESPAWN_QUEUED`) is already out of the engine's entity set, but `spawned` is still `true`. Only code that runs in the
destroy phase (other entities' `onDestroy`) can see this window. The new name keeps this value, so the gap does not
change.

### Readers of `dead` (about 88, in 60 files)

| Question | Count (approx.) | Samples |
|---|---|---|
| Did the player die? (the player's health death queues a destroy; before the first spawn of a level the player is `UNSPAWNED`, so `dead` is also `true`) | about 37 | `C/screens/levels/MegaLevelScreen.kt:805,834,930,936,961,1057`; `targetPlayer.dead` in 17 enemies, bosses and blocks; `C/entities/sensors/PlayerDeath.kt:48`; 11 self reads in `Megaman.kt` and its behaviors and `MegamanJetpackHandler.kt` |
| Is my child, part or slot still mine? (drop it from a list, clear a field) | about 35 | `CannoHoney.kt:221,304`; `GutsTank.kt:343,354,387`; `SigmaRat.kt:244,248`; `PreciousWoman.kt:361,469`; `WilyFinalBoss.kt:3015-3017`; `MegamanWeaponsHandler.kt:94,374` |
| Guard before a spawn or a destroy | 6 | `BigAssMaverickRobotHand.kt:229`; `ReactorMonkey.kt:152`; `CrumblingBlock.kt:104`; `FreezableEntityHandler.kt:59`; `ShieldEntity.kt:68`; `PreciousGemCluster.kt:150` |
| Damage gates on the entity itself (per contact) | 3 | `AbstractHealthEntity.kt:76`; `AbstractEnemy.kt:253`; `Megaman.kt:653` |
| Is the other entity still valid in a contact or a callback? | about 10 | `CapsuleTeleporter.kt:219,245,270`; `PipePortal.kt:190,220`; `GravitySwitcharoo.kt:218`; `RotationAnchor.kt:110,114`; `DrippingToxicGoop.kt:111`; `FreezableEntityHandler.kt:93` |

All of these ask one fact of the state: "is the entity live after the next update?". For the player that fact is also
"the player died", because a death is a destroy (`AbstractHealthEntity.onHealthDepleted`, `Megaman.kt:743`) and the
level start clears the engine. No reader uses `dead` on an entity that is not the player to mean "its health ran out"
(bosses use `defeated`).

Some readers cast only to read `dead`, for example `(entity as MegaGameEntity).dead` in `CapsuleTeleporter.kt:245`,
`GravitySwitcharoo.kt:218` and `FreezableEntityHandler.kt:93`. Since `IMegaGameEntity` extends `IGameEntity`
(`b905d93a2`), a name on `IGameEntity` needs no cast where the type is already an `IGameEntity`.

### Readers of `spawned` (12 in `core`, 2 in the engine)

| Question | Sites |
|---|---|
| Is it in the engine now (it has a body and runs its systems)? | `ReactorMan.kt:412,415`; `WilyFinalBoss.kt:2576,2577`; `QuestionBlock.kt:127`; `FreezableEntityHandler.kt:64,83`; `MegamanWeaponsHandler.kt:377` |
| Wait until the spawn is applied | `WilyCapsuleTentacle.kt:330,380`; `TestWavyTentacleController.kt:169` |
| Player in the engine | `LaserBeamer.kt:248` |
| Pool return of a rejected spawn | `MegamanMaverickGame.kt:931` (`!entity.spawned && !engine.contains(entity, containedIfQueuedToSpawn = true)`) |
| Debug text | `ElecDevilBody.kt:221`; `ElecDevilBodyPiece.kt:220`; `ElecDevilBodyPieces.kt:318` |
| Engine | `E/GameEngine.kt:265,278` (the `reset`/`dispose` filter) |

Not the entity flag, and not in scope: `Spawner.spawned` (`C/spawns/Spawner.kt:20`, means `spawn != null`, read in
`Spawner.kt:34` and `SpawnerForEvent.kt:27`) and the `MegamanWeaponsHandler.spawned` map.

### Readers that mix the two questions

Two holders ask different questions about the same kind of reference. This branch keeps each one as it is; the new
names make the difference visible.

- `WilyFinalBoss`: `updateCannonOrbs` clears an orb when it is not in the engine now (`:2576`); the inner `update`
  clears an orb when it is not live after the update (`:3015`).
- `FreezableEntityHandler`: it spawns the shield when the shield is not live after the update (`:59`), and destroys it
  when it is in the engine now (`:64`). Since `062b` the engine guards make both paths safe.

### Tests

The permanent tests read the flags: `EntityExistenceStateTest` (18 lines), `EntityExistenceHooksTest` (2),
`PlayersTest` (1), `EntityExistenceTest` (6), `SpawnerReleaseTest` (8), `MegaPlayersTest` (1). `MegaGameEntity.toString`
prints `dead`.

## In scope

1. Two read-only properties on the enum `EntityExistenceState` (`E/entities/EntityExistenceState.kt`), given as
   constructor values, one row per state (owner decision, 2026-10-27). A new state must then give both answers, or the
   code does not compile. A read is a field read.

   ```kotlin
   enum class EntityExistenceState(val liveNow: Boolean, val liveAfterUpdate: Boolean) {
       UNSPAWNED(liveNow = false, liveAfterUpdate = false),
       SPAWN_QUEUED(liveNow = false, liveAfterUpdate = true),
       LIVE(liveNow = true, liveAfterUpdate = true),
       DESTROY_QUEUED(liveNow = true, liveAfterUpdate = false),
       RESPAWN_QUEUED(liveNow = true, liveAfterUpdate = true),
   }
   ```

   - `liveNow` has the value of today's `spawned`. First caller: `ReactorMan.kt:412`.
   - `liveAfterUpdate` has the value of today's `!dead`. First caller: `CannoHoney.kt:221`.
2. Move every reader of `dead` and `spawned` in `engine` and `core` (main and test) to the new properties, in batches:
   bosses; enemies; special, blocks, hazards, projectiles, sensors and utils; Megaman; screens, the game and the engine.
   Each read keeps its value: `x.spawned` becomes `x.existenceState.liveNow`, `x.dead` becomes
   `!x.existenceState.liveAfterUpdate`.
3. Drop a cast that exists only to read `dead`, where the value is already an `IGameEntity` (`existenceState` is on
   `IGameEntity`).
4. Remove `IGameEntity.spawned` and `MegaGameEntity.dead` when their last reader moves (see open question 3). The
   entity then has no existence getter of its own. `MegaGameEntity.toString` prints the state in place of `dead`.
5. Tests: one engine test that gives both values for each of the five states.

## Out of scope

- Any change to what a reader asks. A reader that asks the wrong question (for example the mixed holders above) is
  written as a backlog or bug candidate, not changed. A change of question can change play.
- `engine.contains(entity, containedIfQueuedToSpawn)` and the pool return in `MegamanMaverickGame.kt:931`. A plain
  state check there is not equal: an entity whose spawn is queued and then destroyed is `UNSPAWNED` but still in the
  spawn queue. This stays for `062d`.
- `Spawner.spawned` and `MegamanWeaponsHandler.spawned` (other meanings).
- The meaning of "dead" for a player in co-op (item `047`), and the `players.all` `TODO`s in `GutsTank`, `ElecDevil`
  and `WilyFinalBoss`.
- Stale references to pooled objects (a spawn generation; item `062`, "Later, only with evidence").
- Comments and docs strings that use the word "dead" in its normal sense (for example `GauntletMaster.kt:32`).

## Affected areas and risks

- About 75 files in `core` and 3 in `engine`, mostly one-line changes. A wide rename can conflict with other branches;
  no other branch is active now.
- The risk is a wrong substitution: `dead` in place of `!spawned`, or a lost `!`. A wrong one can drop a child that is
  still queued to spawn (for example `CannoHoney` bees) or keep one that is gone. Mitigations:
  - The new properties are exact copies of the old values (an engine test for all five states).
  - Each substitution is mechanical (`x.dead` → `!x.existenceState.liveAfterUpdate`,
    `!x.dead` → `x.existenceState.liveAfterUpdate`, `x.spawned` → `x.existenceState.liveNow`), done by a script with
    a review of the diff per batch.
  - With the old getters removed, the compiler finds every reader that was not moved.
- No gameplay change is planned.

## Overlap with other branches

No other branch is active. Item `019c` (merge the four spawn queues) and item `047` (co-op rules for a dead player) come
later and read the new names.

## Open questions

1. Closed (owner, 2026-10-27): `liveNow` and `liveAfterUpdate`, as properties of `EntityExistenceState` with one
   constructor value per state, not as members of the entity or queries on the engine (see `docs/NOTES.md`,
   "Decisions").
2. Closed (owner, 2026-10-27, "Go with your recommendations"): no `dead` at all; player readers also ask
   `existenceState.liveAfterUpdate`. For the player, "died" and "not live after the update" are one fact: a death is
   a destroy, and before the first spawn of a level the player is `UNSPAWNED`.
3. Closed (owner, 2026-10-27): this branch removes `IGameEntity.spawned` and `MegaGameEntity.dead` when their last
   reader moves, so the compiler shows that no reader was missed. `062d` keeps only
   `engine.contains(..., containedIfQueuedToSpawn)` and the pool return.
