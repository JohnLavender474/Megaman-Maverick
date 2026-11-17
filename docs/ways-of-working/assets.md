# Assets

Read this file before you add or change frames, atlases or other asset files. How to draw the art is in `art.md`.

- **Pack atlases with `Basic` only** (owner, 2026-11-17, branch `076b`). Atlases are packed with TexturePacker (the
  `TexturePacker` CLI and the `.tps` projects in `assets/sprites/texture_packer/`, which pack a folder of
  `assets/sprites/frames/`). Use only the `Basic` algorithm, the setting in the projects. Do not use `MaxRects` or
  rotation, also not as a command-line override. When the frames do not fit the maximum page size of a project with
  `Basic`, move frames into a new atlas: a new frames folder, a new `.tps` project and a new `TextureAsset` entry.
  After a repack, check by script that each region that did not change keeps its name and its pixels.
