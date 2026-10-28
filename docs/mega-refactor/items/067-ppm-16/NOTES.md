# Notes: 067 PPM 16 with 1x pixel art

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-28: research (read-only agent, `master` at `932773168`)


Research date: 2026-10-28. Base: `master` at `932773168` (the scan started there; `master` moved to `edf3524af`
during the scan, with no change to the scanned files that this note depends on). All scripts and CSV files are in the
same scratchpad folder as this note (see "Output files").

### 1. Summary

- **The sprite art is not 2x in the files.** It is 1x art (16 art pixels for each tile) that the code draws at
  2 world units for each texel. Of the 2,227 atlas regions that the game loads, 2,088 (93.8 %) have mixed 2x2 blocks,
  so they are native 1x art. Halving them is lossy and also not necessary.
- **Only the Tiled tilesets and a small part of the atlases are 2x in the files.** All 33 tilesets that the live maps
  use have 32x32 tiles. 4,348 of 4,638 used tiles (93.7 %) halve with no loss. 139 atlas regions (6.2 %) are 2x art.
- **The code already draws in tile units.** 3,709 of 4,239 `ConstVals.PPM` uses are scale factors (`X * ConstVals.PPM`).
  No code sets a sprite size from the texel size of a region. So if `PPM` changes, every sprite keeps its size on the
  screen, and the 1x art becomes 1 world unit for each texel with no asset change.
- **The maps are in 32-unit pixels and the loader does not convert them.** The engine reads map objects with no unit
  scale, and `OrthogonalTiledMapRenderer` uses the default `unitScale = 1`. A change of `PPM` without a map conversion
  breaks every level.
- **Halving all lengths by 2 is exact in IEEE `float`**, because 2 is a power of 2. The simulation stays bit-identical
  in tile units, except at a known list of sites that use `PPM` for non-length values, integer rounding or fixed
  thresholds (section 4).
- **Recommendation:** do not halve the sprite art. If the owner still wants world unit = art pixel, use option C
  (section 6): a map scale at load time, a fix for each non-length use of `PPM`, then a one-line change of `PPM`,
  proven with a dual-`PPM` trace comparison.

### 2. Is the art 2x? (question 1)

Method: `atlas_check.py` parses each atlas `.txt` (both libGDX formats: `xy`/`size`/`orig` and `bounds`/`offsets`) and
checks each region rectangle. A region is halvable when its size is even and every 2x2 block, aligned at the region
origin, has 4 equal RGBA pixels (4 fully transparent pixels count as equal). The script also tries the 3 other phases
(offset by 1 pixel in x, y or both). `tmx_check.py` does the same for each used tile of each tileset of the live maps.
`frames_check.py` checks the source frames.

### 2.1 Atlases (`assets/sprites/sprite_sheets/`)

- 84 atlas files with one page each (86 PNG files; `Bosses1.png` and `LevelSelectScreen-0.png` are not referenced; `Bosses1.txt` uses `Bosses1-0.png`). `TextureAsset.kt` loads 66 atlases (66 pages).
- All atlas files have `filter: Nearest`, `rotate: false` and `index: -1` for nearly all regions (2,969 of 2,979 old-format
  regions). No region is rotated.
- Live regions: 2,227.

| Class (live regions) | Count | Meaning |
|---|---|---|
| Native 1x (mixed 2x2 blocks at every phase) | 2,088 | Halving loses pixels. |
| 2x, even size, even phase | 119 | Halvable with no loss. |
| 2x at an odd phase (1-pixel shift) | 19 | Halvable only with a 1-texel crop, which moves the image by half an art pixel. |
| 2x, odd size | 1 | `Hazards1.txt` `Spike1` (33x31). |

- 806 live regions start at an odd offset in their page (TexturePacker `shapePadding`, `borderPadding` and `extrude`
  of 1 or 2 pixels). So a whole page cannot be halved. Only 2 live pages halve as a page: `Colors.png` (solid colors)
  and `Bits.png` (a false positive: its 1x regions sit at odd offsets with 1-pixel extrude).
- 25 live regions have an odd width or height.
- 4 live pages have an odd size: `Backgrounds5.png` (300x225), `Enemies2.png` (1008x1819), `Specials1.png` (256x1137),
  `Test.png` (185x235).
- Examples of native 1x art: `Megaman_v3.txt` `airdash` is 48x48 texels with a 24-pixel-wide figure drawn at 1x;
  `Megaman` draws it at `MEGAMAN_SPRITE_SIZE * ConstVals.PPM` = 96 world units
  (`entities/megaman/components/SpritesComponent.kt:29`, `:121`). `Met` regions are 32x32 and the sprite is
  `2f * ConstVals.PPM` = 64 world units (`entities/enemies/Met.kt:305`). Both are 2 world units for each texel.
- The 256x224 UI screens (`LevelSelectScreen*`, `LevelPauseScreen*`, `Backgrounds*` 256x224) have very few mixed blocks
  (0.2 % to 1.5 %) because of large flat areas, but they are NES-resolution 1x images.
- Examples of 2x art: `Platforms1.txt` `Brick`, `FrozenBlock`, `GoldBlock`; `Hazards1.txt` `Lava/*`, `Spike4`..`Spike7`;
  `Environs1.txt` `QuickSand*`; `Enemies2.txt` `Hanabiran/*`, `JetMet/*`, `Robbit/*`; `Backgrounds2.txt` `BKG11`..`BKG16`.
  Full list: `atlas_regions_classified.csv`, column `class`.

### 2.2 Tilesets of the live maps

- 20 live maps (all `LevelDefinition` entries). Every map has `tilewidth="32"`, `tileheight="32"`.
- 33 tilesets, all 32x32 tiles, spacing 0, margin 0, no `tileoffset`.
- 4,638 distinct used tiles; 4,348 halve with no loss; 290 do not:

| Tileset | Used | Not halvable | Maps |
|---|---|---|---|
| `RumblecadeFactory.tsx` (`rumblecade_factory_default.png`) | 282 | 278 | `RumblecadeFactory.tmx` |
| `MoonManTiles.tsx` | 42 | 5 | `MoonMan_16x14_v5.tmx` |
| `Gravity Man Stage.tsx` | 6 | 5 | `Test1.tmx` |
| `mm9jewel.tsx` | 4 | 1 | `PreciousWoman_v2.tmx` |
| `UnderwaterFanPillar_Gray.tsx` | 24 | 1 | `WilyStage2_v2.tmx` |

- `CrystalManTileset.png` is 800x305 (odd height), but its used tiles halve.
- 6 tilesets have animated tiles (`CrystalManTileset` 22, `RumblecadeFactory` 6, `Wily2` 2).
- No live map has an image layer.

### 2.3 Other images and fonts

- `Megaman.png`: window icon only (`DesktopLauncher.kt:26`, `:118`). Not affected.
- `Megaman.kt:367` loads `sprites/frames/Megaman_v2/*.png` directly, but the code is inside a block comment
  (`Megaman.kt:347`..`:398`). Not live.
- Font: one TrueType font, `assets/Megaman10Font.ttf` (`unitsPerEm` 1024). `FreeTypeFontGenerator` rasterizes it at
  `(ConstVals.PPM / 2f).roundToInt()` = 16 px (`utils/MegaUtilMethods.kt:36`, used by `MegaFontHandle.kt:62`, `:83`).
  At `PPM = 16` the size is 8 px. If the font has an 8-pixel design grid (1024 / 8 = 128 units), 8 px is its native size.
  A visual check is necessary.

### 2.4 Repack from sources

- `assets/sprites/texture_packer/` has 86 `.tps` files (TexturePacker 7.4, `libgdx` format, `scale` 1, trim `None`,
  `allowRotation` true but no rotated output). Settings differ for each atlas: `shapePadding` 0 or 2, `borderPadding` 0
  or 2, `extrude` 0 or 1.
- 56 of the 66 live atlases have a `.tps` with the same name. `MegamanChargedShot` has none. The 9 `Megaman_v3_*`
  weapon atlases have `.tps` files with other spellings (`Megaman_v3_Axe.tps`, ...).
- 12 live `.tps` files (`BluntMan`, `DistributorMan`, `FreezerMan`, `GutsTank`, `MicrowaveMan`, `MisterMan`, `NukeMan`,
  `PreciousMan`, `RoasterMan`, `RodentMan`, `TimberWoman`, `WintryMan`) point to `../Frames/Bosses/...`, which does not
  exist. They cannot repack without a path fix.
- `assets/sprites/frames/` has 3,132 PNG files. Only 120 of them halve with no loss. So the sources are also 1x art.
  A repack from halved sources is not possible for the sprites, and is not necessary.
- 401 `.piskel` files exist in `assets/sprites/piskel/`.

### 2.5 Verdict

The statement "all artwork is scaled 2x" is true at draw time, not in the files. In the files:

- Sprites, UI and most backgrounds (2,088 regions): 1x. No change is necessary for `PPM = 16`; halving is lossy.
- Tilesets (4,638 used tiles): 2x. 4,348 halve with no loss; 290 do not.
- Atlas regions (139): 2x. 119 halve with no loss; 20 need a shift or have an odd size.

### 3. Tiled maps (question 2)

- Tile size: 32x32 in all 20 live maps. Map sizes are in `tmx_summary.csv`.
- Objects: 6,910 objects in the live maps. All `x`, `y`, `width`, `height` and polygon or polyline points are whole
  numbers, except:
  - 16 `FallingLeaves` objects in `TimberWoman_16x14_v4.tmx` (`decorations` layer) with `height="61.8843"` and 2 with
    `y="1921.06"`.
  - 4 odd values: `DesertMan_16x14_v4.tmx` `Cactus` id 11 (`y=2847`), `PreciousWoman_v2.tmx` `PreciousBlock` id 164
    (`y=2207`), `SMB3_1.tmx` `CollideDownOnlyBlock` ids 40 and 41 (`height=3`).
  - All others are even. Halving a decimal value is exact in `float` (it is a power-of-2 scale), so the 20 exceptions
    give `.5` or longer decimals, not an error. List: `tmx_objects_not_even.csv`.
- No layer has `offsetx` or `offsety`. 4,278 tile cells in the live maps use flip flags (no effect on scale).
- Custom properties: 45 numeric property names. All length-like values are in tiles and the code multiplies them by
  `PPM`, for example `radius` (`LaserBeamer.kt:132`), `offset_x` (`BackgroundLayerBuilder.kt:239`), trajectories
  (`Trajectory(..., ConstVals.PPM)` in `RocketPlatform.kt:93`, `PreciousBlock.kt:77`, `PropellerPlatform.kt:91`,
  `BulbBlaster.kt:123`). No pixel-valued custom property was found. List: `tmx_object_properties.csv`.
- Loader: `MegamanMaverickGame.kt:861` uses a plain `TmxMapLoader`. `TiledMapLoader.kt:30` reads `tilewidth`.
  `TiledMapLevelRenderer` extends `OrthogonalTiledMapRenderer(map, batch)` with the default `unitScale = 1`
  (`engine/.../tiledmap/TiledMapLevelRenderer.kt:9`). Layer builders read `mapObject.rectangle` and other shapes as
  world units directly (222 reads in 117 files; central helpers in `utils/extensions/Extensions.kt:54`..`:97`). So one
  map pixel is one world unit, and one tile = 32 = `PPM`. The loader does not divide by `PPM` or by the tile size.
- Other places that assume a 32-unit tile in the map:
  - `MegaLevelScreen.kt:452`..`:467`: the quadtree world container takes `ppm = tmr.tileWidth` and the world size as
    `tiles * tileWidth`. Only with `--worldContainer quadtree` (the default is `simple`, `DesktopLauncher.kt:51`).
  - `utils/level_builder/level_builder.py:8` (`TILE = 32`), the `level-builder`, `level-designer` and
    `gauntlet-stages` skills, and `utils/tiled_extensions/`.

### 4. Code (questions 3 and 4)

### 4.1 Counts

`ppm_classify.py` classifies each `ConstVals.PPM` use in `core/src/main`, `engine/src/main` and `lwjgl3/src/main`
(comments excluded): 4,239 uses on 3,547 lines in 467 files. Output: `ppm_uses.csv`.

| Class | Count | Stays correct if `PPM` changes? |
|---|---|---|
| (a) `X * ConstVals.PPM` scale factor | 3,709 | Yes, if `X` is a length in tiles (see 4.2 for exceptions). |
| (a) `ConstVals.PPM.toFloat()` (one tile) | 456 | Yes. |
| (a) `ConstVals.PPM / x` | 36 | Yes for float division. 3 font sizes round (see 4.3). |
| (a) `value / ConstVals.PPM` (to tiles) | 22 | Yes. |
| (a) `PPM` as a parameter | 10 | Yes. |
| (a) `+`/`-` one tile | 6 | Yes. |
| Same line has `toInt`, `roundToInt`, `floor` or `ceil` | 36 | Mostly tile counts of a tile-aligned box; exact if the box is a whole number of tiles. |

### 4.2 `PPM` used for values that are not lengths (behavior changes at `PPM = 16`)

These multiply a rate, an angular speed or a frequency by `PPM`. At `PPM = 16` the value halves and gameplay or visuals
change. Fix: replace `ConstVals.PPM` with a fixed constant that keeps the current value (32).

- Friction rates (`Body.kt:104`..`:107` uses `exp(-frictionOnSelf * delta)`, so `frictionOnSelf` is 1/s):
  - `world/contacts/rules/FeetIceRule.kt:24`, `:36`
  - `world/contacts/rules/SideIceRule.kt:24`
  - `world/contacts/rules/FeetSandRule.kt:34`, `:56`
  - `world/contacts/rules/FeetSnowRule.kt:27`
  - Test: `core/src/test/.../IceRulesTest.kt:42`, `:63` assert `ICE_FRICTION * ConstVals.PPM`.
- Angular speed of `RotatingLine` (degrees per second, `engine/.../motion/RotatingLine.kt:36`):
  - `entities/bosses/SigmaRatClaw.kt:123`, `entities/special/RotationAnchor.kt:85`, `entities/enemies/PetitDevil.kt:270`,
    `entities/hazards/FireballBar.kt:66`, `entities/hazards/Saw.kt:168`, `entities/hazards/LaserBeamer.kt:136`
- `SineWave` frequency (time domain, `engine/.../motion/SineWave.kt`):
  - `entities/decorations/FloatingEmber.kt:109`, `entities/decorations/Snowfall.kt:124`, `:127` (visual only)

This list comes from targeted searches (friction, rotation, frequency). Other non-length uses can exist; the
dual-`PPM` trace in section 5 finds them.

### 4.3 Hard-coded pixel numbers and integer math (class b)

- `engine/.../fonts/BitmapFontHandle.kt:34`: `DEFAULT_FONT_SIZE = 10` (pixels). `notificationFont`
  (`MegamanMaverickGame.kt:350`) uses it, so its text doubles in size on the screen at `PPM = 16`.
- `utils/MegaUtilMethods.kt:34`, `:38`: small and large font sizes `(PPM / 3f).roundToInt()` (11 -> 5, not 5.5) and
  `(PPM / 1.5f).roundToInt()` (21 -> 11, not 10.5). No caller uses them now.
- `engine/.../viewports/PixelPerfectFitViewport.kt:20`: the integer scale is per world unit. At `PPM = 16` the integer
  steps are twice as fine, so `--pixelPerfect` shows a larger image at some window sizes, for example 1280x720
  (448 -> 672 pixels high) and 3840x2160 (1792 -> 2016). It is the same at 640x480 (the default), 1920x1080 and
  2560x1440. Also used by `Background.kt:73`, `:90` and `MegamanMaverickGame.kt:321`, `:332`, `:681`, `:682`.
- `engine/.../shapes/GameRectangle.kt:423`, `:424`: `splitIntoCells` rounds the cell size up to a whole world unit.
  Gameplay caller: `entities/enemies/Beezee.kt:297` (hover targets). Visual callers through `splitByCellSize`:
  `RisingLavaRiver.kt:240`, `DecoratedGravityForce.kt:84`, and 7 callers with a cell of one tile (exact).
- `entities/enemies/MockingByte.kt:548`, `:593`: a sort comparator returns `(dst2 - dst2).toInt()`. Differences below
  1 world unit squared become "equal". At `PPM = 16` more pairs tie, so the chosen return position can change.
- `entities/hazards/Electrocutie.kt:117`, `:155` (`roundToInt() / ConstVals.PPM`), `entities/decorations/WindyGrass.kt:64`
  (`toInt() / ConstVals.PPM`): integer division, exact only if the bounds are whole tiles.
- `entities/special/Darkness.kt:148`..`:165`: `ConstVals.PPM / ppmDivisor` is integer division. Exact for divisors 1, 2,
  4, 8, 16 at `PPM = 16`. The default is 4. Only `Test1.tmx` uses `Darkness`.

### 4.4 Rounding, epsilons and order (class c)

- `engine/.../container/SimpleGridWorldContainer.kt:49`: `value % 1f` checks for a whole world unit and moves the value
  by `MathUtils.FLOAT_ROUNDING_ERROR` (1e-6). It only changes the result on a cell boundary, which is a whole number at
  both scales. Near the origin the nudge has a different effect at 16 and 32 because of `float` spacing. It changes only
  the broad-phase candidate set; the final overlap test is exact. Low risk.
- `entities/bosses/GutsTank.kt:366`: `epsilonEquals(..., 0.01f)`, not scaled by `PPM`.
- Division guards `dist > 0.0001f`: `WilyFinalBoss.kt:2693`, `:2711`, `:3210`, `WilyCapsuleTentacle.kt:276`,
  `WavyTentacleOfJoints.kt:137`, `TestTentacle.kt:398`. Negligible.
- All other epsilons found (about 30 sites, for example `0.1f * ConstVals.PPM`) scale with `PPM`.
- Hash order: `ObjectSet<Vector2>` iteration depends on the float bits of world positions:
  `world/contacts/rules/LaserRules.kt:23`, `:94`, `entities/hazards/Laser.kt:172`, `entities/hazards/TubeBeamerV2.kt:115`.
  The laser keeps the nearest hit, so only exact ties can change.
- No shader, no camera rounding, no `toInt()` on body positions was found. The camera (`RotatableCamera.kt`,
  `CameraManagerForRooms.kt`, `CameraShaker.kt:43`..`:47`) uses floats and `PPM` factors only.
- Backgrounds use `PPM` factors and map rectangles (`Background.kt:66`, `BackgroundLayerBuilder.kt:239`). No pixel
  literals.

### 4.5 Engine (question 4)

`engine/` has no `ConstVals.PPM` and no literal 32 for world size. It takes `ppm` as a parameter:
`WorldSystem(ppm = ConstVals.PPM)` (`MegamanMaverickGame.kt:883`, used in `WorldSystem.kt:237`..`:240`, `:264`..`:267`),
`SimpleGridWorldContainer(ppm)`, `QuadtreeWorldContainer`, `LooseQuadtreeWorldContainer(ppm)`, and
`Trajectory(ppm = 1)` (`motion/Trajectory.kt:11`). Engine items to watch: `BitmapFontHandle.DEFAULT_FONT_SIZE`,
`PixelPerfectFitViewport`, `GameRectangle.splitIntoCells`, `SimpleGridWorldContainer` `% 1f`, and the `TiledMapLevelRenderer`
`unitScale`. Test only: `engine/src/test/.../PendulumTest.kt:12` hard-codes 32.

### 5. Physics equivalence (question 5)

- `Body.process` (`Body.kt:103`..`:117`): friction `v *= exp(-f * dt)`, gravity `v += g * dt`, clamp, `translate(v * dt)`.
  All are linear in length. `StandardCollisionHandler` and `MegaCollisionHandler` push by the overlap width or height.
  `WorldSystem` finds cells with `floor(x / ppm)` and `ceil(maxX / ppm)`, so the cell indices and the visit order are the
  same at both scales.
- In IEEE `float`, `round(a op b) / 2 == round(a/2 op b/2)` for `+`, `-`, and `round(a * b)` scales by the product of
  the scales, because a power-of-2 scale moves the exponent only (no underflow or overflow at game magnitudes). `sqrt`
  of a value scaled by 4 is the root scaled by 2. Normalization (`nor()`), `atan2`, ratios and `exp(-f * dt)` give the
  same bits. `c * 32 == 2 * (c * 16)` exactly for every float `c`. So, if every length (map data, constants, `PPM`) is
  halved, every position in tiles (`x / PPM`) is bit-identical after each step.
- It breaks only where a value is not scaled with the lengths:
  - non-length values multiplied by `PPM` (section 4.2);
  - unscaled length literals or epsilons (section 4.3, 4.4);
  - integer conversion of world values (`toInt`, `roundToInt`, `ceil(...).toInt()`), section 4.3;
  - order that depends on hash codes of floats (section 4.4);
  - map data that is not halved exactly (odd or non-integer values are still exact if written as decimals).
- Frame timing: `WorldSystem.process` uses a time accumulator with the real frame delta (`WorldSystem.kt:103`..`:132`).
  Two live runs never have the same steps. A proof needs a fixed delta (see `010` and `006`).

### How to prove "plays the same"

1. **Dual-`PPM` trace (recommended).** Make `PPM` selectable for a test build (it is `const val`, so either compile two
   builds, or change it to a non-`const` `val` first; `TestWavyTentacleController.kt:50`..`:65` has `const val`s that
   depend on `PPM` and must change). Run a headless level with fixed delta and scripted inputs (items `006`, `010`,
   `055`) at `PPM = 32` and at `PPM = 16`. After each world cycle, write each body's bounds divided by `PPM`, its velocity
   divided by `PPM`, and the ordered contact list. Assert bit equality. The first difference names the entity and the
   frame, which leads to the site that does not scale.
2. **Static check in the same branch:** a unit test that builds each entity (as `MegamanHeartTankTest` does) at both
   values and compares body and fixture sizes divided by `PPM`.
3. **Render check:** screenshots of the same frame at both values with `--pixelPerfect` at 640x480 and 1920x1080 must
   be pixel-identical (same projection ratio, same textures). Fonts are the expected exception until the font size is
   fixed.

### 6. Options and recommendation (question 6)

### Option A: change `PPM` to 16 and halve all assets and maps

- Halve tilesets and map geometry, halve the 2x atlas regions, change `PPM` to 16.
- Pros: one unit everywhere; map files and tools move to 16-pixel tiles.
- Cons: impossible for the 2,088 native 1x regions (and not needed for them). 290 used tiles and 20 atlas regions are
  not lossless. All map files, `level_builder.py` (`TILE = 32`), the level skills and Tiled extensions change. Large
  diff in `assets/`.
- Size: large (assets + 20 maps + tools + code fixes). Risk: high.

### Option B: keep world units (`PPM = 32`); make only the files 1x

- Halve the 4,348 lossless tiles (and optionally the 119 lossless 2x regions) and draw them with `unitScale = 2`
  (or `setSize` in tiles, which the code already does for regions). No physics change.
- Pros: no gameplay risk; assets become consistent 1x where lossless.
- Cons: does not give world unit = art pixel; world unit stays half an art pixel. 290 tiles stay 2x.
- Size: small to medium. Risk: low.

### Option C: `PPM = 16` with a map scale at load time (recommended if the owner wants world unit = art pixel)

- Keep the TMX files and tilesets as they are. At load, scale every map object (rectangles, polygons, polylines,
  ellipses, tile objects) by `PPM / tilewidth`, and give `TiledMapLevelRenderer` `unitScale = PPM / tilewidth`. Then fix
  the sites in 4.2 to 4.4 so that they do not depend on `PPM`. Then change `PPM` to 16.
- Pros: no art change at all (sprites become 1 world unit for each texel automatically; tiles 0.5). Maps, tools and
  skills keep 32-pixel tiles. Every step before the last one is the identity at `PPM = 32`, so each branch is easy to
  prove.
- Cons: the map pixel and the world unit differ (one factor in one place). Tile art stays 2x (option B can follow).
- Size: medium (one loader change, about 20 code sites, one test harness). Risk: medium, contained by the trace.

### Recommendation and branch order

First ask the owner what `PPM = 16` must give (for example world unit = art pixel for later pixel snapping, or only
smaller asset files). The data shows that the sprite art does not need to change. If the goal is world unit = art
pixel, use option C:

1. `067a` map scale: scale map objects at load and set the renderer `unitScale`; use `PPM` for the quadtree
   (`MegaLevelScreen.kt:452`..`:467`). Identity at `PPM = 32`.
2. `067b` non-length uses of `PPM`: friction (4 files), `RotatingLine` speeds (6 sites), `SineWave` frequencies
   (3 sites), `IceRulesTest`. Identity at `PPM = 32`.
3. `067c` pixel numbers and integer math: font sizes (`BitmapFontHandle.kt:34`, `MegaUtilMethods.kt:34`..`:38`),
   `GameRectangle.splitIntoCells`, `MockingByte` comparator, `GutsTank` epsilon, `Electrocutie`, `WindyGrass`,
   `Darkness`, and a decision on `PixelPerfectFitViewport` (keep the old scale steps or accept finer steps). Identity
   at `PPM = 32`.
4. `067d` dual-`PPM` trace harness (needs `006`/`010`; can reuse `055` if it exists by then). Run it until it shows no
   difference.
5. `067e` flip `PPM` to 16, run the trace, the render check and the owner's play test.
6. Optional `067f` (option B on top): halve the 4,348 lossless tiles and the 119 lossless regions, after `040`
   (atlas checks and repack) and with `028`/`033` in mind.

### 7. Output files

All in the scratchpad folder `067/`:

- `atlas_check.py`, `tmx_check.py`, `tps_check.py`, `frames_check.py`, `ppm_classify.py`: the scripts.
- `atlas_regions.csv`: all 3,081 regions with block counts and reasons.
- `atlas_regions_classified.csv`: the same with a `class` column (`1x_native_or_mixed`, `2x`, `2x_shifted_phase`,
  `2x_but_odd_size`).
- `atlas_regions_failing.csv`: regions that are not losslessly halvable at phase 0 (2,970; 2,116 live).
- `atlas_pages.csv`: pages with size and whole-page block counts.
- `tmx_summary.csv`, `tmx_tilesets.csv`, `tmx_objects_not_even.csv`, `tmx_object_properties.csv`: map facts.
- `ppm_uses.csv`: each `ConstVals.PPM` use with its class.
- `grep_literal_lengths.txt`, `ppm_nonlength_candidates.txt`: raw search output used for sections 4.2 to 4.4.
