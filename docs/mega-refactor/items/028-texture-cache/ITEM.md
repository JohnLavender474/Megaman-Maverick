# 028: Game-owned texture and animation cache

- Status: planned
- Type: refactor
- Depends on: none
- Related: `004`, `016`, `021`, `022`, `033`, `040`
- Branches: none yet

## Goal

Replace about 200 companion-object region maps with one cache that the game owns, so that no static holds GPU resources.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- One `TextureCache` keyed by atlas + region. Index each atlas once on load (O(1) lookups).
- Clear the cache on atlas unload and on game dispose. No statics hold GPU resources (Android-safe).
- Missing-region errors name the atlas, the region and the requesting entity, and list all misses at once.
- `AnimationDef` holds atlas + region. Entities request animations by key. Shared frames, per-entity copies.
- A prerequisite for `033`.

## Owner's input

Answer to review item 27 (2026-10-22, verbatim):

> yes, the cache is not implemeted well currently

## Open questions

- Animation defs in Kotlin data objects first, or straight in JSON or YAML?
- Do some entities choose atlases at run time (for example by level variant)?

## Sources

- `NOTES.md` in this folder.
