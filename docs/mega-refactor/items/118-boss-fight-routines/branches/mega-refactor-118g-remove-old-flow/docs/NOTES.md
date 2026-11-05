# Notes: mega-refactor-118g-remove-old-flow

## Decisions

- (2026-11-05, owner) Remove the parts that lose their only caller in this branch: `getEventOnDefeated` (with the `WilyFinalBoss` override), the private `AbstractBoss.end`, `getHealthFillType` and `ConstKeys.HEALTH_FILL_TYPE`. The item said that `118g` keeps the 14 hooks; the hooks that still have callers stay for `118i`.
- (2026-11-05, my call, root `CLAUDE.md` "Fail fast") `FightPlans.find` throws `MissingFightPlanException` for a boss with no plan, because no old flow is left to take it.

## Overlap with other branches

- At the scan (`373923c8e`): `compactor-level` changes none of `fights/`, the bosses, `MegaLevelScreen` or `EventType`. `x6-intro-extra-level` (last commit `f4221aef5`) adds `D1000`, `D1000Core`, `D1000Frames`, `D1000Pillar`, `D1000Bomb` and `D1000Test`, and the `D1000` lines in `FightPlans.kt`. None of these files overrides `getEventOnDefeated` or `getHealthFillType`, sends a removed event, or reads `HEALTH_FILL_TYPE` or `owns`.
- `FightPlans.find` changes its return type to `FightPlan`. The `D1000` entry in the `when` still fits. A test on that branch that expects `find` to return `null` would need a change; `D1000Test` does not call `find`.
