# 060: Proto Man as a playable character

- Status: active
- Type: feature
- Objectives: `O8`
- Depends on: `081a`, `084`; `024` only for `060h`; `081` (`CharacterDef`) only for `060i`
- Related: `029`, `031`, `047`, `048`, `146`
- Branches: `mega-refactor-060a-shared-run-body` (merged, 2026-11-10), `mega-refactor-060b-protoman-move` (active, paused after planning, 2026-11-10), `mega-refactor-060j-mega-player` (merged, 2026-11-11)

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

2026-11-10 (owner, `060j` scope): "Keep in mind that the base class should be able to support Proto Man and other
similar playable characters (Bass, Axl, Zero, etc.) in the future."

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

- (2026-11-10, owner) Merge target: each branch of this item squash-merges directly into `master`.
- (2026-11-10, owner) `060a` is a refactor-only branch, as `081a` was for X. Mega's run, body and momentum are
  `Megaman` receiver functions, so a thin `Protoman` cannot call them, and a copy breaks `O8-R1`. No other item covers
  this move (`084` moved the state, not these functions; the `081` body and controller parameters are in its "not
  placed" row). The first Proto Man code is in `060b`. The branch started as `mega-refactor-060a-proto-run-jump`
  with no commits, and was renamed to `mega-refactor-060a-shared-run-body`.
- (2026-11-10, owner) `146` (a marker `IPlayer` interface) starts in a separate session. When `146` merges into
  `master`, `060a` (if not merged yet) and every later `060` branch take it in and use its player type (for example
  `Protoman` implements `IMegaPlayer`). (Replaced: `146a` D1 made no core `IMegaPlayer`; the type is the engine
  `IPlayer`. See the `MegaPlayer` decision below.)

- (2026-11-10, owner, from the `IPlayer` review, `temp/060-iplayer-review/report.md`) An abstract `MegaPlayer` base class:
  `MegaPlayer : AbstractHealthEntity, IPlayer, IBodyEntity, IFaceable, IDirectional`. It owns the shared lifecycle
  wiring and the shared components, and the shared `players/` functions take a `MegaPlayer` receiver in place of the
  generic bounds. `Megaman`, X and `Protoman` extend it. `O8-R1` changes to allow it ("thin": only what every player
  class shares). It is built in a refactor-only branch, `060j`, which merges before `060b` goes on. The 2 last
  `playerSlot!!` reads (item `148`) go in the work on `060` (owner); `060j` does them, because they move to the
  `MegaPlayer` receiver.

## Proposed phases

Not approved. One branch per phase. `060a` is a refactor only. `060b` to `060e` build the MM9/MM10 baseline to
completion. Then `060f` and `060g` take Proto Man to his Maverick version. His move set, behaviors, look and feel are
as refined as possible before co-op (`060i`) (owner, 2026-11-10).

| Phase | Content | Needs |
|---|---|---|
| `060a` | Refactor only, contract "Megaman plays the same", no Proto Man code: Mega's run, body (feet, gravity table, water, ice) and momentum move from `Megaman` receiver functions into `players/` functions with parameters | `081a`, `084` |
| `060j` | Refactor only, contract "Megaman and X play the same": the `MegaPlayer` base class; the shared functions take it; the 2 last `playerSlot!!` reads. Runs between `060a` and `060b` (added later, so its letter is out of order) | `060a` |
| `060b` | `Protoman` in slot 0 on a test level: idle, step, walk, jump, fall; sliced atlas | `060j` |
| `060c` | Buster: shoot poses, Proto Buster, charge and Proto Strike | |
| `060d` | Slide and ladder, with Mega's rules | |
| `060e` | Proto Shield; his damage reaction, stun, collapse, teleport in and out | |
| `060f` | The "Proto Sabre" (working name) | |
| `060g` | Experiments: settings, moves and behaviors, from the sheet scan and play tests. Can be more than one branch | |
| `060h` | Weapon poses and the loadout: Proto Man's variants of the special weapons | `024` |
| `060i` | `CharacterDef` as the third caller (if `081` has not done it); co-op with mixed characters | `047` |

Sprite source: a fan sheet, "Proto Man (Expanded) (Inti Creates 8-bit-Style)", sprites by Hansungkee and others (the
credits are on the sheet). Keep the credits.

## Open questions

- Does Proto Man share the slide and ladder rules (X6 shares the step routines)?
- (`060h`) How does Proto Man use each special weapon: Mega's version with his poses, or his own variant? Many
  questions are open, so this phase comes late.
- (`060f`) What is the Proto Sabre: its input, its slash kinds, and how much it shares with X's Z-sabre (`081e`,
  `141`)?
- Moved to `113`: how does the player pick a character, and can each co-op slot pick its own (`047`)?

## Sources

- `NOTES.md` in this folder.
- `081` `ITEM.md`.
- Item `029` `NOTES.md`, "`CharacterDef` design".
