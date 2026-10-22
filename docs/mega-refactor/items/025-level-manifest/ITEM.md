# 025: Level manifest: traits, variants and rewards instead of level-identity checks

- Status: idea
- Type: refactor
- Depends on: none
- Related: `007`, `013`, `021`, `024`, `036`, `040`
- Branches: none yet

## Goal

Remove code that checks which level is running (9 `MOON_MAN` checks, the Inferno frozen variant), and describe levels with traits and variants.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `environment` traits (low gravity, vacuum, dark) and `variants` conditions on `LevelDefinition`. Replace the `MOON_MAN` and Inferno frozen checks.
- TMX objects can carry `variant=<name>` / `variant=!<name>`, and the spawner filters them.
- Move the weapon reward, the select position and the gauntlet preset into the manifest. Move the boss intro definition to the boss def.
- A stable `saveId` for each level, separate from the enum name.
- Move the per-spawn moon scalars to level defaults (see `036`).
- Benefit: a new low-gravity or variant level needs data, not code in many files. A test level can turn on `LOW_GRAVITY`.

## Owner's input

Answer to review item 24 (2026-10-22, verbatim):

> let's discuss this further, what benefits would we see? examples?

## Open questions

- Will more robot master levels or user-made levels be added?
- Should "moon" be a level trait or a room trait?

## Sources

- `NOTES.md` in this folder.
