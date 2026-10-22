# Notes: 021 Entity registry with stable readable IDs and aliases

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEM 20: ENTITY REGISTRY WITH STABLE, READABLE IDS".

````text
(1) Current state

- `SpawnersLayerBuilder.kt:48-58,71-85` maps the layer to `EntityType`. It then calls `MegaEntityFactory.getEntityClass(entityType, name)`, which runs `Class.forName("com.megaman.maverick.game.entities.<package>.<name>")` (`MegaEntityFactory.kt:47-48`).
- Construction uses `kotlin.reflect.full.primaryConstructor` (`MegaEntityFactory.kt:69`).
- So the TMX object name is the Kotlin simple class name, and it is tied to the package. If you move or rename a class, the maps break with no warning: the error is logged and the object is skipped (`SpawnersLayerBuilder.kt:79-85`).
- Pools are keyed by KClass. Only `Bullet` (25) and `Block` (25) are prewarmed (`MegaEntityFactory.kt:30-32`).
- `EntityFactories` is marked deprecated (`EntityFactories.kt:10`) but has 76 call sites.
- `TAG` already works as an informal ID:
  - It is the damage key for the player (`MegamanDamageNegotiator.kt:26-31`).
  - It is the `LevelOverride.canSpawn` filter (`LevelDefinition.kt:144`).
  - It is the atlas region prefix (for example `"${MoonMan.TAG}/jump"`).
- 440 files define a `TAG`. Most are equal to the class name. Some are not:
  - `BigAssMaverickRobotHand` → `BigAssMaverickRobot/Hand`; `Tree` → `Tree1`; `Rain` → `RainDrop`; `DecoratedGravityForce` → `GravityForce`; and others.
  - Duplicate TAGs: `SniperJoe` (`SniperJoe.kt` and `SniperJoe_OLD.kt`), `SpikeBot`, `ReactorMan`, `MechaDragon`, `AbstractItem`.

Human-readability:
- A stable ID does not need to be a number. It can be a readable string, such as `SniperJoe`. It only needs these properties:
  - (a) It is unique across the game, not per layer.
  - (b) It is not derived from the Kotlin class name or package.
  - (c) It never changes after maps use it.
  - (d) Renames add an alias.
- X6 uses a numeric type for each record (engine.md, "Spawning and despawning"). Maverick keeps the readable name and gets the same stability.
- Recommended form: PascalCase, equal to the current TMX name, for example `SniperJoe`. Then no map edits are needed. A dotted form such as `enemy.sniper_joe` would need all live TMX objects to be rewritten. That can be done with a script, but it gains little.

(2) Options

A. Hand-written registry in Kotlin.
- Example: `EntityDef(id = "SniperJoe", ctor = ::SniperJoe, layers = setOf(ENEMIES), aliases = listOf(...), prewarm = 2)`, collected in one list.
- Pro:
  - no reflection, so it works on web (GWT / TeaVM) and with R8;
  - explicit and easy to read;
  - it carries the schema (item 21) and the damage profile (item 22).
- Con:
  - one more place to edit for each new entity;
  - the list is long (~431).
- Fix for the con: a test that fails if a concrete `MegaGameEntity` subclass is not registered. Classpath scanning in test code only.

B. Annotation + KSP code generation.
- Example: `@EntityId("SniperJoe", aliases = [...])` on the class, and KSP generates the registry.
- Pro: the ID is next to the class, there is no manual list, and there is no runtime reflection.
- Con:
  - adds a KSP plugin, tied to the Kotlin version;
  - generated code is harder to debug;
  - the schema and profile still need a home.

C. Registry from a data file (YAML/JSON: ID → class, layers, aliases).
- Pro: one file that Tiled tooling and the validator can read.
- Con: it still needs codegen or reflection to make objects. The class-name strings break in the same way as now.

Effect on pooling:
- Pools are keyed by ID, or by class through the def.
- `prewarm` can be computed for each level: the validator counts objects per ID in the TMX.

Effect on web:
- A and B remove `Class.forName` and `kotlin-reflect`. This is a requirement for the web target, item 31 and the "Support web browsers and Android" backlog item.

(3) Recommendation

- Option A, with the def as a `companion object` value (`SniperJoe.DEF`), plus the registration test.
- Initial IDs = the current TMX names.
- Remove or rename the `_OLD` duplicates first (link to item 38).
- Resolve the TMX name through ID or alias. The layer becomes a validation rule, not a namespace.
- Delete `EntityFactories` after its 76 call sites move to `fetch(DEF)`.

(4) Open questions for the owner

- Must the ID stay equal to the atlas region prefix, or can the art keys separate from it later?
- Do you want IDs with a namespace (`enemy.*`) for clarity in large maps, or plain PascalCase names?

(5) Backlog wording

Title: "Entity registry with stable readable IDs and aliases"
- Each entity declares `EntityDef(id, ctor, layers, aliases, prewarm)`. The ID is a readable string that equals today's TMX name and is not tied to the class or package.
- The spawner resolves the ID or an alias. The validator warns when a map uses an alias.
- No reflection at runtime. A test fails for an unregistered entity class.
- Prewarm pools for each level from the TMX counts. Delete `EntityFactories`.

(6) Dependencies

Item 2 (the validator uses the registry), 3 (smoke test iterates the registry), 21, 22, 31, 38 (`_OLD` duplicates).
````
