# 081: Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`

- Status: active
- Type: feature
- Objectives: `O8`
- Depends on: `047b`, `084`; `024` only before X moves to slot 0 or gets more than the buster
- Includes: the work of `069` (dash button), folded in on 2026-11-05. Its research and decisions are in
  `069` `ITEM.md` and `NOTES.md`.
- Related: `024`, `029`, `031`, `047`, `048`, `051`, `060`, `061`, `067`, `068`, `069`, `113`, `120`, `121`, `122`
- Branches: `mega-refactor-081a-player-lifecycle` (merged, 2026-11-06), `mega-refactor-081b-x-run-jump-shoot` (active, 2026-11-06)

## Goal

Add X as a playable character for the "extra" levels that are built from X6 stages. X is an approximation of X in the
X6 reimplementation (`~/IdeaProjects/mmx-x6`). The "classic" game mode (the main Maverick game) keeps Megaman, and it
must play exactly as now.

X is the second playable character. With Mega and X as its two callers, this item adds `CharacterDef`: the data that
makes one character different from another. The two callers give the def its shape, so the shape is not a guess.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Facts are in `NOTES.md`.

- **The character:** X is a registered player from the first branch, not a stand-in that takes relayed input.
  `MegaPlayers.register()` already binds the input of the slot, and an unregistered entity has no `asPlayer`, so it
  gets no pickups and no `GATE` or `CART` contacts.
- **`CharacterDef`:** this item adds it, in core (package `characters/` recommended), with a `MegamanDef` and an X
  def. It also owns the work that `060` planned for this def:
  - the values reach the code through parameters or primitive suppliers (`FloatSupplier`) on the behavior factories
    and on the body and controller definitions; Mega's numbers stay in `MegamanValues`, and X gets his own values file;
  - the prerequisites in `029` `NOTES.md`, "What a second character needs first";
  - the places where a new player class compiles and then plays wrongly: `084` `NOTES.md`, "Silent failures for `060`";
  - the shared `onSpawn`, `onEvent`, schedule and `invincible` functions, and the signed table;
  - the readers that apply Mega's values to any player (the stomp bounce in 6 enemies, `SwimBooster`).
- **Shared behaviors:** X needs jump, wall slide, climb, air dash and crouch. They are `Megaman` receiver functions
  now (662 lines), and `O8-R1` forbids a copy, so they get parameters. The ground dash is new, for X only.
- **The move set** (from `mmx-x6` `Player.java`): stand, walk, dash and dash end, crouch, land, jump, fall, wall grab,
  wall slide, wall kick (and the dash wall kick), ladder and climb. The dash and the dash jump use the input from
  `069`. X keeps the X6 rule that the x speed does not change on a slope (`068`).
- **The values:** walk, jump, gravity, fall, dash, wall slide and wall kick values from `mmx-x6` `PlayerConst.java`,
  in world units.
- **Body, sprites, animations and sounds:** X's own body def and feet (X6 uses only the bottom-centre point for the
  floor; see `068`).
- **Weapons:** the buster with charge levels, as in X6. X has no loadout before `024`. A loadout per character comes
  from `024`.
- **Damage:** X's own damage reaction and damage negotiator.
- **Where X plays:** only in the new X6-based extra levels, not in the `GAUNTLET_*` levels. How a level picks its character is a question for this item (see
  "Open questions"). X plays in slot 0 (see "Decisions", 2026-11-05).
- **Rules per player:** each X-specific rule belongs to the player, not to a level. Examples: `SELECT` is always
  `NEXT_WEAPON`, the dash on `R`, the air dash rules and the X6 values. A level never turns one of these rules on or
  off.
- **X only, for now:** the new X6-based extra levels are playable only by X. This limit is one item of level data
  (`O7-R3`), not a condition in the code of a level or a rule. Removing it must be enough for Megaman to play these
  levels with his own rules.
- **Controls:** a keyboard or a pad with an `R` button. An NES-style pad is not enough to play as X (see `069`).
- **Not in this item:** the falcon dash, ropes and bars, armor parts. The sabre is phase 4 (see "Phases").

## Phases

Owner, 2026-11-05. Each phase can be one or more branches.

| Phase | Moves |
|---|---|
| 0 | No moves. `081a` is a refactor only: the shared player lifecycle moves out of `Megaman` (see "Branch split"). |
| 1 | Run, jump and shoot (the buster). |
| 2 | Wall slide and air dash. The air dash is like Megaman's air dash. |
| 3 | The X6 ground dash and the dash wall kick, with the dash input from `069`. |
| 4 | The Z-sabre slash. The button or the button combination is not decided. |

## Branch split

Revised 2026-11-05 (owner): `081a` is a refactor-only branch, and X plays in slot 0. This replaces the 2026-11-01
split, which put X in slot 1 behind a dev flag. Each branch `SCOPE.md` can change this list. Only `081a` is approved
as a branch; the letters after it are proposals.

| Step | Branch | Goal |
|---|---|---|
| 0 | `081a` (`mega-refactor-081a-player-lifecycle`) | Refactor only, with the contract "Megaman plays the same", and no X code. Move the player lifecycle out of `Megaman` into shared functions or components that a second player class can call. See below. |
| 1 | `081b` | Phase 1: the `MegamanX` class in slot 0, the `characters` field on `LevelDefinition` and the slot-0 swap at level start, the focus set at each level start, the `TextureAsset` entry, body, sprites, stand, walk, jump, fall and buster. The workarounds for the weapon reads in the screens. `jumpBehavior()` gets parameters. |
| 2 | phase 2 | Wall slide and air dash. `airDashBehavior()` and the wall-slide behavior get parameters. |
| 3 | phase 3 | The dash input (from `069`), the ground dash and the dash wall kick. |
| 4 | phase 4 | The sabre. |
| — | not placed | Extract `CharacterDef` from Megaman and X (open question 7). Crouch, ladder and climb (open question 10). |

### `081a`: the shared player lifecycle

The parts come from `NOTES.md`, "Silent failures for X". The branch `SCOPE.md` decides the final list.

- The spawn and destroy work: the `PLAYER_SPAWN` submit (the level reset), the `PLAYER_JUST_DIED` submit, and the
  world gravity set from the spawn data.
- The event handling: the room-transition events, the charge stop at `GATE_INIT_OPENING`, and `STUN_PLAYER`.
- The out-of-bounds kill call (`084b` moved only the test).
- The invincibility rule, `canMove`, and the behaviors lock hook.
- The `takeDamageFrom` tail (knockback, freeze, `players.onPlayerDamaged`) and `editDamageFrom`.
- The damage negotiator takes a player entity in place of `Megaman`.

Each part that another item owns gets a `TODO` that points to that item, and the move does not change its behavior:
`editDamageFrom` (`023`), the camera turn in the `direction` setter (`058`), the heart-tank max health (`059`), the
owner walk in `canBeDamagedBy` (`030`), `PLAYER_SPAWN` as a level reset (`038`), `PLAYER_JUST_DIED` (`119`), and
`AbstractHealthEntity` (`048`).

A test can drive X through `InputSource` with a fixed input sequence, with no new production code. Use this for
repeatable move checks. It does not replace a play test.

## Owner's input

2026-10-29 (verbatim): "In the \"classic\" game mode (the \"main\" Maverick game), there won't be any slopes since
they won't look right in an 8-bit game. However, the extra levels will have a new playable X character that is an
approximation of X in the X6 reimplementation (@~/IdeaProjects/mmx-x6)."

2026-10-29: the first X6-based extra level is the intro stage, or a section of it. Slopes (`068`) must support both
Megaman and X.

2026-11-01 (verbatim): "The sheet is an 8-bit rendition of Mega Man X. He will be a new playable character".

2026-11-05 (verbatim): "phase 1 is basic run, jump, and shoot; phase 2 is wall slide and air dash (similar to
Megaman's air dash); phase 3 is ground slide and wall dash (X6 moveset; need to decide how to handle dedicated dash
button, or if we can potentially include a button combo so that X is playable on NES controllers); phase 4 is Z-sabre
slash (we'll need to work out what button or button combo controls this)".

## Decisions

- (2026-10-30, from `084`) This item depends on `084` (split `Megaman` into player components). The split of
  `Megaman.kt` is a refactor with the contract "plays the same"; it must not mix with the new gameplay of this item.
- (2026-11-01, owner) X's art is the 8-bit sheet `1LMX.png` from the artist, not `mmx-x6` data. See `NOTES.md`,
  "`1LMX` sprite sheet".
- (2026-11-01, owner) X comes before Proto Man (`060`). X is the second caller of `CharacterDef`, so this item owns
  `CharacterDef`. This replaces the 2026-10-25 decision that `CharacterDef` waits for a Proto Man item. It closes open
  question 4 here and open question 5 in `069`.
- (2026-11-01, owner, from `060`) The choice between thin character classes with shared components and functions and
  one `PlayerEntity(services, def)` class moves to this item, because it now has the second real caller.
- (2026-11-01, owner) X can play in a co-op slot behind a dev flag before `060`. This is an exception to the `047`
  decision that a new character for a co-op slot waits for `060`.
- (2026-11-01, owner) The first X branch does not wait for `024`. X has only the buster and plays in slot 1 until then.
- (2026-11-01, owner) An NES-style pad is not enough to play as X. See `069`, "Decisions", and `O9-R1`.
- (2026-11-01, owner) For now, the X6-based extra levels are playable only by X. The code must not block Megaman from
  playing them later. X-specific rules are done on a player basis, not on a level basis. This closes open question 2.
- (2026-11-05, owner) `069` (dash button) is folded into this item.
- (2026-11-05, owner) A `characters` field on `LevelDefinition` gives the characters that the players can select in
  the level. The default is `setOf(MEGAMAN)`, not `null`, so no caller needs a default for `null`. The select screen
  for a set of more than one character is `113`. Only the X6 intro extra level sets X (to confirm: the owner's
  sentence was cut off). This closes open question 1.
- (2026-11-05, owner) X comes in 4 phases (see "Phases"). This closes open question 3.
- (2026-11-05, owner) Single player only, and X plays in slot 0. This replaces the 2026-11-01 decisions that X plays
  in slot 1 behind a dev flag and moves to slot 0 after `024`.
  - At level start, slot 0 gets the character from the level's `characters`. On the X6 intro extra level, slot 0 holds
    X, and Megaman is unregistered, not spawned and fully dormant. On every other level, X is dormant. Mark this setup
    with `TODO`s, because it can change later.
  - With more than 1 player on an X level, the game may throw. Co-op as X waits for `047`.
- (2026-11-05, owner) Each character has its own focus. The camera focus is set at each level start. Later, the
  character can change during a level, so the focus must then change on demand. Only the level start is in scope now.
- (2026-11-05, owner) Temporary workarounds are allowed in a branch of this item. Each one has a `TODO` at the code.
  The branch `process-log.md` and the target item document the code smell or the regression. Example: the weapon
  reads in the screens (`as Megaman` casts) can use a shared weapon part (a slice of `024`), an `is Megaman` branch,
  or both. Then `024` and `029` document the workaround.
- (2026-11-05, owner) X shares Megaman's game-state progress (heart tanks, health tanks, lives) for now, with a `TODO`.
- (2026-11-05, owner) Merge target: each `081` branch squash-merges directly into `master`, with no parent branch.
  `081a` is a refactor that keeps Megaman's play, and X appears only on a level whose `characters` names X, so each
  branch is safe to ship alone. `081b` sets `characters` on `X6_INTRO`, so that level must be on `master` before
  `081b` merges, or `081b` adds the field with no X level yet.
- (2026-11-05, owner) The first branch is `081a`, a refactor only (see "Branch split").

## Open questions

1. Closed 2026-11-05: a `characters` field on `LevelDefinition` (see "Decisions").
2. Closed 2026-11-01: the X6-based extra levels are X only for now (see "Decisions"). The existing `GAUNTLET_1` and
   `GAUNTLET_2` stay Megaman levels.
3. Closed 2026-11-05: 4 phases (see "Phases").
4. Closed 2026-11-01: X comes first (see "Decisions").
5. The burst frames in `damaged` and `air_damaged`: do they stay in these strips, or move to their own strips, drawn
   as an effect over X? See `NOTES.md`, "`1LMX` sprite sheet".
6. Is `CharacterDef` moved to the engine once two characters show the shared shape? (from `060`)
7. Thin classes or one `PlayerEntity(def)`? Decide this before any swap work. One class needs a way to rebuild on a change of def, because `init()` is not safe to repeat, and it does not fit a per-class pool for `121` clones. A level that picks its character needs a spawn-time `setFocus` in place of the once-only `MegaLevelScreen.kt:337` (`047` `NOTES.md`, "Can `Megaman` be pooled?"). (from `047`, 2026-11-02)
8. Phase 2: which input starts X's air dash? Megaman's air dash is `A` in the air, or `SELECT` when
   `selectButtonAction` is `AIR_DASH` (`AirDashBehavior.kt:38-46`). `069` decided that for X, `A` is only jump and
   wall kick, `R` is the only dash input, and `SELECT` is always `NEXT_WEAPON`. If phase 2 uses Megaman's input, phase
   3 changes X's controls when `R` arrives.
9. Phase 3: a dash button, or a button combination so that X is playable on an NES-style pad? This reopens the
   2026-11-01 decision that an NES-style pad is not enough for X (see "Decisions", `069` "Decisions" and `O9-R1`).
   `069` dropped `DOWN` + `A`, because a dash jump needs the dash held when the jump starts.
10. Which phase gets crouch, ladder and climb? They are in the move set above, but not in "Phases".
11. Phase 4: which button or button combination starts the sabre slash?

## Sources

- `NOTES.md` in this folder.
- `029` `NOTES.md`, "`CharacterDef` design" and "What a second character needs first".
- `mmx-x6`: `src/main/java/mmx/x6/game/player/Player.java`, `PlayerConst.java`.
