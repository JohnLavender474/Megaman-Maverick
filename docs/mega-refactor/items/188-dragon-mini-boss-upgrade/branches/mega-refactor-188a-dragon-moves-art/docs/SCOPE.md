# Scope: mega-refactor-188a-dragon-moves-art

What this branch changes and why, and its contract. This is Claude's record. If the scope changes later, update this file.

- Status: approved (art-only; the owner directed each round in chat)
- Approved: 2026-11-26 (owner, in chat: Opus ideation first, then a Fable art agent; later rounds: dangling legs, the other airborne strips)

## Goal

Design and art only. Give the `MechaDragon` mini-boss legs, and draw the frames for its new attacks. No code and no atlas change: the code branch `188b` wires the art in.

## Item

`188` Dragon mini-boss upgrade (legs and new attacks). This branch is `188a`: step 1 (ideate new moves) and step 2 (redraw the frames with legs and draw the new attack frames) of the owner's request in `ITEM.md`.

## Current state

- `MechaDragon` (`core/.../entities/bosses/MechaDragon.kt`) hovers, spits 1 to 3 aimed `SpitFireball`s, and charges across the room. It has no legs. 30 HP, 1 phase, a 2x5-tile `ABSTRACT` body, an 8x8-tile sprite cut from 128x128 cells.
- The arena is `mini_boss_room` under `room8` of `INFERNO_MAN`: a 4-wide center block, two 2-wide lava pits, and two side ledges.
- Findings of the ideation (`NOTES.md`): `charging.png` was a byte copy of `fly.png`; `fire1` frames 11 to 16 never play; the low charge lane may be undodgeable.

## In scope

- `NOTES.md`: the ideation summary (the standing sweeping fire, the stomp landing and takeoff, the chin chomp, the aimed volley, the charge) and the art layouts.
- Frames in `assets/sprites/frames/Bosses1/MechaDragon/`:
  - every existing strip redrawn with hind legs (airborne strips: legs dangle, in cells 128x144; `defeated`: limp legs; `frozen`: blue recolor);
  - new grounded strips (`land`, `stand`, `breath_windup`, `breath_sweep`, `breath_end`, `chomp`; 128x128 cells, feet on the bottom row) and `flame_puff`;
  - `fire1` trimmed from 16 to the 10 cells that play (5 cols x 2 rows).
- `ANCHORS.json`: the mouth pixel and head box of the sweep and chomp cells.

## Out of scope

- Code, atlases, the TMX map, sounds, the README credits. TexturePacker is not installed in the cloud, so the atlases are not repacked. The code branch `188b` repacks (or adds the atlas entries), wires the new states and the `MechaDragonFlame` projectile, and adds the credit lines (`art.md`: when the art goes into the game).

## Affected areas and risks

- Only PNG files in the frames folder and docs of this branch. The game does not read the frames folder at run time (it reads the atlases in `assets/sprites/sprite_sheets/`), so the game is unchanged.
- Risk: the strips and the atlas disagree until `188b` repacks. The airborne strips are 128x144 cells and the grounded ones 128x128, so `188b` sizes each animation from its own strip.
- The dragon's colors (green, orange, blue, white) are not within the `art.md` palette rule (2 sprite palettes, 3 colors per 8x8 area). The old art was not either. The owner chooses whether to keep the look (see the PR).

## Overlap with other branches

None: no other active branch touches `Bosses1/MechaDragon/`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| Always-on rules (`O1-R5`, `O2-R2`, `O5-R1`, `O6-R1`) | no effect | No code. |
| `O4` (deeper enemies and bosses) | helps | Frames for a richer mini-boss fight. |

## Open questions

- Keep the dragon's old color scheme (not within the 2-palette rule), or redo it strictly? (Recommendation: keep it for now.)
- Is "Inferno Dragon - Ansimuz" in the README credits this dragon?
- Opus picks that the owner has not overruled: the dragon stands on its side ledge; 1 health phase with a panic attack below 25%; high charge lane only; `fire1` trimmed to 10 cells.

## Steps

Art rounds (agents), then a draft PR:

1. Opus ideation (`temp/dragon-moves/IDEAS.md`, summarized in `NOTES.md`). Done.
2. Fable round 1: legs on every cell, new strips. Done.
3. Fable round 2: `fly` and `charging` legs dangle (cell 128x144). Done.
4. Fable round 3: the other airborne strips get the dangling legs at 144 rows.
5. Draft PR.

# Contract

## Automated checks

- [ ] The PNG checks of the art rounds (`temp/dragon-art/`, not committed): sizes, alpha, the NES master palette. Hard fails: 0.
- [ ] CI of the PR passes (`.github/workflows/ci.yml`). The Gradle checks may not run in the cloud (`claude-web.md`), and the branch changes no code or atlas, so CI is the only automated check.

## Manual playthrough

None: the art is not in the game yet. The owner checks the contact sheets in the PR.

## Behaviors to check

Nothing in the game changes. The `MechaDragon` fight plays as before until `188b`.

## Intended changes

None in gameplay. The frames in `Bosses1/MechaDragon/` change on purpose.
