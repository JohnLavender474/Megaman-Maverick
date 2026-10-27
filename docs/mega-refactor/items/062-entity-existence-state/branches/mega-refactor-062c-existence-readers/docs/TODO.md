# TODO: mega-refactor-062c-existence-readers

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- At the merge, on `master`: the root `CLAUDE.md` still names `dead` ("Entity base classes" and "Gotchas"); change it to
  `existenceState.liveAfterUpdate`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Entity contracts are entity types (owner question, 2026-10-27; read-only review in the process log). Make every core
  entity contract and `IDamageable`/`IDamager` extend `IGameEntity` (or `IMegaGameEntity` where `game` is needed),
  rename the two damage interfaces to `IDamageableEntity`/`IDamagerEntity` (about 470 references; a rename-only
  branch), add `destroy()` (not `spawn`) to `IGameEntity`, preferably as `IGameEntity : IDestroyable` (second review:
  15 casts go; `spawn` has no caller that needs it through `IGameEntity`; object-style engines put the removal request
  on the entity and keep creation in a world or factory), delete the unused `IUpsideDownable`. About 25 casts go; type-only, no
  gameplay change. Later, inside items `048` and `023`: the state-only contracts become components (`IFreezableEntity`
  first: 64 copies of the same `frozen` delegate; then `ILightSource`, `IScalableGravityEntity`), and the markers
  `IFreezerEntity`/`IFireEntity` become damage tags. The callback contracts (`IDamageable`, `IDamager`, `IHazard`,
  `IBossListener`, ...) stay interfaces for now.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `Starkner` removes its id from the static `ALIVE` set only in its `destroy()` override (`Starkner.kt:192-196`), and
  nothing else clears `ALIVE` (`:73`, `:147`, `:186`). A destroy that does not go through that member skips it: the
  engine's `reset()`/`dispose()` at a level end, or any `engine.destroy(starkner)`. A `Starkner` that is live at a level
  end then stays in `ALIVE`, and `canSpawn` refuses it later in the run [inferred, not played]. The cleanup belongs in
  `onDestroy`. Found by the second review agent; checked in the code.
