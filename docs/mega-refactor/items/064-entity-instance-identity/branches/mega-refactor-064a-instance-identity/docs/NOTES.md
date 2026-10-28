# Notes: mega-refactor-064a-instance-identity

## Decisions

- 2026-10-27: owner decisions in the item `ITEM.md`, copied in `SCOPE.md`. The second decision split the item:
  `064a` does groups only (an engine `GroupsComponent` and an engine group index); `id` goes to `064b`.
- 2026-10-27 (owner, through the coordinator): the manual checks use the real rooms; no `RefactorTest064` map.
- 2026-10-27 (owner, item `ITEM.md`, commit `41ec18082`): groups go in a `GroupsSystem` (a `GameSystem`), not in
  `GameEngine`; the index is like today's `MegaGameEntities`; `GameSystem` may get hooks or open functions. Approved:
  `getEntitiesInGroup(group)`, a read-only return with no allocation per call, the component added in `onSpawn` only
  with a group, a set of names, the accepted `super.onDestroy()` change. `super.init()` is item `065`; the `SpiderWeb`
  fix is a `fix/` branch after `064a`.

## Overlap with other branches

- `mega-refactor-063b-damage-entity-rename` (`.worktrees/mega-refactor-063b-damage-entity-rename`), read on 2026-10-27
  at `02d1a4eb7` (scope and contract approved, plan in draft, no code yet): a mechanical rename of `IDamageable`/`IDamager` to
  `IDamageableEntity`/`IDamagerEntity`, kept in `engine`, plus `IChildEntity`/`IParentEntity : IGameEntity`. None of
  the `064a` files (`GameSystem.kt`, the new `engine/.../groups/` files, `MegamanMaverickGame.kt`, `MegaGameEntity.kt`,
  `MegaGameEntities.kt`, `GameEntityUtils.kt`) names these types, and `063b` does not touch `GameSystem`. Only root `CLAUDE.md` can be shared, on different lines. The first `064a`
  scope (with `id`) touched `SmallIceCube.kt`, `Laser.kt` and `TubeBeamerV2.kt`; that overlap is gone. Read the `063b`
  folder again before the merge.
- Recheck before the play test (2026-10-27): `063b` at `fad9a543a` (play test passed, ready to merge). Its code
  changes (181 files) and the `064a` code changes share no file. The new `064a` code does not name
  `IDamageable`/`IDamager` (or the new names), `IChildEntity` or `IParentEntity`, so either merge order works with no
  rename step for `064a`.
