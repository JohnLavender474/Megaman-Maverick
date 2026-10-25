# Scope: mega-refactor-029g-character-def

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

The player's 8 behaviors move out of one 787-line function into 8 named factory functions, with no change in
their bodies. Tests fix the behavior order and show that each `Megaman` gets its own behavior objects. This makes
the behaviors easier to read and a smaller lift to reuse later. `CharacterDef` is not added: the design goes to
the item notes for the Proto Man item, which will have two callers. The game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029g` `character-def`: `CharacterDef`, movement values table, ordered ability list, thin player
  base. Weapon couplings in behaviors overlap `024`". This branch takes only the preparation for the ordered
  ability list: the split of the behaviors (decision 2026-10-25, after the review).
- Decision 2026-10-23: no `IPlayer` interface on the player entity; roles come from components (`048`). So no
  player base class.
- The rule "write code only when production code calls it": `CharacterDef`, a values table and factory
  parameters would each have one caller and a guessed shape (the `ComponentKey` lesson). They wait for a second
  character.

## Current state

The full scan is in `docs/NOTES.md` ("Scan", on `ab5f8cd31`) and the review in `docs/NOTES.md` ("Review").
Summary, with `B/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/BehaviorsComponent.kt`:

- `B/:43-787` (`defineBehaviorsComponent`) builds the 8 behaviors as local values that close over `Megaman`,
  then adds them in a fixed order (`:777-784`): `WALL_SLIDING`, `SWIMMING`, `JUMPING`, `AIR_DASHING`,
  `GROUND_SLIDING`, `CROUCHING`, `CLIMBING`, `JETPACKING`. The engine runs them in that order (`OrderedMap`).
- The declaration order is not the insertion order: `crouch` is declared at `:373`, before `groundSlide` at
  `:418`, but it is added after it (`:781-782`).
- The behaviors connect through `isBehaviorActive(BehaviorType.X)`, through shared fields on `Megaman`
  (`airDashConsumed`, `jumpVel`, `wallJumpVel`, `swimVel`, timers) and through `aButtonTask`.
- Two behaviors build timers when they are made: the air dash (`:257-258`, and `:298` resets the duration in
  `init`) and the ground slide (`:420-422`). Each behavior object keeps its own state.
- The file also holds the six `MEGAMAN_*_BEHAVIOR_TAG` constants and `MEGAMAN_LADDER_MOVE_OFFSET` (`:34-41`).
- No test covers the behaviors or their order.

## In scope

1. **The 8 factory functions.** Each behavior becomes one `internal` factory function on `Megaman` that returns
   its `IBehavior`, with no parameters. The body of each behavior moves with no change (only the indentation and
   the wrapping function change). Each factory goes in its own file in a `behaviors/` folder in the player
   package, with the constants that only it uses. Shared constants stay where their other readers can reach them.
2. **`defineBehaviorsComponent`** stays the one explicit, ordered list: it calls the 8 factories and adds the
   behaviors in today's order. One `TODO` there (`024`): the weapon checks inside the factories belong to the
   loadout.
3. **`MegaPlayer.megaman` `TODO`:** it no longer names `029g`. The loadout reads go with `024`; the `running` and
   sprite reads wait for a second character (the Proto Man item).
4. **Tests:**
   - the order of the behavior keys in a built `Megaman`'s `BehaviorsComponent` (as `MegamanHeartTankTest`
     builds a `Megaman`);
   - two `Megaman` objects get different behavior objects for each key.

New APIs and their first callers:

| API | First callers |
|---|---|
| The 8 behavior factory functions | `defineBehaviorsComponent` |

## Out of scope

- `CharacterDef`, `MegamanDef`, a `character` field (to the Proto Man item, with two callers; design in the item
  `NOTES.md`).
- Factory parameters or suppliers, and a values table. `MegamanValues` does not change.
- The `direction` setter, the body and controller values, the timers made at construction in `Megaman.kt`.
- A player base class (`048` decision).
- The weapon checks and the loadout (`024`); the input reads (`031`); the enhancement reads; the level checks;
  the shared camera calls (`047`).
- The readers outside the player (the stomp bounce, `SwimBooster`).
- Any change to a behavior body, including names inside it.

## Affected areas and risks

- **The behavior order.** If the split keeps the declaration order, `GROUND_SLIDING` and `CROUCHING` swap. The
  order test catches this.
- **Closure state.** Each factory call must make a new behavior object with its own timers and fields, as the
  local values do today. A factory that returned a shared object would not show with one `Megaman`, only in
  co-op. The two-instance test catches this.
- **Timers made at construction.** The air-dash and ground-slide timers are made when the factory runs, which is
  when `defineBehaviorsComponent` runs today. No change in when the values are read.
- **Bodies unchanged.** The diff review compares each moved body with the original (a whitespace-insensitive
  diff).
- **Files:** `BehaviorsComponent.kt`; 8 new files in the player package; `PlayerExtensions.kt` (`TODO`); new test.

## Overlap with other branches

- No other branch is active. `024` will change the factory bodies (the weapon checks); `031` the input reads;
  `048` may turn the shared `Megaman` fields into components. Smaller files make those changes easier.

## Open questions

None.
