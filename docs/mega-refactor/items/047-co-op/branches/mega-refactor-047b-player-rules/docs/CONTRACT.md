# Contract: mega-refactor-047b-player-rules

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes, contract approved")

## Intended changes

Without `--players 2`, this branch changes no gameplay.

With `--players 2`:

1. With no `--mode`, or with `--mode coop`, friendly fire is off. A shot, an explosion and the needle spin from one player do not hurt the other player. A projectile passes through the other player and their shield, and it does not explode there. The same applies to an entity that the other player owns, for example a `PreciousGem`, and to a shot of the other player.
2. With `--mode pvp`, friendly fire is on, and damage between players acts as it does on `master`.
3. An entity that reads `targetPlayer` gets the nearest live player, by the distance from the seeker. A tie goes to the lower slot. A seeker with no body gets the first live player in slot order. With no live player, the result is the same as on `master`.
4. `Lift`, `Gate`, `FallingIcicle` and `GroundSlideBlocker` react to any live player. While one player is live, a dead player does not count at these sites, or at `SwimBooster`, `SpiderWeb`, `MagFly` and the `BigAssMaverickRobot` hands. With no live player, all players count, as on `master`.
5. `SwimBooster` boosts each player in the water.
6. `SpiderWeb` holds, follows and frees the player that it stuck to.
7. `BigAssMaverickRobot` skips its hand launch while a player stands on each hand. It does not throw.
8. A `ChargedShot` from a player takes the direction and facing of that player.
9. A `HealthBulb` and a `WeaponEnergyBulb` go to the player who collects them. Lives, currency, heart tanks and health tanks stay shared.

Other intended changes:

- `--mode <coop|pvp>` is a new launch option, and case does not matter. A bad value, `single`, or either mode with 1 player, exits with code 2.

## Known wrong with `--players 2`

These are not failures of this branch.

- A target can flip between 2 players at almost equal distance (`112`).
- A boss can change target during an attack, and its parts can choose a different player from the main body (`047e`, `112`).
- Slot 1 has no HUD, so its health gain does not show, and only slot 0 can pause (`047d`).
- A slide by one player turns `GroundSlideBlocker` off for both players.
- All webs share one stuck set, and each web reacts to any player death (`096`, `047e`).
- `RisingLavaRiver` speeds up from the nearest player, so the speed-up can change from frame to frame (`047e`).
- The drop odds and the one-up read the nearest player (`047` open questions, "Rewards").
- The death of slot 1 resets a gauntlet (`047c` or `047e`).
- A boss intro can wait for a dead slot 1 (`047e`).
- When both players stand on one `QuickSand` area, neither player can jump out (`docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`).

## Must not change

Without `--players 2`:

- The target of every entity, also in the time between a death and a respawn.
- All damage: to the player, to enemies, and from reflected shots.
- Projectile contacts: hits, shields, block and water contacts.
- `Lift`, `Gate`, `FallingIcicle`, `GroundSlideBlocker`, `SwimBooster`, `SpiderWeb`, `ChargedShot` and the `BigAssMaverickRobot` hand launch.
- Health and weapon energy pickups, with the stop of the world and the sounds.
- The saved data.
- No new allocation per frame.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] A new test: `--mode` accepts `coop` and `pvp` with 2 players, and refuses a bad value and either mode with 1 player.
- [ ] A new test: the rules of each `GameMode`.
- [ ] A new test: `targetFor` with 1 player (live, dead), 2 live players, 1 dead, none live, a tie, a player in `PRE_PLAY`, and a seeker with no body.
- [ ] A new test: `canBeDamagedBy` for a teammate's shot, and for an explosion that the shot owns, with friendly fire on and off.
- [ ] A new test: `canBeDamagedBy` of a `PreciousGem` that a player owns, and `ProjectileRule` on a shield that a teammate owns, with friendly fire on and off.
- [ ] A new test: `anyLive` and `forEachLive` skip a dead player and a player who is not in play, and visit all players when no player is live.
- [ ] A new test: `ProjectileRule` does not call `hitBody` or `hitShield` for a teammate with friendly fire off, and does with it on.
- [ ] A new test: `SpiderWeb` unlocks the player that it stuck to, also when the other player is nearer. No live map has a `Bospider`, so this test is the only check of that change.
- [ ] A new test: `Lift` reacts to a player who is not the nearest. This test covers `Lift`, because only `TEST_1` has one.
- [ ] A new test: `ADD_PLAYER_HEALTH` and `ADD_WEAPON_ENERGY` act on the slot in the event, if `testServices()` can reach the handler. If it cannot, the plan says so, and the manual check covers it.
- [ ] `--diagnostics` before and after, in one real stage with no `--players`, shows no slower frame time (`O5-R2`).

## Manual checks

Run all checks from this folder:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047b-player-rules`

A plan step adds the rooms `target_nearest` and `pickups` to `RefactorTest047.tmx`. The room names are the planned names.

Keep the keyboard for player 1 and the gamepad for player 2.

### Experimental checks

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2`: shoot each player with the other, with a normal and a charged shot. The shots pass through, they do not explode, and no player takes damage.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2 --mode coop`: the same result as the check above.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2 --weapons all`: try each weapon on the other player, with the needle spin and the shields too. No damage, and a shield does not deflect the shot. Shoot the `PreciousGem` shield of the other player: the shot passes through, and the gem takes no damage.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2 --mode pvp`: shots hurt the other player, as on `master`.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room target_nearest --players 2`: stand one player on each side of the enemies. Each enemy faces and fires at the nearer player. Move to change which one is nearer. Then kill player 2: all enemies go for player 1. Stand at equal distance and write down how often a target flips.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room target_nearest --players 2`: charge a shot with player 2 while player 1 stands behind an enemy. The shot takes the direction and facing of player 2.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room pickups --players 2`: hurt player 2, then collect a `HealthBulb` with player 2. Player 2 heals, and player 1 does not. Do the same with a `WeaponEnergyBulb`. The `Life` raises the shared count.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room ground_slide_blocker --players 2`: slide with player 2 while player 1 stands away. The blocker turns off for the slide.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1 --players 2`: walk player 2 under a `FallingIcicle` while player 1 is nearer to it but not under it. The icicle falls.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1 --players 2`: kill player 2 under a `FallingIcicle` that has not fallen, with player 1 away from it. The icicle stays up while player 2 is dead.
- `./run-game-alpha.sh --level DESERT_MAN --room room13 --players 2`: leave player 1 behind, and walk player 2 to the `Gate`. The `Gate` opens.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room9 --players 2`: play through `room10`, and take both players through the transition into the water of `room11`. Both get the boost.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --players 2`: go to the boss, and stand one player on each hand until the launch time passes. The game does not crash, and the launch comes after a player leaves a hand.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --mode pvp`: the game exits with code 2 and prints an error.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --mode coop`: the game exits with code 2 and prints an error.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2 --mode bad`: the game exits with code 2 and prints an error.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2 --mode PVP`: the game starts in PvP.

### Control checks (no flag)

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room target_nearest`: one player. All enemies aim, face and fire at the player, as on `master`.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room pickups`: health and weapon energy fill, with the stop and the sound, as on `master`.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the death, the life count and the respawn act as on `master`.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room ground_slide_blocker`: the blocker acts as on `master`.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: a short pass of a real stage, with `FallingIcicle` and enemies, as on `master`.
- `./run-game-alpha.sh --level DESERT_MAN --room room13`: the `Gate` and the room transition act as on `master`.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room9`: play to `room11`. The `SwimBooster` boost acts as on `master`.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the boss fight, with the hand launches, acts as on `master`.

The plan confirms that each `--room` above has a player spawn, and that each real-stage room leads to the object that its check names.
