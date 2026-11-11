# Scope: mega-refactor-060a-shared-run-body

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file.

- Status: approved
- Approved: 2026-11-10 (brief)

## Goal

Refactor only, with the contract "Megaman plays the same". Move Megaman's run, body (fixtures, size, gravity table,
clamps, water, ice) and max run speed out of `Megaman` receiver functions into `players/` functions with parameters.
Megaman calls them with his values and his hooks. No Proto Man code: `060b` is the second caller.

## Item

`060` (Proto Man). From `ITEM.md`, "Proposed phases" and "Decisions" (2026-11-10, owner):

> `060a` is a refactor-only branch, as `081a` was for X. Mega's run, body and momentum are `Megaman` receiver
> functions, so a thin `Protoman` cannot call them, and a copy breaks `O8-R1`.

Modes: the branch changes no mode. `SINGLE`, `COOP` and `PVP` play the same (`O8-R3`).

## Current state

Scan of 2026-11-10, `master` `1b015cc6a`. Paths relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

- Run: `entities/megaman/components/ControllerComponent.kt:174-216` (`Megaman.run`), with `onJustStopRunning` and
  `onNotRunning` below it, wired at `:41-71`. Megaman-only reads: `currentWeapon == RODENT_CLAWS && shooting` (block and
  facing), `movementScalar`, `MegamanValues.RUN_*`, `getMaxRunSpeed()`. The rest uses shared state: `RunComponent`,
  `running`, `isRunSidePressed`, `canMove`, `damaged`, `teleporting`, `inPlay`, `stopMomentum`.
- Max run speed: `entities/megaman/components/BodyComponent.kt:429-434` (`Megaman.getMaxRunSpeed`). It reads the
  water scalar and checks `currentLevel == MOON_MAN` (an existing `O7-R3` violation).
- Body: `BodyComponent.kt:61-427` (`Megaman.defineBodyComponent`). Shared shape: player, body, feet, feet gravity,
  head, 2 sides, damageable, water listener and teleporter listener fixtures; the `preProcess` that sizes the body
  (stand or slide/crouch height), positions the fixtures, applies the gravity table, the friction, the clamps and the
  damageable rect for each direction, and keeps the momentum on ice. Megaman-only parts: `aButtonTask` writes in the
  feet and water receivers, the land sound (`canMakeLandSound`), the Mario bump sound in the head receiver,
  `wallSlideNotAllowedTimer` in `onBounce`, the `GutsTank` side rule, the needle spin and axe shield fixtures and their
  `preProcess` lines, `slipSliding`.
- Gravity table: 6 `internal var` fields on `Megaman.kt:212-217`, signed by direction in the `direction` setter
  (`Megaman.kt:147-174`), read only by the body `preProcess`. `gravityScalar` (`IScalableGravityEntity`) is set by the
  water receiver.
- Momentum and room carry (`MovementComponents.kt`) are already shared components; Megaman's 2 definitions are only
  wiring, with `getMaxRunSpeed()` as the speed supplier. They stay.
- X has its own body and run (`megamanx/MegamanXBody.kt`, `MegamanXController.kt:69`), on purpose (X6 feet and walk).
- Tests: `MegamanRunTest` (calls `megaman.run` at `:68,73`), `MegamanJumpTest`, `MegamanBehaviorsTest`,
  `MegamanWallSlideTest`, `MegamanAirDashTest`. No test covers the body fixtures or the gravity table.

## In scope

- `players/PlayerRun.kt`: the run step and the left and right controller entries, with a `RunParams` (impulses, the
  ramp, the max speed, the movement scalar, and a `blocked` hook with its facing rule for Rodent Claws).
- `players/PlayerBody.kt`: the shared fixtures and the shared `preProcess`, with a `PlayerBodyParams` (sizes, the
  gravity values, the clamps, and hooks for feet land and leave, water begin and end, bounce, head hit and the
  side stick rule). It returns the body and the fixtures that Megaman needs for his own parts (needle spin, axe).
- The gravity table as typed state: one component in `players/` with the 6 values, signed by direction. The
  `direction` setter calls it in place of the 6 lines.
- `playerMaxRunSpeed(...)` in `players/`, with the water scalar; the `MOON_MAN` check stays in Megaman's own
  parameter, unchanged.
- `Megaman` calls these with `MegamanValues` and his hooks. His fixture order stays the same.
- Tests: the shared run, the gravity table per direction, and a fixture test (types, sizes, order) of Megaman's body
  before and after.
- `players/README.md`: add the run and the body to "The parts".

## Out of scope

- Proto Man (`060b`). X (keeps his own body and run).
- Swim, ground slide, climb, crouch, air dash, wall slide: they stay as they are.
- The camera part of the `direction` setter (`TODO` for `029d` stays).
- `AButtonTask` stays Megaman's; the hooks write it.
- The `MOON_MAN` level check (`O7-R3`): not fixed here.

## Affected areas and risks

- Fixture order sets the contact order. Megaman's fixtures must be added in the same order. The fixture test checks it.
- A missed line in the `preProcess` changes Mega's feel (gravity, slide height, ice). The tests and the control play
  checks cover it.
- The direction setter: the gravity signs must apply at the same time as now (in the setter, before the next step).

## Overlap with other branches

- `081f` and its sub-branches change `D1000`, X6 intro and X sprite files, `PlayerDamageNegotiator`, `StunComponent`
  and `Stuns`. No overlap with the run or the body.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` No copy of `Megaman.kt` | helps | A second caller can use the run and the body |
| `O1-R2` Components, not new inheritance | helps | Free functions and one component, no base class |
| `O1-R3` No new fragile order | no effect | Fixture order kept and tested |
| `O1-R4` Typed state | helps | Gravity table as a component |
| `O1-R5` Code only with a production caller | no effect | `Megaman` calls each new function |
| `O2-R1` Input only through `InputFrame` | no effect | Same input reads |
| `O2-R5` No logic assumes one player | no effect | Per-entity state |
| `O5-R1` No per-frame allocation | no effect | Params and hooks are built once in `init()` |
| `O7-R3` No level-identity checks | waiting | The `MOON_MAN` check moves into a Megaman parameter, not into `players/` |
| `O8-R2` Capabilities, not character checks | no effect | No new check |
| `O8-R3` Name the modes | helps | No mode changes |

## Decisions

Owner answers to the brief, 2026-11-10:

- D1: one `playerBody(PlayerBodyParams)` builder with hooks; it returns the fixtures, and Megaman adds his weapon
  fixtures after it.
- D2: X stays on his own body and run.
- D3: the gravity table is a `PlayerGravity` component in `players/`, signed by direction; the direction setter calls it.
- D4: the `MOON_MAN` check stays in Megaman's own max-speed parameter.
