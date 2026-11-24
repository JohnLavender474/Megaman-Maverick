# Plan: mega-refactor-185a-lost-memories-launcher

## Steps

### Wave 1 (parallel, no shared files)

1. **Title hook in `core`** (agent A). `IGameTitle`, `MaverickTitle`, wiring in
   `MegamanMaverickGame`, `LaunchOptionsResolver`, `PlayerSettingsStore`. Check: `core:test`.
2. **`desktop-common` module** (agent B). Move `StartupHelper` and the generic launcher code;
   `lwjgl3` `DesktopLauncher` becomes a thin call. Folded bug `version`. Check: `lwjgl3:jar`.

### Wave 2

3. **Lost Memories modules** (agent C). `lost-memories-core` (title, splash screen) and
   `lost-memories-desktop`. Check: `lost-memories-desktop:jar`.
4. **Run scripts and CI** (agent D). Check: `bash -n` on each script; read the CI file.

### Wave 3

5. Review before the play test (one Sonnet agent), fixes, play scripts in `temp/play/`.
