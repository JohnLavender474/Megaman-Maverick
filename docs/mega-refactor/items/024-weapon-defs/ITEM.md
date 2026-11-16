# 024: One `WeaponDef` per weapon; loadout per character

- Status: active
- Type: refactor
- Objectives: `O8`, `O1`
- Depends on: `029`
- Related: `007`, `023`, `025`, `040`, `084`
- Branches: `mega-refactor-024a-weapon-defs` (merged 2026-11-15); `024b` next (no branch yet)

## Goal

Put the data for each weapon in one place, instead of `when` blocks in at least 8 files.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `WeaponDef` holds cooldown, costs, charge rules, fire behavior, HUD, icon and animation keys, ability blocks and damage tags.
- A registry with stable ids. The HUD, pause grid and save use the loadout.
- Remove the weapon `when`s from the handler, behaviors, controller, HUD and pause screen.
- Rewards come from the level manifest through `CharacterDef.unlocks`.

## Workarounds from `081b` (2026-11-06)

X (`MegamanX`, item `081`) has only the buster. Until this item gives each character a loadout, these places check
`is Megaman`, with a `TODO` that names this item. Each one is a code smell that this item removes (`O8-R2`: 5 lines).

| Place | What it does for a player that is not `Megaman` |
|---|---|
| `PlayerStatsHandler`, the weapon bar supplier | no weapon bar |
| `MegaLevelScreen`, `ADD_WEAPON_ENERGY` | the pickup does nothing |
| `LevelPauseScreen`, the ammo bars and the weapon select | 0 ammo, and no weapon change |
| `MegamanMaverickGame.resetPlayerProgress` | resets the weapon of each `Megaman` in `PlayerRoster` |
| `EndLevelEventHandler`, the beam-out | uses the buster beam art (item `081` owns X's beam art) |

## Decisions

- (2026-11-14, owner) Merge target: each branch of this item squash-merges directly into `master`.
- (2026-11-14, proposed at item start) Split: `024a` refactor only (Megaman's weapons as `WeaponDef`s, plays the
  same); `024b` a loadout per character (removes the `is Megaman` workarounds). `060h` builds on them. The `024a`
  brief confirms it.

## Owner's input

Answer to review item 23 (2026-10-22, verbatim):

> yes

## Open questions

- Is progress (bosses beaten) shared across characters, as in X6?
- Can two characters share a `WeaponDef` with different sprites?
- Must saved weapon ids stay readable from the old save?

## Sources

- `NOTES.md` in this folder.
