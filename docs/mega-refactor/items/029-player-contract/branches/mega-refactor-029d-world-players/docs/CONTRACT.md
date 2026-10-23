# Contract: mega-refactor-029d-world-players

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: draft
- Approved: no

This branch changes how the world entities reach the player, adds `WorldGravity` as the one source of
the gravity direction, renames `MegamanDeath` to `PlayerDeath`, and comments out two unused paths.
With one player every helper gives Megaman and `WorldGravity` always equals Megaman's direction, so
every world entity must act as before.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 110 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test passes, and `contact-rule-selection.txt` is unchanged.
- [ ] No world entity, contact rule or world body code reads the global player: the grep of `029c`
      over `entities/special`, `projectiles`, `blocks`, `hazards`, `decorations`, `sensors`,
      `explosions`, `world/` and `damage/` (code lines; commented-out files left out) finds only the
      exact-class `Megaman::class` sets, Tiled property names and tag strings.
- [ ] Every write of a player's direction goes through `WorldGravity.set`, except inside the player's
      own setter (grep for `direction =` on a player view or `Megaman`).
- [ ] `TODO`s exist for: the camera rotation in the player's direction setter (belongs to
      `WorldGravity`); Moon Man setting gravity only while the target player is alive (`047`);
      `SwimBooster`'s swim velocity (`029g`); `ChargedShot` direction from the owner (`030`);
      `FeetSandRule` (`047`).
- [ ] `MegamanDeath` exists nowhere (Kotlin and TMX); `PlayerDeath` is in the 4 TMX files with 29
      objects.
- [ ] `GravityCapsule` is commented out with its `TODO`; `PortalHopper`'s `next_body_direction` is
      commented out with its `TODO`.
- [ ] `HIT_BY_PLAYER` takes `MegaPlayer`, and `BodyPlayerRule` has no cast and no `029` `TODO`.
- [ ] A review shows only the player reference changed in each moved expression, and the intent
      rule followed (target, `any`, `forEach`, `viewOf`, `gravity.direction`).

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline
play-through; see "Test maps" in `docs/mega-refactor/CLAUDE.md`). The test rooms are added to
`RefactorTest029.tmx` in the plan, and a short launch checks that each one loads on the baseline
code; their names can change in the plan.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `player_death` | Falling into the pit kills Megaman (`PlayerDeath`, after the rename); a falling `GravityBlock` that lands on his head kills him. |
| `REFACTOR_TEST_029`, `push_flip` | Megaman pushes the `PushableBlock` only while touching it, facing it and moving toward it; the `FlipperPlatform` flips when he stands on it. |
| `REFACTOR_TEST_029`, `hit_by_player` | `SmallGrass` rustles, `Togglee` switches, and `SmallIceCube` shatters (when Megaman cannot be damaged) when he touches them. |
| `REFACTOR_TEST_029`, `gravity_switch` | The `GravitySwitcharoo` flips gravity and rotates the camera; after the flip, A jumps if Megaman was on the ground and air dashes if he was in the air; a `ConveyorBelt` in the room keeps working in both gravities. |
| `MOON_MAN`, `room6` | The gravity switch flips gravity; the falling `GravityBlock` follows the gravity. |
| `MOON_MAN`, `room11` | The portal hopper sends Megaman through and sets his direction as before. |
| `MOON_MAN`, `room12` | Megaman respawns at the checkpoint with the gravity of the spawn. Moon Man's gravity swap during the fight (as in `029c`), and none while Megaman is dead. |
| `WILY_STAGE_3`, `room2` | The gravity switch flips gravity; walking on to `room8` sets gravity back up (the room property). |
| `WILY_STAGE_3`, `room4` | The capsule teleporter sends Megaman and sets his direction on arrival. |
| `WILY_STAGE_1`, `room8` | The dark room: Megaman gives light, larger while charging; the light follows the beam-in. |
| `DESERT_MAN`, `room9` | The dark room light as above; `CartV2` moves when Megaman stands on it. |
| `DESERT_MAN`, `room2` | Quicksand splashes as before. |
| `GLACIER_MAN`, `room6` | Conveyor belts carry Megaman as before. |
| `PRECIOUS_WOMAN`, `room2` | Pink blocks break only from the laser that Megaman reflected; the laser beamers fire only while Megaman is ready. |
| `TIMBER_WOMAN`, `room13` | The swim booster pushes Megaman up in water. |
| `TIMBER_WOMAN`, `room8` | Flipper platforms flip under Megaman. |
| `RODENT_MAN`, `room7` | The pushable block moves as before. |
| `SMB3_1`, `room2` | Question blocks give their item when hit from below; pipes take Megaman in and out; Bullet Bill launchers stop while Megaman is dead or beaming in. |

## Behaviors to check

- The game boots, and a level starts with no exception.
- Projectiles that Megaman fires hit and break things as before (wood crates, breakable blocks, the
  Wily fake capsules).

## Intended changes

None.
