# 121: Enemy clones of a playable character

- Status: idea
- Type: feature
- Objectives: `O4`, `O8`
- Depends on: `081`
- Related: `011`, `024`, `030`, `031`, `048`, `084`, `112`, `118`, `120`
- Branches: none yet

## Goal

A level can hold many enemies that fight with the same moves as a playable character, for example numerous Mega Man clones.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `MegamanClone` is a new class in `enemies/`, with no slot. It shares its move parts with `Megaman`. It is pooled, and Tiled places it as a normal enemy.
- It does not extend `Megaman`: a slot-free `Megaman` crashes at `Megaman.kt:364`. Without the crash, it stays in `PRE_PLAY`, hidden and with no movement, and it writes world gravity and the camera.
- `081` must first move the 8 behaviors, the body and the controller off the `Megaman` receiver. `084` split the support state, but not the moves.
- Each clone holds its own `InputFrameBuilder`. A brain calls `build(heldBits)` once per tick, and `read()` returns the cached frame. This needs no engine change.
- First moves: run, jump, air dash and buster. Each clone class chooses its own list. Player enhancements and owned weapons do not apply.
- A limited animation set and the buster atlas: `Megaman` has 28 components and 431 `Animation` objects, and a typical enemy has 9 components.
- A clone extends `AbstractEnemy` until `048` removes it, so it has drops, cull, spawn blink and the enemy contact filter.
- A clone boss holds `BossComponent` itself, because it needs no slot.
- Player-only kills: `PlayerDeath`, `CameraEdgeBlock` and `GravityBlock` loop over players, and `MegaLevelScreen.kt:333` reads slot 0. One shared kill capability at those 4 sites covers clones.

Known limits: clones with one brain and no collision between them stack on one spot, and clones stack sounds unless they use a camera gate.

## Owner's input

2026-11-02 (verbatim):

> will the described setup also work if I want P2 to be just an enemy and not a boss? What if Mega Man has to go through a level where numerous Mega Man clones exist as enemies but not necessarily bosses?

## Decisions

- (2026-11-02, owner) Keep `Megaman` slot-only. A clone is a separate class with shared parts (option (b) in `NOTES.md`). A simple imitator breaks the "same moves" rule, so it is only for a one-move decoy.
- (2026-11-02, owner) Clones die from the player-only kills too, through one shared capability at the 4 sites. This keeps the kill rule of `120` true for clones.
- (2026-11-02, owner) Player enhancements and owned weapons do not apply to clones.
- (2026-11-02, owner) Build nothing for clones now. The seam comes through `081`, because it has a real caller.

- (2026-11-10, owner, from `060j`) After `060j`, the player moves are plain functions that take `player: MegaPlayer`
  (`playerRunButton`, `playerBody`, `playerJumpBehavior`, `playerWallSlideBehavior`, `playerAirDashBehavior`), and the
  run's movement part is the generic `runStep` in `entities/movement/Run.kt`. A clone is not a `MegaPlayer`: this
  item widens the parameter type to what a clone has when it starts. Each such function has a `TODO` that names this
  item.

## Open questions

- Does `081` choose thin classes or one `PlayerEntity(def)`? One class does not fit a per-class pool for clones (see `081`).

## Sources

- `NOTES.md` in this folder.
