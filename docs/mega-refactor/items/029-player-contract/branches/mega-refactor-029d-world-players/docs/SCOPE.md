# Scope: mega-refactor-029d-world-players

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

The world entities (special entities, projectiles, blocks, hazards, decorations, sensors, explosions), the contact rules and the world body code reach the player through the players service instead of the global `megaman` and the `Megaman` type, with the `029b` intent rules. `HIT_BY_PLAYER` takes the player view. With one player the game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029d` `world-players`: Special, projectiles, blocks, hazards, sensors, decorations, world code: `is IPlayer` checks, kill zones per contact player, stomp bounce, gravity devices, `HIT_BY_PLAYER` retype".
- The `029b` and `029c` rules: call sites name their intent (`targetPlayer`, `players.any`, `players.all`, `players.forEach`, `players.viewOf`); shared player state goes in `PlayerStateComponent`; write code only when production code calls it.

## Current state

Scan on `9432e437b`. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Counts

About 204 code lines in about 80 files: `entities/special` 83 lines, `projectiles` 38, `blocks` 34, `hazards` 31, `decorations` 7, `sensors` 5, `explosions` 3, `world/` 7, `damage/` 3. Members: `direction` 26 (plus `.rotation`, `.name`), `body.getCenter` 19, `body.getBounds` 17, `body.physics` 10, `ready` 7, `dead` 6, fixtures (feet, side, head, feet gravity) about 20, `isFacing`/`facing` 5, `charging` 4, `fullyCharged` 2, behaviors 4, `depleteHealth` 2, `aButtonTask` 2, `invincible`, `teleporting`, `spawned`, `id`, `bodyOverGround` 1 each.

### By intent

| Intent | Examples |
|---|---|
| The player's gravity direction as the world direction | `ConveyorBelt:178`, `GravityBlock:137`, `Spike:121, 209`, `MagmaWave`, `MagmaPellet`, `MagmaFlame`, `MagmaGoop`, `Disintegration`, `DustPuff`, `PreciousGem:158`, `PreciousShard:96`, `PortalHopper:276`, `Bullet:244, 252` |
| Position, facing, fixtures and state of one player | `PushableBlock` (side fixtures, facing, velocity), `FlipperPlatform`, `QuestionBlock` (head-bump velocity), `PipePortal:152-158` (fixture by direction), `InfernoChainPlatform`, `Lift`, `DropperLift`, `CartV2`, `FloorButton`, `GroundSlideBlocker`, `DrWily` (`dead`), `TubeBeamer`, `BulletBillLauncher`, `LaserBeamer` (`spawned`, `ready`), `ChargedShot:113-114` (direction and facing at the shot) |
| Identity from a contact or collision | `entity == megaman` / `is Megaman` / `isAny(Megaman::class, ...)` (an instance check): `PipePortal`, `SpringBouncer`, `GroundSnow` x3, `CapsuleTeleporter:208`, `WoodCrate:236`, `QuestionBlock:141`, `GreenPelletBlast`, `UFOBomb`, `BreakableBlock:153`, `AbstractBlock:44`, `Rain:200`, `SpitFireball`, `Laser:426`, `FeetRiseSinkBlock:99`, `CeilingCrusher:159`, `ToxicWaterfall:136, 150`, `FlipperPlatform:202` |
| Ownership | `owner == megaman` / `owner is Megaman`: `IceBomb`, `Snowhead`, `BunbyRedRocket`, `Snowball:78`, `PreciousGem:94, 211, 212, 392`, `PreciousGemCluster`, `ReactorManProjectile:194, 215`, `SmokePuff`, `BreakableBlock:144`, `PinkBlock:35, 53`, `WoodCrate:265`, `WilyFakeCapsule` x4, `SmallIceCube:158-160`, `DeadlyLeaf`, `Water:151`, `PolygonWater:100`, `JoeBall:150`, `Snowball:91` |
| Kill zones | `MegamanDeath.update` (overlap, not dead, not invincible unless instant, then `depleteHealth()`), `GravityBlock:209-210` (block falls on the head) |
| Writes to the player | `CapsuleTeleporter:208` (`direction`), `GravitySwitcharoo:191-196` and `GravityCapsule:181-186` (`direction` and the Mega-only `aButtonTask`), `SwimBooster:58` (velocity with `MegamanValues.SWIM_VEL_Y`), `SpiderWeb:119, 149` (sticks to the player) |
| Light per player (`DarknessV2`, `V3`, `V4`) | a light source keyed by `megaman` (or `megaman.id`), with a radius from `charging`, `fullyCharged` and `JETPACKING`; `Megaman::class pairTo STANDARD_LIGHT_SOURCE` in the light table; the beam properties `"${Megaman.TAG}_beam"` |
| World code | `IBodyExtensions:20` (only the player's body updates during a camera rotation), `MegaContactRules:33` (`FeetSandRule { game.megaman.ready }`), `BodyPlayerRule` (`as Megaman`, the `029` `TODO`), `Receivers.HIT_BY_PLAYER` |
| Exact-class sets (`Megaman::class` in a `KClass` set) | `StandardDamageNegotiator:50, 89, 128`, `SmallIceCube:67`, `UnderwaterFan:83`, `JoeBall`/`Snowball` masks, the Darkness light tables |
| Names and Tiled keys | `Megaman.TAG` in Tiled property names (`CapsuleTeleporter:71`, `Spike:182`), the tag filter `"megaman,..."` in `DecoratedGravityForce:32` |

### Other findings

- The gravity devices put only `megaman` in their trigger set, and set `aButtonTask` (jump if on the ground, else air dash) right after the direction change. `aButtonTask` is a `Megaman` property, not shared player state.
- The input reads in `PipePortal`, `SpringBouncer`, `SpiderWeb` and `GroundSlideBlocker` (controller 0) are `031`'s; only their player references change here.
- The beam properties keyed by `Megaman.TAG` are written by the spawn and end-level handlers (`029f`).
- The items (`entities/items/`, `ItemEntity`, `PlayerItemRule`) are `029e`'s: `Life` reads lives, `HealthTank`/`HeartTank` read the profile.

## In scope

1. **`MegaPlayer` accessors and commands** with a caller in this branch: `depleteHealth()` (the entity must be an `IHealthEntity`; kill zones), `headFixture` and `feetGravityFixture` (from the body properties; `FloorButton`, `GravityBlock`, `InfernoChainPlatform`, `PipePortal`), `teleporting` (open question 2), `isAnyBehaviorActive` (`GroundSlideBlocker`). The rest already exists.
2. **The call sites, by intent:**
   - world direction, position, facing, fixtures and state reads of one player: `targetPlayer`;
   - identity (`== megaman`, `is Megaman`, `isAny(Megaman::class, ...)`): `players.viewOf(entity)`;
   - ownership: `players.viewOf(owner)`;
   - contact with any player (owner decision): `PushableBlock` (pushed by any player whose side fixture touches it while facing and moving toward it) and `FlipperPlatform` (any player whose feet are on it) use `players.any`, or `players.forEach` where the block acts on that player, with the checks of today on the same player; other "one player" sites that are really about contact get the same treatment or a `TODO` for `047`;
   - kill zones: `players.forEach` with today's checks for each player (`MegamanDeath`: not dead, overlapping, not invincible unless instant; `GravityBlock`: head overlap);
   - `MegamanDeath` is renamed `PlayerDeath` (owner decision): the class, its `TAG`, the comment in `Death.kt`, and the 29 objects named `MegamanDeath` in 4 TMX files (`MoonMan_16x14_v5.tmx`, `InfernoMan_16x14_v3.tmx`, `TimberWoman_16x14_v4.tmx`, `TimberWoman_16x14_v5_draft_1.tmx`). A test room checks it;
   - `CapsuleTeleporter` sets the direction of the player it received (the contact entity);
   - `SwimBooster` keeps `MegamanValues.SWIM_VEL_Y` through the view's body, with a `TODO` for `029g` (like the stomp bounce);
   - `SpiderWeb` sticks to the target player;
   - `ChargedShot` takes the direction and facing of the target (with one player the shooter); a `TODO` says the shooter is the owner's view (`030`);
   - the gravity devices: a player receiver that Megaman registers ("a gravity device switched you to this direction"); Megaman sets its direction and `aButtonTask` there, unchanged; a device switches every player in its trigger area (owner decision, open question 1);
   - the Darkness light: `players.forEach`, one light source per player, with the radius from a light-radius receiver that each player registers (owner decision, open question 3);
   - `teleporting` moves into `PlayerStateComponent`, like `ready` (owner decision, open question 2);
   - the player's gravity direction used as the world direction: see "Gravity direction" (under discussion).
3. **World code:** `IBodyExtensions.defaultDoUpdate` tests `players.viewOf(entity) == null` instead of `!is Megaman`; `FeetSandRule` takes `{ game.players.any { it.ready } }`, with a `TODO` for `047` (owner decision, open question 4); `Receivers.HIT_BY_PLAYER` becomes `(player: MegaPlayer) -> Unit`, `BodyPlayerRule` passes `players.viewOf(entity)` (its `TODO` goes), and `SmallGrass`, `Togglee`, `SmallIceCube` (`it.canBeDamaged`) take the view.
4. **Tests:** the new view accessors; `HIT_BY_PLAYER` is called with the view by `BodyPlayerRule` (with a relaxed game mock and registered test player, if practical).

## Out of scope

- The items, `ItemEntity` and `PlayerItemRule` (`029e`).
- The exact-class `KClass` sets (`030` or `023`), the Tiled property names and tag strings with "megaman", the beam properties (`029f`).
- The controller reads (`031`).
- Co-op rules (whose gravity a room follows, light for players who join later): `047`.

## Affected areas and risks

- **About 80 files**, many small. With one player every helper gives Megaman.
- **Instance checks:** `is Megaman` and `isAny(Megaman::class, ...)` are instance checks; `players.viewOf(entity) != null` is true for exactly the same entity (Megaman, the only registered player).
- **Kill zones:** `forEach` visits Megaman also while he is not present; each keeps its own `dead`/`invincible` checks, so the result is the same.
- **`IBodyExtensions.defaultDoUpdate`** runs for every body every frame; `viewOf` is a loop over 1 slot.
- **Real-level checks:** gravity rooms, dark rooms, the Mario level, pipes, carts, the Wily capsules.

## Overlap with other branches

- No branch is active.

## Gravity direction (under discussion)

The owner asked for one source of truth for the gravity direction, and for a scan of every way the player's direction changes (2026-10-23). The scan result and the options are in the process log entry of that date; this section gets the decision.

## Open questions

Answered by the owner (2026-10-23): 1 (receiver; players in the trigger area), 2 (`teleporting` in `PlayerStateComponent`), 3 (light-radius receiver, one light per player), 4 (`players.any { it.ready }` with a `TODO`). Kept below for the record.

1. **Gravity devices** (`GravitySwitcharoo`, `GravityCapsule`) set `direction` and then the Mega-only `aButtonTask`. Options:
   - (a) a player receiver (a typed key on the entity, as in `014d`) that Megaman registers: "a gravity device switched you to this direction"; Megaman sets its direction and its `aButtonTask` there, unchanged. The devices call it for each player they affect.
   - (b) move `aButtonTask` into `PlayerStateComponent`. But it is Mega's A-button behavior, not state that every player shares.
   Recommendation: (a). Also: which players does a device switch? Today only `megaman`, when he is in the trigger. Recommendation: the players in the trigger area (`players.forEach` with the trigger check), which is the same with one player.
2. **`teleporting`** (`PreciousGem:392` hides the gem while its owner teleports): shared player state? It is Mega's teleport flag, used by pipes and teleporters for any player. Recommendation: a `MegaPlayer.teleporting` accessor through a `PlayerStateComponent` field, like `ready`.
3. **Darkness light per player:** each player should give light. Recommendation: `players.forEach`, one light source per player (keyed by the player's entity), with the radius from the player's state. That needs `charging` and `fullyCharged` on the view: shared player state (every player can charge a weapon), or weapon state for the loadout (`024`)? Options: (a) `PlayerStateComponent` fields read from Megaman's charge status; (b) a light-radius receiver that each player registers (the player decides its own light). Recommendation: (b), because the radius rule (charging, jetpacking) is Mega's content.
4. **`FeetSandRule` supplier** (`{ game.megaman.ready }`, used for every feet fixture on sand): `players.any { it.ready }` (same with one player), or the ready flag of the feet fixture's own player (more correct, but a change for enemy feet on sand while Megaman is not ready)? Recommendation: `players.any { it.ready }` now, with a `TODO` for `047`.
