# Scope: mega-refactor-024a-weapon-defs

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-14

## Goal

Refactor only. Megaman's 10 weapons become `WeaponDef` objects with stable string ids, in one ordered list. The data
and the fire code of each weapon move into its definition, in place of the `when` blocks on `MegamanWeapon`. Megaman
plays the same.

## Item

`024`: One `WeaponDef` per weapon; loadout per character. From `ITEM.md`, "Proposed scope":

- `WeaponDef` holds cooldown, costs, charge rules, fire behavior, HUD, icon and animation keys, ability blocks and
  damage tags.
- A registry with stable ids. The HUD, pause grid and save use the loadout.
- Remove the weapon `when`s from the handler, behaviors, controller, HUD and pause screen.
- Rewards come from the level manifest through `CharacterDef.unlocks`.

This branch takes the first and the third point, and the registry of the second point. It leaves for `024b`: the
loadout per character, the `is Megaman` workarounds, and the HUD and pause screen for other characters. It leaves the
damage tags for `023` (damage keys on the projectile class, see "Current state") and the rewards from a manifest for
`025`.

## Current state

The full inventory is in `temp/mega-refactor-024a-weapon-defs/scan-outside-handler.md` (main checkout). Summary:

- `entities/megaman/constants/MegamanWeapon.kt`: an enum of 10 entries with no data.
- `MegamanWeaponsHandler.kt` (1290 lines): `createWeaponEntry` (`:303-511`) is a `when` that builds one
  `MegaWeaponHandler` per weapon; 4 entries are anonymous subclasses with their own timers (`MOON_SCYTHES`,
  `PRECIOUS_GUARD`, `NEEDLE_SPIN`, `RODENT_CLAWS`). The fire dispatch (`:658-668`) is a second `when`.
  `onChangeWeapon` (`:515-539`) has deselect rules for 2 weapons. The buster costs 0 by 2 special cases (`:607`,
  `:641`) and has infinite ammo by a third (`:573`).
- Outside the handler: 126 lines in 33 main files and 65 lines in 18 test files name `MegamanWeapon`.
  - Data reads by a `when` on the weapon: the atlas per weapon (`MegamanAnimations.kt:30-41`), the HUD bit region
    (`PlayerStatsHandler.kt:90-118`), the shoot-key suffix (`MegamanAnimationKeySupplier.kt:105-125`), the
    `jump_down` charge keys (`:130`), the slide and air-dash trail keys (`EffectComponents.kt:55-65`).
  - Move rules: 8 lines in 8 behaviors are one rule, "`NEEDLE_SPIN` and `shooting` blocks the move". `RUSH_JET` (7
    lines) changes the air dash and owns the jetpack. `RODENT_CLAWS` stops the run and grips the wall. `AXE_SWINGER`
    and `NEEDLE_SPIN` turn on body fixtures.
  - Strings made from the enum name: animation keys (`stand_megabuster`), trail regions, pause atlas regions, the
    get-weapon atlas regions and display text, the `--weapons` values. The cycle order and the energy fill order are
    the enum order (`ordinal`).
  - Rewards: `LevelRewards.weaponsFor(level)`. Owned weapons: `GameState.weaponsOwned`.
- Save: weapons are not in the save string. `fromString` rebuilds the owned set from the completed levels through
  `LevelRewards`. A replacement of the enum does not change the save format.
- Damage tables key on the projectile class (191 lines in 66 files), never on the weapon. X's buster fires the same
  `Bullet` and `ChargedShot`.
- Per-weapon runtime state (ammo, cooldown, spawned entities, the 4 extra timer sets, the claws `slash_index` in the
  Megaman property bag) lives in `MegaWeaponHandler`, one per weapon, inside the handler of each Megaman.

## In scope

- New package `core/.../weapons/`:
  - `WeaponDef`: `id`, the per-weapon data that today's `when`s give, the move rules as flags, and `newState()` and
    the fire hook. Each field has a caller in this branch (list in `PLAN.md`).
  - `WeaponState`: the per-player state of one weapon (ammo, cooldown, spawned). A weapon with more state has its own
    subclass (the moon scythe wait, the guard cooldown, the claws combo and refill).
- `entities/megaman/weapons/`: `MegamanWeapons`, the ordered list of the 10 defs with `byId(id)`, and one file of fire
  code per weapon that today is a `shoot*` function of the handler.
- `MegamanWeapon` goes. Callers take `WeaponDef`. The ids are today's enum names in lower case (`mega_buster`), so the
  atlas keys, the region names, the `--weapons` values and the display text stay the same strings.
- `MegamanWeaponsHandler` keeps the per-player part: the states, `getSpawnPosition`, `canFireWeapon`, `fireWeapon`,
  ammo. It loses `createWeaponEntry`, the fire `when`, the buster special cases and the deselect `when`.
- Every single-weapon rule outside the handler becomes a hook or a data field on the def (owner, D3): the moves that
  a weapon blocks while it fires (Needle Spin, 7 behaviors and the trails), the air-dash change and the jetpack (Rush
  Jet), the run stop and the wall grip (claws), the body fixtures while firing (Needle Spin, axe), the charge stop
  (Rush Jet), and the look rules (atlas, shoot key, air-dash key, trail key, tornado sprite, animation time and
  restart rules, `jump_down` charge keys, HUD bit region). The hooks take `Megaman`, with a `TODO` for `024b`.
- The claws `slash_index` moves from the property bag into the claws state.
- Tests: the tests that name `MegamanWeapon` move to the defs. New tests: the id strings, the list order, and each
  former special case (buster cost 0, infinite ammo).

## Out of scope

- A loadout per character, the `is Megaman` / `as Megaman` workarounds and the `GetWeaponScreen` hard casts (`024b`).
- Megaman's buster on `PlayerBuster` (`024b` or later; a behavior risk).
- Damage tags (`023`). Rewards from a level manifest (`025`). Proto Man's slots as weapons (`060h`).
- The `MOON_MAN` level checks in the barrage and the axe code (`O7-R3`, `025`): they move with the fire code,
  unchanged.
- `WeaponChargeComponent` stays a separate component (`084h` note): the charge timer is per player, the rule
  `chargeable` moves to the def.

## Affected areas and risks

- About 33 main files and 18 test files, mostly a type change at each line.
- The fire code moves file, with no logic change. Risk: a changed order of the pre-shoot, fire, post-shoot and ammo
  steps. Check: the steps stay in `fireWeapon`; a test fires each weapon through the public flow.
- The derived strings. Risk: a typo in an id breaks an atlas lookup at run time. Check: a test that lists every
  derived string against today's strings; `core:smokeTest` loads Megaman's atlases.
- Per-player state of the 4 special weapons. Risk: state that was per handler becomes shared. Check: `newState()`
  makes a new state for each Megaman; a co-op test with 2 players.

## Overlap with other branches

- `060d` (active, play test): rewrites `GroundSlideBehavior.kt` and adds `MegamanGroundSlideTest.kt`, which name
  `NEEDLE_SPIN` and `MEGA_BUSTER`. Plan: change `GroundSlideBehavior.kt` last; if `060d` merges first, merge `master`
  into this branch before that step. Else `060d` takes the change when it merges `master`.
- `fix/small-bugs-3` and `compactor-level`: read before the merge.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R4` One definition for each weapon | helps | The data and the fire code of each weapon are in its def. The loadout per character is `024b`. |
| `O8-R2` Capabilities, not character checks | waiting | The `is Megaman` workarounds stay until `024b`. No new check. |
| `O8-R1` No copy of `Megaman.kt` | no effect | |
| `O1-R2` Components, not new inheritance | helps | The 4 anonymous handler subclasses become state classes of one small base. |
| `O1-R4` Typed state, not new property keys | helps | The claws `slash_index` leaves the property bag. |
| `O1-R5` Code only with a production caller | no effect | Each `WeaponDef` field has a caller in this branch. |
| `O1-R3` No new fragile order | no effect | The list order is the enum order, and a test holds it. |
| `O2-R4` Stable iteration order | no effect | Same. |
| `O2-R5` No logic assumes one player | helps | The weapon state stays per player; the defs hold no state. |
| `O2-R2` Random only from the seeded source | waiting | The claws angles keep `getRandom` (`011`). |
| `O5-R1` No per-frame allocation | no effect | The fire code keeps its pooled vectors. |
| `O6-R2` Named checks | helps | New tests for the ids, the order and the special cases. |
| `O7-R3` No level-identity checks | no effect | The 2 `MOON_MAN` checks move unchanged. |
| `O9-R2` Saves stay loadable | no effect | Weapons are not in the save. |

## Open questions

None. The owner's answers to the brief are in `NOTES.md`, "Decisions".
