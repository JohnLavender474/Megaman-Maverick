# Notes: 183 NES-faithful rotation

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-11-23: rotation inventory


Scan of `master` at `2ed068162`. Read-only. `.worktrees/` not scanned.

Issue #4 ("IMPROVE: Rotated sprites are not faithful to the NES"): sprites that rotate at
non-90-degree angles must use custom regions that show the rotated look, so that the sprite
itself does not rotate.

### Counts

| Class | Sites |
|---|---|
| A | 108 |
| B | 30 (2 world/background, 27 entity sprites, 1 test-only shape) |
| C | 3, plus the generic Tiled `rotation` path |

### Method

- `GameSprite` (`engine/.../drawables/sprites/GameSprite.kt`) extends LibGDX `Sprite`. Its
  `draw()` sends `getVertices()`, so `Sprite.rotation` (with `setOrigin*`) is the only sprite
  rotation path. `GameSprite` adds no rotation code.
- Grep in `engine/src/main` and `core/src/main` for `rotation =`, `rotation +=`, `rotation -=`,
  `setRotation(`, `rotate(`, `rotateBy`, `Affine2`, `transformMatrix`, `ShaderProgram`,
  `ParticleEffect`, `Batch.draw` overloads with a rotation argument, and prod shape suppliers.
  No `Batch.draw` call with a rotation argument, no `Affine2`, no shader, no particle effect, no
  scene2d actor rotation was found.
- Each `sprite.rotation = x` site was traced to the source of `x`.
- `Direction.rotation` is `UP 0`, `LEFT 90`, `DOWN 180`, `RIGHT 270`
  (`engine/.../common/enums/Direction.kt:5`).
- Tiled: LibGDX 1.14.2 `BaseTmxMapLoader` puts an object's `rotation` attribute into its
  properties as `"rotation"` (= `ConstKeys.ROTATION`), and `MapProperties.toProps()` copies it into
  the spawn props. So a Tiled object rotation reaches every entity that reads
  `ConstKeys.ROTATION`. The 39 live TMX files (named in `LevelDefinition.kt`) have no
  `rotation="` attribute. They have `rotation` custom properties only on `PortalHopper` objects
  (`MoonMan_16x14_v5.tmx`, `Test1.tmx`), with the values `0`, `90`, `180` and `-1`.

Classes:

- **A**: the angle is always a multiple of 90.
- **B**: the angle can be a value that is not a multiple of 90 (fixed diagonal, aimed, stepped by
  45, continuous, random, interpolated).
- **C**: the angle depends on data; the note says what decides it.

### Class B: world and backgrounds

| file:line | entity/class | what rotates | angle source | class |
|---|---|---|---|---|
| `engine/.../camera/CameraRotation.kt:51`, `engine/.../camera/CameraOrientation.kt:11` | game camera | the full world view: every world sprite, tile and block | `Interpolation.smooth` blend from old to new `Direction.rotation` over `ConstVals.GAME_CAM_ROTATE_TIME` (`0.75` s); started at `MegaLevelScreen.kt:839` on `START_GAME_CAM_ROTATION` (`players/PlayerGravity.kt:64`) | B (during a turn) |
| `core/.../drawables/backgrounds/Background.kt:132-133` | `Background` | background sprite matrix (own camera) | same blend; started at `MegaLevelScreen.kt:841`; `rotatable` defaults to `true` (`BackgroundLayerBuilder.kt:250`, Tiled bool `rotation`) | B (during a turn) |

### Class B: entity sprites

| file:line | entity/class | what rotates | angle source | class |
|---|---|---|---|---|
| `entities/bosses/ElecDevilBodyPiece.kt:230` | `ElecDevilBodyPiece` | body piece sprite | `(target - start).angleDeg()` | B (aimed) |
| `entities/bosses/elecdevil/LiveWireArcSprites.kt:86` | `LiveWireArcSprites` (used by `ElecDevilBodyPiece`, `ElecDevilClaw`) | each arc tile | `(to - from).angleDeg()` | B (aimed) |
| `entities/projectiles/MoonScythe.kt:195` | `MoonScythe` | scythe sprite | spawn `ROTATION` (trajectory angle, `MoonMan.kt:667`, `MegamanWeaponFire.kt:288`) plus `ROTATIONS_PER_SEC * 360 * delta` (`:133`) | B (continuous spin) |
| `entities/projectiles/SharpStar.kt:116` | `SharpStar` | star sprite | spawn `ROTATION` (`trajectory.angleDeg()`, `MoonMan.kt:707`) plus spin (`:83`) | B (continuous spin) |
| `entities/projectiles/MagmaGoop.kt:156` | `MagmaGoop` | goop sprite | `START_ROTATION (135) + ROTATION`; `ROTATION` is 45/315/135/225/90/270 (`InfernoMan.kt:675-685`) or 90/270 (`SniperJoe.kt:536`, `SniperJoe_OLD.kt:546`); 90/270 give 225/45 | B (fixed diagonal for some shots) |
| `entities/projectiles/MagmaMeteor.kt:181` | `MagmaMeteor` | meteor sprite | `rotation + 135`; `rotation` is UP 0, DOWN 180, LEFT 135, RIGHT 225 (`:67-73`); UP and DOWN give 135/315 | B (fixed diagonal) |
| `entities/projectiles/ChargedShot.kt:280` | `ChargedShot` | shot sprite | `trajectory.angleDeg()`; a bounce sets the trajectory to `(-x, abs(x)/2)` and similar (`:186-229`) | B (after bounce) |
| `entities/explosions/ChargedShotExplosion.kt:142` | `ChargedShotExplosion` | explosion sprite | spawn `ROTATION`: `ChargedShot` trajectory (`ChargedShot.kt:242`) or `EnemyShot` velocity (`EnemyShot.kt:232`) | B |
| `entities/decorations/BulletResidual.kt:70` | `BulletResidual` | residual sprite | `Bullet` trajectory or velocity angle (`Bullet.kt:105-108`); aimed bullets exist (`HeliMet.kt:333`) | B (aimed) |
| `entities/projectiles/TeardropBlast.kt:99` | `TeardropBlast` | blast sprite | `trajectory.angleDeg() + 90`; trajectory from `JetpackBlaster` aim angles 90/120/135/180 (`JetpackBlaster.kt:70-73`) | B (fixed angles) |
| `entities/decorations/FallingPiece.kt:188` | `FallingPiece` | piece sprite | `angle += spin.step`; `FallingPieceDef.BLOCK` step is 45 (`FallingPieceDef.kt:29`); `WOOD_CRATE` 90; `DEBRIS` none | B (45 steps, `BLOCK`) |
| `entities/decorations/D1000DebrisWarning.kt:160` | `D1000DebrisWarning` | ceiling chunk | `WOBBLE_DEGREES * sin(...)` | B (continuous) |
| `entities/hazards/D1000CeilingDebris.kt:222` | `D1000CeilingDebris` | debris sprite | `rotation += spin * delta`; `spin` random in `MIN_SPIN..MAX_SPIN` (`:130`) | B (continuous, random) |
| `entities/projectiles/SmallGreenMissile.kt:152` | `SmallGreenMissile` | missile sprite | `velocity.angleDeg() + 270` under gravity | B (follows velocity) |
| `entities/projectiles/SlashWave.kt:135` | `SlashWave` | wave sprite | `velocity.angleDeg() + 180`; angles random 0-45 / 150-180 (`MegamanWeaponFire.kt:519-532`) and 45/135/225/315 (`RodentMan.kt:126-131`) | B |
| `entities/projectiles/BoulderProjectile.kt:268` | `BoulderProjectile` | boulder sprite | `MEDIUM`/`SMALL`: `rotation + SPEED * delta` | B (continuous spin) |
| `entities/projectiles/Needle.kt:223` | `Needle` | needle sprite | `velocity.angleDeg() + 270`; impulses at angles (`DesertMan.kt:1106,1164,1231`), gravity | B (follows velocity) |
| `entities/projectiles/SpitFireball.kt:179` | `SpitFireball` | fireball sprite | `velocity.angleDeg()`; angles 45/135/225/315 (`:67-69`) | B (fixed diagonal) |
| `entities/projectiles/PurpleBlast.kt:119` | `PurpleBlast` | blast sprite | `velocity.angleDeg()`; `GutsTank.BLAST_ANGLES` 180-220 in 10 steps (`GutsTank.kt:121`) | B |
| `entities/projectiles/CactusMissile.kt:232` | `CactusMissile` | missile sprite | `velocity.angleDeg() - 90`; velocity snapped to 45 toward player (`:182-188`) | B (45 steps) |
| `entities/projectiles/EnemyShot.kt:269` | `EnemyShot` kind `FIRE_PELLET` (`Turn.ROTATE`) | pellet sprite | `velocity.angleDeg() - 90`; aimed at player with offsets (`DemonMet.kt:281-284`) | B (aimed) |
| `entities/projectiles/SphinxBall.kt:177` | `SphinxBall` | ball sprite | `rotation += 45` on a timer | B (45 steps) |
| `entities/enemies/TurnBlaster.kt:203-205` | `TurnBlaster` | all sprites but `base` | `direction.rotation + angleOffset`; `angleOffset` turns at `TURN_SPEED` 90/s, clamp ±45 (`:52-53,146-149`) | B (continuous) |
| `entities/enemies/MockingByte.kt:447` | `MockingByte` | body sprite in `DIVE` | `velocity.angleDeg() - 90`; dive to player position (`:345-358`) | B (aimed) |
| `entities/hazards/SwingingAxe.kt:149` | `SwingingAxe` | axe sprite | `pendulum.angle` in degrees (`:146` is a debug path, also B) | B (continuous) |
| `entities/hazards/WilyCapsuleTentacleScissor.kt:209` | `WilyCapsuleTentacleScissor` | scissor sprite | `atan2` of tip to joint, rounded to 45 (`:124`) | B (45 steps) |
| `entities/hazards/MetHelmet.kt:332` | `MetHelmet` | helmet sprite | `spinSteps * 45` plus direction (`SPIN_STEP_DEGREES = 45`, `:75`) | B (45 steps) |

### Class B: shapes in prod draws

| file:line | entity/class | what rotates | angle source | class |
|---|---|---|---|---|
| `entities/special/TestTentacle.kt:312`, `entities/special/TestWavyTentacleController.kt:193` | test tentacles | filled `GameLine` segments in `addProdShapeSupplier` | joint positions | B (test entities only) |

`WilyCapsuleTentacle` builds `RECT_LINE` lines but does not add them as prod shapes. Other
`DrawableShapesComponent` uses are debug only, or axis-aligned bounds (`WhiteArrowPool.kt:119`).

### Class C

| file:line | entity/class | what rotates | angle source | class |
|---|---|---|---|---|
| `entities/decorations/Rain.kt:226` | `RainDrop` (in `Rain.kt`) | drop sprite | `velocity.angleDeg() + 90`; velocity is `(0, V)` rotated by Tiled `angle` (default `180`, `:241,294,323`) | C: the Tiled `angle` on the `Rain` object; live `IntroStage_v3.tmx` sets none, so today the angle is 360 (A) |
| `entities/special/PortalHopper.kt:280` | `PortalHopper` | portal sprite | Tiled `rotation` property, `-1` means gravity direction | C: Tiled data; live values `0`/`90`/`180`/`-1`, so today A |
| `entities/decorations/ChargedShotResidual.kt:91` | `ChargedShotResidual` | residual sprite | `ChargedShot` trajectory angle at spawn (`ChargedShot.kt:123,135`), before any bounce | C: the trajectories that `ChargedShot` spawners pass (Megaman buster, `MegamanXBuster`, bosses, `TestChargedShooter`); no aimed spawner found next to the `fetch` calls |
| all readers of `ConstKeys.ROTATION` (for example `D1000BombMarker.kt:49`, `Splash.kt:121`, `ForceDecoration.kt:55`, `SlashDissipation.kt:61`) | map-spawnable entities | sprite | a Tiled object `rotation` attribute also lands in this key | C: live TMX files have no `rotation="` attribute, so today A |

### Class A

All angles below come from `Direction.rotation`, a `when` on `Direction`/`Facing` with values
0/±90/180/270, a constant multiple of 90, or a step of 90.

| file:line | entity/class | what rotates | angle source | class |
|---|---|---|---|---|
| `entities/megaman/components/SpritesComponent.kt:109,131,213` | `Megaman` | player, flame, tornado | `getSpriteRotation()` (`:53`), `direction.rotation` | A |
| `entities/megaman/sprites/MegamanTrailSpriteV2.kt:74` | `MegamanTrailSpriteV2` | trail | `getSpriteRotation()` | A |
| `entities/megamanx/MegamanX.kt:413` | `MegamanX` | player | `direction.rotation` | A |
| `entities/protoman/Protoman.kt:648` | `Protoman` | player | `direction.rotation` | A |
| `entities/players/PlayerSabreSlash.kt:161` | `PlayerSabreSlash` | slash | `player.direction.rotation` | A |
| `screens/levels/events/EndLevelEventHandler.kt:188` | end-level beam | beam | `player.direction.rotation` | A |
| `entities/bosses/D1000Core.kt:944,948` | `D1000Core` | beam | ±90 or 0 | A |
| `entities/bosses/RodentMan.kt:621` | `RodentMan` | body | 0 or `direction.rotation` | A |
| `entities/bosses/MoonMan.kt:487` | `MoonMan` | body | `direction.rotation` | A |
| `entities/decorations/D1000BombMarker.kt:59` | `D1000BombMarker` | marker | spawn `ROTATION` ±90/0 (`D1000Volley.kt:217`, `D1000Bomb.kt:114`) | A |
| `entities/decorations/Splash.kt:130` | `Splash` | splash | spawn `ROTATION`, no code caller passes it (default 0) | A |
| `entities/decorations/SlashDissipation.kt:84` | `SlashDissipation` | sprite | `megaman.direction.rotation` (`MegamanWeaponFire.kt:587`) or default 0 | A |
| `entities/decorations/StarknerTrailSprite.kt:82` | `StarknerTrailSprite` | trail | `Starkner` sprite rotation (quantized to 90) | A |
| `entities/decorations/ForceDecoration.kt:66` | `ForceDecoration` | sprite | `direction.rotation` (`DecoratedGravityForce.kt:99`) | A |
| `entities/decorations/Snow.kt:176-177` | `Snow` | flake | spin only when `ROTATION_ENABLED` (`false`, `:40`) | A (B if the flag is set) |
| `entities/enemies/Starkner.kt:418` | `Starkner` | body | `(rawRotation / 90).toInt() * 90` | A |
| `entities/projectiles/HomingMissile.kt:253` | `HomingMissile` | missile | angle snapped to 45; diagonal uses an angled region with `45 - angle` | A |
| `entities/projectiles/SniperJoeShield.kt:178` | `SniperJoeShield` | shield | `Loop(0, 90, 180, 270)` | A |
| `entities/projectiles/GroundPebble.kt:163`, `entities/projectiles/BouncingPebble.kt:188` | pebbles | pebble | `+= 90` | A |
| `entities/special/PortalHopper.kt:288` | `PortalHopper` | moon | `+= 90` | A |
| `entities/projectiles/GreenPelletBlast.kt:160` | `GreenPelletBlast` | blast | axis-aligned trajectory (`UziJoe.kt:236-245`) | A |
| `entities/projectiles/SuperCoolActionStarWarsSpaceLazer.kt:129` | lazer | lazer | horizontal velocity, `% 180` | A |
| `entities/projectiles/TubeBeam.kt:129`, `entities/hazards/TubeBeamerV2.kt:355` | tube beams | beam | 0/90, `direction.rotation + 90` | A |
| `entities/hazards/D1000Pillar.kt:182,214,219,227`, `entities/hazards/D1000Shockwave.kt:144,147` | D-1000 hazards | sprite | 0, ±90 | A |
| `entities/sensors/Gate.kt:388`, `entities/special/SpringBouncer.kt:157`, `entities/enemies/Wanaan.kt:209-212`, `entities/enemies/Met.kt:925`, `entities/enemies/SniperJoe.kt:341`, `entities/enemies/SniperJoe_OLD.kt:408`, `entities/enemies/UziJoe.kt:424`, `entities/enemies/BabySpider.kt:223`, `entities/enemies/SuctionRoller.kt:239`, `entities/enemies/Petaspark.kt:253`, `entities/enemies/ShieldGuardBot.kt:302`, `entities/enemies/ShieldAttacker.kt:326` | various | sprite | `when` with 0/±90/180/270 | A |
| `entities/enemies/PetitDevil.kt:216,343`, `entities/enemies/Bat.kt:360`, `entities/enemies/Adamski.kt:137`, `entities/enemies/TellySaucer.kt:215`, `entities/enemies/Eyee.kt:204` | various | sprite | `direction.rotation` or `targetPlayer.direction.rotation` | A |
| `entities/projectiles/Bullet.kt:251`, `entities/special/GravitySwitcharoo.kt:271` | `Bullet`, `GravitySwitcharoo` | sprite | gravity `direction.rotation` | A |
| 52 more sites with `sprite.rotation = direction.rotation` (see list below) | various | sprite | `direction.rotation` | A |
| `controllers/ScreenController.kt:54` | touch d-pad | button | 0/90/180/270 (`:118-123`) | A |
| `screens/utils/BlinkingArrow.kt:57` | `BlinkingArrow` (`MenuScrollPane.kt:52-53`) | arrow | 90/270 | A |

Sites with `sprite.rotation = direction.rotation`: `FloatingPoints.kt:134`, `UnderWaterBubble.kt:107`,
`WhiteArrow.kt:119`, `AbstractEnergyItem.kt:110`, `HealthTank.kt:91`, `Life.kt:62`, `HeartTank.kt:185`,
`Axe.kt:177`, `BunbyRedRocket.kt:145`, `Fireball.kt:295`, `RocketBomb.kt:122`, `PreciousShard.kt:190`,
`PreciousGem.kt:401`, `MagmaGoopExplosion.kt:146`, `SigmaRatElectricBallExplosion.kt:128`,
`BigAssMaverickRobotOrbExplosion.kt:142`, `SpreadExplosion.kt:220`, `Disintegration.kt:91`,
`SmokePuff.kt:117`, `ToxicGoopSplash.kt:117`, `MagmaWave.kt:209`, `Darspider.kt:257`,
`ChompFlower.kt:199`, `HeliMet.kt:307`, `Hanabiran.kt:240`, `PopupCanon.kt:248`, `FireFlower.kt:234`,
`BunbyTank.kt:384`, `ConveyorBelt.kt:175`, `DragonFly.kt:276`, `Lift.kt:168`, `TorikoPlundge.kt:334`,
`PropellerPlatform.kt:151`, `FlameThrower.kt:222,228`, `MoonEyeStone.kt:359`, `Bolt.kt:123`,
`RocketPlatform.kt:173`, `LavaBeam.kt:103`, `SaucerRay.kt:114`, `Spike.kt:302`, `InfernoOven.kt:222`,
`ElectrocutieChild.kt:140`, `LaserBeamer.kt:252`, `PipePuff.kt:208`, `LavaBeamer.kt:173`,
`Lava.kt:249`, `PreciousSpike.kt:119`, `UnderwaterFan.kt:281`, `MagmaFlame.kt:198`, `SwitchGate.kt:170`,
`Flame.kt:114`.

### Not sprite rotation (checked, out of scope)

- `GameLine`/`GamePolygon`/`GameRectangle` rotation, `Body`/`Fixture` rotation, `RotatingLine`
  (`Saw.kt`, `RotationAnchor.kt`, `JetpackBlaster.kt:208`): collision shapes, motion paths and
  aim lines. The `Saw` and `RotationAnchor` sprites move on a circle and do not rotate.
- `rotateDeg` on `Vector2` (trajectories, offsets): no drawing.
- `ElecDevilClaw.kt:207`: tether fixture normal; the tether art is `LiveWireArcSprites` (B above).
- `CameraShake`: translation only.
- Fonts, HUD and screen transitions: no rotation found.
