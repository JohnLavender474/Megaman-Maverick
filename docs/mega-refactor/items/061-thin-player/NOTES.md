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
