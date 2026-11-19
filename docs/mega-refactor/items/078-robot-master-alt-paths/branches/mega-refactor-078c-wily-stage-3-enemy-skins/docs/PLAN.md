# Plan: mega-refactor-078c-wily-stage-3-enemy-skins

Drafts: `temp/078c-wily3-skins/` in the main checkout. Palette: `temp/078-wily3-art/STYLE.md`.

1. **Art** (Claude Fable 5.2 art lead; Sonnet for mechanical steps): `lab` frames for `Bat`, `IceFox`,
   `ShieldAttacker`, `UziJoe`, `AstroAssAssaulter`, `BunbyTank`, `WanaanLauncher` and the shots `IceBomb`,
   `GreenPelletBlast`, `Wanaan`, `BunbyRedRocket`, `SuperCoolActionStarWarsSpaceLazer`, `StagedMoonLandingFlag`; same
   sizes and alpha masks as the default frames; previews on the new Wily Stage 3 tiles. Check: mask and size script.
2. **Code**: `SpriteSkin.LAB`; `SpriteSkin.of(spawnProps, skins)`; a `SKINS` set in each of the 10 skinned classes;
   skins in the 4 new enemy classes and the 4 new shot classes; spawners pass their skin. Check: `core:test`.
3. **Assets and map**: frames in `assets/sprites/frames/<atlas>/<Tag>/lab/`, atlases repacked (`Basic`), old
   regions checked by script; `color=lab` on every placement of the 7 enemies in `WilyStage3_v4.tmx`. Check:
   `core:smokeTest`, map script (only `color` added).
4. **Credits** in `README.md`.
5. **Checks**: the automated checks of the contract, then the review before the play test.
6. **Hazard skins (scope change 2026-11-19).** Art (Fable lead): `lab` frames for `InfernoOven` (cold, warming_up,
   hot, flame), `RisingLavaRiver` (its regions) and `FloatingEmber`, same layout as the defaults. Code: `SKINS` =
   `DEFAULT, LAB` in the 3 classes; the lava passes its skin to its embers. Assets: frames, `Hazards1` and
   `Hazards2` (and the ember's atlas) repacked with the old-region check; `color=lab` on the 13 ovens and 4 lavas of
   `WilyStage3_v4.tmx`; credits. Check: `core:test`, `core:smokeTest`, the atlas and map scripts, then the review
   before the play test again.
7. **Gravity switch skin (scope change 2026-11-19).** Art (Fable lead): `lab` frames for `GravitySwitcharoo` and
   `GravitySwitchAura`, same layout. Code: `SKINS` = `DEFAULT, LAB`; the switch passes its skin to the aura;
   `color=lab` on the 21 placements. Assets: `Specials1` (and the aura's atlas) repacked with the old-region check;
   credits. Check: all Gradle checks, then the review before the play test.
