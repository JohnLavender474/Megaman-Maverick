# Scope: mega-refactor-029b-enemy-players

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Every enemy in `entities/enemies/` reaches the player through the players service instead of the global `megaman`. Each call site names its intent (the target, any player, the player in a contact). With one player every helper gives Megaman, so the game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes these parts of `ITEM.md`:

- Branch split: "`029b` `enemy-players`: Enemies (about 395 lines, 104 files) use `game.players` helpers in place of `megaman`. Adds `targetFor`, `TargetPolicy` and the `SlotZero` default (see "Targeting design" in `NOTES.md`). Proximity uses `any`. `MegaPlayer` gets a required field only when a moved site needs it."
- Decisions (2026-10-23):
  - Call sites name their intent (`targetFor`, `any`, the contact entity, `forEachPresent`, `require(PlayerSlot(0))`), not a rule and not `players[0]`.
  - Targeting is a `TargetPolicy` with a game default (slot 0 in `029`) and an optional per-entity `TargetingComponent`; a policy returns one player. (See open question 1: this branch may add only part of it.)
  - The engine assumes only a slot and an entity. The game view resolves its required parts at registration.

## Current state

Scan on `6fcb858fb`. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Counts

`grep -w "megaman|Megaman|MegamanValues"` in `entities/enemies/`, without `import` and `package` lines: 395 lines in 104 files. By intent (a regex classifier, checked by hand):

| Intent | Lines | Files | Example |
|---|---|---|---|
| Facing toward the player | 167 | 67 | `facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT` |
| Position or aim (other) | 113 | 56 | `megaman.body.getCenter().sub(body.getCenter()).nor()` (`DemonMet.kt:280`); line-of-sight boxes (`SniperJoe.kt:449-460`); pathfinding targets (`DeathBat.kt:249`, `SubmarineJoe.kt:251`) |
| Proximity or overlap | 42 | 31 | `overlaps(megaman.body.getBounds())` in a sensor check |
| Reads player state | 23 | 17 | `megaman.dead` (7), `megaman.direction` (17, with `.rotation`), `megaman.ready` (`BuilderKibbo.kt:119`), `isBehaviorActive(CLIMBING)` (`RatRobot.kt:144`), `body.physics.velocity` |
| Identity ("is this the player?") | 18 | 16 | `damageable == megaman` (`Bat.kt:231`), `entity is Megaman` (`Matasaburo.kt:70`), `feet.getEntity() == megaman` (`Goomba.kt:171`) |
| Comments and logs | 10 | 7 | |
| Fixtures | 9 | 5 | `megaman.leftSideFixture.getShape().overlaps(...)` (`GreenKoopaShell.kt:176-178`, `RedKoopaShell.kt:177-179`), `megaman.damageableFixture` (`JetpackBlaster.kt:236`) |
| Stomp bounce (writes the player's velocity) | 6 | 6 | `megaman.body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM / 2f` (`Goomba.kt:177`, `RedKoopa.kt:208`, `GreenKoopa.kt:210`, `BulletBill.kt:134`, both Koopa shells) |
| Ownership | 5 | 3 | `projectile.owner == megaman` (`Cokeyro.kt:258`, `StagedMoonLandingFlag.kt:275, 298`, `UFOhNoBot.kt:303, 307`) |
| Damage-class sets | 2 | 2 | `objectSetOf(Megaman::class)` (`SwinginJoe.kt:218`, `PreciousTron.kt:344`) |

Members read: `body.getX` 146, `body.getCenter` 66, `body.getBounds` 47, `body.getY` 36, `body.getMaxX` 18, `direction` 17, `body.physics` 9, `body.getMaxY` 8, `dead` 7, fixtures 7, `body.getPositionPoint` 2, `ready` 1, `isBehaviorActive` 1, `damaged` 1 (`MagFly.kt:121`).

### Other findings

- 8 lines use `game.megaman` directly instead of the extension (`Matasaburo`, `SuctionRoller`, `Petaspark`, `DragonFly`, `MagFly`).
- `utils/misc/FacingUtils.kt` (`getPreferredFacingFor`, `isFacingBlock`) reads `entity.megaman`; enemies call it.
- The facing idiom has variants that differ when the x values are equal (`<` gives `RIGHT`, `>` gives `LEFT`, `>=` gives `RIGHT`). A shared "face the player" helper would change the result for some enemies at equal x. So this branch keeps each expression as it is and changes only the player reference.
- Most enemies read `megaman.body` each frame; none keeps a copy of the player in a field. Pathfinding targets are lambdas that read the player on each call (`DeathBat.kt:249`, `SubmarineJoe.kt:251`).
- `SniperJoe_OLD` is still live: `SwingingPlatform` spawns it by tag, and the deprecated `EnemiesFactory` pools it.
- `AbstractEnemy` (`entities/contracts/`) reads the player for drop odds (lives and health ratio, `:96-103`), damage scaling and the owner check (`:243`, `:255`), `isMegamanShootingAtMe` (`:287-290`), the one-up (`:329`) and the floating points direction (`:332`, `:367`). These are profile (`029e`) and faction (`030`) concerns.
- Names that say "Megaman" (`directlyChaseMegaman`, `isMegamanLeft`, `targetMegaman`, `stuckToMegaman`) are method and parameter names, not references.
- Real levels use the Mario enemies (`Goomba`, the Koopas, `BulletBill`) only in `SMB3_1`.

## In scope

1. **The target helper (core):** a core extension for "the player this entity targets", for example `val IMegaGameEntity.targetPlayer: MegaPlayer`, built on `players.targetFor(this)`. In `029` it gives the view in slot 0 (Megaman), also while he is dead, as `megaman` does today. It is not null: the game always has at least one registered player, and a slot stays registered while its player waits to respawn. Open question 1 decides how much of the policy design comes with it.
2. **An identity helper (engine):** `IPlayers.viewOf(entity): V?` (the view of a registered entity, or null), in slot order. `slotOf` is then built on it.
3. **`MegaPlayer` accessors**, only those that the moved enemy sites need. Each one reads a part that the view checks at registration:
   - `direction` (the entity is an `IDirectional`), `dead` (a `MegaGameEntity`), `isBehaviorActive` (an `IBehaviorsEntity`);
   - the feet, side and damageable fixtures (from the body properties that Megaman's fixture accessors read today);
   - `ready` and `damaged` (2 sites), from a new core `PlayerStateComponent` (decision below).
4. **`PlayerStateComponent` (core):** the state that every player shares, as a component that the player view requires.
   - It holds `ready` and gives `damaged` from the player's damage timer (`!damageTimer.isFinished()`, the same as `Megaman.damaged` today).
   - `Megaman` adds it in `init()`. His `ready` property reads and writes the component, so the about 40 existing `ready` call sites do not change; `damaged` keeps its meaning.
   - Its `reset()` stays a no-op: the engine resets every component on destroy, and Megaman already sets `ready` himself on spawn and death.
   - Other shared state (for example `stunned`, `teleporting`) moves in later, only when a moved call site needs it. It is the start of the lifecycle component that `048` plans.
5. **The enemy call sites (104 files)**, by intent:
   - Facing, position, aim, line of sight, pathfinding targets and state reads: `megaman` becomes the target helper. Each expression stays the same apart from the reference.
   - Sensor or area overlap with the player's body: `players.any { ... }`.
   - Identity from a contact or damage callback (`== megaman`, `is Megaman`): `players.viewOf(entity)`. When the site then reads the player, it reads the view.
   - Ownership (`owner == megaman`, 5 lines): `players.viewOf(owner) != null`. With one player this is the same result. `030` later replaces it with a faction check.
   - Stomp bounce (6 lines): see open question 3.
   - Fixture overlap in the Koopa shells and `JetpackBlaster`: through the target's view accessors.
   - The 8 direct `game.megaman` reads: as above.
6. **`FacingUtils`:** `entity.megaman` becomes the target helper.
7. **Test map rooms:** a subagent adds rooms to `RefactorTest029.tmx` with the `level-builder` skill, built and checked on the baseline code first. A first list (the plan fixes it): a facing and aiming room (`Met`, `SniperJoe`), a proximity room (`Darspider` or `SuicideBummer`), a chaser room (`Bat` or `DeathBat`, pathfinding), a `MagFly` room near a ladder (climbing and damaged checks), a `JetpackBlaster` room (aim line against the damageable fixture), a contact-identity room (`Matasaburo` or `Coldier` wind, `GapingFish` chomp), and an ownership room (`Cokeyro` or `UFOhNoBot`, hit by a player shot). The Mario enemies stay in `SMB3_1` (see the risks).
8. **Tests:** engine `viewOf`; core: the target helper gives the slot 0 view, also while the player is not present; the `MegaPlayer` accessors read through their parts, and registration fails without them; `Megaman.ready` reads and writes `PlayerStateComponent`.

## Out of scope

- `AbstractEnemy` and `AbstractBoss`: their reads are profile (`029e`), faction (`030`) or boss flow (`029c`). Open question 4.
- Bosses (`029c`), world entities (`029d`), screens and handlers (`029f`).
- A shared "face the player" helper, and renames of methods with "Megaman" in the name (behavior at equal x and churn; a candidate for `049`).
- The damage-class sets (`Megaman::class` in `SwinginJoe` and `PreciousTron`): `030` or `023`. They name the type, not the global, so they do not block removing the `megaman` extension in `029f`.
- Any co-op targeting rule (`047`).

## Affected areas and risks

- **104 enemy files.** The change is mechanical but large. Risk: a wrong intent choice, for example `any` where the old code read the target's position. With one player both give the same result, so the risk is in review, not in play. The plan does the work in batches (by intent or by file group), each reviewed, built and tested.
- **Behavior while Megaman is dead:** the target helper must give Megaman also while he is dead or not ready, because some enemies read his position then. `any` does not filter by `present` unless the site already checks `dead`.
- **The Mario enemies** run only in `SMB3_1` (a `MARIO_LEVEL`). Their stomp and shell checks are tested there, not in the test map, unless the test map can place them.
- **Performance:** the target helper runs a slot lookup on each read (about 300 reads in update code). It must not allocate. Today it is a field read; after the change it is one array read in `Players.get`, plus the policy call if open question 1 adds one.
- **Pooled enemies:** none keeps a player reference, so pooling is not affected.

## Overlap with other branches

- No branch is active. `029c` and `029d` will use the same target and identity helpers. If they run in parallel, the helpers come from whichever branch merges first.

## Open questions

1. **How much of the targeting design comes now?** The recorded design is a `TargetPolicy` with a game default and an optional per-entity `TargetingComponent`. In this branch no entity needs its own rule, and the only policy is "slot 0". Under the "no speculative API" lesson (`ComponentKey`), I recommend that `029b` adds only `targetFor` (and the `targetPlayer` extension), which gives slot 0 in one place. `TargetPolicy` and `TargetingComponent` come with their first real caller (a second rule in `047`, or a boss in `029c`). The call sites do not change when they come. Or add `TargetPolicy` with its one `SlotZero` implementation now, as recorded?
2. Answered (2026-10-23): `ready` and `damaged` are shared by every player, so they go in a core `PlayerStateComponent` that the view requires (in scope, item 4), not through a `Megaman` cast and not through a role interface.
3. **Stomp bounce** (6 Mario enemies write the player's velocity with `MegamanValues.JUMP_VEL`): (a) keep the write, through the view's body, as today (mechanical; the Mega constant stays in the enemy); (b) a player capability (a body receiver or key that Megaman registers, like the `014d` receivers), so each character can bounce its own way. Recommendation: (b) has 6 real callers, but it changes how the bounce is applied; (a) is the smaller step and keeps this branch mechanical. I lean to (a), with (b) noted for `029g`.
4. **`AbstractEnemy`:** leave it to `029c` (with `AbstractBoss`, as "base classes") and `029e` (drops and the one-up), or move its target reads (`isMegamanShootingAtMe`, the floating points direction) here?
5. **Target helper name:** `targetPlayer`, `target`, or another name?
