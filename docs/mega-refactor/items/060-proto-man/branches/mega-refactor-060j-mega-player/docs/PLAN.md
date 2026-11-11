# Plan: mega-refactor-060j-mega-player

The source of truth for the class shape is `temp/mega-refactor-060j-mega-player/scan.md` ("Proposed `MegaPlayer`",
Q1, Q2, Q4, Q5), with the owner answers in `SCOPE.md` ("Decisions").

## New APIs

| API | First production caller |
|---|---|
| `players/MegaPlayer` | `Megaman`, `MegamanX` |
| `cameraFocusHeight` | `Megaman`, `MegamanX` |
| abstract `defineBodyComponent()`, `defineMomentumComponent()`, `defineKnockbackComponent()`, `defineStunComponent()`, `defineBehaviorsComponent()`, `defineControllerComponent()`, `defineSpritesComponent()`, `defineAnimationsComponent()` (members; D3 moves the bodies) | `Megaman`, `MegamanX` |
| `teleportableComponent()` (open) | `Megaman` |
| `onLevelDefeated(level)` (open) | `Megaman` |
| `onPlayerSpawn`, `onPlayerDestroy`, `onPlayerEvent`, `afterPlayerDamage` (open) | `Megaman`, `MegamanX` |
| `TestMegaPlayer` (test only) | `TestHealthPlayer`, `RunPlayer` |
| `entities/movement/Run.kt`: `RunParams`, `runStep`, `maxRunSpeed` (scope change B) | `playerRunButton`; `Megaman.getMaxRunSpeed` |
| `playerRunButton(player, side, params, blocked, onBlocked)` (replaces the extension) | `Megaman` controller |

## Steps

1. **`MegaPlayer`.** Add `players/MegaPlayer.kt` as in the scan sketch: the shared members, the open `init()` that adds
   the shared components in today's order, the final `onSpawn` (with `setMaxHealthFromHeartTanks()`, D1), `onDestroy`,
   `onEvent`, `takeDamageFrom`, the hooks. One concise `TODO` where it reads `MegamanValues` (D2).
   Check: compiles with steps 2 and 3 (the classes must move in the same step to compile).
2. **`Megaman` on `MegaPlayer`.** Extend it; remove the members that moved; move each `define...Component()` body into
   `Megaman.kt` as the member override (D3); put the class lines in the hooks at today's points (scan Q2); delete the
   emptied component files. Keep `IScalableGravityEntity` and every Megaman-only part on `Megaman`.
3. **`MegamanX` on `MegaPlayer`.** The same for X.
   Check for steps 1 to 3: `core:test`, `core:smokeTest`.
4. **Shared functions.** Scope change (2026-11-10): the lifecycle functions become private members of `MegaPlayer`
   (A), and the run splits into the generic `entities/movement/Run.kt` and the player `playerRunButton` (B), as in
   `SCOPE.md`; the 4 other moves become plain functions with `player: MegaPlayer` and a `TODO` for item `121` (C).
   Then: The 18 functions take a `MegaPlayer` receiver (`playerBody`: `MegaPlayer` +
   `IScalableGravityEntity`); the 6 in scan Q4 keep their wider receiver. The 2 `playerSlot!!` reads become `slot`
   (item `148`). Check: `core:test`.
5. **Test doubles.** `TestMegaPlayer` with stubs for the abstract members; `TestHealthPlayer` and `RunPlayer` extend
   it; remove the `onEvent` override that becomes final. Check: `core:test` (same test count, all pass).
6. **Docs.** `players/README.md`: the base class rule, the parts diagram, "The order of a spawn". Branch
   `docs/NOTES.md`: "Fit for future characters" (the notes for Bass, Axl, Zero; no code).
7. **Checks.** `engine:test core:test core:smokeTest lwjgl3:jar`. Review before the play test (subagents: the diff
   against the old code, with the component order; the play checks). Fix, check again. Play checks and Diffity.
