# Plan: mega-refactor-134b-interpolation-audit

How to do the work in `SCOPE.md` (approved 2026-11-09). No game launch in any step (also for subagents).

## Steps

### 1. Test map rooms `attach` and `pipe`

- Changes: a subagent adds 2 rooms to `assets/tiled_maps/tmx/RefactorTest134.tmx` with the `level-builder` skill:
  `attach` (a `FireMet`, a `PetitDevil`, an `Electrocutie`, a `Spike` on a moving `RocketPlatform` or other parent,
  with the spawn properties copied from a live map) and `pipe` (a `PipePortal` pair that Megaman can enter). Painted
  `tiles`, a player spawn in each room. The rooms of `134a` do not change.
- Check: the `level-analyzer` check passes; each spawner name is a class in the package of its layer.
- Done: yes

### 2. Draw link (engine) and its first caller

- Changes: `SpritesComponent.drawOffsetBody: (() -> IBody?)?`; `SpritesSystem.getDrawOffset` uses the linked body
  when the function gives one, else the entity's own body; an entity with no body and a link gets the linked offset.
  `GutsTankFist` sets the function in `init()` (the tank body while `ATTACHED`). The `SpritesSystem`, `GutsTankFist`
  and `GutsTank` `TODO`s change to match.
- Check: new engine tests (a link, a `null` link, an entity with no body and a link, no link). A break of the link
  read makes a test fail. `engine:test`, `core:test`.
- Done: yes

### 3. Restart of the step move (engine) and `PipePortal`

- Changes: `IBody.restartStep()` and `Body`: the step start becomes the current position, and the step move becomes
  0. `PipePortal.pullEntityIn` calls it after the snap. The `WorldSystem` `TODO` changes to name the guard as the
  fallback.
- Check: new engine tests (outside a step: 0; inside a step: only the move after it counts). `engine:test`.
- Done: yes

### 4. The other link callers

- Changes: `ReactorManProjectile` (held), `PreciousGem` (shield of `PreciousWoman`; gem of a cluster, linked to the
  cluster's player), `PreciousGemCluster` (its player), `BigAssMaverickRobotHand` (`FOLLOW`), `BigAssMaverickRobotBody`,
  `FireMetFlame`, `PetitDevilChild`, `Electrocutie` children, `CeilingCrusher` block (if it has sprites), `Spike` on a
  parent, `MegamanXSabreSlash` (the X body). Each function gives `null` in the detached states. A case that turns out
  not to need a link is written in the process log with the reason.
- Check: `core:test`, `core:smokeTest`; a read of each detach path.
- Done: yes

### 5. Shapes and the `I` key

- Changes: `MegaLevelScreen.draw(renderer)` moves the game camera by the camera draw offset and puts it back, as
  `draw(batch)` does; its `TODO` goes. The `I` key does nothing while `clock.paused`.
- Check: `core:test`; read the shape draw path for a camera that is not put back.
- Done: yes

### 6. Full automated checks and `O5-R2`

- Changes: none.
- Check: `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar`. The `--diagnostics` run is in the play checks.
- Done: yes

### 7. Review before the play test

- Changes: fixes from the review.
- Check: subagents review the diff (also `O2-R7`: no gameplay read of the link or the step state) and every manual
  check in `CONTRACT.md`. Read the `081f` folder again for overlap in `MegamanXSabreSlash.kt`.
- Done: yes

### 8. Play test and diff review

- Changes: none. Give the owner the play checks; open Diffity.
- Check: the owner's report.
- Done: yes
