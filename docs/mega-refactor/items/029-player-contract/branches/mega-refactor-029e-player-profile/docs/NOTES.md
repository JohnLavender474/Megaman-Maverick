# Notes: mega-refactor-029e-player-profile

## Decisions

## Overlap with other branches

No other `029` branch is active. The active items `001`, `008` and `019` do not touch the profile code.
Item `007` (save slots) is planned and reads the same data: this branch must not change the save string.

## Scan (2026-10-23, on `b3bc97f80`)

Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Where the profile data is today

`GameState` (`state/GameState.kt`) is already a session profile for most of the data. It is one object on
the game, it is the source of the save string (`toString`/`fromString`), and it tells its listeners about
each change (`IGameStateListener`).

| Data | Where | Saved | Who changes it |
|---|---|---|---|
| Levels defeated | `GameState` | yes | `MegaLevelScreen:859`, load, reset |
| Heart tanks | `GameState` | yes | `PlayerStatsHandler:133`, level override, load, reset |
| Health tanks and their fill | `GameState` | the tanks yes, the fill no (load puts 0) | `PlayerStatsHandler:166`, `Megaman.addToHealthTanks`, `LevelPauseScreen:586` |
| Enhancements | `GameState` | yes | level override, load, reset (no pickup exists) |
| Currency (screws) | `GameState` | yes | `MegaLevelScreen:664` (`ADD_CURRENCY`) |
| Difficulty | `GameState` | yes | `SelectDifficultyScreen`, level override |
| Lives | `Megaman.lives` (`Megaman:189`), a `Points` 0..9, start 3 | no (load resets to 3) | `MegaLevelScreen:622, 644`, `MainMenuScreen:135`, `MegamanMaverickGame:774, 791, 833`, `Life:52`, `AbstractEnemy:335` |
| Weapons owned | keys of `MegamanWeaponsHandler.weaponHandlers` (per entity, with the ammo) | no: derived from the levels defeated | `Megaman.onAddLevelDefeated` (reward table `Megaman.getWeaponsFromLevelDef`), level override (`putWeapon`), `clearWeapons` on reset, `MEGA_BUSTER` in `init` |
| Weapons just won | game property `WEAPONS_ATTAINED` | no | set by `Megaman.onAddLevelDefeated`; read and removed by `GetWeaponScreen`; removed by reset and the main menu |
| Coins, level points | game properties `COINS`, `LEVEL_POINTS` | no | per level (`Coin`, `QuestionBlock`, `AbstractEnemy`) |

So the data that is still on the player entity is: lives, and the weapons owned.

### What the player does with the profile

`Megaman` is the only `IGameStateListener` (registered once in `MegamanMaverickGame:401`):

- `onAddLevelDefeated`: puts the reward weapons in its handler and sets `WEAPONS_ATTAINED`.
- `onRemoveLevelDefeated`: removes them.
- `onAdd/RemoveHeartTank`: changes its max health by `HEALTH_BUMP`.
- `onAddCurrency`: plays the pickup sound.

`Megaman` also wraps profile reads: `hasEnhancement`, `hasHeartTank`, `hasHealthTank`, `hasAnyHealthTanks`,
`addToHealthTanks` (fill order: the fullest tank first). Inside the player, `hasEnhancement` is read in
`BehaviorsComponent:139, 197, 204, 492`, `ControllerComponent:65` and `editDamageFrom` (`Megaman:754`).

### Reads from outside the player

- Lives: `MegaLevelScreen:622` (`removeOneLife`), `:642-644` (`isAtMinLives`, game over, `resetLives`),
  `MainMenuScreen:135`, `MegamanMaverickGame:774, 791, 833`, `LevelPauseScreen:229` (display),
  `AbstractEnemy:100` (drop odds), `:335` (one-up), `Life:52`.
- Tanks: `HealthTank:62` and `HeartTank:99` (spawn only if not owned), `ReactorMonkey:156`,
  `PlayerStatsHandler:111, 150, 269-271`, `LevelPauseScreen:290, 407, 621, 631`.
- Enhancements: `AbstractEnemy:249`, `AbstractBoss:200` (damage scaling, with `029e` `TODO`s).
- Weapons owned: `LevelPauseScreen:265, 390`, `PlayerStatsHandler:203, 222`, `MegamanJetpackHandler:32`.
- `029e` `TODO`s in code: `AbstractEnemy:95, 246, 333`, `AbstractBoss:197`, `ReactorMonkey:155`.

The screens (`PlayerStatsHandler`, `LevelPauseScreen`, `MegaLevelScreen`, `MainMenuScreen`) belong to `029f`.

### Items and `PlayerItemRule`

- `ItemEntity.contactWithPlayer(megaman: Megaman)` has 7 implementers. `PlayerItemRule:19` checks
  `playerEntity is Megaman` (with a `029` `TODO`).
- Only `Life` uses a member of `Megaman` (`lives`). `HealthBulb`, `HealthTank`, `HeartTank`, `Screw` and
  `WeaponEnergyBulb` use the argument only for `players.slotOf(megaman)!!` in the event props. `Coin` does
  not use it.
- `MegaLevelScreen:652-675` reads the item events but not their `SLOT`; one `PlayerStatsHandler` acts on
  Megaman (`029f`).

### Problems

1. Lives are on the body. A second player has its own `lives`, so the shared-pool rule cannot be set in one
   place, and the drop odds and the one-up read only the global `megaman`.
2. Weapons owned are on the body, but the rule that fills them (levels defeated, level override) is a
   session rule. A player that registers after the levels are defeated gets no weapons, because the listener
   events already happened.
3. The heart tank max-health bump has the same problem: it is applied by a listener event. A player that
   registers later keeps the base max health.
4. Profile reads go through the player (`megaman.hasHeartTank`), so code that only needs the session data
   must find a player first.
5. `ItemEntity` is typed to `Megaman`, so a second character cannot pick up items.

### Facts for the scope

- With one shared session profile (`007` decision 2026-10-23: "the save holds one session profile"),
  the enhancement and tank reads in `AbstractEnemy`, `AbstractBoss`, `ReactorMonkey`, `HealthTank` and
  `HeartTank` become direct reads of `game.state`. The "owner's profile" question in their `TODO`s goes
  away: there is one profile.
- The save string must not change. Lives are not saved today, and load resets them to 3.
- `029` decision H (2026-10-22) asks for a `LivesPolicy` interface (`SharedPool`, `PerPlayer`). Only the
  shared pool has a caller now (capacity 1). The "production caller" rule says to write the interface when
  `047` needs `PerPlayer`.
- `GameStateExtensions.hardMode` and the other difficulty reads (about 60) are a session setting, not
  player data. They do not change.
- `RefactorTest029.tmx` room `pickups` has `HealthBulb`, `Screw`, `WeaponEnergyBulb`, `HealthTank` and
  `HeartTank`, but no `Life`. `Life` is in `IntroStage_v3`, `ReactorMan`, `TimberWoman_16x14_v2` and
  `WilyStage3_v4`.
- The one-up comes from a chain of kills (`AbstractEnemy:310-321`); no map places it.
- Tests: `MegamanHealthTankTest` covers `addToHealthTanks`. No test covers lives or the listener.

### Open questions for the owner

1. **Where the profile lives.** Recommendation: `GameState` stays the session profile, and lives move into
   it (not saved, so the save string does not change). No new `PlayerProfile` class, because no caller needs
   a type separate from `GameState`.
2. **Lives policy.** Recommendation: one shared count in `GameState` now, with a `TODO` at the count for
   `PerPlayer` (`047`). No `LivesPolicy` interface until `047` (production-caller rule). This changes how
   decision H is done, not what it says.
3. **Weapons owned.** Options:
   - (a) A set of owned weapons in `GameState` (not saved; the reward table fills it from the levels
     defeated, and the level override adds to it). Each player puts the set into its handler at
     registration and on change. The reward table moves out of the `Megaman` companion.
   - (b) Leave the weapons owned in `MegamanWeaponsHandler` until `024` (loadout per character), with a
     `TODO`.
   Recommendation: (a), because the screens (`029f`) then read one set, and a later player gets the weapons.
   The ammo stays per player in the handler.
4. **Effects at registration.** A player that registers after progress exists misses the heart tank bumps
   and the weapon events. With capacity 1 this cannot happen. Recommendation: a `TODO` at the registration
   (`047`), no code.
5. **Drop odds.** The `Life` odds read lives (shared: becomes `game.state`). The `HealthBulb` odds read the
   health ratio of the global `megaman`. Recommendation: read the `targetPlayer`'s health, with the `047`
   `TODO` (which player's health counts is a co-op rule).
6. **`ItemEntity`.** Recommendation: `contactWithPlayer(player: MegaPlayer)`; the items use `player.slot`;
   `Life` adds to the shared lives; `PlayerItemRule` uses `asPlayer` and loses its cast and `TODO`.
7. **The wrappers on `Megaman`** (`hasEnhancement`, `hasHeartTank`, `hasHealthTank`, `hasAnyHealthTanks`,
   `addToHealthTanks`, `lives`, `removeOneLife`, `isAtMinLives`, `resetLives`). Recommendation: move the
   logic to `GameState` (for example `addToHealthTanks` and the lives calls) and change every caller,
   including the `029f` screens, in this branch, so the screens no longer reach the profile through
   `megaman`. The screens keep `megaman` for the health, ammo and current weapon until `029f`.
