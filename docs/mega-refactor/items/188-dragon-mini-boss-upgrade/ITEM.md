# 188: Dragon mini-boss upgrade (legs and new attacks)

- Status: active
- Type: feature
- Objectives: `O4`
- Depends on: none
- Related: `118`, `159`
- Branches: `mega-refactor-188a-dragon-moves-art` (active, 2026-11-26)

## Goal

Upgrade the `MechaDragon` mini-boss in Inferno Man's stage (`room8`). Today the dragon has no legs and a very simple attack pattern. Give it legs and new moves, including a standing "sweeping" fire attack.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. A proposed split:

- `188a` (design and art only): an Opus agent ideates new moves (`temp/dragon-moves/IDEAS.md`, summarized in the branch `NOTES.md`); a Fable art agent redraws the existing frames with legs and draws new frames for the recommended moves.
- `188b` (code): the new states and attacks in `MechaDragon.kt` and its fight plan, with the new frames wired in.

## Owner's input

- 2026-11-26: "New mega-refactor branch for Mega Man Maverick: Upgrading the dragon mini-boss in Inferno Man's stage. Right now, the dragon has no legs and has an overly simplistic attack pattern. Let's do the following sequentially: 1. Dispatch an Opus agent to ideate on some cool new moves for the dragon, including one where he stands and does a "sweeping" fire attack. We'll let the agent come up with the 1st pass of ideas. 2. Dispatch a Fable art agent to redraw the existing dragon frames with legs, along with some new frames to match the recommended best new attacks from step 1."

## Decisions

- 2026-11-26 (owner): the branches of this item merge directly into `master`.
- 2026-11-26 (owner): Fable is approved for the art of this item (`art.md` confirmation).

## Open questions

- See the Opus ideation's open questions in the branch `NOTES.md` once step 1 is done.

## Sources

- `core/.../entities/bosses/MechaDragon.kt`; `docs/mega-refactor/items/118-boss-fight-routines/branches/mega-refactor-118e-batch-4/docs/SCOPE.md` (the fight flow).
