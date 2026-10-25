# Scope: mega-refactor-029g-character-def

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

The player's movement and damage-reaction values and its ordered ability list come from a `CharacterDef`
(`MegamanDef`), not from the global `MegamanValues` and one 787-line behavior function. The game plays the same.
A later character can be the same player class with another def.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029g` `character-def`: `CharacterDef`, movement values table, ordered ability list, thin player
  base. Weapon couplings in behaviors overlap `024`".
- Proposed scope: "`CharacterDef`: movement table, ordered ability set, body def, animations, loadout.
  `MegamanDef` keeps behavior identical". Only the movement table and the ability set have a caller in this
  branch (decision 2026-10-25).
- Decision 2026-10-23: no `IPlayer` interface on the player entity; roles come from components (`048`). So no
  player base class (decision 2026-10-25).
- The `029b` to `029f` rules: write code only when production code calls it; unused paths are commented out
  with a `TODO`.

## Current state

The full scan is in `docs/NOTES.md` ("Scan", on `ab5f8cd31`). Summary, with `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`:

- `M/constants/MegamanValues.kt` is one global `object` with 90 entries: 46 movement, gravity and timing
  values, 11 damage-reaction values, about 27 weapon values, and 6 values for the profile, the HUD, the input
  and one level rule.
- The `direction` setter (`M/Megaman.kt:202-269`) copies 10 of them into signed fields (`:323-332`), negated for
  `DOWN` and `LEFT`. All other values are read where they are used.
- `M/components/BehaviorsComponent.kt:43-787` builds the 8 behaviors as closures over `Megaman` in one function
  and adds them in a fixed order (`:777-784`). The engine runs them in that order. The behaviors connect only
  through `isBehaviorActive` and shared fields on `Megaman`.
- The behaviors also hold weapon checks (`024`), input reads (`031`), enhancement reads and level checks.
- Outside the player: 6 enemies bounce the player with `JUMP_VEL` (the stomp), `SwimBooster` uses
  `SWIM_VEL_Y`, `AbstractBoss` uses `EXPLOSION_ORB_SPEED` for its own death orbs.
- No test covers the behaviors, their order or the direction values.

## In scope

The owner accepted the scan recommendations 1-9 on 2026-10-25 (see `NOTES.md`, "Decisions").

1. **`CharacterDef`** in `players/`, with two parts:
   - `values: CharacterValues`;
   - `abilities`: the ordered list of `BehaviorType` to behavior factory.

   No `id`, body def, animations, loadout or damage table (no caller). Class names: the scan said
   `MovementValues`; because the table also holds the damage-reaction values (recommendation 3), this scope
   names it `CharacterValues`.
2. **`CharacterValues`** in `players/`: the 46 movement, gravity and timing values and 10 damage-reaction
   values from `MegamanValues`, with the same numbers, each scaled by `PPM` where it is used today (no change
   in where the scaling happens).
   - Movement: slip-slide threshold, clamp, the run steps, post-action momentum, run speed and impulses, the
     water and ice scalars and gravities, swim, jump, wall jump and wall slide, the 6 gravities, air dash, ground
     slide, climb, the crouch delays and speed, the wall-slide delay, the spawn duration.
   - Damage reaction: `DAMAGE_DURATION`, `DAMAGE_RECOVERY_TIME`, `DAMAGE_FLASH_DURATION`, `DMG_X`, `DMG_Y`,
     `STUN_DUR`, `STUN_IMPULSE_X`, `STUN_IMPULSE_Y`, `HITS_TO_UNFREEZE`, `FROZEN_PUSH_DUR`.
3. **The signed values for a direction:** one function on `CharacterValues` gives the 10 values with the sign
   for a direction. The `direction` setter uses it in place of its two copy blocks.
4. **`MegamanDef`** in `entities/megaman/`: the one `CharacterDef`, with today's values and today's ability
   order (`WALL_SLIDING`, `SWIMMING`, `JUMPING`, `AIR_DASHING`, `GROUND_SLIDING`, `CROUCHING`, `CLIMBING`,
   `JETPACKING`).
5. **`Megaman.character: CharacterDef = MegamanDef`.** `Megaman`, its behaviors, `ControllerComponent` and
   `BodyComponent` read the values through it.
6. **The ability factories:** the 8 behaviors become 8 factory functions on `Megaman`, with no change in their
   bodies. `defineBehaviorsComponent` adds the behaviors in the order of `character.abilities`. One `TODO`
   (`024`) at the factory list: the weapon checks inside the factories belong to the loadout.
7. **Readers outside the player:** the 6 enemies with the stomp bounce and `SwimBooster` read
   `MegamanDef.values`, with a `TODO` each that the bounce should use the contact player's character. Backlog
   candidate: a bounce capability on the player.
8. **`MegamanValues` keeps** the weapon values (`024`), `START_HEALTH` and `MAX_WEAPON_AMMO` (profile and HUD),
   `BUTTONS_TO_UNFREEZE` (`031`), `MOON_RUN_MAX_SPEED_SCALAR` (a level rule) and `EXPLOSION_ORB_SPEED`
   (`AbstractBoss` also reads it for its own orbs; it is a death effect, not a player value). The moved
   entries are removed from it.
9. **`MegaPlayer.megaman` stays.** Its `TODO` no longer names `029g`: the loadout reads go with `024`; the
   `running` and sprite reads wait for a second character.
10. **Tests:** the ability order of `MegamanDef`; the signed values for all 4 directions; the `MegamanDef`
    values against today's numbers.

New APIs and their first callers:

| API | First callers |
|---|---|
| `CharacterDef` (`values`, `abilities`) | `Megaman.character`, `defineBehaviorsComponent` |
| `CharacterValues` (56 values) | `Megaman`, the 8 behavior factories, `ControllerComponent`, `BodyComponent`, 6 enemies, `SwimBooster` |
| The signed values for a direction | `Megaman.direction` setter |
| `MegamanDef` | `Megaman.character`, 6 enemies, `SwimBooster` |
| `Megaman.character` | `defineBehaviorsComponent`, the value reads in the player package |
| The 8 behavior factory functions | `MegamanDef.abilities` |

## Out of scope

- A player base class (`048` decision).
- A body def, sprite and animation def, sounds, a damage table, a character `id` (no caller; later, when a
  caller exists).
- The inline numbers: body and fixture sizes, crouch and slide offsets, ladder offset, death offsets, sprite
  offsets, the weapon spawn-offset table.
- The weapon checks, the weapon values and the loadout (`024`); the input reads (`031`); the enhancement reads
  (profile, `029e`); the level checks (`MARIO_LEVEL`, `MOON_MAN`).
- Factories that a separate player class can reuse (they read `Megaman` fields; later, with a second character
  or `048`).
- Moving `CharacterDef` to the engine (when a second character shows the shared shape).
- Proto Man: a separate feature item after `029g` and `024`.
- The swim impulse sign question from the item notes (review 1, section 5): the values and their signs stay as
  they are.

## Affected areas and risks

- **The values must not change.** A copy error (a wrong number or a wrong sign) changes the feel of the game
  and is hard to see in play. The tests compare each value with today's number, and the diff review checks
  each read site.
- **The ability order.** A wrong order changes which behavior wins on the same frame. A test fixes the order.
- **The behavior split.** Each factory must keep its closure state (its own timers and fields) per `Megaman`
  instance, as today. The bodies move without change; the diff review checks this.
- **Construction order.** `character` is set when `Megaman` is made, before `init()` and before the first
  `direction` set, so the values are ready when they are first read.
- **Files:** new `players/CharacterDef.kt`, `players/CharacterValues.kt`, `entities/megaman/MegamanDef.kt`;
  `Megaman.kt`, `BehaviorsComponent.kt`, `ControllerComponent.kt`, `BodyComponent.kt`, `MegamanValues.kt`,
  `PlayerExtensions.kt`, the 6 enemies, `SwimBooster`; new tests.

## Overlap with other branches

- No other branch is active. `024` will change the same behavior factories (the weapon checks) and add the
  loadout to `CharacterDef`. `048` may turn the shared `Megaman` fields into components.

## Open questions

None. The owner accepted the scan recommendations on 2026-10-25.
