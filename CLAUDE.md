# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

Megaman Maverick is a non-commercial fan game written in **Kotlin** using the **LibGDX** framework. 
It is built with Gradle and targets Java 17. The project is structured as a multi-module Gradle build with three subprojects: `engine`, `core`, and `lwjgl3`.

---

## Build & Run Commands

```bash
# Run in debug mode (shows debug shapes, window, text, etc.)
./run-game-debug.sh

# Run in alpha mode (pixel-perfect, no debug overlays)
./run-game-alpha.sh

# Build a runnable JAR
./gradlew lwjgl3:jar
# Output: lwjgl3/build/libs/

# Run directly via Gradle (Windows or quick run)
./gradlew lwjgl3:run

# Run engine tests
./gradlew engine:test

# Run a single test class
./gradlew engine:test --tests "com.mega.game.engine.common.objects.LoopTest"

# Clean build
./gradlew clean
```

The game requires **Java 17+**. The `run-game-debug.sh` and `run-game-alpha.sh` scripts call `./gradlew lwjgl3:run` with appropriate `--args` flags.

---

## Module Architecture

### `engine/`
A custom, reusable ECS (Entity-Component-System) game engine. It knows nothing about Megaman-specific logic. Key packages:
- `entities/` — `GameEntity`, `IGameEntity`, component contracts (`IBodyEntity`, `ISpritesEntity`, `IAudioEntity`, etc.)
- `systems/` — `GameSystem` base class; systems process entities that own specific components
- `components/` — `IGameComponent` base; each system is paired with one or more component types
- `world/` — physics world container, contacts, pathfinding
- `events/` — `EventsManager`, `IEventListener`; pub/sub event bus
- `screens/` — base screen and tiled map loading infrastructure

### `core/`
All Megaman-specific game logic. Built on top of `engine/`.

**Entry point:** `MegamanMaverickGame.kt` — extends LibGDX `Game`; initializes all systems, the `GameEngine`, asset loading, audio, controllers, and screen management.

**Key packages in `core/src/main/kotlin/com/megaman/maverick/game/`:**

| Package                 | Purpose                                                                                                                |
|-------------------------|------------------------------------------------------------------------------------------------------------------------|
| `entities/`             | All game entities organized by type                                                                                    |
| `entities/contracts/`   | Abstract base classes: `MegaGameEntity`, `AbstractEnemy`, `AbstractBoss`, `AbstractProjectile`, `AbstractHealthEntity` |
| `entities/megaman/`     | The player character — `Megaman.kt` and its sub-components (`components/`, `handlers/`, `sprites/`, `constants/`)      |
| `entities/bosses/`      | Robot Masters and other boss entities                                                                                  |
| `entities/enemies/`     | Standard enemy entities                                                                                                |
| `entities/projectiles/` | Player and enemy projectiles                                                                                           |
| `entities/factories/`   | `EntityFactories` (legacy) and `MegaEntityFactory` (pooled, reflection-based factory)                                  |
| `screens/`              | All game screens enumerated in `ScreenEnum`                                                                            |
| `screens/levels/`       | `MegaLevelScreen` — the main gameplay screen; manages camera, spawners, events, HUD                                    |
| `screens/levels/tiled/` | Tiled map layer builders that parse `.tmx` maps into spawners, triggers, game rooms, etc.                              |
| `events/`               | `EventType` enum — all game events sent over the engine's event bus                                                    |
| `levels/`               | `LevelDefinition` enum — maps level names to `.tmx` files, music, and completion logic                                 |
| `world/body/`           | `FixtureType`, `BodySense`, `BodyLabel`, fixture/body extension helpers                                                |
| `world/contacts/`       | `MegaContactListener`, `MegaContactFilter` — handles physics collision callbacks                                       |
| `assets/`               | `TextureAsset`, `MusicAsset`, `SoundAsset` enums — all asset identifiers                                               |
| `damage/`               | `DamageNegotiation`, `IDamageNegotiator` — damage negotiation between damagers and damageables                         |
| `state/`                | `GameState` — persistent save state (lives, health tanks, weapons unlocked, etc.)                                      |
| `difficulty/`           | `DifficultyMode` — normal/hard mode; affects which entities spawn                                                      |

### `lwjgl3/`
Desktop launcher. Configures window settings and starts `MegamanMaverickGame`.

---

## Core Patterns

### Entity Lifecycle
Entities are managed by `GameEngine`. The lifecycle is:
1. `init()` — called once on first spawn; set up components here
2. `onSpawn(spawnProps: Properties)` — called each time an entity is spawned; configure from props
3. `onDestroy()` — called on removal

`MegaGameEntity` is the base class for all game entities. It registers/unregisters with `MegaGameEntities` (a global entity registry) on spawn/destroy.

### Entity Factory & Pooling
`MegaEntityFactory` is the preferred (non-deprecated) way to create and recycle entities. It uses object pools keyed by `KClass`. Entities are fetched via `MegaEntityFactory.fetch(MyEntity::class)` and freed via `MegaEntityFactory.free(entity)`.

The older `EntityFactories` object (organized by `EntityType`) is still present but marked as deprecated.

### Spawn Props
Entities receive a `Properties` map on spawn (populated from Tiled map object properties or code). Prop keys are string constants defined in `ConstKeys`.

### Events
The event bus (`EventsManager`) uses `EventType` enum values. Entities and screens implement `IEventListener` and register/unregister as needed. Common events: `PLAYER_SPAWN`, `BOSS_DEFEATED`, `END_LEVEL`, `GAME_OVER`, `BEGIN_ROOM_TRANS`, etc.

### Tiled Maps
Levels are `.tmx` files loaded from `assets/tiled_maps/`. The `MegaLevelScreen` loads the map and processes each layer through a corresponding builder in `screens/levels/tiled/layers/` (e.g., `SpawnersLayerBuilder`, `GameRoomsLayerBuilder`). Spawners trigger entity creation when the camera enters their bounds.

### Physics & Fixtures
Bodies use `FixtureType` to categorize fixtures (e.g., `DAMAGER`, `DAMAGEABLE`, `BLOCK`, `PROJECTILE`). Contact callbacks in `MegaContactListener` check fixture-type pairs to determine damage, death, item pickup, etc.

### Damage Negotiation
`IDamageNegotiator` maps damage source types to damage amounts. `AbstractEnemy` and `AbstractBoss` expose a `damageNegotiator` that defines how much damage each weapon/projectile type deals.

### Assets
All assets are referenced by enum: `TextureAsset` (sprite sheet `.txt` atlas descriptors), `MusicAsset`, `SoundAsset`. Loaded via LibGDX `AssetManager`.

### Constants
- `ConstVals` — numeric game constants (PPM=32, VIEW_WIDTH=16, VIEW_HEIGHT=14, FPS=60, MAX_HEALTH=30, etc.)
- `ConstKeys` — string keys used in `Properties` maps

---

## Level & Boss Structure

Levels are defined in `LevelDefinition` (enum). Each entry specifies:
- `type: LevelType` — intro level or robot master level
- `tmxMapSource` — `.tmx` filename
- `music: MusicAsset`
- `screenOnCompletion` — which `ScreenEnum` to go to after beating the level

Robot Masters extend `AbstractBoss` → `AbstractEnemy` → `AbstractHealthEntity` → `MegaGameEntity`.

---

## Testing

Tests live in `engine/src/test/`. They use JUnit/Kotlin test. The `core/` module has no tests. To run engine tests:

```bash
./gradlew engine:test
```
