# 108: Spec Ops co-op missions

- Status: idea
- Type: feature
- Objectives: `O2`, `O8`
- Depends on: `047`, `102`
- Related: `094`, `095`, `109`, `110`
- Branches: none yet

## Goal

Dedicated co-op missions, separate from the main campaign, for play on one machine or on the net. This mode and PvP (`109`) are the two named multiplayer modes, and they are the crux of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- None yet.

## Owner's input

2026-11-01 (verbatim):

> I imagine these will be "extra", so not at all in the "main" game. Players can either play co-op or pvp multiplayer locally or on the net. (I used to play Call of Duty, so I'm kinda imagining co-op as like spec ops in Modern Warfare, and pvp as being like team deathmatch and objective gameplay.)

> Let's go ahead and make the "spec ops" and "PvP" modes the crux of the multiplayer objective. These are the only two modes I've outright named and defined for multiplayer.

## Decisions

- 2026-11-01: Spec Ops and PvP are the crux of `O2`, and they are the only two named multiplayer modes.
- 2026-11-01: co-op in the main campaign is optional and comes after these modes (`110`).

## Open questions

- Do missions use existing stages, or new content made for co-op? The nearest existing content is `GAUNTLET_1` and `GAUNTLET_2` (16 `GauntletMaster` objects). They are `LevelType.EXTRA_LEVEL` entries with a `LevelOverride`.
- How many players does a mission support, and does the mission say this? `LevelDefinition` has no player count field. The target is 2 players, and the design must scale to 4 or more.
- Must missions use one-screen rooms for the shared-screen phase? Content made for co-op can avoid most of the camera and room questions in `047`. Of the 214 rooms in the 15 live stages, 122 fit in one 16x14 view.
- See `102` for the mode concept, the menu, the start flow and progress.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
