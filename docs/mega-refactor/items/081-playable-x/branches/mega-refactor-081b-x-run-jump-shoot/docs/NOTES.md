# Notes: mega-refactor-081b-x-run-jump-shoot

## Decisions

- D1 (owner, 2026-11-06): the ladder top at tile 62 of `X6_INTRO` `room1`. X passes a one-way top while `DOWN` is
  held. Temporary, with a `TODO`, until X can climb.
- D2 (owner, 2026-11-06): charge feedback is a tint flash on X's sprite at half and at full charge, made in code,
  with a `TODO` for charge art.
- D3 (owner, 2026-11-06): X starts with the X6 values as they are, in `MegamanXValues`, converted to seconds and
  tiles x `PPM`. Tune after the play test.
- Claude's calls in the brief (no owner objection): the weapon reads in the screens use `is Megaman` workarounds
  with `TODO`s for `024` (owner allowed this 2026-11-05); the stand shot uses the `land_shoot` pose; the `jump` strip
  splits into frames 0-3 (rise) and 4-5 (fall).

- D4 (owner, 2026-11-06): `X6_INTRO` `room1` has a 2.5-tile step at tile 20, above X's 2.05-tile jump. Accepted for
  phase 1: X keeps the X6 values, the level is not finishable until the wall kick, and the `room1` play check covers only
  the start area. The test map covers the slope, the ladder-top drop and the buster.
- D5 (owner, 2026-11-06): the `X6_INTRO` boss room had no floor. Fix it in this branch: a floor block at the painted
  floor (y 512 px, object 287), and the `D1000` object moved down 96 px so that its bottom (the boss floor) is at 512.
  The map is made by `nesmap`; a new export must keep this block (see `docs/x6-intro/`).

- D6 (owner, 2026-11-06): a simplified stand for X, as Megaman's: 2 frames, eyes open for 1.5 s, then a 0.15 s blink.
  The 5-frame `stand` strip needs more work later. See "Custom art change: `stand_simplified`".

## Custom art change: `stand_simplified`

- `assets/sprites/frames/MegamanX/stand_simplified.png` (48 x 96): frame 1 (eyes open) and frame 3 (eyes closed) of
  `stand.png`. These 2 frames differ only in 3 eye pixels. `stand.png` did not change.
- It is not made by `utils/sprite_slicer/sheets/MegamanX.yaml`, so a slicer pack leaves it out of the atlas (the data
  file has a comment). The atlas was packed with `TexturePacker` from every strip in the frames folder, with the
  slicer's settings, in the format of the committed atlas (`bounds` lines, `filter: Nearest, Nearest`). The 36 old
  regions have the same pixels as before; `stand_simplified` is new.
- `MegamanXSprites.kt` plays `stand_simplified` for the `stand` key, with a `TODO` that the 5-frame stand needs work.

## Custom art change: `jump_v2`

- Owner (2026-11-06): a v2 of the jump and fall strip with the last frame removed.
- `assets/sprites/frames/MegamanX/jump_v2.png` (48 x 240): frames 0-4 of `jump.png`. `jump.png` did not change.
- `MegamanXSprites.kt` plays frames 0-3 of `jump_v2` as the rise and frame 4 as the fall.
- `master` reworked `jump.png` (frames 1-5, `def3f36ea`). At the merge of `master` (2026-11-06), `jump_v2` was cut
  again from the reworked strip, so it keeps following `jump.png`.
- Owner (2026-11-06): the same for the shoot strip. `jump_shoot_v2.png` is frames 0-4 of `jump_shoot.png`, which did
  not change; the shoot rise is frames 0-3 and the shoot fall is frame 4.
- Not made by the slicer data file (a comment in `MegamanX.yaml` says so). Packed as for `stand_simplified`; at each
  pack the older regions kept the same pixels.

## X6 sources

- `~/IdeaProjects/mmx-x6` at `73667bd`: `core/src/main/java/mmx/x6/game/player/PlayerConst.java` (speeds, gravity,
  body box), `Player.java` (walk start, jump start, jump release, charge times, shot limits, shot pose times),
  `BusterShot.java` (shot speeds). Facts only; no code copied.
- Unit conversion: `docs/x6-intro/intro-objects-plan.md` (1 px/frame = `1.875f * PPM` per s).

## Overlap with other branches

- `038a` merged before the code work; the branch took it in at `c9d99c6bc`.
- `038b` (planned) changes `playerSpawnEnd`, which X calls.

## Workarounds for `024` and `029` (copy into their docs at the merge)

Owner rule (2026-11-05): each temporary workaround has a `TODO` at the code and is documented in the target items.

| Site | Workaround | Removed by |
|---|---|---|
| `PlayerStatsHandler` weapon bar supplier | no weapon bar for a player that is not `Megaman` | `024` (loadout per character) |
| `MegaLevelScreen`, `ADD_WEAPON_ENERGY` | the pickup does nothing for a player that is not `Megaman` | `024` |
| `EndLevelEventHandler`, beam-out | a player that is not `Megaman` uses the buster beam art | `081` (X beam art) |
| `LevelPauseScreen`, ammo bars and weapon select | 0 ammo, and no weapon change, for a player that is not `Megaman` | `024` |
| `MegamanMaverickGame.resetPlayerProgress` | resets the weapon of each `Megaman` in the roster | `024` |
| `PlayerRoster.putInSlot0` | only slot 0 changes character; 2 players on an X level throw | `047`, `113` |
| `MegamanX` progress | X shares Megaman's heart tanks, health tanks and lives | `047`, `059` (`029` profile) |

Code smell: `O8-R2` goes from 0 to 5 `is Megaman` lines in the screens and the game.
