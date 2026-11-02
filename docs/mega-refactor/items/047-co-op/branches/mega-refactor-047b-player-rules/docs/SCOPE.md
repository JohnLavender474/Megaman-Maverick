# Scope: mega-refactor-047b-player-rules

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes, scope approved")

## Goal

With `--players 2`, the two players act as a team or as rivals, as the rules say: shots pass through a teammate in co-op and hit in PvP, enemies go for the nearest live player, a shot faces the way of its shooter, and health and weapon energy go to the player who collects them.

Without `--players 2`, no behavior changes.

## Item

Item `047`. The row of the branch split: "Session rules, friendly fire, targeting, `ChargedShot` and pickups".

Decisions in `ITEM.md` that this branch takes:

- 2026-11-01: for now, friendly fire is off, and shots pass through teammates.
- 2026-11-01: gameplay rules such as friendly fire are a suite of toggles, and each toggle has a value for single player, co-op and PvP. `047b` builds a small rules object now, and `102` extends it later.
- 2026-11-01: for now, only CLI flags choose the rules. `--mode coop|pvp` chooses them, `--players 2` defaults to co-op, and either mode with 1 player is a launch error.
- 2026-11-01: for the first pass, an enemy chooses the nearest live player on each read. The owner expects this rule to need much more refinement later.
- 2026-11-01: health and weapon energy go to the player who collects them. Lives, currency, heart tanks and health tanks stay shared.

## Current state

Line numbers are from `master` at `5a82179f6`. All paths are under `core/src/main/kotlin/com/megaman/maverick/game/`. The scan is in the item `NOTES.md`, "`047b` scan". A code review added the facts on targets below. Nothing below comes from a run.

### Friendly fire

- `Megaman.canBeDamagedBy` refuses only the player's own shots (`entities/megaman/Megaman.kt:426-432`). The owner check goes 2 levels deep.
- `Bullet.kt:142` and `ChargedShot.kt:145-148` explode when a target refuses damage. So a change to `canBeDamagedBy` alone makes shots explode on a teammate.
- A teammate's shield deflects a shot (`Bullet.kt:156`). `Megaman` also has a needle spin `DAMAGER` fixture (`entities/megaman/components/BodyComponent.kt:257`).
- `ProjectileRule` is an `object` (`world/contacts/rules/ProjectileRules.kt:21`), and `megaContactRules` builds the table (`world/contacts/MegaContactRules.kt:21,71`). `create()` resolves the launch options (`MegamanMaverickGame.kt:278`) before `createGameEngine()` (`:331`), so the rules exist in time.

### Targeting

- 613 `targetPlayer` lines in 145 entity files go through `MegaPlayers.targetFor` (`players/MegaPlayers.kt:36-39`), which ignores the seeker.
- 8 production seekers have no body: `SwimBooster`, `DrWily`, `DarknessV4`, `InfernoMeteorShower`, `WilyCapsuleTentacle`, `RainFall`, `Snow`, `FallingLeaf`. `.body` throws on these.
- Live tests: a dead slot 1 stays `IN_PLAY`, and a player in the beam-in is `LIVE` but `PRE_PLAY`. So a live player is `existenceState.liveNow && inPlay` (precedent: `LaserBeamer.kt:248`).
- With no live player, `targetFor` must still give a player, because readers use `.body` at once. A destroyed player keeps its body. `Lift.kt:119` and `MoonMan.kt:526` read a dead player on purpose.
- `MegaPlayersTest.kt:87-101` pins the old rule. `SpiderWebDestroyTest.kt:44` and `DrWilyTest.kt:59` mock `targetFor`.

The 23 files in `blocks`, `sensors`, `special`, `hazards` and `projectiles` have 41 lines. The review sorts them:

| Result of the review | Sites | Action |
|---|---|---|
| Nearest is correct, or the seeker has no body and gets the first live player | `DrWily`, `DarknessV4`, `InfernoMeteorShower`, `PipiEgg`, `HomingMissile`, `RocketBomb`, `CactusMissile`, `SeaMine`, `WilyCapsuleTentacle`, `TubeBeamer`, `LaserBeamer`, `BulletBillLauncher`, `WanaanLauncher`, `RisingLavaRiver` | none |
| A test that must be true for any player | `GroundSlideBlocker.kt:46,47`, `Lift.kt:119`, `Gate.kt:348`, `FallingIcicle.kt:168-170` | use `players.any` |
| Must act on each player | `SwimBooster.kt:60,61` | use `players.forEach` |
| Means the shooter | `ChargedShot.kt:118,119` | read the owner |
| Means the stuck player | `SpiderWeb.kt:125,153,156,191,192` | store the player |

`SpiderWeb` is the worst case: with a nearest default, it can lock one player and unlock the other.

`BigAssMaverickRobot.kt:489-491` throws if both hands report that a player stands on them. Today only slot 0 can be on one hand. With 2 players, one on each hand, a nearest default or `players.any` reaches the throw.

The 97 enemy files (320 lines) have no review yet. A search found no `targetPlayer.lock` or similar write in them. The plan has a review step for the same stored-state fault.

### Pickups

Each item sends the slot. `MegaLevelScreen.kt:621-644` drops it, and `PlayerStatsHandler.kt:58` acts on slot 0. Lives and currency are one shared pool.

## In scope

1. **`--mode` flag.** `coop` or `pvp`. A launch error for a bad value and for either mode with 1 player (owner approved, 2026-11-01, with the contract).
2. **Session rules.** `GameMode` (`SINGLE`, `COOP`, `PVP`) and a typed, read-only `SessionRules`, with one field: `friendlyFire`. `SINGLE` and `COOP` have it off, and `PVP` has it on.
3. **Friendly fire off.** With the toggle off, a projectile from one player ignores the `BODY` and `SHIELD` fixtures of another player, and no damage passes between players. This includes the needle spin. With it on, today's behavior stays.
4. **Targeting.** `targetFor` gives the nearest live player to the seeker. A seeker with no body gets the first live player in slot order. With no live player, it gives what it gives today. A tie goes to the lower slot.
5. **Site fixes** from the table: 4 tests become `players.any`, `SwimBooster` acts on each player, `SpiderWeb` stores the stuck player.
6. **`BigAssMaverickRobot`.** With both hands stood on, the boss skips the launch and does not throw. `isBeingStoodUpon` tests any player.
7. **`ChargedShot`** reads a player owner, and it keeps the target read if the owner is not a player.
8. **Pickups.** `ADD_PLAYER_HEALTH` and `ADD_WEAPON_ENERGY` act on the slot in the event.
9. **Review of the 320 enemy lines** for stored-state faults like `SpiderWeb`. A fault that a nearest default causes gets a fix here. Any other fault goes in `docs/TODO.md`.
10. **Test rooms** in `RefactorTest047.tmx`.
11. Root `CLAUDE.md`: the `--mode` flag and the rules object.

### New APIs and their first callers

| New API | First caller |
|---|---|
| `LaunchOptions.mode`, `DesktopAppArgs.mode`, `ResolvedLaunch.mode` | `resolveLaunchOptions`, `create()` |
| `GameMode`, `SessionRules`, `MegaServices.rules` | `Megaman.canBeDamagedBy` |
| A `rules` value on `megaContactRules`; `ProjectileRule` becomes a class | `createGameEngine()` |
| A function that gives the player at the root of an owner chain | `Megaman.canBeDamagedBy`, `SessionRules.isFriendlyFire` |
| `SessionRules.isFriendlyFire` | `ProjectileRule`, `PreciousGem.canBeDamagedBy` |
| `MegaPlayers.isLive`, `firstLive`, `anyLive`, `forEachLive` | `targetFor` and the site fixes of this branch |
| A player value on `addHealth` and `addWeaponEnergy` | `MegaLevelScreen.onEvent` |

## Out of scope

- `TargetPolicy`, `TargetingComponent`, `Sticky`, and the removal of `targetPlayer` from entities with no target (`112`).
- A boss that changes target in an attack, and parts that choose their own target (`047e`, with the first branch of `112`).
- The toggles for lives, death and respawn (`047c`), and all other toggles.
- Teams, a menu, or a mode that a level sets (`102`, `109`).
- PvP damage values, score and match rules (`109`, `023`, `024`).
- `Faction` and the explosion owner rule (`030`).
- The HUD for slot 1 (`047d`).
- The drop odds and the one-up at `AbstractEnemy.kt:104-110,331`, which read the target.
- The one shared set of webs (`SpiderWeb.kt:75`), and the `SpiderWeb` destroy on any death.
- The quicksand fix (`docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`).

## Affected areas and risks

- Single-player play: `targetFor`, `ProjectileRule` and the pickup path are hot or common paths. With 1 player, each must give today's result. `targetFor` must not read a position with only one live player.
- `ProjectileRule` changes from `object` to `class`, so `ContactRuleSelectionTest` can need a change.
- `targetFor` runs often. `.body` is one component lookup, and `getCenter()` uses a pool. `O5-R2` asks for a measure before and after.
- A target can flip between 2 players at almost equal distance. The owner accepted this for the first pass.
- Bosses and their parts can choose different players. `GutsTank`, `SigmaRat`, `WilyFinalBoss`, `PreciousWoman`, `TimberWoman` and `InfernoMan` read the target again in an attack. These go in "Known wrong" for `047e`.
- `GroundSlideBlocker` is one global state, so a slide by one player also turns it off for the other. This stays, as a known-wrong entry.
- A heart tank is shared, but `PlayerStatsHandler.attain` acts on slot 0. The plan must confirm how slot 1 reads its maximum health.

## Overlap with other branches

No other `mega-refactor-` branch is active. The `compactor-level` worktree changes `CeilingCrusher.kt`, `LevelDefinition.kt` and `LevelCompletionScreensTest.kt`. This branch can change the last 2 if the test rooms need a new entry; they do not, because the rooms go in `REFACTOR_TEST_047`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | The rules reach entities through `MegaServices`. |
| `O1-R2` Components, not new inheritance | no effect | No new class tree. |
| `O1-R3` No new fragile order | no effect | The rules exist before `createGameEngine()`, and they do not change. |
| `O1-R4` Typed state | helps | A typed field, with no property key. |
| `O1-R5` Code only with a caller | helps | Each new API has a caller. The component waits for one. |
| `O2-R1` Input only through `InputFrame` | no effect | `SpiderWeb` reads the stored player's input. |
| `O2-R4` Stable order, players in slot order | helps | A tie goes to the lower slot. |
| `O2-R5` No logic assumes one player | helps | `targetFor` uses the seeker, and the site fixes remove assumptions. |
| `O2-R6` No logic assumes one camera | no effect | |
| `O2-R7` Gameplay does not depend on rendering or audio | no effect | |
| `O2-R8` Local-only data stays out of the simulation | helps | The rules are set once at session start. |
| `O2-R9` Events and progress name a slot | helps | The pickup slot is no longer dropped. |
| `O5-R1` No per-frame allocation | no effect | `targetFor` must not allocate. |
| `O5-R2` Measure hot paths | no effect | A measure with `--diagnostics`, before and after. |
| `O6-R1`, `O6-R2` | no effect | CI and named checks, as usual. |
| `O7-R3` No level-identity checks | no effect | |
| `O8-R2` Capabilities, not character checks | no effect | The owner chain reads `asPlayer`. |
| `O9-R1` Playable with an NES-style pad | no effect | |

No conflicts.

## Open questions

None. The component moved to item `112` (owner, 2026-11-01). The code review showed that `Sticky(Nearest)` on `AbstractBoss` does not work alone, because the boss parts are not `AbstractBoss` subclasses. The `TODO` comments at `MegaPlayers.targetFor` and `targetPlayer` change to name `112`.
