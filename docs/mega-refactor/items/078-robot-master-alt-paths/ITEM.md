# 078: Alt paths in the robot master stages

- Status: idea
- Type: feature
- Depends on: none
- Related: `073`, `075`
- Branches: none yet

## Goal

Each robot master stage is fully linear. That is not bad, but alt paths can make the stages better. Find good alt
path ideas for each stage and add them.

Example from the owner: in Desert Man's stage, a sandstorm starts and Megaman has two options. He can go into the
darkness cave (it is already in the stage), or he can go through the sandstorm in an outdoor section. The sandstorm
is like the lava in *Mega Man X5*: it comes at intervals, and Megaman must stand behind blocks so that it does not
push or kill him.

This item changes gameplay on purpose. It is level content, not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A list of alt path ideas, one or more for each of the 8 robot master stages. The owner chooses which ones to make.
- Desert Man: the sandstorm alt path. It needs a new hazard (the sandstorm: a wave at intervals that pushes or kills
  Megaman when no block is in front of him) and a new outdoor section next to the darkness cave.
- One branch per stage is probable.

## Owner's input

2026-10-29 (verbatim):

> Each of the robot-master levels is completely linear, which isn't inherently bad, but it'd be good to come up with
> ideas for cool alt paths: for example, Desert Man's stage can be set up so that a sand storm spawns and Mega Man
> has two options: enter the darkness cave (the one that already exists in the stage) or else prevail through the
> sandstorm in an outdoors section. (The sandstorm can be kind of like the lava in MMX5 where the player must stand
> behind blocks in order to avoid being pushed or killed by the hazard that appears at intervals)

## Open questions

- Does the sandstorm push Megaman, kill him, or do damage? The owner's text says "pushed or killed".
- Do the two paths join again before the boss, and does each path give a different reward (for example a heart tank
  or a health tank)?
- How does the stage show the player that two paths exist?
- The memory rule for level drafts: alt paths must not be dead ends.

## Sources

- `NOTES.md` in this folder.
- X6 decomp notes in `~/IdeaProjects/mmx-demake-tools/docs/` for ideas only (see `CLAUDE.md`, "Inspiration").
