# Plan: mega-refactor-014d-contact-receivers

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`.

Each of steps 4-9 moves one piece of player code and is its own commit: the rule calls the new
receiver at the old place, and Megaman registers the old code. After each step the build, the tests
and a short launch pass. The selection test must keep passing (the table does not change).

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `014d` row in `ITEMS.md` to `active` with
  the start date, mark it active under "Branches" in `ITEM.md`, set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-014d-contact-receivers
  .worktrees/mega-refactor-014d-contact-receivers master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 454 engine, 79 core) and the warning
  count (expected 114) in the process log.
- Done: yes

### 3. Declarations

- Changes:
  - `Receivers`: `FEET_BLOCK_CONTACT: (state: ProcessState, block: Block) -> Unit`,
    `WATER_CONTACT: (state: ProcessState, wasInWater: Boolean) -> Unit`,
    `ICE_FRICTION_FILTER: () -> Boolean`, `SIDE_ICE_FRICTION: () -> Boolean`.
  - New `core/.../world/body/BodyKeys.kt`: `PASS_THROUGH_ONE_WAY: PropertyKey<() -> Boolean>` and
    `FALL_THROUGH_PRESS_UP: PropertyKey<() -> Boolean>`.
  - New marker interfaces `IGateTrigger` and `ICartRider` in `core/.../entities/contracts/`.
- Check: `./gradlew core:compileKotlin` passes.
- Done: yes

### 4. Feet-block contact

- Changes:
  - `FeetBlockRule`: in each phase, replace the `is Megaman` block with
    `feetFixture.receiver(Receivers.FEET_BLOCK_CONTACT)?.invoke(state, block)` at the same place.
  - Megaman (`megaman/components/BodyComponent.kt`): register `FEET_BLOCK_CONTACT` on the feet
    fixture with the three old blocks, unchanged, chosen by `state`. In end, the old block reads the
    feet body's `hasAnyFeetBlock()`; the receiver reads the same body.
  - Test (`FeetBlockRuleTest`): with a recording receiver and a relaxed block and bodies, the receiver
    runs with the right state, after the stick translate and before `FEET_ON_GROUND` is set and
    `block.hitByFeet` runs (begin, continue), and after `FEET_ON_GROUND` is updated and before
    `block.hitByFeet` (end).
- Check: build, tests, a 25-second `TEST_1` launch.
- Done: yes

### 5. Water contact

- Changes:
  - `WaterRule`: replace the three `is Megaman` blocks with
    `listenerFixture.receiver(Receivers.WATER_CONTACT)?.invoke(state, wasInWater)` at the same places
    (begin: at the end, with `wasInWater`; continue: at the end, `false` is passed and unused; end:
    inside "no longer in water", after the splash).
  - Megaman: register `WATER_CONTACT` on the water-listener fixture with the three old blocks. The
    begin block zeroes the velocity of the listener's body, which is Megaman's body.
  - Test (`WaterRuleTest`): the receiver gets the right state and `wasInWater`, runs after the
    splash decision, and on end only when no water is left.
- Check: build, tests, launch.
- Done: yes

### 6. Ice friction

- Changes:
  - `FeetIceRule` continue: replace the `is Megaman` early return with
    `if (feetFixture.receiver(Receivers.ICE_FRICTION_FILTER)?.invoke() == false) return` at the same
    place. Megaman registers the old condition, negated, on its feet fixture.
  - `SideIceRule`: replace `(entity is Megaman && entity.isBehaviorActive(WALL_SLIDING))` with
    `sideFixture.receiver(Receivers.SIDE_ICE_FRICTION)?.invoke() == true`. Megaman registers
    `{ isBehaviorActive(BehaviorType.WALL_SLIDING) }` on its left and right side fixtures.
  - Tests: `FeetIceRule` continue skips the friction when the filter returns `false` and applies it
    when there is no filter; `SideIceRule` applies the friction when the predicate returns `true` and
    the body property is `false`, and not when both are false.
- Check: build, tests, launch.
- Done: yes

### 7. Gate and cart capabilities

- Changes: `GateRule` checks `entity is IGateTrigger`; `FeetCartRule` checks `feetEntity is
  ICartRider`. `Megaman` implements both markers. Test: `GateRule` triggers a triggerable gate for an
  `IGateTrigger` entity and not for another entity.
- Check: build, tests, launch.
- Done: yes

### 8. Sand splash supplier

- Changes: `FeetSandRule(splashesOn: () -> Boolean)` instead of `(game)`;
  `megaContactRules` passes `{ game.megaman.ready }`.
- Check: build, tests, launch.
- Done: yes

### 9. `TODO` comments for `029`

- Changes: a `TODO` comment at the `Megaman` reference in `PlayerItemRule` and in `BodyPlayerRule`,
  which says that the player type in the signature waits for `029` (`IPlayer`).
- Check: build.
- Done: yes

### 10. Collision handler body keys

- Changes:
  - `MegaCollisionHandler` takes no game. `dynamicBody == megaman.body &&
    megaman.isBehaviorActive(CLIMBING)` becomes
    `dynamicBody.getProperty(BodyKeys.PASS_THROUGH_ONE_WAY)?.invoke() == true` (two places), and the
    press-up check becomes `dynamicBody.getProperty(BodyKeys.FALL_THROUGH_PRESS_UP)?.invoke() == true`.
  - Megaman registers the two keys on its body: `{ isBehaviorActive(CLIMBING) }` and
    `{ isBehaviorActive(CLIMBING) && game.controllerPoller.isJustPressed(MegaControllerButton.UP) }`.
  - `MegamanMaverickGame.createGameEngine()`: `MegaCollisionHandler()`.
  - Test (`MegaCollisionHandlerTest`): a dynamic body whose `PASS_THROUGH_ONE_WAY` returns `true`
    passes a `COLLIDE_DOWN_ONLY` static body; without the key it is handled as before; the press-up
    key lets a body through a `PRESS_UP_FALL_THRU` body.
- Check: build, tests, launches of `TEST_1` and `INTRO_STAGE`.
- Done: yes

### 11. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the checks and reviews in `CONTRACT.md`; the owner's play test. Write the
  results in the process log.
- Done: yes

### 12. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval. Item `014` is done after this merge.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: yes
