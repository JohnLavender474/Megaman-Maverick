# Notes: 081 Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29: first scan (`master` at `685cdfe78`, `mmx-x6` at `393e376`)

### What already exists in the items

- `069`: the dash button, the `LevelType.X_LEVEL` select-button override, the NES-pad layout. Input only.
  (2026-11-01: `069` removed the override, `X_LEVEL` and the NES-pad layout.)
- `060`: Proto Man and `CharacterDef`. The nearest model for a second character.
- `029` `NOTES.md`, "What a second character needs first": values; body def; sprites, animations and sounds;
  loadout (`024`); an ability-set contract (`BehaviorType` keys, `setBehaviorAllowed`, `aButtonTask`); damage
  reaction; Mega-named APIs and `is Megaman` checks; factories that read `Megaman` fields (`048`). `061` removed
  `MegaPlayer` after that list was written, so item 7 of it must be checked again.
- `024`: `WeaponDef` and a loadout per character.
- `051`: X6-style camera bounds for the extra levels.
- `068`: slopes. They work for any body with a `FEET` fixture, so they do not need X.

### `mmx-x6` player (facts only; no code is copied)

- `game/player/`: `Player.java` (1,621 lines), `PlayerConst.java` (32), `BusterShot.java` (119),
  `BusterPoints.java` (73), `ChargeFlash.java` (56), `SabreSlash.java` (96), `PlayerStep.java` (6).
- States in `Player.java` (one `update...` method each): beam-in, stand, walk, dash, dash end, crouch, land, jump,
  fall, wall grab, wall slide, wall kick, falcon dash, ladder, climb, rope, bar, sabre, hurt, death. Charge is a
  separate update.
- `PlayerConst.java` values: `WALK_START`, `WALK`, `JUMP`, `FALL_MAX`, `FALL_GRAVITY`, `JUMP_GRAVITY`, `DASH_START`,
  `DASH_MIN`, `DASH_DECEL`, `DASH_TIME`, `WALL_SLIDE`, `WALL_KICK_UP`, `LADDER_FIRST`, `LADDER`, `BAR_MOVE`,
  `ROPE_MOVE`, `HANG_DASH`, `FALCON_DASH_TIME`, `MAX_HP`, the box offsets and half sizes, `FEET`.

## From `084h`: X has 2 charges (2026-11-01)

X has a buster charge and a special-weapon charge (`mmx-demake-tools/docs/players/attacks.md:31-32,100`), and an entity holds 1 component per class. So `WeaponChargeComponent` as it is cannot hold both. `081` decides the form: 2 timers in the part, or another form.

## `1LMX` sprite sheet (2026-11-01, `master` at `ed04d966c`)

The artist's sheet `1LMX.png` is cut into 30 strips with 110 frames in 48x48 cells. The feet are on `y=47`, as in
`Megaman_v3`. The work is on `master`: `utils/sprite_slicer/` (the tool and `sheets/MegamanX.yaml`),
`assets/sprites/frames/MegamanX/`, and `MegamanX.png` and `MegamanX.txt` in `assets/sprites/sprite_sheets/`. There is
no `TextureAsset` entry yet, because no code reads the atlas.

For the code that adds X:

- **`wallslide_shoot` is mirrored:** X's arm extends left, and Megaman's extends right (`x=7-36` against `x=12-38`).
  The code must flip this strip for X, or X shoots into the wall. `walljump_shoot` has its arm on the right.
- **The burst frames sit high:** frame 2 of `damaged` and of `air_damaged` is a burst. On the sheet, the burst is
  centered on the body and extends 5 pixels below the feet. In a cell with the feet on `y=47` there is no room below
  the feet, so the burst is about 6 pixels higher than on the sheet. See open question 5.
- **`spawn_out` rises 5 pixels at frame 1:** a glow goes 5 pixels below the feet in frames 1 to 4, and a cell has no
  room below `y=47`. Frame 0 stays on `y=47` to join `victory`, so the feet are on `y=42` in frames 1 to 4. `offsets`
  in `MegamanX.yaml` set the sideways position of frame 0 and put the beam in frames 5 and 6 on its sheet lines. The
  owner chose the rise at frame 1 (2026-11-01).
- **Names follow `Megaman_v3` where a name exists:** `run_trans`, `climb_finish`, `wallslide`. `run` has 8 frames
  (Megaman has 4), so X needs its own `AnimationDef` values. `r13_unknown` is a pose with no name yet.
- **No art for the buster charge:** the sheet has no `charge_half` or `charge_full` body versions, and this item's
  scope has charge levels. A recolor by script is possible, because the sheet has 12 colors.
- **No art for** `swim`, `cartin`, `pipe_portal_*`. X needs these only if the extra levels use these features.
