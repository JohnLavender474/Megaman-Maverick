# Scope: mega-refactor-060l-protoman-one-input

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file.

- Status: approved
- Approved: 2026-11-15

## Goal

Proto Man has one attack input in place of the 2 weapon slots of `060c`: `B` is the buster, `UP`+`B` is the sabre, as
X. `SELECT` no longer switches a slot.

## Item

From `060` `ITEM.md`, Decisions (2026-11-15, owner, at the `060d` play test):

> The 2 weapon slots (`060c`, 2026-11-13) go in a new branch, `060l`, after `060d`: X's input, `B` is the buster
> (later the current special weapon of `060h`) and `UP`+`B` is the sabre. On a ladder `B` stays the buster (the
> `060c` rule). `SELECT` stays for the special weapons.

Plan row: "`ProtomanWeapon`, the 2 HUD slot icons and the slot branches go". Open questions: `DOWN`+`B` on the ground
(the crouch shot or the crouch slash); `UP`+`B` on the ground (the 3-slash combo or a single slash).

## Current state

- `entities/protoman/ProtomanWeapon.kt`: enum `SABRE`, `BUSTER` with a HUD region each and `next()`.
- `entities/protoman/Protoman.kt`:
  - `var weapon` (default `SABRE`, reset in `onPlayerSpawn`).
  - The `B` actuator order: `sabre.active` chains; `climbing` shoots; `sliding` calls `slideAttack()`;
    `crouchAttackWanted()` calls `crouchAttack()`; `guardWanted()` does nothing; sabre slot starts the sabre (combo on
    the ground); `UP` starts a single slash (`chains = false`); else the standing shot (also the jump shot in the air).
  - `crouchAttack()`: a `when (weapon)`: the crouch slash (`SabreState.CROUCH`) or the crouch shot.
  - `slideAttack()`: buster slot: `B` the slide shot, `UP`+`B` the single slash; sabre slot: the floor combo.
  - `SELECT` (on release, with `selectButtonAction` `NEXT_WEAPON`) calls `selectNextWeapon()`: the next slot and a
    ping; not out of play, while damaged, teleporting or in a slash.
- `screens/levels/stats/PlayerStatsHandler.kt`: `protomanIcons` (one `GameSprite` per slot, `Hud` atlas regions
  `protoman_sabre`, `protoman_buster`), drawn with `(player as? Protoman)` (`O8-R2` workaround, `TODO` for `153`).
- `smokeTest/.../HudIconsSmokeTest.kt`: checks the 2 regions.
- `screens/levels/MegaLevelScreen.kt`: the `SELECT` pause rule reads `selectButtonAction` for every player but X. For
  Proto Man, `START` still pauses on `SELECT`; no change.
- `ProtomanTest.kt`: 2218 lines; about 30 places switch the slot (`useBuster()`, `SELECT` steps, 5
  `listOf(SABRE, BUSTER)` loops) and 1 group tests the switch itself.
- Art: `assets/sprites/frames/Hud/protoman_sabre.png`, `protoman_buster.png`, and the 2 regions in `Hud.txt`.

## In scope

1. `B` actuator, in this order: `sabre.active` chains; `climbing` shoots (the `060c` rule); `sliding`:
   `slideAttack()`; `crouchAttackWanted()`: the one crouch attack of decision D1; `guardWanted()`: nothing; `UP` held:
   the sabre (ground: decision D2; air: one air slash); else the buster (standing shot, jump shot).
2. `slideAttack()`: `B` the slide shot; `UP`+`B` ends the slide with the slash of decision D2 (no slash under a low
   ceiling, as today).
3. `ProtomanWeapon`, `weapon`, `selectNextWeapon()` and the `SELECT` actuator go. `SELECT` does nothing for Proto Man
   until `060h` (`TODO` at the controller component).
4. HUD: `protomanIcons` and the `as? Protoman` line go. The `Hud` atlas goes (D3).
5. The crouch slash goes with its code path (D1).
6. `ProtomanTest`: the slot cases go; the cases that switched to the buster use `B`, the sabre cases use `UP`+`B`.
7. `060c` and `060d` manual checks that name a slot are rewritten in this contract.

## Out of scope

- Special weapons and `SELECT` for them (`060h`).
- X's input and Megaman's input.
- The sabre and buster rules themselves (damage, combo timing, shot speed, poses), except the sabre damage on large
  enemies of scope change 1.

## Affected areas and risks

- `Protoman.kt` (the `B` actuator, `crouchAttack`, `slideAttack`, spawn reset, controller), `ProtomanWeapon.kt`
  (deleted), `PlayerStatsHandler.kt`, `HudIconsSmokeTest.kt`, `ProtomanTest.kt`; maybe `ProtomanSabre.kt` (D1).
- Risk: `UP` is also the ladder grab. `UP`+`B` at a ladder: the controller runs before the behaviors
  (`MegamanMaverickGame.kt`, system order), so a same-frame `UP`+`B` starts the combo and `ClimbParams.blocked`
  keeps the grab off until the slash ends; with `UP` held a frame earlier he grabs and `B` is the ladder shot.
  `ProtomanTest` pins it (corrected at step 5).
- Risk: the many test edits hide a rule change; each removed case is mapped to a kept or a new case.

## Overlap with other branches

- `mega-refactor-024a-weapon-defs` (active, in its play test): changes `PlayerStatsHandler.kt` (Megaman's weapon
  bars from `WeaponDef`, the imports). `060l` removes `protomanIcons` and its draw line, other hunks of the same
  file; a conflict there is the simple kind (imports, next hunks). No Proto Man file in `024a`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` | helps | Slot code with no remaining use goes. |
| `O1-R3` | no effect | The `B` order stays one `when`. |
| `O2-R1` | no effect | Input stays through the controller frame. |
| `O8-R1` | no effect | No new copy of `Megaman` or X code. |
| `O8-R2` | helps | The `as? Protoman` line in `PlayerStatsHandler` goes. |
| `O8-R4` | no effect | `ProtomanWeapon` is not a `WeaponDef`; `060h` joins the loadout. |

### Scope change 1 (2026-11-16, owner, during the play test)

The owner, at `./run-game-alpha.sh --level REFACTOR_TEST_060 --room sabre`: "The purple bunny in this room requires
like 4 sabre slashes which is ridiculous. Update this enemy as well as all similar enemies to be dealt more damage by
the sabre."

- The `PlayerSabreSlash` entry of the `LARGE` row in `StandardDamageNegotiator` goes from 4 (normal) and 5 (full) to
  10 and 12. Target: one 3-slash floor combo (10 + 10 + 12 = 32) kills a large enemy of 30 health; 3 air slashes too.
- Affected: every `Size.LARGE` enemy on the standard table: `Robbit` (the purple bunny), `BigJumpingJoe`,
  `PreciousTron`. `MetaDridler` takes no damage, `MetaWheelF` has its own sabre entry (6 and 9), bosses use the
  `AbstractBoss` table: no change.
- X uses the same `PlayerSabreSlash` (every X move is full strength), so X's sabre also does 12 to these enemies.
- No change to the `MEDIUM` and `SMALL` rows, the buster, or the bosses. Item `141` keeps the later per-slash and
  per-target rule.

## Decisions

Owner, 2026-11-15, at the brief:

- D1: `DOWN`+`B` on the ground is the crouch shot. The crouch slash goes: `SabreState.CROUCH`,
  `ProtomanSabreMoves.CROUCH`, the `crouchAttack()` slot branch, and its animation keys and table rows. Its art frames
  stay in the `Protoman_v2` atlas (060g or `060k` can use or drop them).
- D2: `UP`+`B` on the ground starts the 3-slash floor combo (`chains = true`); `B` during a slash chains it, with or
  without `UP`. `UP`+`B` in a slide ends the slide with the floor combo. In the air, `UP`+`B` is the one air slash.
- D3: the 2 HUD icon frames go, and with them the whole `Hud` atlas (it holds only these 2 regions): `Hud.txt`,
  `Hud.png`, `assets/sprites/frames/Hud/`, `TextureAsset.HUD`, `HudIconsSmokeTest`. At the merge, `153` `ITEM.md` gets
  a note that it makes the `Hud` atlas again (follow-up question, 2026-11-15).

## Open questions

None.
