# 060: Proto Man as a playable character

- Status: idea
- Type: feature
- Objectives: `O8`
- Depends on: `024`, `081`, `084`
- Related: `029`, `031`, `047`, `048`
- Branches: none yet

## Goal

Add Proto Man as a third playable character, after X (`081`). He uses the shared player parts and `CharacterDef`
from `081`, as its third caller.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A Proto Man def on `CharacterDef`, and his own values file.
- His own sprites, animations and sounds, loadout (`024`) and damage reaction.
- Any change that `CharacterDef` needs for a third caller. `081` owns the first shape, and the work that was planned
  here for `CharacterDef`.
- How the game picks the character: `113`.

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
  (2026-11-01: this choice moved to `081`.)
- (2026-10-31, owner, from `084`) `AButtonTask` has no component until this item shows a need. `IEventListener` stays on the player class, and this item
  shares the handler body as one function.
- (2026-11-01, owner, from `081`) X comes before Proto Man. `CharacterDef` moves to `081`, and this item depends on
  `081`. This replaces the 2026-10-25 input above that `CharacterDef` waits for this item.

## Open questions

- Does Proto Man share the slide and ladder rules (X6 shares the step routines)?
- Moved to `113`: how does the player pick a character, and can each co-op slot pick its own (`047`)?

## Sources

- `NOTES.md` in this folder.
- `081` `ITEM.md`.
- Item `029` `NOTES.md`, "`CharacterDef` design".
