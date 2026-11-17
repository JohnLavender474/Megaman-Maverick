# 078: Alt paths in the robot master stages (not Desert Man)

- Status: idea
- Type: feature
- Objectives: `O4`
- Depends on: none
- Related: `073`, `075`, `166`
- Branches: none yet

## Goal

Each robot master stage is fully linear. That is not bad, but alt paths can make the stages better. Find good alt
path ideas for each stage and add them.

Desert Man's stage is not in this item since 2026-11-17: its alt path (the darkness cave or the sandstorm) is part 2
of `166`. This item covers the other 7 robot master stages.

Example from the owner (now in `166`): in Desert Man's stage, a sandstorm starts and Megaman has two options. He can
go into the darkness cave (it is already in the stage), or he can go through the sandstorm in an outdoor section. The
sandstorm is like the lava in *Mega Man X5*: it comes at intervals, and Megaman must stand behind blocks so that it
does not push or kill him.

This item changes gameplay on purpose. It is level content, not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A list of alt path ideas, one or more for each of the other 7 robot master stages. The owner chooses which ones to
  make.
- One branch per stage is probable. When a stage gets its own epic item (as `166` for Desert Man), its alt path moves
  to that item.

## Decisions

- 2026-11-17: the owner merged the Desert Man alt path into `166`, an epic for all of Desert Man's stage. A separate
  epic item can exist for each other level (or batch of levels).

## Owner's input

2026-10-29 (verbatim):

> Each of the robot-master levels is completely linear, which isn't inherently bad, but it'd be good to come up with
> ideas for cool alt paths: for example, Desert Man's stage can be set up so that a sand storm spawns and Mega Man
> has two options: enter the darkness cave (the one that already exists in the stage) or else prevail through the
> sandstorm in an outdoors section. (The sandstorm can be kind of like the lava in MMX5 where the player must stand
> behind blocks in order to avoid being pushed or killed by the hazard that appears at intervals)

## Open questions

- Do the paths of a stage join again before the boss, and does each path give a different reward (for example a
  heart tank or a health tank)?
- How does a stage show the player that two paths exist?
- The memory rule for level drafts: alt paths must not be dead ends.
- The sandstorm questions moved to `166`.

## Sources

- `NOTES.md` in this folder.
- X6 decomp notes in `~/IdeaProjects/mmx-demake-tools/docs/` for ideas only (see `CLAUDE.md`, "Inspiration").
