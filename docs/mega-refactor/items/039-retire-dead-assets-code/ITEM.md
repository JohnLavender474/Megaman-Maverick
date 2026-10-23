# 039: Retire unused assets and review dead code

- Status: planned
- Type: tooling
- Depends on: none
- Related: `003`, `021`, `025`, `033`, `040`, `043`
- Branches: none yet

## Goal

Move unused assets to a retired location that is not shipped, and let the owner review dead code before anything is removed.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `retired/` at the repo root with the same paths inside. Use `git mv` so the history follows. Never bundled.
- A Gradle staging task with an allow-list for packr, so source art and unlisted maps are not shipped.
- `utils/dead_code` generates review sheets (code and assets) with evidence and a verdict column.
- Apply the owner's verdicts: delete (with a SHA index), retire, or mark `@NotYetUsed`.
- Known candidate (from `029a`, 2026-10-23): `entities/megaman/sprites/MegamanTrailSprite.kt` (V1) and its pool in the deprecated `DecorationsFactory`. Nothing fetches it. It is the last read of the global `megaman` in the player package, so `029f` (which removes the `megaman` extension) must delete it if this item has not.

## Owner's input

Answer to review item 38 (2026-10-22, verbatim):

> Instead of deleting "dead" assets, let's just put them into a "retired" location that's not bundled with the game. This way, I can still support versioning, experimentation, etc. For the dead code, we'll need to identify what is truly dead versus "not used yet" or "not used because another class/file offers a different version that the game currently uses". It might be case that it's all dead code, but I want to comb through it first

## Open questions

- Do old TMX maps go to `retired/`, or stay but outside the shipped list? Moving them can break tileset paths.
- Is git history enough for retired code, or do you also want the files on disk?

## Sources

- `NOTES.md` in this folder.
