# 072: Remove `IMegaGameEntity`

- Status: planned
- Type: refactor
- Depends on: `016`
- Related: `029`, `047`, `048`, `063`, `064`
- Branches: none yet

## Goal

Fewer interfaces describe an entity. The core entity contracts extend `IGameEntity` directly, and `IMegaGameEntity`
is deleted. `MegaGameEntity` keeps `ITaggable` and `ITypable<EntityType>`; `IProjectileEntity` names them itself.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the research in `NOTES.md` (option (c), done after
`016`):

- Before: `016` removes `game` from the contracts (only `IFreezableEntity` (`FreezableEntityHandler.kt:53`) and
  `ILightSource` (`LightSourceUtils.kt:56`) need it through the interface), and `029`/`047` move `targetPlayer` and
  `players`. Then the removal is a type-only change of about 20 files and 40 lines, with no casts back.
- The 3 default methods (`getGameCamera`, `playSoundNow`, `stopSoundNow`) and the extensions (`players`,
  `targetPlayer`, `IBodyEntity.overlapsGameCamera()`) move to their new homes from `016`/`029`/`047`.
- Every `getType()` and `getTag()` value stays the same (for example `BulletBill` keeps `ENEMY` over the
  `IProjectileEntity` default `PROJECTILE`). `EntityContractsTest` changes its assertion to `IGameEntity`.
- No game content goes into `engine`.
- Optional trims now, with real callers: remove the hidden cast in `overlapsGameCamera` with a receiver bound to both
  `IMegaGameEntity` and `IBodyEntity`.

## Owner's input

2026-10-28 (verbatim): "see if it's advisable to get rid of `IMegaGameEntity` and instead to tie the funcs/fields to
`IGameEntity` or a system/service/func/etc. This way, we can get rid of maintaining multiple interfaces to describe an
entity."

2026-10-28 (verbatim, about the goal): "both. Fewer interfaces, as well as the goal of 016" and "Yes, record as a new
item."

## Decisions

- 2026-10-28 (owner): the goal is both fewer entity interfaces and no `game` on entities (`016`). The research advises
  to keep `IMegaGameEntity` until `016` is done, then delete it (option (c)); not chosen: moving its parts to
  `IGameEntity` or to components and services now (option (b)), or deleting it before `016` (casts come back).

## Open questions

1. Do the 3 default methods go in `016`, or in this item?

## Sources

- `NOTES.md` in this folder.
