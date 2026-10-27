# 063: Entity contracts are entity types

- Status: idea
- Type: refactor
- Depends on: none
- Related: `023`, `048`, `062`, `014`, `056`
- Branches: none yet

## Goal

Entity contract interfaces (for example `IFreezableEntity`, `IDamageable`, `IDamager`) say that they are entities, so
code that holds one can use entity members (for example `existenceState`, `destroy()`) with no cast to
`MegaGameEntity` or `GameEntity`. Names say what they are (`IDamageableEntity`, `IDamagerEntity`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the two reviews in `NOTES.md`:

- First branch (type-only): every core entity contract extends `IGameEntity` (or `IMegaGameEntity` where it needs
  `game`); `IDamageable`/`IDamager` extend `IGameEntity`; `IGameEntity` gets `destroy()` (not `spawn`), preferably as
  `IGameEntity : IDestroyable`; the casts that existed only for these members go (about 25, including the 15
  `(x as GameEntity).destroy()` casts); delete the unused `IUpsideDownable`; drop the redundant cast at
  `TimberWoman.kt:931`. No gameplay change.
- Second branch (rename only): `IDamageable` → `IDamageableEntity`, `IDamager` → `IDamagerEntity` (about 470
  references in about 190 files); a mechanical commit for `.git-blame-ignore-revs`.
- Not in this item: the state-only contracts as components (`IFreezableEntity` first, then `ILightSource`,
  `IScalableGravityEntity`, `IFireableEntity`) belong to item `048`; the markers `IFreezerEntity`/`IFireEntity` as
  damage tags belong to item `023`. The callback contracts (`IDamageable`, `IDamager`, `IHazard`, `IBossListener`,
  `ITeleporterEntity`, ...) stay interfaces for now.
- Not candidates: `IDirectional` (`IBody` implements it), `IActivatable` and `IEventListener` (non-entity
  implementers), `IDamageNegotiator` (a strategy object), `IFaceable`/`ISizable` (generic interfaces).

## Owner's input

2026-10-27 (in `062c`, about the casts kept because `IFreezableEntity` does not extend `IGameEntity`; verbatim):
"Dispatch a separate agent to see if it's feasible to go ahead and make each and every single "contract" like this
extend IGameEntity. I think `IDamageable` and `IDamager` should also be renamed to have the `Entity` suffix and extend
the interface, too. Or would some or all of these behaviors be better suited as components instead of interfaces?"

2026-10-27 (verbatim): "Right now, IGameEntity.kt does not have `spawn` and `destroy` because the interface doesn't
want to enforce that entities handle their own spawning. However, I'm wondering what other engines do in this regard
and if it's advisable to go ahead and add these new funcs to the contract."

## Open questions

1. `destroy()` on `IGameEntity` directly, or `IGameEntity : IDestroyable`? (The review prefers `IDestroyable`.)
2. Move `IDamageable`/`IDamager` to `core`, or keep them in the engine? (Only their own files reference them in the
   engine.)
3. Narrow `GameEngine.spawn`/`destroy` to `GameEntity` (the engine already throws for anything else in
   `setExistenceState`)?

## Sources

- `NOTES.md` in this folder.
- `062c` branch `docs/TODO.md` (the backlog candidate) and `process-log.md`.
