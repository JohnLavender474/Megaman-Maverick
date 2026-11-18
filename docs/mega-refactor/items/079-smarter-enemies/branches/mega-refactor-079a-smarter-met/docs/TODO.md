# TODO: mega-refactor-079a-smarter-met

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- **Shared ledge-walk helper (from `079a`).** `Met` keeps its walk rules in the class (`runOrJumpAhead`,
  `tryJumpToPlayerPlatform`, `planJump`, the goal lambdas and the jump tables): today no other enemy uses the same
  rules (`O1-R5`; owner, 2026-11-18). When a second enemy gets "stop, turn or jump at a ledge" (likely `SpikeBot` or
  `FireMet` in a later `079` branch, or a `042` branch), move the `GroundAhead` dispatch into a helper in
  `entities/utils/` that returns a result (run, jump with a `JumpPlan`, blocked) and keeps the state change, the jump
  tables and the goals in each enemy. Do it after `015` if that has landed, so that the helper fits the new state
  machine. It changes gameplay for the second enemy: it needs its own scope and test room. `Starkner` cannot use it:
  its body is `ABSTRACT` and it flies.
- **Mega Man picks up and wears a `Met` helmet (owner, 2026-11-18).** A resting `MetHelmet` (after its `Met` died)
  can be picked up and used, and Mega Man can catch a falling one and "wear" it. Today the helmet rests on blocks and
  passes through Mega Man.
- **Foot sensors to `GroundProbe`.** 8 enemies copy a `CONSUMER` foot-sensor pair (`RedKoopa`, `RedKoopaShell`,
  `DarkSerket`, `RatRobot`, `DrillTankXT`, `FireMet`, `SpikeBot`, `Darspider`; also `BunbyTank`).
  `GroundProbe.checkAhead` can replace them, but the behavior is not the same: the probe counts only `STATIC` solids
  (moving platforms become pits), never counts `COLLIDE_UP_ONLY` as ground, uses one `lead` in place of each sensor
  offset, and sees `DEATH` where `SpikeBot` and `Darspider` do not. Needs a test room per enemy.

## Bug candidates

- **`MetHelmet` spin lags 1 frame.** `MetHelmet.update` calls `updateLook` before it changes `status`, so in the frame
  the helmet seats or comes to rest the sprite keeps its last spin angle; upright from the next frame.
- **Desperate `Met` at a pit shows `NoHelmetRun` for 1 frame** on each `DESPERATE_PAUSE` -> `DESPERATE_RUN` change:
  `enterState` clears `blockedAhead` before the next update computes it.

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
