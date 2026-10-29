# 081: Playable X for the extra levels, ported from `mmx-x6`

- Status: idea
- Type: feature
- Depends on: `024`, `069`; `CharacterDef` (`060`, or this item if X comes first)
- Related: `029`, `031`, `048`, `051`, `060`, `061`, `067`, `068`
- Branches: none yet

## Goal

Add X as a playable character for the "extra" levels that are built from X6 stages. X is an approximation of X in the
X6 reimplementation (`~/IdeaProjects/mmx-x6`). The "classic" game mode (the main Maverick game) keeps Megaman, and it
must play exactly as now.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Facts are in `NOTES.md`.

- **The character:** X is a second player character next to Megaman, through `CharacterDef`. If `060` (Proto Man) is
  not done first, this item adds `CharacterDef` with Mega and X as its two callers.
- **The move set** (from `mmx-x6` `Player.java`): stand, walk, dash and dash end, crouch, land, jump, fall, wall grab,
  wall slide, wall kick (and the dash wall kick), ladder and climb. The dash and the dash jump use the input from
  `069`. X keeps the X6 rule that the x speed does not change on a slope (`068`).
- **The values:** walk, jump, gravity, fall, dash, wall slide and wall kick values from `mmx-x6` `PlayerConst.java`,
  in world units.
- **Body, sprites, animations and sounds:** X's own body def and feet (X6 uses only the bottom-centre point for the
  floor; see `068`).
- **Weapons:** the buster with charge levels, as in X6. A loadout per character comes from `024`.
- **Damage:** X's own damage reaction and damage negotiator.
- **Where X plays:** only in the extra levels. How a level picks its character is a question for this item (see
  "Open questions").
- **Not in the first version:** the sabre, the falcon dash, ropes and bars, armor parts.

## Owner's input

2026-10-29 (verbatim): "In the \"classic\" game mode (the \"main\" Maverick game), there won't be any slopes since
they won't look right in an 8-bit game. However, the extra levels will have a new playable X character that is an
approximation of X in the X6 reimplementation (@~/IdeaProjects/mmx-x6)."

2026-10-29: the first X6-based extra level is the intro stage, or a section of it. Slopes (`068`) must support both
Megaman and X.

## Open questions

1. How does a level pick its character: a `LevelDefinition` field, the `LevelType.X_LEVEL` from `069`, or a choice on
   the stage select?
2. Can Megaman also play the extra levels, or are they X only?
3. Which X6 moves are in the first version, and which wait (sabre, falcon dash, ropes, bars, armor)?
4. Does X come before or after Proto Man (`060`)? The first of the two adds `CharacterDef`.
5. X's art: X6 art is 16-bit. Is X drawn as a new sprite in the Maverick style, or taken from `mmx-x6`? (The `mmx-x6`
   data cannot be committed to this repo; see `docs/mega-refactor/CLAUDE.md`, "Inspiration".)

## Sources

- `NOTES.md` in this folder.
- `029` `NOTES.md`, "What a second character needs first".
- `mmx-x6`: `src/main/java/mmx/x6/game/player/Player.java`, `PlayerConst.java`.
