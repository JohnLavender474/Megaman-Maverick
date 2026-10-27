# Process log: mega-refactor-063a-contracts-are-entities

## Status

- State: active
- Goal: entity contracts extend `IGameEntity`/`IMegaGameEntity`; casts that existed only for entity members go
- Done: planning approved; steps 1 to 8; play test passed; temporary suite deleted
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master`. Worktree `.worktrees/mega-refactor-063a-contracts-are-entities`.
- Scan: a research agent checked the item `NOTES.md` against `master` (section "2026-10-27: research for 063a");
  the scan is in `docs/SCAN.md`. 31 casts in scope (5 need `destroy()` on `IGameEntity`, 4 need a contract to extend
  `IMegaGameEntity`, 22 already redundant); the 4 `IDamageable`/`IDamager` casts in the item draft stay (they cast to
  `IBodyEntity`).
- Owner decisions (in `ITEM.md`): `IGameEntity : IDestroyable`; damage interfaces stay in `engine` for now; the engine
  accepts any `IGameEntity`; `existenceState` becomes a public `var`. Draft `docs/SCOPE.md`.
- Owner: no `setExistenceState`; the engine writes `existenceState` directly. `docs/SCOPE.md` updated.
- Scope approved (owner, 2026-10-27: "yes"). Draft `docs/CONTRACT.md`: a temporary suite for the default
  `explodeAndDie`, permanent tests for a non-`GameEntity` `IGameEntity` and for the contract supertypes, and five short
  manual checks in rooms found in the live TMX files.
- Contract approved (owner, 2026-10-27: "yes"). Draft `docs/PLAN.md` (8 steps).
- Plan approved (owner, 2026-10-27: "yes").
- Step 1 (baseline, clean build, JDK 17 Corretto): build and jar pass; engine 546 tests, core 172 tests, 0 failures;
  88 unique warnings (scratchpad `063a-warnings-baseline.txt`).
- Step 2: `Temp063aExplodeAndDieTest` (5 cases, one per existence state; two `Cokonut`s built side by side, since an
  `engine.update` in the setup of one moves the other) passes on the old code.
- Step 3: `IGameEntity : IDestroyable`; `existenceState` is a `var` on `IGameEntity` and `GameEntity` (no `internal
  set`); `GameEngine.setExistenceState` deleted, its 10 call sites write `existenceState` directly; `IDamageable` and
  `IDamager` extend `IGameEntity`. New `AnyGameEntityTest` (3 cases): a plain `IGameEntity` and a `GameEntity` go
  through the same calls with the same states; `destroy()` through an `IGameEntity` reference gives the `062b` results
  and runs an override. Engine 549, core 177, 0 failures.
- Step 4: 10 core contracts extend `IMegaGameEntity`; `IBossListener` extends `IMegaGameEntity` in place of
  `IDestroyable`; `IUpsideDownable` deleted. New `EntityContractsTest` (14 cases). Engine 549, core 191, 0 failures.
- Step 5: the 9 casts removed (culler, `RotationAnchor`, `Togglee` x2, `IProjectileEntity.explodeAndDie` now calls
  `destroy()`, `FreezableEntityHandler` x2, `ShieldEntity` as `owner?.existenceState?.liveAfterUpdate == false`,
  `LightSourceUtils` with an `IMegaGameEntity` parameter); 3 unused imports removed. Tests pass, including the
  temporary suite.
- Step 6: the 22 redundant casts removed (11 `destroy()` casts, `TimberWoman.kt:931`, `PetitDevil.kt:147`, 8 iterator
  casts, and the `child as MegaGameEntity` statement in `RotationAnchor`); 10 unused imports removed. Tests pass; no
  `as GameEntity).destroy()` or `as MegaGameEntity).destroy()` left in `core/`.
- Step 7: root `CLAUDE.md` gotcha: the `existenceState` setter is public, but only the engine should write it.
- Step 8 (automated checks, clean build): build and jar pass; engine 549 (546 + 3), core 191 (172 + 5 temporary + 14);
  0 failures. Warnings: the same 88 lines as the baseline, no new warning. Greps: no `setExistenceState`, no
  `IUpsideDownable`, no `destroy()` casts, no `game.engine.destroy(this)` in `IProjectileEntity`;
  `contact-rule-selection.txt` unchanged. Review: only `Starkner` overrides `destroy()` in `core/` (not a projectile);
  the old culler cast called the same virtual `destroy()`; `ShieldEntity` keeps the null check;
  `sendLightSourceEvent` reads only `entity.game` and puts the same object in the event.
- Owner play test (2026-10-27: "Play tests pass"): all manual checks in `docs/CONTRACT.md` pass.
- Deleted `Temp063aExplodeAndDieTest`. Automated checks again (clean build): build and jar pass; engine 549, core 186,
  0 failures; warnings the same 88 lines as the baseline.
