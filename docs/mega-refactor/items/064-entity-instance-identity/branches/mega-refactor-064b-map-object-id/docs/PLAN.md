# Plan: mega-refactor-064b-map-object-id

- Status: approved
- Approved: 2026-10-28

JDK: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13` (the default JDK 25 does not run Gradle 8.10.1). Scratch
files: `/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/064b/`.
One commit per step. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. Build command (all checks):
`JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar`.

## New API and first production caller

| API | Module | First production caller (step) |
|---|---|---|
| `MapObjectComponent(var id: Int) : IGameComponent` (`C/entities/state/`; `reset()` keeps the id) | core | `MegaGameEntity.onSpawn` writes it (step 4) |
| `val IGameEntity.mapObjectId: Int?` (`C/entities/state/MapObjects.kt`) | core | `MegaGameEntities.add`/`remove` (step 4); the readers (steps 5, 6) |
| `Laser.setReflector(reflector: MegaGameEntity, parent: Laser)` (`internal`): the existing reflect lines (`ignoring.clear()`, `addAll(parent.ignoring)`, `add(reflector.id)`, `owner = reflector`) moved into one function, with no change | core | the `Laser` updatable, at the old place (step 2) |
| `Laser.ignoringEntities` (`private`, `ObjectSet<IGameEntity>`) | core | `Laser.isLaserIgnoring` (step 6) |

Removed: `MegaGameEntity.id`, `RANDOM_ID_POOL`, `CURRENT_NEW_ID` (step 8); class `DarknessV3` (step 7).

`setReflector` exists so that the temporary `Laser` suite runs the real reflect code. It is a pure move with a
production caller. The name and the parameters can change in step 2 if the moved code needs it; the process log says
so.

## Temporary test code

- `Temp064bFrozenIdRegistry` and `Temp064bMapIdEquivalenceTest` (core test sources): added in step 3, switched to the
  new code in step 4, deleted in step 12 in their own commit.
- `Temp064bFrozenLaserIgnore` and `Temp064bLaserIgnoreEquivalenceTest` (core test sources): added in step 3, run again
  after step 6, deleted in step 12 in the same commit as the other suite.

## Steps

### 1. Merge `master` and baseline

- Changes: merge `master` into the branch (it holds `9d3f67fc5`, the `SpiderWeb` fix with `SpiderWebDestroyTest` and
  `OnDestroySuperCallTest`; and the `RUMBLECADE_FACTORY` change if it is there). No other change. Then the clean build.
  Write the engine and core test counts in the process log. Save the unique warning lines
  (`grep '^w:' | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u`) to `064b-warnings-baseline.txt` in the scratchpad.
- Check: the merge has no conflict (the branch has docs only); the build passes, 0 failures; `SpiderWebDestroyTest`,
  `OnDestroySuperCallTest` and `StarknerAliveTest` are in the report and pass; the counts and the warning file exist.

### 2. `Laser.setReflector` (pure move)

- Changes: `C/entities/hazards/Laser.kt`: move the reflect lines (`:585-590` on the base: the `ignoring` copy, the
  reflector id, `owner = reflector`) into `internal fun setReflector(...)` on the reflecting laser, called at the old
  place. The order of the statements around it does not change.
- Check: `core:test` passes; review: the diff is a move only.

### 3. Temporary suites on the old code

- Changes, core test sources:
  - `Temp064bFrozenIdRegistry`: a frozen copy of the old id logic (`CURRENT_NEW_ID`, `RANDOM_ID_POOL` with the same
    `Pool` setup and the eager fetch in `onSpawn`, the free of a negative id in `onDestroy`, and the old `idToEntities`
    add and remove with `getOfId`/`existsAnyOfId`).
  - `Temp064bMapIdEquivalenceTest`: test entities are `MegaGameEntity` subclasses on a real `GameEngine` with a relaxed
    `MegamanMaverickGame` mock (as `StarknerAliveTest`). Each keeps an old-style id field and feeds the frozen copy at
    the old points (in `onSpawn` before `super`, and in `onDestroy`). A seeded generator (at least 200 seeds, at least
    200 operations each) draws: spawn with a map id (some ids shared by several entities), spawn with none, destroy, a
    destroy and a spawn in one frame, a pooled respawn with another map id or with none, `engine.update`,
    `engine.reset()`. Map ids are unique to the suite (the registry is a global object). After each operation, and
    from a probe `UpdatablesComponent` entity during `update`, it compares for every map id used: the ordered set of
    `MegaGameEntities.getOfId` and `existsAnyOfId` with the frozen copy. In this step the real registry is the old
    code, so the suite proves the frozen copy and the generator. A check with a wrong lookup (for example the reversed
    order) must fail, to show that the suite can fail.
  - `Temp064bFrozenLaserIgnore`: a frozen copy of the old `Laser` ignore rule (the `ignoring: ObjectSet<Int>` with the
    owner id at spawn, the Tiled ids, the copy from the parent and the reflector id in the reflect step, the clears;
    the check `ignoring.contains(id)`), with ids from `Temp064bFrozenIdRegistry`.
  - `Temp064bLaserIgnoreEquivalenceTest`: real `Laser` objects spawned on the real engine (owner and `ignore*` map
    objects in the spawn props, as `LaserBeamer` gives them), a reflecting laser driven through `setReflector`, and
    owners, reflectors and other entities (map-spawned, with a player-spawn-like map id, and runtime). Seeded sequences:
    spawn, destroy, pooled respawn, reuse of a freed random value, shared map ids, reflector changes and resets. For
    every candidate entity, `Laser.isLaserIgnoring` is compared with the frozen rule. Equal whenever the owner and the
    current reflector are live in the spawn that was added. In the window cases (the owner or the reflector destroyed
    while the laser holds it) each difference is counted and logged; on the old code the count is 0.
- Check: `core:test` passes on the old production code, with both suites; the mutation check fails as expected (then
  removed); the counts of each operation kind are in the process log.

### 4. `MapObjectComponent` and the registry

- Changes:
  - New `C/entities/state/MapObjectComponent.kt` (`var id: Int`, `reset()` keeps the id) and
    `C/entities/state/MapObjects.kt` (`val IGameEntity.mapObjectId: Int?`).
  - `MegaGameEntity.onSpawn`, at the line that sets `id`: if `ConstKeys.ID` is in the spawn props, get or add the
    component and set its `id`; else remove the component. The field `id` stays in this step (its readers move in
    steps 5 and 6).
  - `MegaGameEntities.add`/`remove`: index `idToEntities` by `entity.mapObjectId`, and only when it is not `null`.
- Check: `core:test` passes, including `Temp064bMapIdEquivalenceTest` (now against the new index) and
  `StarknerAliveTest`, `SpiderWebDestroyTest`.

### 5. Readers of classes 0, 1 and 2

- Changes (no cast to `MegaGameEntity` for the id):
  - Class 0 (log text): `Block`, `FallingLeaves`, `Gate`, `GauntletMaster`, `MockingByte` (`:560`), `Starkner`
    (`:190`, `:209`) print `mapObjectId`.
  - Class 1: `AstroAssAssaulter`, `BikerKibbo`, `Jetto`, `GreenKoopa`, `RedKoopa`, `Starkner`, `SteamRollerMan` use
    `mapObjectId!!`; `SteamRollerMan.canSpawn` compares with `it.mapObjectId`; `MockingByte.findNewNest` uses
    `sortedNests[0].mapObjectId!!`.
  - Class 2: `Rain` (`:197`, `:200`), `Bat`, `BulletBill`, `FallingIcicle`, `FireWall`, `Fireball`, `SpitFireball`,
    `SmallIceCube`, `TubeBeamerV2`: "the hit entity has a map id and the list holds it".
- Check: `core:test` passes (both suites, `StarknerAliveTest`, `OnDestroySuperCallTest`); grep: no
  `as MegaGameEntity).id` in `core/`; review: each changed `onDestroy()` keeps `super.onDestroy()`.

### 6. `Laser` (the exact option)

- Changes: `C/entities/hazards/Laser.kt`:
  - `ignoring` keeps the Tiled ids and, at spawn, the owner's map id when the owner has one;
  - new private `ignoringEntities`: the owner at spawn; in `setReflector`, the copy of the parent's
    `ignoringEntities` and the reflector;
  - `isLaserIgnoring`: `entity in ignoringEntities`, or the entity's map id is in `ignoring`; then the class list as
    today;
  - `ignoringEntities` is cleared everywhere `ignoring` is cleared (`onDestroy`, `resetReflectingLaserIfAny`,
    `setReflector`).
- Check: `core:test` passes; `Temp064bLaserIgnoreEquivalenceTest` passes: equal in all live cases, every difference is
  a window case, and the window count is in the process log.

### 7. Delete `DarknessV3`

- Changes: delete `C/entities/special/DarknessV3.kt`. In `SpecialsFactory.kt:49`, remove the commented
  `/* DarknessV3(game) */` (the code of the line stays the same).
- Check: the build passes; grep: no `DarknessV3` in `core/` or `assets/`.

### 8. Remove the old id

- Changes: `MegaGameEntity`: remove `id`, `CURRENT_NEW_ID`, `RANDOM_ID_POOL` (and the unused `Pool` import), the
  pool fetch in `onSpawn` and the pool free in `onDestroy`; `toString` prints `mapObjectId`.
- Check: the build passes (the compiler proves that no reader of `id` is left); `core:test` passes, including both
  suites; grep: no `RANDOM_ID_POOL`, `CURRENT_NEW_ID` in `core/`.

### 9. Permanent tests

- Changes: new core test (for example `MapObjectComponentTest`), the cases in `CONTRACT.md`: the component after a
  spawn with and without `ConstKeys.ID` and after a pooled respawn without it or with another id; the id in
  `onDestroy`; `getOfId` order, shared ids, removal at destroy, no runtime entity; `Laser.isLaserIgnoring` for the
  owner, the reflector, the owner's map id, a Tiled id, and a runtime entity.
- Check: `core:test` passes.

### 10. Docs

- Changes: root `CLAUDE.md`: the `MegaGameEntity` line drops `id`; one line says that the map object id is a
  `MapObjectComponent` (read with `mapObjectId`), present only on entities spawned with `ConstKeys.ID`, and that
  `MegaGameEntities.getOfId` finds the entities of a map object.
- Check: the lines read correctly.

### 11. Contract checks

- Changes: none (fixes only if a check fails). If `master` changed since step 1, merge it first and run step 1's
  counts again.
- Run the clean build and all tests; compare the warnings with step 1 (list each new warning in the process log); the
  greps in `CONTRACT.md`; `contact-rule-selection.txt` unchanged; the review items in `CONTRACT.md`;
  `SpiderWebDestroyTest` and `OnDestroySuperCallTest` pass. Then short smoke launches (about 15 to 20 seconds each) of
  `--level MOON_MAN --room room12`, `--level PRECIOUS_WOMAN --room room2` and `--level SMB3_1 --room room2`, to check
  that the levels load with no exception. Then STOP and ask for the owner's play test with the manual checks in
  `CONTRACT.md`.
- Check: every automated box in `CONTRACT.md` ticked and logged.

### 12. After the play test

- Changes: delete `Temp064bMapIdEquivalenceTest`, `Temp064bFrozenIdRegistry`, `Temp064bLaserIgnoreEquivalenceTest`
  and `Temp064bFrozenLaserIgnore` in their own commit. Run the automated checks again. Update the process log and the
  status block, read the folders of the other active branches again for overlap, and update `docs/NOTES.md`. Do not
  merge.
- Check: build and tests pass; the test counts drop only by the temporary suites; the warnings are the same as in
  step 11; no `Temp064b` left in `core/` or `engine/`.
