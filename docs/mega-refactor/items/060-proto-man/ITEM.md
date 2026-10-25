# 060: Proto Man as a second playable character, with `CharacterDef`

- Status: idea
- Type: feature
- Depends on: `024`
- Related: `029`, `031`, `047`, `048`
- Branches: none yet

## Goal

Add Proto Man as a second playable character. With Mega and Proto as its two callers, add `CharacterDef`: the
data that makes one character different from another (values, ability set and order, and later the body,
sprites, sounds, loadout and damage reaction). The second character shows which parts are really shared, so the
def gets its shape from two real callers, not from a guess.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `CharacterDef` in core (package `characters/` recommended), with `MegamanDef` and a Proto Man def.
- The values reach the code through parameters or primitive suppliers (`FloatSupplier`) on the behavior
  factories (`entities/megaman/behaviors/`, from `029g`) and on the body and controller definitions. Mega's
  numbers stay in `MegamanValues`; Proto Man gets his own values file.
- The prerequisites in item `029` `NOTES.md`, "What a second character needs first": jump, gravity and run
  values; a body def; sprites, animations and sounds; a loadout (`024`); an ability-set contract (the
  `BehaviorType` keys that outside code expects, `setBehaviorAllowed` on a missing key, the `aButtonTask`
  transitions); a damage reaction; the remaining Mega-named APIs (`MegaPlayer.megaman`,
  `IHazard.getDamageToMegaman`).
- Readers that apply Mega's values to any player (the stomp bounce in 6 enemies, `SwimBooster`) read the
  contact player's own value (their `TODO`s point here).
- How the game picks the character (a select screen, a dev flag) is open.

## Owner's input

2026-10-22, answer to review item 28 (verbatim, from item `029`):

> yes, there needs to be an `IPlayer` contract so that I can create as many different playable characters as possible (Mega, Proto, etc.) They should all adhere to a minimal contract, but being able to customize behaviors, physics, etc. for each should be easy

2026-10-25 (from the `029g` planning): Proto Man is a separate feature item after `029g` and `024`; `CharacterDef`
waits for it.

## Open questions

- Does Proto Man share the slide and ladder rules (X6 shares the step routines)?
- How does the player pick a character, and can each co-op slot pick its own (`047`)?
- Is `CharacterDef` moved to the engine once two characters show the shared shape?

## Sources

- `NOTES.md` in this folder.
- Item `029` `NOTES.md`, "`CharacterDef` design".
