# Notes: 081 Playable X for the extra levels, ported from `mmx-x6`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29: first scan (`master` at `685cdfe78`, `mmx-x6` at `393e376`)

### What already exists in the items

- `069`: the dash button, the `LevelType.X_LEVEL` select-button override, the NES-pad layout. Input only.
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
