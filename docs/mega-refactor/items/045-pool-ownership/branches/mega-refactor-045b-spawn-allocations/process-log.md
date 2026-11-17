# Process log: mega-refactor-045b-spawn-allocations

## Status

- State: active
- Goal: a pooled entity makes its objects once: no constructor call in `onSpawn`, `onDestroy` or an event handler
- Brief: https://claude.ai/artifact/N9Cq95bcT8keb6mrAFCVQD
- Done: scan, scope, contract, brief, plan steps 1 to 5, review before the play test
- Next: the owner's play test and diff review; the owner confirms D1 to D5 (`SCOPE.md`)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-16

- No branch exists yet. The owner, in the `045a` Diffity review (2026-11-16), on `SpikeBall.kt`
  (`getCenter(Vector2())` in `onSpawn`): "Instantiating a new `Vector2()` defeats the whole purpose of the pool which
  is to prevent the GC from having to collect new instances. [...] I propose refactoring code like in this class."
  Owner choice: `045a` fixes the `CODE_STYLE.md` rule and refactors `SpikeBall` as the model; `045b` does the sweep, by
  a subagent.
- Measured on `045a` at `071f6f1ad` (text count, so it can include a few false hits): 146 lines in 75 files construct
  an object inside an `onSpawn` body (`Vector2`, `GameRectangle`, `GameCircle`, `GameLine`, `GameSprite`, `Pendulum`,
  `MotionDefinition`, `Timer`); 84 of them are `Vector2`, `GameRectangle`, `GameCircle` or `GameLine`. Event handlers
  and `onDestroy` bodies are not counted yet. The scan step counts them.
- `045a` merged (2026-11-16, squash `aaf51b514`), so this branch can start. Added to its scope from the `045a`
  `docs/TODO.md`: the per-frame `new` that `045a` left (`ShapeUtils.intersectRectangleAndLine`, a `Vector2` per hit,
  each frame for `Laser` and `TubeBeamerV2`; the `--debugShapes` suppliers of `Saw`, `SwingingAxe` and
  `SwingingPlatform`; `Darkness.handleLightEvent`) and `MegaUtilMethods.delayRun` (a `UUID` string and a `Timer` per
  call, often from `onSpawn` and events). Inventory: `temp/mega-refactor-045a-pool-ownership/scan-new.tsv` (main
  checkout).
- Branch start on `master` (2026-11-16). Merge target: directly into `master` (as `045a`). Done end to end by a subagent.

## 2026-11-16 (subagent session)

- Scan: `temp/mega-refactor-045b-spawn-allocations/scan.py` (main checkout): 319 constructor rows in 102 files in
  `onSpawn`, `onDestroy`, `onEvent` and `canSpawn` bodies and the same-file functions they call. Scope D1: the shared
  spawn paths, 54 often-spawned classes and the `045a` per-frame leftovers here; bosses and level objects to `045c`.
  The owner asked for no stops, so D1 to D5 are Claude's choices, pending owner confirmation.
- Brief rendered (`temp/.../BRIEF.html`, no `✗`) and published: https://claude.ai/artifact/N9Cq95bcT8keb6mrAFCVQD
- Step 1 (engine): `CullableOnUncontained.reset()` clears the time; `SineWave.set`, `Trajectory.set`,
  `RotatingLine.set`; `ShapeUtils.intersectRectangleAndLine(rectangle, line, out): Int`. `engine:test` passed.
- Step 2 (shared paths): `AbstractEnemy`, `AbstractProjectile`, `AbstractItem`, `Block` (also fixed the `cull_room`
  listener leak), `Spike`.
- Step 3: `LaserContacts` (new) for `Laser`, `TubeBeamerV2`, `LaserRules`; `Laser` pieces grow only (keys made once,
  no key string per frame) and `laserFixture.setShape(line)` only when the shape changes (each call dropped the
  adjusted shape, and the next `getShape()` made a copy); debug shapes made once in `Saw`, `SwingingAxe`,
  `SwingingPlatform`; `Darkness` light circle; `delayRun` pool, `cancelDelayRun`, `MegamanWeaponFire`.
- Step 4: 3 parallel subagents swept 44 files (no Gradle in the agents); their reports are in
  `temp/.../sweep-reports.md`. One `TODO` left in `LightSourceEntity` (bug candidate in `TODO.md`).
- Step 5: `SpawnAllocationGuardTest` with `core/src/test/resources/spawn-allocation-baseline.txt`: the direct bodies
  of the entity files hold 158 calls in 73 files on `master` and 85 calls in 38 files on the branch. Docs:
  `CODE_STYLE.md`, `utils/README.md`.
- Test map: room `swing` in `RefactorTest045.tmx` (a subagent with the `level-builder` skill): `SwingingPlatform`,
  `SwingingAxe`, a rotation `Saw` and a trajectory `Saw`.
- Tests: `core:test` 2400 tests, 0 failures (first run). Mutations in one batch (restored after): `timeUncontained`
  reset, `SineWave.set` origin, `Saw` anchor, `cancelDelayRun` finished guard, `LaserContacts.clear` pairs, an extra
  `Vector2()` in `Flame.onSpawn`: each made its test fail. `cullOnEvents.reset()` in `AbstractEnemy.onSpawn` removed:
  no test failed, because the cullables component resets the cullable at destroy and it cannot be culled while out
  of the component; the call is a defensive reset, kept.
- Automated checks on `11deb4f51`: `engine:test` 710 tests, `core:test` 2400, `core:smokeTest` 469, 0 failures;
  `lwjgl3:jar` built.
- Review before the play test, 2 subagents (read only, no game launch):
  - Diff review: no bug. Fixed: `SwingingPlatform` cleared its owned props copy after use (it kept a scratch until
    the next spawn); a free `delayRun` run drops its `services`; `cancelDelayRun` fails fast on an id that is not a
    `delayRun` id; `Trajectory` copies the array of its primary constructor; import order; the `LightSourceEntity`
    `TODO` names `045c`; `SCOPE.md` moves the `Block` extra fixtures and the `Spike` animation to `045c`. Not changed:
    a generation in the `delayRun` id (no caller stores a stale id), the lazy `Trajectory` in `BulbBlaster` and `Saw`
    (allowed by the rule), the place of `LaserContacts`, long lines in 4 subagent edits.
  - Check review: all commands valid, no throw. Fixed: the trajectory `Saw` of room `swing` removed (it jumps to near
    the world origin, also on `master`: bug candidate); rooms `spike_ball` and `swing` joined by a door, and the
    platform got `cull_room`, so a second life is possible; axe and platform moved lower; the contract says that a
    death respawns only enemies and gives a leave-and-return step for hazards; the `GLACIER_MAN` `Bat` text (no
    trigger in that map); `PRECIOUS_WOMAN` starts at `room4`; the control check starts at `TIMBER_WOMAN` `room9`.
- Automated checks after the fixes: `engine:test` 710, `core:test` 2400, `core:smokeTest` 469, 0 failures; `lwjgl3:jar` built. Brief republished (version 2, same link).
