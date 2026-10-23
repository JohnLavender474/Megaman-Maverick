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
2. **Owned weapons in `GameState`.** A set, not in the save string.
   - The reward table moves out of the `Megaman` companion to the levels package.
   - `addLevelDefeated` adds the reward weapons; `removeLevelDefeated` removes them; the level override
     adds its weapons; `reset` clears the set. `MEGA_BUSTER` stays owned at all times, as today.
   - New listener calls `onAddWeapon`/`onRemoveWeapon`. Megaman puts or removes the weapon in its handler
     there. The ammo and the current weapon stay on the player. Reset still sets the current weapon to
     `MEGA_BUSTER`.
   - `WEAPONS_ATTAINED` keeps today's value at each point where the screens read it.
   - A `TODO` at the player registration: a player that registers after progress exists misses the
     heart-tank bumps and the weapon events (`047`). No code.
3. **The profile reads move to `game.state`.** The `Megaman` wrappers go. Their logic moves to `GameState`
   (`addToHealthTanks`, `hasAnyHealthTanks`, the lives calls, the weapon calls). Every caller changes,
   inside the player and outside it, including the `029f` screens (`MegaLevelScreen`, `MainMenuScreen`,
   `PlayerStatsHandler`, `LevelPauseScreen`). The screens keep `megaman` for health, ammo and the current
   weapon until `029f`. The `029e` `TODO`s at `AbstractEnemy`, `AbstractBoss` and `ReactorMonkey` go.
4. **Call sites that need a slot:**
   - death: `MegaLevelScreen` reads the slot from `PLAYER_JUST_DIED` (it is there since `029a`);
   - `Life`: the slot of the player view it receives;
   - the one-up: `targetPlayer`'s slot, with a `047` `TODO` (who gets it is a co-op rule);
   - the `Life` drop odds: `targetPlayer`'s slot;
   - the pause screen display: `PlayerSlot(0)` by name.
5. **`HealthBulb` drop odds** read `targetPlayer`'s health ratio, with a `047` `TODO` (whose health counts
   is a co-op rule).
6. **`ItemEntity.contactWithPlayer(player: MegaPlayer)`.** The items put `player.slot` in their event
   props. `PlayerItemRule` uses `asPlayer` and loses its cast and `TODO`.
7. **Tests:** the lives calls (shared count, the slot is ignored, bounds, `livesExhausted`, reset); the
   owned-weapon set (reward table on level add and remove, override, reset, listener calls, the buster);
   the save string is the same as before for the same data; `addToHealthTanks` moves with
   `MegamanHealthTankTest` (renamed for `GameState`); `PlayerItemRule` passes the view, if practical.

New APIs and their first callers:

| API | First callers |
|---|---|
| `GameState.getLives`, `addLife`, `removeLife`, `livesExhausted`, `resetLives`, `setLives` | `LevelPauseScreen`, `Life`, `AbstractEnemy`, `MegaLevelScreen`, `MainMenuScreen`, `MegamanMaverickGame` |
| `GameState` weapon set (`containsWeapon`, `addWeapon`) and the reward table | `LevelPauseScreen`, `PlayerStatsHandler`, `MegamanJetpackHandler`, the level override, `addLevelDefeated` |
| `IGameStateListener.onAddWeapon`, `onRemoveWeapon` | `Megaman` |
| `GameState.addToHealthTanks`, `hasAnyHealthTanks` | `PlayerStatsHandler`, `LevelPauseScreen` |
| `MegaPlayer.healthRatio` | `AbstractEnemy` (`HealthBulb` drop odds) |

## Out of scope

- The save format, health-tank fill in the save, saving lives (`007`).
- `LivesPolicy` and per-player lives, game over with more players, late join (`047`).
- Coins and level points (per-level game properties), difficulty reads.
- The level screens' use of `megaman` for health, ammo, current weapon and the stats handler (`029f`).
- The weapon handler and loadout per character (`024`).
- The `SLOT` prop on the item events is still not read by `MegaLevelScreen` (`029f`).

## Affected areas and risks

- **The save string.** Lives and weapons must stay out of `toString`; `fromString` must fill the weapons
  through `addLevelDefeated` as today. A test compares the string.
- **Listener order.** Today the weapons arrive in the handler inside `onAddLevelDefeated`. With the set,
  they arrive through `onAddWeapon` in the same call. The pause screen, the stats handler and
  `GetWeaponScreen` must see the same weapons at the same time.
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
