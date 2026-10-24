# Notes: mega-refactor-029f-level-players

## Decisions

## Overlap with other branches

No other `029` branch is active. No branch of `001`, `008` or `019` is active (`001b` is blocked). `019`
(spawn phase) will touch the spawn pass in `MegaLevelScreen.render`; this branch does not change that code.

## Scan (2026-10-24, on `5da7a6db5`)

Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### What is left of the global player

- `MegamanMaverickGame.megaman` (`:211`, `lateinit`), made in `postCreate` (`:397-407`): `init`, the
  `GameState` listener, the registration in slot 0. Also `resetPlayerProgress` selects the buster
  (`:777`).
- The extension `IMegaGameEntity.megaman` (`entities/contracts/IMegaGameEntity.kt:22`). Users:
  - `AbstractItem:83` (spawn direction default) and `:178` (follows the direction each frame): the
    player's direction used as the world direction.
  - `MegamanTrailSprite` (V1, `entities/megaman/sprites/`, 6 reads of the global for its sprite). It is
    only in the deprecated `DecorationsFactory` pool, and nothing fetches that pool key (V2 is the live
    trail; `029a` fixed its self-references).
- `game.megaman` in screens and handlers:

| File | Uses | What |
|---|---|---|
| `screens/levels/MegaLevelScreen.kt` | 20 | a `megaman` property (`:154`); checkpoint only while `ready` (`:233`); `PlayerStatsHandler(megaman)` (`:251`); camera focus (`:268`); room-transition projectile cleanup `owner != megaman` (`:300`); a log (`:324`); gravity aura at the player's center (`:372`); `running = false` after a transition (`:381`); `megaman.spawn(spawnProps)` on `PLAYER_SPAWN_INIT` (`:595`); `BOSS_DEFEATED`/`BOSS_DEAD` do nothing if dead (`:790, 819`); `canBeDamaged` false on a big boss defeat (`:805`), true on `INTERMEDIATE_BOSS_DEAD` (`:831`); pause only if not dead and `ready`, forced resume if dead (`:915, 921`); death handler inactive if not dead (`:950`); focus snap-back reads `getFocusPosition` (`:972`); death draw if dead (`:1046`) |
| `screens/levels/events/PlayerSpawnEventHandler.kt` | 14 | beam-in: `ready`, `canBeDamaged`, `body.physics.gravityOn`, `setAllBehaviorsAllowed`, body position, facing, `getSpritePriority`; beam properties keyed by `Megaman.TAG` |
| `screens/levels/events/EndLevelEventHandler.kt` | 15 | beam-out: same, plus `currentWeapon` (beam color), `direction`, `shouldFlipSpriteX/Y` |
| `screens/levels/events/PlayerDeathEventHandler.kt` | 4 | `gravityOn`, `ready`, the slot for `PLAYER_DONE_DYIN` |
| `screens/levels/stats/PlayerStatsHandler.kt` | 22 | constructor `Megaman`; health bar (current, max), weapon bar (`ammo`, `currentWeapon`), heart tank (max health), weapon energy (`weaponsHandler.getAmmo`, `translateAmmo`), health (`getMaxHealth`, `getCurrentHealth`, `translateHealth`), the slot for `ADD_PLAYER_HEALTH` |
| `screens/menus/level/LevelPauseScreen.kt` | 8 | a `megaman` property; health bar, weapon ammo bars, sets `currentWeapon`; health tank use (`hasMaxHealth`, `getMaxHealth`, `getCurrentHealth`, `translateHealth`) |
| `screens/other/GetWeaponScreen.kt` | 2 | `currentWeapon` (the start of the weapon animation) |

The weapon and jetpack handlers (`entities/megaman/handlers/`) and `MegamanDamageNegotiator` use their own
`megaman` field (the player that owns them). They are not global uses.

### By kind

1. **Generic player state** (any character has it): `ready`, `dead`, `canBeDamaged`, `setAllBehaviorsAllowed`,
   `direction`, `facing`, body. `MegaPlayer` already has these.
2. **Generic, not yet on `MegaPlayer`**: health current, max and `translateHealth` (HUD, pause);
   `spawn(props)` (level screen); the camera focus (`IFocusable`, Megaman implements it) and
   `getFocusPosition` (level screen).
3. **Megaman only**: `currentWeapon`, `ammo`, `weaponsHandler.getAmmo`, `translateAmmo` (HUD, pause,
   end-level beam color, get-weapon screen, `resetPlayerProgress`); `running` (level screen);
   `getSpritePriority`, `shouldFlipSpriteX/Y` (the beam sprites). These are the loadout (`024`) and the
   character's look (`029g`).
4. **Identity**: the projectile cleanup `owner != megaman` (the `029d` rule: `owner.asPlayer == null`).

### Rules that assume one player (the owner keeps them unchanged; decision 2026-10-23)

The spawn, death, beam-in, beam-out, pause and boss-defeat flows each act on "the" player. With capacity 1
every choice below acts on Megaman. The co-op rules are `038` (spawn split) and `047`.

### Other `Megaman` names that stay

- The exact-class `KClass` sets (`StandardDamageNegotiator`, `PreciousWoman`, `PreciousTron`, `SwinginJoe`,
  `UnderwaterFan`, `SmallIceCube`, the Darkness light tables, `JoeBall`/`Snowball` masks): `030` or `023`.
- Tiled property names built from `Megaman.TAG` (`CapsuleTeleporter:72`, `Spike:181`), the room property
  `megaman_direction`, the tag filter string in `DecoratedGravityForce`, the `LevelSelectScreen` atlas
  region, log text. Renaming them would change the maps or assets.
- The beam properties `"${Megaman.TAG}_beam"` and `_beam_center` (written by the spawn and end-level
  handlers, read by `DarknessV2` and `DarknessV4`): one global value, read as "the player is beaming".

### Tests

- No core test uses `game.megaman`. The level screen and handlers have no tests.

### Open questions for the owner

1. **How the level code names its player.** Recommendation: each class that needs "the" player reads
   `game.players.get(PlayerSlot(0))!!` through one private property, with a `TODO` that the lead slot is a
   session policy (`047`). No public `primary` or `lead` on `MegaPlayers` (decision 2026-10-23).
2. **Generic reads that are missing on `MegaPlayer`.** Recommendation: add only the ones with a caller here:
   health (`currentHealth`, `maxHealth`, `translateHealth`) for the HUD and pause, `spawn(props)` for the level
   screen, and `focus` (`IFocusable`, required at registration) for the camera and the snap-back check.
3. **Megaman-only reads** (weapons, ammo, current weapon, `running`, the beam sprite priority and flip).
   Options:
   - (a) one typed accessor in the player package, for example `val MegaPlayer.megaman: Megaman` (the entity
     cast once), used by the HUD, pause, beam handlers, get-weapon screen and `resetPlayerProgress`, with a
     `TODO` (`024` loadout, `029g` character look);
   - (b) receivers on the player entity for each of them (as the `029d` gravity and light receivers).
   Recommendation: (a). These screens are Megaman's UI today; (b) adds about 8 receivers that `024` and
   `029g` replace.
4. **Level-wide writes** (`canBeDamaged` on boss defeat and on the intermediate boss, `running = false` after
   a room transition). Recommendation: `players.forEach` (the same with one player, right with more).
   **Level rules** (pause, the boss-defeat dead checks, death draw, checkpoint `ready`, spawn, the gravity
   aura position, focus): the lead slot (question 1), with `047` `TODO`s.
5. **`MegamanTrailSprite` (V1)**: nothing fetches it. Recommendation: comment it out and its
   `DecorationsFactory` entry, with a `TODO` (the owner's rule for unused paths in `029d`).
6. **`AbstractItem`**: the item direction follows the world direction. Recommendation: `game.gravity.direction`
   (the `029d` rule).
7. **The beam properties keyed by `Megaman.TAG`**: recommendation: keep them, out of scope (a string key, one
   value for the level; per-player beams are `047`).
