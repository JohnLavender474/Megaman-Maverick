# Contract: mega-refactor-014b-typed-fixture-receivers

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-22

This branch changes how fixture receivers are stored and read: typed object keys instead of string
keys and hand-written extension functions. Each receiver must still be stored on the same fixture,
and called at the same point, with the same arguments, in the same order. So no gameplay may
change. The compiler checks most of this. The play test covers the entities that register
receivers.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 427 tests on `master`, plus the new `PropertyKey` tests.
- [ ] `./gradlew core:test` passes (77 on `master`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (133 on `master`). The count may drop, because the
      unchecked casts in the old trios go away.
- [ ] None of the old receiver functions remain:
      `grep -rnE "\b(set|has|get)HitBy[A-Za-z]*|setConsumer|getConsumer|hasConsumer|setFilter|hasFilter|getFilter|setRunnable|getRunnable|setVelocityAlteration|getVelocityAlteration|ForceAlteration|ShouldStickToBlock|shouldStickToBlock|setHitByShield|ExceptionForNoProjectileCollision" core/src/main/kotlin`
      finds nothing.
- [ ] No string-built receiver key remains (`"${ConstKeys.HIT_BY_BLOCK}_..."`, `"${ConstKeys.HIT_BY_FEET}_..."`,
      `"..._force_velocity_alteration"`, `"${FixtureLabel.NO_PROJECTILE_COLLISION}_${ConstKeys.EXCEPTION}"`).
- [ ] A review by hand of `MegaContactListener.kt` and `MegaContactFilter.kt` in the diff: each read
      is at the same place, and the four special cases keep their meaning:
  - velocityAlteration: a missing value still throws;
  - stickToBlock: a missing value still means `true`;
  - forceAlteration: a missing value still falls back to `VelocityAlterator.alterate`;
  - the no-projectile-collision exception: a missing value still means `false`.
- [ ] A review of the migration script and of a sample of about 15 migrated registration sites,
      including labelled returns and the 7 sites that passed a function-typed `val`.

## Manual playthrough

The entities below register receivers. A level scan of the 18 live maps found 48 of the 74
classes that register receivers. The levels below cover all 48. The other 26 are spawned at run
time (projectiles, items, Megaman's own fixtures), and the same levels exercise most of them.

| Level | What to check |
|-------|---------------|
| `TEST_1` | The most coverage (22 classes). Ride the cart (`CartinJoe`), bounce on `SpringBouncer` and `SpringHead`, ride `ConveyorBelt`, step on `CrumblingBlock` and `Togglee`, swim near `UnderwaterFan`, and fight `Jetto`, `FlyBoy`, `SpikeBot`, `Bat` and `DrillTankXT`. Shoot enemies and blocks. Touch the ladder. |
| `GLACIER_MAN` | Walk on `GroundSnow` (slower, snow sense), break `BreakableIce`, and fight `Coldier`, `IcicleTelly`, `SeaMine` and `Tropish`. |
| `DESERT_MAN` | Ride `CartV2` (side contacts on the cart). Walk on quicksand. |
| `SMB3_1` | Stomp `Goomba`, kick a `GreenKoopa` or `RedKoopa` shell into blocks and enemies. |
| `MOON_MAN` | Stand on `GravityBlock`, and watch the asteroids from `AsteroidSpawnersBlock` hit blocks. |
| `REACTOR_MAN` | Touch `DrippingToxicGoop` and `ToxicWaterfall`, and fight `UFOhNoBot`. |
| Optional | `RODENT_MAN` (`FloorButton`, `RatRobot`), `TIMBER_WOMAN` (`LumberJoe`, the boss), `WILY_STAGE_1` (`GreenUziJoe`, `SpikeCopter`, the Desert Man refight), `INTRO_STAGE` (`BikerKibbo`), `INFERNO_MAN` (`FrozenBlock`), `PRECIOUS_WOMAN` (`PinkBlock`), `GAUNTLET_2` (`MagFly`). |

Use `./run-game-alpha.sh --level <LEVEL>` from the worktree; add `--room <name>` to jump to a room.

## Behaviors to check

- Megaman: land sound, jump after landing, wall contact, ladder, water entry and exit (splash,
  swim), standing on a moving platform (stick to block), being pushed by a force or a spring.
- Shots: Megaman's buster shots hit blocks, enemies and shields as before.

## Intended changes

None. The `hitByLaser` receiver kind is deleted, but no entity registered it, so its two reads in
`MegaContactListener` were always false. This removes dead code only
(`docs/issues/2026-10-22-unused-contact-hooks.md`, the `setHitByLaserReceiver` part).
