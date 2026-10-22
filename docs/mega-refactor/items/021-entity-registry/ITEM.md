# 021: Entity registry with stable readable IDs and aliases

- Status: planned
- Type: refactor
- Depends on: none
- Related: `003`, `004`, `022`, `023`, `032`, `039`
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

## Owner's input

Answer to review item 20 (2026-10-22, verbatim):

> yes, this would mirror how it's done in X6. One issue though is that when building levels in TMX, it's easier when objects have human-readable names rather than ids. Think about the human-readability aspect, too

## Open questions

- Must the ID stay equal to the atlas region prefix?
- IDs with a namespace (`enemy.*`) or plain PascalCase names?

## Sources

- `NOTES.md` in this folder.
