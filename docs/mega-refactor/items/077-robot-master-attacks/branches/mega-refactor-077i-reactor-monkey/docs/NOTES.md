# Notes: mega-refactor-077i-reactor-monkey

Fight design: `077h` `docs/IDEAS.md`. Frames: `docs/ASSETS.md`. Art by Claude Fable 5.2 (an AI model by Anthropic),
directed by the developer, on the original Reactor Monkey frames by Balloon Cart (Reactor Man asset pack).

## Decisions

- 2026-11-26 (owner): death plays the full sequence (`dying`, `collapse`, `dead`), no alpha fade.
- 2026-11-26 (owner): Hot Potato is in: a charged shot can knock his ball back; the monkey responds.
- 2026-11-26 (owner): the number of chest-beat loop passes is decided after the play test; ONE constant.
- 2026-11-26 (owner, from the phone): go ahead with the branch and the first pass, then push a draft PR.
- Without the owner: see `SCOPE.md` "Decisions made without the owner".

## Art sources

- `ReactorMonkey/*` except `chest_beat*`: `077h` `docs/drafts/round-4/` (round 4: the green mohawk; built on rounds
  1 to 3 and on the original `stand` and `throw` frames).
- `ReactorMonkey/chest_beat`, `chest_beat_damaged`: `077h` `docs/drafts/round-5/` (the heave).
- `ReactorMonkeyBall/mad_0..3`: `077h` `docs/drafts/round-4/ball_mad/` (a recolor of `Projectiles1`
  `ReactorMonkeyBall/0..3`).
- README `CREDITS`: the existing "Reactor Monkey - Balloon Cart" line became an "Original by" line (Balloon Cart,
  Reactor Man asset pack, the same link) and an "Updated by Claude Fable 5.2" line. The round notes say the original
  artist was "not known"; the README already named Balloon Cart, so the credit follows the README.

## Packing

- No TexturePacker in the cloud. `temp/077i-reactor-monkey/pack.py` packs the frames folder into columns (first fit by
  decreasing height; 2 px border, 2 px between regions, no rotation, no trim; transparent pixels cleared, as
  `ClearTransparentPixels`) and writes the libGDX text format of the other atlases (`rotate/xy/size/orig/offset/index`).
  `verify.py` checks each region against its frame pixel for pixel. The layout is not TexturePacker's Basic layout; a
  repack on a laptop with `ReactorMonkey.tps` changes only the `xy` values.

## Hints for the code agent (inferred from the code, not tested)

- Hot Potato contact: `ChargedShot` vs the ball's `PROJECTILE` fixture runs `ProjectileRule` → `hitProjectile` on both
  (`world/contacts/rules/ProjectileRules.kt`); the ball's `SHIELD` fixture makes `ChargedShot.hitShield` bounce the
  shot. Decide one path so the hit is handled once. `ChargedShot.fullyCharged` gives the charge level.
- A returned ball must not hit the player and must hit the monkey: today `PlayerDamageNegotiator.get` gives 0 when the
  damager's owner is the player; the monkey's `canBeDamagedBy` and `damageOverrides` decide his side.
- The phase-2 pose after the throw in progress: `GlacierMan.phaseStartPending` (`077g`).
- Death timing pattern: `GlacierMan` `DEFEAT_DUR` and its `onDefeated` (`077g`).
- `Animation.of(entries, loop)` or `Animation(frames, durations, loop)` builds the chest beat with repeated cells.

## Open

- None for the owner from this step. (The original artist is named in the README; no missing credit.)

## Overlap with other branches

- `077h` (the art branch, not merged): this branch is cut from it. It touches only its own docs folder; no overlap in
  code. Merge `077h` before this branch.
- Other `077` branches (`077a` to `077g`) are merged. The shared files this branch touches: `PlayerDamageNegotiator`
  (one entry per tag, additive), `TextureAsset` (one entry), `README.md` (one credit entry).
