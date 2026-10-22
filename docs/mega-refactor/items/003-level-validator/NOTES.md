# Notes: 003 Level content validator (local tool in `utils/`, CI test)

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Testing, CI and bug tracking (2026-10-22)

Copied as written from the review section "ITEM 2. Level content validator as a local tool in `utils/`".

````text
(1) Current state
- Resolution rules in the game:
  - `MegaMapLayerBuilders.build` processes only visible layers. A layer name with no builder is skipped at debug log level only (`MegaMapLayerBuilders.kt:49-57`).
  - `getLayerKey` maps `blocks_*` to `blocks` (`:25-28`).
  - `SpawnersLayerBuilder` maps the layer to an `EntityType` (`SpawnersLayerBuilder.kt:47-56`) and calls `Class.forName(EntityType.getFullyQualifiedName(name))` (`MegaEntityFactory.kt:47-48`). On failure it logs an error and `return@forEach`, which drops the object (`SpawnersLayerBuilder.kt:74-83`). `SensorsLayerBuilder.kt:30-35` does the same.
  - Enum properties are parsed lazily, for example `EventType.valueOf(str.uppercase())` for `events` (`SpawnersLayerBuilder.kt` near line 110).
  - The `spawn_room` property is read with `!!` and split by commas. Nothing checks that the room names exist in `game_rooms`.
- Recount: my rough check (visible spawner layers, class name searched by regex in the package folder) found 14 unresolved objects in 7 of the 18 maps listed in `LevelDefinition.kt`: DesertMan 5 (`Cactus` in `blocks`, `Bat` in `specials`), WilyStage2 2 (`Saw` in `enemies`), Test1 2, QuickRun 2 (`SpikeCopter` in `hazards`), GlacierMan 1 (`Sealion` in `blocks`), PreciousWoman 1 (`FirstSpawn` in `blocks`), WilyStage1 1 (`DuoBallCanon` in `blocks`). The brief says "14–30 objects in 9 of 18". The item should give the number that the real validator reports. Most of these are objects in the wrong layer, for example an enemy in `blocks`.
- Some layers have no builder on purpose: `children`, `temp`, `TODO`, and tile layers. So "unknown layer" needs an explicit allowlist, not a hard error.
- Existing `utils/` tools are standalone Python with `xml.etree.ElementTree` (`level_analyzer.py:4`), stdlib only (`level_builder`), and a `run.sh` plus `run_tests.sh` (venv + pytest). `utils-tests.yml` runs their tests. None of them knows Kotlin class names.

(2) Options
- A. Pure Python in `utils/level_validator/`, with hard-coded lists. Pros: same style as the other tools, starts fast. Cons: the class and enum lists drift from Kotlin as soon as an entity is added or renamed. That is exactly the bug class the tool must find. Rejected.
- B. Python, plus a Gradle task `core:exportContentManifest` that writes JSON: layer to package, class names per package, enum values (`EventType`, `SpawnType` and the others), and the layer allowlist. Pros: Python parsing and pytest, and the tool can reuse `level_analyzer` code. Cons: two languages for one rule set. The manifest can be stale locally unless `run.sh` runs Gradle first, and then the startup cost is the same as option C. The layer-mapping rules (`getLayerKey`, the `when` in `SpawnersLayerBuilder`) must still be copied into Python or exported as data.
- C. Kotlin JVM main in a small new Gradle subproject `tools` that depends on `core`, plus a thin `utils/level_validator/run.sh` that calls `./gradlew -q tools:validateLevels --args="$@"`. Pros: it calls the real `MegaMapLayerBuilders.getLayerKey`, `EntityType.getFullyQualifiedName`, `Class.forName`, `Enum.valueOf` and `LevelDefinition.entries`, so it cannot drift. It can also check that the class is a concrete `MegaGameEntity` with a `(game)` primary constructor. Parse TMX with JDK `javax.xml` or LibGDX `XmlReader`. Both are pure Java with no `Gdx` init and no natives. It becomes the natural home for item 21 (per-entity schema checks) and item 35 (checkpoint checks). Cons: Gradle startup (a few seconds with the daemon) and a compile after code changes.
- C-variant: put the main in `core/src/main`. Rejected, because then it ships in the game JAR.

(3) Recommendation
- Option C.
- Local use: `utils/level_validator/run.sh [tmx...] [--strict]` runs Gradle. With no arguments it checks every map in `LevelDefinition` and also all TMX files in `assets/tiled_maps/tmx/`, which reports unused maps as info. Output is one line per problem, in the form `file:objectId layer name: message`, with a severity.
- CI use: a Kotest spec in `tools/src/test` calls the validator on all `LevelDefinition` maps and fails on any error, so `./gradlew tools:test` in the item 1 job covers it. No Python is needed in CI for this.
- First rules:
  - Every object in a built layer resolves to a concrete class for its layer (error).
  - `spawn_type` and other enum-typed properties parse (error).
  - Every `spawn_room` name exists in `game_rooms` (error).
  - Unknown layer not on the allowlist (warning).
  - Visible layer with objects but no builder (warning).
  - Objects in a hidden layer (info).
- Fix the 14 current findings in a separate content commit or branch. Moving an object to the right layer makes it spawn, and that changes gameplay, so check that against the contract.

(4) Open questions
- For each finding: was the object meant to spawn (fix the layer) or was it kept as a note (move it to `temp`)? For example `Cactus` in DesertMan `blocks`.
- Which layer names are intentional non-built layers (`children`, `temp`, `TODO`, and others)?
- Should the validator also run in Tiled (through `utils/tiled_extensions`) later?

(5) Backlog wording
Title: Level content validator (Kotlin tool, local wrapper in `utils/`, CI test)
- New `tools` Gradle subproject with a validator main that reuses the `core` layer-key, `EntityType` and enum code. No copied lists.
- `utils/level_validator/run.sh` wrapper with `--strict`, and output in the form `file:objectId`.
- Rules: class resolves for its layer, enums parse, `spawn_room` names exist, unknown or hidden layers reported, intentional layers on an allowlist.
- A Kotest spec runs it on all `LevelDefinition` maps in CI.
- Fixing existing findings is a separate content change with its own contract note.

(6) Dependencies: item 1 (CI). Later extended by 20 (registry and aliases: validate aliases), 21 (schema), 35 (checkpoints) and 40 (inferred `spawn_room`).
````
