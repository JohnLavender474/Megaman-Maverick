# Plan: mega-refactor-045a-pool-ownership

The owner asked for the branch end to end with no stops; the plan was written with the scope and needs no approval.

## Steps

### 1. Scan and research

- Changes: none in code. Scan inventory in `temp/mega-refactor-045a-pool-ownership/` (main checkout); lifetime research
  in `NOTES.md`.
- Check: every `free()` site, non-reclaim fetch, hidden default and per-frame candidate is sorted.
- Done: yes

### 2. Scratch-only pool and debug checks

- Changes: `GameObjectPools` (no `reclaim`, no `free`, poison and no reuse with `debugChecks`); `--poolChecks` in
  `DesktopLauncher` and `MegamanMaverickGameParams`; `reclaim` flags removed from the extensions.
- Check: `GameObjectPoolsTest`; compile.
- Done: yes

### 3. Owned values

- Changes: `OwnedArray`; every free pair and non-reclaim fetch becomes an owned field, an `OwnedArray` entry, or a
  per-spawn `new`.
- Check: `OwnedArrayTest`; no `GameObjectPools.free` and no `reclaim` left (`grep`).
- Done: yes

### 4. Stored scratch and hidden defaults

- Changes: spawn-prop receivers copy; `ElecDevilBody.getPositionOf(out)`; `WorldPathfinder` copies; core
  `Camera.toGameRectangle` default removed; `calculateJumpImpulse` scratch form.
- Check: `WorldPathfinderTest` copy case; `PoolOwnershipGuardTest`.
- Done: yes

### 5. Per-frame sweep

- Changes: `ArcMotion`, `Pendulum`, `toBoundingBox`, `ShapeUtils`, and the core lambdas listed in `SCOPE.md`.
- Check: engine motion tests; compile.
- Done: yes

### 6. Docs

- Changes: `docs/CODE_STYLE.md` rule, `core/.../utils/README.md`, branch docs, brief.
- Check: brief render has no `✗`.
- Done: yes

### 7. Automated checks and review before the play test

- Changes: fixes from the review.
- Check: `engine:test core:test core:smokeTest lwjgl3:jar`; review findings logged.
- Done: yes
