# 027: Non-null APIs instead of `!!`

- Status: planned
- Type: refactor
- Depends on: none
- Related: `021`, `028`
- Branches: none yet

## Goal

Remove the `!!` code smell (for example 298 `fetch(...)!!` calls). APIs return non-null values or fail with a clear message.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Non-null variants of the common lookups (`MegaEntityFactory.fetch`, `getTextureAtlas`, `getTextureRegion`, spawn property reads).
- A failure names what was asked for (class, atlas, region, key).
- Migrate the call sites. The registry (`021`) and the texture cache (`028`) remove many of them.

## Owner's input

Answer to review item 26 (2026-10-22, verbatim):

> yes, this is a major code smell

## Open questions

None yet. The scan for the first branch will find them.

## Sources

- `NOTES.md` in this folder.
