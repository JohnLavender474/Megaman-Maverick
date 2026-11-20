# Test debt: mega-refactor-077g-glacier-man

The fight's spec while the moveset changes, one rule per line in plain words. Each line becomes a test when the owner
says the fight is final (`docs/ways-of-working/gameplay.md`, "Gameplay iteration"). The final pass uses a
`GlacierManRig` on the `077b` rig pattern (a scripted `GameRandom` picks the move).

## Rules to make into tests

### Phases and damage (step 2)

- [x] Glacier Man has 2 health phases; the bar rolls over at 0 in phase 1; defeat only at 0 in phase 2. The second
  bar region is `BossBitsBar.PHASE_2_BITS`.
- [x] Damage on Glacier Man: each value is 2 × the `master` value (pellet 2, half charge 2, full charge 4, charge
  explosion 2 / 4, sabre 2 / 4, `ProtoBuster` 2, `Fireball` 8, `MagmaWave` 8, `MagmaFlame` 8, `ReactorManProjectile`
  6, `MoonScythe` 2, `SmallIceCube` 2, `Axe` 2, `Needle` 2, `SlashWave` 2, `PreciousGem` 2, `IceBombExplosion` 2,
  `ReactorExplosion` 2; a damager not in the table gives the `BOSS_DMG_NEG` value), so each weapon takes the same
  number of hits as on `master` over the 2 phases.
- [x] A fire hit (`IFireEntity`) that does damage starts the `FIRED` stun (1 s) and plays `SOLAR_BLAZE_SOUND`.
- [x] Damage to the player: `GlacierMan` 3; `Snowhead`, `SmallIceCube` and `FallingIcicle` with owner Glacier Man 3.
- [x] Damage to the player: `Snowhead`, `SmallIceCube` and `FallingIcicle` of another owner (or no owner) give the
  same value as the fallback on `master` (size `MEDIUM`: 3).
- [x] A `SmallIceCube` of Glacier Man still freezes the player.
- [x] The tuning row follows the difficulty and the phase (`GlacierManTuning.of`).

### Move choice, chains, tells, timing (step 3)

- [x] From `STAND`, the next move is a weighted pick of ice blast, duck, sled and jump; the weights come from the row.
- [x] Weights: no duck when the player is above (top of the player ≥ 1.5 tiles over his feet); no sled when the
  player is within 2.5 tiles in x; jump × `nearJumpScale` when near and × `aboveJumpScale` when the player is
  above; ice blast × `farIceBlastScale` when far; no ice blast in the air or when a throw spawn point is in a wall.
- [x] The last move gets its weight × `lastMoveScale`; no move runs 3 times in a row (ice blast, duck, sled, jump;
  chained moves count).
- [x] When every weight is 0, the move is jump, or duck when the last 2 moves were jumps. (No current row reaches
  the jump branch: the jump weight is 0 only after 2 jumps; only the duck branch is tested.)
- [x] Chain: when the sled stops at a wall and the row's `sledJumpChainChance` roll passes, he jumps over the player
  (the launch speed aims 2 tiles past the player in x, clamped to the arena; the body's y drag makes him land
  about 1 tile past, a bug candidate in `TODO.md`), then on landing faces the player and shoots one aimed
  snowball, then brakes.
- [x] After the sled, when the player is above, he jumps at once (no tell), as on `master`.
- [x] Chain: after the duck, when the row's `duckSledChainChance` roll passes or the player is far, he sleds (with
  the sled tell); else jump (with tell) when the player is above; else stand.
- [x] Tells: sled → `brake_flash` for `sledTellDur`; jump → `fall_flash` for `jumpTellDur`; ice blast →
  `throw_flash` for `iceBlastTellDur`. He faces the player and does not move during a tell. Duck has no tell
  state (its pose before the shot is the tell).
- [x] Stand time is random in [`standDurMin`, `standDurMax`]; its one snowball at a random time in
  [`standShotMin`, `standShotMax`]; duck time `duckDur` with one shot in [`duckShotMin`, `duckShotMax`]; sled shots
  at 0.5 s and 1 s, each ± `sledShotJitter`.
- [x] Every random value comes from `services.random` (rule `O2-R2`).
- [x] No pool allocation per call in `canIceBlast`, `shoot` and the throw spawn point (rule `O5-R1`).

### Throws and snowball aim (step 4)

- [x] Each ice-blast volley has `iceBlastCount` cubes: 6 on normal, 12 on hard (both phases).
- [x] A volley's aims are `OrbAim` `LEAD`, `CURRENT`, `NEAR`, `WIDE` in turn, shuffled with `services.random`, so a
  6-cube volley has 2 `LEAD`, 2 `CURRENT`, 1 `NEAR`, 1 `WIDE` and a 12-cube volley 3 of each.
- [x] Each throw: `aim_up` when the player is above; else `chunk` with chance `chunkChance`, else `aim`. He faces the
  player at the start of each throw.
- [x] The cube spawns at the release frame (frame 2, at 2 × `throwFrameDur` into the throw), at the strip's spawn
  point (chunk (14, 28), aim (13, 20), aim_up (13, 32) px (play round 1) from the bottom-left of the 64 × 48 cell; x = 64 − px when
  facing right), scaled to the 4 × 3 tile sprite placed bottom-center on the body.
- [x] Chunk: a lob with gravity −22.5 tiles/s² that reaches the aim point (flight time = |dx| / 7 tiles/s, clamped
  to 0.6–1.3 s), no speed clamp. Aim and aim_up: a straight throw at 10 tiles/s to the aim point, no gravity.
- [x] One throw lasts frames × `throwFrameDur` + `throwRest`; the volley ends in `STAND`.
- [x] A snowball on the ground aims at the player's center (an arc with the `Snowhead` gravity, flight time =
  |dx| / 10 tiles/s clamped to 0.4–1 s) when the row's `snowballAimChance` roll passes and the player is in front;
  else the fixed `master` velocities. A snowball in the air drops with no speed, as on `master`.
- [x] The arena for the aim points is between the inner edges of the `wall_1` and `wall_2` objects, from his feet
  up 14 tiles; a spawn with no wall on one side of the boss throws an `IllegalStateException`.

### `FallingIcicle` form and timed drop (step 5)

- [x] With none of `ConstKeys.DELAY`, `ConstKeys.DURATION` and `STATE` = `FORM` (the map icicles; `IcicleTelly`
  uses `STATE` = `FALL`; the `GapingFish` spawn is commented out on `master`), the icicle waits in `STILL` until a live player is under it, shakes 0.25 s, then falls, as on `master`;
  `STATE` = `FALL` still falls at once.
- [x] `STATE` = `FORM`: the icicle plays `form` (4 × 0.15 s, no loop), then goes to `STILL`.
- [x] With `ConstKeys.DELAY`: after `STILL` for that time, it shakes for `ConstKeys.DURATION` (default 0.25 s), then
  falls, with no player under it.
- [x] Each new per-spawn value (`timed`, the still, form and shake timers) is set again in `onSpawn` (pooled reuse).

### Icicle Rain and the double rain (step 6)

- [x] Icicle Rain is in the pick only when the row's `icicleRainWeight` > 0: 0 in `NORMAL` and `HARD` (phase 1).
- [x] It starts with `pound` (5 × 0.1 s); at frame 2 (0.2 s) it submits `SHAKE_CAM` and starts the wave.
- [x] A wave has `icicleCount` icicles (fewer only when the arena has fewer free columns), in 1-tile columns between
  the walls; icicle i spawns at 0.2 s + i × `icicleStagger`, with `STATE` = `FORM`, `DELAY` = `icicleStillDur`,
  `DURATION` = `icicleShakeDur` and `owner` = Glacier Man.
- [x] The first icicle of a wave is in the column of the `OrbAim.LEAD` point of the target player
  (`icicleLeadTime`).
- [x] Each wave leaves a safe gap of 2 adjacent columns with no icicle; the gap is not the lead column and not under
  Glacier Man's body.
- [x] Each icicle forms with its top on the lowest block bottom over its column that is at or above the top of
  Glacier Man's body (14 tiles above his feet if none).
- [x] Under 25 % of the phase-2 health (`healthPhase` 2 and health < 0.25 × max), the move pounds twice
  (`lowHealthRainWaves` = 2), each wave with its own lead icicle and gap; the second pound starts only when no icicle
  of the first wave is alive. Else 1 wave (`icicleRainWaves`).
- [x] After the last wave: `STAND` at 0.2 s + (`icicleCount` − 1) × `icicleStagger` + `icicleWaveRest`.
- [x] Icicle Rain counts in the repeat rule (no 3 times in a row).
- [x] A `FIRED` stun in Icicle Rain stops the spawns; spawned icicles still drop.
- [x] The defeat shatters the boss's live icicles; a destroy of the boss destroys them.

### Sled Ram (step 7)

- [x] Sled Ram is in the pick only when the row's `sledRamWeight` > 0: 0 in `NORMAL` and `HARD` (phase 1).
- [x] A `brake_flash` tell for `sledRamTellDur`, then the `sled` strip at `sledRamSpeed` (12 tiles/s normal, 14 hard;
  the normal sled is 8) toward the player.
- [x] At the first wall it reverses once. It fires no snowballs (owner, play round 1).
- [x] At the second wall (or after 3 s) it goes to `BRAKE`, then `STOP` and `STAND`.
- [x] The x velocity clamp is the ram speed only in Sled Ram; 10 tiles/s in every other state.
- [x] Sled Ram counts in the repeat rule (no 3 times in a row).

### Phase-2 start (step 8)

- [x] The phase-1 to phase-2 roll-over starts a `PHASE_START` pose at the first update where Glacier Man is on the
  ground and not in `INIT`, `JUMP` or `FIRED`; a move in progress on the ground ends at once (as a `FIRED` stun ends
  it); a jump first lands.
- [x] The pose: the `fist_flash` strip (raised fist, no cube), facing the player, x velocity 0, a cyan sprite tint that blinks every
  0.1 s, one `SHAKE_CAM` (0.5 s), `CHILL_SHOOT_SOUND`; it lasts the phase-2 row's `phaseStartDur` (0.8 s normal,
  0.6 s hard).
- [x] No move, no snowball and no throw starts during the pose; it does not count in the repeat rule; then `STAND`.
- [x] Glacier Man takes damage during the pose (as the `077b` and `077c` bosses); a fire hit in the pose starts the
  `FIRED` stun. (In play the hit that rolls the phase over starts the 1.25 s damage timer, longer than the pose.)
- [x] A fire hit that rolls the phase over: the `FIRED` stun first, then the pose.
- [x] The first pick after the pose uses the phase-2 row (`NORMAL_PHASE_2` or `HARD_PHASE_2`).

### Freeze-and-shatter death (step 9)

- [x] At defeat: velocity 0 and gravity off for the whole defeat; `freeze` (5 × 0.15 s), then `frozen_crack`
  (0.25, 0.25, 0.1 s), then `frozen_shatter` (3 × 0.1 s). No damage blink in the sequence.
- [x] At frame 0 of `frozen_shatter` (1.35 s after the defeat), `IceShard.spawn5` once, from cell point (32, 22)
  (mirrored with `64 - x` when he faces right).
- [x] No defeat explosion at any time (owner, play round 1); after the shatter (1.65 s) the sprite is hidden.
- [x] The explosion orb ring and `DEFEAT_SOUND` start at the first `frozen_shatter` frame (with the shards), once;
  none at destroy. During the defeat the boss sprite draws above the orbs (`FOREGROUND`, orb priority + 1); back to
  `PLAYGROUND` 1 on the next spawn.
- [x] The defeat time is 1.65 s + a 1 s hold (2.65 s): the boss is destroyed (end of the fight, stage exit) only
  after the shatter.
- [x] The death is the same in `GLACIER_MAN` and in the `WILY_STAGE_2` refight (no level read).

## Existing tests that this branch can break

- [x] `BossComponentTest` ("exists on a boss that has not spawned yet"): builds `GlacierMan` with no `init()`; the
  new fields must not need the assets.
- [x] `PlayerRulesTest` "PlayerDamageNegotiator": the fallback moved to `fallbackDamage`; the 4 cases must keep their
  values.
- [x] `SpawnAllocationGuardTest`: `GlacierMan.onSpawn` keeps 1 constructor call (the grow-only `walls` rectangle);
  the baseline line `bosses/GlacierMan.kt 1` stays.
- [x] `FightTest` (Glacier Man plans): the fight plan and `end` handling do not change.
- [x] `core:smokeTest` `init()` of `GlacierMan`: the new regions `brake_flash`, `fall_flash`, `chunk`, `aim`,
  `aim_up`, `freeze`, `frozen_crack`, `frozen_shatter` load from `Bosses1` (`defeated` is no longer loaded);
  `entity-tags.txt` does not change.
- [x] `DesertManDamageTest`, `ElecDevilDamageTest`, `BigAssMaverickRobotDamageTest`: they use `SmallIceCube` as a
  damager on other bosses; no change expected.
