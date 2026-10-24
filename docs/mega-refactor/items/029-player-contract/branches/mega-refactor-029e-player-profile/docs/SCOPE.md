# Scope: mega-refactor-029e-player-profile

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

`GameState` is the one session profile. The lives and the owned weapons move off the player entity and
into it. All code reads the profile from `game.state`, not through `megaman`. Lives calls name the player's
slot, so a later change from a shared pool to per-player lives changes only `GameState`. `ItemEntity` takes
the player view. With one player the game plays the same, and the save string does not change.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029e` `player-profile`: Lives, tanks, enhancements and weapons owned move to one session
  profile; `ItemEntity` retype (`Life` reads lives). With `007` (save)".
- Decision H (2026-10-22): "lives are a shared pool or per player, by game mode. Use an interface (for
  example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap." This branch keeps
  the switch easy (the slot argument) but does not write the interface (no caller yet; see `NOTES.md`).
- `007` decision (2026-10-23): "The save holds one session profile (what the lead player earns is shared),
  not one profile for each player."
- The `029b` to `029d` rules: call sites name their intent; write code only when production code calls it.

## Current state

The full scan is in `docs/NOTES.md` ("Scan", on `b3bc97f80`). Summary, with paths relative to
`core/src/main/kotlin/com/megaman/maverick/game/`:

- `GameState` (`state/GameState.kt`) already holds the levels defeated, heart tanks, health tanks with
  their fill, enhancements, currency and difficulty. It makes the save string and tells its listeners about
  each change.
- Lives are on the entity: `Megaman.lives` (`entities/megaman/Megaman.kt:189`), with `removeOneLife`,
  `isAtMinLives` and `resetLives` (`:1039-1043`). They are not saved; load resets them to 3.
- Weapons owned are the keys of `MegamanWeaponsHandler.weaponHandlers`. `Megaman.onAddLevelDefeated`
  (`:960-964`) fills them from the reward table `Megaman.getWeaponsFromLevelDef` (`:84-98`) and sets the
  game property `WEAPONS_ATTAINED`. The level override calls `putWeapon` (`MegamanMaverickGame.kt:789`);
  reset calls `clearWeapons` (`:773`). `MEGA_BUSTER` is put in `Megaman.init` (`:438`) and never removed.
- `Megaman` is the only `IGameStateListener` (`MegamanMaverickGame.kt:401`). It wraps the profile reads:
  `hasEnhancement`, `hasHeartTank`, `hasHealthTank`, `hasAnyHealthTanks`, `addToHealthTanks`, `hasWeapon`,
  `putWeapon`, `clearWeapons`.
- Outside the player, the profile is read through `megaman` in `AbstractEnemy` (drop odds `:100, 106-108`,
  damage scaling `:249`, one-up `:335`), `AbstractBoss:200`, `ReactorMonkey:156`, `HealthTank:62`,
  `HeartTank:99`, `Life:52`, `MegaLevelScreen:622, 642-644`, `MainMenuScreen:135`,
  `MegamanMaverickGame:774, 791, 833`, `PlayerStatsHandler`, `LevelPauseScreen`.
- `ItemEntity.contactWithPlayer(megaman: Megaman)` (7 items); `PlayerItemRule:19` checks `is Megaman`
  with a `029` `TODO`.

## In scope

1. **Lives in `GameState`.** One shared count (`Points`, 0 to 9, start 3), not in the save string.
   - `getLives(slot)`, `addLife(slot): Boolean`, `removeLife(slot)`: the slot is the player the call is
     about. The shared count ignores it, with a `TODO` for per-player lives (`047`, decision H).
   - `livesExhausted()`: today's `isAtMinLives` (game over). A `TODO` says that with per-player lives this
     is a level-flow rule (`047`).
   - `resetLives()` and `setLives(value)` (level override).
   - `GameState.reset()` does not reset the lives, as today; the callers keep their own `resetLives` call.
2. **Owned weapons in `GameState`; the player reads them from there** (owner design, 2026-10-23).
   - A set in `GameState`, not in the save string. The reward table moves out of the `Megaman` companion
     to the levels package. `addLevelDefeated` adds the reward weapons; `removeLevelDefeated` removes them;
     the level override adds its weapons; `reset` clears the set. `MEGA_BUSTER` is owned at all times.
   - `MegamanWeaponsHandler` no longer stores ownership. It keeps one runtime entry for each weapon (ammo,
     cooldown, the shots on screen, chargeable), and `hasWeapon` reads `game.state`. `putWeapon`,
     `removeWeapon` and `Megaman.clearWeapons` go. No listener call copies weapons into the player.
   - Two effects that today come from the map change are kept: a weapon that is gained has full ammo, and a
     lost current weapon falls back to `MEGA_BUSTER`. `Megaman.onSpawn` already refills all ammo and selects
     the buster on each spawn; the reset paths keep their current-weapon reset.
   - `WEAPONS_ATTAINED` keeps today's value at each point where the screens read it.
3. **Heart tanks: max health from `GameState`.** Megaman's heart-tank listener sets the max health to
   `START_HEALTH + count × HEALTH_BUMP` from `game.state`, in place of `+=`/`-=`. The value is the same as
   today in every case (no other code writes the max). A `TODO` at the player registration says that a
   player that registers later must run the same set (`047`). The set stays a stored value, because
   `Points.max` is a field; a max that is read through on each access needs an engine change (not in scope).
4. **Shared and per-player data** (owner direction, option (a), 2026-10-23). Shared data lives in
   `GameState` and has no slot (weapons, heart tanks, health tanks, enhancements, currency, levels). Data
   that can become per-player is keyed by slot in its calls (lives). All data is shared in this branch.
   Configuration to pick per field (option (b)) is for later (`047`).
5. **The profile reads move to `game.state`.** The `Megaman` wrappers go. Their logic moves to `GameState`
   (`addToHealthTanks`, `hasAnyHealthTanks`, the lives calls). Every caller changes,
   inside the player and outside it, including the `029f` screens (`MegaLevelScreen`, `MainMenuScreen`,
   `PlayerStatsHandler`, `LevelPauseScreen`). The screens keep `megaman` for health, ammo and the current
   weapon until `029f`. The `029e` `TODO`s at `AbstractEnemy`, `AbstractBoss` and `ReactorMonkey` go.
6. **Call sites that need a slot:**
   - death: `MegaLevelScreen` reads the slot from `PLAYER_JUST_DIED` (it is there since `029a`);
   - `Life`: the slot of the player view it receives;
   - the one-up: `targetPlayer`'s slot, with a `047` `TODO` (who gets it is a co-op rule);
   - the `Life` drop odds: `targetPlayer`'s slot;
   - the pause screen display: `PlayerSlot(0)` by name.
7. **`HealthBulb` drop odds** read `targetPlayer`'s health ratio, with a `047` `TODO` (whose health counts
   is a co-op rule).
8. **`ItemEntity.contactWithPlayer(player: MegaPlayer)`.** The items put `player.slot` in their event
   props. `PlayerItemRule` uses `asPlayer` and loses its cast and `TODO`.
9. **Tests:** the lives calls (shared count, the slot is ignored, bounds, `livesExhausted`, reset); the
   owned-weapon set (reward table on level add and remove, override, reset, the buster) and `hasWeapon`
   reading it; the max health from the heart-tank count;
   the save string is the same as before for the same data; `addToHealthTanks` moves with
   `MegamanHealthTankTest` (renamed for `GameState`); `PlayerItemRule` passes the view, if practical.

New APIs and their first callers:

| API | First callers |
|---|---|
| `GameState.getLives`, `addLife`, `removeLife`, `livesExhausted`, `resetLives`, `setLives` | `LevelPauseScreen`, `Life`, `AbstractEnemy`, `MegaLevelScreen`, `MainMenuScreen`, `MegamanMaverickGame` |
| `GameState` weapon set (`containsWeapon`, `addWeapon`) and the reward table | `MegamanWeaponsHandler.hasWeapon`, `LevelPauseScreen`, `PlayerStatsHandler`, the level override, `addLevelDefeated` |
| `GameState.addToHealthTanks`, `hasAnyHealthTanks` | `PlayerStatsHandler`, `LevelPauseScreen` |
| `MegaPlayer.healthRatio` | `AbstractEnemy` (`HealthBulb` drop odds) |

## Out of scope

- The save format, health-tank fill in the save, saving lives (`007`).
- `LivesPolicy` and per-player lives, game over with more players, late join, configuration of shared and
  per-player data (`047`).
- A max health that is read through from `GameState` on each access (needs a `Points` change in the engine).
- Coins and level points (per-level game properties), difficulty reads.
- The level screens' use of `megaman` for health, ammo, current weapon and the stats handler (`029f`).
- The weapon handler and loadout per character (`024`).
- The `SLOT` prop on the item events is still not read by `MegaLevelScreen` (`029f`).

## Affected areas and risks

- **The save string.** Lives and weapons must stay out of `toString`; `fromString` must fill the weapons
  through `addLevelDefeated` as today. A test compares the string.
- **Weapon ownership.** The handler today makes a new entry when a weapon is gained (full ammo) and removes
  it when a weapon is lost (current weapon to the buster). With fixed entries, these come from
  `Megaman.onSpawn` (all ammo full, buster selected) and the reset paths. The pause screen, the stats
  handler and `GetWeaponScreen` must see the same weapons at the same time as today.
- **Max health.** The heart-tank listener now sets the max, not adds to it. Same value, if no other code
  writes the max (the scan found none).
- **The buster.** The pause screen loops over every `MegamanWeapon`, including `MEGA_BUSTER`; it must stay
  owned after reset, load and override.
- **Reset paths.** `resetPlayerProgress`, the main menu, load, and game over each reset a different mix of
  state, lives and weapons today. Each keeps its exact mix.
- **Lives bounds.** `addLife` returns whether the count changed (the one-up sound plays only then).
- **Files:** `GameState`, `IGameStateListener`, `Megaman`, `MegamanWeaponsHandler` (maybe), the 7 items,
  `ItemEntity`, `PlayerItemRule`, `AbstractEnemy`, `AbstractBoss`, `ReactorMonkey`, `MegaPlayer`,
  `MegamanMaverickGame`, `MegaLevelScreen`, `MainMenuScreen`, `PlayerStatsHandler`, `LevelPauseScreen`,
  `GetWeaponScreen` (maybe), the player components that read enhancements.

## Overlap with other branches

- No other `029` branch is active. The active items `001`, `008` and `019` do not touch this code.
- `007` (planned) will read this profile for the save. This branch keeps the save string unchanged.

## Open questions

None. The owner answered the scan questions on 2026-10-23 (see `NOTES.md`, "Decisions").
