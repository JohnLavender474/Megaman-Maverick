# Notes: 189 villain and dog boss fight

Research for this item. Based on `master` `328de51` (2026-11-26).

## The characters (2026-11-26)

The villain and the dog are new original characters of this game (owner, 2026-11-26). Claude Fable 5.2 designs and
draws them and their combined form in `189a`; no existing sprite sheet is used. Their names are open (`ITEM.md`).

## How a two-body boss fits the fight flow

- `FightDirector` holds one fight, and a fight has one boss (`core/.../fights/README.md`). `FightPlans.find` maps a
  boss `TAG` to a plan; any boss not in the list throws `MissingFightPlanException` when the player enters the room.
- So the fight has one boss entity: the villain. The dog is a child entity that the villain spawns and owns, as
  `ElecDevil` spawns its body pieces and `BigAssMaverickRobot` its hands. The win rule reads the villain only.
- Phase 2 is a state of the villain (the dog despawns at the merge), not a second boss. One health bar and one fight
  plan hold across both phases. `WilyFinalBoss` already runs phases inside one boss (`phase_1/...` animation keys).
- Inference: a shared plan object on the villain that issues orders to the dog keeps the attacks coordinated and
  keeps one owner of the timing (`O1-R3`, no new fragile order).

## Where the fight can go (`189c` scans this)

- `LevelType` has `ROBOT_MASTER_LEVEL`, `INTRO_LEVEL`, `WILY_LEVEL`, `TEST_LEVEL`, `EXTRA_LEVEL`, `MARIO_LEVEL`.
  `levelState.isInExtraLevel` exists, so an extra level is a supported path.
- Wily stages 1 to 3 exist (`WILY_STAGE_1..3`); a mid-stage rival fight there is the other natural place.
