# 026: Per-level render layers and an allocation-free draw queue

- Status: planned
- Type: refactor
- Depends on: none
- Related: `003`, `006`, `008`, `009`, `022`, `046`
- Branches: none yet

## Goal

Let each level define its draw order (for example tiles above some entities and below others), and draw with no per-sprite allocation.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Replace the `DrawingSection`/`DrawingPriority` literals with named render layers plus a small `order` int. A codemod changes the 258 sites.
- Each level defines an ordered render plan: backgrounds, tile layers drawn one at a time, and entity layers. Without a plan, a default reproduces today's order.
- The entity schema gives the default layer. A spawn property `render_layer` overrides it.
- One reusable bucket per layer with a stable sort. No per-sprite allocation. Delete `InsertionOrderPriorityQueue`.
- A debug overlay that lists layers with counts and toggles. The validator rejects unknown layer names.

## Owner's input

Answer to review item 25 (2026-10-22, verbatim):

> yes, and furthermore there's an issue where since the "render order" is global and fixed, it's hard to customize the rendering. for example, in certain levels, I might want to have tiles printed above certain entities and below certain entities; we need to rework the render ordering entirely to be much more flexible and also easier to work with

Answer to review item 45 (2026-10-22, verbatim):

> yes, and this ties into what I said above about making drawing order as configurable and easy to manage as possible

## Decisions

- F (2026-10-22, partial): a map property selects the render order: `legacy` (today's order; the value for existing maps), `default` (the order of the layers in the Tiled panel) or `custom` (an explicit order). The rest of F is still under discussion.

## Open questions

- Decision F, the rest: the owner suggested that the order of the layers in the level is the default render order (for example `tiles1`, `enemies1`, `tiles2`, `enemies2`), with ways to override parts or all of it. Proposed (2026-10-22): the game draws what the Tiled editor shows (the top layer in the panel draws last); each object layer is the render slot of the entities it spawns; entities spawned in code use their spawner's slot, unless the schema or a named slot overrides it; overrides from narrow to wide are a per-object `render_layer`, the schema default and the map's `custom` order. Confirm the draw direction and the rule for code-spawned entities.
- Must an entity have sprites in two layers at once?
- Must a background be able to appear in front of tiles or entities (fog, rain)?
- Must a whole tile layer change its layer at run time, or only its visibility?

## Sources

- `NOTES.md` in this folder.
