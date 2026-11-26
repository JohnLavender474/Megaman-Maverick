# Scope: mega-refactor-077h-reactor-monkey-art

What this branch changes and why, and its contract. This is Claude's record.

- Status: draft (first pass, art drafts and fight ideas only; not yet approved by the owner)
- Approved: no

## Goal

Start the upgrade of the `ReactorMonkey` mini boss (Reactor Man's stage, 3 spawns in `ReactorMan_v3.tmx`) from one
repeated catch-and-throw loop to a fight with phases, an intro and a death. This branch makes draft art and a design
note. No code reads the art yet (a later branch, `077i`, codes the fight on the accepted art).

Owner request (2026-11-26, from Claude on the web): "introduce phases to the boss. When he gets mad, he might beat
his chest like an ape. He might also have a red mad face with steam coming out of his ears in a cartoonish fashion."
Plus: draw first drafts of the new animations (Fable art agents) and ideate on a more dynamic fight (Opus agent).

## Item

`077`. Branch name follows `077f-glacier-man-art` (art first, fight code in a later branch). The owner did not name an
ID or letter: `077h` is the next free letter of item `077`.

## Current state (`master` `19e713e`)

- `ReactorMonkey.kt`: states `STAND` and `THROW` only; a random throw delay (interpolated by health ratio, quicker on
  hard); `ReactorMonkeyBall` thrown at the player's position with a calculated jump impulse; no phases, no intro frames
  (the sprite is hidden during `BossPhase.ENTRANCE`), no dying animation (the shared defeat explosion and alpha fade).
- Frames: `Bosses1/ReactorMonkey/{stand,stand_damaged,throw,throw_damaged}` (160 x 160 cells, drawn at 320 x 320 game px).

## In scope

- Draft strips (in `docs/drafts/`, from `temp/077h-reactor-monkey-art/`): `intro_drop`, `intro_land`, `intro_roar`,
  `dying`, `collapse`, `dead`, `chest_beat`, `mad_stand`, `mad_throw` (+ `_damaged`), `steam`, `steam_jet`, and an
  optional enraged ball. Final list and timings: `docs/NOTES.md`.
- Design ideas for the fight: `docs/IDEAS.md`.

## Out of scope

- Code, atlases (no repack, no new `TextureAsset`), `README` credits (added when art goes into the game), sounds.
- Changes to the existing frames.

## Contract

- Behavior change: none. No production code, asset or atlas file changes in this branch.
- Check: `git diff --stat master` lists only files under this branch folder.

## Decisions made without the owner

- The Fable confirmation of `art.md` was taken from the owner's request to use Fable art agents.
- Branch name and letter chosen by Claude (see "Item").
- Drafts are committed as images under the branch `docs/drafts/` (not `temp/`, which is never committed), so that the
  owner can open them from the pull request.
