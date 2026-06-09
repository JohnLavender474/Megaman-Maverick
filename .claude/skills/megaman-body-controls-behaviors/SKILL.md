---
name: megaman-body-controls-behaviors
description: Use when reading or modifying Megaman's physical body setup, controller input handling, 
  or behavior logic (jumping, dashing, climbing, wall sliding, etc.) — or when understanding how fixtures,
  sensors, and input actuators connect to behaviors.
---

# Megaman Body, Controls & Behaviors

## Overview

Megaman's physics, input, and movement are split into three extension-function files, all called from 
`Megaman.init()`. Each is an `internal fun Megaman.defineXxxComponent()` that returns a component.

## File Map

| Concern                      | File                                                 |
|------------------------------|------------------------------------------------------|
| Body / fixtures / gravity    | `entities/megaman/components/BodyComponent.kt`       |
| Controller / input actuators | `entities/megaman/components/ControllerComponent.kt` |
| Behaviors (state machines)   | `entities/megaman/components/BehaviorsComponent.kt`  |
| Behavior type enum           | `behaviors/BehaviorType.kt`                          |
| Engine behavior base         | `engine/.../behaviors/BehaviorsComponent.kt`         |
| Engine controller base       | `engine/.../controller/ControllerComponent.kt`       |

---

## Body Component

**`defineBodyComponent()`** in `BodyComponent.kt`

### Fixtures

Fixtures are grouped by role. Read `BodyComponent.kt` for current property keys and sizes.

| Category            | Fixture types present               | Notes                                                 |
|---------------------|-------------------------------------|-------------------------------------------------------|
| Colliders           | BODY, PLAYER                        | Sized to match the body each frame                    |
| Sensors (direction) | FEET, HEAD, SIDE (×2)               | Trigger bounce callbacks; feed `BodySense` flags      |
| Ground detection    | CONSUMER (feet gravity)             | Separate from the FEET fixture — see gotcha below     |
| Environment         | WATER_LISTENER, TELEPORTER_LISTENER | Passive overlap detectors                             |
| Damageable          | DAMAGEABLE                          | Not attached to body; sized and positioned separately |
| Weapon-specific     | DAMAGER, SHIELD (×2)                | Activated only when certain weapons are active        |

### `bodyOverGround` — important gotcha

`bodyOverGround` is **not** the same as `body.isSensing(BodySense.FEET_ON_GROUND)`. 
It is derived from a dedicated CONSUMER fixture that tracks overlapping block fixtures 
just below Megaman's feet. Always use the `Megaman.bodyOverGround` extension property 
rather than `FEET_ON_GROUND` when checking whether Megaman is over solid ground.

### Body size

The body shrinks vertically when crouching or ground sliding to allow passing through low gaps. 
The exact constants are in `BodyComponent.kt`; the concept is that `body.setSize()` is called 
every frame inside `preProcess`.

### `preProcess` lambda (runs every frame)

The core per-frame physics setup lives in a `preProcess` block inside `defineBodyComponent()`. It:

- Resizes the body and all fixtures that must match the body bounds
- Selects the correct gravity value based on the current movement state (grounded, airborne, water, ice, etc.)
- Applies `gravityScalar` and sets physics fields (`gravity`, `velocityClamp`, `defaultFrictionOnSelf`)
- All of the above are **direction-aware** — the active axis swaps when `Megaman.direction` is LEFT or RIGHT

This is the right place to look when investigating gravity bugs or adding new surface-type physics.

---

## Controller Component

**`defineControllerComponent()`** in `ControllerComponent.kt`

Returns a `ControllerComponent` mapping `MegaControllerButton` → `() -> IButtonActuator`.

### Button map

| Button       | Responsibility                                                          |
|--------------|-------------------------------------------------------------------------|
| LEFT / RIGHT | Horizontal movement (run); updates `facing` and `running`               |
| B            | Hold to charge weapon; release to fire via `shoot()`                    |
| SELECT       | Cycle weapon or trigger air-dash (depends on `game.selectButtonAction`) |

**A / UP / DOWN are not registered here.** They are polled directly inside behavior `evaluate` lambdas in `BehaviorsComponent.kt`.

### Running

Running is impulse-based with progressive acceleration — speed ramps up over time. 
The exact stages and constants live in `ControllerComponent.kt` and `MegamanValues`.
The velocity axis used depends on `Megaman.direction`, same as everywhere else.

`getMaxRunSpeed()` (extension in `BodyComponent.kt`) computes the cap, factoring in 
water and level-specific modifiers.

---

## Behaviors Component

**`defineBehaviorsComponent()`** in `BehaviorsComponent.kt`

### Registered behaviors

All `BehaviorType` values are defined in `behaviors/BehaviorType.kt`. 
Each is registered in `defineBehaviorsComponent()`.

For the precise trigger conditions of any behavior, read its `evaluate` 
lambda in `BehaviorsComponent.kt` — they are the authoritative source.

### Behavior lifecycle

Each `IBehavior` has four phases called by the engine's behavior system:

```
evaluate(delta) → true/false each frame
  true + was inactive → init()
  true + was active   → act(delta)
  false + was active  → end()
```

### `AButtonTask` state machine

`AButtonTask` tracks what the A button should do next. It is mutated by behaviors (`init`/`end`) 
and by the `UpdatablesComponent` update loop in `Megaman.kt`. When investigating jump/dash/swim 
behavior, trace where `aButtonTask` is being set. The transition logic lives inside two places: 
inside individual callbacks in `BehaviorsComponent.kt`, and in the `defineUpdatablesComponent`
block in `Megaman.kt`.

### Mutual exclusions in `evaluate`

Every behavior guards itself at the top of `evaluate`. Common shared guards are:
- Entity state: `dead`, `!ready`, `!canMove` (`canMove` is `false` while stunned, damaged, or frozen)
- In-progress states: `damaged`, `teleporting`
- Conflicting active behaviors (check `BehaviorsComponent.kt` for each behavior's specific exclusions)

### Momentum helpers (defined in `Megaman.kt`)

These helper functions manage the post-action momentum system and are called from both behaviors and the
update loop. Look them up in `Megaman.kt` for current signatures:

- `preserveMomentum()` — extends the momentum window (e.g., after a dash or on ice)
- `stopMomentum(stopMegaman)` — ends the momentum window; optionally zeroes lateral velocity
- `killPostActionMomentum()` — clamps lateral velocity back down to normal run speed
- `shouldPreserveMomentum(checkTimer)` — query whether momentum should currently be preserved

---

## Direction-Aware Pattern

`Megaman.direction` (`Direction.UP/DOWN/LEFT/RIGHT`) rotates the gravity axis. 
Nearly every physics and behavior calculation switches on it:

- `Direction.UP` / `DOWN` → gravity on Y, run on X
- `Direction.LEFT` / `RIGHT` → gravity on X, run on Y

When adding new physics or behavior code, always handle all four `Direction` cases.
