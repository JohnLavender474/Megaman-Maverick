# Notes: 033 Per-level asset manifests and texture budgets

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEM 32: Per-level asset loading and texture budgets".

````text
1. Current state

- `queueAssets()` (`MegamanMaverickGame.kt:795-806`) queues every `MusicAsset` (77), every `SoundAsset` (83), every `TextureAsset` (66 atlases) and every level TMX at boot. `assMan.update(ASSET_MILLIS)` is called each frame (`:426-427`). Nothing is ever unloaded.
- Textures: 66 atlas pages, all RGBA8888, Nearest filter, `repeat: none`. The decoded GPU size is about 182 MB, from the sum of width × height × 4 over the `size:` lines. On disk, `assets/music` is 199 MB (93 `.ogg`, 16 `.mp3`), `sounds` is 1.7 MB and the sprite sheets are 2.5 MB.
- Two pages are larger than 2048: `WilyFinalBoss.txt` is 2048×2688 and `Backgrounds3.txt` is 2000×2172. Many pages have sizes that are not powers of two. This is acceptable for WebGL1 with Nearest and no repeat, but it blocks GL repeat (see item 46).
- Atlas use is in code: for example `TextureAsset.ENEMIES_1` is referenced 75 times and `PROJECTILES_1` 51 times. Entities spawn other entities in code, so the TMX content alone does not give the full set.

2. Options

A. Hand-written manifest per level (a list of atlases, sounds, music). Simple, but it drifts out of date.
B. Derived manifest: TMX object names → registry (item 20) → schema (item 21), which declares `atlases`, `sounds` and `spawns` (other entity ids). Take the transitive closure. Add the level's backgrounds, tilesets and music.
C. Lazy loading on first request, with no manifest. Simple, but it causes hitches in play and errors in the middle of a level.

3. Recommendation

Use B, with a "core bundle" and a dev-mode check.
- Core bundle, always loaded: Mega and other players, HUD/UI, common explosions, buster shots, items and common sounds.
- Level bundle = closure over the schema `spawns` lists + backgrounds layer + tilesets + music (`LevelDefinition` and room `music` properties).
- On a level change: new set − old set → load, old set − new set → unload. Use `AssetManager` reference counts. Unload before load, so that the peak memory stays low. Clear the cache entries (item 27). Reuse the `update(ms)` progress for a loading screen.
- Dev mode "strict manifest": any request for a region or sound outside the loaded set throws and names the missing schema entry. The headless run (item 5) and the smoke test (item 3) find gaps before release.
- Texture cap 2048 (a safe floor for old phones and WebGL): set the TexturePacker max size to 2048 and repack the 2 large pages.
- Music for web: some Safari/iOS versions do not play OGG Vorbis, so ship MP3 or AAC (`.m4a`) for web and keep OGG for desktop and Android. Map the format per platform at build time. Do not load 199 MB at boot on web: stream or fetch per level. Watch for loop gaps: MP3 encoder padding can add a gap at the loop point. The intro/loop split (`MusicAsset.onCompletion`) can give an audible break.
- Risks: missed transitive spawns (mitigated by strict mode and tests). A hitch on level enter (loading screen). Bosses reused in several levels (they must count as shared). Pool prewarm (item 20) needs its assets loaded first.

4. Open questions for the owner

- Is a short loading screen between levels acceptable?
- Must the level-select or stage-select screens preview assets from many levels?
- What is the minimum Android device and browser you want to support (this sets the texture budget)?

5. Proposed backlog wording

Title: Per-level asset manifests and texture budgets
- Derive each level's asset set from TMX names → registry → schema (`atlases`, `sounds`, `spawns`), plus a fixed core bundle.
- Load and unload per level with reference counts and a loading screen. Unload before load.
- Dev-mode strict check: a request outside the manifest throws with the entity and asset names.
- Cap atlas pages at 2048 (repack `WilyFinalBoss`, `Backgrounds3`). Set a per-level GPU budget and report it.
- Per-platform music format (MP3/AAC on web). No full music preload on web.

6. Dependencies

20, 21, 27 (must come first), 24 (level manifest), 3 and 5 (checks), 38 (retiring unused music reduces the size), 31 (web and Android targets).
````
