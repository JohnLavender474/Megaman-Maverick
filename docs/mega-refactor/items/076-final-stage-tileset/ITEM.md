# 076: Better tileset art for every level

- Status: active
- Type: feature
- Objectives: `O9`, `O4`
- Depends on: none
- Related: `067`, `077`
- Branches: `mega-refactor-076a-desert-man-tileset` (merged, 2026-11-17, squash `2bede4d69`), `mega-refactor-076b-desert-man-backgrounds` (active, 2026-11-17)

## Goal

The game uses a mix of tilesets. Some are high-quality art from commissioned artists. Others come from free asset
packs: they are not bad, but they do not have the detail or the art style that the game needs. Give each level
tileset art of the same high quality, so that the game has one consistent look.

Widened on 2026-11-17 (owner) from "A new tileset for the final stage", in the same way as `077` was widened to cover
every boss. The final stage stays in the item as one of the levels.

This item changes how the levels look. It must not change the block geometry or the gameplay unless the scope says
so.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Sort the tilesets of the live levels: keep (high-quality art) or replace (free asset pack, or not good enough).
  The owner decides each tileset. `NOTES.md` lists the tilesets of each live level.
- For each tileset to replace: choose or make new art, and paint the level tiles with it.
- Keep the block objects as they are, so that each level plays the same.
- One branch per level or per tileset, because a tileset can be in more than one level (for example `Tileset1.tsx`).
  The owner picks each level as the work goes on.

## Owner's input

2026-10-29 (verbatim):

> The final level of the game uses a generic brick-like tileset that is lackluster for the final stage.

2026-11-17 (verbatim):

> Right now, the game is a mishmash of tilesets, some of them high quality designed by commissioned/talented
> artists, while others are from free asset packs that while not bad aren't detailed or "artistic" enough for this
> game.

> Widen 076, similar to how 077 is widened to cover all bosses

## Decisions

- 2026-11-17 (owner): the item covers the tilesets of every level, not only the final stage.
- 2026-11-17 (owner): the first level is Desert Man. Its tileset is fully new and original art, based on the current Desert Man tilesets, drawn in phases (rock, steel, props, sand, oasis). Drafts: `temp/076-desert-tiles/` in the main checkout.
- 2026-11-17 (owner): keep the gap-0 vertical rock joins (a rock mass that stands on another keeps the sand lip between them).
- 2026-11-17 (owner): merge target: each branch squash-merges directly into `master`.
- 2026-11-17 (owner): the item also covers the level backgrounds where they do not match the new tiles. Desert Man first (`076b`): a simpler, Mega Man-like style; the round shapes are hills, not buildings; the sunset with heat haze only after the cave section.

## Open questions

- Which tilesets are high quality (keep), and which need new art (replace)?
- Is "the final level" `WILY_STAGE_3` (it holds `WilyFinalBoss`)? The owner confirms.
- Where does the new art come from: commissioned art, new art made in the repo, or an existing tileset in
  `assets/tiled_maps/tsx/`?
- Does this wait for `067` (PPM 16 with 1x pixel art)? New art made at 2x before `067` can need a second change.

## Sources

- `NOTES.md` in this folder.
