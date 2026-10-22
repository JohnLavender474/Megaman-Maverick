# 003: Level content validator (local tool in `utils/`, CI test)

- Status: planned
- Type: tooling
- Depends on: `002`
- Related: `021`, `022`, `036`, `041`
- Branches: none yet

## Goal

Find TMX objects that the game skips without a warning, and other content errors, before a play test.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A validator that reuses the real `core` code (layer keys, `EntityType`, enums), so no list is copied and nothing drifts. Recommended: a new `tools` Gradle subproject in Kotlin.
- A wrapper `utils/level_validator/run.sh [tmx...] [--strict]` for local use. Output is one line per problem in the form `file:objectId layer name: message`.
- Rules: the class resolves for its layer, enums parse, `spawn_room` names exist, unknown or hidden layers are reported, intentional layers are on an allowlist.
- A Kotest spec runs it on all `LevelDefinition` maps in CI.
- Fixing the existing findings (about 14 objects in 7 maps) is a separate content change, because a fix makes an object spawn.

## Owner's input

Answer to review item 2 (2026-10-22, verbatim):

> Yes, and the level-content validator should be a tool that can be invoked locally; see the `utils` folder at the project root

## Open questions

- Decision C: where do the level validation rules live? Proposed (2026-10-22): the rules in `engine` (a pure `validate(map, registry)` with no `Gdx`, because rooms, layer kinds, spawners and checkpoints are genre concepts), the content (entity registry, schemas, allowed layer names) in `core`, which also calls `validate` at level load in dev runs, and only a thin CLI runner plus the CI test in a `tools` subproject (a CLI main in `core` would ship in the game JAR). The owner asked whether one project makes this simpler; decision P keeps one repo with the `engine` / `core` split, so the proposal still fits. Not confirmed yet.
- For each finding: was the object meant to spawn, or is it a note that belongs in `temp`?
- Which layer names are intentional non-built layers (`children`, `temp`, `TODO`, others)?
- Should the validator also run inside Tiled later (`utils/tiled_extensions`)?

## Sources

- `NOTES.md` in this folder.
