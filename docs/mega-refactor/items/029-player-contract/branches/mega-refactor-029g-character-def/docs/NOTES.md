# Notes: mega-refactor-029g-character-def

## Decisions

- 2026-10-25: the owner accepted the scan recommendations 1-9 ("I agree with all recommendations").
- 2026-10-25: `CharacterDef` goes in core (`players/`), with `MegamanDef` in `entities/megaman/`. It moves to the
  engine when a second character shows which fields and which factory signature all characters share. Pros and
  cons as given to the owner:
  - Engine: allowed (genre concepts) and forces a generic shape; but the factories use core types
    (`Megaman`, `BehaviorType`) and need a type parameter, some fields are this game's tuning, and one caller
    cannot show the right generic shape (the `ComponentKey` risk).
  - Core: concrete types, no generics, matches the item notes ("core first; maybe the engine later"), cheap to
    move later because only the player package calls it; but another game cannot reuse it until then.
- 2026-10-25: `CharacterDef` grows later (body def, sprite and animation def, loadout and unlocks from `024`,
  damage table, sounds, `id`), each when a caller exists. As designed, it supports the same player class with
  other data (values, ability set and order; variants through `copy`). A separate player class cannot reuse the
  factories, because they read `Megaman` fields; that waits for a second character (or `048`).
- 2026-10-25: `Megaman` holds `val character: CharacterDef = MegamanDef` (one field; the swap is one line).
- 2026-10-25: keep the name `CharacterDef`, not `PlayerDef`: a player is a slot and its controller; the def
  describes the body it controls (review 5, `Pawn` and `Controller`). A later NPC sidekick could use the same
  def once input (`031`) and the player-only parts (`048`) allow it.
- 2026-10-25 (replaces recommendations 2 to 4 and 6): no values table. The owner found that a
  `CharacterValues` type would claim to be generic while its fields are the settings of Mega's behaviors.
  Options discussed: per-ability value classes (`JumpValues`); keeping `MegamanValues` with no change; the
  middle option (numbers stay in `MegamanValues`, behaviors read them through a parameter). The owner chose the
  middle option in its simplest form: each behavior factory takes its values as parameters, and `MegamanDef`
  passes `MegamanValues` constants. `CharacterDef` holds only `abilities`. The reads in `Megaman.kt`, its
  components and outside the player stay on `MegamanValues` with `TODO`s. A helper that switches on the target
  player's character was rejected (`is Megaman` under a new name); when a second character exists, each
  character gives its own value and the reader reads it from the contact player.

## Overlap with other branches

No branch is active. `024` (weapon defs and loadout) and `048` (components in place of inheritance) are
planned and touch the same code later (see "Scan").

## Scan (2026-10-25, on `ab5f8cd31`)

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

### Where the character values are today

`M/constants/MegamanValues.kt` is one global `object` with 90 entries. By kind:

| Kind | Entries | Examples | Owner |
|---|---|---|---|
| Movement, gravity and timing | about 50 | `RUN_*`, `JUMP_VEL`, the 6 gravities, `AIR_DASH_*`, `GROUND_SLIDE_*`, `CLIMB_VEL`, `CLAMP_X/Y`, `SWIM_*`, crouch and wall-slide delays, `SPAWNING_DUR` | the character |
| Damage reaction | 11 | `DAMAGE_DURATION`, `DAMAGE_RECOVERY_TIME`, `DAMAGE_FLASH_DURATION`, `DMG_X/Y`, `STUN_*`, `HITS_TO_UNFREEZE`, `FROZEN_PUSH_DUR`, `EXPLOSION_ORB_SPEED` | the character |
| Weapons | about 27 | `BULLET_VEL`, `NEEDLE_*`, `AXE_*`, `MOON_SCYTHE_*`, `SHIELD_GEM_*`, `JETPACK_*`, `RUSH_JET_*`, charge times | the loadout (`024`) |
| Profile | 2 | `START_HEALTH`, `MAX_WEAPON_AMMO` | `GameState`, HUD |
| Input | 1 | `BUTTONS_TO_UNFREEZE` | `031` |
| Level rule | 1 | `MOON_RUN_MAX_SPEED_SCALAR` (read only on `MOON_MAN`, `M/components/BodyComponent.kt:449`) | the level |

Readers in `M/`: `Megaman.kt` (35 names), `BehaviorsComponent.kt` (19), `ControllerComponent.kt` (run values),
`BodyComponent.kt` (clamp, run speed, water scalar), `MegamanWeaponsHandler.kt` and `MegamanJetpackHandler.kt`
(weapon values), `PointsComponent.kt` (`START_HEALTH`).

Readers outside `M/`:

- The stomp bounce writes the player's velocity with `JUMP_VEL * PPM / 2` in 6 enemies (`Goomba:180`,
  `BulletBill:136`, `RedKoopa`, `RedKoopaShell`, `GreenKoopa`, `GreenKoopaShell`). `SwimBooster:60` writes
  `SWIM_VEL_Y`. These are Megaman's values applied to whichever player is in the contact.
- `AbstractBoss` reads `EXPLOSION_ORB_SPEED` (its own death orbs, not the player's).
- `PlayerStatsHandler` and `LevelPauseScreen` read `MAX_WEAPON_AMMO`.

Values not in the table (inline numbers): the body size and the crouch height (`BodyComponent.kt:37-41`), 18
lines of fixture sizes and offsets in `BodyComponent.kt`, 11 lines of slide and crouch offsets in
`BehaviorsComponent.kt`, `MEGAMAN_LADDER_MOVE_OFFSET`, `DEATH_X/Y_OFFSET`, `UNDER_WATER_BUBBLE_DELAY`,
`TRAIL_SPRITE_DELAY`, the sprite offsets, and the weapon spawn-offset table. These are a body def and a sprite
def, not movement values.

### How the values are applied

1. The `direction` setter (`M/Megaman.kt:202-269`) copies 10 values (`jumpVel`, `wallJumpVel`, 7 gravities,
   `swimVel`) into signed fields (`:323-332`): as-is for `UP` and `RIGHT`, negated for `DOWN` and `LEFT`.
   `body.preProcess` selects one gravity and applies it on the axis of the direction.
2. All other values are read at the point of use, with a `when (direction)` per use.

A table can replace (1) with one call that fills the signed fields. (2) is a mechanical rename
(`MegamanValues.X` to `movement.x`).

### Behaviors (the ability list)

- `M/components/BehaviorsComponent.kt:43-787`: one function builds 8 behaviors as closures over `Megaman`,
  then adds them in a fixed order (`:777-784`): `WALL_SLIDING`, `SWIMMING`, `JUMPING`, `AIR_DASHING`,
  `GROUND_SLIDING`, `CROUCHING`, `CLIMBING`, `JETPACKING`. The engine evaluates them in insertion order
  (`OrderedMap`), so the order is gameplay.
- The behaviors read each other only through `isBehaviorActive(BehaviorType.X)`, and share state through
  fields on `Megaman` (`airDashConsumed`, the crouch delays, `wallSlideNotAllowedTimer`). So each behavior can
  be its own factory function on `Megaman` with no change in logic.
- **Weapon couplings inside the behaviors** (all `024`):
  - `NEEDLE_SPIN && shooting` blocks 6 behaviors (`:50, 117, 156, 279, 392, 431, 534`);
  - `RUSH_JET` turns the air dash into a jet (`:280-283, 297, 310, 362`) and is the only way to start
    `JETPACKING` (`:722-725`);
  - `RODENT_CLAWS` sticks to walls (`:95`).
  - 11 more weapon checks in `BodyComponent.kt`, `ControllerComponent.kt` and `Megaman.kt`.
- **Profile reads inside the behaviors:** the enhancement scalars (`JUMP_BOOST` at `:139, 197, 204`;
  `GROUND_SLIDE_BOOST` at `:492`; `FASTER_BUSTER_CHARGING` in `ControllerComponent.kt:65`;
  `DAMAGE_INCREASE` in `Megaman.kt:730`). They read `game.state` (`029e`).
- **Input:** 31 `game.controllerPoller` reads in the behaviors, and `game.selectButtonAction` (`:263-269`).
  Input per player is `031`.
- **Level reads:** `MARIO_LEVEL` sounds (`:238`, `BodyComponent.kt:189`, `Megaman.kt:555`), the `MOON_MAN`
  run speed.

### The player base

- `Megaman : AbstractHealthEntity` with 16 interfaces. `MegaPlayer` (`players/`) is the view that the rest of
  the game uses since `029a` to `029f`. Nothing outside `M/`, the players package and `postCreate` names the
  `Megaman` class for player state, except through `MegaPlayer.megaman` (`029f`).
- Decision 2026-10-23 (`ITEM.md`, `048`): no `IPlayer` interface on the player entity; roles come from
  components, not base classes. An `AbstractPlayer` base class would be a new inheritance layer, against
  `048`.

### The `MegaPlayer.megaman` accessor (`029f`)

Its callers read the current weapon, ammo, `translateAmmo`, `running`, the beam sprite priority and flip. All
but `running` and the sprite reads are the loadout (`024`). A `CharacterDef` alone cannot remove it. The
accessor can go only after `024`.

### Tests

- `core/src/test/.../entities/megaman/`: `MegamanHeartTankTest`, `MegamanStunTargetTest`, a `handlers/` folder.
  No test covers the behaviors, their order or the direction values.
- The item notes suggest a unit test that fixes the ability order and the direction table.

### Open questions for the owner

1. **The player base.** The branch split names a "thin player base". Recommendation: no base class (the `048`
   decision). `Megaman` keeps its class; the def is data that `Megaman` reads.
2. **What `CharacterDef` holds in this branch.** Recommendation: only the two parts with a caller now:
   - `movement: MovementValues` (the about 50 movement, gravity and timing values), read by `Megaman`, its
     behaviors, controller and body;
   - `abilities`: the ordered list of `BehaviorType` to behavior factory, read by `defineBehaviorsComponent`.

   No `id`, body def, animations, loadout or damage table (no caller; body and animations need their own
   work, the loadout is `024`). `MegamanDef` is the one instance. `CharacterDef` goes in `players/`,
   `MegamanDef` in `M/`.
3. **The damage reaction values** (11 entries). Options: (a) in `MovementValues` as well; (b) stay in
   `MegamanValues` for now. Recommendation: (a), as a group in the same table. They are per-character, they
   are read in the same places, and one table is easier to keep than two.
4. **The signed values for the direction.** Recommendation: one function on `MovementValues` that fills the
   10 signed fields for a direction, called by the `direction` setter. It must give exactly the current values;
   a test checks all 4 directions.
5. **The ability factories.** Recommendation: split the 787-line function into 8 factory functions on
   `Megaman` (one for each behavior), with no change in their bodies, and `MegamanDef.abilities` lists them in
   today's order. The weapon couplings, input reads and enhancement reads stay inside the factories, with one
   `TODO` (`024`) at the factory list. A test fixes the order.
6. **The readers outside the player** (the stomp bounce in 6 enemies, `SwimBooster`). Options: (a) they read
   `MegamanDef.movement`, with one `TODO` each that the bounce should use the contact player's character
   (backlog candidate: a bounce capability on the player); (b) `MegaPlayer` gets the character's movement
   values and they read the contact player. Recommendation: (a). (b) gives the same result with one
   character, and the bounce is a rule that a later item decides.
7. **What stays in `MegamanValues`:** the weapon values (`024`), `START_HEALTH` and `MAX_WEAPON_AMMO`
   (profile and HUD), `BUTTONS_TO_UNFREEZE` (`031`), `MOON_RUN_MAX_SPEED_SCALAR` (a level rule). The inline
   numbers stay too. Recommendation: agree.
8. **The `MegaPlayer.megaman` accessor.** Recommendation: it stays; its `TODO` points only at `024`.
9. **Proto Man** (open question in `ITEM.md`). Recommendation: a separate feature item after `029g` and `024`.
   With `029g` merged, item `029` is done.
