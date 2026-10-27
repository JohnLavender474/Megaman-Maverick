# Contract: mega-refactor-063a-contracts-are-entities

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: everything. The branch changes types and removes casts. The compiler checks most of it.
Two runtime paths change in form only:

- `IProjectileEntity.explodeAndDie` calls the virtual `destroy()` in place of `game.engine.destroy(this)`. No
  projectile overrides `destroy()`, so the same engine call runs.
- `GameEngine` writes `entity.existenceState` directly in place of `setExistenceState`. For a `GameEntity` the write
  is the same; the cast and the throw go.

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [x] The build passes and the jar builds.
- [x] All engine and core tests pass, 0 failures. The counts go up only by the new tests.
- [x] The compiler warnings are the same set as the baseline (counted at plan step 1), except new "useless `is`
  check" or "no cast needed" warnings that this branch makes visible; each new warning is listed in the process log.
- [x] **Temporary contract suite** `Temp063aExplodeAndDieTest` (core test sources), with a frozen copy of the old
  destroy step (`game.engine.destroy(this)`): for a projectile that uses the default `explodeAndDie` (for example
  `Cokonut`), two fresh instances in the same state (`UNSPAWNED`, `SPAWN_QUEUED`, `LIVE`, `DESTROY_QUEUED`,
  `RESPAWN_QUEUED`), one through the old step and one through the new `destroy()`. The return values are equal, and the
  `existenceState` is equal after the call and after one `engine.update()`. Deleted before the merge.
- [x] **Permanent engine test:** an `IGameEntity` that is not a `GameEntity` goes through `spawn`, `update`,
  `destroy`, `update`, a respawn, and `reset()` with no exception, and its `existenceState` follows the same states as a
  `GameEntity` on the same calls.
- [x] **Permanent engine test:** `destroy()` on a `GameEntity` held as `IGameEntity` returns the `062b` results (`true`
  for `LIVE`, `false` for `UNSPAWNED` and `DESTROY_QUEUED`) and runs a subclass override.
- [x] **Permanent core test** `EntityContractsTest`: each core contract in scope is a subtype of `IMegaGameEntity`, and
  `IDamageable`/`IDamager` are subtypes of `IGameEntity`.
- [x] Greps: no `setExistenceState` in `engine/`; no `IUpsideDownable`; no `as GameEntity).destroy()` or
  `as MegaGameEntity).destroy()` in `core/`; `IProjectileEntity` has no `game.engine.destroy(this)`.
- [x] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

Passed (owner, 2026-10-27). A short sanity pass. The owner plays these once, after the implementation, from the worktree. Each room has an
entity whose code this branch changes.

- `./run-game-alpha.sh --level DESERT_MAN --room room15`: `Cokeyro` throws `Cokonut`s; each one explodes when it hits
  a block or Megaman and then is gone (the default `explodeAndDie`).
- `./run-game-alpha.sh --level DESERT_MAN --room room3`: `TorikoPlundge` and its blocks appear, work and disappear as
  before.
- `./run-game-alpha.sh --level MOON_MAN --room room1`: ride a `RocketPlatform`; it flies and comes back as before.
- `./run-game-alpha.sh --level RODENT_MAN --room room13`: the `DisappearingBlocks` appear and disappear in order.
- `./run-game-alpha.sh --level REACTOR_MAN --room room4`: the `FloatingCanHole` sends `FloatingCan`s; destroy some;
  leave the room and come back; no crash.
- In each room: shoot enemies until they die, and leave the room with live projectiles on screen (the culler at
  `MegamanMaverickGame.kt:900` now calls `entity.destroy()`).

## Behaviors to check by review

- Each removed cast: the static type after the change has the same member, and the call resolves to the same
  implementation (`GameEntity.destroy()` or an override).
- `ShieldEntity.kt:68` keeps its null check on `owner`.
- `LightSourceUtils.sendLightSourceEvent` reads only `IMegaGameEntity` members from its `entity` parameter.
- The frozen-enemy shield (`FreezableEntityHandler`), the light sources and the boss-defeat cleanup
  (`IBossListener.onBossDefeated`) change only in types; they are checked by review and the compiler, not by play.

## Intended changes

None.
