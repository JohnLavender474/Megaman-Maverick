# Scope: mega-refactor-159a-intro-boss-art

- Status: approved
- Approved: 2026-11-15 (owner, in place of the brief's decisions)

## Goal

Add a crack to the `BigAssMaverickRobot` death head (`Head/defeated`, the aghast face).

## Owner decision (2026-11-15)

The owner dropped the new fist frames (slam impact, clap pose, open palm): "I'm satisfied with the boss's fists as
they are. Let's just add a 'crack' to the defeated head's sprite." The brief's decisions D1 to D3 are void.

## Current state

`Head/defeated` is 80x160 (2 frames: the aghast face with the pupils 1 px apart, shown for the whole 4 s death,
`BigAssMaverickRobot.kt` `"defeated"`). The frame file is
`assets/sprites/frames/Bosses3/BigAssMaverickRobot/Head/defeated.png`; the atlas region is at 883,439 in
`assets/sprites/sprite_sheets/Bosses3.png` (palette mode `P`).

## In scope

- Cracks drawn into both frames of `Head/defeated`, in the frame file and in the atlas region, in place: a jagged crack
  down the right of the dome with a branch, a crack on the left of the dome, and a crack on the left cheek. Black crack
  pixels with a white edge. No new region, no code change.

## Out of scope

- New hand frames, crack steps over time, any code change.

## Affected areas and risks

- `Bosses3.png`: only the pixels of the `Head/defeated` region change (checked by a script against `master`: 170
  pixels inside the region, 0 outside).

## Overlap with other branches

None: `060d` and `fix/small-bugs-3` do not touch `Bosses3`.

## Objective check

| Rule | Check |
|---|---|
| Always-on rules | no effect (art only) |
| `O4`, `O9` | helps: the death reads more mature |
