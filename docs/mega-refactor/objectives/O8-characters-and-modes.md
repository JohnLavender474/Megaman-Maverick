# `O8`: More playable characters and modes

- Status: active

## Problem

- `P1`: Megaman is the only playable character. Proto Man (`060`) and X (`081`) cannot be added with no large copy of
  player code while the player logic stays tied to `Megaman`.
- `P2`: Weapon data is in `when` blocks in at least 8 files (`024`), so a character cannot have its own loadout with no
  change to each file.
- `P3`: X needs features that the main game does not have, for example a dash button (`069`) and slopes (`068`).

## Goal

More than one playable character, each with its own abilities and physics, and modes such as extra levels, Spec Ops
missions and PvP that use them.

## Done means

- Proto Man is playable from the shared player parts.
- X is playable in the extra levels.

## Rules

### `O8-R1`: No copy of `Megaman.kt`

- Rule: a new character uses shared player components, not a copy of `Megaman.kt` or a player base class.
- Problem: `P1`
- Check: review the new character class.
- Applies: `linked items`
- Active: `now`

### `O8-R2`: Capabilities, not character checks

- Rule: no logic checks for a specific character where a capability fits.
- Problem: `P1`
- Check: search for `is Megaman` and casts to `Megaman` in gameplay code.
- Applies: `always-on`
- Active: `now`

### `O8-R3`: Name the modes

- Rule: a character item names the modes that it changes.
- Problem: `P3`
- Check: read `ITEM.md` and `SCOPE.md`.
- Applies: `linked items`
- Active: `now`

### `O8-R4`: One definition for each weapon

- Rule: the data of a weapon is in one definition, and a loadout belongs to a character.
- Problem: `P2`
- Check: count the `when` blocks on `MegamanWeapon`.
- Applies: `linked items`
- Active: after `024`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01, with the `084h` changes (`d796e00ba`). Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O8-R1` | `not checked` | No new character exists: 0 `ProtoMan` and 0 `CharacterDef`. A test entity shows that a player need not be `Megaman` (`SecondPlayerEntity.kt:13`). Since `081a` (2026-11-06), the shared spawn, destroy, room transition, out-of-level kill, invincible, can-move and damage rules are free functions in `players/PlayerLifecycle.kt` and `players/PlayerRules.kt`, and the damage table is `players/PlayerDamageNegotiator.kt`; `Megaman` is their only game caller, and tests run them on a player that is not `Megaman`. Since `081b` (2026-11-06), `MegamanX` is a second player class: it calls the same `players/` functions, `players/playerJumpBehavior(JumpParams)` and `PlayerDamageNegotiator`, and has its own body, run and buster; no copy of `Megaman.kt`. `CharacterDef` is not made yet. | `060`, `081` |
| `O8-R2` | `partly` | 5 `is Megaman` lines since `081b` (workarounds with `TODO`s for `024`; list in `024` `ITEM.md`). 10 `as Megaman` and `as? Megaman` lines (recount 2026-11-02), for example `LevelPauseScreen.kt:569`, `PlayerStatsHandler.kt`. 13 gameplay `Megaman::class` lines in 9 files, for example `JoeBall.kt:151`. 32 files outside `entities/megaman/` import `Megaman`. | `024`, `030` |
| `O8-R3` | `partly` | `081`, `068` and `069` name the modes. `060` `ITEM.md` does not. | `060` |
| `O8-R4` | `broken` (waiting) | 0 `WeaponDef`. At least 5 `when` blocks on a weapon (`MegamanWeaponsHandler.kt:303,658`, `MegamanAnimationKeySupplier.kt:105`, `MegamanAnimations.kt:29`, `Megaman.kt:119`). 28 files name a `MegamanWeapon` value. The loadout is one session set (`GameState.kt:33`). | `024` |

## Redo risk

- `029g`, `084`: all 8 files in `entities/megaman/behaviors/` and all 11 in `components/` use the `Megaman` type. `081` plans this change for the behaviors. The state class `entities/state/WeaponChargeComponent.kt` does not use it, but the 5 charge reads and `stopCharging()` in `components/WeaponChargeComponent.kt` do.
- `081a`: a second player class must call the `players/` functions, not copy the lines of `Megaman.onSpawn`, `onDestroy`, `onEvent` and `takeDamageFrom`. The branch `x6-intro-extra-level` adds 7 entries to `MegamanDamageNegotiator`, which is now `players/PlayerDamageNegotiator.kt`; they must move when it takes in `master`.
- `081b`: a third character must not copy `MegamanX` either. The `is Megaman` workarounds (5) grow with each character until `024`. The 8 Megaman behaviors other than the jump still take the `Megaman` type; phase 2 of `081` parameterizes the wall slide and the air dash.
- `029e`: `GameState.weaponsOwned` must become per character or per loadout (`024`).

## Items

| Item | Status | Note |
|------|--------|------|
| `024` | planned | [One `WeaponDef` per weapon; loadout per character](../items/024-weapon-defs/ITEM.md) (main) |
| `060` | idea | [Proto Man as a playable character](../items/060-proto-man/ITEM.md) (main) |
| `068` | done | [Floor slopes for extra levels](../items/068-slopes/ITEM.md) (main) |
| `123` | idea | [X6 flat step-down off a slope](../items/123-slope-flat-step-down/ITEM.md) |
| `069` | dropped | [A dash button for X](../items/069-dash-button/ITEM.md) (main) |
| `081` | active | [Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`](../items/081-playable-x/ITEM.md) (main) |
| `084` | done | [Split `Megaman` into player components](../items/084-split-megaman/ITEM.md) (main) |
| `029` | done | [`IPlayer` contract, players service and `CharacterDef`](../items/029-player-contract/ITEM.md) |
| `030` | planned | [Faction and root owner instead of `owner == megaman`](../items/030-faction/ITEM.md) |
| `061` | done | [Thin player view; locks and signals as components and functions](../items/061-thin-player/ITEM.md) |
| `102` | idea | [A game mode concept](../items/102-game-modes/ITEM.md) (main) |
| `108` | idea | [Spec Ops co-op missions](../items/108-spec-ops-mode/ITEM.md) |
| `109` | idea | [PvP modes](../items/109-pvp-mode/ITEM.md) |
| `113` | idea | [Character select screen](../items/113-character-select/ITEM.md) (main) |
| `120` | idea | [A player that turns hostile and comes back](../items/120-hostile-player/ITEM.md) (main) |
| `121` | idea | [Enemy clones of a playable character](../items/121-playable-character-clones/ITEM.md) |
| `122` | idea | [Human-driven boss](../items/122-human-driven-boss/ITEM.md) (main) |
| `126` | idea | [Movement and damage blocks as lock reasons](../items/126-movement-damage-lock-reasons/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-01 (owner): X (`081`) comes before Proto Man (`060`), and `081` adds `CharacterDef`. The first X branch plays X in slot 1 behind a dev flag, and it does not wait for `024`.

## Open questions

- The code has no game mode concept, only `DifficultyMode`. Does a mode need its own item?
- How does a player pick a character, and does each co-op slot pick its own? (`113`)
