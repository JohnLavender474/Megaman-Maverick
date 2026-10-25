# Scope: mega-refactor-029g-character-def

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

The player's behaviors come from an ordered ability list in a `CharacterDef` (`MegamanDef`), not from one
787-line function. Each behavior factory takes its movement values as parameters, so another character can use
the same behavior with other numbers. Mega's numbers stay in `MegamanValues`. The game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029g` `character-def`: `CharacterDef`, movement values table, ordered ability list, thin player
  base. Weapon couplings in behaviors overlap `024`".
- Proposed scope: "`CharacterDef`: movement table, ordered ability set, body def, animations, loadout.
  `MegamanDef` keeps behavior identical". This branch takes only the ordered ability set. The values stay in
  `MegamanValues` and reach the behaviors as factory parameters, in place of a values table (decision
  2026-10-25).
- Decision 2026-10-23: no `IPlayer` interface on the player entity; roles come from components (`048`). So no
  player base class (decision 2026-10-25).
- The `029b` to `029f` rules: write code only when production code calls it; unused paths are commented out
  with a `TODO`.

## Current state

The full scan is in `docs/NOTES.md` ("Scan", on `ab5f8cd31`). Summary, with `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`:

- `M/constants/MegamanValues.kt` is one global `object` with 90 entries (movement, damage reaction, weapons,
  profile, input, one level rule). All code reads them directly.
- `M/components/BehaviorsComponent.kt:43-787` builds the 8 behaviors as closures over `Megaman` in one function
  and adds them in a fixed order (`:777-784`). The engine runs them in that order. The behaviors connect only
  through `isBehaviorActive` and shared fields on `Megaman`.
- The behaviors read 16 movement values from `MegamanValues`:
  - wall slide: `WALL_SLIDE_FRICTION_TO_APPLY` (`:89`);
  - swim: `SWIM_TIMER` (`:111`);
  - jump: `WALL_JUMP_HORIZONTAL` (`:189, 214, 226`);
  - air dash: `AIR_DASH_MIN_TIME`, `AIR_DASH_MAX_TIME`, `AIR_DASH_VEL`, `WATER_AIR_DASH_VEL`,
    `AIR_DASH_END_BUMP`, `WATER_AIR_DASH_END_BUMP` (`:257-351`);
  - crouch: `CROUCH_MAX_VEL` (`:396`);
  - ground slide: `GROUND_SLIDE_MIN_TIME`, `GROUND_SLIDE_MAX_TIME`, `GROUND_SLIDE_COOLDOWN`, `GROUND_SLIDE_VEL`,
    `WATER_GROUND_SLIDE_VEL` (`:420-489`);
  - climb: `CLIMB_VEL` (`:663-696`).

  They also read 3 weapon values (`RUSH_JET_AIR_DASH_MAX_TIME`, `RUSH_JET_AIR_DASH_SCALAR`,
  `JETPACK_Y_IMPULSE`), the signed fields on `Megaman` that the `direction` setter fills (`jumpVel`,
  `swimVel`, the gravities), shared timers on `Megaman`, and inline offsets.
- The behaviors also hold weapon checks (`024`), input reads (`031`), enhancement reads and level checks.
- About 30 more values are read by `Megaman.kt`, `ControllerComponent.kt` and `BodyComponent.kt`: the
  gravities, run steps, clamp, damage, stun and freeze values, and the timers made at construction.
- Outside the player: 6 enemies bounce the player with `JUMP_VEL` (the stomp), and `SwimBooster` uses
  `SWIM_VEL_Y`.
- No test covers the behaviors or their order.

## In scope

Decisions of 2026-10-25 (see `NOTES.md`, "Decisions").

1. **`CharacterDef`** with one part: `abilities`, the ordered list of `BehaviorType` to behavior factory. The
   factory runs on `Megaman` (it reads `Megaman` fields) until a second player class exists. No values table,
   `id`, body def, animations or loadout (no caller).
2. **The 8 behavior factories:** the 8 behaviors become 8 factory functions on `Megaman`. Each takes its
   movement values (the 16 listed above) as parameters, with no default values. The bodies change only from
   `MegamanValues.X` to the parameter name. The jetpack factory takes no parameters: its values are the Rush
   Jet's.
3. **`MegamanDef`**: the one `CharacterDef`. It lists the 8 factories in today's order (`WALL_SLIDING`,
   `SWIMMING`, `JUMPING`, `AIR_DASHING`, `GROUND_SLIDING`, `CROUCHING`, `CLIMBING`, `JETPACKING`) and passes
   `MegamanValues` constants as the arguments. One `TODO` (`024`) at the list: the weapon checks and weapon
   values inside the factories belong to the loadout.
4. **`Megaman.character: CharacterDef = MegamanDef`.** `defineBehaviorsComponent` adds the behaviors in the
   order of `character.abilities`.
5. **`MegamanValues` stays** with all 90 entries and the same numbers. One `TODO` in it: `Megaman.kt`,
   `ControllerComponent` and `BodyComponent` read these values directly; a second character needs them through
   a component parameter or a table on the character.
6. **Readers outside the player** (the 6 stomp-bounce enemies, `SwimBooster`) do not change, except for one
   `TODO` each: the value should come from the contact player's character. Backlog candidate: a bounce
   capability on the player.
7. **`MegaPlayer.megaman` stays.** Its `TODO` no longer names `029g`: the loadout reads go with `024`; the
   `running` and sprite reads wait for a second character.
8. **Tests:** the order of `MegamanDef.abilities`; the behavior order in a built `Megaman`'s
   `BehaviorsComponent`, if a test can build it (as `MegamanHeartTankTest` builds a `Megaman`).

New APIs and their first callers:

| API | First callers |
|---|---|
| `CharacterDef` (`abilities`) | `Megaman.character`, `defineBehaviorsComponent` |
| `MegamanDef` | `Megaman.character` |
| `Megaman.character` | `defineBehaviorsComponent` |
| The 8 behavior factory functions (with their parameters) | `MegamanDef.abilities` |

## Out of scope

- A values table or per-ability value classes (decision 2026-10-25: factory parameters in their place).
- Parameters for the values that `Megaman.kt`, `ControllerComponent` and `BodyComponent` read (the `TODO` in
  item 5).
- The signed copy in the `direction` setter: it stays as it is.
- A player base class (`048` decision).
- A body def, sprite and animation def, sounds, a damage table, a character `id` (no caller).
- The inline numbers: body and fixture sizes, crouch and slide offsets, ladder offset, death offsets, sprite
  offsets, the weapon spawn-offset table.
- The weapon checks, the weapon values and the loadout (`024`); the input reads (`031`); the enhancement reads
  (profile, `029e`); the level checks (`MARIO_LEVEL`, `MOON_MAN`).
- Factories that a separate player class can reuse (they read `Megaman` fields; later, with a second character
  or `048`).
- A helper that selects a value by the target player's character (it would be `is Megaman` under a new name).
- Moving `CharacterDef` to the engine (when a second character shows the shared shape).
- Proto Man: a separate feature item after `029g` and `024`.

## Affected areas and risks

- **The values do not move.** Each behavior receives the same `MegamanValues` constant that it reads today.
  The risk is a wrong argument (for example the water value passed for the normal value). Named arguments and
  the diff review check each one.
- **The ability order.** A wrong order changes which behavior wins on the same frame. A test fixes the order.
- **The behavior split.** Each factory must keep its closure state (its own timers and fields) per `Megaman`
  instance, as today. The timers that take a value at construction (air dash, ground slide) get it from the
  parameter at the same time as today. The bodies move without other change; the diff review checks this.
- **Construction order.** `character` is set when `Megaman` is made, before `init()`, so the ability list is
  ready when `defineBehaviorsComponent` runs.
- **Files:** new `CharacterDef.kt` and `entities/megaman/MegamanDef.kt`; `Megaman.kt`,
  `BehaviorsComponent.kt`, `MegamanValues.kt` (`TODO`), `PlayerExtensions.kt` (`TODO`), the 6 enemies and
  `SwimBooster` (`TODO`s); new tests.

## Overlap with other branches

- No other branch is active. `024` will change the same behavior factories (the weapon checks) and add the
  loadout to `CharacterDef`. `048` may turn the shared `Megaman` fields into components.

## Open questions

1. **The package of `CharacterDef`.** `players/` or a new `characters/` package. Recommendation: `characters/`.
   A player is a slot and its controller; the def describes the body that a player (or later an NPC sidekick)
   controls.
