# 061: Thin player view; locks and signals as components and functions

- Status: active
- Type: refactor
- Depends on: `029`, `031`
- Related: `012`, `024`, `030`, `047`, `048`, `053`, `057`, `060`
- Branches: `mega-refactor-061a-thin-player` (active)

## Goal

Stop forcing a player entity into Megaman's shape. `MegaPlayer` becomes a thin slot view (`slot`, `entity`, `input`)
that requires nothing at registration. What it used to forward becomes free functions that read the entity when they
are called. State flags that several sources write (`canBeDamaged`, movement, behaviors) become locks: a component
plus free functions, not fields on a class. The player-damaged notification becomes an immediate signal on the
players service.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- The work on the experimental branch `temp-thin-player` (three commits on `master` at `55e81840c`; review notes in
  `docs/temp-thin-player-review.md` on that branch).
- Cleanup: the lock mechanism moves to the engine (the game supplies its groups and reasons, as with `InputButton`);
  test leftovers; import order.

## Owner's input

2026-10-26, on `MegaPlayer` (verbatim): "WTF is going on with `MegaPlayer.kt`? Why's it so big? And why are we forcing
the player to implement `IDirectional`, `IFaceable`, etc.?"

2026-10-26, on the first recommendations (verbatim, in part): "Should we even require the cached body? Reason I ask is
because it's not a guarantee that every player will have a body." "Again, we're talking about using OOP which I'm not
sure I'm a fan of anymore, because it means that all entities must extend the class. Why can't we use funcs and
components for this, instead?" "What about direct-instantaneous event dispatches and reads? What about static/global
funcs? How do other engines do this?"

2026-10-26 (verbatim): "Here's what I want you to do. Create a new `temp-...` branch. I want you to go ham on that
branch with refactoring the code to align with the concerns and perspectives I have. When done, I will test it and
review it. If all looks good, then we can create a new "item" branch, merge the temp changes over to it, and do any
more cleanup that's needed within the same item (branch)". After testing the temp branch: "Tests pass".

## Decisions

- 2026-10-26: a new item (item `029` is done). The temp branch's work is the base of the first branch.
- 2026-10-26: the cleanup moves the lock mechanism to the engine.
- 2026-10-26 (from the `029` notes discussion): parts a caller needs are filtered or fail at the read; optional
  features give `null` or a default at the call site. Not every player has a body.
- 2026-10-26: locks are a set of reasons per group (a reason is held or not), not a count per reason. Reason: a count
  leaves `SpiderWeb` locked for good (each web locks; the release happens once).

## Open questions

- None.

## Sources

- `NOTES.md` in this folder.
- Item `029` `NOTES.md`: "`MegaPlayer` review and engine patterns", "Functions, components and events instead of
  classes", "Correction: 'or null' lookups versus query filters".
