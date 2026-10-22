# 040: Atlas checks and one-command repack (keep TexturePacker)

- Status: idea
- Type: tooling
- Depends on: none
- Related: `002`, `004`, `028`, `033`, `039`
- Branches: none yet

## Goal

Get the value of a build task (checks and a one-command rebuild) without leaving TexturePacker.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A check that every `findRegion` name exists in its atlas (CI, no license needed).
- A check of `.tps` hygiene: source folders exist, every atlas has a project, pages are not more than 2048.
- `utils/atlas/pack-all.sh` that uses the TexturePacker CLI.
- Fix the 12 stale `.tps` files and add the 3 missing ones.
- Optional: animation definitions as data.

## Owner's input

Answer to review item 39 (2026-10-22, verbatim):

> I've already paid for TexturePacker so I'm not too concerned about this. What benefits are there to making a gradle task versus just continuing with the same atlas setup?

## Open questions

- Will other people edit art? (Only then is a Gradle packer worth it.)
- Are the 12 stale projects still needed, or do they go to `retired/`?

## Sources

- `NOTES.md` in this folder.
