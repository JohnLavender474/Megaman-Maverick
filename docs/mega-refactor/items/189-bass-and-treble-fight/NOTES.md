# Notes: 189 Bass and Treble boss fight

Research for this item. Based on `master` `328de51` (2026-11-26).

## The reference sheet (2026-11-26)

The owner's upload: "Mega Man 10: Bass, Treble and Reggae", The Spriters Resource, ripper Random Talking Bush,
copyright Capcom. 525x922, indexed color. A copy is in `temp/bass-treble-sheet/mm10_bass_treble.png` (not
committed). The sheet is a reference for the Fable art agent, not the source of the game frames: Claude Fable 5.2
draws new Bass and Treble sprites for the fight (owner, 2026-11-26).

The sheet holds more than the fight needs: Reggae (Bass's bird from MM10's shop), the weapon-get portraits, the
stage-select faces and the weapon palettes. The weapon palettes may guide a palette swap when Bass uses a copied
weapon, if a move asks for it.

The poses that the sheet shows per body are in `ITEM.md`. In short: Bass has a full set; Treble alone has only
teleport, idle, toss and an arrow effect; Treble Boost has intro, idle, shoot and revert. Fable draws the poses that
the chosen moves need and the sheet does not show.

## How a two-body boss fits the fight flow

- `FightDirector` holds one fight, and a fight has one boss (`core/.../fights/README.md`). `FightPlans.find` maps a
  boss `TAG` to a plan; any boss not in the list throws `MissingFightPlanException` when the player enters the room.
- So the fight has one boss entity: `Bass`. `Treble` is a child entity that `Bass` spawns and owns, as
  `ElecDevil` spawns its body pieces and `BigAssMaverickRobot` its hands. The win rule reads Bass only.
- Phase 2 is a state of `Bass` (Treble despawns at the merge), not a second boss. One health bar and one fight plan
  hold across both phases. `WilyFinalBoss` already runs phases inside one boss (`phase_1/...` animation keys).
- Inference: a shared plan object on `Bass` that issues orders to `Treble` keeps the attacks coordinated and keeps one
  owner of the timing (`O1-R3`, no new fragile order).

## Where the fight can go (`189c` scans this)

- `LevelType` has `ROBOT_MASTER_LEVEL`, `INTRO_LEVEL`, `WILY_LEVEL`, `TEST_LEVEL`, `EXTRA_LEVEL`, `MARIO_LEVEL`.
  `levelState.isInExtraLevel` exists, so an extra level is a supported path.
- Wily stages 1 to 3 exist (`WILY_STAGE_1..3`); a mid-stage rival fight there is the other natural place.
