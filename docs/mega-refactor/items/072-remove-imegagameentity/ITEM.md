# 072: Remove `IMegaGameEntity`

- Status: planned
- Type: refactor
- Objectives: `O1`
- Depends on: `016`
- Related: `029`, `047`, `048`, `063`, `064`
- Branches: none yet

## Goal

Fewer interfaces describe an entity. The core entity contracts extend `IGameEntity` directly, and `IMegaGameEntity`
is deleted. `MegaGameEntity` keeps `ITaggable` and `ITypable<EntityType>`; `IProjectileEntity` names them itself.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the research in `NOTES.md` (option (c), done after
`016`):

- Done before this item: `016` removed `game` and the 3 default methods from the interface, and `047b` moved `players`
  and `targetPlayer` to `MegaGameEntity.kt`.
- The removal is a type-only change of 14 files and 15 lines (2026-11-02, after `047b`): the definition,
  `MegaGameEntity`, 11 contracts and `EntityContractsTest`. No parameter, cast or generic bound uses the interface.
- `overlapsGameCamera()` moves to `MegaGameEntity.kt`, in the same package, so its 69 imports stay.
- Every `getType()` and `getTag()` value stays the same (for example `BulletBill` keeps `ENEMY` over the
  `IProjectileEntity` default `PROJECTILE`). `EntityContractsTest` changes its assertion to `IGameEntity`.
- No game content goes into `engine`.

## Owner's input

2026-10-28 (verbatim): "see if it's advisable to get rid of `IMegaGameEntity` and instead to tie the funcs/fields to
`IGameEntity` or a system/service/func/etc. This way, we can get rid of maintaining multiple interfaces to describe an
entity."

2026-10-28 (verbatim, about the goal): "both. Fewer interfaces, as well as the goal of 016" and "Yes, record as a new
item."

2026-11-02 (verbatim): "This interface shouldn't exist". This closes "decide again at the end of `016`" in `NOTES.md`.

## Decisions

- 2026-10-28 (owner): the goal is both fewer entity interfaces and no `game` on entities (`016`). The research advises
  to keep `IMegaGameEntity` until `016` is done, then delete it (option (c)); not chosen: moving its parts to
  `IGameEntity` or to components and services now (option (b)), or deleting it before `016` (casts come back).

- 2026-10-28 (owner, decision 6 in `016`): the 3 default methods (`getGameCamera`, `playSoundNow`, `stopSoundNow`) and
  `game` leave `IMegaGameEntity` in `016c`, not in this item.

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
