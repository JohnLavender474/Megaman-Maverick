# Process log: mega-refactor-029b-enemy-players

## Status

- State: merged
- Goal: Enemies reach the player through the players service (target, any, contact identity) instead of the global `megaman`
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `6fcb858fb`. Draft `docs/SCOPE.md` written.
- Open question 2 answered by the owner: `ready` and `damaged` go in a core `PlayerStateComponent`. Scope updated.
- The owner answered the open questions (targetFor only with TODOs; PlayerStateComponent; stomp write kept with TODOs; AbstractEnemy to 029c and 029d; name `targetPlayer`). Scope updated.
- Scope approved by the owner. Draft `docs/CONTRACT.md` written.
- Contract approved by the owner. Draft `docs/PLAN.md` written.
- Plan approved by the owner.
- Step 1: branch start on `master`, worktree created.
- Step 2: baseline: clean build passes; 472 engine and 102 core tests, 0 failures; 114 compiler warnings.
- Step 3: a subagent added rooms `enemy_aim` (`Met`, `SniperJoe`), `enemy_drop` (`Darspider`, `SuicideBummer`),
  `enemy_chase` (`Bat`), `enemy_magfly` (`MagFly` over a `Ladder`), `enemy_contact`, `enemy_owner` (`Cokeyro`) to
  `RefactorTest029.tmx` (map widened to 236 columns, tiles painted, ladder tile 131; spawns `5` to `10`).
  `GapingFish` crashed on spawn (`GapingFish.kt:224`: the atlas `Enemies1.txt` has no `GapingFish/frozen` region),
  on the baseline too; no live level places one. It was replaced by a `Coldier` and the water area removed; bug
  candidate in `docs/TODO.md`. Spawn `10` sorts before `2` as a string (`PlayerSpawnsManager.startAt`); no effect
  here, because the rooms are closed and a spawn activates only in the camera. Launches of every new room: no
  exception (two early clean exits of `enemy_owner` did not repeat; a 70 s launch ran to the end). The owner's
  baseline check of the new rooms is still to do.
- Step 4: engine `IPlayers.viewOf(entity)`; `slotOf` uses it. `PlayersTest` +1 (473 engine tests).
- Step 5: core `PlayerStateComponent(damageTimer)` (`ready`, `damaged`, no-op reset); `Megaman.playerState` is made
  with the damage timer and added first in `init()`; `Megaman.ready` reads and writes it. `MegaPlayer` requires
  `BodyComponent`, `BehaviorsComponent` (deviation: the plan said the `IBehaviorsEntity` interface; the component
  is what `isBehaviorActive` reads, and it fits the components direction), `PlayerStateComponent`, and checks
  `IDirectional` and `MegaGameEntity` (`IllegalArgumentException` if missing). Accessors: `direction`, `dead`,
  `ready`, `damaged`, `isBehaviorActive`, `feetFixture`, `leftSideFixture`, `rightSideFixture`,
  `damageableFixture`. `MegaPlayers.targetFor(seeker: IMegaGameEntity)` gives slot 0, with the `TODO`;
  `IMegaGameEntity.targetPlayer`. Tests: `MegaPlayersTest` (9), `MegamanStunTargetTest` +1 (`ready`). 473 engine,
  108 core.
- The owner checked the new rooms on the baseline gameplay (after step 5): all pass. Contract row `enemy_contact` now checks `Matasaburo` and `Coldier` (no `GapingFish`); a plan-level change after the room swap, no scope change.
- Step 6: identity (`DeathBat`, `Bat`, `BigFishNeo`, `GapingFish`, `StagedMoonLandingFlag` x2, `TellySaucer`,
  `IcicleTelly`, `Coldier`, `Matasaburo`, `MagFly` x2, the Koopa shells' feet receivers) uses `players.viewOf`;
  later reads use the view's `body`, `damaged` and `isBehaviorActive`. Ownership (`Cokeyro`,
  `StagedMoonLandingFlag` x2, `UFOhNoBot` x2) uses `players.viewOf(owner) != null`. The Koopa shells'
  `shouldStartRolling` (side, feet and body overlap) uses `players.any` with the view's fixture accessors;
  `JetpackBlaster` reads `targetPlayer.damageableFixture`. The 6 stomp bounces write through the view's body with
  the `029g` `TODO`. Engine `viewOf` now takes `Any?` (it compares identity; the damage callbacks pass an
  `IDamageable`). Build and tests pass (473, 108). Launches of `enemy_contact` and `enemy_owner`: no exception (the
  owner closed the windows; launches stay short from now on).
- Step 7: rule: a site that only tests a sensor or area against the player's body becomes
  `players.any { player -> ... }`; a site that reads the same player several times in one decision stays with
  one player (the target, step 8). To `any` (22 sites): `DeathBat:154`, `Cactus:362`, `MoonEyeStone:224`,
  `CannoHoney:231`, `SpringHead:184`, `LampeonBandito:269`, `SteamRollerMan:388`, `Cokeyro:297`, `WalrusBot:138`,
  `LumberJoe:428`, `UnderwaterPenguinBot:124`, `Popoheli:196`, `Bat:250`, `ColtonJoe:144`, `UFOhNoBot:204`,
  `DrillTankXT:179`, `Tropish:141`, `DarkSerket:574`, `FireDispensenator:237,245`, `MockingByte:274`; and two
  compound checks with every condition on the same player: `IceSkaterPeng.shouldBrake` (sensor and in front) and
  `RatRobot`'s trigger (not climbing and in a trigger). Kept for the target (step 8): `MagFly:194` (the slow check
  feeds the facing toward the same player), `DragonFly:224` (with `isMegamanBelow`), `BunbyTank:225-232` (dead
  check, shoot and turn scanners), `SubmarineJoe:139,145,286` (chase and aim), `PreciousCube` (orbit), and the
  distance orderings in `DemonMet`, `JetMet`, `PreciousTron`, `MockingByte:545,590`, `JetpackBlaster:223`. Build and
  tests pass (473, 108). Short launches of `enemy_drop` and `enemy_chase`: no exception.
- Step 8: a script replaced every remaining `megaman.` and `game.megaman.` read in `entities/enemies/` with
  `targetPlayer.` (98 files) and fixed the imports. It also touched `FacingUtils`, an `object` with no entity
  receiver; that file was fixed by hand to read `val target = entity.targetPlayer`. A check of the diff: all 323
  changed lines are the old line with only the reference swapped; no local variable named `megaman` was affected.
  What is left in the enemies: two log texts and one comment that say "megaman". Build and tests pass (473, 108).
  Short launches of `enemy_aim`, `enemy_magfly`, `death`: the level loads, no exception.
- Step 9 (automated): clean build passes; 473 engine and 108 core tests, 0 failures; 114 compiler warnings, the
  same list as the baseline; `ContactRuleSelectionTest` passes and the reference file is unchanged; the global
  player grep (with `package` and `import` lines left out, because `com.megaman` matches the word) finds only two
  `Cactus` log texts and one `MockingByte` log text; `Megaman::class` only in `SwinginJoe` and `PreciousTron`;
  `MegamanValues` only at the 6 stomp sites, each with the `029g` `TODO`; `targetFor` has its `TODO`; no
  `TargetPolicy` or `TargetingComponent` type; `PlayerStateComponent.reset()` does nothing and `Megaman.ready` is
  tested; the step 6-8 reviews are in the entries above. Waiting for the owner's play test.
- Step 9: the owner's play test passed. All contract checks pass.
- Step 10: the owner approved the merge (2026-10-23).
