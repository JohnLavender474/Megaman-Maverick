# 013: Level flow state machine; pause as an overlay

- Status: planned
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: `015`
- Related: `006`, `012`, `029`, `037`, `038`, `047`, `053`, `082`, `118`, `119`
- Branches: none yet

## Goal

Replace the fragile first-unfinished-wins handler `when` in `MegaLevelScreen` with an explicit level flow state machine.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `LevelFlow` states (spawning, playing, room transition, victory, dying, game over) replace the ordered handler `when` at `MegaLevelScreen.kt:906-913`. A boss fight is not a level state (see "Decisions").
- Level events become state machine inputs.
- Pause, and later other menus, as an overlay stack. Remove the `game.paused` checks.
- The flow is the orchestrator that owns the level reset (with the `038` routine), in place of the 24 listeners that react to `PLAYER_SPAWN` today.

## Owner's input

Answer to review item 12 (2026-10-22, verbatim):

> Yes, one mistake I made is not utilizes step machines enough. A lot of logic buried in sequential if statements, for loops, and in-code branches that would be much easier to change and maintain if done through state machines.

Item `029` research (2026-10-23, verbatim):

> This makes the case further that we need a dedicated orchestrator rather than having this logic spread around all over the place. This is outside the scope of the current item but should definitely be documented.

Motivation from `029`: co-op needs one owner for level-wide steps (level reset, boss intro locks for all players, pause owner). Spread-out listeners cannot tell "one player respawned" from "the level reset". See `029` `NOTES.md`, "Co-op gaps and ideas".

## Decisions

- (2026-11-02, owner, from `118`) This item has no boss states. They would repeat the fixed chain of `118`, block 2 fights and a fight with no freeze, and add boss states to modes with no boss. A `Fight` runs inside `PLAYING` and across transitions. This item calls `cancelAll()` on the level reset, and a fight calls the victory input in place of `VICTORY_EVENT`.
- (2026-11-02, owner, from `118`) A boss fight and a room transition can overlap. This closes that open question.
- (2026-11-02, owner, from `119`) This item later owns the `LEVEL_FAILED` transition.
- (2026-11-02, owner, from `119`) A player's spawn and death are facts of that player, and the level state is "the level failed". This answers the question on `spawning` and `dying` for slot 1.

## Open questions

- The proposed states include `spawning` and `dying` as level states. With `047` death options 2 and 3, one player can die
  while the level plays. Are spawn and death states of each player, with a level state for "all dead"? The current
  handlers act on slot 0 (`MegaLevelScreen.kt:904-909`).
- Does a mode (`102`) change the flow? A PvP match has no boss, no level reset and its own end condition.

## Sources

- `NOTES.md` in this folder.
