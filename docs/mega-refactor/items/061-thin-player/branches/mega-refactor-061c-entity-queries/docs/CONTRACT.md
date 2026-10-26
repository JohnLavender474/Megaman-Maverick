# Contract: mega-refactor-061c-entity-queries

The gameplay that this branch must not change, and how to check it. Written ahead of time on `master`.

- Status: draft
- Approved: no

The player view class goes: a player is an entity with a `PlayerComponent(slot)`, and every caller asks the entity.
Every read is the same field or call as today, so everything that looks at the player must act as on `master`:
enemies that aim at, chase or touch the player, hazards and blocks that react to the player, items and pickups, life
drops, the HUD, pause, the camera, spawn, death, respawn, the level end, the gravity device, the dark rooms, input
readers, and the boss reaction to player damage.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 501 tests on `master`, with the players tests changed for the registry,
      plus the new tests.
- [ ] `./gradlew core:test` passes: the 167 tests on `master`, with the player tests changed, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (110 on `master`).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] Registry tests (engine): `register` adds `PlayerComponent(slot)` and `unregister` removes it; `get`,
      `forEach`, `any`, `all`, `registeredSlots` follow slot order; `playerSlot` and `isPlayer` read the component;
      registering an occupied slot or an out-of-range slot fails as today.
- [ ] Query tests: on an entity without the parts, the needed parts fail at the read (`body`, `facing`, `direction`,
      `focus`, health) and the optional parts give their empty value (`hasBody`, `input` = `RELEASED`, behaviors,
      `invincible`, `damaged`, light, gravity device, `shooting`); on an entity with the parts, each reads the part.
- [ ] `MegaPlayersTest`: registering binds the slot's input source to the entity's `ControllerComponent`;
      `targetFor` gives the first player with a body; the damage signal reaches listeners in order with the entity.
- [ ] Code checks (grep): no `MegaPlayer` type, `PlayerView`, `viewOf`, `MegaPlayerAccess.kt` or `megaman` accessor;
      `input` returns `InputFrame` with no nullable step (`javap`: no `InputFrame.box-impl` in the query).
- [ ] One for one: a script compares each changed read before and after (receiver change only:
      `targetPlayer.X`/`x.asPlayer?.X`/`player.X` read the same member through the entity).

## Manual playthrough

The owner plays these once, after the implementation, from the worktree. Most rooms are in `REFACTOR_TEST_029`.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: movement, shooting, charge, weapon switch, pause
  (the pause menu shows the health and the weapons); take damage.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: `Met` and `SniperJoe` face and shoot at Megaman.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_chase`: the `Bat` chases Megaman.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_drop`: `Darspider` and `SuicideBummer` drop or dive
  at Megaman.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_contact`: contact damage from `Coldier` and
  `Matasaburo`.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_magfly`: `MagFly` pulls Megaman, not while he climbs.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups`: each pickup works (health bulb, health tank, screw,
  weapon energy); a killed `Met` can drop an item.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room heart`: the heart tank raises the maximum health.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room lives`: the extra life adds a life.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room hit_by_player`: `SmallGrass` and `Togglee` react to Megaman.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room push_flip`: `PushableBlock` and `FlipperPlatform` react to
  Megaman's sides and feet.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: the gravity switch.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the `GravityBlock` kill; the death and the
  respawn.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: the stun pulse stuns Megaman.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`, `--room spring_bouncer` and
  `--room ground_slide_blocker`: the input readers (DOWN into the pipe, the stronger bounce, the blocker off while
  sliding or holding DOWN).
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the boss intro, fight and defeat, the level-end beam-out.
- `./run-game-alpha.sh --level DESERT_MAN`: the dark room light around Megaman.

## Behaviors to check by review

- `PreciousWoman` laughs on a player hit (the damage signal now carries the entity); the `SpiderWeb` input reads (no
  map spawns it).
- The life drop chance and the 1-up use the target's slot (`AbstractEnemy.kt:103,332`), now from `playerSlot`.

## Intended changes

None.
