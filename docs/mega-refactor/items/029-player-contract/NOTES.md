# Notes: 029 `IPlayer` contract, players service and `CharacterDef`

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design, number checks (2026-10-22)

````text
REVIEW: players, input and entity design (items 12+14, 13, 23, 28, 29, 30, 41)
Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` (C/) or `engine/src/main/kotlin/com/mega/game/engine/` (E/), unless the path is given in full. I did not write any files.

NUMBER CHECKS (method: grep on non-import lines)
- Item 28: 889 `megaman` word uses in 224 files outside `entities/megaman/`. The list says 1096 in 235. The difference comes from the count method. The scale is the same.
- Item 29: 70 `owner ==/!= megaman` / `== megaman` checks (confirmed). 37 `is Megaman` (list says 38).
- Item 30: 55 `game.controllerPoller.is*` calls plus 8 `poller.isPressed` calls in Megaman's actuators, so 63 in total. 30 of them are in `entities/megaman/components/BehaviorsComponent.kt`. Index 0 is hard-coded at `E/controller/ControllerUtils.kt:11,23`.
- Item 13: 77 rules (begin 34, continue 26, end 17), confirmed. The "48 megaman refs" number is wrong. It counts import and package text. The real code has 16 refs: 14 `is Megaman` and 2 `megaman`. Fix this number in the backlog.
- Item 14: 117 `enum class *State`. `StateMachine` or the builder is used in 25 files and `Loop(` in 41 files. Two earlier helpers exist and are used by 4 enemies: `C/entities/utils/StateLoopHandler.kt` and `StateMachineHandler.kt`.
````

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 28: `IPlayer` CONTRACT AND PLAYERS SERVICE".

````text
1. Current state
- `game.megaman` is a `lateinit` field (`MegamanMaverickGame.kt:204, 372`), reached everywhere through `IMegaGameEntity.megaman` (`entities/contracts/IMegaGameEntity.kt:22`).
- `Megaman.kt` (1045 lines) implements 15 interfaces (`:71-73`). It mixes character physics (the direction setter copies about 10 gravity/velocity values from `MegamanValues` by sign, `:239-269`; `MegamanValues` has 86 constants), the damage reaction (`:660-707`), progress and profile data (lives `:175`, health and heart tanks `:957-988`, rewards `:79-93, 917-926`), and weapons.
- Uses outside the player package, grouped by member (dotted access only):

| member | count | goes to |
|---|---|---|
| `body` | 524 (`getX` 180, `getCenter` 136, `getBounds` 72, `getY` 46, `getMaxX` 26, `velocity` 19, `getMaxY` 13) | core |
| `direction` | 58 | core |
| `dead` | 22 | core |
| `ready` | 14 | core |
| `feetFixture` / `left/rightSideFixture` / `headFixture` / `damageableFixture` | 10 / 7+7 / 3 / 2 | core |
| `isFacing` / `facing` | 7 / 4 | core |
| `isBehaviorActive` / `isAnyBehaviorActive` | 7 / 1 (CLIMBING, WALL_SLIDING, JETPACKING) | core |
| `canBeDamaged` | 6 (spawn/end handlers, Wily, `MegaLevelScreen`) | core |
| `setAllBehaviorsAllowed` | 3 | core |
| health getters/setters | about 12 | core |
| `getFocusPosition` | 1 | core |
| `currentWeapon` | 7 | loadout |
| `weaponsHandler` | 5 (HUD, pause) | loadout |
| `charging` / `fullyCharged` / `ammo` | 4 / 2 / 1 | loadout |
| `lives` / `resetLives` / `removeOneLife` | 5 / 4 / 1 | profile |
| `hasHealthTank` / `hasHeartTank` / `addToHealthTanks` / `hasEnhancement` | 5 / 3 / 1 / 2 | profile |
| `aButtonTask` | 2 (GravitySwitcharoo, GravityCapsule) | Mega-only |
| `canMakeLandSound`, `teleporting`, `shooting`, `running` | 1 each | Mega-only |

  Conclusion: the external need is about 90% "where is the player, and what is the player doing". The minimal contract is small.
- Other single-player couplings: `PlayerStatsHandler(megaman)` (`MegaLevelScreen.kt:249`) and `cameraManagerForRooms.focus = megaman` (`:266`).
- X6 facts (`mmx-demake-tools/docs/players/map.md`, "Code layout"; `movement.md`, "Physics constants"):
  - X and Zero share `PlayerUpdate` and 32 step routines.
  - Character code is in overlays, called at fixed addresses under a test of `mega+0x02` (0 = X).
  - Character-only steps are in an extended table (X 32-47/74-77, Zero 48-63).
  - Physics values are fields on the player, written at beam-in. Armor-part bonuses are then added. The base values are the same for X and Zero in the states read.
  - Air dash is X-only in form 4 and always available for Zero.
  So X6 uses a shared core, character hooks, and data values with modifiers.

2. Options
A. Extract the interface only: `Megaman : IPlayer`. A new character subclasses `Megaman`. Pro: cheap. Con: Proto Man inherits all of Mega's internals.
B. `IPlayer` plus an `AbstractPlayer` base for shared core work (health/damage flow, spawn/room-transition events, focus, control locks). Each character is configured by a `CharacterDef`: a movement values table, an ordered ability list (slide, wall jump, dash, charge, climb, swim, jetpack as behavior factories), a body/fixture def, animations, a loadout and a damage negotiator.
C. Full composition from data: no `Megaman` class, and the player is built from components by a data file. Pro: most flexible. Con: a big-bang change, and it is hard to keep behavior the same.

3. Recommendation
B, reached through A as the first step.

Minimal interface (from the counts):
```
interface IPlayer : IMegaGameEntity, IBodyEntity, IHealthEntity, IDamageable, IFaceable, IDirectional, IFocusable {
    val playerIndex: Int
    val character: CharacterDef
    val faction: Faction                     // PLAYER (item 29)
    val dead: Boolean
    val ready: Boolean
    val feet: IFixture; val head: IFixture; fun side(left: Boolean): IFixture
    fun isDoing(action: PlayerAction): Boolean   // CLIMBING, WALL_SLIDING, JETPACKING...; false if the ability is absent
    fun lock(reason: Any); fun unlock(reason: Any)   // replaces canBeDamaged/setAllBehaviorsAllowed from handlers (item 11)
    val loadout: ILoadout                    // currentWeapon, ammo, charge status (item 23)
    val input: InputFrame                    // item 30
    fun onGravityChanged(dir: Direction)     // replaces external aButtonTask writes
}
interface IPlayers {
    fun all(): Iterable<IPlayer>
    fun byIndex(i: Int): IPlayer?
    fun nearestTo(p: Vector2, aliveOnly: Boolean = true): IPlayer?
    val primary: IPlayer
    fun anyAlive(): Boolean
}
```
- Lives, tanks and enhancements move to a `PlayerProfile` in game state, not on the character. Owner decision: one profile shared in co-op, or one per slot.
- Mega Man as the first implementation with no behavior change:
  - `Megaman : AbstractPlayer, IPlayer` with `MegamanDef`.
  - `MovementValues` is filled with the exact `MegamanValues` constants, with `forDirection(dir)` for the sign flip.
  - The ability list is the 8 current behaviors in the current order (`BehaviorsComponent.kt:778-785`), because evaluation order matters.
  - `game.players` holds one player.
  - Enemy code switches from `megaman` to `player` = `game.players.nearestTo(body center)`. With one player this is the same object, so behavior does not change.
  - Keep `megaman` as a deprecated typed alias during the migration.
  - Cross-ability couplings (NEEDLE_SPIN or RUSH_JET checks inside behaviors) become weapon-driven ability blocks and modifiers (item 23).
- Prove each step with a replay trace (item 5).

4. Open questions for the owner
- In co-op, one shared life pool, or one per player?
- Target selection: is "nearest" enough, or do some enemies need "last attacker" or "random"?
- Must camera focus follow one player or a group bounds?
- Do all characters share sliding/ladder rules, as in X6 where the steps are shared, with only values and abilities that change?

5. Backlog wording
Replace "Remove the global `megaman`" with: "`IPlayer` contract, players service and `CharacterDef`"
- `IPlayer` (body, facing, direction, dead/ready, health/damage, faction, focus, `isDoing`, locks, loadout, input). `Megaman` implements it.
- `game.players` (`all`, `byIndex`, `nearestTo`, `primary`). Migrate the 889 external `megaman` uses in 224 files.
- Move lives, tanks and enhancements to a `PlayerProfile`.
- `CharacterDef`: movement table, ordered ability set, body def, animations, loadout. `MegamanDef` keeps behavior identical (replay-proven).
- A prototype second character (Proto Man) proves the contract.

6. Dependencies
Item 29 (faction), 30 (input per player), 23 (loadout), 11 (lock reasons), 15 (`LevelContext` gives `players`), 13 (contact receivers), 5 (replay proof), 6 (profile save), 36/37 (respawn per player).
````

## From the old `BACKLOG.md` (2026-10-22)

`megaman` is reached globally through `game.megaman` (for example in `MegaLevelScreen`,
`AbstractEnemy` and `AbstractBoss`). This blocks other playable characters and multiplayer.
