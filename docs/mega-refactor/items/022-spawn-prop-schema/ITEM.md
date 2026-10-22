# 022: Typed property schema per entity, exported to Tiled

- Status: planned
- Type: feature
- Depends on: `021`
- Related: `003`, `023`, `036`, `041`, `050`
- Branches: none yet

## Goal

Each entity declares its spawn properties with types and defaults, and Tiled shows them as typed fields.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Property descriptors (name, type, default, required, doc) on each `EntityDef`, plus shared groups (lifecycle, gravity).
- Export `propertyTypes`, enums and `.tx` templates into `megaman-maverick.tiled-project`. Export `schema.json` for the validator.
- Migrate entity by entity. The defaults stay equal to the current `getOrDefault` literals.
- CI fails when the exported files are out of date.

## Owner's input

Answer to review item 21 (2026-10-22, verbatim):

> yes, a schema per entity would be so much easier to work with

## Decisions

- (2026-10-22) Spawn props also carry the layer and the map the object came from (name, class, properties), so that layer-level and map-level properties can act as defaults. The exact shape (separate context objects or flattened props) is under discussion.

## Open questions

- Commit the exported Tiled files, or generate them locally?
- Set object classes on old maps by script, or only on new placements?

## Sources

- `NOTES.md` in this folder.
