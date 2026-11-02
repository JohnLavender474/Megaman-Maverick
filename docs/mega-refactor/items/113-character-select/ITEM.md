# 113: Character select screen

- Status: idea
- Type: feature
- Objectives: `O8`, `O9`
- Depends on: `081`
- Related: `047`, `060`, `102`, `108`, `109`
- Branches: none yet

## Goal

Let a player choose a character before a level loads, when the level allows more than one character. This item has no
real caller until a level allows 2 characters, so it waits for that level.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A level lists the characters that it allows. `081` proposes this as a `characters` field (see `081` open question
  1). The set lists allowed characters. It does not give the number of players.
- The result depends on the number of characters in the set:

| `characters` | Result |
|---|---|
| Not set | The default is `setOf(MEGAMAN)`: Megaman only, with no screen |
| One character, for example `setOf(X)` | The level loads with that character, with no screen |
| More than one, for example `setOf(X, MEGAMAN)` | The select screen appears before the level loads |

## Owner's input

2026-11-01 (verbatim): "Just to clarify, if `characters` is `null`, then it defaults to Megaman-only? If only X is in
the set, then the level loads with only X? And if more than 1 player is specified, then a \"select your character\"
screen appears before the level is loaded? We don't need to support player selection right now; just asking as a
forward-looking question"

## Decisions

- (2026-11-01, proposal accepted by the owner for this file) The default is `setOf(MEGAMAN)`, not `null`, so that the
  field always has a value and no code treats `null` as "Megaman".

## Open questions

- In co-op, can each slot choose its own character? (from `060`, `O8` and `102`)
- Does a choice stay for the next level, or does the screen appear before each level?
- Where does the screen go in the flow: after the stage select, or in place of a part of it?

## Sources

- `NOTES.md` in this folder.
- `081` `ITEM.md`, open question 1.
