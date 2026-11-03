# Notes: mega-refactor-118b-batch-1

## Decisions

## Overlap with other branches

- 2026-11-03, at branch start (`2db9e6ffc`): `compactor-level` and `x6-intro-extra-level` change none of `fights/`, `MegaLevelScreen`, `AbstractBoss`, the 3 bosses or their tests. `compactor-level` has an uncommitted change only in `CeilingCrusher.kt`. No overlap.

## Agent report: the mid-level resume 1 frame earlier (2026-11-02)

Note: the owner chose to move the resume into a step in `118b`. `SCOPE.md` uses a stateless wait step after the resume, so the 1-frame shift stays and the screen guard can still see the fight.

### Report: the 3 mid-level resume actions 1 frame earlier

I only read and searched files. I did not change, create or commit any file, and I did not run the game. To scan the TMX files I ran one inline `python3 -I -c` command that printed to stdout only.

**What I did not read in full:** root `CLAUDE.md`, item `NOTES.md`, the `118b` `CONTRACT.md`/`PLAN.md`/`NOTES.md`, and the 8 boss files. For the bosses I read only their defeat, destroy and child hooks. I read `Gate.kt` from `:180` to `:300` only. I also did not trace `PlayerDeathEventHandler`.

#### 1. Order: confirmed, with 2 corrections

- **Frame E.** `engine.update` runs the boss updatable. The defeat timer ends, then `destroy()` runs and `BOSS_DEAD` is submitted (`AbstractBoss.kt:213-216`). `destroy()` sets `DESTROY_QUEUED`, which is live now but not live after the update (`GameEngine.kt:152-156`, `EntityExistenceState.kt:7`). The submit happens outside `run()`, so `BOSS_DEAD` reaches `run()` of E. The old branch returns there because `fights.owns(boss)` is still true (`MegaLevelScreen.kt:803`).
- **Tick of E+1.** `fights.update()` runs before `engine.update` (`MegaLevelScreen.kt:933`, `:948`). `AwaitBossDefeatEnd` reads `!liveAfterUpdate` and ends (`FightSteps.kt:124`). `LevelVictory` submits `getEventOnDefeated()`, which is `INTERMEDIATE_BOSS_DEAD` when `end=false` (`FightSteps.kt:127-136`, `AbstractBoss.kt:235-236`).
- **Correction 1: the fight closes in this same tick.** `LevelVictory.isFinished` is `leadPlayerAlive`, so the outro ends, `close()` runs (`Fight.kt:56`) and the director sets `fight = null` (`FightDirector.kt:23`). So in `run()` of E+1, `fights.owns(boss)` is already false.
- **`engine.update` of E+1.** The boss is removed at the start of the update, before any system runs. The kill loop drains every `destroy()` call made from `onDestroy`, so boss children destroyed there also go before the systems run (`GameEngine.kt:224-227`, `:240`).
- **`run()` of E+1** (`MegamanMaverickGame.kt:626`). The screen branch submits `TURN_CONTROLLER_ON`. Because `run()` is active, that event waits for `run()` of E+2 (`EventsManager.kt:29`). The branch then unlocks `DAMAGE/BOSS_DEFEATED` for each player and plays the level music (`MegaLevelScreen.kt:814-820`). `Gate` sets `triggerable` in the same `run()` (`Gate.kt:196-209`).
- **Correction 2: one more frame for input.** Input frames are built at the start of `render`, before the screen renders (`MegamanMaverickGame.kt:582-583`), and `TURN_CONTROLLER_ON` only clears `frameBuilders[*].locked` (`:699-705`). So today the first unlocked input frame is **E+3**. The proposal makes it **E+2**, not E+1.
- **Proposed step in the tick of E+1.** The submit happens outside `run()`, so `TURN_CONTROLLER_ON` reaches `run()` of E+1. The unlock and the music happen before `engine.update` of E+1. This matches your summary, apart from the input frame above.

#### 2. What can happen in that one extra frame

**Damage unlock (now before `engine.update` of E+1).** I found no damager that can reach a player in that update in the 8 live rooms:
- **Boss body:** `canDamage` needs `!defeated` (`AbstractBoss.kt:178`).
- **Projectiles and hazards:** `AbstractProjectile` and `IHazard` are `IBossListener` (`AbstractProjectile.kt:14`, `IHazard.kt:3`). They are destroyed at D+1 by `NotifyListenersOfBossDefeat` (`FightSteps.kt:100-110`). No override in these rooms keeps them alive; the only overrides are for `BigAssMaverickRobot` and `WanaanLauncher`.
- **TMX scan of the rooms** (inference that this list is complete):
  - `AcidGoopSupplier`, `PipePuff`, `FloatingCanHole` and `Spike` are `IHazard`.
  - `BulbBlaster` and `FloatingCan` are `IBossListener`.
  - `QuickSand`, `GravitySwitcharoo` and `DarknessV2` do no damage.
  - The sensors layer holds only `Gate`, so there is no pit or death sensor in these rooms.
- **`Spike`:** it is also destroyed on the `BOSS_DEFEATED` event (`Spike.kt:204`). No map sets `destroy_on_boss_defeated`; I searched all TMX files and found 0 hits.
- **`PreciousGem`:** destroyed on `BOSS_DEFEATED` (`PreciousGem.kt:216`). The shield gems go at `triggerDefeat` (`PreciousWoman.kt:291-296`).
- **Children that are not listeners:**
  - `RodentMan` destroys its `RatRobot` children at `triggerDefeat` (`RodentMan.kt:329-352`).
  - `TimberWoman` destroys `GroundPebble` (not a listener) and `DeadlyLeaf` in `onDestroy` (`TimberWoman.kt:399-409`). They are removed in the kill loop of E+1, before any system runs.
  - `ReactorMan` destroys its projectiles and beamers the same way (`ReactorMan.kt:268-276`).
  - `MoonMan` asteroids go in `onDefeated` (`MoonMan.kt:264-270`).
  - `InfernoMan` spawns no meteors after defeat, because its update returns early (`InfernoMan.kt:277-282`).
- **Explosions:** defeat explosions spawn with `DAMAGER false` (`AbstractBoss.kt:293-303`), and none of the 8 bosses overrides this. `ExplosionOrb` has no damager type and spawns no earlier than E+2.
- **2 players:** a player shot in flight hurts a player only under `PVP` (`SessionRules.kt:18-20`). Projectiles are destroyed at D+1. One possible exception is a charged shot that fires when the lock turns a held button into `JUST_RELEASED` (inference). That case already exists today and is the same in both flows.
- **A player still invincible for other reasons** stays invincible: `invincible` reads `damaged`, `damageRecovery`, any `DAMAGE` reason and `inPlay` (`Megaman.kt:89`), and the unlock clears only the `BOSS_DEFEATED` bit (`LocksComponent.kt:22-26`).
- **No sprite reads the lock** (`SpritesComponent.kt:104-109`).
- **Death fixtures:** a non-instant one respects `invincible` (`DeathRule.kt:28`, `PlayerDeath.kt:47`). If one existed, a player standing in it would die 1 frame earlier. None is in these rooms.

**Music (about 16.7 ms earlier at 60 frames per second).** `unsetMusic` runs at D+1 and acts at once (`MegaAudioManager.kt:148-152`). The room `fade_out_music` fade starts at the room transition, before the fight (`LevelRoomHandler.kt:57-58`), and `playMusic` clears any fade timer (`MegaAudioManager.kt:143`). So no fade or queue is in progress at E+1.
- One side effect, which is a small improvement: if slot 0 has an instant death in `engine.update` of E+1, both `PLAYER_JUST_DIED` and `INTERMEDIATE_BOSS_DEAD` reach `run()` of E+1 today.
- The screen mask is an `objectSetOf` (`MegaLevelScreen.kt:138`), so the order between them is not fixed (inference). The level music can then start during the death.
- With the step, the music starts first and `unsetMusic` in `PLAYER_JUST_DIED` (`:593`) always wins. No live room has such a hazard.

**Controller (first input at E+2, not E+3).**
- **Gate:** it becomes `triggerable` in `run()` of E+1, before `engine.update` of E+2, which is the first update that sees input. So there is no frame in which the player can touch a gate that is not triggerable. `GateRule` reacts only on `onBegin` (`GateRule.kt:11-17`), so such a gap would matter, but none exists.
- **Pause:** `isPauseRequested` needs `!locked` (`LevelPauseRules.kt:17`), so pause becomes possible 1 frame earlier. `queuePause` checks the lock again (`:23`).
- **Held buttons:** a held button gives `JUST_PRESSED` when the lock ends (`InputFrameBuilderTest`), the same as today.
- **`MoonMan`:** gravity is set only at `MoonMan.kt:526`, and only while it is not defeated. Nothing in the 3 actions reads or writes gravity, so there is no interaction.
- **2 players:** the unlock goes to every player and `TURN_CONTROLLER_ON` unlocks every frame builder, so both slots shift the same.

#### 3. Visible or audible?

No. The only difference is the 1-frame shift: about 16.7 ms earlier for the music and for control. I found no bug and no soft lock.

#### 4. Is it the same for all 8 bosses?

Yes. The bosses with children (`PreciousWoman`, `RodentMan`, `TimberWoman`, `ReactorMan`, `MoonMan`) remove them at `triggerDefeat`, `onDefeated`, D+1, or in the kill loop of E+1 before the systems run.

#### 5. Option: a step that waits 1 tick

- **It can be stateless.** A step can read `!boss.existenceState.liveNow`. In the tick of E+1 the boss is `DESTROY_QUEUED` (`liveNow` true). After `engine.update` of E+1 it is `UNSPAWNED` (`GameEngine.kt:193-197`), so the step ends in the tick of E+2. No field and no counter is needed.
- Another way: `FightPlans.find` builds a new plan for each call (`FightPlans.kt:10-13`), so a class instance with state would belong to one fight. But today all steps are `object`s.
- **Result:**
  - The unlock lands before `engine.update` of E+2, which matches today for every system.
  - `TURN_CONTROLLER_ON` is delivered in `run()` of E+2, the same as today.
  - The music starts 1 frame **later** than today (tick of E+2, not `run()` of E+1).
  - The fight stays open through `run()` of E+1, so a `fights.owns(boss)` guard on the screen branch works.

#### Facts that matter more than the frame shift

- **The guard.** Because the fight closes in the tick of E+1, a `fights.owns` guard on the screen branch does **not** stop that branch in the no-wait design. Without another guard, the branch does the work a second time:
  - a second `TURN_CONTROLLER_ON` (harmless);
  - a second unlock (harmless);
  - a second `playMusic`, which stops and restarts the same track (`MegaAudioManager.kt:126-139`).
- **The level music.** The level music is `MegaLevelScreen.music` (`:189`). `MegaServices` does not expose it, so a step needs a new path to it.
- **Mid-level only.** The stage plan also runs `BigAssMaverickRobot`, an end-of-level fight. So the step needs a plain `if` so that it acts only for `INTERMEDIATE_BOSS_DEAD`.
- **Not part of the step.** The `!boss.mini` music rule (`:819`) matters only for `118e`.

#### Direct answer

The order is correct, with the 2 corrections in section 1. Moving the 3 actions into a step with the 1-frame shift has no adverse effect that I could find in any of the 8 mid-level fights, in 1 or 2 players. The only differences are a 16.7 ms earlier music start and an earlier first input frame (E+2, not E+3). The real costs are the guard and the music access, not the timing.

#### Recommendation

Keep the screen branch until `118g`. In `118g`, move the 3 actions into a step with the 1-frame shift and no wait, and name the shift as an intended change in its `CONTRACT.md`. When the old branch is gone, the guard problem is gone too. `118b` then keeps "no new step and no new screen path". If you want the move in `118b`, use the wait variant (`!boss.existenceState.liveNow`): it keeps the damage and controller frames exact and makes the `owns` guard work.

#### Risks

- In the no-wait design, a branch guarded only by `fights.owns` still acts, which restarts the music 1 frame after it starts.
- Inference: my TMX overlap scan skipped the `blocks` layer and only checked map objects. A hazard spawned some other way is not covered.
- I did not trace `PlayerDeathEventHandler` for the slot 0 death edge case.

#### Open questions for the owner

1. Do you accept a 1-frame earlier resume as an intended change? Recommended: yes, in `118g`.
2. Should the step get the level music through a new field on `Fight`/`FightPlan` or through `MegaServices`? Recommended: decide in `118g`, with its first caller.
3. If you move it in `118b`: guard the branch with the wait variant or with an event prop? Recommended: the wait variant.
