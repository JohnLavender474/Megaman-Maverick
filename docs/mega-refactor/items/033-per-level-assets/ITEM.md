# 033: Per-level asset manifests and texture budgets

- Status: planned
- Type: refactor
- Depends on: `028`
- Related: `004`, `006`, `021`, `022`, `025`, `032`, `039`
- Branches: none yet

## Goal

Load only the assets a level needs (today about 182 MB of textures and all music load at boot), and stay inside texture limits for phones and WebGL.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Derive each level's asset set from TMX names → registry → schema (`atlases`, `sounds`, `spawns`), plus a fixed core bundle.
- Load and unload per level with reference counts and a loading screen. Unload before load.
- A dev-mode strict check: a request outside the manifest throws with the entity and asset names.
- Cap atlas pages at 2048 (repack `WilyFinalBoss`, `Backgrounds3`). A per-level GPU budget report.
- A per-platform music format (MP3 or AAC on web). No full music preload on web.

## Owner's input

Answer to review item 32 (2026-10-22, verbatim):

> yes

## Open questions

- Is a short loading screen between levels acceptable?
- Must the select screens preview assets from many levels?
- Minimum Android device and browser (this sets the texture budget)?

## Sources

- `NOTES.md` in this folder.
