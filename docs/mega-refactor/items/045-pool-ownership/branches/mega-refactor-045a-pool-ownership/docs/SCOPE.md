# Scope: mega-refactor-045a-pool-ownership

- Status: draft; the decisions are Claude's choice, pending owner confirmation
- Approved: no (the owner asked for the branch end to end with no stops for questions, 2026-11-16)

## Goal

Give `GameObjectPools` one ownership contract (a pool object is a scratch for the current frames and is never
stored), remove manual `free()`, add debug checks behind a launch flag, fix every stored scratch that the scan
found, and move the per-frame `new` of pooled types in hot paths to the pool or to owned fields. Write the rule in
`docs/CODE_STYLE.md` and a package README.

## Item

`045` (`ITEM.md`), proposed scope, copied:

- Write the contract: scratch objects live until a defined reclaim point and are never stored. Spawn props never pass
  ownership.
- Owned value fields and owned reusable lists for entities. Remove the `onSpawn` fetch / `onDestroy` free pairs.
- An inline `borrow {}` for local scratch use.
- Debug checks: poison on reclaim (NaN), track the generation, assert on `free()` of a reclaimable object.
- Make hidden pooled default parameters explicit (for example `Camera.toGameRectangle()`).
- Sweep: find each per-frame `new` of a pooled type in `engine/` and `core/`, and move it to a pool scratch or an
  owned field. A value made once stays. A value that goes into spawn props is not a pool scratch.
- Docs: a rule in `docs/CODE_STYLE.md`; the package README of `GameObjectPools`; consider a source-scan test.

Plus the owner's research request: is `DEFAULT_CALLS_TO_WAIT = 3` adequate (`NOTES.md`).

## Current state (scan on `a69bcd392`)

- `GameObjectPools` (`core/.../utils/GameObjectPools.kt`): a 3-bucket arena; `fetch(reclaim = false)` gave an
  object that the caller had to `free()`. 19 `free()` sites in 13 entity files, and about 60 non-reclaim fetches, most
  of them never freed (a slow leak out of the pool). `free()` of a reclaimable object was a silent double free.
- Hidden pooled defaults: `Camera.toGameRectangle(out = fetch)` (`Extensions.kt:112`),
  `MegaUtilMethods.calculateJumpImpulse(..., out = fetch)` (2 overloads), `getDefaultCameraPosition(reclaim)`, and the
  `reclaim: Boolean = true` flag on 17 extension functions.
- Stored scratch (a real bug class; the object changes under the entity after 3 frames): `Lift.stopPoint`
  (`Lift.kt:63`), `CeilingCrusher.start` (`:95`), `ElectrocutieChild.spawn` (`:84`, fed by scratch from
  `Electrocutie.kt:100`), `DarknessV4` light centers, `ExplosionField`/`Tree`/`WindyGrass`/`ForceDecoration`/
  `Darkness`/`DecoratedGravityForce` bounds kept from spawn props, `SniperJoeShield`/`TubeBeam` trajectories kept from
  spawn props, `ElecDevilBody.getPositionOf` (a "free manually" fetch that was never freed, and was kept in the launch
  queue), and the async pathfinder (scratch `IntPair` start and target read on a worker thread for up to 500 ms).
- Per-frame `new` scan: 1,611 constructor calls of pooled types in `engine/src/main` and `core/src/main`; 1,394 are
  fields, `init`/`define*` code or constant tables. The rest were read one by one. Inventory:
  `temp/mega-refactor-045a-pool-ownership/scan-new.tsv` (main checkout), with the script `scan_new.py`.
- Hot per-frame `new` found: `ArcMotion.update` (2 `Vector2` per frame per arc), `Pendulum.update` (1 per frame per
  pendulum), `IRectangle.toBoundingBox` and `Rectangle.toBoundingBox` (2 `Vector3` per call; the camera cull test of
  each entity calls it), `ShapeUtils.overlapCircleAndPolygon` (up to 2 per polygon edge), body `preProcess` and
  updatable lambdas in `Met`, `BunbyTank`, `ElectrocutieChild` (2), `SniperJoeShield`, `Popoheli` (a new fixture
  offset each frame), the `D1000` forecasts, and the camera rectangles of `DeathFadeOut`, `BeamInView` and
  `PlayerCheckpointManager`.
- `forceReclaimAll()` and the public `put()` and per-type `callsToWait` have no callers.

## In scope

1. `GameObjectPools`: scratch only. Remove `reclaim`, `free()`, `forceReclaimAll()` and the public `put()`. Rename
   `DEFAULT_CALLS_TO_WAIT` to `SCRATCH_LIFETIME_FRAMES` (value 3). `fetch` of an unknown type throws with a clear
   message.
2. `OwnedArray<T>` (`core/.../utils/OwnedArray.kt`): an owned, reusable list. First callers: `RodentMan`, `DrillHead`,
   `Laser`, `TubeBeamerV2`, `RatRobot`, `UFOhNoBot`, `WilyCapsuleTentacle`, `WavyTentacleOfJoints`, `MockingByteNest`.
3. Remove every `onSpawn` fetch / `onDestroy` free pair; each value is an owned field, an `OwnedArray` entry, or a
   plain `new` made once per spawn or event (`SteamRoller`, `SpringBouncer`, `WilyFinalBoss`, map shapes).
4. Debug checks behind `--poolChecks` (new launcher flag, off by default): poison on reclaim and no reuse.
5. Hidden defaults explicit: remove `Camera.toGameRectangle(out = fetch)` (callers pass an owned rectangle and use the
   engine function); `calculateJumpImpulse` has no `out` and returns a scratch; the `reclaim` flags go.
6. Fix each stored scratch listed above. Entities copy spawn-prop values into owned fields.
7. Sweep the hot per-frame `new` sites listed above.
8. Docs: `docs/CODE_STYLE.md` rule, `core/.../utils/README.md`, `PoolOwnershipGuardTest` (no pool fetch in a default
   parameter), unit tests for the pool and `OwnedArray`, a pathfinder copy test.
9. Research note on the lifetime value (`NOTES.md`). Keep 3.

## Out of scope

- Per-event `new` (a shot, a spawn, a state change): it is not per frame. The sweep leaves these, and the values that
  go into spawn props, as they are.
- Debug-only shape suppliers (`--debugShapes`) that make a `GameLine` per frame (`Saw`, `SwingingAxe`,
  `SwingingPlatform`): debug runs only.
- `ShapeUtils.intersectRectangleAndLine` adds a new `Vector2` per hit to the caller's set (an output API change);
  `Darkness.handleLightEvent` (no live level uses `Darkness` outside `TEST_1`). Backlog candidates in `TODO.md`.
- Moving the reclaim to ticks (item `010`).

## Decisions (Claude's choice, pending owner confirmation)

| ID | Question | Options | Choice |
|---|---|---|---|
| D1 | How do long-lived values work? | a) keep `free()` and add an assert; b) scratch-only pool, owned fields and `OwnedArray` | b: no `free()` means no double free and no leak; an entity is pooled, so an owned field is made once |
| D2 | Add `borrow {}`? | a) add it; b) not now | b: no caller needs an exact lifetime; every local use works with a scratch, and the rule "API only with a caller" applies. Listed as a backlog candidate |
| D3 | Debug checks | a) poison + generation map + `free()` assert; b) poison + no reuse | b: with no `free()` the assert has nothing to check; "no reuse" keeps a stored scratch NaN for good, which a generation map would only report. Flag `--poolChecks`, off by default |
| D4 | Lifetime value | a) keep 3; b) 4 or 5 | a: see `NOTES.md`; no unsafe lifetime in the code is shorter than seconds |
| D5 | Guard test | a) none; b) a source scan for pooled default parameters | b: the one rule that text can find; a per-frame `new` cannot be found by text |
| D6 | Fix the stored-scratch bugs here? | a) file them as bugs; b) fix them in this branch | b: they are the contract violations that the item is about; named in `CONTRACT.md` as intended changes |

## New API and first callers

| API | First production caller |
|---|---|
| `OwnedArray` (`add`, `get`, `clear`, `size`, `isEmpty`, `forEach`, `any`) | `DrillHead.hoverSpots` (`forEach`), `RatRobot.triggers` (`any`, `isEmpty`), `RodentMan.ratSpawns` (`get`, `size`) |
| `GameObjectPools.debugChecks` | `MegamanMaverickGame.create()` |
| `GameObjectPools.SCRATCH_LIFETIME_FRAMES` | `GameObjectPool` |
| `MegamanMaverickGameParams.poolChecks`, `--poolChecks` | `DesktopLauncher` |
| `ElecDevilBody.getPositionOf(row, column, out)` (new `out`) | `ElecDevil`, `ElecDevilBodyPieces` |
| `Pendulum.getPointFromAnchor(distance, out)`, `ArcMotion.computeBezierPoint(..., out)`, `ArcMotion.compute(t, out)` (new `out`) | `Pendulum.update`, `ArcMotion.update`, `Saw`, `SpikeBall`, `SwingingPlatform`, `MoonHead` |

## Affected areas and risks

- 60 or more entity files change their spawn-time data handling. Risk: a value that was a shared reference on purpose
  (a live link to another object) becomes a copy. Each changed site was read; none was found that relies on a live
  link. The play checks cover the live entities.
- `Camera.toGameRectangle` in `core` is gone; `core` now uses the engine function with an owned rectangle.
- `ArcMotion`, `Pendulum` and `ShapeUtils` in `engine` change their internals; their engine tests cover the math.

## Overlap with other branches

See `NOTES.md`, "Overlap with other branches".

## Objective check

| Rule | Effect | Note |
|---|---|---|
| `O1-R1` No global session state | waiting | `GameObjectPools` is a global object already; the branch adds `debugChecks` to it. Same pattern, no new kind |
| `O1-R3` No new fragile order | helps | spawn props no longer depend on the reclaim order |
| `O1-R4` Typed state | no effect | |
| `O1-R5` Code only with a caller | helps | removes `free`, `put`, `forceReclaimAll` and per-type lifetimes (no callers); each new API has a caller (table above) |
| `O2-R7` Gameplay does not depend on rendering | no effect | |
| `O3-R1` No JVM-only API on the web path | no effect | |
| `O5-R1` No per-frame allocation in common paths | helps | the hot sites above; `ArcMotion`, `Pendulum`, `toBoundingBox` are in every level with these motions or culling |
| `O5-R2` Measure hot paths | partly | no `--diagnostics` capture (no game launch on this branch); sizes measured on the JVM (`NOTES.md`); a capture can come with the owner's play test |
| `O6-R1` CI passes | helps | new unit tests and a guard test |
| `O6-R2` Named checks | helps | `CONTRACT.md` |

## Open questions

- The decisions D1 to D6 wait for the owner's confirmation.
