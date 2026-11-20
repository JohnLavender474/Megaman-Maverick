# Plan: mega-refactor-078d-reactor-man-art

How to do the work in `SCOPE.md`. The art source is `temp/078-reactor-art/round3/final/` in the main checkout
(`STYLE.md` "Round 3 (final)" names the target of each file). When the plan changes, write the reason in the
process log.

## Steps

### 1. Water and decor frames into the atlases

- Changes: copy the round-3 water frames (`Specials1/ToxicWater/*`, `Decorations1/ToxicWaterfall`,
  `Environs1/Water/ToxicSplash`) and the decor frames (`Decorations1/DecorLaserTube/cap`, `.../mid`,
  `Decorations1/SteamVent`) into `assets/sprites/frames/`; repack `Specials1`, `Decorations1`, `Environs1` with the
  `Basic` algorithm (`docs/ways-of-working/assets.md`).
- Check: an atlas script in `temp/` shows that each old region keeps its name, and its pixels except the 4 replaced
  water regions; the new regions exist with the sizes of `STYLE.md`.
- Done: yes

### 2. Backgrounds11

- Changes: new frames folder `assets/sprites/frames/Backgrounds11/` with the 5 regions and a `.tps` (`Basic`);
  packed `Backgrounds11.png`/`.txt`; `TextureAsset.BACKGROUNDS_11`.
- Check: the 5 regions exist with the frame sizes of `final/backgrounds.tsv`.
- Done: yes

### 3. Merged tileset and map repaint

- Changes: a remap script in `temp/078-reactor-art/` writes the merged tileset (`.tsx` with the Tiled animations of
  `final/tiles_index.tsv`, `.png`) into `assets/tiled_maps/`, repaints `tiles1` and `tiles2` by the gid mapping
  and the clear-black rule, adds the `decor` tile layer, and removes the 6 old tileset links from the map.
- Check: every non-tile object is the same as on `master`; every painted cell maps to a tile of the new tileset;
  a render of each room matches the round-3 render.
- Done: yes

### 4. Background objects and decor placement in the map

- Changes: `CityBKG` per `city/NOTES.md` (region `ReactorCityBKG`, atlas `Backgrounds11.txt`); 4 new background
  objects per `final/backgrounds.tsv`; show/hide triggers where 2 areas meet (as `078a`/`078b`); `DecorLaserTube`
  and `SteamVent` objects in the `decorations` layer by the `STYLE.md` list.
- Check: the map script check of step 3 (only the named objects are new or changed); each trigger names an object
  that exists.
- Done: yes

### 5. DecorLaserTube and SteamVent

- Changes: 2 classes in `entities/decorations/` (no body; sprites and animations; `SteamVent` wait from
  `services.random`); `core/src/smokeTest/resources/entity-tags.txt`. Read `docs/CODE_STYLE.md` first.
- Check: `./gradlew core:smokeTest detektMain core:verifyRandomRule` pass.
- Done: yes

### 6. Credits

- Changes: `README.md` credit lines per `SCOPE.md` "In scope" 5. Ask the owner for the source of the old water art.
- Check: the lines follow the pattern of `docs/ways-of-working/art.md`.
- Done: yes (the old water art is the developer's own; no credit, owner)

### 7. Automated checks and the review before the play test

- Changes: none, unless a finding needs a fix.
- Check: all automated checks of the contract pass; one Sonnet review agent per `docs/mega-refactor/CLAUDE.md`.
  No game launch.
- Done: yes
