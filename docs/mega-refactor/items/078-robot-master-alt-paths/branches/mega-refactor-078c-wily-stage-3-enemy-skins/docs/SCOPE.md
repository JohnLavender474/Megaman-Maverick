# Scope: mega-refactor-078c-wily-stage-3-enemy-skins

- Status: approved
- Approved: 2026-11-19

## Goal

The enemies of Wily Stage 3 get a stage sprite skin in the "last lab" palette of `078b`, drawn by Claude Fable 5.2,
chosen per placement with the map property `color` (the `SpriteSkin` mechanism of `078a`).

## Item

`078`, decision 2026-11-19 (owner, after the `078b` play test): the Wily Stage 3 enemies get a stage skin, drawn by
Claude Fable 5.2.

## Current state

- `SpriteSkin` (`entities/utils/SpriteSkin.kt`): `DEFAULT`, `WILY`. `SpriteSkin.of(spawnProps)` reads `color` and
  fails fast on an unknown value. `regionName(tag, key)` gives `<tag>/<skin>/<key>`.
- 10 classes take a skin, and each loads the regions of every `SpriteSkin` entry in `init()` (`SpriteSkin.entries`):
  `UziJoe`, `GreenPelletBlast`, `ShieldAttacker`, `WanaanLauncher`, `Wanaan`, `SpikeCopter`, `DuoBallCanon`,
  `DragonFly`, `BulbBlaster`, `LaserBeamer`. So a new entry needs frames for all of them, or `core:smokeTest` fails
  (a missing region fails it).
- Wily Stage 3 enemies (`WilyStage3_v4.tmx`): `Bat`, `IceFox`, `ShieldAttacker`, `UziJoe`, `AstroAssAssaulter`,
  `BunbyTank`, and the hazard `WanaanLauncher`. None sets `color` today. `ShieldAttacker`, `UziJoe` and
  `WanaanLauncher` have a `wily` skin (Wily Stage 1 colors).
- What they spawn: `IceFox` -> `IceBomb`; `UziJoe` -> `GreenPelletBlast` (skinned); `AstroAssAssaulter` ->
  `SuperCoolActionStarWarsSpaceLazer`, `StagedMoonLandingFlag`; `BunbyTank` -> `BunbyRedRocket`;
  `WanaanLauncher` -> `Wanaan` (skinned).
- Atlases: `Enemies1` (`Bat`, `IceFox`, `ShieldAttacker`, `UziJoe`), `Enemies2` (`AstroAssAssaulter`, `BunbyTank`;
  `png8`, a repack shifts old regions a little, as in `078a`), `Hazards1` (`WanaanLauncher`), and the atlases of the
  projectiles.

## In scope

1. Art (Claude Fable 5.2 art lead; mechanical steps may go to Sonnet): a `lab` skin for the enemies and the spawned
   entities that D1 and D3 name; same frame sizes and alpha masks as the default frames.
2. `SpriteSkin.LAB`, and a per-class set of the skins that a class has (D2), so that a class loads only the skins
   it has frames for, and `SpriteSkin.of` fails fast when a map asks for a skin the class lacks.
3. The 4 enemies with no skin today read `color` like the others; the spawners pass their skin to what they spawn.
4. `color=lab` on every placement of these enemies in `WilyStage3_v4.tmx`. No other map change.
5. Added 2026-11-19 (owner, scope change): `lab` skins for the hazards `InfernoOven` (the flame thrower, 13 in
   the stage) and `RisingLavaRiver` (4), and the `FloatingEmber` sparks that the lava spawns. Molten lava in the
   lab-furnace palette (the kill cue stays orange-ember). Same frame layout as the defaults; Inferno Man's stage keeps
   the default look. `color=lab` on these placements.
6. Added 2026-11-19 (owner, scope change): a `lab` skin for `GravitySwitcharoo` (21 in Wily Stage 3) and the
   `GravitySwitchAura` it spawns (it follows the rule of D3: spawned things get the skin). Moon Man's stage and the
   test maps keep the default. `color=lab` on the 21 placements.
7. Atlases repacked (`Basic`), old regions checked by script; credits in `README.md`.

## Out of scope

- Behavior of any enemy or hazard. Other maps. The hazards `FireballBar`, `Saw`, `Spike`.

## Affected areas and risks

- `SpriteSkin.kt`, up to 10 skinned classes (D2), 4 enemy classes, up to 4 projectile classes (D3),
  `WilyStage3_v4.tmx`, `Enemies1`, `Enemies2`, `Hazards1` and projectile atlases, `README.md`.
- A pooled entity keeps its skin from the last life unless `onSpawn` sets it (all skinned classes set it there).
- `Enemies2` is `png8`: a repack shifts the colors of some old regions slightly.

## Overlap with other branches

- `077c` (active): no shared files known. `060k` (active): can repack an atlas; check before the merge.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O4` deeper levels | helps | the stage identity reaches the enemies |
| `O9` player experience | helps | enemies read against the new tiles |
| `O2-R2` random calls | no effect | no random calls |
| A branch keeps all behavior that its contract does not name | no effect | art and skin selection only |

## Open questions

None. Owner answers (2026-11-19, all recommendations): D1 a new `lab` skin also for `ShieldAttacker`,
`UziJoe` and `WanaanLauncher`; D2 a per-class skin set (`SKINS`) and `SpriteSkin.of(spawnProps, skins)` that fails
fast with the class name; D3 the shots get the skin too (`IceBomb`, `GreenPelletBlast`, `Wanaan`, `BunbyRedRocket`,
`SuperCoolActionStarWarsSpaceLazer`, `StagedMoonLandingFlag`), and the art lead may keep a shot in default colors
where the color is a hazard cue (its `lab` frames then equal the default frames).

# Contract

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest detektMain core:verifyRandomRule lwjgl3:jar` pass.
- [ ] Atlas script checks: every old region keeps its name; every `lab` region has the size and alpha mask of its
  default frame.
- [ ] Map script check: only `color` properties are added in `WilyStage3_v4.tmx`.
- [ ] The review before the play test finds that every manual check compiles, launches and runs. No game launch.

## Manual playthrough

From `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-078c-wily-stage-3-enemy-skins`:

- `./run-game-alpha.sh --level WILY_STAGE_3 --room intro_room_2`: into `room1`: `Bat`, `IceFox`, `ShieldAttacker`,
  `UziJoe`, `WanaanLauncher` in the lab skin; their shots and `Wanaan` too.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: `AstroAssAssaulter`, `BunbyTank` and their shots in the
  lab skin.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: `InfernoOven` flames and the rising lava with its embers
  in the lab skin.
- `./run-game-alpha.sh --level INFERNO_MAN --room room5`: walk right into `room6`: ovens, rising lava and embers look as before (control).
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room2`: the gravity switches (and their aura) in the lab skin.
- `./run-game-alpha.sh --level MOON_MAN --room room6`: walk right into `room8`: the gravity switch and its aura look as
  before (control).
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room1`: the `wily` skins look as before (control).

## Intended changes

- Wily Stage 3 enemies show the `lab` skin. No behavior change.
- Owner, play test 2026-11-19: `GravitySwitcharoo` draws behind Megaman in every level: the arrow moves from
  `PLAYGROUND 5` to `PLAYGROUND -2`, the aura from `FOREGROUND 2` to `PLAYGROUND -1` (still over the arrow).
