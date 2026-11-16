# Scope: mega-refactor-024b-character-loadouts

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-15 (D1 A, D2 A, D3 B with the owner type on the def, D4 B; see `NOTES.md`)

## Goal

Each playable character has its own weapon loadout: its weapons in select order and the weapon each boss gives it.
Owned weapons are kept per character. Bosses beaten stay shared. The `is Megaman` weapon workarounds go: the HUD,
the pause screen, the weapon-energy pickup, the progress reset, the beam-out and the get-weapon screen ask the slot's
player for its weapons. Megaman, X and Proto Man play the same, except one fix: enemies that react to a shot (`Met`) also see X's and
Proto Man's shots (D4).

## Item

`024`: One `WeaponDef` per weapon; loadout per character. From `ITEM.md`:

- Proposed scope: "A registry with stable ids. The HUD, pause grid and save use the loadout." "Rewards come from the
  level manifest through `CharacterDef.unlocks`."
- Workarounds from `081b`: 5 places check `is Megaman` with a `TODO` for this item (`PlayerStatsHandler` weapon bar,
  `MegaLevelScreen` `ADD_WEAPON_ENERGY`, `LevelPauseScreen` ammo and select, `resetPlayerProgress`,
  `EndLevelEventHandler` beam-out).
- Decisions (owner, 2026-11-15): the loadout is per character, and one boss can give each character a completely
  different weapon. Bosses beaten are shared. Two characters can share a `WeaponDef` when the attack is similar; a def
  is pure (no play state), and the look fields can differ per character.

## Current state

The scan is in `temp/mega-refactor-024b-character-loadouts/scan.md` (main checkout, not committed). Summary:

- Ownership: `GameState.weaponsOwned` (`state/GameState.kt:33`) is one `ObjectSet<WeaponDef>` with no character key.
  `addLevelCompleted` (`:55-62`) adds `LevelRewards.weaponsFor(level)`; `containsWeapon` (`:82`) is
  `alwaysOwned || contains`. The save string (`:342-366`) holds no weapons: owned weapons come back through the
  completed levels. `--weapons` and `LoadoutOverride` call `addWeapon` (`MegamanMaverickGame.kt:502,858-868`).
- Rewards: `LevelRewards.weaponsFor` (`levels/LevelRewards.kt:9-23`) is a `when` over 8 levels with Megaman's defs.
  `Megaman.onLevelCompleted` (`entities/megaman/Megaman.kt:259-261`) sets `LevelState.weaponsAttained`; every
  `Megaman` in the roster listens to `GameState`, also when X or Proto Man played.
- Characters: `PlayerCharacter` (`players/PlayerCharacter.kt:3`) is an enum with no fields. No `CharacterDef`
  exists. `PlayerRoster.slot0` maps a character to its entity; an entity does not know its character. `MegaPlayer`
  (`players/MegaPlayer.kt:61`) is the base of the 3 player classes and has no weapon member.
- Type checks for weapons: 10 lines in 7 files. The 5 places of the item table, plus `GetWeaponScreen.kt:154,196`
  (a hard `as Megaman` cast) and `EntityQueries.kt:45-47` (`shooting` is always `false` for X and Proto Man, so
  `AbstractEnemy.isMegamanShootingAtMe` ignores their shots).
- One list: `MegamanWeapons.all` feeds at least 9 sites (the handler state map, the select cycle, the HUD bars, the
  pause regions and `FULL_TABLE`, the get-weapon animators, the beam regions, the trail sprites, `--weapons`).
- `MegamanWeaponsHandler` holds the weapon states and the ammo, and also `getSpawnPosition()` (135 lines of Megaman
  body offsets).
- Proto Man has his own slot model (`ProtomanWeapon`: `SABRE`, `BUSTER`), which `060l` replaces now. X has only the
  buster and the sabre.
- `WeaponDef.movesBlockedWhileFiring` is a mutable `ObjectSet`, the only mutable collection on a def.

## In scope

- `CharacterLoadout` (`players/`): for one character, the weapons in select order and the reward weapons per level.
  One loadout per `PlayerCharacter`; Megaman's holds his 10 defs and today's reward table; X's and Proto Man's are
  empty. `LevelRewards.weaponsFor` moves into Megaman's loadout.
- `MegaPlayer.character: PlayerCharacter`, so a player entity can name its loadout.
- Owned weapons per character in `GameState`: derived from the shared completed levels and the character's reward
  table, plus a per-character set for `--weapons` and `LoadoutOverride`. `alwaysOwned` counts only inside the
  loadout that lists the def. The save format does not change.
- A weapons capability on a player (`PlayerWeaponsComponent`, read with a `weapons` extension that gives `null` for a
  player with none): the current weapon, select, ammo per weapon, add weapon energy, reset to the first weapon.
  `Megaman` adds it, backed by his handler. The 5 workarounds, `GetWeaponScreen` and the beam-out read it in place
  of `is Megaman`.
- The reward flow: the level flow sets `weaponsAttained` from the slot-0 character's loadout, not from each
  `Megaman` entity.
- The select cycle, the energy fill order, the handler state map and `--weapons` read the loadout list, not
  `MegamanWeapons.all`. `--weapons` gives each named def to every character whose loadout lists it; `all` means every
  weapon of every loadout.
- The owner type on the def (D3): `WeaponDef<in O : MegaPlayer>`, and the same type parameter on `WeaponState`
  hooks, `WeaponAirDash` and `CharacterLoadout<O>`. Megaman's defs are `WeaponDef<Megaman>` and keep their code. A
  def typed `WeaponDef<MegaPlayer>` is a shared def: it reads only `MegaPlayer` members, and it fits the loadout of
  any character. No cast to `Megaman` in a hook.
- The look per character (D3): the look fields move from `WeaponDef` into a `WeaponLook<O>` that the loadout holds
  for each of its defs: the atlas, the animation-key rules (`shootKey`, `airDashKey`, `jumpDownChargeKeys`,
  `trailWhileFiring`, `showsTornadoWhileFiring`, `keepsAnimTimeAcrossKeys`, `restartsSameKeyOnPress`), the HUD bit
  region, and the pause and get-weapon region names. The strings stay the same for Megaman. The readers (Megaman's
  sprites, the HUD, the pause screen, the get-weapon screen, the beam-out, the trail) read the look of the player's
  loadout. `displayName` stays on the def (made from the id).
- `MegaPlayer.shooting` (D4): each player class gives its shooting flag (Megaman's `shooting`, X's and Proto Man's
  `buster.shooting`). `EntityQueries.shooting` and `AbstractEnemy.isMegamanShootingAtMe` read it, so a `Met` also
  stays under its helmet while X or Proto Man shoots at it.
- `WeaponDef.movesBlockedWhileFiring` becomes a private set read through `preventsWhileFiring(move)` (purity).
- `LoadoutOverride.weapons` and the launch weapons become LibGDX collections (`docs/CODE_STYLE.md`).

## Out of scope

- The split of `getSpawnPosition` out of `MegamanWeaponsHandler`: Megaman's defs keep reading it through `Megaman`.
  A second character's spawn position comes with its first `WeaponDef` (`060h`).
- The pause `FULL_TABLE` layout per character: it stays Megaman's layout, shown for every character as today, with a
  `TODO` (`060h`).
- Proto Man's `ProtomanWeapon` slots (`060l` changes them; `060h` gives him `WeaponDef`s).
- `CharacterDef` as a full character record (`081`). This branch adds only the loadout part.
- The Moon Scythes array bug (`docs/issues/2026-11-14-moon-scythes-stale-array.md`).

## Affected areas and risks

- `state/GameState.kt`, `levels/LevelRewards.kt`, `levels/LevelState.kt`, `levels/LoadoutOverride.kt`,
  `LaunchOptionsResolver.kt`, `MegamanMaverickGame.kt` (launch weapons, overrides, `resetPlayerProgress`).
- `players/` (new loadout and component, `MegaPlayer.character`), `Megaman.kt`, `MegamanX.kt`, `Protoman.kt` (the
  character value), `MegamanWeaponsHandler.kt`, `ControllerComponent.kt`, `MegamanJetpackHandler.kt`.
- Screens: `PlayerStatsHandler.kt`, `MegaLevelScreen.kt`, `LevelPauseScreen.kt`, `GetWeaponScreen.kt`,
  `EndLevelEventHandler.kt`, `RobotMasterLevelOnCompletion.kt`.
- Risks:
  - The reward flow moves out of `Megaman.onLevelCompleted`. A wrong order shows no get-weapon screen after a boss.
  - Derived ownership changes one dev edge: today `removeLevelCompleted` also removes a weapon that `--weapons` gave
    when the same def is a reward. After the change, a `--weapons` weapon stays. No play path calls it with
    `--weapons`; the contract names it.
  - `PlayerStatsHandler.kt` overlaps with `060l` (Proto Man's slot icons). The branch that merges second resolves it.

## Overlap with other branches

- `060l` (active, uncommitted): deletes `ProtomanWeapon.kt`, changes `Protoman.kt` and `PlayerStatsHandler.kt`. This
  branch adds `character` to `Protoman.kt` (1 line) and changes the weapon bar in `PlayerStatsHandler.kt`.
- `011a` (active): moves 2 hunks into `entities/megaman/weapons/` when it takes `master`; this branch does not change
  those functions.
- `077b` (active): art only so far. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` | no effect | a component, not a new class layer |
| `O1-R4` | helps | typed loadout data, no new keys |
| `O1-R5` | helps | each new API has a caller in the branch: Megaman's loadout, defs and looks, and X's and Proto Man's `shooting` |
| `O2-R4` | no effect | the loadout list keeps the select and fill order |
| `O2-R5` | helps | owned weapons and the reward screen name a character, not "the" Megaman |
| `O2-R9` | helps | the reward is the slot-0 character's |
| `O6-R2` | helps | tests for ownership per character and the reward per character |
| `O8-R2` | helps | the 10 type-check lines for weapons go, `EntityQueries` too (D4) |
| `O8-R4` | helps | "a loadout belongs to a character" |
| `O9-R2` | no effect | the save format does not change |

## Open questions

In the brief.
