# Process log: mega-refactor-014d-contact-receivers

## Status

- State: merged
- Goal: Replace the `Megaman` checks in the contact rules and the collision handler with receivers, capabilities and body flags
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `46cbaac38`. Draft `docs/SCOPE.md` written.
- Scope approved by the owner (side-ice predicate, two player rules wait for `029` with `TODO`s, no `SurfaceRule`). Draft `docs/CONTRACT.md` written.
- Contract approved by the owner. Draft `docs/PLAN.md` written.
- Plan approved by the owner. Step 1: branch start on `master`, worktree created.
- Step 2: baseline: 454 engine and 79 core tests, 114 compiler warnings.
- Step 3: four receivers in `Receivers`, `BodyKeys` with two keys, markers `IGateTrigger` and `ICartRider`.
- Step 4: `FeetBlockRule` calls `FEET_BLOCK_CONTACT` where the three `is Megaman` blocks were; Megaman registers
  the three blocks unchanged (`entity.` dropped; `entity.body` is the same object as Megaman's local `body`).
  `FeetBlockRuleTest` (3 tests) pins the call point against the stick check, the ground sense and the block hit.
- Step 5: `WaterRule` calls `WATER_CONTACT`; Megaman registers the three blocks. Continue passes `false` for
  `wasInWater` (unused there). `WaterRuleTest` (3 tests) checks the state, `wasInWater`, the order after the
  splash decision, and that end calls it only when no water is left.
- Step 6: `ICE_FRICTION_FILTER` (feet) and `SIDE_ICE_FRICTION` (left and right side fixtures); Megaman registers
  the old condition (negated for the filter) and `{ isBehaviorActive(WALL_SLIDING) }`. `IceRulesTest` (4 tests).
- Step 7: `GateRule` checks `IGateTrigger`, `FeetCartRule` checks `ICartRider` (it extends `IBodyEntity`, because
  the rule reads the rider's body). `Megaman` implements both. `GateRuleTest` (2 tests).
- Steps 8-9 (one commit): `FeetSandRule` takes a `() -> Boolean`; `megaContactRules` passes `{ game.megaman.ready }`.
  `TODO` comments for `029` in `PlayerItemRule` and `BodyPlayerRule`.
- Step 10: `MegaCollisionHandler` takes no game and reads `BodyKeys.PASS_THROUGH_ONE_WAY` and
  `BodyKeys.FALL_THROUGH_PRESS_UP`; Megaman registers `{ climbing }` and `{ climbing && UP just pressed }` on its
  body (body properties survive `Body.reset`). `MegaCollisionHandlerTest` (4 tests). Launches of `TEST_1` and
  `INTRO_STAGE` show no exception.
- Step 11 (automated): clean build passes; 454 engine and 95 core tests, 0 failures; 114 compiler warnings, the
  same list as the baseline; only the two `029` sites mention `Megaman` in the rules (with `TODO`s), none in the
  collision handler; only Megaman registers the new receivers and keys and implements the markers; the
  reference file is unchanged. Every step's launch showed no exception.
- Step 11: the owner's play test passed. All contract checks pass.
- Step 12: the owner approved the merge (2026-10-22).
