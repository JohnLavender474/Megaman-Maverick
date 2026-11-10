# 060: Proto Man as a playable character

- Status: idea
- Type: feature
- Objectives: `O8`
- Depends on: `081a`, `084`; `024` only for `060g`; `081` (`CharacterDef`) only for `060h`
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

- (2026-11-10, owner) Code shape: a thin `Protoman` class that calls Megaman's shared behaviors and components, with
  his own values file, sprites, shield and Proto Strike. Not a separate move set as for X, and not one
  `PlayerEntity(def)`.
- (2026-11-10, owner) Phase 1 puts Proto Man into a level through a new test `LevelDefinition` entry with
  `characters = setOf(PROTOMAN)`, as `X6_INTRO` does for X. No launcher option; real selection is `113`.
- (2026-11-10, owner) Feel: the MM9/MM10 rules first. Later phases move him toward a Maverick version. Scan the
  sprite sheet for unique move sets; example: the "flame sword" poses can become a slash like X's Z-sabre.

## Proposed phases

Not approved. One branch per phase. `060a` to `060d` build the MM9/MM10 baseline to completion. Then `060e` and
`060f` take Proto Man to his Maverick version. His move set, behaviors, look and feel are as refined as possible
before co-op (`060h`) (owner, 2026-11-10).

| Phase | Content | Needs |
|---|---|---|
| `060a` | `Protoman` in slot 0 on a test level: idle, step, walk, jump, fall; sliced atlas | `081` |
| `060b` | Buster: shoot poses, Proto Buster, charge and Proto Strike | |
| `060c` | Slide and ladder, with Mega's rules | |
| `060d` | Proto Shield; his damage reaction, stun, collapse, teleport in and out | |
| `060e` | The "Proto Sabre" (working name) | |
| `060f` | Experiments: settings, moves and behaviors, from the sheet scan and play tests. Can be more than one branch | |
| `060g` | Weapon poses and the loadout: Proto Man's variants of the special weapons | `024` |
| `060h` | `CharacterDef` as the third caller (if `081` has not done it); co-op with mixed characters | `047` |

Sprite source: a fan sheet, "Proto Man (Expanded) (Inti Creates 8-bit-Style)", sprites by Hansungkee and others (the
credits are on the sheet). Keep the credits.

## Open questions

- Does Proto Man share the slide and ladder rules (X6 shares the step routines)?
- (`060g`) How does Proto Man use each special weapon: Mega's version with his poses, or his own variant? Many
  questions are open, so this phase comes late.
- (`060e`) What is the Proto Sabre: its input, its slash kinds, and how much it shares with X's Z-sabre (`081e`,
  `141`)?
- Moved to `113`: how does the player pick a character, and can each co-op slot pick its own (`047`)?

## Sources

- `NOTES.md` in this folder.
- `081` `ITEM.md`.
- Item `029` `NOTES.md`, "`CharacterDef` design".
