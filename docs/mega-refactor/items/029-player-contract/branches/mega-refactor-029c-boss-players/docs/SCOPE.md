# Scope: mega-refactor-029c-boss-players

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Every boss in `entities/bosses/`, `AbstractBoss` and `AbstractEnemy` reach the player through the players service instead of the global `megaman`, with the same intent rules as `029b`. The boss damage listener works with the player view instead of the `Megaman` type. The profile reads in `AbstractEnemy` (drop odds, the one-up) keep the global with a `TODO` for `029e`. With one player the game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029c` `boss-players`: Bosses (about 140 lines, 22 files), `AbstractBoss`, the boss damage listener (`IMegamanDamageListener`)".
- Owner decisions (2026-10-23): `AbstractEnemy` and `AbstractBoss` go to `029c` and `029d`; this branch takes both. "Put a `TODO` for the drop logic in `AbstractEnemy`."
- The `029b` rules: call sites name their intent (`targetPlayer`, `players.any`, `players.viewOf`, `players.forEach`); shared player state goes in `PlayerStateComponent`; no API without a real caller.

## Current state

Scan on `01d0947e7`. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Counts

155 lines in 24 files (22 bosses, `AbstractBoss`, `AbstractEnemy`), plus `getMegaman()` twice in `SphinxMiniBoss`. Largest: `WilyFinalBoss` 15, `PreciousWoman` 12, `AbstractEnemy` 10.

| Intent | Examples |
|---|---|
| Position, aim, facing (about 105 lines) | `megaman.body.getCenter` 47, `getX` 27, `getY`, `getMaxX`, `getBounds` 7 each |
| State reads | `direction` 6, `dead` 3, `facing` 3, `damaged` 2 (`WilyFinalBoss:719, 731`), `invincible` (`TimberWoman:727`), `body.isSensing(FEET_ON_GROUND)` 4 (intro checks: `GutsTank:273, 323`, `ElecDevil:333`, `WilyFinalBoss:432`), feet and damageable fixtures (`BigAssMaverickRobotHand:246`, `TimberWoman:729`), `shooting` (`AbstractEnemy:287`) |
| Identity | `damageable == megaman` / `is Megaman` (`GutsTankFist:165`, `GutsTank:415, 477, 509`) |
| Ownership | `owner != megaman` (`GutsTankFist:94`, `ReactorMan:283`), `owner == megaman` (`AbstractEnemy:243, 255`, `AbstractBoss:193`) |
| Writes to the player | `WilyFinalBoss:294-295` (`canBeDamaged = false`, `facing`, then `TURN_CONTROLLER_OFF`) at the final defeat; `MoonMan:522` (`if (!megaman.dead) megaman.direction = ...`, the gravity change) |
| Damage listener | `AbstractBoss` implements `IMegamanDamageListener` (`onMegamanDamaged(damager, megaman: Megaman)`), registers with `megaman.addDamageListener(this)` in `onSpawn` (`:130`) and removes itself in `onDestroy` (`:159`). Only `PreciousWoman` overrides it (laughs when Megaman is hurt). `Megaman` keeps an `OrderedSet` of listeners and calls them in `takeDamageFrom` (`Megaman.kt:720`). |
| Profile | `AbstractEnemy` drop odds (`:96` lives, `:102-103` health ratio), the one-up (`:329` `lives.translate(1)`), `hasEnhancement(DAMAGE_INCREASE)` (`AbstractEnemy:243`, `AbstractBoss:193`), `hasHeartTank` (`ReactorMonkey:154`) |
| Damage-class sets | `Megaman::class` in `PreciousWoman:506, 563, 908` |
| Comments and logs | about 10 |

### Other findings

- `Megaman.shooting` is computed from `shootAnimTimer` (`Megaman.kt:199-203`), like `damaged` from the damage timer. Only `Met` calls `isMegamanShootingAtMe`.
- `Megaman.canBeDamaged` is a plain field (`:130`), set back to `true` on spawn (`:464`). 42 lines outside the player package write or read it (mostly handlers, `029f`).
- `ReactorMan_OLD` and `MechaDragon_OLD` are only pooled by the deprecated `BossesFactory`; `SphinxMiniBoss` is only in old `DesertMan` map versions. They still read the global, so they must change too before `029f` removes the extension.
- Bosses spawn only through the boss flow in `MegaLevelScreen` (boss rooms, intro, health fill), so their checks run in real levels, not in the test map.

## In scope

1. **`PlayerStateComponent`** gets the other shared player state that the moved sites need:
   - `shooting` (read from the shoot animation timer, as `Megaman.shooting` reads it today);
   - `canBeDamaged` (a field; `Megaman.canBeDamaged` reads and writes it, so the 42 other call sites do not change);
   - the damage listeners (see item 3).
2. **`MegaPlayer` accessors** for the moved sites: `facing` (the entity must be an `IFaceable`), `invincible` (an `IDamageable`), `shooting`, `canBeDamaged`, `isSensing(sense)` through the body; and write access for the two boss writes: `direction` and `facing` become `var` (through `IDirectional` and `IFaceable`), `canBeDamaged` gets a setter (through the component).
3. **Damage listener:** the interface is retyped to the view: `IPlayerDamageListener.onPlayerDamaged(damager, player: MegaPlayer)` (it replaces `IMegamanDamageListener`). The listener set moves into `PlayerStateComponent`; `Megaman` notifies it from `takeDamageFrom` with its view (`players.viewOf(this)`). `AbstractBoss` adds itself to every registered player in `onSpawn` and removes itself in `onDestroy`, with a `TODO` that a player who joins during a boss fight is not listened to (`047`).
4. **Bosses, `AbstractBoss`, `AbstractEnemy`** by the `029b` intent rules:
   - position, aim, facing and state reads: `targetPlayer` (`SphinxMiniBoss` `getMegaman()` too);
   - identity: `players.viewOf(damageable) != null`;
   - ownership: `players.viewOf(owner)` (the owner part of the enhancement checks too);
   - `isMegamanShootingAtMe`: `players.any { ... }` with the shooting and facing check on the same player (the method name stays);
   - the floating points direction in `AbstractEnemy`: `targetPlayer.direction`;
   - the writes: `WilyFinalBoss` (final defeat) and `MoonMan` (gravity change) change every registered player with `players.forEach`, each with today's checks (`MoonMan` skips a dead player);
   - the intro checks that wait for the player to land (`GutsTank`, `ElecDevil`, `WilyFinalBoss`): `targetPlayer` (see open question 3).
5. **Profile reads keep the global, each with a `TODO` that names `029e`:** the `AbstractEnemy` drop odds and the one-up (owner's request), `hasEnhancement` in `AbstractEnemy` and `AbstractBoss`, `hasHeartTank` in `ReactorMonkey`.
6. **Tests:** the new `PlayerStateComponent` parts and view accessors and setters; `Megaman.canBeDamaged` reads and writes the component; a damage listener added to the component is called with the view when `Megaman` takes damage (if practical with the relaxed game mock).

## Out of scope

- The damage-class sets (`Megaman::class` in `PreciousWoman`): `030` or `023`.
- The other `canBeDamaged` users (handlers, screens, hazards): `029d` and `029f`.
- Renames of methods with "Megaman" in the name (`isMegamanShootingAtMe`, `findBeamerOverMegaman`, `targetMegaman`).
- Co-op rules for bosses (a target per player, a boss that listens to players who join later): `047`.

## Affected areas and risks

- **24 files.** Mechanical apart from the writes, the damage listener and the intent choices. With one player, `targetPlayer`, `any`, `forEach` and `viewOf` all give Megaman.
- **Damage listener timing:** the listener must be called at the same point in `takeDamageFrom` and with the same player as before; the boss must add and remove itself at the same points.
- **The writes at the Wily final defeat** happen in `onDestroy`; `players.forEach` must visit Megaman also while he is not present (it does: `forEach` does not filter).
- **Boss checks need real levels.** Each changed boss is checked in its own stage; the plan lists them. `WilyFinalBoss` is the hardest to reach.
- **Old bosses** (`ReactorMan_OLD`, `MechaDragon_OLD`, `SphinxMiniBoss`) are not in live maps; they are changed but only compile-checked.

## Overlap with other branches

- No branch is active. `029d` will also use `canBeDamaged` and the view setters; whichever branch merges first adds them.

## Open questions

1. **`shooting`, `canBeDamaged` and the damage listeners in `PlayerStateComponent`:** all three are shared by every player. Agree that they go in the component, as `ready` and `damaged` did?
2. **Write access on the view:** `direction`, `facing` and `canBeDamaged` as settable properties on `MegaPlayer`, for the two boss writes (and later the handlers in `029f`). Or keep the view read-only and add explicit commands later (for example locks with reasons in `012`)? Recommendation: settable properties now (3 real callers), locks later.
3. **Intro checks that wait for the player to land** (`GutsTank`, `ElecDevil`, `WilyFinalBoss`): `targetPlayer` now (same as today), and the co-op rule ("all players landed"?) for `047`. Agree?
4. **Damage listener name:** `IPlayerDamageListener.onPlayerDamaged(damager, player)`, or keep `IMegamanDamageListener` and only retype the parameter?
5. **The old bosses** (`ReactorMan_OLD`, `MechaDragon_OLD`, `SphinxMiniBoss`): change them here (needed before `029f` removes the extension), or delete them as dead code (`039`)?
