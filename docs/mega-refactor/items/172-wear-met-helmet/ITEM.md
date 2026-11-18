# 172: Mega Man picks up and wears a `Met` helmet

- Status: idea
- Type: feature
- Objectives: `O4`, `O8`
- Depends on: none
- Related: `079`
- Branches: none yet

## Goal

Since `079a`, a `Met` helmet is its own entity (`MetHelmet`). After its `Met` dies, the helmet rests on the blocks.
Let Mega Man pick up a resting helmet and use it, and let him catch a falling helmet and wear it. Today the helmet
rests on blocks and passes through Mega Man.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A player picks up a resting `MetHelmet` and catches a falling one.
- A worn helmet gives the player an effect (to decide; for example a shield like the `Met`'s).
- Give the ability as a capability, not as a check for `Megaman` (`O8-R2`), so that other characters can get it.
- A test room in `RefactorTest079` or a new test map.

## Owner's input

2026-11-18 (`079a`, owner idea): a resting `MetHelmet` (after its `Met` died) can be picked up and used, and Mega
Man can catch a falling one and "wear" it.

## Open questions

- What does a worn helmet do (a shield, a damage cut, only a look)? How long does it last, and how does it break?
- Which characters can wear it: only Mega Man, or every player?
- Can a player take the helmet of a live `Met`?
- In co-op, which player gets a helmet that two players touch in the same frame?

## Sources

- `NOTES.md` in this folder.
- Branch `docs/TODO.md` of `079a`: `items/079-smarter-enemies/branches/mega-refactor-079a-smarter-met/docs/TODO.md`.
