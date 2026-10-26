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

## Idea: an `owners` structure in place of one `owner` (2026-10-23)

From the `029` research discussion. Not designed yet; for discussion at the `030` scope step.

Owner's input (2026-10-23, verbatim):

> Maybe in `owners` (assuming the design turns out to be a data structure on the entity), each owner has data mapped to it: whether its an instigator, causer, or other; further detail on the state of things when it was added, and why it was added; what triggers its removal from the `owners` structure; etc.

### The problem

Today an entity has one `owner`. One value cannot say both "who is responsible" and "what hit you". Examples:

- A reflected shot: the first owner fired it, the reflector changed its direction. Who gets the kill, and which faction can it hurt now? (`ReactorManProjectile` switches faction today.)
- An explosion spawned by a projectile: today two conventions give different results (see the `030` proposed scope, "one explosion-owner convention").
- A shot from an entity that a player placed or summoned (a turret, a helper): the turret causes the hit, the player is responsible.
- Co-op: kill credit, enhancement scaling and drop odds must go to the right player slot.

Engines often keep two values: the instigator (the player or controller responsible) and the causer (the entity that hit). Example: Unreal `ApplyDamage(..., EventInstigator, DamageCauser)` (see `029` `NOTES.md`, review 5, section 1).

### The idea: an `owners` structure with an entry for each owner

Each entry maps an owner to data about the link. Candidate fields:

| Field | Meaning | Example values |
|---|---|---|
| owner | the owning entity, or better a stable handle (player slot, or entity id and generation), so that a dead or reused owner does not leave a stale reference | `PlayerSlot(1)`, `EntityHandle(id, gen)` |
| role | why this owner is in the structure | `INSTIGATOR`, `CAUSER`, `SPAWNER`, `REFLECTOR`, `OTHER` |
| reason | why the link was added | `FIRED`, `REFLECTED`, `SPAWNED_BY_EXPLOSION`, `SUMMONED` |
| snapshot | the state when the link was added | owner faction, owner direction and facing (today `ChargedShot.megamanDirOnShoot` and `megamanFacingOnShoot`), frame or tick number |
| removal rule | what removes the entry | owner destroyed, owner dead, a reflect replaces the instigator, end of room transition, a timer, never |

Possible queries:

- `instigator`: the entry used for kill credit, score, enhancement scaling and the player slot.
- `causer`: the entity that made the contact.
- `faction`: from the current instigator, or kept in the snapshot.
- `history`: all entries in the order they were added, for debug logs and replays.

### Questions for later

- A structure on each entity, or two fields (`instigator`, `causer`) plus an optional history? The structure is more flexible; two fields are cheaper and cover the known cases.
- Allocation: projectiles are pooled and spawned often. The structure must reset on spawn with no new allocation (`009` GC baseline).
- Removal rules: are they data in the entry, or does a system remove entries on events (owner destroyed, reflect)? `019b` (destroy notification) may give the "owner destroyed" signal.
- Does the faction change when the instigator changes (reflect), or does it stay from the snapshot?
- Determinism: the order of entries must be stable (online lockstep, `047` step 3).
- How does this meet `023` (damage profiles keyed by damage tags) and the golden damage test (`005`)?

## Who drives an entity (2026-10-26)

A proposal that touches this item (driver, thin slot view, faction; the owner's "boss becomes playable" case) is in
`031` `NOTES.md`, "Who drives an entity: driver, slot view and side". Not decided.
