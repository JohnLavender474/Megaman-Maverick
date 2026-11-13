# Notes: mega-refactor-139a-shared-enemy-shot

## Decisions

Owner (2026-11-13): `D1` A, `D2` A, `D3` A (see `SCOPE.md`, "Owner answers").

Claude, during the work (2026-11-13):

- **The Rolling Bot kind has no `BODY` fixture.** `EnemyShot` is an `IDamageableEntity` (the junk kinds need it), and
  a kind cannot change that per spawn. With an active `BODY` fixture on that kind, these readers of the `BODY`
  fixture would change their result:
  - a player `Bullet` or `ChargedShot` would explode on it (`hitBody`: a damageable that refuses the damage);
  - an enemy `Bullet` (for example from `TankBot` and `ToxicBarrelBot`, in the same Reactor Man rooms) would explode
    on it too;
  - `DeathRule` would call `depleteHealth()` (a no-op for a shot) in place of `explodeAndDie()`.

  So the `BODY` fixture was dropped, as `SCOPE.md` allows. What the old `BODY` fixture did, and is now gone: death
  fixtures, bouncers, force and gravity-change areas, lasers, `HIT_BY_BODY` side receivers, `Block.hitByBody` and
  other projectiles' `hitBody` on the shot. A check of the live `ReactorMan_v3.tmx` (the only live map with a
  `RollingBot`; rooms `room1`, `room4`, `room10`): no force, bouncer, gravity-change or laser objects; no entity with
  a `HIT_BY_BODY` receiver; no `Block` subclass that overrides `hitByBody`; of the enemy projectiles there, `UFOBomb`
  and `Bullet` have a `hitBody`: `UFOBomb` reacts only to a player, and `Bullet.hitBody` acts only on an
  `IDamageableEntity`, which the old shot was not. The `Death` sensors in these rooms are pits below the
  floor, out of the flat path of a shot fired from the ground. The review before the play test found one more reader: the wildcard `ConsumerRule` of `ToxicWaterfall`, which
  splashes for any `BODY` in its water. So one cosmetic change is left: no toxic splash when a Rolling Bot shot
  crosses the waterfall in `room4` (named in `CONTRACT.md`). Keeping a `BODY` for it would let `DrippingToxicGoop`
  consume the shot and switch `DeathRule` to `depleteHealth()`, which are larger changes. `EnemyShotTest` pins
  "a player `Bullet` passes through the Rolling Bot kind".
- **Kind field `damageable`** (the `DAMAGEABLE` fixture is active) is separate from `shootable`. `JUNKROID_HARD` keeps
  an active `DAMAGEABLE` fixture that refuses all damage, as `JunkShot` did; the Rolling Bot and pellet kinds have
  none, as before. `DamageRule` calls the damager's `HIT_BY_DAMAGEABLE` receiver also when the damage is refused, so
  an active fixture is not the same as none.
- **Names:** `Type` became `Kind` and `type` became `kind`, to match `D2` (`kind.damage`). Body sizes are in tiles
  (`width`, `height`), scaled by `ConstVals.PPM` at use; `D1000TrapGuard` scales them.
- **Art:** `Art.Ticks` (a `TickFrameDef`, placed with `TickFrames.place`) for the junk kinds; `Art.Grid` (rows,
  columns, frame time, sprite size in tiles) for the moved kinds. The pellet is a 1x1 grid, the same frame as its old
  plain sprite. Each art names its atlas and region, so no region moved. `init()` loads all kinds and throws on a
  missing region.
- **Sprite turn:** `FLIP_MOVING_RIGHT` (junk), `FLIP_MOVING_LEFT` (Rolling Bot; the old flip read `facing`, which was
  set from the same sign as the velocity), `ROTATE` (pellet). The pre-process sets size, flip, origin and rotation
  every frame, so a pooled shot keeps nothing from its last kind.
- **Size:** `EnemyShot` stays `Size.SMALL`. No negotiator reads it for this class: the player negotiator uses the tag
  entry, and no `KClass` table in `StandardDamageNegotiator` names `EnemyShot`.
- **`RollingBot` spawn:** the old fixed speed (8 tiles per second) is now `RollingBot.SHOT_SPEED`, and the trajectory
  is made from `facing`. `DemonMet` passes its old `IMPULSE` vector as `TRAJECTORY` and still no owner.

## Overlap with other branches

Checked 2026-11-13 against `master` at `add9d66d2`.

| Branch | State | Overlap | Risk |
|---|---|---|---|
| `060c` (`mega-refactor-060c-protoman-buster`) | 10 commits, 70 files, plus uncommitted work | Changes imports and damage entries in `Junkroid.kt`, `HoverGunner.kt`, `D1000Core.kt` (`MegamanXSabreSlash` to `PlayerSabreSlash`, new `ProtoBuster` rows); adds lines to `entity-tags.txt` and `StandardDamageNegotiator.kt`. Does not touch `JunkShot.kt` or `PlayerDamageNegotiator.kt`. | Text only: import lines next to the `JunkShot` import, and `entity-tags.txt` lines. The branch that merges second resolves them. |
| `137a` (`mega-refactor-137a-falling-piece-entity`) | branch start, no commits | Will change the `DebrisPiece` spawns in `Junkroid.kt` and `HoverGunner.kt`. Same design question ("spawn data or typed def"). | Text only. Both branches should choose the same shape for the def (typed kind passed as `ConstKeys.TYPE`). |
| `138a` (`mega-refactor-138a-wreck-into-mock-sprite`) | branch start, no commits | `Wreck` and `MockSpriteEntity` only; maps of the X6 intro. | None for code. |
| `compactor-level` | not a refactor branch | Not checked in detail. | Unknown; it is a level branch. |

