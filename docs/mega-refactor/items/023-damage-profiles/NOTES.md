# Notes: 023 Damage profiles keyed by damage tags

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEMS 4 + 22: DAMAGE DESIGN".

````text
(1) Current state

Maverick:
- Engine contract. `IDamager` / `IDamageable` (engine/.../damage/, 18 lines in total). All of the logic is in `core`.
- The default tables are keyed by the damager's KClass:
  - `StandardDamageNegotiator.kt:24-139` has three tables, LARGE / MEDIUM / SMALL, each with 24 damager classes.
  - `AbstractBoss.kt:71-96` has `BOSS_DMG_NEG`, with 15 classes.
  - `Bospider.kt:92` has its own copy of a boss table.
- Damage is decided in up to three override layers, plus a modifier hook:
  - `AbstractHealthEntity.damageOverrides` is read first (`AbstractHealthEntity.kt:73-81`).
  - Then the entity's `damageNegotiator` is read. Most negotiators also have their own `overrides` map (`SelfSizeDamageNegotiator.kt:10`, `StandardDamageNegotiator.kt:20`).
  - Then `editDamageFrom` changes the result (6 overrides).
- The same lookup code is written three times: `StandardDamageNegotiator`, `SelfSizeDamageNegotiator` and `CustomMapDamageNegotiator`.
- The OO extensions are:
  - 49 `damageOverrides.put` calls in 27 files.
  - 9 anonymous negotiators (`SigmaRat.kt:121`, `GutsTankFist.kt:91`, `Bospider.kt:122`, `WilyFinalBoss.kt:159`, `BigFishNeo.kt:60`, `Cactus.kt:126`, `TorikoPlundge.kt:77`, `PreciousGem.kt:90`, ...).
  - 39 `canBeDamagedBy` overrides.
- Dynamic damage today is lambdas that cast the damager:
  - `ChargedShot.fullyCharged` (`StandardDamageNegotiator.kt:30`). The half charge is a `ChargedShot` with `fullyCharged=false`. The lemon is the `Bullet` class, and enemies use the same `Bullet` class.
  - `ChargedShotExplosion.fullyCharged`.
  - `ReactorManProjectile.big`.
  - `SlashWave.getDissipation()`, an integer 1..10 (`SlashWave.kt:142`).
  - Laser hard mode (`MegamanDamageNegotiator.kt:30`).
- Modifiers:
  - `MegaEnhancement.DAMAGE_INCREASE` multiplies damage by 2 for enemies and by 1.5 for bosses, rounded up (`AbstractEnemy.kt:242`, `AbstractBoss.kt:192`, `MegaEnhancement.kt:38-42`).
  - Hard mode adds +1 to the damage that Megaman takes (`Megaman.kt:709-720`).
  - Element rules: `IFireEntity` does max damage and `IFreezerEntity` does 1 (`Coldier.kt:155`, `IceFox.kt:129`, `IceSkaterPeng.kt:135`).
- Player side: `MegamanDamageNegotiator.kt` uses keys of a different kind (entity TAG strings) and the attacker's size. It then falls back to `IHazard.getDamageToMegaman()` and the entity type.
- Finding (please check): `StandardDamageNegotiator.get(damager)` (`StandardDamageNegotiator.kt:151-164`) picks the table by the damager's size. All projectiles are `ISizable`, with default MEDIUM (`AbstractProjectile.kt:13`). The static `get(size, ...)` picks the table by the target's size; the comment at line 141 says so.
  - `DarkSerketClaw` is SMALL (`DarkSerketClaw.kt:58-61`). It uses the instance form, so it gets the MEDIUM values for most shots, and 0 from `Explosion`, which is not `ISizable`.
  - This is probably not intended. The golden test will lock in the current behaviour, so decide before the refactor.

X6 (engine.md, "Damage to an object", "Weapon ids", "The damage tables"):
- Each object points to one 80-entry table (`+0x58`). Each entry is {class, damage}. The classes are: normal damage, no effect (the shot passes through), deflect, and class 7/8.
- The variant is in the weapon id. The lemon is 0, the half charge is 9, the full charge is 18, Falcon is 19, Ultimate is 20/21, and the uncharged / charged special weapons are slot n and n+9.
- The modifiers come after the lookup:
  - +1 for some armour parts, by id;
  - Easy ×1.25;
  - a cap of 127.
- State-dependent damage swaps the table pointer. Examples: `dmg_tbl_sheldon` and `dmg_tbl_sheldon_guard`; `dmg_tbl_reflector_wait` for 33 updates.
- About 15 named tables are shared by many objects (`dmg_tbl_normal`, `minor`, `one_hit`, `deflect`, ...).
- Player damage is not a table. It is an attribute on the attacker (`+0x60`), followed by `PlayerDamageScale` ("Damage to the player").

(2) Pros and cons

| Aspect | Lookup table (X6 style) | OO overrides (Maverick today) |
|---|---|---|
| See all values at once | Yes. One grid, easy to balance and diff | No. Values are in 27+ files and 3 override layers |
| Duplication | Low. Shared named profiles | High. 3 size tables repeat the same rows; lookup code written 3 times |
| Coupling | Target knows weapon ids only | Targets import damager classes (`Axe::class` in bosses); damage package imports ~25 entity classes |
| Dynamic values | Needs the variant in the key, or a modifier step | Free (any lambda), but hidden and untestable as data |
| State-dependent (guard open/closed) | Swap the profile | Any code; sometimes duplicated |
| Reactions (stun, freeze, burn) | Not covered; still code | Code in `takeDamageFrom` / `onDamaged` |
| Compile-time safety | Weak if in JSON; good if Kotlin DSL | Good (KClass keys) |
| Shared classes with different roles | Separate ids (lemon id 0 is not an enemy bullet) | One `Bullet` class for player and enemy shots; owner checks are added on top |
| Testability | Snapshot the grid | Must build entities and fake damagers |
| Flexibility for one-off rules | Low; needs an escape hatch | High |

(3) Options

A. Clean up the OO approach, with no new concept.
- Merge `damageOverrides` and the negotiator `overrides` into one `DamageProfile` per entity: `base` plus `overrides`.
- Delete `SelfSize`, `CustomMap` and the instance form of `StandardDamageNegotiator`.
- Fix the size-key ambiguity.
- Keep the KClass keys.
- Pro: small and low-risk.
- Con: the class coupling stays, the shared `Bullet` stays ambiguous, and the dynamic lambdas stay hidden.

B. Damage tags + a profile table, with one escape hatch (recommended).
- The damager emits a `DamageSource`, built when the shot spawns:
  - `tag`: a stable string such as `buster.lemon`, `buster.half`, `buster.full`, `buster.half.explosion`, `buster.full.explosion`, `reactor_shot.small`, `reactor_shot.big`, `slash_wave`, `explosion`, `enemy.bullet`;
  - `elements`: a set such as `fire` or `ice`, which replaces the `IFireEntity` / `IFreezerEntity` checks for damage;
  - `magnitude`: an optional integer, for example the SlashWave dissipation;
  - `owner` / `faction`, which links to item 29.
  - The charge level picks the tag. `ChargedShot` sets `buster.half` or `buster.full` from `fullyCharged`, so the table never casts.
- Profiles are named tables of tag → outcome:
  - `small`, `medium`, `large`, `boss`, and named shared profiles (for example `minor`, `one_hit`, `fire_immune`), as X6 has `dmg_tbl_minor` and `one_hit`.
  - An outcome is `Damage(n)`, `Scaled(k)` (n = k × magnitude, for SlashWave: large 0.5 rounded up, medium 1, small 3), `Max`, `Immune` (pass through), or `Deflect`.
  - A profile can inherit: `profile("reactor_man", from = "boss") { "slash_wave" to 3; "axe" to 3 }`.
- Each entity declares its profile name and its deltas in its schema (item 21), not in `init()`.
  - A state change swaps the profile, as X6 does. For example `profile = if (guard) "shield_guard" else "boss"`.
- After the lookup, a fixed modifier pipeline runs:
  - attacker enhancements (`DAMAGE_INCREASE` ×2 / ×1.5 rounded up, by target class);
  - difficulty (hard +1 for player damage);
  - the per-target element rules (`fire` → Max for Coldier);
  - a cap.
  - Each modifier is a named, ordered function.
- The escape hatch: an optional `DamageRule` function on the entity, for the few truly dynamic cases. Examples: `WilyFinalBoss.kt:162` (Bullet = 1, else 1 becomes 2), and `SigmaRat.kt:122` (full charge only). Many of these turn into plain profiles. The escape hatch is the only code path, and it must be listed in the snapshot.
- Reactions (stun on Needle, freeze, burn) stay in code. They key on tags or elements, not on classes.
- Pro: one grid, no class coupling, dynamic cases are explicit and can be enumerated.
- Con: a migration over ~25 damager classes and ~80 entities, and a new tag vocabulary to keep stable.

C. Option B, with the tables in JSON or YAML loaded at runtime.
- Pro: modders can edit it, hot reload works, and it fits the "Define entities in data files" backlog item.
- Con: the compiler does not check it, it needs the validator, typos fail at runtime, and it is one more loader.

Where the table lives:
- Start with a Kotlin DSL in one file, `damage/DamageProfiles.kt`.
- Add a generator that exports the grid to CSV / Markdown for review, like the tables in the X6 doc.
- Move to JSON only if the data-files item is approved. The DSL can emit the JSON later with no format change.

How dynamic damage fits option B:
- Lemon / half / full: these are separate tags, as X6 ids 0/9/18 are.
- Explosion variants: separate tags (`buster.full.explosion`), or `explosion` with a `magnitude`.
- SlashWave: `Scaled(k)` with `magnitude` = dissipation.
- ReactorManProjectile big / small: two tags.
- `DAMAGE_INCREASE`: an attacker-side modifier, and it reads the owner's enhancements.
- Laser hard mode: a difficulty modifier, or two player-side values.

Player side:
- Keep the X6 model. Damage to the player is an attribute of the attacker (`contactDamage` in the schema). The default comes from size (2 / 3 / 4) or type (3), as `MegamanDamageNegotiator.kt:41-49` does now.
- Then a player-side modifier pipeline runs (enhancement, hard mode).
- The TAG-keyed `custom` map becomes per-entity data.

Golden test (item 4):
- Do it in two stages.
- Stage 1, before any change:
  - A test enumerates every damageable class × every damager class × every variant: `fullyCharged` true/false, `big` true/false, dissipation 1..10, enhancement on/off, and each difficulty.
  - It calls the current `takeDamageFrom` result path: `damageOverrides`, then the negotiator, then `editDamageFrom`.
  - It writes a sorted CSV snapshot: `target,damager,variant,modifiers,damage`.
- The overrides are set in `init()` (for example `ReactorMan.kt:235`), so the test must init entities. That needs the item-3 harness. A test that calls the negotiators directly with stub damagers misses the `init()` overrides.
- Stage 2: after the move to tags, the same enumeration maps class+variant to the tag and must produce a byte-equal snapshot.
- Allowed changes (for example the `DarkSerketClaw` size key) are separate commits that edit the snapshot. Each such commit is reviewed on its own.

(4) Recommendation

- Use option B with the Kotlin DSL.
- First do the cheap part of option A: merge the override layers into one profile per entity, and make the size-key decision explicit.
- Then add the tags.

(5) Open questions for the owner

- Is the size-of-damager table lookup in `StandardDamageNegotiator.get(damager)` intended for `DarkSerketClaw` and `BigAssMaverickRobot`?
- Do you want X6-style "deflect" and "pass through" as outcomes in the table? Today 0 means "cannot be damaged", and the shield deflection is done elsewhere.
- Should enemy shots that hit enemies ever do damage (friendly fire)? Today `AbstractEnemy.kt:254-255` blocks it by owner.
- Will modders edit damage? If yes, plan for the JSON export.

(6) Backlog wording

Item 4 title: "Golden damage snapshot test"
- Enumerate target class × damager class × variant (charge, size flag, dissipation 1..10) × modifiers (enhancement, difficulty).
- Write a sorted CSV snapshot through the real `takeDamageFrom` path, after `init()`.
- Every intended damage change is a separate commit that updates the snapshot.

Item 22 title: "Damage profiles keyed by damage tags"
- Damagers emit `DamageSource(tag, elements, magnitude, owner)`. Variants are separate tags (`buster.lemon`, `buster.half`, `buster.full`, ...).
- Named, inheritable profiles (`small`, `medium`, `large`, `boss`, shared specials) in one Kotlin DSL file. A per-entity profile and deltas go in the entity schema. A state change swaps the profile.
- An ordered modifier pipeline: enhancement, difficulty, element rules, cap.
- One optional `DamageRule` escape hatch per entity, listed in the snapshot. Reactions (stun, freeze) key on tags, not classes.
- Delete `SelfSizeDamageNegotiator`, `CustomMapDamageNegotiator` and `damageOverrides`. Export the grid to CSV / Markdown.

(7) Dependencies

- Item 4 needs item 3 (entity init harness).
- Item 22 needs item 4. It links to 20 (stable IDs for the tag vocabulary), 21 (per-entity profile in the schema), 23 (`WeaponDef` emits tags), 29 (faction instead of the owner checks) and 1 (CI runs the snapshot).
````
