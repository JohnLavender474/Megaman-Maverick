# Contract: mega-refactor-077c-elec-devil

- Status: approved
- Approved: 2026-11-18 (with `SCOPE.md`)

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new `ElecDevil` fight tests.
- [ ] `./gradlew core:smokeTest` passes; `entity-tags.txt` lists each new entity.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-077c-elec-devil`. Experimental
checks first.

| Level | Room | What to check |
|---|---|---|
| `REFACTOR_TEST_077` | (`ElecDevil` room) | Phase 1: eye patterns Fan, Ring, Stall (colour per pattern), Live Wire gaps, Tether Claw (guide line, safe place under the high end) |
| `REFACTOR_TEST_077` | (`ElecDevil` room) | Phase 2: tell at the phase start, generator power-down, Bounce pattern; all readable in the dark |
| `REFACTOR_TEST_077` | (`ElecDevil` room) | Hits to kill equal to `master` per weapon; death animation |
| `WILY_STAGE_1` | `room10` | The real fight from the gate: intro, both phases, defeat, stage exit |

Each boss check adds `--heartTanks all --healthTanks all`.

## Behaviors to check

- Hard mode: quicker timings, same attack set.
- Death and respawn in the boss room: the fight restarts in phase 1.

## Intended changes

- 2 health phases; the new attacks above; the new art.
- `HAND` is the Tether Claw; the eye no longer shoots in `HAND`. Eye Spread became the eye pattern deck.
- Spark Serpent, Circuit Floor and Storm Rain (`PLAN.md` steps 5 and 6) moved to a later branch of item 077
  (owner, 2026-11-19); the phase-2 pick is a `TODO` at `ElecDevil.pickMoveAttack`.
- Damage to the player moves to `PlayerDamageNegotiator` with the `master` values.
