# 060: Proto Man as a second playable character, with `CharacterDef`

- Status: idea
- Type: feature
- Objectives: `O8`
- Depends on: `024`, `084`
- Related: `029`, `031`, `047`, `048`, `084`
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
  transitions); a damage reaction; the remaining Mega-named API (`IHazard.getDamageToMegaman`).
- The 6 places where a new player class compiles and then plays wrongly: `084` `NOTES.md`, "Silent failures for `060`".
- The 8 behaviors stay on the `Megaman` receiver until this item and `024`. This item also owns `CharacterDef`, the shared `onSpawn`,
  `onEvent`, schedule and `invincible` functions, and the signed table.
- Readers that apply Mega's values to any player (the stomp bounce in 6 enemies, `SwimBooster`) read the
  contact player's own value (their `TODO`s point here).
- How the game picks the character (a select screen, a dev flag) is open.

## Owner's input

2026-10-22, answer to review item 28 (verbatim, from item `029`):

> yes, there needs to be an `IPlayer` contract so that I can create as many different playable characters as possible (Mega, Proto, etc.) They should all adhere to a minimal contract, but being able to customize behaviors, physics, etc. for each should be easy

2026-10-25 (from the `029g` planning): Proto Man is a separate feature item after `029g` and `024`; `CharacterDef`
waits for it.

## Decisions

- (2026-10-30, from `084`) This item depends on `084` (split `Megaman` into player components). The split of
  `Megaman.kt` is a refactor with the contract "plays the same"; it must not mix with the new gameplay of this item.
- (2026-10-31, owner, from `084`) The working direction is a thin `Megaman` and `Protoman` that share components and functions. The choice between that
  and one `PlayerEntity(services, def)` class stays open for this item, because it has the second real caller. See `084` `NOTES.md`.
- (2026-10-31, owner, from `084`) `AButtonTask` has no component until this item shows a need. `IEventListener` stays on the player class, and this item
  shares the handler body as one function.

## Open questions

- Does Proto Man share the slide and ladder rules (X6 shares the step routines)?
- How does the player pick a character, and can each co-op slot pick its own (`047`)?
- Is `CharacterDef` moved to the engine once two characters show the shared shape?

## Sources

- `NOTES.md` in this folder.
- Item `029` `NOTES.md`, "`CharacterDef` design".
