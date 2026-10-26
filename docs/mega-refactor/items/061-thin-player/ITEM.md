# 061: Thin player view; locks and signals as components and functions

- Status: active
- Type: refactor
- Depends on: `029`, `031`
- Related: `012`, `024`, `030`, `047`, `048`, `053`, `057`, `060`
- Branches: `mega-refactor-061a-thin-player` (merged), `mega-refactor-061b-play-phase-teleport` (active: play phase and teleportable components); next: a branch that moves the `MegaPlayerAccess.kt` callers to entity-level functions

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
- 2026-10-26 (owner, "Sure"): `ready` and `teleporting` become entity state, not player state:
  `LifecycleComponent` with `Lifecycle { ENTERING, ACTIVE, LEAVING }` (an entity without it reads as `ACTIVE`) and
  `TransportComponent` holding the teleporter (`teleporting` means a teleporter is set), with free functions. Research
  in `NOTES.md`, "`ready` and `teleporting` for any entity".
- 2026-10-26 (owner, "Yes to all"): no player wrappers for entity questions. Engines ask the entity directly (Bevy
  queries with a `Player` marker, Unity `GetComponent`, Unreal gameplay tags on any actor, Mania and X6 shared
  functions); a player view only finds the entity. So `061b` moves the `ready` and `teleporting` callers to
  `entity.isActive` and `entity.teleporting` with no `MegaPlayer` wrapper, and a later `061` branch moves the callers
  of `MegaPlayerAccess.kt` (`player.body`, `player.facing`, ...) to entity-level functions and deletes the file. The
  player view keeps `slot` and `input`. Reasons: one way to ask a question; no two defaults for the same entity (the
  player wrapper gave `false` where an entity without a lifecycle reads `ACTIVE`); the view does not grow again.
- 2026-10-26 (owner): the play state is named `PlayPhase { PRE_PLAY, IN_PLAY, POST_PLAY }` (`PlayPhaseComponent`,
  `playPhase`, `inPlay`, `setPlayPhase`), not `Lifecycle`: existence (spawn, destroy), play phase and health death are
  three separate axes, and `inPlay` checks only the phase (`NOTES.md`, "'Lifecycle' versus spawning, spawned and dead").
- 2026-10-26 (owner): the teleport state component is `TeleportableComponent(var teleporter: ITeleporterEntity? =
  null)`, not `TransportComponent`: its presence says the entity can be teleported (a teleporter fails without it),
  and `teleporter` is the current carrier (not an owner; `IOwnable.owner` means something else). Reads without the
  play phase or teleportable component give the default; writes require the component.

## Open questions

- None.

## Sources

- `NOTES.md` in this folder.
- Item `029` `NOTES.md`: "`MegaPlayer` review and engine patterns", "Functions, components and events instead of
  classes", "Correction: 'or null' lookups versus query filters".
