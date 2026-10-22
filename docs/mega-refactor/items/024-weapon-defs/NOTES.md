# Notes: 024 One `WeaponDef` per weapon; loadout per character

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 23: ONE `WeaponDef` PER WEAPON".

````text
1. Current state
- `entities/megaman/constants/MegamanWeapon.kt` is a plain enum with 10 entries. Data for each weapon is spread over at least 8 places:
  - cooldown, costs and chargeable flag: `MegamanWeaponsHandler.kt:306-515`, a `when` with anonymous subclasses for MOON_SCYTHES, PRECIOUS_GUARD, NEEDLE_SPIN, RODENT_CLAWS
  - fire dispatch: `:661-670`
  - weapon-specific deselect: `:518-542`
  - animation atlas: `MegamanAnimations.kt:29-39`
  - HUD bar key: `screens/levels/stats/PlayerStatsHandler.kt:73-85`
  - pause grid, hard-coded: `screens/menus/level/LevelPauseScreen.kt:61-66`
  - rewards: `Megaman.kt:79-93`
  - weapon checks inside movement abilities: NEEDLE_SPIN blocks 7 behaviors, RUSH_JET changes air dash (`BehaviorsComponent.kt:51, 118, 157, 280-311, 393, 432, 535, 723`), RODENT_CLAWS appears in `ControllerComponent.kt:157-205`
- The buster costs 0 by a special case (`:610`).
- X6 facts (`mmx-demake-tools/docs/players/weapons.md`, "Slots, weapons and ids"; `zero.md`, "Technique bits"): slot n fires id n and the charged shot is id n+9. Energy is per gauge. Guard Shell costs energy only when it blocks. Owned weapons equal the beaten-Maverick byte `game+0x60`. Zero reads the same byte as technique bits, so one shared progression unlocks a different kit per character.

2. Options
A. An enum with properties and an abstract `fire`. Pro: small change. Con: a closed set that cannot vary per character.
B. `WeaponDef` objects in a registry keyed by a stable string id, plus a `Loadout` per character. Pro: open and per character. Con: more types.
C. B with numbers in JSON and behavior in code. Do this later, together with item 21.

3. Recommendation
Option B:
- `WeaponDef(id, hudKey, icon, animAtlas, cooldown, maxAmmo, cost(charge), chargeable(ctx), canFire(ctx), fire(ctx, charge), onSelect/onDeselect, update(ctx, dt), blocksAbilitiesWhileFiring: Set<AbilityId>, abilityModifiers, damageTags (item 22))`.
- `ctx` is the owner `IPlayer` plus a spawn-position helper. Today `getSpawnPosition` (`MegamanWeaponsHandler.kt:156`) is Mega-specific and moves to the character.
- `Loadout` = ordered weapon list plus a pause-grid layout.
- Rewards: the level manifest names a reward id (item 24). `CharacterDef.unlocks(progress)` maps shared progress to that character's weapons, as X6 does for X and Zero.
- The HUD, pause screen and save file iterate the loadout, not `MegamanWeapon.entries`.

4. Open questions for the owner
- Is progress (bosses beaten) shared across characters, as in X6?
- Can two characters share a `WeaponDef` but use different sprites?
- Must the saved weapon ids stay readable from the old save (item 6)?

5. Backlog wording
"One `WeaponDef` per weapon; loadout per character"
- `WeaponDef` holds cooldown, costs, charge rules, fire behavior, HUD/icon/animation keys, ability blocks and damage tags.
- Registry with stable ids. The HUD, pause grid and save use the loadout.
- Remove the weapon `when`s from the handler, behaviors, controller, HUD and pause screen.
- Rewards come from the level manifest through `CharacterDef.unlocks`.

6. Dependencies
Item 28 (`IPlayer`, `CharacterDef`), 22 (damage tags), 24 (rewards in manifest), 6 (save ids), 39 (animation definitions).
````
