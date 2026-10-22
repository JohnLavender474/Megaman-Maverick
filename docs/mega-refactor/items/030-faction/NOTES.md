# Notes: 030 Faction and root owner instead of `owner == megaman`

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 29: FACTION INSTEAD OF `owner == megaman`".

````text
1. Current state
- `IOwnable<T>` holds a single `owner` (`entities/contracts/IOwnable.kt`). Projectiles get `ConstKeys.OWNER`. Of 195 spawn sites, 111 pass `this` and 12 pass `megaman` (`MegamanWeaponsHandler.kt:769-891`).
- Explosions use two conventions. `IProjectileEntity.explodeAndDie()` passes `OWNER pairTo this`, that is, the projectile (`entities/contracts/IProjectileEntity.kt:74`). At least 19 projectiles pass the root `owner` instead (for example `SubmarineMissile.kt:76`, `LampeonBullet.kt:129`).
- `Megaman.canBeDamagedBy` walks two owner levels by hand (`Megaman.kt:647-650`).
- `AbstractEnemy.canBeDamagedBy` accepts only `owner == megaman || owner == null` (`AbstractEnemy.kt:255`). So an explosion made by `explodeAndDie` of a Mega shot (owner = the shot) does not hurt an `AbstractEnemy` today. A root-owner rule changes this. It is not clear if this is on purpose, so it must be checked (item 4).
- Enhancements are read with `owner == megaman && megaman.hasEnhancement(...)` (`AbstractEnemy.kt:243`, `AbstractBoss.kt:193`).
- Other patterns:
  - "player shot hits me" in `IceBomb.kt:82`, `Snowball.kt:78`, `Snowhead.kt:175`, `BunbyRedRocket.kt:86`
  - "player shots do not splash" in `Water.kt:151`, `PolygonWater.kt:100`
  - target class by owner in `JoeBall.kt:150`, `Snowball.kt:91`
  - the owner flips on reflect in `ReactorManProjectile.kt:215`
- X6 (`docs/engine.md`, "Object classes" and "Damage to an object"): faction is implicit in the object pool. Player shots are "weapon objects" (32 slots), and enemy shots are "visual objects". `ContactWeapon` tests enemies only against weapon objects.

2. Options
A. `enum Faction { PLAYER, ENEMY, NEUTRAL }` on `IFactionMember`. Copy it at spawn from `rootOwner()`, plus `ownerPlayerIndex` for enhancement and score credit.
B. Only add a `rootOwner()` helper and compare `rootOwner is IPlayer`. Pro: small change. Con: looks up owners that may have been destroyed or pooled.
C. A collision-layer mask (bits). Pro: fast. Con: less readable. It can be added later behind A.

3. Recommendation
Option A:
- Copy the faction and player index at spawn. Do not look up the owner at hit time. Owners are pooled and reused (`MegaEntityFactory`), so a live owner reference can point to a reused entity.
- `hostile(a, b)` is a small table. Damage checks become `hostile(damager.faction, target.faction)`.
- Changing `owner` (reflect) updates the faction.
- Enhancement scaling reads `players.byIndex(ownerPlayerIndex)?.profile`.
- Replace `is Megaman` with `is IPlayer` or with a capability.
- First, write a golden test of today's results for the explosion-owner cases (item 4). Then decide if the root-owner rule may change them.

4. Open questions for the owner
- Must Mega's own projectile explosions damage enemies? Today the default `explodeAndDie` path says no.
- In co-op, can player shots hurt or push other players?
- Do reflected shots switch faction, as `ReactorManProjectile` does today?

5. Backlog wording
"Faction and root owner instead of `owner == megaman`"
- `Faction` plus `ownerPlayerIndex`, copied at spawn from the root owner. Owner changes update both.
- Replace 70 `megaman` owner checks and 37 `is Megaman` with faction or capability checks.
- One explosion-owner convention. Golden test first, because today's two conventions give different damage results.
- Enhancements and score read from the owning player's profile.

6. Dependencies
Item 28, 4 and 22 (damage tests and tables), 13 (contact rules), 44 (pooling safety).
````
