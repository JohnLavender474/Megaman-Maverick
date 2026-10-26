# Notes: 061 Thin player view; locks and signals as components and functions

The research for this item is in item `029` `NOTES.md` (sections "`MegaPlayer` review and engine patterns",
"Functions, components and events instead of classes", "Correction: 'or null' lookups versus query filters"). The
review notes of the experimental branch `temp-thin-player` move into the first branch's folder at its start.

## `ready` and `teleporting` for any entity (2026-10-26)

The owner: "`teleporting` can apply to just about any entity, not just the player. Same for `ready`. Therefore, I
think we should rethink these fields so that the same can be applied to other non-player entities." A read-only
research agent, on branch `mega-refactor-061a-thin-player` (step 4). A proposal, not decided. Tags: [code] this repo,
[web] a fetched page, [training] unchecked. Paths are under `core/src/main/kotlin/com/megaman/maverick/game/`. Two
claims were checked again: only Megaman has a `TELEPORTER_LISTENER` fixture, and `PlayerStateComponent` has no
`reset()` override.

### Teleporting today

- Only the player teleports. `Megaman` has the only `TELEPORTER_LISTENER` fixture
  (`entities/megaman/components/BodyComponent.kt:272`). `AbstractEnemy` sets the teleport property lambdas
  (`AbstractEnemy.kt:165-166`), but no enemy can touch a teleporter, so they are dead code. [code]
- The three teleporters (`PipePortal`, `PortalHopper`, `CapsuleTeleporter`; `ITeleporterEntity`, `TeleporterRule`)
  keep their own per-entity maps, and `isTeleporting(e)` answers from them. They call the entity's
  `ON_TELEPORT_START(teleporter)`, `ON_TELEPORT_CONTINUE()` and `ON_TELEPORT_END()` property lambdas
  (`Constants.kt:327-329`); the defaults are in `entities/utils/GameEntityUtils.kt:126-160`. [code]
- On the entity side only `Megaman` keeps state: the `teleporting` flag and a `ConstKeys.TELEPORTER` property, which
  `PipePortal` also writes (entry pipe, then exit pipe). [code]
- Readers: Megaman's controller, jump, air dash and jetpack (the flag); `SpritesComponent.kt:100` and
  `MegamanAnimationKeySupplier.kt:42-43` (the flag and the teleporter); `PreciousGem.kt:395` (the owner player's flag);
  damage uses the `TELEPORT` lock, not the flag. [code]

### `ready` today: three concepts under one name

| Concept | Today | Writers | Effect |
|---|---|---|---|
| Lifecycle phase "entering" | player `!ready` (beam-in); boss `!ready` (intro); enemy spawn blink (`AbstractEnemy.kt:126-180`) | level handler; boss `isReady`; enemy timer | no input or behaviors; invincible; hidden or blinking; hazards wait |
| Lifecycle phase "leaving" | player `ready = false` at death and beam-out | death and end-level handlers | as "entering" |
| Boss sub-phase "health fill" | `betweenReadyAndEndBossSpawnEvent` | `AbstractBoss` | boss AI waits |
| Transport | Megaman `teleporting` + `TELEPORTER` property; teleporter maps | Megaman lambdas, `PipePortal` | no input; hidden or pipe animation; damage lock |
| Capability gates | locks `DAMAGE`, `MOVEMENT`, `BEHAVIORS` | many | the effect itself |

- Player `ready` writers: the spawn sequence start (`false`) and beam end (`true`), death (`false`), the level end
  (`false`). Readers: many Megaman internals, and outside code (`FeetSandRule`, `Rain` with `?: true` for non-players,
  `DarknessV2`/`V4`, `BuilderKibbo`, `TubeBeamer`, `LaserBeamer`, `BulletBillLauncher`, `MegaLevelScreen` checkpoint
  text and pause). [code]
- Boss `ready` (`AbstractBoss.kt:112`): `false` at spawn, destroy and `BossSpawnEventHandler`; `true` when `isReady`
  first passes; gates `canDamage`; about 15 bosses read it. [code]
- A detail to keep: `PlayerStateComponent` has no `reset()`, so player `ready` stays `false` across a destroy until the
  next spawn sequence. [code]
- X6 has the same split: a per-object `state` byte (0 set-up, 1 main, 2 death, 3 off screen) plus separate stop flags;
  the player's state 0 waits for `game.ready` (`mmx-demake-tools/docs/engine.md:684-698, 813, 817, 982-983`).

### Other engines

- GAS tags: hierarchical labels checked with `HasTag`/`HasAny`/`HasAll` [web]; states such as `State.Stunned` block
  abilities; counted [training].
- Unity DOTS enableable components: for "states that you expect to change often and unpredictably"; a disabled
  component counts as absent; add and remove stay preferable for low-frequency state changes [web].
- Bevy: marker components with `With`/`Without` [training]; `seldom_state` (each state a component, a `StateMachine`
  component; "without many interconnected states") and `bevy_fsm` (one enum component per machine) [web]; global
  `States` are app-wide [training].
- flecs: tags; a relationship can be added more than once with different targets [web]; the `Exclusive` trait gives
  one-of-N per entity [training].
- Godot: groups as tags, hand-written per-node state machines [training].
- Sonic Mania: one `state` function pointer per player; transport objects set the state and turn off interaction and
  tile collision while they carry the player [training].

Common pattern: a one-of-N phase per entity, plus separate tags or locks for effects; a transport state is a phase or
tag with data (the carrier).

### Options

| Option | Any entity | Components + functions | Same play | Comment |
|---|---|---|---|---|
| (a) `StateTagsComponent` (bit set of `MegaStateTag`) | yes | yes | yes | no slot for the teleporter; no exclusivity; copies what `LocksComponent` is |
| (b) `LifecycleComponent` + `TransportComponent` | yes | yes | yes | holds the teleporter; the enum is exclusive; two small types |
| (c) derive from locks (`ready = !isLockedBy(…, SPAWN)`) | yes | yes | **no** | death and level end hold no `SPAWN`; `LocksComponent.reset()` clears on destroy, so `ready` would read `true` after death; mixes causes with effects |
| (c2) a lock group `PRESENCE` | yes | yes | needs care | the same reset problem; "lock" is the wrong name for a phase |
| (d) a per-entity state machine | yes | partly | yes | too heavy; the changes come from outside |

Sketch of (b), core first:

```kotlin
enum class Lifecycle { ENTERING, ACTIVE, LEAVING }

class LifecycleComponent(var phase: Lifecycle = Lifecycle.ENTERING) : IGameComponent {
    override fun reset() {}   // keeps its value across a destroy, as PlayerStateComponent does today
}

val IGameEntity.lifecycle: Lifecycle get() = getComponent(LifecycleComponent::class)?.phase ?: Lifecycle.ACTIVE
val IGameEntity.isActive: Boolean get() = lifecycle == Lifecycle.ACTIVE
fun IGameEntity.setLifecycle(phase: Lifecycle) { require(LifecycleComponent::class).phase = phase }

class TransportComponent : IGameComponent {
    var teleporter: ITeleporterEntity? = null
    override fun reset() { teleporter = null }
}

val IGameEntity.teleporter: ITeleporterEntity? get() = getComponent(TransportComponent::class)?.teleporter
val IGameEntity.teleporting: Boolean get() = teleporter != null
```

- An entity without `LifecycleComponent` reads `ACTIVE` (`Rain` already treats non-players as ready). `MegaPlayer.ready`
  and `teleporting` become one-line wrappers, so their call sites do not change.
- Cost: one component lookup per read, as today; no allocation.
- Merge player and boss `ready` into `Lifecycle`; keep `betweenReadyAndEndBossSpawnEvent` on the boss; keep transport
  apart from the lifecycle; keep the locks as the effects (the phase writers keep taking their lock reasons); keep
  `frozen` apart.
- Core first: `TransportComponent` refers to `ITeleporterEntity` (core). `Lifecycle` can move to the engine when an
  engine system reads it (for example item `012` freezes skipping non-`ACTIVE` entities).

### Recommendation and migration (research agent)

1. `061b`: `LifecycleComponent` and `Lifecycle` replace `PlayerStateComponent.ready`; the three level handlers write
   `setLifecycle(ENTERING/ACTIVE/LEAVING)`; `MegaPlayer.ready` becomes read-only; the dead `players.ready` imports in
   `SigmaRatClaw` and `BigAssMaverickRobotHand` go.
2. `061b`: `TransportComponent` replaces Megaman's `teleporting` flag and the `ConstKeys.TELEPORTER` property; the
   teleporters call free functions `beginTeleport(entity, teleporter)` and `endTeleport(entity)` (which call the
   existing lambdas); `PipePortal` keeps its exit write. `ITeleporterEntity.isTeleporting` stays per teleporter (a
   global check in `shouldTeleport` would change play).
3. Cleanup: remove the dead teleport lambdas in `AbstractEnemy.kt:165-166`.
4. Item `048`: boss `ready` becomes `LifecycleComponent`.
5. The enemy spawn blink only when a reader outside `AbstractEnemy` needs it.
6. With item `012`: move `Lifecycle` to the engine when a system reads it.
7. Item `060`: no extra work; a second character gets both components.

Sources: Unity enableable components
<https://docs.unity3d.com/Packages/com.unity.entities@1.0/manual/components-enableable-intro.html>; Unreal Gameplay
Tags <https://dev.epicgames.com/documentation/en-us/unreal-engine/using-gameplay-tags-in-unreal-engine>; flecs
relationships <https://www.flecs.dev/flecs/Relationships.html>; seldom_state <https://docs.rs/seldom_state>; bevy_fsm
<https://docs.rs/bevy_fsm>; X6 notes `mmx-demake-tools/docs/engine.md`.

## "Lifecycle" versus spawning, spawned and dead (2026-10-26)

The owner asked if the proposed `Lifecycle { ENTERING, ACTIVE, LEAVING }` can be confused with "spawning, spawned,
dead", and whether to consolidate or rename. A read-only review agent on `master` at `93b3c7eff`. Two claims were
checked again in the code: `dead` is set only through `onQueueToDestroy` in `GameEngine.destroy()`, so
`engine.reset()`/`dispose()` (which call `destroyNow` directly) leave `dead = false`; and the death handler writes
`ready = false` when `PLAYER_JUST_DIED` arrives, which Megaman's `onDestroy` submits, so after he is destroyed. Tags:
[code], [web], [training]. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

### Three axes

1. **Existence** (engine): queued to spawn, spawned, queued to destroy, destroyed. Today two flags:
   `IGameEntity.spawned` (changes when the engine applies its queues) and `MegaGameEntity.dead` (changes when a change
   is queued). The root `CLAUDE.md` already calls `init`/`onSpawn`/`onDestroy` the "Entity lifecycle".
2. **Play participation:** the entity exists but takes part in play only between its entrance and its exit (the
   player's `ready`, the boss `ready`, the enemy spawn blink). The draft's `Lifecycle` models this axis.
3. **Health death:** health depleted, plus any defeat animation (`wasHealthDepleted`, the boss `defeated`).

Axes 1 and 2 are mostly orthogonal, and both orders occur.

### Player timeline [code]

| # | Moment | queued / `spawned` | `dead` | `ready` | draft value |
|---|---|---|---|---|---|
| 1 | Level start: `playerSpawnEventHandler.init()` sets `ready = false` (`PlayerSpawnEventHandler.kt:125`), submits `PLAYER_SPAWN_INIT` | not queued / `false` | `true` (or a leftover `false`) | `false` | `ENTERING`, before the entity exists |
| 2 | End of the frame: `engine.spawn` (`MegaLevelScreen.kt:607`) | queued / `false` | `false` | `false` | `ENTERING` |
| 3 | Next `engine.update`: `onSpawn`; beam-in about 2.1 s; `SPAWN` locks on | — / `true` | `false` | `false` | `ENTERING` |
| 4 | Beam end (`PlayerSpawnEventHandler.kt:204-213`) | `true` | `false` | `true` | `ACTIVE` |
| a1 | Health depleted: `destroy()` (`AbstractHealthEntity.kt:113-116`; pit kill `Megaman.kt:760`) | destroy queued / `true` | `true` | `true` | `ACTIVE` |
| a2 | Next `engine.update`: `destroyNow`; `onDestroy` submits `PLAYER_JUST_DIED` (`Megaman.kt:536`) | — / `false` | `true` | `true` | `ACTIVE` |
| a3 | End of that frame: the death handler sets `ready = false` (`PlayerDeathEventHandler.kt:59`); 4.25 s | `false` | `true` | `false` | `LEAVING` on a destroyed entity |
| a4 | `PLAYER_DONE_DYIN`: rows 1-4 again | `false` | `true` | `false` | `ENTERING` |
| b1 | `VICTORY_EVENT`: `LEVEL_END` locks, controller off; 2 s + 6 s | `true` | `false` | `true` | `ACTIVE` |
| b2 | Pre-beam ends: `ready = false` (`EndLevelEventHandler.kt:168`); beam-out about 1.2 s; `END_LEVEL` | `true` | `false` | `false` | `LEAVING` |
| b3 | Level screen `reset()` → `engine.reset()` → `destroyNow` directly | — / `false` | stays `false` | `false` | `LEAVING` |

- The player has no "dying" phase as an entity: the death explosion is separate `ExplosionOrb` entities
  (`Megaman.kt:546-553`).
- `LEAVING` covers two different states: "after destroy" (death) and "spawned, beaming out" (level end). The word
  "leaving" fits only the second. This is the confusion the owner saw.
- The death write is still needed: `DarknessV2.kt:349` and `DarknessV4.kt:532` test `player.ready` and the body
  bounds, not `dead`; without it, the dead player's last position would stay lit for 4 s (inferred from the readers).
- "In play but not spawned" is not a design state, but happens in the data: rows a1-a2 (`ready = true` with
  `dead = true`, up to two frames), and after a reset without the end handler (for example, quit from pause).
  Readers that care test both: `!player.dead && player.ready` (`MegaLevelScreen.kt:927`), `dead || !ready`
  (`BulletBillLauncher.kt:108`, `Megaman.kt:117`), `present && ready` (`LaserBeamer.kt:248`). Others read `ready`
  alone (`FeetSandRule`, `Rain`, `Darkness*`, `TubeBeamer`, `BuilderKibbo`), so folding `!dead` into `inPlay` would
  change them for one or two frames at death.
- No reader separates entering from leaving today; only the writers carry the reason.

### Boss and enemy [code]

- Boss (`AbstractBoss.kt`): `ready = false` at `BossSpawnEventHandler.init` and `onSpawn`; intro until `isReady`;
  `ready = true` and the health fill (`betweenReadyAndEndBossSpawnEvent`) until `END_BOSS_SPAWN`; fight; health
  depleted → `triggerDefeat` (the boss is not destroyed at once; `defeated = true`, `ready` stays `true`); the defeat
  timer ends → `destroy()` and `BOSS_DEAD`; `onDestroy` sets `ready = false`. The boss is the one entity with a real
  dying phase (spawned, health depleted, not destroyed). `ready` gates `canDamage` (`:193`), not `canBeDamagedBy`.
- Enemy (`AbstractEnemy.kt`): `onSpawn` with `SPAWN_BLINK > 0` (5 call sites) is invincible and harmless until the
  blink ends; health depleted → `destroy()` at once; `destroyNow` runs the drop and `disintegrate()`.

### Other engines

No engine checked puts "destroyed / slot free" in the same enum as the gameplay phase; engines that have a "dying"
value put it in the gameplay layer, where it means "alive, health depleted".

| Engine | Existence | Gameplay phase |
|---|---|---|
| Unreal | `BeginPlay`/`EndPlay`, `Destroy` ("Destroy calls EndPlay", pending kill) [web: Epic, actor lifecycle]; `IsActorBeingDestroyed()` ≈ `dead` [training] | GAS tags (`State.Dead`), Lyra health component death states [training, not confirmed] |
| Unity | `Awake`/`Start`/`OnDestroy`, `activeSelf` [training] | user state on a `MonoBehaviour` [training] |
| Godot | `_enter_tree`/`_ready`/`_exit_tree`, `queue_free()`/`is_queued_for_deletion()` (≈ `dead`) [training] | own state machine [training] |
| Bevy | `spawn`/`despawn`, hooks `on_add`/`on_insert`/`on_remove`/`on_despawn` [web: docs.rs `bevy::ecs::lifecycle`] | marker or enum components (`Spawning`, `Dying`) by convention [training] |
| flecs | create/delete, `OnAdd`/`OnRemove` [training] | an exclusive relationship or a tag [training] |
| Sonic Mania | `destroyEntity` frees the slot [training] | one `state` function pointer, including the death fall [training] |
| X6 | despawn and the spawn record's "spawned" bit (`mmx-demake-tools/docs/engine.md:670-678`) | state byte 0 set-up, 1 main, 2 death, 3 off screen (`:688-693`); the player's 0 waits and beams in, 2 is a full death routine (`:982-984`) |

### Options

- **(a) Keep the axes apart and rename the phase (recommended by the review):** `PlayPhase { PRE_PLAY, IN_PLAY,
  POST_PLAY }`, `PlayPhaseComponent`, `IGameEntity.playPhase` (`IN_PLAY` without the component), `inPlay`,
  `setPlayPhase`. "Pre/post" are relative to play, not to the world: `POST_PLAY` is right after death (destroyed) and
  during the beam-out (spawned); `PRE_PLAY` is right before the spawn, during the beam-in, a boss intro and a spawn
  blink. Rejected names: `Lifecycle` (the engine's "Entity lifecycle"), `Presence`/`StagePresence` (collide with
  `PlayerView.present`, which is `entity.spawned`), `ACTIVE` (collides with `isActive`), `ENTERING`/`ARRIVING` (motion
  words; suggest spawning and rooms, for example `ENTER_BOSS_ROOM`), `Participation` (long), `ActivityPhase` (near
  `isActive`).
- **(a') The same split as a boolean** (`InPlayComponent(var inPlay = false)`): no reader needs the reason today, so a
  strict "no speculative APIs" reading allows only this; the enum can come later.
- **(b) Consolidate into one state** (for example `EntityPhase { UNSPAWNED, SPAWN_QUEUED, PRE_PLAY, IN_PLAY,
  POST_PLAY, DESTROY_QUEUED, DESTROYED }`): two independent axes in one value (row 1 needs unspawned + pre-play, row
  a3 destroyed + post-play, row b2 spawned + post-play, the boss defeat spawned + dying); one axis would overwrite the
  other (the death write would overwrite `DESTROYED`, or `destroyNow` would erase `POST_PLAY` and break `Darkness*`).
  The engine would have to own and write it in every spawn and destroy path and pooling, and replace `spawned` and
  `dead` (about 60 `dead` readers). Rejected for this branch.
- **(c) Derive it from existence and locks:** rejected earlier (the lock reset on destroy would read `IN_PLAY` after
  death).

### Recommendation (review agent)

1. Do not consolidate; rename to `PlayPhase` as above; `inPlay` closes the draft's open question 1.
2. Add the "three axes" paragraph to the `061b` scope; say that `inPlay` makes no claim about existence and does not
   check `spawned` or `dead`; readers that test both today keep both.
3. Record the timing facts in the scope: `PRE_PLAY` is written before `engine.spawn`; `POST_PLAY` at death is written
   after `destroyNow`; at the level end, `POST_PLAY` starts 8 s after the `LEVEL_END` locks; `reset()` keeps the value.
4. For item `048`: boss intro = `PRE_PLAY`; boss `defeated` is a dying state (spawned, health depleted) and not
   `POST_PLAY` by default (today `ready` stays `true` during the defeat, and mapping it would change `canDamage`).
5. A separate follow-up: `MegaGameEntity.dead` means "not live in the engine, or queued out", the word most likely to
   be confused with health death, and it disagrees with `spawned` after `engine.reset()`/`dispose()`. An existence
   cleanup (one engine-owned flag or state written in every destroy path) as a new item or an issue.
