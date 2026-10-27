# 063: Entity contracts are entity types

- Status: active
- Type: refactor
- Depends on: none
- Related: `023`, `048`, `062`, `014`, `056`
- Branches: `mega-refactor-063a-contracts-are-entities` (active, started 2026-10-27)

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

## Decisions

- 2026-10-27 (owner, "Okay, you've convinced me that this is the best approach"): `IGameEntity : IDestroyable`, with
  the existing `engine/.../entities/contracts/IDestroyable.kt`. Not LibGDX `Disposable`: in this repo `dispose()` is
  the final shutdown, it returns `Unit` (the `062b` accept/refuse result is a `Boolean`), and entities are pooled and
  spawned again.
- 2026-10-27 (owner, "Go with your recommendations on the damage-focused work"): `063a` keeps `IDamageable`/`IDamager`
  in `engine` and adds `: IGameEntity`. `063b` decides the module; a move to `core` goes in the same mechanical commit
  as the rename, so the types move at most once.
- 2026-10-27 (owner, verbatim): "The game engine should always accept any `IGameEntity`. Otherwise, the interface is a
  lie." `GameEngine.spawn`/`destroy` are not narrowed to `GameEntity`. The runtime throw in
  `GameEngine.setExistenceState` (`GameEngine.kt:211-214`) breaks the same rule, so the engine must be able to write
  the state of any `IGameEntity`.
- 2026-10-27 (owner, verbatim): "Let's just make `existenceState` be a var. Callers will need to be smart and not
  accidentally overwrite it." `IGameEntity.existenceState` becomes a `var` with a public setter, and the throw in
  `GameEngine.setExistenceState` goes. This replaces the `062` rule that only the engine can write the state: only the
  engine should write it, but the compiler does not enforce that. Not chosen: a write key with an `internal`
  constructor, and an engine-owned map. Root `CLAUDE.md` changes its wording ("only the engine writes it") in `063a`.

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
- `062c` branch `docs/TODO.md` (the backlog candidate) and `process-log.md`.
