# 081: Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`

- Status: planned
- Type: feature
- Objectives: `O8`
- Depends on: `047b`, `084`; `069` from step 2 of "Branch split"; `024` only before X moves to slot 0 or gets more than
  the buster
- Related: `024`, `029`, `031`, `047`, `048`, `051`, `060`, `061`, `067`, `068`, `069`, `113`, `120`, `121`, `122`
- Branches: none yet

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
  "Open questions"). During development, X plays in slot 1 behind a dev flag.
- **Rules per player:** each X-specific rule belongs to the player, not to a level. Examples: `SELECT` is always
  `NEXT_WEAPON`, the dash on `R`, the air dash rules and the X6 values. A level never turns one of these rules on or
  off.
- **X only, for now:** the new X6-based extra levels are playable only by X. This limit is one item of level data
  (`O7-R3`), not a condition in the code of a level or a rule. Removing it must be enough for Megaman to play these
  levels with his own rules.
- **Controls:** a keyboard or a pad with an `R` button. An NES-style pad is not enough to play as X (see `069`).
- **Not in the first version:** the sabre, the falcon dash, ropes and bars, armor parts.

## Branch split

Proposed 2026-11-01. Not approved. Each branch `SCOPE.md` can change this list. `047b` comes first, because
`MegaPlayers.targetFor` gives slot 0 today, so enemies would not target X in slot 1. `069` comes after step 1, because
only X reads its button, and code needs a real caller.

| Step | Goal |
|---|---|
| 1 | `081a`: X in slot 1 behind a dev flag: the X class, `TextureAsset` entry, body, sprites, stand, walk, jump, fall and buster. `jumpBehavior()` gets parameters. Slot 1 avoids the `as Megaman` casts in the 4 screens that read slot 0. `MegamanMaverickGame.kt:821` casts every slot, so it changes here. |
| 2 | `069`: the `R` button, the ground dash and the air dash for X. `airDashBehavior()` gets parameters. |
| 3 | The other shared behaviors get parameters (wall slide, climb, crouch), and X gets these moves. |
| 4 | Extract `CharacterDef` from Megaman and X. |
| 5 | After `024`: X moves to slot 0, with a loadout. |

Limits of slot 1:

- It needs a gamepad, because slot 1 uses only the last connected pad.
- The `047` notes say that the camera follows the primary player. This is not checked in the code. If it is true, X
  cannot go far from slot 0 until step 5.
- The 13 gameplay `Megaman::class` lines in 9 files stay silent failures for X until a branch changes them. For
  example, the `SwinginJoe` mask cannot hit X.

A test can drive X through `InputSource` with a fixed input sequence, with no new production code. Use this for
repeatable move checks. It does not replace a play test.

## Owner's input

2026-10-29 (verbatim): "In the \"classic\" game mode (the \"main\" Maverick game), there won't be any slopes since
they won't look right in an 8-bit game. However, the extra levels will have a new playable X character that is an
approximation of X in the X6 reimplementation (@~/IdeaProjects/mmx-x6)."

2026-10-29: the first X6-based extra level is the intro stage, or a section of it. Slopes (`068`) must support both
Megaman and X.

2026-11-01 (verbatim): "The sheet is an 8-bit rendition of Mega Man X. He will be a new playable character".

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

## Open questions

1. How does a level pick its character? (2026-11-01: `069` no longer adds `LevelType.X_LEVEL`.)
   - Proposed: a `characters` field on `LevelDefinition`, with the default `setOf(MEGAMAN)`. An X6-based extra level
     sets `setOf(X)`. This is the first real caller of the field. The select screen for a set of more than one
     character is `113`, not this item.
2. Closed 2026-11-01: the X6-based extra levels are X only for now (see "Decisions"). The existing `GAUNTLET_1` and
   `GAUNTLET_2` stay Megaman levels.
3. Which X6 moves are in the first version, and which wait (sabre, falcon dash, ropes, bars, armor)?
4. Closed 2026-11-01: X comes first (see "Decisions").
5. The burst frames in `damaged` and `air_damaged`: do they stay in these strips, or move to their own strips, drawn
   as an effect over X? See `NOTES.md`, "`1LMX` sprite sheet".
6. Is `CharacterDef` moved to the engine once two characters show the shared shape? (from `060`)
7. Thin classes or one `PlayerEntity(def)`? Decide this before any swap work. One class needs a way to rebuild on a change of def, because `init()` is not safe to repeat, and it does not fit a per-class pool for `121` clones. A level that picks its character needs a spawn-time `setFocus` in place of the once-only `MegaLevelScreen.kt:337` (`047` `NOTES.md`, "Can `Megaman` be pooled?"). (from `047`, 2026-11-02)

## Sources

- `NOTES.md` in this folder.
- `029` `NOTES.md`, "`CharacterDef` design" and "What a second character needs first".
- `mmx-x6`: `src/main/java/mmx/x6/game/player/Player.java`, `PlayerConst.java`.
