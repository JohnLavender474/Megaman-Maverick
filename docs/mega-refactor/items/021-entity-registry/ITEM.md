# 021: Entity registry with stable readable IDs and aliases

- Status: planned
- Type: refactor
- Objectives: `O7`, `O3`
- Depends on: none
- Related: `003`, `004`, `022`, `023`, `032`, `039`, `146`
- Branches: none yet

## Goal

Map TMX names to entities through an explicit registry, not through the Kotlin class name and package, and keep the names readable.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Each entity declares `EntityDef(id, ctor, layers, aliases, prewarm)`. The ID is a readable string equal to today's TMX name, not tied to the class or package.
- The spawner resolves the ID or an alias. The validator warns when a map uses an alias.
- No reflection at runtime. A test fails for an unregistered entity class.
- Prewarm pools per level from the TMX counts. Delete `EntityFactories` (76 call sites).
- Remove or rename the `_OLD` duplicates first (see `039`).
- Class-level facts go in the `EntityDef` (from `146a`, 2026-11-10): the default tags (they seed the `TagsComponent`
  that `MegaGameEntity.init()` adds; today the default is `this::class.simpleName`, which may not hold on TeaVM), the
  `EntityType`, and the spawn rules as data, the X6 way: the spawner decides from the record and the definition
  (difficulty, level traits, "already taken" flags) before it touches an instance. The instance `canSpawn` keeps only
  the rules that need the session. Name the base tags in each entry (no superclass walk: that needs reflection).
  Research: `items/146-player-marker-interface/branches/mega-refactor-146a-player-marker/docs/NOTES.md` and the
  `146a` spawn-flow notes.

## Owner's input

Answer to review item 20 (2026-10-22, verbatim):

> yes, this would mirror how it's done in X6. One issue though is that when building levels in TMX, it's easier when objects have human-readable names rather than ids. Think about the human-readability aspect, too

## Decisions

- (2026-10-22) The layer stops being a namespace. The object name resolves to an entity ID across the whole game, so layer names become free text. The old allowed-layer list stays only as a validator warning. Structural layers (`game_rooms`, `player`, tile layers, backgrounds) are recognized by their Tiled layer class, not by their name.

## Objectives audit (2026-11-01)

- The scope must also name `SensorsLayerBuilder.kt:30`, `GauntletMaster.kt:143`, `HeartTank.kt:143-146` and `GameEntityUtils.kt:123`.
- "The layer stops being a namespace" makes 9 dead objects on real stages start to spawn (inference). The contract must decide this first.

## Open questions

- Must the ID stay equal to the atlas region prefix?
- IDs with a namespace (`enemy.*`) or plain PascalCase names?

## Sources

- `NOTES.md` in this folder.
