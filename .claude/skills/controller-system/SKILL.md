---
name: controller-system
description: Use when reading or modifying the controller/input system — including the engine-level
  ControllerSystem, IControllerPoller, ControllerComponent, or the game-specific MegaControllerButton,
  MegaControllerPoller, and Megaman's input actuators. Covers the full stack from ECS processing
  to button-specific actuator callbacks.
---

# Controller System

## Architecture Overview

Input handling is split across two modules:

| Layer  | Concern                                          | Package                                                                 |
|--------|--------------------------------------------------|-------------------------------------------------------------------------|
| Engine | ECS system, component, poller interface          | `com.mega.game.engine.controller`                                       |
| Core   | Game buttons, concrete poller, Megaman actuators | `com.megaman.maverick.game.controllers` / `entities/megaman/components` |

---

## Engine Layer

### `ControllerSystem`

`engine/.../controller/ControllerSystem.kt`

- Extends `GameSystem(ControllerComponent::class)` — processes every entity that has a `ControllerComponent`.
- Each frame, iterates `component.actuators` (a map of `key → () -> IButtonActuator?`).
- Calls `poller.getStatus(key)` and dispatches to the appropriate actuator callback:

```
ButtonStatus.JUST_PRESSED  → actuator.onJustPressed(poller)
ButtonStatus.PRESSED       → actuator.onPressContinued(poller, delta)
ButtonStatus.JUST_RELEASED → actuator.onJustReleased(poller)
ButtonStatus.RELEASED      → actuator.onReleaseContinued(poller, delta)
```

- The actuator factory `() -> IButtonActuator?` returning `null` silently skips that button for a frame — useful for conditionally disabling inputs without removing the actuator.

### `ControllerComponent` (engine)

`engine/.../controller/ControllerComponent.kt`

- Maps `Any` keys → `() -> IButtonActuator?` actuator factories in an `ObjectMap`.
- Construction: pass `vararg GamePair<Any, () -> IButtonActuator?>` (use `key pairTo { actuator }` syntax).
- Mutation methods:
  - `putActuator(name, () -> IButtonActuator?)` — factory overload
  - `putActuator(name, IButtonActuator)` — convenience; wraps in `{ actuator }`
  - `removeActuator(name)` — removes by key

### `IControllerPoller`

`engine/.../controller/polling/IControllerPoller.kt`

- Extends `IActivatable`, `Runnable`, `Initializable`.
- Core method: `getStatus(key: Any): ButtonStatus?` — returns `JUST_PRESSED`, `PRESSED`, `JUST_RELEASED`, `RELEASED`, or `null` if key is unknown.
- All helper methods are default implementations:

| Method                            | Returns true when…                              |
|-----------------------------------|-------------------------------------------------|
| `isPressed(key)`                  | status is `PRESSED` or `JUST_PRESSED`           |
| `isJustPressed(key)`              | status is `JUST_PRESSED`                        |
| `isJustReleased(key)`             | status is `JUST_RELEASED`                       |
| `isReleased(key)`                 | status is `RELEASED` or `JUST_RELEASED`         |
| `areAll*(keys)` / `isAny*(keys)`  | bulk versions of the above                      |
| `allMatch(map)`                   | every key maps to the expected `ButtonStatus`   |

- `run()` must be called each frame (before `ControllerSystem.process`) to latch input state.
- `init()` is a no-op by default; override to seed initial state.

---

## Game Layer

### `MegaControllerButton`

`core/.../controllers/MegaControllerButton.kt`

Enum of all physical buttons. Each entry carries a `defaultKeyboardKey` (libGDX `Input.Keys` constant).

| Button | Default key      | Category |
|--------|------------------|----------|
| UP     | `W`              | D-pad    |
| DOWN   | `S`              | D-pad    |
| LEFT   | `A`              | D-pad    |
| RIGHT  | `D`              | D-pad    |
| A      | `K`              | Action   |
| B      | `J`              | Action   |
| START  | `ENTER`          | —        |
| SELECT | `L`              | —        |

- `isDpadButton()` — returns true for UP/DOWN/LEFT/RIGHT.
- `isActionButton()` — returns true for A/B.
- `DPAD_BUTTONS` / `ACTION_BUTTONS` companion sets for bulk checks.

These enum values are used as keys in both `ControllerComponent.actuators` and `IControllerPoller.getStatus()`.

### `MegaControllerPoller`

`core/.../controllers/MegaControllerPoller.kt`

- Extends `ControllerPoller(controllerButtons)` (engine base).
- Manages physical gamepad connect/disconnect on top of the base poller:
  - On connect: uses `ControllerUtils` to assign `controllerCode` for each `ControllerButton`.
  - On disconnect: nulls out all `controllerCode` values, falling back to keyboard.
- `run()` overrides to check `ControllerUtils.isControllerConnected()` before delegating to `super.run()`.

### Megaman's `ControllerComponent` (core)

`core/.../entities/megaman/components/ControllerComponent.kt`

Defined by `internal fun Megaman.defineControllerComponent()`. Registers four actuators:

| Button | Actuator behavior                                                                         |
|--------|-------------------------------------------------------------------------------------------|
| LEFT   | `onPressContinued` → `runLeft(poller, delta)`; release stops running unless RIGHT held    |
| RIGHT  | `onPressContinued` → `runRight(poller, delta)`; release stops running unless LEFT held    |
| B      | Hold: advances `chargingTimer`; release: calls `shoot()` then `stopCharging()`            |
| SELECT | `onJustReleased`: calls `setToNextWeapon()` or air-dash depending on `selectButtonAction` |

**A / UP / DOWN are not actuator-registered.** They are polled directly inside behavior `evaluate` lambdas in `BehaviorsComponent.kt`.

#### Running impulse detail

`runLeft` / `runRight` are private extensions in the same file.

---

## Adding a New Button Binding

1. Add the button to `MegaControllerButton` if it doesn't already exist.
2. Register a `ControllerButton` for it wherever `MegaControllerPoller` is constructed (wiring up keyboard and/or gamepad codes).
3. In the relevant entity's `defineControllerComponent()`, add a `putActuator(MegaControllerButton.X) { ButtonActuator(...) }` entry.
4. If the behavior is complex, prefer polling (`poller.isPressed(...)`) inside a behavior `evaluate` lambda over adding a new actuator callback.

---

## Common Gotchas

- **`null` from actuator factory** skips the button silently for that frame — don't confuse with the button being unregistered.
- **`isPressed` includes `JUST_PRESSED`** — if you need exactly one frame, use `isJustPressed`.
- **`run()` must precede `process()`** — if input appears stale, check frame-ordering of the poller's `run()` call.
- **Megaman's A button** is not in the actuator map — always look in `BehaviorsComponent.kt` for A-button logic (jump/dash/swim).
